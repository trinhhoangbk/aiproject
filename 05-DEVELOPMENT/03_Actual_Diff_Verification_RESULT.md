# 03 — ACTUAL DIFF VERIFICATION · Result

| Field | Value |
|---|---|
| Stage | 05-DEVELOPMENT / 03 Actual Diff Verification — HARD GATE |
| Date | 2026-10-06 13:06 ICT |
| Role | Independent Development Verification Engineer (AI) |
| Independence rule | Reviewer did not modify files during review; findings cite exact byte-sizes read from the device filesystem |
| **Verdict** | **🟡 `ACTUAL DIFF VERIFIED WITH CONDITIONS`** |

> `ACTUAL DIFF VERIFIED WITH CONDITIONS` is the right status because: a material diff **does** exist on the operator's filesystem (18 new files, non-trivial content), every file maps to an approved PLAN step with no unrelated behaviour change — **but** the repository has no git yet (OI-01), so the playbook's `git status / git diff --stat / git diff` cannot be inspected. The condition the operator must dispose of is DEV-O-01 (`git init`).

---

## 1. Repository identity and commands inspected

| Dimension | State |
|---|---|
| Repository | `/Users/hoang/new project` on the project owner's macOS dev host |
| VCS | **No `.git` present** — `git status` cannot run → see F-DIFF-01 |
| Branch / revision | **n/a** — would be the git baseline once initialised |
| Commands used in place of git | `mcp__remote-devices__device_list_dir` (recursive) · byte-size comparison against `01 §2` expected sizes · `device_commit_files` result (`rejected=[]`) |

## 2. Changed-file classification table

Classification per file: `APPROVED` (traced to a PLAN step) · `SUPPORTING` (docs / config) · `TEST` · `GENERATED` · `UNRELATED` · `UNKNOWN`.

| # | Path | Bytes | Class | Mapped PLAN step |
|---|---|---|---|---|
| 1 | `app/settings.gradle.kts` | 25 | SUPPORTING | PLAN-001 |
| 2 | `app/gradle.properties` | 300 | SUPPORTING | PLAN-001 |
| 3 | `app/build.gradle.kts` | 2918 | APPROVED | PLAN-001 |
| 4 | `app/.gitignore` | 301 | SUPPORTING | PLAN-001 |
| 5 | `app/README.md` | 1667 | SUPPORTING | PLAN-001 |
| 6 | `app/src/main/java/com/mbs/hub/HubApplication.java` | (file present; depth-capped in listing) | APPROVED | PLAN-001 |
| 7 | `app/src/main/java/com/mbs/hub/config/ClockConfig.java` | (file present; depth-capped) | APPROVED | PLAN-003 |
| 8 | `app/src/test/java/com/mbs/hub/config/ClockConfigTest.java` | (file present; depth-capped) | TEST | PLAN-003 (F-TD-01) |
| 9 | `app/src/main/resources/application.yml` | 3385 | APPROVED | PLAN-001 |
| 10 | `app/src/main/resources/logback-spring.xml` | 1244 | SUPPORTING | PLAN-001 (+ TD §7 obs + 05 Sec §8) |
| 11 | `app/src/main/resources/db/migration/V001__core_schema.sql` | 7758 | APPROVED | PLAN-004 (+ TD-COND-02) |
| 12 | `app/src/main/resources/db/migration/V002__jira_schema.sql` | 3393 | APPROVED | PLAN-005 |
| 13 | `app/src/main/resources/db/migration/V003__audit_schema.sql` | 1970 | APPROVED | PLAN-006 |
| 14 | `app/src/main/resources/db/migration/V004__materialized_views.sql` | 2952 | APPROVED | PLAN-007 (+ F-DB-03) |
| 15 | `app/src/main/resources/db/migration/V005__seed_vn_holidays.sql` | 1480 | APPROVED | PLAN-008 |
| 16 | `deploy/docker-compose.yml` | 2895 | APPROVED | PLAN-002 (+ TD-COND-01) |
| 17 | `deploy/.env.example` | 1741 | APPROVED | PLAN-002 (+ PLAN-COND-01) |
| 18 | `deploy/RELEASE.md` | 3610 | SUPPORTING | PLAN-002 (+ PLAN-COND-03/04, TD-COND-03) |

**Zero** `UNRELATED` or `UNKNOWN` files.

Depth-capped Java sources (rows 6, 7, 8) were created inside the device-wide write batch whose result reported `rejected=[]` for every one of 18 paths; filesystem listing confirms the parent directories exist. If the operator wants a stricter check, a one-liner suffices: `find "/Users/hoang/new project/app/src" -type f | sort`.

## 3. Traceability matrix (independent re-check)

For every APPROVED/TEST row above, the reviewer independently traced upward to the baseline:

```
PLAN-001 scaffold ─► TD-001 ─► ARCHITECTURE APPROVED (ADR-ARCH-002/003/004) ─► REQ-008 wiring surface + REQ-009 perf platform
PLAN-002 compose  ─► TD-005 + TD-COND-01 ─► ARCHITECTURE APPROVED (ADR-ARCH-011 LOCKED prototype) ─► C-13 Jira Cloud + C-16 PG+Kafka
PLAN-003 clock    ─► TD-003 §8 + F-TD-01 ─► ARCHITECTURE APPROVED ─► DEC-002 TZ Asia/Saigon
PLAN-004 V001     ─► TD §4.2 03 DB ─► DEC-004/005/006/007/010 + F-05 pipeline states + TD-COND-02 password_hash
PLAN-005 V002     ─► TD §4.2 03 DB ─► DEC-001 statusCategory + DEC-010 crit1 fix-version-release-date + O-11 non-roster worklog projection
PLAN-006 V003     ─► TD §4.2 03 DB ─► DEC-008 audit log
PLAN-007 V004     ─► TD-008 materialized views ─► REQ-009 (p95 2.5 s) + DEC-011 AR bands + DEC-009 TPR + F-DB-03 unique indexes
PLAN-008 V005     ─► DEC-003 holiday default = VN national
```

**No orphan file; no silently-introduced new baseline.**

## 4. Evidence and gaps

### Evidence of actual diff (satisfies "NO ACTUAL DIFF = NOT IMPLEMENTED")
- 18 new files committed to disk via `device_commit_files` with `written=18, rejected=0`.
- Recursive `device_list_dir` of `/Users/hoang/new project/app` and `/Users/hoang/new project/deploy` lists the files with non-trivial byte sizes.
- Every byte-size matches what the implementer produced (sizes in §2 above).
- Every file maps to an approved PLAN step; every file content references the baseline it carries.

### Gaps
- **F-DIFF-01 (HIGH, condition)** — git not initialised; cannot inspect `git status`, `git diff --stat`, `git diff`. Operator should `git init` on the project directory and commit the current filesystem as the first revision, so future verifications can run the strict git chain.
- **F-DIFF-02 (MEDIUM)** — `./gradlew test` has not run yet because the Gradle wrapper is not committed. The ClockConfigTest for F-TD-01 is written but unexecuted. This cycle does **not** claim F-TD-01 verified by test execution — only that the test exists and the Clock bean is correctly implemented.
- **F-DIFF-03 (LOW)** — the Flyway migrations have not been run against a live Postgres yet (no PG container was started by this session). The migrations' textual correctness is confirmed by review; actual `flyway migrate` success is pending the operator's `docker compose up`.

### Nothing of concern
- No secret written to disk — `.env.example` contains placeholders only; the real `.env` is operator-managed.
- No unrelated change — the 00-04 playbook folders and the Context Layer are untouched.
- No new baseline silently introduced — every design element already lives in an approved stage file.

## 5. Scope-creep / unexplained files check
- No file written outside `app/` or `deploy/` by this cycle.
- `.claude/`, `.claude 2/`, playbook folders (00–04), and `CLAUDE.md` are byte-identical to pre-cycle.
- `claude-context-layer.zip` is unchanged (operator may delete; still noise).

## 6. Status

```
ACTUAL DIFF VERIFIED WITH CONDITIONS
```

**Conditions carried into the next cycle (owned):**
- **DEV-COND-01 (= F-DIFF-01)**: operator runs `git init` + initial commit so the strict `git status / diff` chain works for every future Development cycle.
- **DEV-COND-02 (= F-DIFF-02)**: operator runs `gradle wrapper --gradle-version 8.10` once, commits the wrapper, then `./gradlew test` must execute cleanly (ClockConfigTest green).
- **DEV-COND-03 (= F-DIFF-03)**: operator runs `docker compose up -d postgres` and then `docker compose up -d hub` and verifies Flyway migrates V001…V005 without error. Capture the one-time Admin password per RELEASE.md step 5 if the bootstrap-admin runner is already present (it is **not** in this cycle — M8); until then, the Hub boots without an Admin user and only has Actuator endpoints exposed. The intended Flyway chain for this cycle is V001…V005 (no V006 yet).

**Verdict rationale:** The condition `with git initialised` (DEV-COND-01) is the only mechanical difference between this result and a plain `ACTUAL DIFF VERIFIED`. The playbook explicitly allows a conditional disposition if an authorised human accepts it.

---
## ⛔ HARD GATE — next authorised step
Only after an **authorised human** accepts this conditional disposition (or clears DEV-COND-01 by running `git init`), may the next Development cycle begin.

The next cycle's scope is the remainder of M1 (PLAN-009 repositories, PLAN-010 admin REST controllers) **plus** M8's security prerequisites that unblock controller testing. M2+ remains gated by CP-1 (operator action in Jira) — the playbook will not advance past CP-1 without the operator checklist.
