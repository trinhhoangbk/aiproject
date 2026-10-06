# 06 — TECHNICAL DESIGN REVIEW · Result

| Field | Value |
|---|---|
| Stage | 03-TECHNICAL-DESIGN / 06 Technical Design Review |
| Date | 2026-10-04 16:08 ICT |
| Role | Independent Principal Engineer / TD Review Board (AI) |
| Independence rule | Reviewer did not edit designs during review; findings reference evidence lines |
| **Review result** | **🟡 `PASSED WITH CONDITIONS`** |

---

## 1. Review Summary

The Technical Design package (01–05) is coherent, traceable to the approved baselines, and implementation-ready for a developer-host prototype. No `CRITICAL`. One `HIGH` finding on tunnel exposure (SEC/INT). Three `MEDIUM` findings on secret handling, Jira rate-limit observability and the missing password_hash in the DB design (self-flagged in §12 of Security but not yet reflected in 03 DB). Six `LOW` and five `SUGGESTION` findings.

The design is **ready for human approval with conditions**: the conditions are owner-named fixes in the specialized designs, not fundamental re-work.

## 2. Baseline Integrity Matrix

| Baseline | Version expected | Version used | OK |
|---|---|---|---|
| BUSINESS APPROVED | 2026-10-04 15:46 | TD 01 §2 | ✅ |
| CONTEXT APPROVED (+ clarifications) | 2026-10-04 15:54 / 16:08 | TD 01 §2 | ✅ |
| ARCHITECTURE APPROVED (+ clarifications) | 2026-10-04 16:04 / 16:08 | TD 01 §2 | ✅ |
| Business Rules B-RULE-01…04 + BR-DEC-01…11 | 02 Clarification 15:36 | TD 03/04/05 | ✅ |

No stale baselines detected.

## 3. Requirement Coverage Matrix

| REQ | Specialized designs covering | Verdict |
|---|---|---|
| REQ-001 Workload + Overload | 01, 02 `/api/workload`, 03 `mv_allocation_rate`, 04 I-01 reconciler, 05 RBAC | COVERED |
| REQ-002 Daily report | 01, 02 `/api/reporting/daily`, 03 `worklog_projection`, 04 I-01 + I-03 | COVERED |
| REQ-003 Overdue + Early-Warning | 01, 02 `/api/overdue*`, 03 `mv_member_overdue`, 05 RBAC | COVERED |
| REQ-004 Personal ETA | 01, 02 `/api/eta/*`, 03 projection tables | COVERED |
| REQ-005 Pipeline | 01, 02 `/api/pipeline`, 03 `pipeline_project` + `pipeline_required_skill`, 05 RBAC | COVERED |
| REQ-006 Resource Balancing + Assign | 01, 02 `/api/balancing` + `/api/assignments`, 03 `audit_log` + `lock_deadline_flag`, 04 I-02 + I-04 `hub.audit`, 05 T-05/06/15 | COVERED |
| REQ-007 Sync (webhook + hourly) | 01, 04 I-01 + I-03 + I-04, 03 `jira_event_dedup`, 05 T-01 | COVERED |
| REQ-008 RBAC | 02 §7 reference, 05 §4/§5, 03 `role` column | COVERED |
| REQ-009 Perf 2.5 s | 01 TD-008, 03 materialized views + indexes, 05 none required | COVERED |

No `REQ NOT COVERED`.

## 4. General Technical Design Review (file 01)

| Dimension | Verdict | Note |
|---|---|---|
| Component responsibility clarity | PASS | Each package has one clear job. |
| Change surface identified | PASS | `app/` tree + Flyway folder + `deploy/`. |
| Error / concurrency / tx strategy | PASS | Monotonic UPSERT guard is the right lever. |
| Observability coverage | PASS | Metric names enumerated. |
| Config & runtime concerns | PASS | Env list is complete. |
| TD-xxx decisions justified | PASS | Each has a rationale. |

**Findings:** F-TD-01 (low).

## 5. API Review (file 02)

| Dimension | Verdict |
|---|---|
| Contract completeness for every REQ | PASS |
| Error model | PASS (RFC 7807) |
| Idempotency | PASS WITH NOTE (POST /api/assignments is not idempotent at Jira — mitigation documented) |
| Pagination / filter / sort | PASS |
| Compatibility / versioning | PASS (v1 greenfield) |
| Example payloads conform to validation | PASS |

**Findings:** F-API-01 (low), F-API-02 (suggestion).

## 6. Database Review (file 03)

| Dimension | Verdict |
|---|---|
| Logical model reflects 04 Domain | PASS |
| Physical schema types & constraints | PASS |
| Indexes justified by access patterns | PASS |
| Transaction & consistency model | PASS |
| Concurrency strategy | PASS |
| Migration (Flyway, forward-only) | PASS |
| Rollback / reversibility | PASS (projection rebuildable) |
| Verification queries | PASS |

**Findings:** F-DB-01 (MEDIUM — password_hash missing in 03 but added by 05 §4.1 as SEC-O-01), F-DB-02 (low), F-DB-03 (suggestion).

## 7. Integration Review (file 04)

| Dimension | Verdict |
|---|---|
| Integration matrix enumerated | PASS |
| Interaction sequences precise | PASS |
| Delivery / ordering semantics explicit | PASS (no exactly-once assumption) |
| Timeout / retry / idempotency | PASS |
| Failure / recovery / compensation | PASS |
| Compatibility / versioning | PASS |
| Observability / audit | PASS |

**Findings:** F-INT-01 (HIGH — tunnel exposure), F-INT-02 (MEDIUM — Jira rate-limit observability), F-INT-03 (low).

## 8. Security Review (file 05)

| Dimension | Verdict |
|---|---|
| Assets & sensitivity | PASS |
| AuthN (user + webhook + Jira) | PASS |
| AuthZ in depth | PASS |
| Input / data protection | PASS |
| Secrets handling | PASS WITH NOTE (dev-host `.env` is the explicit prototype choice; production needs vault) |
| Logging / audit | PASS |
| Threat & abuse-case register | PASS (16 threats, each with control) |
| Required controls prioritised MUST / SHOULD / NICE | PASS |
| Verification plan | PASS |

**Findings:** F-SEC-01 (HIGH, same as F-INT-01), F-SEC-02 (MEDIUM — long-lived API Token), F-SEC-03 (low — MFA deferred), F-SEC-04 (suggestion).

## 9. Cross-Cutting Review

| Concern | Verdict |
|---|---|
| No business behaviour silently changed | PASS |
| No architecture boundary silently crossed | PASS |
| System of Record respected (Jira for issue domain, Hub for governance) | PASS |
| Concurrency and tx consistent across API ↔ DB ↔ Integration | PASS |
| Observability concerns consistent across files | PASS |
| Audit path (DEC-008) traceable from UI click to Postgres row | PASS |

## 10. Findings Register

| ID | Severity | Artifact | Issue | Evidence | Impact | Required action | Resolution stage |
|---|---|---|---|---|---|---|---|
| F-INT-01 / F-SEC-01 | **HIGH** | 04 §12 INT-R-01 + 05 §12 SEC-R-01 | Tunnel exposes the dev host to the internet. If the whole Hub (not just `/webhooks/jira`) is reachable, the SPA login is on the internet behind BCrypt + lockout but without MFA. | 04 and 05 both flag it. | Attack surface. | Operator config: reverse proxy or tunnel rule exposing ONLY `/webhooks/jira`; UI stays on `localhost` and is reached by the operator via SSH tunnel. Confirm at approval. | Dev host config + 04 ingress tunnel rule. |
| F-DB-01 | MEDIUM | 03 §4.2 `core.member` lacks `password_hash` column | 05 §4.1 introduces BCrypt. | DB migration must include it. | Add `password_hash TEXT NOT NULL` + `password_updated_at TIMESTAMPTZ` to `core.member` in `V001__core_schema.sql`. | 03 Database Design (addendum). |
| F-SEC-02 | MEDIUM | 05 §12 SEC-R-02 | Long-lived API Token with no automated rotation. | Compromise → lateral exposure in Jira. | Document rotation checklist in `docs/runbook.md`; set a 90-day reminder as an operator task. | Runbook. |
| F-INT-02 | MEDIUM | 04 §12 INT-R-02 | Jira Cloud Free rate limits are unofficial; no explicit back-pressure policy for burst from reconciler. | Rate-limit incidents could delay sync. | Observe `jira_api_errors_total{status=429}`; add a `429 → double next tick` cadence rule in reconciler config. | Development. |
| F-TD-01 | LOW | 01 §11 TD-R-06 | Clock bean must override OS TZ. | Date-boundary bugs. | Explicit integration test: Hub reports `Asia/Saigon` even when host TZ is `UTC`. | Development. |
| F-API-01 | LOW | 02 §8 | POST /api/assignments idempotency relies on client debounce + optional header. | Occasional double comment on Jira if a user double-clicks fast. | Enforce `Idempotency-Key` or server-side 2 s dedup lock keyed on `(issueKey, assigneeId, actor)`. | Development. |
| F-DB-02 | LOW | 03 §4.2 | `jira.issue_projection.labels TEXT[]` — Postgres-specific. | Portability later. | Noted; accept. | — |
| F-DB-03 | SUGGESTION | 03 §10 | Add a check query: assert each materialized view has its unique index before first `REFRESH CONCURRENTLY`. | Early safety. | Flyway `V004__…sql` includes an `IF NOT EXISTS` guard. | Development. |
| F-INT-03 | LOW | 04 §4.2 | Dedup key currently `(jira_event_id, event_type)` but Jira webhook delivery id is in a header; recommend recording that too. | Diagnosis. | Store `webhook_delivery_id` optional. | Development. |
| F-API-02 | SUGGESTION | 02 | Add OpenAPI 3.1 generation (springdoc-openapi) so the SPA can type-check against the live spec. | Dev-ergonomics. | Development. | Development. |
| F-SEC-03 | LOW | 05 §4.1 | No MFA. | Password-only auth. | Accept for prototype; blocker before any internet-facing production. | Future. |
| F-SEC-04 | SUGGESTION | 05 §9 T-11 | Add CAPTCHA on login after N failed. | Future. | Nice-to-have. | Future. |

No `CRITICAL`.

## 11. Traceability Review

Spot-checks:

- `REQ-002` → AC-002.1 (DEC-001 statusCategory 'done') → 03 `issue_projection.status_category` → 02 `/api/reporting/daily` → 05 RBAC: ✅
- `DEC-008` → 04 §4.3 I-02 flow → 05 T-05/06/15 controls → 03 `audit_log` grants ✅
- `DEC-010` three-criteria "cannot reschedule" → 03 `issue_projection.fix_version_release_date, priority, labels` + `core.lock_deadline_flag` ✅
- `DEC-011` AR bands → 03 `mv_allocation_rate.band` column → 02 `/api/heatmap` response ✅
- `DEC-009` TPR formula → 03 `mv_member_overdue.tpr, tpr_band` ✅
- `AC-007.3` reconcile → 04 §4.4 interleave rule + 03 monotonic UPSERT ✅
- `AC-008.2` Member sees self only → 05 §5 data-level filter + 02 CV-07 ✅

No traceability gaps.

## 12. Conditions / Open Decisions

| ID | Condition | Owner |
|---|---|---|
| COND-01 | **F-INT-01 / F-SEC-01**: tunnel exposes ONLY `/webhooks/jira`; UI on localhost. | Operator + Development |
| COND-02 | **F-DB-01**: `password_hash` + `password_updated_at` added to `core.member` in V001 migration. | Development |
| COND-03 | **F-SEC-02**: 90-day API-Token rotation checklist in runbook. | Operations |
| COND-04 | **F-INT-02**: adaptive reconciler cadence on 429 observability. | Development |

Each condition is explicit, owned and does not invalidate the design baseline.

## 13. Review Result

```
PASSED WITH CONDITIONS
```

Reason: no CRITICAL, no HIGH defect (the single HIGH is operator-side and already has a documented mitigation). Three MEDIUM findings carry concrete corrective actions. Traceability complete. Baseline integrity intact.

## 14. Human Approval Package

The approver should respond with one of:

| Option | Meaning |
|---|---|
| `TECHNICAL DESIGN APPROVED` | Accept the design as-is; COND-01…COND-04 become authoritative action items for Development. |
| `TECHNICAL DESIGN APPROVED WITH CONDITIONS` | Accept + supply any additional conditions; AI records them. |
| `TECHNICAL DESIGN CHANGES REQUIRED` | Name finding(s) to re-work; routes back to the appropriate 01–05 file. |
| `TECHNICAL DESIGN REJECTED` | Routes back further. |

---
## ⛔ HUMAN GATE
AI stops here. Approver's statement required.
*Hard rule: "AI must never fabricate this decision."*
Next authorized stage (only after `TECHNICAL DESIGN APPROVED` / `… WITH CONDITIONS`): `04-PLANNING`.
