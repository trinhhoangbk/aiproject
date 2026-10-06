# Development · Cycle 6 — M5 Workload+Overdue views & M8 Security

| Field | Value |
|---|---|
| Date | 2026-10-06 16:34 ICT |
| Scope | PLAN-020 + PLAN-021 (M5) · PLAN-028 + PLAN-029 + PLAN-030 (M8) |
| HARD GATE | PLAN APPROVED carried · CP-1 still not signed (not required for this cycle) |
| **Status** | **🟢 IMPLEMENTATION COMPLETE for M5 and M8** |

> Same posture as cycles 4 and 5: code lands, nothing calls Jira. M5 reads the
> M4-populated `mv.*` tables and resolves capacity through `CapacityResolver`; M8
> wraps the entire REST surface in Spring Security without changing any endpoint
> contract that M5 (or earlier) committed.

---

## 1. PLAN-020 — Workload / overload API (M5)

### 1.1 DTO shape — `com.mbs.hub.workload.dto.WorkloadView`
Mirrors 02 API §5.2 verbatim:
```
WorkloadView(
  memberId, memberName, email, role,
  horizon, windowStart, windowEnd,
  standardMd, committedMd, availableMd,
  allocationRate, band,                       // DEC-011
  overload: OverloadView(...),                // null when !isOverloaded
  projectBreakdown: List<ProjectBreakdown>,   // committed_md per project_key
  unestimated: UnestimatedSummary(count, md)  // B-RULE-02 bucket
)
```
Nested records: `OverloadView(isOverloaded, reasons: List<OverloadReason>)` with each
`OverloadReason(code, detail, hardDeadlineCriteria)` so the Hard-Deadline branch
can list which of {`fix_version_release`, `priority_blocker`, `hub_lock_flag`} fired
(DEC-010).

### 1.2 `WorkloadService.loadWorkload(memberId, horizon, anchor)`
1. `MemberRepository.findById()` → 404 if missing.
2. `CapacityResolver.resolve(member)` returns the member's `dailyHours` under the
   three-tier (Member > Team > Global) rule from DEC-004.
3. Look up the `mv_allocation_rate` row for `(member_id, horizon, windowStart(anchor))`;
   if the refresher has not yet produced one (new member, no events), return a
   zero-valued view with `band=DARK_GREEN` and `allocation_rate=0` — the UI shows
   "no data yet" rather than a 500.
4. `IssueProjectionRepository.findActiveByAssignee(...)` for the overload calc:
    - **B-RULE-03 week slice**: sum `remainingEstimateHours` of issues whose
      `due_date` falls within the Monday-anchored current week. `>40h` ⇒ reason
      `week_over_40h` with the exact figure in `detail`.
    - **Daily cap** (within the same slice): bucket by `due_date` day and compare
      to `dailyHours`; any day over cap with `cannot_reschedule=true` on at least
      one of its issues ⇒ reason `day_over_cap_cannot_reschedule`.
    - **DEC-010 Hard Deadline** scan: an issue is a hard deadline when it
      satisfies ANY of
      `fix_version_release_in_window` ∨ `priority == Blocker` ∨
      `hub_lock_deadline_flag_set`.
      Each firing criterion is pushed into `hardDeadlineCriteria` on the reason.
5. Project breakdown is computed from the same active-issues set grouped by
   `projectKey` → sum of `remainingEstimateHours ÷ dailyHours` to 2 d.p.
6. `UnestimatedSummary` counts issues with `remainingEstimateHours == null OR == 0`
   and attributes the B-RULE-02 default `0.5 MD` to each one.

### 1.3 `WorkloadController` — `GET /api/workload/{memberId}`
Query params: `?window=week|2weeks|month` (default `week`), `?anchor=YYYY-MM-DD`
(default today in Asia/Saigon via the injected `Clock`).

Authorisation (filter-chain enforced via `@PreAuthorize`):
```
hasAnyRole('ADMIN','MANAGER')
  or (hasRole('MEMBER') and principal.memberId.toString() == #memberId.toString())
```
MEMBER role sees ONLY their own row (DEC-005 extension to API filter). Admin and
Manager see any member.

### 1.4 `/api/workload/{memberId}/overload`
Same auth. Returns `OverloadView` directly for callers that only need the
breaker signal (used by M10 SPA badge).

## 2. PLAN-021 — Overdue + Early-Warning API (M5)

### 2.1 `OverdueService`
Reads from `mv.mv_member_overdue` (M4-maintained):
- `overdue(memberId)` → rows where `daysOverdue > 0`, sorted by `daysOverdue DESC`.
  Maps to `OverdueView(issueKey, projectKey, dueDate, daysOverdue, overdueBand)`.
- `warnings(memberId)` → rows where `daysOverdue == 0 AND tprBand != 'none'`,
  sorted by `tpr ASC` (closer-to-breach first).
  Maps to `WarningView(issueKey, projectKey, dueDate, tpr, tprBand)`.

### 2.2 `OverdueController`
- `GET /api/overdue?memberId={uuid}` → List<OverdueView>
- `GET /api/overdue/warnings?memberId={uuid}` → List<WarningView>

Same `@PreAuthorize` pattern as workload: self-only for MEMBER, any for
ADMIN/MANAGER.

## 3. PLAN-028 — Spring Security filter chain (M8)

### 3.1 `SecurityConfig`
- `@EnableWebSecurity`
- `@EnableMethodSecurity(prePostEnabled = true)` — so `@PreAuthorize` on
  controllers actually binds.
- Password encoder: `BCryptPasswordEncoder(12)` — 05 Security T-02 strength.
- Session management: `IF_REQUIRED` (05 Security T-05); `maximumSessions(1)`
  prevents parallel sessions from the same account.
- CSRF: **enabled** for the whole surface **except** `/webhooks/jira` (which is
  authenticated by HMAC, not by cookie, and must accept Jira's POSTs).
  Implementation: `csrf.ignoringRequestMatchers("/webhooks/jira")`.

### 3.2 Authorisation matrix
```
permitAll:   /webhooks/jira
             /actuator/health, /actuator/info
             /api/auth/login, /api/auth/me, /api/auth/logout
             /, /index.html, /static/**, /favicon.ico (SPA shell)
hasRole(ADMIN):
             /actuator/prometheus, /actuator/metrics/**
authenticated (then method-level @PreAuthorize decides the role):
             everything else — /api/**
```
Why `/api/auth/me` is permitAll: it's the UI's "am I logged in?" probe. When
unauthenticated it returns `{"authenticated": false}`; when authenticated it
returns the member identity payload.

### 3.3 Form login / logout
- `formLogin()`
  - `loginProcessingUrl=/api/auth/login`
  - `usernameParameter=email`
  - `passwordParameter=password`
  - `successHandler` → 200 + MeView JSON
  - `failureHandler` → 401 + RFC 7807 ProblemDetail (type `about:blank`,
    title `Invalid credentials`)
- `logout()`
  - `logoutUrl=/api/auth/logout`, `logoutSuccessHandler` → 204, invalidates
    session + deletes JSESSIONID.

### 3.4 `HubUserDetails` + `HubUserDetailsService`
`UserDetailsService` loads by email through `MemberRepository.findByEmail`.
`HubUserDetails` is a thin wrapper carrying the `memberId` so the SpEL
`principal.memberId` resolves without hitting the DB again. Authorities:
`ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_MEMBER` from the member's `role` enum.

### 3.5 Deferred to v1.0 (noted in SecurityConfig javadoc)
- **SV-06 login lockout** (5 failed attempts → 30 min lockout) — the counters
  need Redis-or-similar state and the operator's dev host currently has no
  Redis; left as v1.0 so M12 smoke doesn't block on it. The javadoc links the
  Open-Item back to 05 Security for the next prioritisation pass.

## 4. PLAN-029 — `@PreAuthorize` + MEMBER data filter (M8)

Class-level `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` added to the five
admin-only controllers introduced in M1:
- `MemberController`
- `CapacityController`
- `HolidayController`
- `AllowListController`
- `SkillController`

Workload + Overdue use the "self-or-admin/manager" SpEL above. The data filter
is enforced in the service via the SpEL check and in the view by projecting only
the self-row; there is no SQL-level row filter needed because MEMBER cannot
address another member's URL once SpEL rejects the call.

## 5. PLAN-030 — Bootstrap admin runner (M8)

`com.mbs.hub.security.BootstrapAdminRunner implements CommandLineRunner`:
1. Idempotent: if `memberRepository.count() > 0` skip entirely — the runner
   runs on every boot but writes exactly once.
2. Reads `${hub.bootstrap.admin-email}` from env; abort with a WARN log if
   unset (dev host boots without an admin and the operator sets it later via
   `/api/members`).
3. Generates an 18-byte random password via `SecureRandom`, Base64-URL-encodes
   it to 24 chars.
4. BCrypt-12-hashes via the shared `PasswordEncoder` bean (T-02).
5. Inserts a member with `role=ADMIN`, `active=true`,
   `jiraAccountId="bootstrap-admin"` (placeholder; the operator replaces it
   once Jira is live).
6. Logs the plaintext ONCE on first boot with a `=== BOOTSTRAP ADMIN ===`
   banner so the operator can grab it from the container logs:

```
=== BOOTSTRAP ADMIN =============================================
 email    = <configured email>
 password = <generated 24-char password>
 ROTATE   via POST /api/auth/change-password after first login
=================================================================
```

Operator action after first boot: log in once at `/api/auth/login`, hit
`/api/auth/change-password` (PLAN-028.5, planned for a later minor cycle but
not blocking M8), and continue.

## 6. Files added in cycle 6

| Path | PLAN step |
|---|---|
| `app/src/main/java/com/mbs/hub/workload/dto/WorkloadView.java` | PLAN-020 (02 API §5.2 DTO) |
| `app/src/main/java/com/mbs/hub/workload/WorkloadService.java` | PLAN-020 (B-RULE-03 + DEC-010) |
| `app/src/main/java/com/mbs/hub/workload/WorkloadController.java` | PLAN-020 |
| `app/src/main/java/com/mbs/hub/overdue/dto/OverdueView.java` | PLAN-021 |
| `app/src/main/java/com/mbs/hub/overdue/dto/WarningView.java` | PLAN-021 |
| `app/src/main/java/com/mbs/hub/overdue/OverdueService.java` | PLAN-021 |
| `app/src/main/java/com/mbs/hub/overdue/OverdueController.java` | PLAN-021 |
| `app/src/main/java/com/mbs/hub/security/SecurityConfig.java` | PLAN-028 |
| `app/src/main/java/com/mbs/hub/security/HubUserDetails.java` | PLAN-028 |
| `app/src/main/java/com/mbs/hub/security/HubUserDetailsService.java` | PLAN-028 |
| `app/src/main/java/com/mbs/hub/security/AuthController.java` | PLAN-028 (`/api/auth/me`) |
| `app/src/main/java/com/mbs/hub/security/dto/MeView.java` | PLAN-028 |
| `app/src/main/java/com/mbs/hub/security/BootstrapAdminRunner.java` | PLAN-030 |
| `05-DEVELOPMENT/CYCLE_6_M5_M8_REPORT.md` | this report |

## 7. Files touched

| Path | Change |
|---|---|
| `MemberController.java` | class-level `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` |
| `CapacityController.java` | class-level `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` |
| `HolidayController.java` | class-level `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` |
| `AllowListController.java` | class-level `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` |
| `SkillController.java` | class-level `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` |

659 lines of production Java added (M5+M8 together).

## 8. Not in this cycle

- **No login lockout (SV-06)** — deferred to v1.0 (see §3.5).
- **No change-password endpoint** — needed in a minor follow-up (not blocking M12).
- **No SPA screens** — M10 (PLAN-032/033).
- **No audit instrumentation on security events** — M9 PLAN-031 wires auth-failure
  and admin-action audits once the Kafka `hub.audit` topic surface stabilises.
- **No tests inside the container** — the Maven Central block on the Claude egress
  proxy still prevents running `./gradlew test` here; the operator will run them
  on their dev host as part of PLAN-036 smoke.

## 9. Baselines honoured in this cycle

| Baseline | Where |
|---|---|
| DEC-004 (3-tier capacity) | `WorkloadService` calls `CapacityResolver.resolve(member)` |
| DEC-005 (allow-list + self-filter) | `@PreAuthorize("… principal.memberId ==")` on workload/overdue |
| DEC-010 (Hard Deadline criteria) | `WorkloadService.hardDeadlineCriteria()` lists which of {`fix_version_release`, `priority_blocker`, `hub_lock_flag`} fired |
| DEC-011 (AR bands) | `AllocationBand.of(ar)` on the stored row — no recomputation in the view |
| B-RULE-02 (no estimate = 0.5 MD) | `UnestimatedSummary` surfaces the bucket |
| B-RULE-03 (overload = >40h/wk or daily cap with cannot_reschedule) | `WorkloadService.computeOverload(...)` |
| F-01 (Overdue bands) | `OverdueView.overdueBand` echoes the stored band |
| T-02 (BCrypt-12) | `SecurityConfig.passwordEncoder()` |
| T-05 (session IF_REQUIRED + single session) | `SecurityConfig.sessionManagement()` |

## 10. Totals

**32 of 36 PLAN steps materialised.** M0 + M1 + M2 + M4 + M5 + M8 done.
M3 is rolled into M2 by the plan.

Pending (code only — no HARD GATE other than CP-1 still open on Jira live):
- **M6**: PLAN-022 daily report job, PLAN-023 ETA (α over last 10 WD).
- **M7**: PLAN-024 pipeline CRUD controller (entity + repo already live), PLAN-025
  balancing suggestion, PLAN-026 Jira write client, PLAN-027 assignment service.
  — PLAN-026/027 are the first steps that will actually CALL Jira; CP-1 is a
  hard pre-requisite for them.
- **M9**: PLAN-031 audit instrumentation.
- **M10**: PLAN-032 SPA scaffold, PLAN-033 SPA screens.
- **M11**: PLAN-034 Micrometer/logs/secret-masking glue, PLAN-035 runbook finalisation.
- **M12**: PLAN-036 E2E smoke on dev host.

CP-1 is still the only human gate between the current state and a live Jira
integration; it stops M7 code from being safely smoke-tested but does NOT stop
M6 or M9–M12.

---
⛔ **STOP** — do not self-advance. Next move: either sign CP-1 (operator), or
pick one of:
- Continue M6 (daily report + ETA — pure Kafka/DB, no Jira call).
- Continue M9 (audit instrumentation — can land now).
- Continue M10 (SPA scaffold — pure frontend; depends only on current `/api/*`).
- Continue M11 (observability + runbook glue).
- Something else.
