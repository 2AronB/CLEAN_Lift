# Body measurement ownership

Read data/BodyMeasurements.kt, ui/MeasurementsViewModel.kt, ui/MeasurementsScreen.kt. Shared plot: ui/TrendChart.kt. Entry: Metrics > body metric chart, or Metrics > Daily measurements for several new values.

Schema 4 adds body_measurements only. Original workout/exercise/template tables are untouched. Unique metric/epochDay prohibits duplicate entries for one metric on a calendar day. Inserts abort on conflict; edits update by stable ID and cannot change metric. Deletion affects that record only. Mass units kg/lb and circumference units cm/in must not cross. Stored values are exact integer thousandths of their entered unit; charts convert only for display.

Dates are plain calendar days represented as UTC epoch-day integers, not measurement timestamps. Entry uses the shared native calendar, 1970 through device-local today; internal date text remains strict YYYY-MM-DD. Formatting/plot labels preserve the day even across time zones. Values: positive through 1000 with at most three decimals. This is a broad input bound, not a physiological recommendation. Bodyweight, waist/chest/hips/neck, and left/right biceps/thighs/calves are separate metrics. One entry per day is an intentional simplification.

Changing the unit in the editor labels the number entered; changing it in the chart converts the display. Duplicate-date edits and invalid values keep the form open and preserve prior saved data. Drafts survive rotation; force-stop is not guaranteed to preserve unsaved edits. Plots and change-from-first figures show recorded values without targets or judgments.

Tests: MeasurementInputTest (strict dates/leaps/bounds/conversion), MeasurementDataTest (reopen persistence, edits/deletion isolation, duplicate protection, wrong metric/unit rejection, populated schema-3 migration). Older migration tests cover full chains to version 4. Unit/keyboard/dark-mode/font-size usability remains manual.

Unimplemented ideas (not task orders): optional measurement notes/custom metrics and explicit measurement-technique labels. Date pickers, Metrics date-range controls and local backup/restore are implemented. Do not silently combine left/right or overwrite duplicate days.

## Daily form and native calendar — 2026-10-15

Implemented under the explicit four-suggestion request. Entry is Metrics > Daily measurements; single-entry edits remain under each body-metric chart (the old History entry above is historical). Native calendar controls cover single/daily measurement dates, health dates and Metrics custom ranges. Picker dates use UTC calendar days, limited to 1970 through local today.
Daily fields start blank in the preferred display units. Only nonblank fields are inserted; existing metric/day entries are never replaced. Changing date retains the draft. Conflicts are identified on the form; repository revalidation and one transaction protect against concurrent conflicts or invalid input. Any failure rolls back the whole batch. Save/discard returns to Metrics; changed numeric drafts are protected and survive rotation. Calculated health snapshots are not generated or changed.
Ownership additions: data/DailyMeasurements.kt, MeasurementRepository.saveDay in BodyMeasurements.kt, ui/DailyMeasurementsScreen.kt, ui/DailyMeasurementsViewModel.kt and shared ui/MeasurementDateField.kt. Schema remains 8; no dependencies/permissions.
Automated verification 2026-10-15: build, 104 JVM tests, lint (0 errors/32 warnings), all 44 isolated emulator database tests passed. New tests cover skip-blank/exact units, duplicate and invalid batches, transactional conflict rollback, unrelated-data preservation and reopening.
Manual: Metrics > Daily measurements, choose yesterday, enter bodyweight and waist, leave other fields blank, save and verify both charts. Try that same date again: existing metrics must flag conflicts; clear those fields to save a different metric. Change a value and press Back to test keep/discard; rotate with the date picker open. No manual acceptance inferred.
