# NUR-M3 development staging

Base: the successful 0.4 source at 33a90ba46273fe8e63fca6543c14a5530d7a19fa.

The user requested source-only development until the remaining features are added. This branch is intentionally not attached to an open pull request and is not main, so ordinary pushes do not trigger the Android APK workflow. Do not merge it, open a PR, dispatch a workflow, or distribute an APK until the user requests the build. Static review is not a substitute for compilation or device testing.

## Level 0.5: expressive motion and progress — source committed

- Added NurMotion with bounded progress values, rounded percentages, independent Animatable state and a centralized reduced-motion preference.
- Android's animation duration scale is respected, including the disabled-animation setting.
- Progress state is keyed by local date so a new day begins at its real value rather than animating from yesterday's percentage.
- Connected Daily Light's two rings, its numeric percentage, and all Journey section indicators to the new motion system.
- Connected Amanah, Muhasaba and Rhythm screen indicators and completion-row feedback to the same date-scoped system.
- Added pure progress-boundary unit tests. They have been committed but not executed.
- Preserved existing persistence, schedules, navigation, signing configuration, package ID and Room schema. No database migration was needed for this UI-only level.

Verification is deferred at the user's request. Compilation, tests, signing verification, and installation have not been performed for this level. This is development source, not a release-ready APK.

## Remaining levels

Review and complete companion modules, verified Islamic content, optional NUR AI, notification scheduling, accessibility/localization, privacy and backup hardening, widget behavior, and final integration. Existing implementations must be inspected before changes to avoid duplicating or replacing working features. Each level receives a separate source commit and review.

The final build gate includes compilation, unit tests, migration and backup-restore tests, APK signing verification, and installation testing. Existing user data must not be erased to work around a signing or migration problem. No feature is considered release-ready merely because source has been committed.
