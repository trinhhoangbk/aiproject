# Development · Cycle 3 — PLAN-009 + PLAN-010 (M1 remainder)

| Field | Value |
|---|---|
| Date | 2026-10-06 15:03 ICT |
| HARD GATE | PLAN APPROVED carried · DEV-COND-01/02/03 fulfilled in cycle 2 |
| **Status** | **🟢 IMPLEMENTATION COMPLETE for PLAN-009 and PLAN-010** |

> Scope is the remainder of M1: JPA entities + Spring Data repositories + the 3-tier
> `CapacityResolver` service (PLAN-009), and the 5 admin REST controllers for roster /
> capacity / holidays / allowlist / skills (PLAN-010). Security filter chain and
> `@PreAuthorize` RBAC come in M8 per the plan — deliberately absent here.

---

## 1. PLAN-009 — Repositories + CapacityResolver

### 1.1 Entities (11 files)
Jakarta Persistence annotations; Lombok `@Getter @Setter @NoArgsConstructor` for mutable
shapes. Timestamps auto-stamped via `@PrePersist`/`@PreUpdate`. Password hash + updated-at
on `Member` honour TD-COND-02.

| Entity | Schema / Table | Notes |
|---|---|---|
| `core.member.Team` | `core.team` | — |
| `core.member.Member` | `core.member` | `password_hash` + `password_updated_at` (TD-COND-02); `operations_lead` subrole (DEC-003 §5.3); `role` enum (DEC-004) |
| `core.capacity.CapacityGlobal` | `core.capacity_global` | single-row singleton (id=1 CHECK) |
| `core.capacity.CapacityTeam` | `core.capacity_team` | history keyed by `effective_from` |
| `core.capacity.CapacityMember` | `core.capacity_member` | history keyed by `effective_from` |
| `core.holiday.Holiday` | `core.holiday_calendar` | `kind` enum (DEC-003) |
| `core.allowlist.AllowListEntry` | `core.allow_list` | DEC-005 |
| `core.skill.SkillTag` + `SkillKey` | `core.skill_taxonomy` | 2-level composite PK (`l1`, `l2`); DEC-007 |
| `core.skill.MemberSkill` + `MemberSkillKey` | `core.member_skill` | composite PK + FK to SkillTag |
| `core.pipeline.PipelineProject` | `core.pipeline_project` | `state` enum covers 7 approved states (F-05) |
| `core.pipeline.PipelineRequiredSkill` + `Key` | `core.pipeline_required_skill` | composite PK |
| `core.lockdeadline.LockDeadlineFlag` | `core.lock_deadline_flag` | DEC-010 criterion 3 |

### 1.2 Repositories (12 Spring Data JPA interfaces)
Each entity has a `JpaRepository`. Two have custom queries:
- `CapacityTeamRepository.findCurrent(teamId, on)` — latest row with `effective_from <= on`.
- `CapacityMemberRepository.findCurrent(memberId, on)` — same shape, member-tier.

### 1.3 `CapacityResolver` service — DEC-004 three-tier precedence
`com.mbs.hub.core.capacity.CapacityResolver` implements the business rule:
MEMBER override › TEAM override › GLOBAL default. Uses the injected `Clock` bean
(DEC-002, `Asia/Saigon`) so `resolveToday()` is deterministic in tests.

Returns an `EffectiveCapacity` record with `source: GLOBAL|TEAM|MEMBER` so callers
know which tier applied.

### 1.4 Test — `CapacityResolverTest`
4 cases with Mockito stubs cover every branch:
1. Member override beats team + global.
2. Team override beats global when member has none.
3. Global default when neither team nor member override.
4. Global default when member has team but team has no override.

## 2. PLAN-010 — Admin REST controllers (5 resources)

Spring MVC controllers under `/api/*`. Request DTOs use Jakarta Bean Validation.
Response shapes are either entities (where safe) or `*View` records (used for `Member`
to avoid leaking `passwordHash`). All error responses are RFC 7807 `ProblemDetail` via
`web.GlobalExceptionHandler`.

| Endpoint family | Verbs | Owner (future M8 RBAC) |
|---|---|---|
| `/api/roster` | GET list, GET /{id}, POST (BCrypt-12 hash of `initialPassword`), PATCH /{id} partial, DELETE /{id} (deactivate — soft) | Manager + Admin |
| `/api/capacity/global` | GET, PUT | Admin + Manager (DEC-004) |
| `/api/capacity/team/{teamId}` | POST (adds dated override) | Admin + Manager |
| `/api/capacity/member/{memberId}` | POST (adds dated override) | Admin + Manager |
| `/api/capacity/effective/{memberId}` | GET (`?on=yyyy-mm-dd` optional) — returns resolved tier + hours | Any role (self for Member) |
| `/api/holidays` | GET (`?from&to` optional), GET /{date}, PUT /{date} upsert, DELETE /{date} | Admin + Operations Lead (DEC-003) |
| `/api/allowlist` | GET, PUT /{projectKey} upsert, DELETE /{projectKey} | Admin + Manager (DEC-005) |
| `/api/skills/taxonomy` | GET, PUT, DELETE (`?l1&l2`) | Admin + Manager (DEC-007) |
| `/api/skills/members/{memberId}` | GET, POST (adds a tag — rejects if tag not in taxonomy), DELETE (clear) | Admin + Manager |

Security filter chain and `@PreAuthorize` annotations arrive in M8 (PLAN-028/029).
In this cycle the endpoints are reachable without auth; `JIRA_WRITE_DRY_RUN=true`
remains the committed default so no accidental Jira write can occur.

## 3. Baselines honoured

| Baseline item | Where it lives in cycle 3 |
|---|---|
| TD-COND-02 `password_hash` | `Member.passwordHash` + `MemberController` hashes `initialPassword` via BCrypt-12 (`PasswordConfig`) |
| DEC-002 `Asia/Saigon` clock | `CapacityResolver.resolveToday()` uses the `Clock` bean produced by `ClockConfig` |
| DEC-003 holiday ownership | `operations_lead` flag on `Member`, `HolidayController` |
| DEC-004 3-tier capacity | `CapacityResolver` tested exhaustively |
| DEC-005 allow-list | `AllowListEntry` + `AllowListController` |
| DEC-006 roster → `jira_account_id` | unique index enforced by schema; `MemberRepository.existsByJiraAccountId` |
| DEC-007 2-level skill taxonomy | `SkillKey` composite PK; `SkillController` rejects member-skill if tag absent |
| DEC-010 criterion 3 | `LockDeadlineFlag` entity + repo (M7 wires the controller) |
| DEC-016 Vietnamese UI | not relevant to this cycle (M10) |

## 4. Files added in cycle 3

| Path | PLAN step |
|---|---|
| `app/src/main/java/com/mbs/hub/config/PasswordConfig.java` | PLAN-009 (hash) |
| `app/src/main/java/com/mbs/hub/core/member/{Team,Member,Role}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/member/{Team,Member}Repository.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/member/dto/{MemberCreateRequest,MemberUpdateRequest,MemberView}.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/member/MemberController.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/capacity/{CapacityGlobal,CapacityTeam,CapacityMember}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/capacity/{CapacityGlobalRepository,CapacityTeamRepository,CapacityMemberRepository}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/capacity/{EffectiveCapacity,CapacityResolver}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/capacity/dto/{CapacityUpdateRequest,EffectiveCapacityView}.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/capacity/CapacityController.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/holiday/{Holiday,HolidayKind,HolidayRepository}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/holiday/dto/HolidayUpsertRequest.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/holiday/HolidayController.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/allowlist/{AllowListEntry,AllowListRepository}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/allowlist/dto/AllowListUpsertRequest.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/allowlist/AllowListController.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/skill/{SkillKey,SkillTag,SkillTagRepository,MemberSkillKey,MemberSkill,MemberSkillRepository}.java` | PLAN-009 |
| `app/src/main/java/com/mbs/hub/core/skill/dto/SkillTagUpsertRequest.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/skill/SkillController.java` | PLAN-010 |
| `app/src/main/java/com/mbs/hub/core/pipeline/{PipelineProject,PipelineState,PipelineRequiredSkill,PipelineRequiredSkillKey,PipelineProjectRepository,PipelineRequiredSkillRepository}.java` | PLAN-009 (controller lives in M7) |
| `app/src/main/java/com/mbs/hub/core/lockdeadline/{LockDeadlineFlag,LockDeadlineFlagRepository}.java` | PLAN-009 (controller lives in M7) |
| `app/src/main/java/com/mbs/hub/web/GlobalExceptionHandler.java` | PLAN-010 (RFC 7807) |
| `app/src/test/java/com/mbs/hub/core/capacity/CapacityResolverTest.java` | PLAN-009 verification |
| `05-DEVELOPMENT/CYCLE_3_PLAN_009_010_REPORT.md` | this report |

## 5. What is NOT in this cycle

- No Spring Security filter chain (M8 — PLAN-028/029).
- No bootstrap-admin runner (M8 — PLAN-030).
- No Jira adapter, webhook, reconciler, write-back (M2 — gated by CP-1).
- No materialized-view refresher wiring (M4 — PLAN-018).
- No workload/overdue/report/pipeline-balancing services (M5–M7).
- No SPA (M10). No E2E tests (M12).

## 6. Status

```
IMPLEMENTATION COMPLETE  (for the authorised scope PLAN-009 + PLAN-010)
```

Across all Development cycles so far: **18 of 36 PLAN steps materialised** (M0 full; M1 full).
Next cycle is M2 — gated by **CP-1** (operator action in Jira).

## 7. Verification notes

- `./gradlew test` was not run inside the Claude session: the egress proxy blocks Maven
  Central from this container (see `CYCLE_2_DEV_COND_REPORT.md §1`). The operator runs
  it on their dev host where network access is normal.
- Standalone F-TD-01 harness in cycle 2 confirmed the Clock bean behaviour the
  `CapacityResolver` depends on.
- Flyway migrations V001…V005 were verified against a live PostgreSQL 16 in cycle 2;
  entities above target those exact tables.

---
⛔ **STOP** — do not self-advance to M2. Operator action next:
1. `git pull` on the dev host inside `/Users/hoang/new project/` once it is a clone of
   `github.com/trinhhoangbk/aiproject`.
2. `cd app && ./gradlew test` to exercise `ClockConfigTest` + `CapacityResolverTest`.
3. Signal continue; cycle 4 starts the M2 Jira adapter (reads first), still gated by CP-1.
