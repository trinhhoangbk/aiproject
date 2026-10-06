# 02 — DEPENDENCY ANALYSIS · Result (re-run)

| Field | Value |
|---|---|
| Stage | 04-PLANNING v5.2 / 02 Dependency Analysis |
| Date | 2026-10-06 10:00 ICT (re-run) |
| **Status** | **🟢 DEPENDENCY ANALYSIS COMPLETE** |

---

## 1. Summary
Small, well-known dependency footprint: 2 toolchain items, 1 BOM, ~16 libs, 3 local infra services, 1 external vendor (Jira Cloud Free), 0 cross-team ownership. No cycles.

## 2. Dependency Matrix

### 2.1 Toolchain
| ID | Dep | Version | Class |
|---|---|---|---|
| D-TC-01 | JDK | 21 LTS (Temurin / OpenJDK) | HARD |
| D-TC-02 | Docker Desktop | current | HARD |
| D-TC-03 | Git | current | SOFT |
| D-TC-04 | Tunnel (ngrok / cloudflared) | current | HARD |

### 2.2 Libraries (Gradle, Spring Boot BOM 3.3.x LTS)
Spring Boot (BOM) · Spring Web · Spring Security · Spring Data JPA · PostgreSQL JDBC · Spring Kafka · Flyway core + PG plugin · Jackson + jsr310 · Spring Boot Actuator + Micrometer · Hibernate Validator · Spring RestClient · springdoc-openapi (optional) · logback-classic · JUnit 5 + Spring Boot Test · Testcontainers (PG + Kafka) · WireMock / MockWebServer.

### 2.3 Infrastructure (local containers)
PostgreSQL 16 (HARD/RUNTIME) · Apache Kafka 3.7+ KRaft (HARD/RUNTIME) · Tunnel runtime (RUNTIME).

### 2.4 External
Jira Cloud **Free** site (test / prototype, ≤10 users; upgrade to Standard before 11th member) · dedicated service-account API Token · webhook registered on allow-listed projects only.

### 2.5 Organisational
Project owner authorises Atlassian service account + API Token + Jira webhook + plan approvals.

## 3. Dependency Graph (coarse)

```
D-TC-01 JDK ─┐
D-TC-02 Docker ──► M0 scaffold ─► M1 Flyway + core
                                     ├─► M2 Jira adapter (read)  (D-EXT-01, D-EXT-02, D-TC-04)
                                     ├─► M3 ingestion            (D-INF-02 Kafka)
                                     ├─► M4 materialized views
                                     ├─► M5 workload/overload/overdue
                                     ├─► M6 reporting + ETA
                                     ├─► M7 pipeline/balancing/assign (needs M2 writes)
                                     ├─► M8 security             (COND-02 password_hash in M1)
                                     ├─► M9 audit
                                     ├─► M10 SPA
                                     └─► M11 observability + M12 smoke
```

No cycle.

## 4. Hard Blockers before Development
| ID | Item |
|---|---|
| HB-01 | JDK 21 installed. |
| HB-02 | Docker Desktop installed. |
| HB-03 | Non-production Jira Cloud Free site chosen. |
| HB-04 | Service-account API Token issued. |
| HB-05 | Tunnel binary + reserved URL (named Cloudflare preferred for stability). |
| HB-06 | `password_hash` column present in V001 (TD-COND-02). |
| HB-07 | Tunnel ingress rule exposes **only** `/webhooks/jira` (TD-COND-01). |

## 5. Compatibility
Spring Boot 3.3 LTS ↔ Java 21 ✓ · Flyway 10 ↔ PG 16 ✓ · Spring Kafka ↔ Kafka 3.7 KRaft ✓ · Jira v3 ↔ Jira Cloud Free ✓ · ADF JSON ↔ Jira v3 comment ✓ · PG `REFRESH CONCURRENTLY` requires unique index per MV ✓ (F-DB-03).

## 6. Ordering Constraints
- Flyway forward-only; V001 → V002 → V003 → V004 → V005 → V006.
- V001 must include `password_hash` before Spring Security starts up (COND-02).
- V004 after V001/V002 (sources exist). Unique indexes before first `REFRESH CONCURRENTLY`.
- Jira webhook registered only after first tunnel URL known and endpoint reachable.

## 7. External / Org
Only project owner (Master) involvement required. No cross-team gate.

## 8. UNKNOWN / Risks
| ID | Risk |
|---|---|
| DEP-R-01 | ngrok free URL changes on reconnect → named Cloudflare Tunnel recommended. |
| DEP-R-02 | Jira Cloud Free API rate limits are unpublished; reconciler cadence adaptive on 429 (COND-04). |
| DEP-R-03 | Spring Boot 3.3 point release pinned in `gradle.properties`. |

## 9. Traceability
| Milestone | HARD deps |
|---|---|
| M0 | D-TC-01/02, D-LIB-01 |
| M1 | D-LIB-07, D-INF-01, COND-02 |
| M2 | D-LIB-11, D-EXT-01/02, D-TC-04 |
| M3 | D-LIB-06, D-INF-02 |
| M4 | D-INF-01, M1 |
| M5–M7 | M3 + M4 |
| M8 | COND-02, D-LIB-03 |
| M9 | D-INF-01, M8 |
| M10 | M5+M6+M7 live |
| M11 | COND-01, COND-03 |
| M12 | all prior |

## 10. Status
```
DEPENDENCY ANALYSIS COMPLETE
```

⛔ **STOP — stage boundary.** Next: `03_Implementation_Plan.md`.
