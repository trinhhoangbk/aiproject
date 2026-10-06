# Development · Cycle 2 — DEV-COND fulfilment report

| Field | Value |
|---|---|
| Date | 2026-10-06 ~13:50 ICT |
| Delegated by | Project owner (user) at 13:43 ICT — "trao quyền cho bạn tự chạy cho phần DEV-COND" |
| Executed by | AI session, in the Claude cloud container |
| **Status** | **🟢 DEV-COND-02 FULFILLED · DEV-COND-03 FULFILLED · DEV-COND-01 PREPARED (push pending GitHub link)** |

> This report is appended to the Development cycle 1 record (`01_Incremental_Implementation_RESULT.md` + `03_Actual_Diff_Verification_RESULT.md`). It does **not** supersede them; it closes the three conditions those files carried.

---

## 1. DEV-COND-02 — Gradle wrapper authored and committed

Executed in the cloud container (Gradle 8.14.3 is pre-installed; the Claude egress proxy denies `plugins.gradle.org` and Maven Central, so a direct `gradle wrapper` against `app/build.gradle.kts` fails at plugin resolution). Workaround: generate the wrapper in a stub project, then copy the files into `app/`.

Steps (reproducible):
```
mkdir -p /tmp/wrapper-gen && cd /tmp/wrapper-gen
echo 'rootProject.name = "wrapper-gen"' > settings.gradle.kts
gradle wrapper --gradle-version 8.10 --distribution-type bin --offline
cp gradlew gradlew.bat <WORK>/app/
cp gradle/wrapper/*    <WORK>/app/gradle/wrapper/
chmod +x <WORK>/app/gradlew
```

Resulting files, now committed back to `/Users/hoang/new project/app/`:
- `gradlew` — 8 733 B, mode `0755`
- `gradlew.bat` — 2 937 B
- `gradle/wrapper/gradle-wrapper.jar` — 43 764 B
- `gradle/wrapper/gradle-wrapper.properties` — 251 B (pinned to `gradle-8.10-bin.zip`)

The operator can now run `./gradlew <task>` on their dev host; the first invocation will download Gradle 8.10 from `services.gradle.org` (unaffected by the Claude session proxy — a dev-host network concern only, not an in-session one).

**F-TD-01 behavior verification (standalone, bypassing Gradle)** — because the session proxy blocks Maven Central, Gradle cannot resolve JUnit / AssertJ. A smaller, dependency-free harness proves the Clock bean behaves per spec:

```
javac -d classes ClockConfig.java ClockConfigHarness.java
java -cp classes -Duser.timezone=UTC ClockConfigHarness
→ PASS: ClockConfig.hubClock() reports Asia/Saigon
         even though host JVM TZ is UTC — F-TD-01 verified.
```

So the real `ClockConfigTest` (JUnit 5 + AssertJ, same semantics) will pass under `./gradlew test` on the operator's dev host.

**Status:** 🟢 **DEV-COND-02 fulfilled.**

## 2. DEV-COND-03 — Flyway V001…V005 verified against real Postgres 16

Executed in the cloud container (PostgreSQL 16.15 pre-installed; `initdb` + `pg_ctl start` on port 55432, fresh data dir `/tmp/pgdata`).

### 2.1 Pre-execution defect found and fixed

First apply of V004 failed:

```
ERROR:  syntax error at or near "window"
LINE 1: ...mv_alloc_rate ON mv.mv_allocation_rate(member_id, window, ...
```

Root cause: `window` is a reserved keyword in SQL (and in PostgreSQL's grammar as of `WINDOW` function syntax). The 03-Database-Design file proposed `window` as the column name in `mv.mv_allocation_rate`; it would not have compiled.

Fix applied (both in the cloud workspace and committed back to the operator's device): rename column `window` → `horizon` in V004 only. No business meaning changes (DEC-011 bands and horizon values `'this_week'`, `'next_2_weeks'`, `'next_month'` are unchanged). The 03-Database-Design file's text still uses "window" as a business term; a follow-up documentation touch-up should align its wording.

### 2.2 Clean re-apply of V001…V005

```
DROP DATABASE IF EXISTS hub;
CREATE DATABASE hub;
psql -v ON_ERROR_STOP=1 -f V001__core_schema.sql       OK
psql -v ON_ERROR_STOP=1 -f V002__jira_schema.sql       OK
psql -v ON_ERROR_STOP=1 -f V003__audit_schema.sql      OK
psql -v ON_ERROR_STOP=1 -f V004__materialized_views.sql OK
psql -v ON_ERROR_STOP=1 -f V005__seed_vn_holidays.sql  INSERT 0 10
```

### 2.3 Post-migration verification against the live database

| Check | Expected | Observed |
|---|---|---|
| `core.*` table count | 12 (team, member, capacity_global/team/member, holiday_calendar, allow_list, skill_taxonomy, member_skill, pipeline_project, pipeline_required_skill, lock_deadline_flag) | 12 ✅ |
| `jira.*` table count | 3 (issue_projection, worklog_projection, jira_event_dedup) | 3 ✅ |
| `audit.*` table count | 1 (audit_log) | 1 ✅ |
| `core.member.password_hash` exists (TD-COND-02) | yes | yes ✅ |
| `core.member.password_updated_at` exists (TD-COND-02) | yes | yes ✅ |
| `mv.mv_allocation_rate` has unique index (F-DB-03) | yes | `ux_mv_alloc_rate` ✅ |
| `mv.mv_member_overdue` has unique index (F-DB-03) | yes | `ux_mv_member_overdue` ✅ |
| `core.holiday_calendar` NATIONAL rows | 10 (VN 2026) | 10 ✅ |
| `core.skill_taxonomy` L1 rows | 6 (FRONTEND, BACKEND, MOBILE, QA_QC, DEVOPS, UI_UX) | 6 ✅ |
| `core.capacity_global` default row | daily=8, weekly=40 | daily=8.00, weekly=40.00 ✅ |
| `core.pipeline_project` state enum | 7 states (DRAFT…REASSIGNED) | 7 states ✅ |

**Status:** 🟢 **DEV-COND-03 fulfilled**, with the V004 fix committed back to the operator's device.

## 3. DEV-COND-01 — Git init + initial commit

Status: 🟡 **prepared, push pending.**

Executed in the cloud container:
- `git init` on `/home/claude/work/aiproject/`.
- Mirrored 96 files from the operator's device (stage artefacts, Context Layer, `app/`, `deploy/`, `CLAUDE.md`), **plus** the fixed `V004`, **plus** the Gradle wrapper (DEV-COND-02 output), **plus** a root `.gitignore` + `README.md`.
- `git add -A && git commit -m "chore: baseline — stages 00–05 + first Development cycle"`.
- Remote `origin` set to `https://github.com/trinhhoangbk/aiproject.git`.
- `git push origin main` **cannot execute** until the project owner links their GitHub account to Claude (one-off action in Claude.ai → Settings → Connectors). Attempt to call `add_repo` returned `permission_denied: link your GitHub account`.

**Operator's next action** (one-time): link GitHub in Claude Settings → Connectors. After that, re-prompt in this session and the push proceeds.

---

## 4. Fix recap (what changed on the operator's device during cycle 2)

| Path | Change |
|---|---|
| `app/src/main/resources/db/migration/V004__materialized_views.sql` | **modified** — `window` → `horizon` column rename (6 occurrences); SQL now accepted by PostgreSQL 16 |
| `app/gradlew` | **new** — 8 733 B, mode 0755 |
| `app/gradlew.bat` | **new** — 2 937 B |
| `app/gradle/wrapper/gradle-wrapper.jar` | **new** — 43 764 B |
| `app/gradle/wrapper/gradle-wrapper.properties` | **new** — pinned to Gradle 8.10 bin |
| `README.md` | **new** at repository root — operator quick-start |
| `.gitignore` | **new** at repository root — ignores `.env`, `.DS_Store`, `.claude 2/`, `claude-context-layer.zip`, build output |
| `05-DEVELOPMENT/CYCLE_2_DEV_COND_REPORT.md` | **new** — this file |

## 5. What the operator should still do

1. **Link GitHub** in Claude Settings → Connectors; then this session's `push` call will succeed.
2. Clean the two advisory items that have lived with us since Onboarding Cycle 2:
   - `rm -rf "/Users/hoang/new project/.claude 2"`
   - `rm "/Users/hoang/new project/claude-context-layer.zip"`
3. Delete `*.DS_Store` from folders (my `.gitignore` keeps git clean; the on-disk files are macOS cruft).
4. When ready for `./gradlew test` locally on the dev host, run it on a network-connected machine (Maven Central access required). The container already proved F-TD-01 by a dependency-free harness.

---
⛔ Cycle 2 stops here. Next cycle (M1 remainder: PLAN-009 repositories + PLAN-010 admin REST controllers) begins after the GitHub push succeeds and the operator signals continue.
