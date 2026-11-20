# Development and change workflow

## Documentation ownership

| Question | Current source |
|---|---|
| How must agents work? | Root AGENTS.md |
| What does the app do/exclude? | PRODUCT.md and relevant feature contract |
| Why this lasting choice? | DECISIONS.md |
| Where is it implemented? | ARCHITECTURE.md and feature note |
| What was verified; what is next? | CONTINUE.md |
| What happened earlier? | archive/ and historical milestone records |

User instructions can change an earlier choice. Update current documents and mark old choices superseded. Brainstorms, backlog and historical logs are not standing task orders.

Standing intent as of 2026-10-16 (AGENTS.md and D-011) authorizes choosing and implementing useful incremental offline improvements without individual feature prompts. Prioritize common friction; preserve existing constraints. Batch manual acceptance and continue independent safe work; only the owner's report completes manual acceptance. Carry this authorization and unresolved consequential decisions in CONTINUE so another chat does not restart approval.

## Before a behavior change

For a small fix, capture expected behavior and acceptance in the task/checkpoint. For a material feature or changed requirement, add a short section to its feature note:

```text
Change / date:
Status: proposed | approved | implemented | automated verified | manually accepted
Requested behavior and authorization evidence:
Existing behavior affected:
In scope / explicitly deferred:
Acceptance criteria (observable examples, failure/boundary cases):
Data/schema/history implications:
Screens/navigation/state and related features affected:
Dependencies/permissions/privacy implications:
Tests to add/run and manual checks:
Recovery approach / remaining unknowns:
```

Track criteria separately when their statuses differ. A user request authorizes work within existing safety rules. Resolve consequential tradeoffs before dependent implementation; routine choices need no repeated approval. If an exploratory question leaves intent unclear, clarify scope briefly.

Later requirements changes should identify what changed, which old criteria still apply, affected code/data/tests and any replacement behavior. Revise criteria before coding and add focused regression coverage. Do not turn a small request into a broad redesign.

## Complete a slice

1. Inspect relevant files/evidence and reproduce bugs when feasible.
2. Choose the smallest coherent change, designing data preservation first.
3. Implement validation, failure handling and draft/state behavior too.
4. Run `.\scripts\verify.ps1`; add `-DatabaseTests` for data/restoration changes as appropriate. Report unavailable checks. Documentation-only edits need consistency/link checks.
5. Update feature contracts and replace the current checkpoint. Do not hand off a partial migration or misleading placeholder.
6. Give concise manual steps. Only the owner's report marks manual checks accepted.

## Checkpoints and recovery

Use local Git per app for future code changes to preserve reviewable file history. This workspace is a Git repository since 2026-11-20, with a public GitHub remote; pushing publishes, so it needs the owner's go-ahead. Keep generated output, signing secrets, private exports and machine configuration out of version control.

Git does not back up on-device data. Reverting code does not imply a newer database can be downgraded. Plan a tested forward fix or deliberate recovery for migrations. Never uninstall, clear storage or wipe the emulator to recover. Archived documents are evidence, not source control.

## Handoff

Replace CONTINUE's status rather than endlessly appending “latest” sections. Include date/slice, behavior, schema/dependency/permission impact, checks actually run/results, checks not repeated/reason, manual acceptance still pending and one next action. Archive significant older evidence. Keep file maps in ARCHITECTURE.
