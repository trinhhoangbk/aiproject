# 04 — TEST STRATEGY · Result (re-run)

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 04 Test Strategy |
| Date | 2026-10-06 10:00 ICT (re-run) |
| **Status** | **🟡 TEST STRATEGY COMPLETE WITH GAPS** |

---

## 1. Strategy Summary
JUnit 5 + Spring Boot Test running on dev host + Testcontainers (PG + Kafka) + WireMock for Jira. Three levels — Unit, Component/Integration, E2E. REQ-009 benchmark is a dev-host measurement (gap documented).

## 2. AC Coverage Matrix (compact)

| AC | Scenario | Level |
|---|---|---|
| AC-001.1 workload view | 2 allow-listed projects → per-project ratio | Int |
| AC-001.2 week overload | 42 h → Red week | Unit + Int |
| AC-001.3 day overload | 3× tomorrow × 16 h with Blocker → Red day | Int |
| AC-001.4 no-estimate | 0.5 MD placeholder, flagged | Unit |
| AC-001.5 capacity override | Member › Team › Global | Unit |
| AC-002.1 daily done | 23:59 ICT closes count for day D | Int |
| AC-002.2 worklog totals | per-project sums match | Int |
| AC-002.3 Discarded | Won't Fix/Dup/Invalid flagged, excludable | Int |
| AC-002.4 Member scope | 403 cross-member | Int |
| AC-003.1 overdue list | Due < today, not-done | Int |
| AC-003.2 severity bands | 1_to_3 / 4_to_7 / >week | Unit |
| AC-003.3 freshness ≤ 5 min | webhook → DB latency delta | E2E |
| AC-003.4 TPR bands | 0.5 none, 0.75 yellow, 1.5 red | Unit |
| AC-003.5 RWD=0 edge | treat as Red | Unit |
| AC-004.1 remaining per project | sum match | Unit |
| AC-004.2 ETA formula | α=0.5, h=8, remaining=40 → +10 WD | Unit |
| AC-005.1 pipeline create | 201 | Int |
| AC-005.2 pipeline reject invalid | 400 RFC 7807 | Int |
| AC-006.1 AR math | 4.5/5 → 90% | Unit |
| AC-006.2 heatmap bands | 50/70/95/110 → dark_green/light_green/yellow/red | Unit |
| AC-006.3 suggest members | 5 MD by day X → ≥5 Available_MD list | Int |
| AC-006.4 assign write-back | 204 happy path | Int |
| AC-006.5 assign rollback | 403 → 422 ProblemDetails + FAILED audit | Int |
| AC-006.6 audit entry | mutation → row with full payload | Int |
| AC-007.1 webhook ingest | signed → Kafka → DB delta < 5 min | E2E |
| AC-007.2 hourly reconcile | 2 pages → 2 issues in DB | E2E |
| AC-007.3 reconcile fallback | webhook silence 1 h → delta ingested | E2E |
| AC-008.1 Manager | sees all | Int |
| AC-008.2 Member self | peer → 403 | Int |
| AC-008.3 Member admin | pipeline → 403 | Int |
| AC-009.1 perf 2.5 s | 50 members × 2 000 issues × 90-day worklog | Benchmark (gap) |

## 3. Regression Strategy
Greenfield → no in-repo regression. External: Jira write only via WireMock in CI; a `scripts/check-no-playbook-drift.sh` ensures 00–04 playbook folders are untouched by build/migrate steps.

## 4. Risk-Based Scenarios (highlights)

| Risk | Scenario | Expected |
|---|---|---|
| TD-R-02 Tunnel instability | 30-min disconnect | Next reconcile fills gap |
| INT-R-02 Jira 429 | WireMock returns 429+Retry-After=60 | Sleep then retry; cadence doubles next tick (COND-04) |
| INT-R-03 worklog_deleted shape | with/without key context | Both parsed; deleted worklog removed |
| INV-01 non-roster user | unknown accountId worklog | `in_roster=false`, excluded from team-MD |
| T-01 webhook spoofing | wrong HMAC | 401 + metric |
| T-03 horizontal escalation | MEMBER queries peer | 403 |
| T-07 SQLi | fuzzed filter params | no SQL fragments in response |
| T-10 audit tampering | app role DELETE on audit_log | Permission denied |
| F-TD-01 clock TZ | host TZ=UTC | Hub still Asia/Saigon |
| F-DB-01 password_hash | fresh V001 | column present |
| F-DB-03 unique index | fresh V004 + REFRESH CONCURRENTLY | no error |

## 5. Test Levels
- Unit — JUnit 5 + Mockito.
- Component/Integration — `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` + Testcontainers + WireMock.
- E2E — Docker Compose + HTTP scenarios.
- Load — Gatling or k6 for AC-009.1 only.
- Security — OWASP ZAP baseline + unit/integration tests for 05 Security SV-*.

## 6. Data / Environment
50 members + 2 000 issues + 90-day worklog seeded via Flyway + WireMock fixtures. Clock bean fixed; tests override JVM TZ to prove independence.

## 7. Dependency Simulation
Jira REST → WireMock fixtures (gap: field-drift not caught). Jira webhooks → signed canned payloads. Kafka/PG → Testcontainers. Tunnel → not simulated; manual dev-host test.

## 8. Required Evidence
Per step: command run + test report HTML + Gatling/k6 report for benchmark + ZAP report + Docker logs for E2E. Archived under `app/build/reports/`, summarised in `docs/evidence-<date>.md`.

## 9. Existing Tests to Preserve
None (greenfield).

## 10. Gaps / Blockers
| ID | Gap |
|---|---|
| TEST-G-01 | AC-009.1 benchmark on dev host, not production hardware (accepted for v1.0). |
| TEST-G-02 | ZAP automated baseline doesn't cover business authz (covered by integration tests). |
| TEST-G-03 | Jira field drift not caught by WireMock (manual canary mitigation). |

No BLOCKED-level items.

## 11. Traceability
Covered inline in §2 and §4.

## 12. Status
```
TEST STRATEGY COMPLETE WITH GAPS
```

⛔ **STOP — stage boundary.** Next: `05_Deployment_Rollback_Plan.md`.
