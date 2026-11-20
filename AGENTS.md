# CLEAN Lift (formerly Offline Workout Logger) — engineering instructions

## Collaboration and standing intent — 2026-10-16

Use recorded intent to choose reasonable defaults within authorized scope. The owner wants the workout app Android should have shipped with: comprehensive, data-backed, native, offline, and free of ads, distractions and exploitation. Act as primary engineer: select and implement complete incremental improvements that reduce common friction or improve useful capability, without waiting for a feature-by-feature instruction. Prefer the everyday start/log/review/repeat loop before niche additions. Comprehensive means coherent coverage with progressive disclosure, not a crowded interface.

Report consequential decisions and tradeoffs. Ask only when recorded intent cannot resolve a material ambiguity or an explicit approval boundary below applies; prepare a concrete proposal first. Complete available local checks, batch manual acceptance, and continue independent authorized work while awaiting results. Manual acceptance remains required for claims about device UI/audio; pending acceptance alone does not block unrelated safe work. Preserve authorizations, constraints and remaining unknowns in project handoff documents. Project requirements belong here and in project docs, not global instructions.

## Start here

Read `docs/CONTINUE.md` for current verified state and the next task, then only the relevant feature note. Read `docs/PRODUCT.md` for scope, `docs/ARCHITECTURE.md` for file ownership, `docs/DECISIONS.md` for lasting choices and `docs/DEVELOPMENT.md` for the change workflow. Archived notes and milestone sections of START-HERE are historical evidence, not current task lists.

The owner is learning Android development. Act as primary engineer and concise guide. Explain manual steps with exact clicks or commands; explain tradeoffs in plain language. Avoid narrating routine edits.

## Product and authorization

- CLEAN Lift (`dev.clean.lift`, D-013; formerly Workout Logger): a dependable offline Android strength logger, with native Material 3 interaction, light/dark themes and minimal clutter.
- Priority: correctness, data durability, simple UX, maintainability, performance, visual polish, then features.
- Current tabs (D-012, 2026-11-19): Workout / Plans / Progress / Tools; fresh launch opens Workout. Settings opens from the gear on each tab root. Code keeps the older internal names (Templates, Metrics, Widgets) for files, preferences and backups. Exercises live under Plans; history under Progress.
- Visual direction: follow `..\DESIGN-PREFERENCES.md` (quiet screens, short wording, selectable palettes). Review and open items: docs/REVIEW-2026-11-19.md.
- Prior authorization covers incremental offline history/progress, body measurements, template convenience and equipment-aware planning. Widgets includes the specifically authorized calculators/timers, PACER/HAMR, Air Force practice assessment and five personal benchmarks, including documented Metrics/storage crossover. Read feature boundaries before changing them.
- Standing authorization includes proactively choosing evidence-informed offline improvements consistent with the intent above. Old backlog entries are candidates to assess, not automatic requirements. Preserve existing data and documented behavior unless the chosen change explicitly revises its contract. The former acceptance-only work restriction is superseded; batch outstanding manual checks while continuing safe implementation.
- No accounts, ads, subscriptions, analytics, telemetry, tracking, backend or embedded web content. No android.permission.INTERNET. User data stays on device. External source links may open in the normal browser; this grants no app networking permission.
- Explicit approval is required before adding/upgrading dependencies, SDKs, permissions, services or network functionality. Explain necessity and alternatives. Do not opportunistically modernize the toolchain. Development tool downloads do not authorize app networking.

## Safe change workflow

1. Inspect relevant code, tests, schemas and feature constraints. Reproduce reported bugs where feasible; otherwise state evidence and uncertainty.
2. State intended behavior and observable acceptance criteria. For scope changes record impact using docs/DEVELOPMENT.md. Resolve consequential unknowns before dependent implementation.
3. Implement one complete small slice, preserving unrelated behavior and user artwork. No broad rewrite or elaborate architecture for preference alone.
4. Run appropriate automated checks; fix straightforward failures. Report unexecuted checks honestly. Add regression tests for substantive deterministic/data bugs where valuable.
5. Update the relevant feature note and replace the current checkpoint in docs/CONTINUE.md. Archive significant prior checkpoints instead of appending conflicting current instructions.

Routine implementation choices do not need fresh approval. Clarify only material ambiguity affecting data, privacy, scope, cost, irreversible actions or externally visible behavior. Complete useful independent work if blocked.

## Technology and UI boundaries

- Kotlin, Jetpack Compose, Material 3, Room and AndroidX. Follow existing Gradle conventions and pinned versions. Build files are authoritative for versions; do not maintain a duplicate version list here.
- UI displays/collects input; ViewModels own lifecycle/state; repositories enforce validation and transactional mutations; pure Kotlin helpers own deterministic calculations. Enforce data rules below the UI too.
- Preserve navigation origin, tab/draft/scroll state and sensible Back behavior. Protect changed drafts. Failed saves stay editable; never show success before persistence succeeds.
- Keep Room I/O and bounded expensive calculations off the main thread. Prefer stable IDs, deterministic ordering and limited queries. Avoid speculative abstractions and unrelated cleanup.
- Use Material controls, accessible labels/tap targets, wrapping content, keyboard padding, large-text support and clear empty/error/loading states. User handles visual and subjective checks.

## Data durability — mandatory

- Never use destructive Room migration fallback, reset production data, uninstall the app, clear storage or wipe an emulator as a debugging shortcut. Tests must not use workout-logger.db.
- Schema changes need a preserving migration, retained exported JSON schemas, populated migration tests from supported prior versions, reopen checks and transaction/rollback tests where relevant. Explain how existing records survive. If database checks cannot run, label data verification incomplete.
- Preserve stable exercise/record IDs, exact entered units/numeric storage and original timestamps. Never remap old records by names or reinterpret old data using new metadata without an explicit reviewed policy.
- Names and plans snapshot into history as designed. Template/profile/source-record edits must not silently rewrite historical snapshots. Corrections are deliberate, record-scoped and preserve unrelated fields.
- Enforce active-workout, completion, one-action override and ownership rules in repositories. Multi-record saves/deletes remain atomic. Prevent duplicate saves using existing identities/tokens.
- Derived projections never overwrite source records. Keep entered results, estimates, units, formula/protocol versions and methods distinct. Unknown loads/inputs remain unknown.
- Verify authoritative sources before changing health/fitness scoring/formulas. Retain evidence, date/version, population and limitations in feature notes. Do not present estimates as measured maxima or clinical results.
- Explicit local JSON backup and confirmed replacement restore are implemented and authorized; preserve their recovery policy. Android automatic backup remains disabled. Merge, format conversion and automatic transfer require a concrete reviewed policy before implementation.

## Verification

From PowerShell in the project root:

```powershell
.\scripts\verify.ps1
# Add isolated data tests when the desktop emulator is running:
.\scripts\verify.ps1 -DatabaseTests
```

The script uses installed Android Studio Java and the existing Gradle cache; it builds the debug APK, runs JVM tests and lint. For connected tests check adb devices first; target only the desktop emulator (emulator-5554 by default), never the WiFi phone. Do not drive emulator UI automatically or add UI automation without user direction.

- Run build/JVM/lint after meaningful code changes. Add connected tests for schema, DAO/repository, durable preference or restoration changes as appropriate. Test calculations, boundaries, identities, migrations and rollback rather than mirroring implementation.
- Inspect the merged manifest after dependency/manifest/build-configuration changes for Internet, tracking or unnecessary permissions introduced transitively.
- Documentation-only edits need consistency/link checks, not an app build. Never present historical test results as newly run.
- Report known lint warnings; zero errors does not mean zero warnings. Do not suppress checks merely to obtain green results.
- Distinguish automated checks passed, reviewed but unexecuted, manual emulator acceptance pending and phone acceptance pending. Only a user report completes manual acceptance.

## Handoff and recovery

Report what changed, why, checks actually run, material limits and a short manual checklist when behavior changes. Android steps: open Android Studio, open this folder, start the existing device in Device Manager, select it in the top toolbar, click Run, then give specific actions/expected results. Do not restart initial setup as if unfinished.

Local Git repository since 2026-11-20 (branch `main`; remote `origin` = github.com/2AronB/CLEAN_Lift, **public**). Standing rule (owner, 2026-11-20): commit and merge to `main` whenever a slice passes all checks (`scripts/verify.ps1`, plus `-DatabaseTests` when the emulator is running) and no other checkout/worktree of the repo is in use. Pushing to the public remote still needs the owner's go-ahead. Before any push, check staged files for personal data (names, home paths, emails, device addresses, backups). `/tmp/`, logs and `Ideas.md` stay untracked; repo-local identity uses the GitHub no-reply address. Keep private data, generated output, signing secrets and local configuration out of version control. Preserve user edits. Code rollback is not database downgrade or device-data backup.

Keep one owner per topic: behavior in PRODUCT/feature contracts, rationale in DECISIONS, file ownership in ARCHITECTURE, current work in CONTINUE. Historical logs cannot grant new scope. New user instructions take precedence over old project guidance; reconcile affected docs without erasing decision history.
