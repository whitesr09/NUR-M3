# NUR-M3 development staging

Base: the successful 0.4 source at 33a90ba46273fe8e63fca6543c14a5530d7a19fa.

The user requested source-only development until the remaining features are added. This branch is intentionally not attached to a pull request and is not main. Do not merge it, open a PR, dispatch a workflow, or distribute an APK until the user requests the build. Static review is not a substitute for compilation or device testing.

## Level 0.5: expressive motion and progress — source committed

Added centralized reduced-motion behavior, date-keyed independent progress animations, smooth Daily Light rings and section indicators, completion feedback, and pure boundary tests. Preserved persistence, signing, package ID, navigation, and Room schema. Tests are committed but have not been executed.

## Level 0.6: offline Quran Reflections — source committed

- Added a small curated catalog of six Quran passages with stable references, Arabic text, original brief meaning summaries, and source URLs.
- Added an offline search and reading screen with expandable Arabic text, source links, and reduced-motion-aware transitions.
- Connected the reader to Settings and the existing navigation stack without changing database or signing files.
- Added deterministic date selection and pure catalog tests, committed but not executed.
- Added a dedicated Daily Light progress helper so Rhythm and optional companion modules do not inflate the established prayer + Amanah + Muhasaba denominator. The Journey screen must be switched to this helper in a subsequent integration pass.

The catalog is a curated starting set, not a complete Quran or Hadith library. Its English meanings are editorial summaries, not literal translations. Final source-text review, broader content expansion, Daily Light integration, and compilation are still pending. Do not label this level release-ready or claim all Islamic content is verified.

## Remaining levels

Complete sourced Daily Light integration and Hadith review, optional NUR AI, notification scheduling, accessibility/localization, privacy and backup hardening, widget behavior, and final integration. Existing implementations must be inspected before changes to avoid duplicating or replacing working features. Each level receives a separate source commit and review.

The final build gate includes compilation, unit tests, migration and backup-restore tests, APK signing verification, and installation testing. Existing user data must not be erased to work around a signing or migration problem. No feature is considered release-ready merely because source has been committed.
