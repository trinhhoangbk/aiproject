# ARCHITECTURE APPROVAL — CLARIFICATIONS

| Field | Value |
|---|---|
| Date | 2026-10-04 16:08 ICT |
| Supersedes | §2.2, §3, §2.3 of `ARCHITECTURE_APPROVAL_RECORD.md` |

## 1. Jira Free seat cap (F-ARCH-NEW-01) — RESOLVED
Project owner selected **Option B**: prototype on Jira Cloud Free (10-user cap); upgrade to Jira Standard **before** onboarding the 11th member. Procurement trigger: team size reaches 11.

## 2. Audit retention (F-ARCH-NEW-02) — RESOLVED
Project owner confirmed **Reading A**: audit log is still produced per DEC-008; no long-term retention mandate. Default v1.0 retention: Kafka topic `hub.audit` = 7 days, DB table `audit_log` = 30 days; operator may truncate at any time.

## 3. Hub deployment target (ADR-ARCH-011) — RESOLVED
Deployment target = **developer machine** (local dev host). Running via Docker Compose (Spring Boot + Postgres + Kafka). Single-user dev environment. Implications carried into Technical Design:
- Postgres single-node, Kafka single-broker.
- No HA at infra layer (consistent with "business-hours critical only").
- Backup = local disk + manual snapshot.
- **Webhook ingress to a dev host requires a public tunnel** (ngrok, Cloudflare Tunnel, SSH forward) because dev machines typically lack a public IP — flagged in Integration Design.
- Secrets live on the dev host filesystem (`.env` perm 0600) — flagged in Security Design.

ADR-ARCH-011 status now **LOCKED (prototype)**.

All open architecture conditions are resolved. Technical Design may begin.
