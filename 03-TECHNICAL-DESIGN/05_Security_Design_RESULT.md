# 05 — SECURITY DESIGN · Result

| Field | Value |
|---|---|
| Stage | 03-TECHNICAL-DESIGN / 05 Security Design |
| Date | 2026-10-04 16:08 ICT |
| Role | Security Architect (AI) |
| **Status** | **🟡 SECURITY DESIGN COMPLETE WITH OPEN ITEMS** |

> This design does **not** issue `SECURITY APPROVED`. It defines the controls that a human security owner must sign off. v1.0 target is a developer-machine prototype, not a production internet-facing deployment at MBS; threats and controls are sized accordingly.

---

## 1. Security Scope

Spring Boot application running on a developer host, reachable to Jira Cloud over a public tunnel; stores API Token and session secrets; writes back to Jira. One Jira Cloud Free site, up to 10 users (per F-ARCH-NEW-01 resolution Option B). No production customer data; data classification is **Internal**, elevated to **Confidential** for `.env` secrets.

Out of scope: production hardening, DLP, red-team exercises, PCI/SOC2 compliance work — these belong to a later production stage.

## 2. Assets & Sensitive Data

| Asset | Sensitivity | Location |
|---|---|---|
| Jira API Token (service account) | **Confidential** | `.env` on dev host (0600) |
| Jira webhook secret (HMAC key) | **Confidential** | `.env` on dev host (0600) |
| Spring Boot session cookie | Transit secret | Browser (HttpOnly) |
| DB credentials | **Confidential** | `.env` + Postgres role |
| Audit log (business trail) | Internal | Postgres + Kafka `hub.audit` |
| Jira issue/worklog projection | Internal | Postgres |
| Member PII (email, display name) | Internal | Postgres `core.member` |
| Capacity & workload data (reflects member performance) | Internal | Postgres |

## 3. Actors / Identity / Trust Boundaries

| Actor | Identity | Trust |
|---|---|---|
| Admin | Hub session (first user bootstrapped via `HUB_BOOTSTRAP_ADMIN_EMAIL`) | High |
| Manager | Hub session, role in `core.member.role` | High |
| Member | Hub session, role=MEMBER | Medium (self-only scope) |
| Jira Cloud | Signed webhook (HMAC) + outbound IPs listed by Atlassian | High for signed payloads |
| Operator (dev host user) | OS login on dev host | High (owns the whole stack) |
| Public internet (via tunnel) | None | Hostile by default |

**Trust boundaries:**
1. OS boundary (dev host) ↔ tunneled public endpoints.
2. Hub ↔ Jira (TLS, API Token).
3. UI ↔ Hub (same origin; session cookie).
4. Hub ↔ Postgres (local socket or TCP + password).
5. Hub ↔ Kafka (local socket or TCP; v1 no auth — localhost only).

## 4. AuthN Design

### 4.1 User authentication
- Spring Security form login with username + password; passwords stored as **BCrypt** (strength 12) in `core.member.password_hash` (added to the schema — this supersedes the omission in 03 DB; see Open Item SEC-O-01 and the DB addendum below).
- Session cookie: `HttpOnly`, `Secure` (when behind the public tunnel over HTTPS), `SameSite=Lax`.
- Session idle timeout: 30 min; absolute timeout: 8 h.
- Lockout after 10 consecutive failed logins (per account, 15-min lockout window).
- No MFA in v1.0 (prototype); flagged for production.

### 4.2 Jira service-account authentication
- Email + API Token in HTTP Basic header per Atlassian docs; token stored only in `.env` (0600) and injected via `JIRA_API_TOKEN`.
- Token rotation: manual, documented in `docs/runbook.md`. Rotation triggers: suspected leak, employee churn on the owner of the Atlassian account.
- The service account's Atlassian user is the single Jira identity used for all Hub reads **and** writes. DEC-006 excludes bot accounts from team-MD so the service-account activity does not pollute metrics.

### 4.3 Jira webhook authenticity
- HMAC-SHA256 over the raw body using `JIRA_WEBHOOK_SECRET`; header `X-Hub-Signature` compared in constant time (`MessageDigest.isEqual`).
- Mismatch → 401, no Kafka publish, metric incremented.
- Secret rotation: regenerate the webhook in Jira; update `.env`; roll Hub.
- Optional IP allow-list for `/webhooks/jira` using Atlassian's published outbound IP ranges — recommended on the tunnel/reverse proxy.

## 5. AuthZ Design

### 5.1 Enforcement points
1. Spring Security `SecurityFilterChain` on every HTTP request.
2. `@PreAuthorize` on every controller method AND every service method that returns member data.
3. Data-level filter in repositories: when the caller role is MEMBER, every query is scoped by `member.id = currentUser.id`.
4. `/webhooks/jira` is **not** session-protected; only HMAC-authenticated.

### 5.2 RBAC matrix (summary)

| Resource / Action | Admin | Manager | Member |
|---|---|---|---|
| View own workload / daily / overdue | ✓ | ✓ | ✓ (self) |
| View any member's workload | ✓ | ✓ | ✗ |
| View heatmap / pipeline | ✓ | ✓ | ✗ |
| Create / edit pipeline project | ✓ | ✓ | ✗ |
| Assign / Re-assign | ✓ | ✓ | ✗ |
| Toggle Lock Deadline | ✓ | ✓ | ✗ |
| Edit roster | ✓ | ✓ | ✗ |
| Edit capacity (any tier) | ✓ | ✓ (per DEC-004) | ✗ |
| Edit holiday calendar | ✓ | (Operations Lead subrole; see §5.3) | ✗ |
| Edit allow-list | ✓ | ✓ | ✗ |
| Edit skill taxonomy | ✓ | ✓ | ✗ |
| Read audit log | ✓ | ✗ (deferred post-prototype) | ✗ |

### 5.3 Subroles
DEC-003 names "Operations Lead" as a Holiday-Calendar co-owner. For v1.0 this is modeled as a flag on `core.member.operations_lead BOOLEAN` so a Manager-with-flag can edit calendars without becoming Admin.

### 5.4 Deny by default
Any endpoint not explicitly annotated denies. Any field returned to a Member role is first stripped of peer identifiers by the response mapper.

## 6. Input / Data Protection

- All controllers validate inputs with Bean Validation (JSR-380) annotations; invalid → 400 ProblemDetails.
- No raw SQL concatenation; JPA / jOOQ parameter binding only.
- JSON parsing with Jackson in default (safe) mode; polymorphic deserialization disabled.
- File upload / arbitrary content: **not supported** in v1.0.
- Jira webhook body size cap: 1 MB; larger → 413.
- CSRF: Spring Security default enabled for the UI; disabled only for `/webhooks/jira` (external).
- CORS: default-deny; allow own origin only.
- XSS: no server-rendered HTML; SPA uses standard framework escaping.

## 7. Secrets / Crypto References

- Secrets backend v1.0 = **`.env` on dev host (perm 0600, owned by Hub process user)** (O-10 decision, record §5).
- Secrets never logged; `logback` masking filter for keys matching `token|secret|password`.
- Session cookies rely on Spring Security's default secure random; cookie value is opaque.
- At-rest encryption: dev-host disk encryption (OS-level) — operational responsibility, not application-level.
- In-transit: HTTPS everywhere, including the tunnel. Tunnel vendor must terminate TLS at edge (ngrok and Cloudflare both do by default).

## 8. Logging / Audit

- Application logs: structured JSON (logback-spring.xml) to stdout; retained by the dev host's log rotation.
- Audit log (business trail): `audit.audit_log` + `hub.audit` Kafka topic (short retention per F-ARCH-NEW-02 Reading A).
- Sensitive fields never logged. Masking rules in logback config.
- Each HTTP response carries `X-Request-Id`; this id is recorded in audit entries.
- Failed authentication events logged at WARN with username (hashed) and source IP.

## 9. Threat & Abuse-Case Register

| ID | Threat | Precondition | Impact | Required control | Enforcement point | Verification |
|---|---|---|---|---|---|---|
| T-01 | Webhook spoofing (attacker POSTs forged `jira:issue_updated`) | Tunnel URL discovered | Fake issue state injected | HMAC verification + IP allow-list | Webhook Controller | IV-01, test with wrong HMAC |
| T-02 | Session hijack via XSS | XSS vulnerability in SPA | Takeover | HttpOnly cookies; CSP header; framework auto-escaping | Spring Security filter chain | DAST scan |
| T-03 | Horizontal privilege escalation (Member reads peer data) | Member authenticated | Confidentiality breach | `@PreAuthorize` + data-level filter + DTO sanitization | Controllers + repos | CV-07, dedicated unit test |
| T-04 | Vertical privilege escalation (Member hits admin endpoint) | Member authenticated | Compromise | `@PreAuthorize("hasRole('ADMIN')")`; deny by default | Controllers | CV-07 variant |
| T-05 | API Token theft from dev host | Attacker gets OS-level read | Full Jira write under service account | Perm 0600, OS disk encryption, rotation policy | OS + runbook | Red-team flagged for prod |
| T-06 | Jira write replay | Attacker intercepts Hub request | Repeated reassignments | TLS; same-origin UI; optional Idempotency-Key dedup | Hub API | Audit shows duplicate attempt |
| T-07 | SQLi via inputs | Any input field | DB compromise | Parameterised queries, Bean Validation | Repos | DAST + code review |
| T-08 | Mass-assignment via PATCH | PATCH endpoints | Role elevation | Explicit DTOs; whitelist of fields; separate role-change endpoint `/api/roster/{id}/role` restricted to Admin | Controllers | Unit tests |
| T-09 | Tunnel traffic sniffing | Tunnel vendor compromised | Confidentiality | TLS all paths; webhook secret rotation on vendor incident | Vendor + runbook | — |
| T-10 | Audit tampering | Attacker has DB access | Lose business trail | App role has `INSERT, SELECT` only on `audit.audit_log`; delete only by retention job role | Postgres grants | DB test |
| T-11 | Credential stuffing on login | Public tunnel URL | Account takeover | BCrypt + lockout + CAPTCHA (deferred post-prototype) | Spring Security | — |
| T-12 | SSRF via Jira adapter | Attacker influences Hub to call internal URL | — | Hub only calls hardcoded `JIRA_BASE_URL`; no user-supplied URLs. | Adapter | Code review |
| T-13 | Deserialization RCE | Attacker crafts payload | RCE | Jackson default mode; no polymorphic types enabled | Config | Code review |
| T-14 | DoS via webhook flood | Attacker spams `/webhooks/jira` | Resource exhaustion | HMAC gate rejects before heavy work; reverse-proxy rate limit; metric alarm | Tunnel + controller | — |
| T-15 | Over-privileged Jira API Token | Token has global admin | Collateral damage on leak | Scope the Atlassian account to least-privilege roles (Project Admin on allow-list projects, no site admin). | Atlassian config | Document in runbook |
| T-16 | Member sees discarded/sensitive resolution content | Pulled projection includes labels | Minor | Response mapper strips labels for MEMBER role; discarded flag shown without the resolution text when sensitive. | Mapper | Unit tests |

## 10. Required Controls

| Control | Where | Priority |
|---|---|---|
| HMAC verify on `/webhooks/jira` | Webhook Controller | MUST |
| BCrypt-12 password hashing | Spring Security `PasswordEncoder` | MUST |
| Spring Security form login + session + CSRF | Filter chain | MUST |
| `@PreAuthorize` on every controller + sensitive service method | annotations | MUST |
| Data-level filter for MEMBER role | repository layer | MUST |
| `.env` 0600 for secrets | OS | MUST |
| Masking of secrets in logs | logback filter | MUST |
| Parameterised queries only | JPA / jOOQ | MUST |
| Deny-by-default Spring Security + CSRF enabled | filter chain | MUST |
| Append-only audit grants in Postgres | DB | MUST |
| Explicit DTOs on PATCH endpoints | controllers | MUST |
| Login lockout after N failed | Spring Security | SHOULD |
| Reverse-proxy IP allow-list for webhook endpoint | tunnel config | SHOULD |
| CSP header on SPA responses | filter | SHOULD |
| MFA, DLP, CAPTCHA, WAF | Deferred post-prototype | NICE-TO-HAVE |

## 11. Security Verification Plan

| ID | Test | Type |
|---|---|---|
| SV-01 | Wrong HMAC webhook rejected | Unit + integration |
| SV-02 | Member cannot GET /api/workload/{otherMember} | Integration (403) |
| SV-03 | Member cannot POST /api/pipeline | Integration (403) |
| SV-04 | PATCH /api/roster/{id} payload with `role` field by Manager returns 400 (DTO whitelist) | Integration |
| SV-05 | SQL injection probe on filter params → safe (parameterised) | Automated DAST |
| SV-06 | 10 failed logins → account locked for 15 min | Integration |
| SV-07 | Audit insert with app role succeeds; DELETE/UPDATE fail | DB test |
| SV-08 | Session cookie `HttpOnly=true`, `Secure=true` behind HTTPS | Integration |
| SV-09 | Password stored as BCrypt | DB inspect |
| SV-10 | `.env` perm equals 0600 | Dev-host checklist |
| SV-11 | Logs do not contain token/secret substrings during a full test run | Log scan |

## 12. Risks / Decisions Required

| ID | Severity | Item |
|---|---|---|
| SEC-R-01 | HIGH | Dev-host deployment is reachable from the internet via tunnel. Must have HTTPS and IP allow-list for `/webhooks/jira`; the UI side should be unreachable from the public tunnel (serve UI on `localhost` only; expose only `/webhooks/jira` publicly). |
| SEC-R-02 | MEDIUM | Jira API Token stored on dev host is a long-lived secret; rotation discipline is operator-dependent. |
| SEC-R-03 | MEDIUM | No MFA on Hub login; acceptable for prototype on dev host behind restricted tunnel; blocker for production. |
| SEC-R-04 | LOW | Kafka and Postgres run without auth on localhost; acceptable while bound to loopback. |
| SEC-O-01 | **Decision**: add `core.member.password_hash` column — see §4.1. Supersedes 03 DB omission. | Confirmed as prototype control. |
| SEC-O-02 | OPEN | Should the SPA bind to `localhost` only (not reachable via tunnel), or should the whole app be exposed? Recommendation: expose only `/webhooks/jira`. Confirm with operator. |
| SEC-O-03 | OPEN | Tunnel vendor choice (ngrok paid / Cloudflare Tunnel). Both acceptable. |

## 13. Traceability

| REQ / AC | Threat / Control |
|---|---|
| REQ-008 / AC-008.1/2/3 | T-03, T-04, T-08; controls §4, §5 |
| DEC-008 write-back | T-05, T-06, T-15 |
| REQ-007 webhook | T-01, T-09, T-14 |
| DEC-013 audit retention (Reading A) | §8 + T-10 |
| Member peer visibility (DEC-014) | T-03, T-16 |

## 14. Status

```
SECURITY DESIGN COMPLETE WITH OPEN ITEMS
```

---
⛔ **STOP — stage boundary.** Next: `06_Technical_Design_Review.md`.
*AI must not issue `SECURITY APPROVED`.*
