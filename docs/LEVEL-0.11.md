# Level 0.11 — Dhikr experience and genuine history

Status: source committed, not compiled or device-tested. The user explicitly requested no build for this level.

## Scope

- Rebuilt the Dhikr screen with the established NUR panels, typography, personal-goal ring, large count action, accessible progress, independent selected-phrase state and optional saved-count haptics.
- Added a read-only history projection from actual `dhikr_days` records. It filters by phrase, excludes zero-count records and malformed dates, sorts by recorded date, and offers all-recorded and inclusive last-30-day views. History is displayed in bounded pages of 20 rows; no missing days or counts are synthesized.
- Exposed dated records through the existing ViewModel and serialized Dhikr mutations with a coroutine Mutex. A tap captures its local date before waiting for the lock; pending counts are visible, and editing, reset, archive and restore controls wait for pending counts to drain. The database remains authoritative.
- Added save feedback, keyboard-aware editor layout, personal-goal validation, unsaved-edit discard confirmation, reset/archive confirmation, and an accessible archive/restore section.
- Added `DhikrHistoryTest` for phrase isolation, date ordering, exact recorded totals, the inclusive 30-day boundary, no invented dates, and unchanged history after session reset/archive. Tests have not been executed.
- Carried the exact motion API compatibility fix from the successful 0.10 preview back to the development branch. Closed temporary preview PR #4 unmerged after preserving its successful artifact and source branch.

## Data and compatibility

No Room schema, migration, backup format, application ID, versionCode, or signing configuration was changed in this level. Existing Dhikr records and earlier history remain intact. The development branch retains its current version configuration; the previous preview version bump was not silently copied into it.

## Deferred verification

The 0.10 preview passed compilation, unit tests, APK assembly and signing verification in GitHub Actions run 34309266603. That result does not validate the new 0.11 changes.

Before a future build, verify Kotlin compilation and the new tests, rapid multi-tap counting, count/reset and archive races, queued count cancellation during navigation, midnight and timezone changes, record restoration, large-text/320dp layout, TalkBack, long history lists and memory usage. Verify database migration and backup restoration using existing data before any release. If further concurrency changes are required, preserve every accepted count without inventing an optimistic total.

The original NUR and NUR-material-3 repositories remain untouched. No workflow was dispatched, PR opened, APK produced, or main-branch merge performed for 0.11. The temporary preview PR was closed to keep source-only development separate from CI. Continue subsequent levels on `development/next-levels` without building until the user asks.
