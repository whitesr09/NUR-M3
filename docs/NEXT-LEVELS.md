# NUR-M3 development staging

Base: the successful 0.4 source at 33a90ba46273fe8e63fca6543c14a5530d7a19fa.

The user requested source-only development until the remaining features are added. This branch is intentionally not attached to a pull request and is not main. Do not merge it, open a PR, dispatch a workflow, or distribute an APK until the user requests the build. Static review is not a substitute for compilation or device testing.

## Level 0.5: expressive motion and progress — source committed

Added centralized reduced-motion behavior, date-keyed independent progress animations, smooth Daily Light rings and section indicators, completion feedback, and pure boundary tests. Preserved persistence, signing, package ID, navigation, and Room schema. Tests are committed but have not been executed.

## Level 0.6: offline Quran Reflections — source committed

Added a six-passage offline catalog, search and reading screen, references, reduced-motion-aware transitions, and navigation. Added a dedicated Daily Light calculation helper. Catalog text still needs external verification before release.

## Level 0.7: Daily Light reflection integration — source committed, unverified

- Connected the existing prayer + Amanah + Muhasaba progress helper to the actual Daily Journey screen. Rhythm and reading never inflate Daily Light.
- Added a dated reflection carousel with 10-second optional playback, previous/next controls, pause, and a direct Read action.
- The timer runs only while the relevant screen is resumed. Automatic playback is opt-in, so reading does not unexpectedly advance. Reduced motion replaces crossfades with direct updates.
- Added a reader route with an optional verse identifier. A selected passage opens expanded and scrolls into view; the Settings route remains compatible.
- Added pure tests for progress eligibility, previous-day isolation, and deterministic reflection selection.
- No database schema, signing configuration, package ID, or existing user data was changed.

Compilation, unit tests, source-text verification, signing and device testing have not yet been performed for this level. The source is not release-ready. Existing backups remain versioned; any future persistent companion data will need explicit backup/restore coverage.

## Remaining levels

Complete religious-source verification and Hadith review, optional NUR AI, notification scheduling, accessibility/localization, privacy and backup hardening, widget behavior, and final integration. Existing implementations must be inspected before changes to avoid duplicating or replacing working features. Each level receives a separate source commit and review.

The final build gate includes compilation, unit tests, migration and backup-restore tests, APK signing verification, and installation testing. Existing user data must not be erased to work around a signing or migration problem. No feature is considered release-ready merely because source has been committed.
