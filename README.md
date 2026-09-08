# NUR — Material 3

A fresh, native Android Islamic productivity app. This repository is independent of the original NUR V2 and NUR-material-3 projects.

## Current milestone: 0.1.0

The initial implementation contains a Kotlin/Jetpack Compose Material 3 application, a dark black-blue and gold theme, optional dynamic color, local prayer checklists, Amanah, Muhasaba, Daily Light progress, history, and persistent appearance settings. Room stores entries and date-scoped completion records; DataStore stores preferences. The five prayers are manually tracked, not calculated prayer times. Tasks and reflection entries remain after midnight, while the next day's checkboxes start unchecked. History uses actual recorded completions, not generated sample dates.

This is an early foundation, not the finished original-V2 redesign. The custom splash animation, final artwork, verified Quran/Hadith library, prayer-time calculations, reminders, advanced dashboard effects, and Gemini-powered NUR AI are future milestones. No API key, signing key, or secret should be committed.

## Build an APK on your Android phone

Open the repository's **Actions** tab, select **Android APK**, and open the newest successful run. Under **Artifacts**, select **NUR-M3-debug**. The downloaded ZIP contains `app-debug.apk`. Extract the ZIP using your phone's file manager and open the APK to install it. GitHub may require you to be signed in to download an Actions artifact.

To run the build again, open **Android APK → Run workflow → Run workflow**. Each push to `main` also starts a build automatically. The workflow installs JDK 17, Gradle 8.11.1, Android SDK 35, runs unit tests, assembles the debug APK, and uploads it for 30 days. The debug APK is signed with Android's development key and is for testing, not a production release. A private release signing key will be configured later.

For local development, use Android Studio with JDK 17, Android SDK 35, and Gradle 8.11.1. Run `gradle testDebugUnitTest assembleDebug`. The CI environment currently supplies Gradle explicitly; the checked-in lightweight `gradlew` launcher is not yet a complete standard wrapper because its wrapper JAR is absent. Use the documented command until the wrapper is restored.

## Application identity

- Application ID: `com.nshd.nurm3`
- Minimum Android version: Android 8.0 (API 26)
- Target and compile SDK: 35
- Version: 0.1.0 (versionCode 1)

The different application ID allows this project to coexist with the original NUR package. It does not migrate or modify the original app's data. Keep the same package name and signing identity for subsequent updates to this project.

## Development principles

Offline-first functionality, local data ownership, explicit user consent for future network/location features, verified religious sources, accessible Material 3 controls, and reproducible builds. See `docs/BUILD-PLAN.md` for the roadmap. Made by NSHD.
