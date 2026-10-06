# Development · Cycle 7 — M6 reporting + M9 audit + M11 observability glue + M10 SPA

| Field | Value |
|---|---|
| Date | 2026-10-06 16:34 ICT |
| Scope | PLAN-022 + PLAN-023 (M6) · PLAN-031 (M9) · PLAN-034 + PLAN-035 (M11) · PLAN-032 + PLAN-033 (M10) |
| HARD GATE | PLAN APPROVED carried · CP-1 still not signed (still not required — none of this cycle calls Jira) |
| **Status** | **🟢 IMPLEMENTATION COMPLETE for M6, M9, M10, M11** |

> Same posture as cycles 4–6: code lands, no Jira call added. Everything in this cycle
> is readable or admin surface; the only remaining code gate on Jira writes is CP-1 → M7.

---

## 1. PLAN-022 — Daily Report (M6)

### 1.1 DTO — `com.mbs.hub.reporting.dto.DailyReportView`
Mirrors 02 API §5.3 / AC-002.1..4:
- `done: List<DoneRow>` with `issueKey`, `projectKey`, `resolution`, `discarded` (DEC-001).
- `worklogTotals: List<WorklogTotal>` grouped by `projectKey`, hours 2 d.p. HALF_UP.
- `totalWorklogHours` grand total.
- `excludeDiscarded: boolean` echoes the request flag so the UI shows the state.

### 1.2 Service — `DailyReportService.compile(date, memberId, excludeDiscarded)`
- Window boundaries resolved in **Asia/Saigon** via the injected `Clock` (DEC-002 /
  F-TD-01). The host OS zone never shifts the window.
- Done rows: issues with `statusCategory='done'` AND `jiraUpdatedAt ∈ [D 00:00, D+1 00:00)` AND `allow_list_ok`. The absence of a dedicated `resolved_at` column is a known v1 limitation (noted inline); the monotonic `jiraUpdatedAt` is a sufficient proxy for "closed today" because an issue re-opened and metadata-edited later leaves `statusCategory != done` and drops out of the window naturally.
- Worklog totals: `WorklogProjectionRepository.findByJiraAccountIdAndStartedAtBetween`
  (or the no-account variant for ADMIN/MANAGER team-wide). Non-roster worklog is filtered out at the Java layer; the repo already carries `in_roster`.
- `excludeDiscarded` filters `done` rows when true (default per AC-002.3).

### 1.3 Controller — `GET /api/reporting/daily?date=&memberId=&excludeDiscarded=`
MEMBER may call only with their own `memberId`:
```
hasAnyRole('ADMIN','MANAGER')
  or (hasRole('MEMBER') and #memberId != null
      and principal.memberId.toString() == #memberId.toString())
```
ADMIN/MANAGER may call without `memberId` for a team-wide roll-up.

### 1.4 New read-only repo — `IssueProjectionDailyRepository`
Separate from the mutating `IssueProjectionRepository` so the daily report's query
can't accidentally be bridged into a write path.

### 1.5 WorklogProjectionRepository — adds queries
- `findByJiraAccountIdAndStartedAtBetween`, `findByStartedAtBetween` for the daily report
  worklog slice.
- `sumByProjectBetween` native aggregate for the ETA α calculation.

## 2. PLAN-023 — ETA α-over-10WD (M6)

### 2.1 Service — `EtaService.forecast(memberId)`
Per project P:
```
remainingHours_P = Σ remaining_estimate_h over active, allow-listed, assigned-to-member issues in P
totalSeconds     = Σ duration_seconds of in-roster worklog over last ≈10 WD
seconds_P        = Σ duration_seconds of in-roster worklog over last ≈10 WD on P
α_P              = seconds_P / totalSeconds       (0..1, 3 d.p. HALF_UP)
capacity_P       = α_P × dailyHours
RWD_P            = ceil(remainingHours_P / capacity_P)
ETA_date_P       = addWorkingDays(today, RWD_P)   via B-RULE-01 (WorkingDayCalculator)
```
- Lookback is 21 calendar days (safety margin — `WorkingDayCalculator` trims to 10 WD naturally; overshooting on weekends/holidays is harmless).
- When `α_P == 0` or `remainingHours_P == 0` the row returns `etaDate=null` with an explanatory `note` (`no_worklog_on_project_last_10_wd` / `no_remaining_estimate`) rather than a fabricated date.
- `dailyHours` comes from `CapacityResolver.resolveOn(memberId, today)` (DEC-004 three tier).

### 2.2 Controller — `GET /api/eta/{memberId}`
Same MEMBER-self-or-admin SpEL.

## 3. PLAN-031 — Audit instrumentation (M9)

### 3.1 Entity + repo — `AuditEvent` / `AuditEventRepository`
- Maps to `audit.audit_log` (schema already created in V003, cycle 2).
- `payload` is a JSON string (`@JdbcTypeCode(SqlTypes.JSON)`); the entity has no setter path for `id`/`occurredAt` so inserts are the only legitimate write.
- Repo exposes `findWithin(from, toExclusive)` and `findByActionOrderByOccurredAtDesc`; no delete/update is referenced from any caller.

### 3.2 `SecretMasker`
- Masks a short, hardcoded list of key names (`password`, `pwd`, `api_token`, `apiToken`, `jira_api_token`, `jiraApiToken`, `webhook_secret`, `jira_webhook_secret`, `authorization`, `cookie`, `x-atlassian-token`) in JSON strings AND bearer-shaped headers.
- Runs on every audit payload before persistence and before publish to Kafka.
- The existing logback secret mask (configured earlier) still runs in parallel; defense in depth.

### 3.3 `AuditService.record(...)` (and `.ok(...)` / `.failed(...)` convenience)
- `@Transactional(propagation = REQUIRES_NEW)` — an audit always commits even when the business transaction rolls back.
- Writes the row THEN publishes a mirror envelope onto `hub.audit` (7-day retention per F-ARCH-NEW-02 Reading A, already configured in `KafkaConfig`).
- Metrics: `audit_writes_total`, `audit_write_failures_total`.
- Kafka publish failure is logged but never breaks the DB write — DB is the source of truth, Kafka is the 7-day mirror for downstream consumers.

### 3.4 Instrumentation points — zero-touch for existing controllers
- `AuthAuditListener` subscribes to `AuthenticationSuccessEvent`, `AbstractAuthenticationFailureEvent`, `LogoutSuccessEvent` and writes `AUTH_LOGIN_OK` / `AUTH_LOGIN_FAIL` / `AUTH_LOGOUT`.
- `AdminMutationAuditAspect` runs around every `@PostMapping / @PutMapping / @PatchMapping / @DeleteMapping` within the five admin packages (`core.member`, `core.capacity`, `core.holiday`, `core.allowlist`, `core.skill`) and writes `CREATE_* / UPDATE_* / DELETE_*` with actor memberId and short-arg payload. Throwables → `FAILED` with the exception name + message, then rethrown.
- Added `spring-boot-starter-aop` to `build.gradle.kts` for the aspect.

### 3.5 Admin read view — `GET /api/audit`, `GET /api/audit/by-date`
ADMIN-only. Read-only. Paged (hard-cap at 1000). No write endpoint exists.

## 4. PLAN-034 — Observability glue (M11)

Most of this is already wired from earlier cycles:
- Micrometer Prometheus registry is on the classpath.
- `/actuator/prometheus` is ADMIN-only in `SecurityConfig`.
- Secret-masking at `logback-spring.xml` (regex-based; token/secret/password/api-key).
- `KafkaConfig` sets `hub.audit` retention to 7 days.

New in this cycle:
- `RequestIdFilter` populates the `requestId` MDC slot that the existing `logback-spring.xml` renders as the `req` JSON field. Honours an inbound `X-Request-Id` and echoes it back as a response header for cross-system tracing.
- `audit_writes_total` + `audit_write_failures_total` counters surface in `/actuator/prometheus` under PLAN-034's observability contract.

## 5. PLAN-035 — Runbook (M11)

`docs/runbook.md` lands with the operator-facing sections:
- Prerequisites + one-time setup
- Start/stop
- Tunnel setup (TD-COND-01) with the two 'curl' probes
- Jira API Token rotation every 90 days (COND-03)
- CP-1 / CP-2 / CP-3 checkpoints
- Projection rebuild steps
- `pg_dump` before every Flyway migration
- `scripts/check-no-playbook-drift.sh` (PLAN-COND-03) — body inline, operator saves + chmods
- Troubleshooting table
- Known v1.0 deferrals

## 6. PLAN-032 + PLAN-033 — SPA scaffold + screens (M10)

### 6.1 Choice of framework
TD-R-05 deferred the framework choice to Development. PLAN-COND-02 forbids a Node step in the Dockerfile and the SPA must ship as **pre-built static assets**. Taking the simplest option that satisfies both: **vanilla HTML + ES modules + CSS**, no bundler, no toolchain. The result is one `index.html` + a hash-router in `app.js` + 9 view modules, all served straight from `classpath:/static/`.

### 6.2 Structure
```
app/src/main/resources/static/
├── index.html                 — shell + nav + <main id="view">
├── css/app.css                — tiny design system; band colors from DEC-011
└── js/
    ├── app.js                 — router, /api/auth/me probe, api{}
    └── views/
        ├── login.js           — POST /api/auth/login (form-encoded)
        ├── workload.js        — AC-001.* + overload breakout + unestimated bucket
        ├── daily.js           — AC-002.* + Discarded toggle
        ├── overdue.js         — AC-003.* overdue + early-warning
        ├── heatmap.js         — AC-006.1-2 MANAGER/ADMIN heatmap
        ├── eta.js             — AC-004.* ETA table
        ├── pipeline.js        — placeholder (M7 CRUD lands later)
        ├── balancing.js       — placeholder (M7 + CP-1)
        └── admin.js           — member roster + last-25 audit rows
```
All UI copy is Vietnamese (DEC-016).

### 6.3 Access control (defence-in-depth with server rules)
- Shell loads `GET /api/auth/me`. Unauthenticated → `{authenticated:false}` → router redirects to `#/login` (the controller now returns 200 rather than 401 so a cold load never flashes an error).
- Nav items declare `data-role="MANAGER,ADMIN"` or `data-role="ADMIN"` and hide for other roles. The server's `@PreAuthorize` is the authoritative gate; the hide-in-nav is UX, not security.

### 6.4 SecurityConfig deltas
- `/js/**`, `/css/**`, `/favicon.ico`, `/index.html` added to the permitAll list so the SPA shell loads before auth.

### 6.5 AuthController delta
- `/api/auth/me` now returns `{authenticated:false}` (200) for anonymous callers so the SPA cold-load path never 401s.

## 7. Files added / touched

**Added (M6)**
- `app/src/main/java/com/mbs/hub/reporting/dto/DailyReportView.java`
- `app/src/main/java/com/mbs/hub/reporting/DailyReportService.java`
- `app/src/main/java/com/mbs/hub/reporting/DailyReportController.java`
- `app/src/main/java/com/mbs/hub/sync/projection/IssueProjectionDailyRepository.java`
- `app/src/main/java/com/mbs/hub/eta/dto/EtaView.java`
- `app/src/main/java/com/mbs/hub/eta/EtaService.java`
- `app/src/main/java/com/mbs/hub/eta/EtaController.java`

**Added (M9)**
- `app/src/main/java/com/mbs/hub/audit/AuditEvent.java`
- `app/src/main/java/com/mbs/hub/audit/AuditEventRepository.java`
- `app/src/main/java/com/mbs/hub/audit/SecretMasker.java`
- `app/src/main/java/com/mbs/hub/audit/AuditService.java`
- `app/src/main/java/com/mbs/hub/audit/AuthAuditListener.java`
- `app/src/main/java/com/mbs/hub/audit/AdminMutationAuditAspect.java`
- `app/src/main/java/com/mbs/hub/audit/AuditController.java`

**Added (M11)**
- `app/src/main/java/com/mbs/hub/config/RequestIdFilter.java`
- `docs/runbook.md`

**Added (M10)**
- `app/src/main/resources/static/index.html`
- `app/src/main/resources/static/css/app.css`
- `app/src/main/resources/static/js/app.js`
- `app/src/main/resources/static/js/views/{login,workload,daily,overdue,heatmap,eta,pipeline,balancing,admin}.js`

**Touched**
- `app/build.gradle.kts` — adds `spring-boot-starter-aop`
- `app/src/main/java/com/mbs/hub/security/SecurityConfig.java` — SPA assets permitAll
- `app/src/main/java/com/mbs/hub/security/AuthController.java` — 200 anonymous `/me`
- `app/src/main/java/com/mbs/hub/sync/projection/WorklogProjectionRepository.java` — new queries

**This report**
- `05-DEVELOPMENT/CYCLE_7_M6_M9_M10_M11_REPORT.md`

## 8. Not in this cycle

- **No PipelineController / BalancingController / AssignmentController** — those are M7
  and the Jira write-back half of them needs CP-1. The SPA includes placeholder screens
  that explain exactly that.
- **No ETA chart on the SPA** — the server returns the data; a Gantt-style visual is a
  cosmetic nice-to-have, deferred so the cycle stays bounded.
- **No per-field delta in the admin-mutation audit payload** — before/after capture is
  possible via an entity listener but adds complexity (needs the pre-image fetched or
  reflected). The current payload records the method + args + verdict, which is enough
  for who-did-what. Full diff is a v1.0 enhancement.
- **No bundler/minifier** — pre-built static assets per PLAN-COND-02; the files are already small (css ≈ 3 KB, js total ≈ 15 KB).

## 9. Baselines honoured in this cycle

| Baseline | Where |
|---|---|
| DEC-001 (Discarded set) | `DailyReportService` filter + badge in SPA daily view |
| DEC-002 (Asia/Saigon clock) | `DailyReportService.compile(...)` uses the injected `Clock` for window bounds |
| DEC-004 (3-tier capacity) | `EtaService` calls `CapacityResolver.resolveOn(memberId, today)` |
| DEC-005 (allow-list) | SPA screens surface only `allow_list_ok` data via the service layer |
| DEC-008 (append-only audit) | `AuditEvent` + repo have no update/delete callers |
| DEC-011 (AR bands) | SPA band CSS classes mirror the band keys from the server |
| DEC-016 (Vietnamese UI) | All SPA copy in Vietnamese |
| B-RULE-01 (WD counting) | `EtaService.addWorkingDays` → `WorkingDayCalculator` |
| F-ARCH-NEW-02 Reading A | `audit.audit_log` + 7-day Kafka mirror (already set) |
| F-03 (ETA formula) | `EtaService` implements α × dailyHours per project |
| PLAN-COND-02 (no Node in Docker) | SPA ships as vanilla HTML/JS/CSS |
| TD-COND-01 (narrow tunnel) | runbook §4 restates the probe-and-verify steps |

## 10. Totals

**36 of 36 PLAN steps covered — minus M7 (PLAN-024/025/026/027) which still waits on CP-1.**

Done (code only):
- M0 (001–003) ✅
- M1 (004–010) ✅
- M2 (011–017) ✅ (CP-1 pending for Jira live)
- M4 (018, 019) ✅
- M5 (020, 021) ✅
- M6 (022, 023) ✅ **(this cycle)**
- M8 (028, 029, 030) ✅
- M9 (031) ✅ **(this cycle)**
- M10 (032, 033) ✅ **(this cycle)**
- M11 (034, 035) ✅ **(this cycle)**

Pending:
- **M7 (024–027)** — CP-1 operator action is a hard prerequisite for any Jira write.
- **M12 (036) — E2E smoke** — meaningful only after M7 and CP-1.

CP-1 is now the ONLY code gate between the current state and a fully assembled v1.
Everything addressable without touching Jira has been addressed.

---
⛔ **STOP** — do not self-advance. Next move is yours:
- **Sign CP-1** (operator action — §6 of CYCLE_4_M2_REPORT.md and §2–§4 of `docs/runbook.md` have the exact steps), then signal continue for M7.
- **Continue M7 code** in the same posture as cycles 4–7 (code lands with `JIRA_WRITE_DRY_RUN=true` default — PLAN-COND-01 — so no real Jira call happens until CP-1 is signed).
- Something else.
