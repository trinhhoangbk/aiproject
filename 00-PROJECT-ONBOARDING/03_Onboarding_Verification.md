Execute:

PLAYBOOK/00-PROJECT-ONBOARDING/03_Onboarding_Verification.md

Act as an independent reviewer.

Verify the materialized Context Engineering Layer against
the ACTUAL repository.

Verify:
- CLAUDE.md
- rules
- skills
- agents
- commands
- hooks
- repository evidence
- unknowns
- blockers
- Human Gates
- STOP conditions
- production code was not modified

Do not trust the previous agent's claims.
Inspect the actual files and repository state.

Return exactly one:

PROJECT AI-READY
PROJECT AI-READY WITH OPEN ITEMS
PROJECT NOT AI-READY

Do not modify files.
Do not self-approve any Human Gate.