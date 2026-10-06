#!/usr/bin/env bash
# PLAN-COND-03 — fail when the frozen playbook stages (00-04) got touched since
# the previous commit. Development artifacts (05+) and app/ code are free to move.
set -euo pipefail

if git diff --name-only HEAD~1 2>/dev/null | grep -E '^(00-PROJECT-ONBOARDING|01-REQUIREMENT|02-ARCHITECTURE_FINAL|03-TECHNICAL-DESIGN|04-PLANNING)/' >/tmp/_drift; then
  if [[ -s /tmp/_drift ]]; then
    echo "ERROR: playbook stages touched — those are frozen baselines (PLAN-COND-03):"
    sed 's/^/  /' /tmp/_drift
    exit 1
  fi
fi
echo "OK: no playbook drift."
