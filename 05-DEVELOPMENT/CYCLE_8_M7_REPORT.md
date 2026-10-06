# Development · Cycle 8 — M7 Pipeline + Balancing + Jira write + Assignment

| Field | Value |
|---|---|
| Date | 2026-10-06 21:12 ICT |
| Scope | PLAN-024 + PLAN-025 + PLAN-026 + PLAN-027 (all of M7) |
| HARD GATE | PLAN APPROVED carried · **CP-1 credentials received** (JIRA_BASE_URL, JIRA_USER_EMAIL, JIRA_API_TOKEN) · `JIRA_WRITE_DRY_RUN=true` **kept as the committed default** per PLAN-COND-01 — operator flips to `false` only after smoke |
| **Status** | **🟢 IMPLEMENTATION COMPLETE for M7 (dry-run safe)** |

> Operator provided Jira credentials in-session. The code path for Jira writes is
> fully in place but SHORT-CIRCUITS inside `JiraWriteClient` whenever
> `hub.jira.write-dry-run=true` (the default). The audit record of the intended
> call is written in both branches, so smoke-testing produces a complete audit
> trail even before the first real HTTP write.

---

## 1. PLAN-024 — Pipeline CRUD (M7)

### 1.1 DTOs
- `PipelineRequest` — validated: `name` (1..200), `objective` (not blank), `targetDeadline` (not null), `totalEstimatedMd ≥ 0.01`, `requiredSkills` (non-empty, each with non-blank `l1`).
- `PipelineView` — includes `requiredSkills` as `[{name, l1, l2}]`, `createdBy`, `createdAt`, `updatedAt`.

### 1.2 `PipelineService`
- `list(state)` — filters by `PipelineState` when given, otherwise returns all; ordered by `targetDeadline` ASC via the existing repo method.
- `create` — validates each required skill against `core.skill_taxonomy` via `SkillTagRepository.existsById`; a skill not in the taxonomy is rejected with `IllegalArgumentException`. Writes the pipeline + the `pipeline_required_skill` rows under one `@Transactional`.
- `update` — same validation path; replaces skill set in one go (`deleteByKey_PipelineId` then re-insert).
- `delete` — removes skill rows then the pipeline (child first, respects FK).
- `transition` — `PATCH /{id}/state` to move through the F-05 lifecycle (DRAFT → READY_TO_ALLOCATE → ALLOCATED, etc.).

### 1.3 `PipelineController` — `/api/pipeline`
MANAGER / ADMIN only (class-level `@PreAuthorize`). Endpoints:
- `GET /api/pipeline?state=` → list.
- `GET /api/pipeline/{id}` → single.
- `POST /api/pipeline` → 201 + body.
- `PUT /api/pipeline/{id}` → 200 + body.
- `PATCH /api/pipeline/{id}/state?next=…` → 200 + body (state transition).
- `DELETE /api/pipeline/{id}` → 204.

## 2. PLAN-025 — Balancing suggestion (M7)

### 2.1 `BalancingService.suggestFor(pipelineId)` — B-RULE-04 / AC-006.3
Per every active roster member:
1. **Skill filter**: load `MemberSkill` rows; required-skills set must be a subset of the member's set. Fail → bump `rejectedBySkill` counter and skip.
2. **Capacity**: `dailyHours = CapacityResolver.resolveOn(member, today)` (DEC-004); `standardMd = workingDays(today, deadline+1)` via `WorkingDayCalculator` (B-RULE-01); `committedHours = Σ remaining_estimate_h` over the member's active, allow-listed issues whose `due_date` lies in `[today, deadline]`; `availableMd = standardMd − committedHours/dailyHours` (2 d.p. HALF_UP). B-RULE-02 fallback inside the sum: a `null` or `0` estimate counts as `4 h` (0.5 MD @ 8 h). Fail (`availableMd < required`) → bump `rejectedByCapacity` and skip.
3. **Pass** → add as `Candidate(memberId, displayName, availableMd, dailyHours, matchedSkills)`.

Sorted DESC by `availableMd`. Returns `BalancingView` with the window, requirements, candidates, and the two reject counters so the UI can show "9 inspected, 4 skill-rejected, 3 capacity-rejected, 2 candidates".

### 2.2 `BalancingController` — `GET /api/balancing/{pipelineId}` (MANAGER / ADMIN)
Read-only — the caller then POSTs the chosen assignment to `/api/assignments` (PLAN-027).

## 3. PLAN-026 — Jira write client (M7)

### 3.1 `JiraWriteClient`
- Two operations: `assignIssue(issueKey, accountId)` and `commentIssue(issueKey, adfDoc)`.
- Basic auth derived from `JiraProperties` (same as read client); `Content-Type: application/json`; `X-Atlassian-Token: no-check` to be explicit about XSRF non-exemption.
- **PLAN-COND-01 dry-run gate** — when `props.writeDryRun() == true`, each method:
  - increments `jira_writes_dry_run_total` and `jira_writes_attempted_total`,
  - logs the intended method + path (NOT the full body — body may contain user input),
  - returns `WriteOutcome.dryRun(method, path)` with `statusCode=0, dryRun=true`,
  - **does NOT dispatch any HTTP call**.
- On a real call, 429 → `JiraRateLimitException(retryAfter)` (parses `Retry-After` header, clamps to [1, 600] s). Other non-2xx → `JiraClientException(status, message)`. Metrics: `jira_writes_attempted_total`, `jira_writes_dry_run_total`, `jira_writes_ok_total`, `jira_writes_failed_total`, `jira_writes_rate_limited_total`.
- The comment body is wrapped as `{"body": <adfDoc>}` per Jira Cloud REST v3 — the caller passes the ADF document as a `Map<String,Object>`, not a stringified JSON (common pitfall).

### 3.2 `WriteOutcome`
Record `(method, path, statusCode, dryRun, message)`. `ok()` = dry-run OR 2xx.

## 4. PLAN-027 — Assignment service (M7)

### 4.1 `AssignmentService.assign(req, requestedBy)` — AC-006.4..6 / DEC-008 / CV-04
1. Resolve `targetMemberId` → `Member` → `jiraAccountId`. Inactive member → reject.
2. Look up previous assignee from `IssueProjection` (nullable).
3. Build the audit payload with `issueKey, targetMemberId, targetJiraAccountId, previousJiraAccountId, pipelineId, reason`.
4. `writeClient.assignIssue(...)` → short-circuits as DRY_RUN when the flag is on, else PUT /rest/api/3/issue/{key}/assignee.
5. If assignee call succeeds: build an ADF comment attributing the actor + reason + pipeline, call `writeClient.commentIssue(...)`. **Comment failure is logged but NOT rolled back** — the assignee transfer is already live in Jira (which is the SoR); the audit row records both outcomes so an operator can retry the comment manually if needed.
6. On success → `audit.ok("ASSIGNMENT", ...)`; on any `RuntimeException` → `audit.failed(...)` then rethrow.
7. Rate-limit (`JiraRateLimitException`) is NOT caught: the caller sees a 5xx mapped by Spring's default advice; the SPA shows the message and the user can retry.
8. The returned `AssignmentView` includes `dryRun` so the SPA can show "DRY_RUN — no HTTP call issued" instead of "Sent to Jira".

### 4.2 `AssignmentController` — `POST /api/assignments` (MANAGER / ADMIN)
Validates the body via Jakarta Bean Validation (`@Valid`); returns 201 + the `AssignmentView`. No GET surface — assignment history is read via `/api/audit?action=ASSIGNMENT`.

### 4.3 Why the service self-audits (not the aspect)
The `AdminMutationAuditAspect` wraps controllers in `core.*` and `core.pipeline.*`. The assignment flow is more interesting than one row — it audits both the assignee call and the comment call separately, with their statuses, and records the pre-image of the assignee for forensics. That's richer than a generic method-level wrap.

## 5. SPA updates

### 5.1 `pipeline.js` — real screen (was stub in cycle 7)
- Lists existing pipelines filterable by `state`.
- Vietnamese state badges (`DRAFT → Nháp`, `READY_TO_ALLOCATE → Sẵn sàng phân phối`, …).
- Inline "+ Thêm pipeline" form posts to `POST /api/pipeline`. Required-skills input accepts a comma-separated list of L1 keys (`BACKEND,FRONTEND`).
- Each row has a "Gợi ý phân phối →" link to the balancing screen.

### 5.2 `balancing.js` — real screen (was stub in cycle 7)
- Takes `?pipelineId=<uuid>`, calls `GET /api/balancing/{id}`, lists candidates sorted DESC by Available MD with matched skills.
- Inline "Phân phối…" form opens for each candidate, pre-filled with a reason tied to the pipeline. Submit calls `POST /api/assignments`.
- Response display differentiates DRY_RUN (`DRY_RUN — đã audit intent, không gọi Jira`) from a real call (`Đã gửi assignee + comment tới Jira cho XYZ-123`).

### 5.3 `AdminMutationAuditAspect` scope extension
Pipeline mutations now go through the aspect (`within(com.mbs.hub.core.pipeline..*)`). Assignment stays out — the service audits itself with richer payloads.

## 6. Files added

| Path | PLAN step |
|---|---|
| `app/src/main/java/com/mbs/hub/core/pipeline/dto/PipelineRequest.java` | PLAN-024 |
| `app/src/main/java/com/mbs/hub/core/pipeline/dto/PipelineView.java` | PLAN-024 |
| `app/src/main/java/com/mbs/hub/core/pipeline/PipelineService.java` | PLAN-024 |
| `app/src/main/java/com/mbs/hub/core/pipeline/PipelineController.java` | PLAN-024 |
| `app/src/main/java/com/mbs/hub/balancing/dto/BalancingView.java` | PLAN-025 |
| `app/src/main/java/com/mbs/hub/balancing/BalancingService.java` | PLAN-025 |
| `app/src/main/java/com/mbs/hub/balancing/BalancingController.java` | PLAN-025 |
| `app/src/main/java/com/mbs/hub/jira/write/JiraWriteClient.java` | PLAN-026 |
| `app/src/main/java/com/mbs/hub/jira/write/WriteOutcome.java` | PLAN-026 |
| `app/src/main/java/com/mbs/hub/assignment/dto/AssignmentRequest.java` | PLAN-027 |
| `app/src/main/java/com/mbs/hub/assignment/dto/AssignmentView.java` | PLAN-027 |
| `app/src/main/java/com/mbs/hub/assignment/AssignmentService.java` | PLAN-027 |
| `app/src/main/java/com/mbs/hub/assignment/AssignmentController.java` | PLAN-027 |
| `05-DEVELOPMENT/CYCLE_8_M7_REPORT.md` | this report |

## 7. Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/mbs/hub/audit/AdminMutationAuditAspect.java` | adds `core.pipeline..*` to scope |
| `app/src/main/resources/static/js/views/pipeline.js` | stub → real screen |
| `app/src/main/resources/static/js/views/balancing.js` | stub → real screen |

## 8. CP-1 state (operator credentials received 2026-10-06)

| Env var | Value provided | Persisted to |
|---|---|---|
| `JIRA_BASE_URL` | `https://weeklywtf.atlassian.net` | `deploy/.env` on dev host (operator file) |
| `JIRA_USER_EMAIL` | `txhoang.bk@gmail.com` | `deploy/.env` on dev host |
| `JIRA_API_TOKEN` | *(operator-supplied token, never committed; rotatable in Atlassian UI)* | `deploy/.env` on dev host |
| `JIRA_WEBHOOK_SECRET` | Generated this cycle (32-byte hex) | `deploy/.env` on dev host — must be entered identical in Jira UI |
| `JIRA_WRITE_DRY_RUN` | **true** | `deploy/.env` (PLAN-COND-01 default — flip only after smoke) |
| `HUB_PUBLIC_URL` | **operator TODO — placeholder** | `deploy/.env` (set once tunnel is up) |
| `HUB_BOOTSTRAP_ADMIN_EMAIL` | `txhoang.bk@gmail.com` | `deploy/.env` |

CP-1 is NOT yet fully signed — the tunnel URL and the webhook registration in Jira UI are still operator steps (see `docs/runbook.md` §4 and §6). Flipping `JIRA_WRITE_DRY_RUN=false` is the final CP-1 step and happens only after smoke.

## 9. Not in this cycle

- **No change-password endpoint** — orthogonal; still a minor follow-up.
- **No pipeline listing filter by skill** — only by state. Follow-up.
- **No multi-assignee / sprint-assignment** — M7 scope is one issue at a time.
- **No ADF rich text (mentions, links)** — simple paragraph is enough for the audit trail; richer ADF is a cosmetic follow-up.
- **No rollback of assignee on comment failure** — intentional (assignee is live in Jira SoR; the audit row carries both outcomes).

## 10. Baselines honoured

| Baseline | Where |
|---|---|
| PLAN-COND-01 (dry-run default) | `JiraWriteClient.*` short-circuits when `writeDryRun=true` |
| DEC-005 (allow-list) | `BalancingService` filters issues via `findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue` |
| DEC-007 (skill taxonomy) | `PipelineService.validateSkills` rejects unknown L1/L2 |
| DEC-008 (assignee + ADF comment) | `AssignmentService` orchestrates both calls |
| B-RULE-01 (WD counting) | `BalancingService` uses `WorkingDayCalculator` |
| B-RULE-02 (no-estimate = 0.5 MD) | `BalancingService.effectiveRemaining` returns 4 h when null/zero |
| B-RULE-04 (Available ≥ required) | `BalancingService.suggestFor` reject condition |
| F-05 (pipeline lifecycle) | `PipelineController.PATCH /{id}/state` moves through `PipelineState` |

## 11. Totals

**36 of 36 PLAN steps materialised.** Only M12 PLAN-036 (E2E smoke on dev host) remains, and that is executed BY the operator against a running stack.

| Milestone | Status |
|---|---|
| M0 (001–003) | ✅ |
| M1 (004–010) | ✅ |
| M2 (011–017) | ✅ (code done; CP-1 half-signed) |
| M4 (018, 019) | ✅ |
| M5 (020, 021) | ✅ |
| M6 (022, 023) | ✅ |
| M7 (024, 025, 026, 027) | ✅ **(this cycle, dry-run safe)** |
| M8 (028, 029, 030) | ✅ |
| M9 (031) | ✅ |
| M10 (032, 033) | ✅ |
| M11 (034, 035) | ✅ |
| M12 (036) | ⏸️ pending operator smoke on dev host |

---
⛔ **STOP** — do not self-advance. Operator next moves:

1. Install `deploy/.env` on the dev host (file `deploy/hub-env-filled.txt` was shipped — rename + chmod 600 per the instructions I sent).
2. Bring up a tunnel (`docs/runbook.md` §4), set `HUB_PUBLIC_URL` in `.env`, verify probes.
3. Register the Jira webhook pointing at `${HUB_PUBLIC_URL}/webhooks/jira` with the generated secret.
4. `docker compose -f deploy/docker-compose.yml up -d`; capture the bootstrap admin password from logs.
5. Smoke CP-2: look at `/actuator/prometheus` to confirm `jira_events_processed_total`, `mv_refresh_total`, `audit_writes_total` all tick.
6. Smoke PLAN-036 scenarios (PS-01..PS-08) with `JIRA_WRITE_DRY_RUN=true`.
7. If every scenario is green, flip `JIRA_WRITE_DRY_RUN=false` + restart Hub + re-run PS-04 (assignment path) once to confirm real writes land.
