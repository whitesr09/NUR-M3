# NUR — Material 3

A fresh, native Android Islamic productivity app. This repository is independent of the original NUR V2 and NUR-material-3 projects.

## Current milestone: 0.3.0

The implementation now contains Kotlin, Jetpack Compose, Material 3, a black-blue/gold identity, light/dark/system themes, dynamic color, Appearance Studio, customizable Daily Journey cards, manual prayer tracking, Amanah, Muhasaba, Rhythm, genuine history, recurring entries, date-scoped progress, Room persistence, DataStore preferences, backup JSON groundwork, widget groundwork, and a stable debug signing key for future test updates.

Amanah, Muhasaba, and Rhythm entries remain saved after midnight. Only date-scoped completion records change per day. History uses actual recorded completions, never generated fake dates.

## Stable signing warning

Version `0.3.0` introduces a stable debug signing key for GitHub Actions artifacts. APKs built from `0.3.0` onward can update each other if the same package name and signing key are kept. The first `0.1.0` and early `0.2.0` debug APKs may have used ephemeral GitHub runner keys, so Android may require uninstalling those old builds before installing the new stable-debug line. See `docs/SIGNING-AND-MIGRATION.md`.

## Build an APK on your Android phone

Open the repository's **Actions** tab, select **Android APK**, and open the newest successful run. Under **Artifacts**, download **NUR-M3-stable-debug**. The downloaded ZIP contains the APK. Extract the ZIP using your phone's file manager and open the APK to install it. GitHub may require you to be signed in to download an Actions artifact.

To run the build again, open **Android APK → Run workflow → Run workflow**. Each push to `main` also starts a build automatically. The workflow installs JDK 17, Gradle 8.11.1, Android SDK 35, runs unit tests, assembles the stable debug APK, verifies the signing certificate, and uploads the artifact for 30 days. If private release signing secrets are added, it also builds a release APK.

For local development, use Android Studio with JDK 17, Android SDK 35, and Gradle 8.11.1. Run `gradle testDebugUnitTest assembleDebug`.

## Application identity

- Application ID: `com.nshd.nurm3`
- Minimum Android version: Android 8.0 (API 26)
- Target and compile SDK: 35
- Version: 0.3.0 (versionCode 3)

The different application ID allows this project to coexist with the original NUR package. It does not automatically migrate the original V2 app's data. Keep the same package name and signing identity for subsequent NUR-M3 updates.

## Development principles

Offline-first functionality, local data ownership, explicit user consent for future network/location features, verified religious sources, accessible Material 3 controls, no fabricated religious quotations, and reproducible builds. See `docs/PRODUCT-SCOPE.md` and `docs/BUILD-PLAN.md` for the roadmap. Made by NSHD.
