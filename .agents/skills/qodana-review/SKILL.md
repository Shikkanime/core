---
name: qodana-review
description: Trigger this if any modifications are made to the codebase.
---

# Qodana-review

When any modifications are made to the codebase, trigger the skill `qodana-local` to collect the findings and fix them.

Count each scan with Qodana as one round. A new scan after fixes starts a new round.

Make the changes to the codebase to fix the findings and request a new scan.
Continue this process until all findings are resolved.