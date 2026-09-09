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

Compilation, unit tests, source-text verification, signing and device testing have not yet been performed for this level. The source is not release-ready.

## Level 0.8: offline Dhikr counter — source committed, unverified

- Added a dedicated counter screen with a large touch target, independently animated progress ring, personal target, session count, today's actual count and lifetime total.
- Added three editable starter phrases, custom phrases and user-defined targets from 1 to 100,000. These are personal counting aids, not religious prescriptions or promises of reward.
- Added optional saved haptic feedback, reduced-motion compatibility, confirmation before resetting a session, archive and restore controls, and an accessible route through Settings.
- Added Room version 3 with separate Dhikr phrase and dated-count tables. Explicit migration 2-to-3 retains the existing 1-to-2 chain. No destructive migration, package change or signing change.
- Counting is transactional. Session resets do not clear daily/lifetime history; archived phrases retain records. A new local date receives its own count without deleting earlier dates.
- Used Room Upsert rather than REPLACE for phrase edits to prevent foreign-key cascades from deleting dated history.
- Extended JSON backup format to version 3, including phrases, session counts and dated records. Versions 1 and 2 remain readable. Replacing from an older backup preserves current Dhikr data instead of erasing a module absent from that backup.
- Merge retains local records on identifier/date conflicts and does not add counts together. Replacement writes a recovery snapshot before changing data, with the snapshot and database replacement serialized in one transaction.
- Added pure counter and backup tests plus Android instrumentation tests for persistence, session reset, archive/restore and the Room 2-to-3 migration.

### Deferred verification

No APK build, Gradle compilation, unit-test execution, instrumentation run or device installation was performed for level 0.8. Room schema generation and migration validation, full backup restore regression testing, responsive layout review and final signing checks remain release gates. The existing versionCode and stable development signing identity are unchanged. This is development source, not a verified installable release.

## Remaining levels

Continue with verified Islamic content and Hadith review, optional NUR AI, notification scheduling, accessibility/localization, privacy and backup hardening, widget behavior, advanced productivity tools and final integration. Inspect existing implementations before changes to avoid duplicating working features. Each level receives separate source commits and review.

The final build gate includes compilation, unit tests, migration and backup-restore tests, APK signing verification, and installation testing. Existing user data must not be erased to work around a signing or migration problem. No feature is considered release-ready merely because source has been committed.
