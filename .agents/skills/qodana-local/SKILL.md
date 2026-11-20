---
name: qodana-local
description: Run Qodana Community locally through Docker, collect every lint finding as structured data, and clean the output directories afterwards so they never pollute the commit.
---

# Qodana local lint workflow

Use this skill to lint the whole project with Qodana (the same engine as the IDE inspections)
without leaving generated artifacts in the repository.

## Prerequisites

- Docker running (`docker --version`).
- The `qodana` CLI on `PATH` (installed at `~/.local/bin/qodana`; it only orchestrates the
  `jetbrains/qodana-jvm-community` Docker image, first run pulls it).
- A `qodana.yaml` at the repository root selects the linter and profile. This project uses
  `linter: qodana-jvm-community` with `profile: qodana.recommended`.

## Run the scan

Always write results OUTSIDE the repository so nothing generated can be committed:

```bash
qodana scan -i . -o /tmp/qodana-results --print-problems
```

- `-i .` — project directory (the repo root).
- `-o /tmp/qodana-results` — results directory. Do NOT use the default
  (`~/.cache/JetBrains/...`) if you want a predictable path; either way it stays out of git.
- `--print-problems` — echoes every finding to the console (ANSI-colored, grouped by file).
- The scan takes a few minutes: image pull on first run, then project opening, configuration
  and inspection inside the container.
- Useful extras: `--clear-cache` to force a cold run, `-d <dir>` to scope to one module,
  `--commit <sha>` for a diff-only run.

## Read the findings

The machine-readable report is SARIF:

```text
/tmp/qodana-results/qodana.sarif.json      # full report
/tmp/qodana-results/qodana-short.sarif.json # condensed
/tmp/qodana-results/report/                # HTML report (open index.html)
/tmp/qodana-results/log/                   # inspection logs
```

Extract every problem with:

```bash
python3 - <<'EOF'
import json
d = json.load(open('/tmp/qodana-results/qodana.sarif.json'))
for r in d['runs'][0].get('results', []):
    loc = r['locations'][0]['physicalLocation']
    print(r['ruleId'], loc['artifactLocation']['uri'],
          loc['region']['startLine'], '-', r['message']['text'][:160])
EOF
```

`ruleId` is the inspection name (e.g. `UnusedSymbol`, `KDocUnresolvedReference`,
`ArrayInDataClass`); `level` is `error`/`warning`/`note`.

## Fixing findings

- Fix the code first; only adjust `qodana.yaml` `exclude` entries when the finding is a
  structural false positive (e.g. classes instantiated reflectively by Koin/Quartz — the
  project already excludes `UnusedSymbol` for `jobs/src/main/kotlin/impl` and
  `admin/src/main/kotlin/usecases/impl` for that reason).
- Document genuinely new exclusions with a comment explaining WHY the pattern is invisible
  to static analysis.
- Re-run the scan to confirm zero (or only accepted) findings, then `./gradlew test` and
  `tools/check_line_length.py` before committing.

## Cleanup — keep the commit clean

Results go to `/tmp` (or `~/.cache`), never into the repo, so there is normally nothing to
delete. If a run ever wrote inside the project (custom `-o`, CI artifacts), remove it:

```bash
rm -rf /tmp/qodana-results        # or the in-repo path used
# verify nothing generated remains tracked or untracked:
git status --short | grep -i qodana
```

`qodana.sarif.json`, `report/`, `log/` and `projectStructure/` are pure output — never commit
them.
