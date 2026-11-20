# Exercise progress ownership

Read data/StrengthEstimates.kt, ui/LiftEstimateTools.kt and ui/TrendChart.kt; Metrics provides the completed-set stream from ProgressDao. Entry is the expandable tools section in each chosen-lift chart. Legacy ProgressScreen/ProgressViewModel are no longer navigation destinations. No schema change is needed for this feature.

## Rules

Only completed workouts contribute. One projection per workout/exercise combines repeated occurrences; grouping is by workout ID, so same-date workouts remain distinct. Dates use workout start time. Catalog IDs preserve continuity through renames; stored snapshot names remain in history. Convert lb to kg with 0.45359237 for comparisons and convert only for display. Never mutate recorded units.

Heaviest load considers every saved set. Estimates use only positive-external-load sets with exactly 5 or 10 reps, via Epley w × (1 + r/30). Rep filter affects estimates only; time filter affects chart/session list only. Best records remain all-time and show dates. Actual weight and estimated 1RM are explicitly separate metrics. Zero external weight cannot estimate total-body strength. Machine assistance, unilateral/per-dumbbell conventions and non-maximal effort are not automatically known.

Break scenario is deliberately user-chosen, default 0% loss. It compounds the assumed monthly rate over days since the latest eligible log, never changes recorded or estimated historical values, and is labelled what-if rather than predicted detraining. Lack of app records is not evidence of no exercise. No personalized physiological model exists here.

## Evidence and limits

- Epley and other submaximal prediction methods: [research comparing prediction models](https://pmc.ncbi.nlm.nih.gov/articles/PMC11435939/).
- Individual uncertainty in indirect 1RM predictions: [Sigvaldsen et al., 2023](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0288649).
- Detraining varies with age, training status and cessation duration: [Bosquet et al., 2013](https://pubmed.ncbi.nlm.nih.gov/23347054/).

These papers do not validate a universal personal monthly-loss rate. Scenario rates are user assumptions, not derived recommendations.

Opening a session from a chart records its navigation origin. Back from details returns to that exercise/chart with unit, rep and period filters retained via a child saveable-state holder. History corrections update the query projection when the chart returns. Numeric estimate session rows provide an accessible alternative to Canvas-only values.

## Tests and extensions

StrengthEstimatesTest: exact formula, valid attempts, filters, conversion, scenario compounding and bounds. ProgressDataTest: mixed-unit/repeated-entry grouping, active exclusion, corrections reflected, literal history search and bodyweight exclusion. Canvas chart is intentionally small and dependency-free; accessible session rows provide exact values. UI/large-font/dark-mode checks remain manual.

Potential next work: optional effort/RPE qualifiers, per-exercise estimate eligibility, rep-count records beyond 5/10, point selection on chart and measured performance with multi-year data. Do not silently change formula interpretation or chart date conventions.

## Consolidation — 2026-10-15 (supersedes the legacy entry above)

User requested consolidating the older tools into lift details. Current entry: Metrics > choose a lift chart > 5/10-rep estimates & break scenario, above the recorded-value list. No standalone Lift estimates overview entry. Rep choice, expansion, scenario slider and pagination save within the originating detail state. Session links open History and return to that same lift/detail. The main 2–10-rep method and the older exact-5/10 method remain distinctly labelled.
Ownership: ui/LiftEstimateTools.kt and StrengthEstimates.progress. Reuses Metrics' existing completed-set stream; no new queries/schema. Legacy ProgressScreen/ViewModel remain source history but are no longer navigation destinations. Date range controls are shared with the main chart; all-time best/latest remain explicitly labelled. Scenario starts at 0%, remains user-chosen and never alters saved records.
Automated: build, 105 JVM tests and lint passed 2026-10-15. New projection regression covers repeated lift occurrences, other-exercise exclusion, same-time distinct workouts, zero load and missing data. Earlier isolated 44-test data run covers unchanged source queries.
Manual: open a chosen lift, expand tools, select 5 reps, expand the break scenario and choose a rate. Open one estimate's workout, correct a set and Back; filters/scenario must remain and values must update. Rotate and change date range; exact numeric session rows must remain accessible. Visual/phone acceptance pending.
