# 10-TESTING — AI AUTOMATED TESTING WORKFLOW

`ACTUAL DIFF VERIFIED → Test Analysis → Test Case Generation → Automated Test Implementation → Automated Test Execution → Failure Analysis (when needed) → Test Evidence Review → TEST EVIDENCE VERIFIED`

Core rules: `NOT RUN != PASS`; `GENERATED != EXECUTED`; `NO EVIDENCE = NOT VERIFIED`; never weaken a valid test to make the build green. Step 11 remains responsible for cross-component/business-flow Integration & E2E verification.
