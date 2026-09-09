# NUR-M3 development staging

Base: the successful 0.4 source at 33a90ba46273fe8e63fca6543c14a5530d7a19fa.

The user requested source-only development until the remaining features are added. This branch is intentionally not attached to a pull request and is not main. Do not merge it, open a PR, dispatch a workflow, or distribute an APK until the user requests the build. Static review is not a substitute for compilation or device testing.

## Level 0.5: expressive motion and progress — source committed

Added centralized reduced-motion behavior, date-keyed independent progress animations, smooth Daily Light rings and section indicators, completion feedback, and pure boundary tests. Preserved persistence, signing, package ID, navigation, and Room schema. Tests are committed but have not been executed.

## Level 0.6: offline Quran Reflections — source committed

Added a six-passage offline catalog, search and reading screen, references, reduced-motion-aware transitions, and navigation. Added a dedicated Daily Light calculation helper. Catalog text still needs external verification before release.

## Level 0.7: Daily Light reflection integration — source committed, unverified

Connected the prayer + Amanah + Muhasaba progress helper to Journey, excluding Rhythm. Added a dated reflection carousel with opt-in 10-second playback, pause, previous/next and direct reading. Playback is lifecycle-aware; reduced motion is respected. Added a reader route with an optional verse ID and source-only tests. No database or signing change.

## Level 0.8: offline Dhikr counter — source committed, unverified

Added an offline counter with editable personal goals, separate session/today/lifetime counts, optional haptics, confirmation, archive/restore and dated history. Added Room version 3 and explicit 2-to-3 migration, preserving the 1-to-2 chain. Added version 3 backups with Dhikr records and backward-compatible restore behavior. Room Upsert protects dated records from replacement cascades. Added pure and instrumentation tests, not yet executed.

## Level 0.9: professional UI and interaction foundation — source committed, unverified

The user requested a substantial visual and usability improvement, especially for checkboxes and everyday interactions. This level prioritizes that foundation rather than adding another untested companion module.

### Implemented in source

- Introduced custom NUR design primitives, a restrained black-blue/gold palette, a warm light theme, consistent typography, subtle borders, compact spacing and shape preferences. Existing dynamic palettes remain optional.
- Replaced the stock bottom navigation with a compact custom five-destination bar and branded app header. Added edge-to-edge system-bar handling, theme-aware icon contrast and a full Arabic header that can wrap on narrow screens.
- Replaced standard checklist controls with a custom animated check mark and a large semantic row target. Full-row tapping, ripple/press feedback, separate overflow actions, disabled/saving states and reduced-motion support are shared across prayer, Amanah, Muhasaba and Rhythm lists.
- Added per-entry/date completion request gating. The stored Room completion remains authoritative; overlapping taps are rejected, save failures are surfaced through the root snackbar, and a midnight date change cannot silently save into the previous day.
- Rebuilt Daily Journey with consistent surfaces, improved hierarchy, refined concentric rings, four markers, slim progress bars, readable counts and quick access to Quran and Dhikr. The established Daily Light denominator remains unchanged.
- Rebuilt task/habit list layouts with search, Today/To do/Completed filters, responsive empty states, an overflow menu, archived-entry management and reversible archive actions. The editor now has a scrollable form, wrapped recurrence choices, accessible day selection, a keyboard-aware action area, validation, save-in-progress feedback and an unsaved-changes confirmation. It waits for the actual save result before closing.
- Reworked genuine History into expandable dated groups with preserved title snapshots and completion timestamps.
- Restyled Appearance Studio, its live interactive preview, palette choices, theme choices, typography controls, setting toggles and Journey Studio. Settings destinations now share a consistent visual hierarchy. The last visible Journey card cannot be hidden, and an invalid saved layout recovers without discarding its order.
- Added shared root snackbar access for archive Undo and save-error feedback. Added completion-gate and Journey-layout regression tests to the source tree.

### Scope and remaining visual work

This is a substantial core-interface pass, not a claim that every feature screen is finished. Dhikr, Quran reader, privacy, backup and other companion-specific screens still need their own detailed layout/accessibility passes, shared component adoption and final device review. Existing working data and feature logic should be preserved during those passes.

### Deferred verification

No APK build, Gradle compilation, unit-test execution, instrumentation run or device installation was performed for level 0.9. The new source and tests are not yet verified as compiling. Final integration must check the complete source graph, Room migrations 1-to-2-to-3, backup versions 1/2/3, archive/restore and checkbox races, large text and 320dp layouts, edge-to-edge/keyboard behavior, reduced motion, TalkBack semantics and signing compatibility. New code must not be called release-ready merely because it is committed.

The application ID, existing versionCode, stable development signing identity, Room version 3 and backup version 3 were not changed by this level. Do not uninstall an existing installation to work around migration or signing errors. A production signing key must remain owner-controlled and private.

## Remaining levels

Continue focused visual polish, verified Islamic content and Hadith review, optional NUR AI, notification scheduling, accessibility/localization, privacy and backup hardening, widget behavior, advanced productivity tools and final integration. Inspect existing implementations before changes to avoid duplicating working features. Each level receives separate source commits and review.

The final build gate includes compilation, unit tests, migration and backup-restore tests, APK signing verification and installation testing. Existing user data must not be erased to work around a signing or migration problem. No feature is considered release-ready merely because source has been committed.
