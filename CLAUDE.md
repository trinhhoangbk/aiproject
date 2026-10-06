# CLAUDE.md — new project

> Context Engineering Layer approved at Human Gate 02 (2026-10-04 15:01 ICT).
> Source of every statement: files in `00-PROJECT-ONBOARDING/` and the Stage-01 result.
> Anything not listed here is UNKNOWN. Do not fill gaps with assumptions.

## 1. Repository identity (CONFIRMED)
- Repository: `new project`.
- Content: a stage-gated AI onboarding playbook (`00-PROJECT-ONBOARDING/`).
- No application / production code exists in this repository.
- Not under version control (no `.git`) — see Open Items.

## 2. Global Guardrails (verbatim — `01_Project_Onboarding_Gate.md`)
- Evidence before assumption. UNKNOWN is better than WRONG.
- Preserve approved scope, boundaries, System of Record and decisions.
- Separate CONFIRMED / ASSUMPTION / PROPOSED / UNKNOWN / DECISION REQUIRED.
- Maintain traceability to authoritative evidence.
- Do not silently change an approved baseline.
- Do not write production code unless authorized by Development stage.
- AI proposes/reviews; authorized humans approve.
- NO EVIDENCE = NOT VERIFIED.

Detailed rules: `.claude/rules/`.

## 3. Stage flow (CONFIRMED)
| Stage | File | Output | Ends with |
|---|---|---|---|
| 01 Onboarding Gate | `00-PROJECT-ONBOARDING/01_Project_Onboarding_Gate.md` | Stage artifact, findings, open items, traceability, readiness | STOP |
| 02 Approval & Apply | `00-PROJECT-ONBOARDING/02_Onboarding_Approval_Apply.md` | Materialized Context Layer + git evidence | STOP — `CONTEXT LAYER MATERIALIZED — VERIFICATION REQUIRED` |
| 03 Verification | `00-PROJECT-ONBOARDING/03_Onboarding_Verification.md` | Exactly one verdict: `PROJECT AI-READY` / `PROJECT AI-READY WITH OPEN ITEMS` / `PROJECT NOT AI-READY` | STOP |

Commands: `/onboarding-gate`, `/onboarding-apply`, `/onboarding-verify` (`.claude/commands/`).
Independent reviewer for Stage 03: `.claude/agents/independent-reviewer.md`.

## 4. Human Gates (CONFIRMED)
- Gate 02: human approval required before the Context Layer is materialized.
- Gate 03: independent verification; the reviewer must not self-approve any Human Gate.
- STOP at every stage boundary and wait for the next authorized instruction.

## 5. Artifact location (user decision 2026-10-04)
- All stage outputs are saved inside this repository, in `00-PROJECT-ONBOARDING/`.
- Keep existing folder/file numbering as-is.

## 6. UNKNOWN — do not assume
- Business purpose beyond the playbook.
- Whether application code will be added to this repository.
- Tech stack, build, test, lint, CI/CD commands.
- System of Record / data domains.
- Named human approvers for Gates 02 and 03 (to be provided later).

## 7. Open Items affecting AI work
- No git: `git status` / `git diff` evidence cannot be produced until git is initialized (DECISION REQUIRED).
- `03_Onboarding_Verification.md` references `PLAYBOOK/00-PROJECT-ONBOARDING/...`; actual path is `00-PROJECT-ONBOARDING/...` (DECISION REQUIRED).
- `.claude/skills/` and `.claude/hooks/` are intentionally empty — no supporting evidence yet.
