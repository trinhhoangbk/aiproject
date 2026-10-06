# CONTEXT APPROVAL · Record

| Field | Value |
|---|---|
| Date | 2026-10-04 15:54 ICT |
| Decision | **`CONTEXT APPROVED`** |
| Authorized by | Project owner (user) — in-session statement |

## Resolved baseline (frozen for Architecture)
- Jira Cloud + REST API v3 (C-13, C-14; ADF JSON for comment bodies — DEC-008).
- Hub platform: Java + Spring Boot (C-15).
- Datastore: PostgreSQL. Event bus: Apache Kafka. (C-16)
- Release: no CI/CD for v1.0; manual build and deploy (C-17).

## Carried as Architecture conditions
`O-03` Jira auth · `O-04` deployment target · `O-07` concurrent manager viewers · `O-08` HA/RTO/RPO · `O-09` DEC-013 audit retention · `O-10` security posture / secrets · `O-11` non-roster worklog treatment.

Architecture must flag these as inputs, not silently decide behaviourally.

## Next authorized file
`02_Architecture_Design.md`.
