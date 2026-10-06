# Development · Cycle 5 — M4 Materialized-view refresher

| Field | Value |
|---|---|
| Date | 2026-10-06 15:58 ICT |
| Scope | PLAN-018 (refresher + debouncer) + PLAN-019 (nightly rebuild) |
| HARD GATE | PLAN APPROVED carried · CP-1 still not signed (not required for this cycle) |
| **Status** | **🟢 IMPLEMENTATION COMPLETE for M4** |

> Same posture as cycle 4: code lands, nothing calls Jira. The refresher reads the
> projection tables that `JiraEventConsumer` populates — if Jira never fires a webhook
> and the operator never ingests any issue, the refresher just produces an empty
> heatmap and empty overdue list. That is correct behaviour, not a bug.

---

## 1. Architecture note — materialized views pivoted to plain tables

The V004 shells created in M1 (`WHERE FALSE` materialized views) could not express the
real derivation: DEC-004 three-tier capacity resolution + DEC-003 holiday-aware
working-day counting + DEC-011 band thresholds + DEC-009 TPR require application logic
the SQL view definition cannot cleanly carry.

The TD/Design always said "the Hub refresher populates these". This cycle makes that
explicit: V006 drops the two empty MVs and recreates them as **plain tables** with the
same schema (`mv.mv_allocation_rate`, `mv.mv_member_overdue`) and the same unique PKs
that the API queries use. The refresher is pure Java — predictable, debuggable, testable.

Design impact: F-DB-03 "unique index for REFRESH CONCURRENTLY" is replaced by the
primary key (same guarantee, cleaner shape). No downstream contract changes — the
`/api/heatmap` and `/api/overdue*` endpoints queried in M5+ still read from
`mv.mv_allocation_rate` and `mv.mv_member_overdue`.

**Verified in the container:** fresh database `V001 → V002 → V003 → V004 → V005 → V006`
applies clean on PostgreSQL 16.15. `mv.*` now contains two BASE TABLE rows with
primary keys `mv_allocation_rate_pkey` and `mv_member_overdue_pkey`.

## 2. PLAN-018 — Event-driven debounced refresher

### 2.1 Internal event
`com.mbs.hub.sync.ProjectionUpdatedEvent(issueKey, projectKey)` — a plain record
published by the consumer after a successful UPSERT (not stale, not deduped).

### 2.2 Consumer wiring
`JiraEventConsumer.applyUpsert()` now returns the native query's row count. On
`rows > 0` the consumer publishes `ProjectionUpdatedEvent` through
`ApplicationEventPublisher`. On `rows == 0` (stale, monotonic guard blocked it) the
`jira_event_stale_dropped_total` metric increments and no event is published.

### 2.3 Refresher — `com.mbs.hub.mv.MaterializedViewRefresher`
- `@TransactionalEventListener` + `@EventListener` both subscribed — the former runs
  AFTER_COMMIT so a half-rolled-back transaction can never trigger a refresh.
- A single-thread `ScheduledExecutorService` holds exactly one pending refresh via
  `AtomicReference<ScheduledFuture>`. A new event replaces the pending future (cancel +
  reschedule), coalescing bursts of events into one refresh pass.
- Debounce window: **2 s** (03 DB §6 / TD §6).
- Metrics: `mv_refresh_total` (counter) and `mv_refresh_duration_seconds` (timer).

### 2.4 `refreshAll()`
For every `Member` in the roster:
- Recompute 3 rows into `mv.mv_allocation_rate` — one per `Horizon` value — and `save()`.
- Replace the `mv.mv_member_overdue` rows for that member (DELETE then INSERT the new set).

The whole pass runs inside `@Transactional`, so either all rows refresh or none do.

## 3. PLAN-019 — Nightly full rebuild

`@Scheduled(cron = "0 30 2 * * *", zone = "Asia/Saigon")` on `MaterializedViewRefresher`
calls `refreshAll()`. The nightly path is the safety net against any lost update event
or clock skew; a missed night does nothing worse than keep the heatmap stale by one day.

## 4. Business-logic details

### 4.1 Horizon → window
Horizons supported: `this_week`, `next_2_weeks`, `next_month`.

- `this_week.windowStart(today) = today.with(ChronoField.DAY_OF_WEEK, 1)` → Monday.
- `next_2_weeks` extends two weeks from that Monday.
- `next_month.windowStart(today) = today.withDayOfMonth(1)`; end = +1 month.

### 4.2 Working-day counter — `WorkingDayCalculator`
Implements B-RULE-01 exactly:
- Default working day: NOT Sat/Sun.
- `COMPENSATED_WORKDAY` overrides a weekend to count.
- `NATIONAL` and `COMPANY_DAY` exclude the date regardless of weekday.

Verified by `WorkingDayCalculatorTest` — 4 cases: plain week=5 WD; national mid-week=4;
compensated Saturday raises Mon–Sat week to 6; company day excludes.

### 4.3 Allocation Rate — `AllocationRateCalculator`
Per member × horizon:
- `standard_h = dailyHours × workingDays(window)`
- `committed_h = Σ remainingEstimateHours(active issues assigned to member on allow-list)`
  — issues without an estimate contribute 4 h = 0.5 MD (B-RULE-02).
- Convert to MD using the member's own dailyHours (so 1 MD = member's own standard day,
  consistent with 04 Domain §A "Man-day").
- `available_md = standard_md − committed_md` (two decimal places half-up).
- `allocation_rate = committed_md / standard_md × 100%`; when standard_md = 0 (e.g.
  holiday-only horizon) default to 100% so the band flags the schedule.
- Band via `AllocationBand.of(arPercent)` using DEC-011 thresholds verbatim.

### 4.4 Overdue + Early-Warning — `OverdueCalculator`
Per active issue of the member (allow-list OK, status != done, due_date set):
- **Overdue** when `due_date < today`:
    - `days_overdue = today − due_date` (calendar days).
    - `overdue_band` per F-01 (1_to_3 / 4_to_7 / more_than_a_week).
    - `tpr_band = none`.
- **Not overdue** → compute TPR per DEC-009:
    - `RWD = workingDays(today, due+1)` (inclusive of due day).
    - `RDC = remaining_h / (RWD × dailyHours)` — HALF_UP to 3 d.p.
    - When `RWD ≤ 0` → treat as RED (AC-003.5 edge-case).
    - `tpr_band = TprBand.of(rdc)`. Only yellow/red rows are stored; `none` rows are
      filtered out so the overdue table does not drown in healthy issues.

Tests for the enum band thresholds (DEC-009 TPR, DEC-011 AR, F-01 overdue) are added so
the thresholds are locked at the type level — any accidental refactor that moves a
boundary one tick trips a unit test.

## 5. Files added in cycle 5

| Path | PLAN step |
|---|---|
| `app/src/main/resources/db/migration/V006__mv_plain_tables.sql` | PLAN-018 schema pivot |
| `app/src/main/java/com/mbs/hub/mv/Horizon.java` | PLAN-018 |
| `app/src/main/java/com/mbs/hub/mv/AllocationBand.java` | DEC-011 bands |
| `app/src/main/java/com/mbs/hub/mv/OverdueBand.java` | F-01 bands |
| `app/src/main/java/com/mbs/hub/mv/TprBand.java` | DEC-009 bands |
| `app/src/main/java/com/mbs/hub/mv/AllocationRateKey.java` + `AllocationRateRow.java` + `AllocationRateRepository.java` | PLAN-018 |
| `app/src/main/java/com/mbs/hub/mv/MemberOverdueKey.java` + `MemberOverdueRow.java` + `MemberOverdueRepository.java` | PLAN-018 |
| `app/src/main/java/com/mbs/hub/mv/calc/WorkingDayCalculator.java` | B-RULE-01 |
| `app/src/main/java/com/mbs/hub/mv/calc/AllocationRateCalculator.java` | DEC-011 + B-RULE-02 |
| `app/src/main/java/com/mbs/hub/mv/calc/OverdueCalculator.java` | DEC-009 + F-01 |
| `app/src/main/java/com/mbs/hub/mv/MaterializedViewRefresher.java` | PLAN-018 + PLAN-019 |
| `app/src/main/java/com/mbs/hub/sync/ProjectionUpdatedEvent.java` | internal event |
| `app/src/main/java/com/mbs/hub/sync/consumer/JiraEventConsumer.java` | rewired to publish event |
| `app/src/test/java/com/mbs/hub/mv/AllocationBandTest.java` | DEC-011 |
| `app/src/test/java/com/mbs/hub/mv/OverdueBandTest.java` | F-01 |
| `app/src/test/java/com/mbs/hub/mv/TprBandTest.java` | DEC-009 |
| `app/src/test/java/com/mbs/hub/mv/calc/WorkingDayCalculatorTest.java` | B-RULE-01 |
| `05-DEVELOPMENT/CYCLE_5_M4_REPORT.md` | this report |

## 6. Not in this cycle

- **No `/api/heatmap`, `/api/overdue*` controllers** — those live in M5 (PLAN-020,
  PLAN-021). The data they will serve is now computable, so M5 is unblocked.
- **No per-issue targeted refresh** — the refresher recomputes all members on every
  event. At 10–50 members this costs ~hundreds of microseconds per member; the metric
  `mv_refresh_duration_seconds` will tell us when (if ever) to target.
- **No worklog projection ingestion** — see cycle 4 notes; still bound to PLAN-022
  (daily report).

## 7. Totals

**27 of 36 PLAN steps materialised.** M0 + M1 + M2 + M4 done. M3 (ingestion) is rolled
into M2 by the plan.

Pending:
- M5 PLAN-020 workload / overload view, PLAN-021 overdue + early-warning view.
- M6 PLAN-022 daily report, PLAN-023 ETA (α over last 10 WD).
- M7 PLAN-024 pipeline CRUD controller (entity + repo already live), PLAN-025 balancing
  suggestion, PLAN-026 Jira write client, PLAN-027 assignment service.
- M8 PLAN-028 Spring Security chain, PLAN-029 `@PreAuthorize` + MEMBER data filter,
  PLAN-030 bootstrap-admin runner.
- M9 PLAN-031 audit instrumentation.
- M10 PLAN-032 SPA scaffold, PLAN-033 SPA screens.
- M11 PLAN-034 Micrometer/logs/secret-masking (metrics counters are already in place —
  this step glues them to the final logback config), PLAN-035 runbook (already partly
  written).
- M12 PLAN-036 E2E smoke on dev host.

CP-1 remains the only human gate between the current state and a live Jira integration;
it is NOT a code gate.

---
⛔ **STOP** — do not self-advance. Next move: either sign CP-1 (operator), or ask me to
continue M5 (workload + overdue API controllers) in the same posture.
