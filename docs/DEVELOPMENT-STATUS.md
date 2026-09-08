# Development status

The last verified source build is 0.3.0, commit cab1458e9f85737b77c8b812df8d7cc608098e6a. It passed the GitHub Actions compilation, JVM tests, and APK signature verification. It has not passed an on-device upgrade test.

The complete-experience branch is based on that commit. Its first priority is working backup/import and migration-safe data access, followed by real privacy enforcement, progress/insights, widgets, motion, and optional companion features. This file is not a claim that those features already work. Each feature must be connected to an actual user flow and pass applicable tests before the status is changed.

The original 0.1.0 install may have a different signing certificate. Do not uninstall it to install a new debug APK if it contains useful data. A same-package update requires a compatible signing certificate and increasing versionCode. A backup import can transfer data only after the old installation has exported it. The current 0.1.0 build has no completed export bridge, so data recovery is not yet guaranteed.