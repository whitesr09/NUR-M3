# NUR Material 3 — Product scope

NUR-M3 is a new native Android application, not a reskin of the old WebView app. Its identity is calm, accessible Material 3 with a black-blue and gold signature, optional system dynamic color, and a complete light theme. The old repositories are not modified.

## 1. Reliable foundation
- Five daily prayer checkboxes, with no invented prayer times.
- Daily Light progress: five prayers plus active Amanah/Muhasaba entries. When there are no custom entries, the five prayers are the entire denominator. Never count completed entries that are inactive or not part of the selected date.
- Amanah, Muhasaba, Rhythm, genuine history, stable navigation, and persistent settings.
- Task and reflection definitions survive midnight. Completions are date-scoped. A new day starts unchecked, while past records remain. Deletion and archival must not silently erase historical evidence.
- Preserve existing NUR-M3 data through explicit Room migrations and stable application/signing identity. The original NUR V2 database is a separate app and is not automatically migrated.
- Unit tests for date boundaries, progress, recurrence, persistence migrations, and deletion/history behavior.

## 2. Signature experience
- Appearance Studio: light/dark/system modes, signature and dynamic palettes, typography scale, motion controls, card density and shape.
- Daily Journey: a configurable home layout with reorderable and hideable cards, prayer-first and focus-oriented presets, and progress that is derived from the same stored data as all other screens.
- Refined Daily Light: accessible concentric progress rings, Arabic calligraphy, a calm gold treatment, and sourced Quran/Hadith content. Any content collection must be verified before inclusion; no fabricated quotations.
- Expressive Material motion with reduced-motion support. Animations must not alter or duplicate saved progress.

## 3. Power features
- Recurring tasks, optional reminders and Android widgets.
- Insights using genuine dated records, export/import backups, privacy controls, optional app lock, and optional companion modules.
- Optional NUR AI using Gemini, with a user-supplied credential stored privately, explicit network consent, clear source citations and uncertainty handling. Never ship a shared API key in a public APK.
- Prayer-time calculations only after adding explicit location and calculation-method configuration, timezone handling, and verification. Do not present manual checklists as a calculated timetable.

## Delivery rules
Ship vertical slices that compile and pass tests. Do not label a feature complete because a screen or placeholder exists. A green CI build verifies compilation and tests, not all on-device behavior. Keep the last successful APK available while new work is reviewed. Increase versionCode for installable updates and preserve the application's identity. No production signing key is committed to Git.
