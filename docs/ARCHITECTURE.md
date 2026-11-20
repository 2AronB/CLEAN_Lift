# Architecture and feature ownership

Kotlin + Compose + Material 3; Room repositories own durable mutations, ViewModels own screen state, and pure Kotlin helpers own deterministic calculations. Private local preferences store settings. Keep this proportional to a small app; no new architecture framework is needed.

## Feature map

| Work | Read these files first |
|---|---|
| Capture / guidance | ui/WorkoutScreen.kt, ui/WorkoutViewModel.kt, data/WorkoutRepository.kt, data/GuidedPlan.kt |
| Templates / generator | docs/features/WORKOUT-GENERATOR.md, ui/GeneratorScreen.kt, ui/TemplatesScreen.kt, ui/TemplatesViewModel.kt, data/WorkoutGenerator.kt, data/GeneratorRepository.kt, data/TemplateRepository.kt |
| History | ui/HistoryScreen.kt, ui/HistoryViewModel.kt |
| Exercise charts/estimates | docs/features/PROGRESS.md, data/ProgressDao.kt, data/StrengthEstimates.kt, ui/LiftEstimateTools.kt (current); ui/ProgressScreen.kt (legacy, no entry) |
| Body measurements | docs/features/MEASUREMENTS.md, data/BodyMeasurements.kt, data/DailyMeasurements.kt, ui/MeasurementsScreen.kt, ui/DailyMeasurementsScreen.kt, ui/DailyMeasurementsViewModel.kt, ui/MeasurementDateField.kt |
| Template duplication/prior values | docs/features/TEMPLATE-CONVENIENCE.md |
| Equipment-aware planning / catalog | data/ExerciseCatalog.kt, docs/features/EXERCISE-CATALOG.md; EQUIPMENT-PLANNER-DESIGN.md, EXERCISE-CATALOG-RESEARCH.md, WORKOUT-GENERATOR-PLAN.md under docs/features (generator design only) |
| Schema / migrations | data/WorkoutDatabase.kt, data/Entities.kt, app/schemas, docs/LOCAL-DATA.md |
| Validation | data/SetValues.kt, data/ExerciseNames.kt, matching JVM tests |
| Root navigation | ui/WorkoutLoggerApp.kt (four tabs, Settings overlay), ui/AppBarButtons.kt (Back arrow, Settings gear) |
| Theme and palettes | ui/WorkoutLoggerTheme.kt (color schemes, typography), ui/Appearance.kt (Palette/ThemeMode, AppearanceStore in `local-profile`), MainActivity.kt (system bar style) |
| Workout rest countdown | ui/RestTimer.kt (pure timing + RestCard), ui/WorkoutScreen.kt |
| Widgets calculators / tool plan | docs/features/WIDGETS.md, widgets/WidgetCalculations.kt, widgets/WidgetCalculatorModel.kt, widgets/WidgetsScreen.kt |
| Metrics dashboard / calculated health / cardio / strength | docs/features/METRICS.md, ui/MetricsScreen.kt, ui/MetricsViewModel.kt, ui/HealthEntryScreen.kt, ui/MetricGuideScreen.kt, ui/StrengthPreferencesScreen.kt, data/HealthMetrics.kt, data/HealthGuides.kt, data/MetricSeries.kt, data/StrengthMetrics.kt |
| Local backup / replacement restore / startup recovery | docs/features/PORTABILITY.md, data/BackupRepository.kt, data/BackupPreferences.kt, ui/BackupScreen.kt, ui/BackupViewModel.kt, ui/RestoreAwareApp.kt, MainActivity.kt |
| Settings / local profile / CLEAN jokes | docs/features/SETTINGS.md, ui/SettingsScreen.kt, ui/SettingsViewModel.kt |

Paths above are relative to app/src/main/java/dev/clean/lift unless otherwise stated. Tests live in app/src/test and app/src/androidTest. Each new feature should have a short docs/features note listing ownership, invariants, queries, tests and next steps.

Verification shortcut: `scripts/verify.ps1` for build/JVM/lint; `scripts/verify.ps1 -DatabaseTests` adds isolated Android database tests using emulator-5554. Device must be running. Current metric/navigation manual checklist: docs/features/METRICS.md; earlier features: docs/ITERATION-1-TESTS.md. Charts/body tracking are now in Metrics; the older 5/10-rep estimates and break scenario are expandable inside each chosen-lift detail.


