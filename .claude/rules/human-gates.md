# Rule: Human Gates & STOP Conditions

Sources: `00-PROJECT-ONBOARDING/01_Project_Onboarding_Gate.md`, `02_Onboarding_Approval_Apply.md`, `03_Onboarding_Verification.md`.

- AI proposes and reviews; only authorized humans approve.
- Never self-approve a Human Gate. Approval text that predates the proposal it approves is a template, not an approval.
- STOP at the end of every stage and wait for the next authorized instruction.
- Stage 02 materializes ONLY the approved Context Engineering Layer; it must not modify production application behavior or introduce facts not supported by repository evidence.
- Stage 03 is performed as an independent reviewer: do not trust previous agent claims, inspect actual files and repository state, do not modify files.
- Do not write production code unless authorized by the Development stage.
- Do not silently change an approved baseline; changes go back through a Human Gate.
