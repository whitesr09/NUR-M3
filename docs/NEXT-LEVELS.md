# NUR-M3 development staging

Base: the successful 0.4 source at 33a90ba46273fe8e63fca6543c14a5530d7a19fa.

The user requested source-only development until the remaining features are added. This branch is intentionally not attached to a pull request and is not main, so ordinary pushes do not trigger the Android APK workflow. Do not merge it, open a PR, dispatch a workflow, or distribute an APK until the user requests the build. Static review is not a substitute for compilation or device testing.

## Next level: expressive motion and progress

- Centralized motion policy, including the app preference and Android animation scale.
- Date-keyed progress animation that never carries yesterday's value into today.
- Smooth, independently animated Daily Light, prayer, Amanah, Muhasaba and Rhythm indicators.
- Responsive completion feedback without changing stored completion records.
- Preserve package ID, signing files, Room schema, migration chain and all existing data APIs.

## Later levels

Finish companion features, religious-source verification, accessibility and localization, reminders, backup/privacy hardening, and integration testing. Each level receives a separate source commit and review. The final build gate includes unit tests, migration/backup restore tests, APK signing verification and installation testing. No feature is considered release-ready merely because source has been committed.
