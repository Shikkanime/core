---
name: qodana-kotlin-analysis
description: Run Qodana on modified Kotlin code before sub-agent review.
version: 1.0.0
author: Ziedelth
license: MIT
platforms: [linux, macos]
metadata:
  hermes:
    tags: [qodana, kotlin, static-analysis, shikkanime]
    related_skills: [development-workflow, shikkanime-core-backend]
---

# Qodana Kotlin Analysis Skill

## Overview

This skill defines the mandatory static analysis step for all Kotlin developments across Shikkanime.
Whenever Kotlin code is added, modified, or refactored, Qodana must be executed locally before submitting
the work to sub-agent code review. Any issues flagged on the modified code must be fixed.

## When to Use

- When finishing any Kotlin development (feature, fix, refactor) before delegating to review sub-agents.
- Before opening a pull request for Kotlin modules.

## Prerequisites

- JDK 21 installed and configured in `JAVA_HOME` (`/opt/data/home/jdk`).
- Gradle toolchain path configured: `org.gradle.java.installations.paths=/opt/data/home/jdk` in
  `gradle.properties`.
- Qodana CLI installed at `~/.local/bin/qodana`.

## Execution Command

Run Qodana in native mode (without Docker):

```bash
JAVA_HOME=/opt/data/home/jdk \
PATH=/opt/data/home/jdk/bin:$PATH \
GRADLE_USER_HOME=/opt/data/.gradle \
~/.local/bin/qodana scan \
  --within-docker false \
  --linter qodana-jvm-community \
  --profile-name qodana.recommended \
  --print-problems
```

## Remediation Rules

1. **Strict Scope Limit**:
   Inspect reported problems and match them against the total diff of the branch (`git diff origin/<base>`).
   Only fix problems located on files and lines touched by the current development.
   Do NOT modify unrelated pre-existing code outside the scope of the feature.

2. **Quality Gates**:
   - Zero High / Moderate severity issues on the modified code.
   - Code formatting must strictly respect project guidelines (expression body on line after `=`,
     line length <= 110 characters, required KDoc).
