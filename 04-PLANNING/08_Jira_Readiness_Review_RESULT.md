# 08 — JIRA READINESS REVIEW · Result

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 08 Jira Readiness Review |
| Date | 2026-10-06 10:00 ICT |
| Role | Independent Enterprise Delivery Reviewer (AI) |
| Independence rule | Reviewer did not edit 07's breakdown during review; findings cite evidence rows |
| Precondition | `PLAN APPROVED` (carried) + `07_Jira_Task_Breakdown_RESULT.md` present ✅ |
| **Review result** | **🟡 `PASSED WITH CONDITIONS`** |

> No `CRITICAL`. One `HIGH` (Jira project configuration unknown — resolvable before import, not a backlog defect). Three `MEDIUM`, four `LOW`, three `SUGGESTION`. All material PLAN steps covered. No orphan ticket. Dependency graph acyclic and consistent with Dependency Analysis.

---

## 1. Review Summary

The proposed Jira backlog in `07_Jira_Task_Breakdown_RESULT.md` is complete, traceable, non-duplicative, independently verifiable, and does not introduce scope beyond approved baselines. Jira project configuration is unknown and must be configured by the operator before import — this is the main condition carried into Human Gate `JIRA READY`.

## 2. Baseline Integrity

| Baseline | Verified against |
|---|---|
| BUSINESS APPROVED | 07 §2 cites 2026-10-04 15:46 ✅ |
| CONTEXT APPROVED (+ clar) | ✅ |
| ARCHITECTURE APPROVED (+ clar) | ✅ |
| TECHNICAL DESIGN APPROVED (+ 4 TD-COND) | ✅ |
| PLAN APPROVED (carried) | `PLAN_APPROVAL_RECORD.md` carried from 2026-10-04 16:30; the package reviewed in 06 was byte-identical ✅ |

No ticket introduces new REQ/AC. Checkpoint CP-1/2/3 and GOV G01…03 only expose approved human decisions; they do not grant new scope.

## 3. PLAN Coverage Matrix

| PLAN | Status |
|---|---|
| PLAN-001 | **COVERED** by JIRA-TMP-001 |
| PLAN-002 | COVERED by 002 |
| PLAN-003 | COVERED by 003 |
| PLAN-004 | COVERED by 004 (TD-COND-02 embedded via `password_hash` column) |
| PLAN-005 | COVERED by 005 |
| PLAN-006 | COVERED by 006 (append-only grants embedded) |
| PLAN-007 | COVERED by 007 (unique indexes embedded; F-DB-03) |
| PLAN-008 | COVERED by 008 |
| PLAN-009 | COVERED by 009 |
| PLAN-010 | COVERED by 010 |
| PLAN-011 | COVERED by 011 |
| PLAN-012 | COVERED by 012 |
| PLAN-013 | COVERED by 013 |
| PLAN-014 | COVERED by 014 |
| PLAN-015 | COVERED by 015 (dedup + UPSERT guard embedded) |
| PLAN-016 | COVERED by 016 (TD-COND-04 adaptive cadence embedded) |
| PLAN-017 | COVERED by 017 (TD-COND-01 narrow ingress embedded; GOV G01 adds operator evidence) |
| PLAN-018 | COVERED by 018 |
| PLAN-019 | COVERED by 019 |
| PLAN-020 | COVERED by 020 |
| PLAN-021 | COVERED by 021 (TPR DEC-009 embedded) |
| PLAN-022 | COVERED by 022 |
| PLAN-023 | COVERED by 023 |
| PLAN-024 | COVERED by 024 |
| PLAN-025 | COVERED by 025 |
| PLAN-026 | COVERED by 026 (PLAN-COND-01 dry-run flag embedded) |
| PLAN-027 | COVERED by 027 |
| PLAN-028 | COVERED by 028 |
| PLAN-029 | COVERED by 029 (MEMBER data-level filter embedded) |
| PLAN-030 | COVERED by 030 (PLAN-COND-04 scrollback-clear step embedded in runbook) |
| PLAN-031 | COVERED by 031 |
| PLAN-032 | COVERED by 032 (PLAN-COND-02 no-Node embedded) |
| PLAN-033 | COVERED by 033 (DEC-016 Vietnamese UI embedded) |
| PLAN-034 | COVERED by 034 |
| PLAN-035 | COVERED by 035 (TD-COND-03 rotation + PLAN-COND-03 drift-script + PRE-00 habit embedded) |
| PLAN-036 | COVERED by 036 |

| Checkpoint | Status |
|---|---|
| CP-1 | COVERED as JIRA-TMP-CP1 |
| CP-2 | COVERED as JIRA-TMP-CP2 |
| CP-3 | COVERED as JIRA-TMP-CP3 |

| TD-COND / PLAN-COND | Status |
|---|---|
| TD-COND-01 | Operational evidence tracked by JIRA-TMP-G01 |
| TD-COND-02 | Embedded in JIRA-TMP-004 |
| TD-COND-03 | Operational evidence tracked by JIRA-TMP-G03; runbook step in JIRA-TMP-035 |
| TD-COND-04 | Operational evidence tracked by JIRA-TMP-G02; implementation in JIRA-TMP-016 |
| PLAN-COND-01 | Embedded in JIRA-TMP-026 |
| PLAN-COND-02 | Embedded in JIRA-TMP-032 |
| PLAN-COND-03 | Embedded in JIRA-TMP-035 |
| PLAN-COND-04 | Embedded in JIRA-TMP-030 |

**No UNMAPPED, no DUPLICATED, no PARTIALLY COVERED items.**

## 4. Reverse Traceability Audit

Spot-checks (every chain holds):

| JIRA-TMP | → PLAN → TD → AC → REQ |
|---|---|
| 004 | PLAN-004 → TD-002, TD-COND-02 → — (schema) → REQ-008 |
| 015 | PLAN-015 → TD §6 UPSERT guard → AC-007.1 / AC-007.3 → REQ-007 |
| 018 | PLAN-018 → ADR-ARCH-009 → AC-003.3, AC-006.2 → REQ-003, REQ-006, REQ-009 |
| 021 | PLAN-021 → DEC-009 formula → AC-003.4 / AC-003.5 → REQ-003 |
| 026 | PLAN-026 → TD-003 / TD-009 / ADR-ARCH-006 / ADR-ARCH-010 → AC-006.4 / AC-006.5 → REQ-006 (+ DEC-008) |
| 029 | PLAN-029 → ADR-ARCH-007 → AC-008.1 / AC-008.2 / AC-008.3 → REQ-008 |
| 035 | PLAN-035 → TD-COND-03 → — (operational) → operator discipline |
| CP-1 | CP-1 playbook rule → precondition for 018+ → RBH operator checklist |
| G01 | GOV TD-COND-01 → operator evidence → tunnel ingress constraint |

**No orphan ticket.** Every GOV ticket is anchored to a TD-COND; every CP is anchored to a PLAN step boundary.

## 5. Issue Boundary / Size Review

| Issue | Verdict |
|---|---|
| 001 Scaffold | Right-sized (one coherent change). |
| 004 V001 | Larger than average (introduces 10+ tables) but naturally one migration unit — a single Flyway file is one atomic diff; keep as-is. |
| 007 V004 (MVs + unique indexes) | One file, two views, both share the same refresh model — keep as-is. |
| 015 Event consumer | Contains dedup + UPSERT + MV trigger wiring. Three cohesive responsibilities under one acceptance; keep. If a team later wants split, a sub-task is admissible. |
| 020 WorkloadService + endpoint | Right-sized. |
| 027 AssignmentService | Orchestration includes write + audit emission; one acceptance boundary — keep. |
| 029 `@PreAuthorize` + data-level filter | Cross-cuts many controllers; this is intentional (one filter is one acceptance). Flagging as F-JR-01 for operator awareness. |
| 033 SPA screens | Multiple screens in one issue; consider split per screen if ownership requires. F-JR-02. |
| 035 Runbook | Multiple sections, one owner — keep. |

No ticket spans unrelated REQ/AC. No ticket crosses independent deployment boundaries. No ticket requires multiple independent rollback paths.

## 6. Dependency Graph Review

Verified against `02_Dependency_Analysis_RESULT.md`:
- Topological order given in 07 §5 is a valid linearisation. ✅
- No cycle detected (confirmed by independent re-check). ✅
- CP-1 is correctly placed before any Jira write (018/026/027 blocked by CP-1). ✅
- GOV G01…G03 are blocked by their anchors (017/016/035). ✅
- All HARD orderings from Dependency Analysis §3 are present. ✅
- No false blocker found (017→CP-1 is required because CP-1 inspects a live tunnel).

## 7. Acceptance / DoD / Verification Review

Each implementation issue carries at least one measurable acceptance in §4 + §10. Vague words ("complete", "support", "optimize") are absent — every acceptance line names a measurable observable or an HTTP code. DoD template in 07 §4 applies to every ticket; no project-specific DoD override supplied.

| Issue | Acceptance quality |
|---|---|
| 004 | OK — DB test asserts columns exist. |
| 017 | OK — operator probe commands with expected responses. |
| 026 | OK — four distinct WireMock scenarios enumerated. |
| 029 | OK — SV-02…04 referenced. |
| 035 | OK — runbook sections enumerated with evidence requirement. |

## 8. Operational / Test / Security Coverage

- **Migrations**: V001…V006 each own their JIRA-TMP (004, 005, 006, 007, 008, 030). ✅
- **Config / secrets**: 007 (indexes), 017 (`.env.example` keys), 034 (secret-masking) carry it. ✅
- **Integration**: 011…016 cover reads; 026/027 cover writes; CP-1 gates writes. ✅
- **Release / tunnel**: 017 + G01 + 035 (runbook). ✅
- **Testing**: 036 covers E2E; AC-level tests are embedded in each S/T acceptance. Test Strategy is not reduced to a single "QA test" issue. ✅
- **Security**: 028/029/030/031 cover filter, RBAC in depth, bootstrap and audit. HMAC verifier is in 012; dry-run safety is in 026; G01 captures tunnel ingress evidence. SV-01…11 traceable. ✅

## 9. Jira Configuration Compatibility

**Marked `TO CONFIGURE`.** The breakdown is Jira-neutral:
- Issue types used: Epic / Story / Task (plus `Checkpoint-task` modelled as a plain Task; plus `GOV` modelled as a plain Task). If the target Jira project allows Sub-tasks, 015, 027 and 033 are candidates for split; no sub-task is pre-created.
- Workflow states referenced: generic `Open → In Progress → In Review → Done`.
- Custom fields referenced: none required beyond standard summary/description/parent/dependencies/labels.
- Suggested components / labels (optional): `area/foundation`, `area/sync`, `area/views`, `area/pipeline`, `area/security`, `area/ui`, `area/ops`, `type/checkpoint`, `type/governance`.

A **Jira project (any key, e.g. "RBH") must be created on the chosen non-production Jira Cloud Free site before import.** The project is distinct from the Jira project(s) the Hub observes via allow-list (DEC-005): the operator may use the same site but should keep the "RBH" project separate from the business projects whose issues get aggregated.

## 10. Findings Register

| ID | Severity | Issue | Impact | Required action | Owner |
|---|---|---|---|---|---|
| JR-01 | **HIGH** | **Jira project configuration unknown** (issue types, workflow, required fields, DoD, estimation). | Cannot create issues until configured. | Project owner creates a Jira project on the chosen non-production site; declares issue types / workflow / required fields; confirms the backlog mapping in §9 or supplies overrides. | Project owner / Operator |
| JR-02 | MEDIUM | JIRA-TMP-033 bundles multiple SPA screens | Per-screen tracking harder. | Optionally split into one issue per screen (workload, daily, overdue, heatmap, pipeline, assign, admin, login). Not blocking. | Development |
| JR-03 | MEDIUM | JIRA-TMP-015 bundles dedup + UPSERT + MV trigger | Three concerns in one acceptance. | Keep as-is for v1.0 (one acceptance owner). Admissible split into sub-tasks if assignment occurs. | Development |
| JR-04 | MEDIUM | GOV G01…G03 are not standard Jira issues in all workflows | Some orgs don't model governance evidence in Jira. | If the operator prefers runbook-only, drop the GOV tickets and keep evidence as runbook attachments. | Project owner |
| JR-05 | LOW | Checkpoints as plain Tasks | CP-1/2/3 are human decisions, not developer tasks. | Mark them with a `type/checkpoint` label and a non-developer assignee; or model them as external approval records. | Project owner |
| JR-06 | LOW | SPA framework still open (IMP-O-03) | Does not block Jira creation. | Decide at PLAN-032 time. | Development |
| JR-07 | LOW | Skill taxonomy L1/L2 values beyond seed | Does not block Jira creation. | Fill before 024 implementation. | Project owner |
| JR-08 | LOW | Full YAML spec in 07 §10 is a representative sample | Jira import still works mechanically from §4+§5+§7, but a full-expansion file is nicer. | Optional: emit `jira-import.csv` as part of `JIRA READY WITH CONDITIONS`. | Development |
| JR-SUGG-01 | SUGGESTION | Add label `v1.0` to every ticket | Keeps backlog filterable. | — | — |
| JR-SUGG-02 | SUGGESTION | Add link to the PLAN step id in the ticket description | Reinforces traceability inside Jira. | — | — |
| JR-SUGG-03 | SUGGESTION | Register webhook via Jira REST (automated) | Repeatability. | Post-v1.0. | — |

No `CRITICAL`.

## 11. Conditions / Required Corrections before `JIRA READY`

| ID | Condition | Owner |
|---|---|---|
| JIRA-COND-01 | Project owner creates a Jira project (any key) on the chosen non-production site and names the mapping (issue types, workflow, required fields, DoD, estimation). | Project owner |
| JIRA-COND-02 | Project owner decides whether to split JIRA-TMP-033 by screen and whether to keep GOV tickets (JR-02 + JR-04). | Project owner |
| JIRA-COND-03 | Operator confirms Checkpoint tickets will carry a `type/checkpoint` label (or equivalent) and a non-developer assignee (JR-05). | Operator |

None invalidates the backlog shape; all are config-level.

## 12. Readiness Result

```
PASSED WITH CONDITIONS
```

- No CRITICAL. The HIGH is a known configuration gap, not a backlog defect.
- 100% PLAN coverage. 100% CP coverage. 100% COND coverage.
- Dependency graph acyclic and consistent.
- Every ticket has measurable acceptance and traceable chain to REQ/AC.

## 13. Jira Creation Package Status

**Not yet created.** Awaiting Human Gate `JIRA READY` (or `JIRA READY WITH CONDITIONS`).

If a Jira integration/tool is authorized later, the mechanical import package is:
- Backlog table (`07 §4`) → row-per-issue CSV.
- Dependency map (`07 §5`) → `issuelinks` block per CSV row.
- Traceability matrix (`07 §7`) → populate `description` and `labels`.
- Structured YAML (`07 §10`) → canonical reference for Epics and representative issues; fully-expanded YAML can be generated on request.

## 14. Human Approval Package

The authorised approver issues **one** of:
- `JIRA READY` — accept as-is; JIRA-COND-01…03 become authoritative. (Still requires an authorized Jira create/import tool to actually create issues; this file never creates issues.)
- `JIRA READY WITH CONDITIONS` — accept + supply the Jira project configuration (issue types, workflow, required fields, DoD, estimation).
- `JIRA CHANGES REQUIRED` — name the finding(s) to re-work; routes back to `07_Jira_Task_Breakdown.md`.
- `JIRA REJECTED` — routes back further.

Items to confirm at approval:
- Jira project key + site chosen (JIRA-COND-01).
- Issue-type mapping (Epic/Story/Task/Sub-task) and whether to use sub-tasks for 015/027/033 (JR-02/03).
- Checkpoint and GOV ticket representation (JR-04/05).
- Any additional labels/components (JR-SUGG-01/02).

---
## ⛔ HUMAN GATE
AI stops here. Approver's statement required.
*Hard rule from the playbook: "AI must never fabricate this human decision."*
Next authorised action **only after** `JIRA READY`:
1. authorised Jira create/import tool is used to materialise the issues;
2. `JIRA-TMP-xxx` references are replaced with actual Jira keys;
3. the mapping `Actual Jira Key ↔ PLAN ↔ TD ↔ AC ↔ REQ` is preserved;
4. then — and only then — `05-DEVELOPMENT` begins per team workflow.

Creating Jira issues does not itself authorize code outside the approved Plan.
