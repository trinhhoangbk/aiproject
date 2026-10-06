# 02 — API CONTRACT DESIGN · Result

| Field | Value |
|---|---|
| Stage | 03-TECHNICAL-DESIGN / 02 API Contract Design |
| Date | 2026-10-04 16:08 ICT |
| Role | API/Contract Architect (AI) |
| **Status** | **🟡 API CONTRACT DESIGNED WITH OPEN ITEMS** |

---

## 1. Contract Scope

Two contract surfaces:
- **Hub REST API** served by the single Spring Boot app, consumed by the single-page UI (same-origin).
- **Jira Webhook endpoint** served by the same app, consumed by Jira Cloud.

Hub does **not** publish an external public API in v1.0. The Jira REST calls Hub makes are covered by Jira's own contract and detailed in Integration Design (04).

## 2. Existing Contract Evidence
None — greenfield. No incumbent consumer exists. The UI is net-new and ships with the Hub, so there is no backward-compatibility obligation for Hub REST in v1.0.

## 3. Contract Change Matrix

| New Interface | Direction | Consumer | REQ coverage |
|---|---|---|---|
| `/api/auth/*` | UI → Hub | SPA | REQ-008 |
| `/api/roster/*` | UI → Hub | SPA (Manager/Admin) | DEC-006 |
| `/api/capacity/*` | UI → Hub | SPA (Admin/Manager) | DEC-004 |
| `/api/holidays/*` | UI → Hub | SPA (Admin/Ops Lead) | DEC-003 |
| `/api/allowlist/*` | UI → Hub | SPA (Manager) | DEC-005 |
| `/api/skills/*` | UI → Hub | SPA (Manager) | DEC-007 |
| `/api/workload/*` | UI → Hub | SPA | REQ-001 |
| `/api/overdue/*` | UI → Hub | SPA | REQ-003 |
| `/api/reporting/daily` | UI → Hub | SPA | REQ-002 |
| `/api/eta/*` | UI → Hub | SPA | REQ-004 |
| `/api/pipeline/*` | UI → Hub | SPA (Manager) | REQ-005 |
| `/api/balancing/*` | UI → Hub | SPA (Manager) | REQ-006 |
| `/api/assignments` | UI → Hub | SPA (Manager/Admin) | REQ-006 / DEC-008 |
| `/api/heatmap/*` | UI → Hub | SPA (Manager/Admin) | REQ-006 (DEC-011 bands) |
| `/api/audit` | UI → Hub | SPA (Admin) | DEC-008 (read-only) |
| `/webhooks/jira` | Jira → Hub | Jira Cloud | REQ-007 |

## 4. Detailed Operations/Interfaces

Representative endpoints (full list enumerated at implementation; shape fixed here).

### 4.1 Auth

| Method | Path | Purpose | Roles |
|---|---|---|---|
| POST | `/api/auth/login` | Form login; session cookie | public |
| POST | `/api/auth/logout` | Invalidate session | authenticated |
| GET | `/api/auth/me` | Current user + role | authenticated |

### 4.2 Workload / Overload (REQ-001)

| Method | Path | Query | Returns |
|---|---|---|---|
| GET | `/api/workload/{memberId}` | `window=day|week|2weeks|month`, `anchor=YYYY-MM-DD` (default today Asia/Saigon) | `WorkloadView` — see 5.2 |
| GET | `/api/workload/{memberId}/overload` | same | `OverloadFlag[]` with reasons tied to DEC-010 criteria |

### 4.3 Daily report (REQ-002)

| Method | Path | Query | Returns |
|---|---|---|---|
| GET | `/api/reporting/daily` | `date=YYYY-MM-DD`, `memberId=` (optional) | `DailyReport` — done list + worklog totals; Discarded flagged per DEC-001 |

### 4.4 Overdue + Early-Warning (REQ-003)

| Method | Path | Query | Returns |
|---|---|---|---|
| GET | `/api/overdue` | `memberId=` (optional) | `OverdueList` with severity bands 1–3 d / 4–7 d / >1 w |
| GET | `/api/overdue/warnings` | `memberId=` (optional) | `WarningList` with TPR band (yellow/red) per DEC-009 |

### 4.5 Resource Balancing (REQ-006)

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/api/heatmap` | query: `window=` | `HeatmapRow[]` with AR + band (DEC-011) |
| POST | `/api/balancing/suggest` | `{ pipelineProjectId }` | `MemberSuggestion[]` sorted by Available_MD |
| POST | `/api/assignments` | `{ issueKey, newAssigneeId }` | `AssignmentResult` (success) or `ProblemDetails` (403/workflow error) |

### 4.6 Pipeline (REQ-005)

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/api/pipeline` | — | `PipelineProject[]` |
| POST | `/api/pipeline` | `PipelineProjectCreate` (see 5.4) | `PipelineProject` |
| PATCH | `/api/pipeline/{id}` | partial | `PipelineProject` |

### 4.7 Jira Webhook

| Method | Path | Headers | Body |
|---|---|---|---|
| POST | `/webhooks/jira` | `X-Hub-Signature: sha256=<hmac>` | Jira Cloud webhook payload (JSON) |

Response: `200 OK` always on verified payload; `401` on signature mismatch. Jira does not re-deliver on 2xx.

## 5. Request / Response Schemas & Validation

JSON everywhere; UTF-8; `Content-Type: application/json`; dates `YYYY-MM-DD`; timestamps RFC 3339 with Asia/Saigon offset.

### 5.1 Common

```json
"Role": "ADMIN" | "MANAGER" | "MEMBER"
"MemberId": "<uuid>"
"IssueKey": "<string>"           // e.g. "PRJ-123"
"JiraAccountId": "<string>"      // Atlassian global account id
"Window": "day" | "week" | "2weeks" | "month"
"HeatmapBand": "dark_green" | "light_green" | "yellow" | "red"
"OverdueBand": "1_to_3_days" | "4_to_7_days" | "more_than_a_week"
"WarningBand": "none" | "yellow" | "red"
```

### 5.2 `WorkloadView`

```json
{
  "memberId": "...",
  "memberName": "...",
  "window": "week",
  "anchor": "2026-10-04",
  "standardMd": 5,
  "committedMd": 4.5,
  "availableMd": 0.5,
  "allocationRate": 90.0,
  "band": "yellow",
  "overload": {
    "flag": "red",
    "reasons": [
      { "level": "week", "committedH": 42, "capacityH": 40 },
      { "level": "day", "date": "2026-10-05", "committedH": 10, "capacityH": 8, "hardDeadlineCriteria": ["fix_version_release", "priority_blocker"] }
    ]
  },
  "projects": [
    { "projectKey": "PRJ-A", "issueCount": 5, "remainingH": 20, "ratioByHours": 0.55, "ratioByCount": 0.5 },
    { "projectKey": "CORE-SYS", "issueCount": 3, "remainingH": 16, "ratioByHours": 0.45, "ratioByCount": 0.3 }
  ],
  "unestimated": {
    "count": 2,
    "defaultEachMd": 0.5
  }
}
```

### 5.3 `DailyReport`

```json
{
  "date": "2026-10-04",
  "memberId": "...",
  "done": [
    {
      "issueKey": "PRJ-A-123",
      "projectKey": "PRJ-A",
      "doneAt": "2026-10-04T18:30:00+07:00",
      "resolution": "Done",
      "discarded": false
    }
  ],
  "worklogByProject": [
    { "projectKey": "PRJ-A", "hours": 4.0 },
    { "projectKey": "CORE-SYS", "hours": 3.5 }
  ],
  "totalHours": 7.5
}
```

### 5.4 `PipelineProjectCreate`

```json
{
  "name": "string, required, 1..200",
  "objective": "string, required, 1..2000",
  "targetDeadline": "YYYY-MM-DD, required, >= today",
  "totalEstimatedMd": "number, required, > 0",
  "requiredSkills": [
    { "l1": "FRONTEND|BACKEND|MOBILE|QA_QC|DEVOPS|UI_UX", "l2": "optional controlled list value" }
  ]
}
```

Validation rules:
- All string length bounds enforced (RFC 7807 400 on violation).
- `l1`/`l2` must exist in `skill_taxonomy`.
- Rejection returns the field path per `ProblemDetails`.

## 6. Response Status & Error Model

Standard HTTP + RFC 7807 `application/problem+json`:

| Status | Meaning |
|---|---|
| 200 | OK |
| 201 | Created (POST pipeline, POST roster) |
| 204 | No content (DELETE, logout) |
| 400 | Validation error — body: `ProblemDetails` with `errors[]` |
| 401 | Not authenticated |
| 403 | Forbidden (RBAC) |
| 404 | Not found |
| 409 | Conflict (concurrent edit) |
| 422 | Business rule violation (e.g. assign to non-roster user) |
| 429 | Rate-limited (reserved) |
| 503 | Downstream unavailable (e.g. Jira) |

### Assignment errors (DEC-008)

```json
{
  "type": "https://hub.local/errors/jira-write-blocked",
  "title": "Jira rejected the assignment",
  "status": 422,
  "detail": "Jira responded 403 Forbidden; current workflow does not allow assignee change in status 'In Review'.",
  "retryable": false,
  "clientAction": "Rollback optimistic UI change; show error to user; do not auto-retry.",
  "jira": { "status": 403, "errorMessages": ["Forbidden"] }
}
```

## 7. AuthN / AuthZ (reference)

Full spec in `05_Security_Design_RESULT.md`. Contract rules:
- All `/api/**` endpoints except `/api/auth/login` require session.
- Method-level `@PreAuthorize` per role; data-level filter ensures Member sees self only.
- `/webhooks/jira` is **not** session-protected; it is HMAC-verified instead.

## 8. Idempotency / Concurrency

| Endpoint | Semantics |
|---|---|
| POST `/api/assignments` | **Non-idempotent** at the Jira level; a double-POST writes a second internal comment. Mitigation: UI debounce + optional `Idempotency-Key` header the server deduplicates for 60 s. |
| POST `/api/pipeline` | Non-idempotent (creates); clients may send `Idempotency-Key`. |
| PATCH endpoints | Optimistic concurrency via optional `If-Match: "<etag>"`; conflict → 409. |
| Jira webhook | Idempotent at the consumer via `(jira_event_id, event_type)` dedup. |

## 9. Pagination / Filter / Sort

| Endpoint pattern | Convention |
|---|---|
| List endpoints with potential > 100 rows (`/api/overdue`, `/api/audit`) | Query: `page=` (1-based), `size=` (default 50, max 200). Response envelope: `{ "items": [...], "page": 1, "size": 50, "total": 123 }`. |
| Sort | `sort=field:asc|desc` (whitelist of fields per endpoint). |
| Filter | Named query params; everything else is 400. |

## 10. Compatibility / Versioning

- v1.0 prototype → no public consumers → **no versioning scheme** for Hub REST yet.
- When second consumer arrives, introduce `/api/v2/...` or a `Hub-API-Version` header; backward compatibility within v1.
- Jira webhook payload is Jira-defined; Hub tolerates unknown fields via Jackson `FAIL_ON_UNKNOWN_PROPERTIES=false`.

## 11. Consumer Impact

Only consumer today is the UI shipped in the same artifact. Zero migration cost. If a mobile app or BI consumer appears later, this list becomes the first public contract.

## 12. Contract Verification Scenarios

| ID | Endpoint | Scenario | Expected |
|---|---|---|---|
| CV-01 | GET /api/workload/{id}?window=week | Member M has 42 h committed this week | AR 105, band "red", overload at week level |
| CV-02 | GET /api/reporting/daily?date=2026-10-04 | M closed 2 issues Oct 4; 1 is Won't Fix | 2 items; one `discarded:true` |
| CV-03 | GET /api/overdue?memberId=M | M has 3 overdue issues (2 d, 5 d, 10 d) | three bands: 1_to_3, 4_to_7, more_than_a_week |
| CV-04 | POST /api/assignments | Jira returns 403 | HTTP 422 ProblemDetails with Jira message, UI rolls back |
| CV-05 | POST /webhooks/jira | invalid HMAC | HTTP 401, no Kafka publish |
| CV-06 | POST /webhooks/jira | valid, duplicate event | HTTP 200; consumer dedup drops the duplicate |
| CV-07 | GET /api/workload (Member role, other member) | — | HTTP 403 |

## 13. Traceability

| AC | Contract | Verification |
|---|---|---|
| AC-001.1…1.5 | `/api/workload/*` | CV-01 |
| AC-002.1…2.4 | `/api/reporting/daily` | CV-02 |
| AC-003.1…3.5 | `/api/overdue*` | CV-03 |
| AC-004.1…4.2 | `/api/eta/*` | TBD at integration test |
| AC-005.1…5.2 | `/api/pipeline` | — |
| AC-006.1…6.6 | `/api/heatmap`, `/api/balancing`, `/api/assignments` | CV-04 |
| AC-007.1…7.3 | `/webhooks/jira` + reconciler | CV-05, CV-06 |
| AC-008.1…8.3 | Spring Security + RBAC filters | CV-07 |

## 14. Open Items

| ID | Item |
|---|---|
| API-O-01 | ETag format for `If-Match` on PATCH endpoints — Development stage. |
| API-O-02 | `Idempotency-Key` dedup window (currently 60 s proposed) — Development stage. |
| API-O-03 | Final list of sortable fields per endpoint — Development stage. |
| API-O-04 | SPA framework (no architectural impact) — Development stage. |

## 15. Status

```
API CONTRACT DESIGNED WITH OPEN ITEMS
```

---
⛔ **STOP — stage boundary.** Next: `03_Database_Design.md`.
