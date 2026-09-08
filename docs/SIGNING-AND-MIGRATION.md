# Stable signing and migration plan

## What changed in 0.3.0

Starting with version `0.3.0`, CI debug APKs are signed with a stable test key stored at `app/keystores/nur-m3-test.jks`. This key is public and is only for development/testing, but it fixes the previous problem where every GitHub runner could create a different debug key.

The package name stays `com.nshd.nurm3`, so future APKs signed with the same key can update each other and keep the app database.

## Important limitation

The first `0.1.0` and early `0.2.0` artifacts may have been signed by an ephemeral GitHub debug key. Android will not allow an APK signed with the new stable test key to update an install signed with a different old debug key. That old private key cannot be recovered from the APK.

If a tester installed one of those early builds and already entered useful data, they should keep that install until an export/import path is available. Installing 0.3.0 over it may require uninstalling the old app, which deletes local app data.

## Database migration

NUR-M3 uses the same database name, `nur-m3.db`. The Room migration from version 1 to version 2 adds recurrence, date ranges, and history snapshots without deleting existing entries or completions. Future schema changes must add explicit migrations rather than destructive migration.

Rules:

- Keep `applicationId = "com.nshd.nurm3"` for this app line.
- Increase `versionCode` for every installable update.
- Never use `fallbackToDestructiveMigration`.
- Archive entries instead of deleting history.
- Store daily completion records separately from task/reflection definitions.

## Private release signing

For a real release, do not use the public test key. Add these GitHub repository secrets and run the workflow:

- `NUR_RELEASE_KEYSTORE_BASE64`
- `NUR_RELEASE_STORE_PASSWORD`
- `NUR_RELEASE_KEY_ALIAS`
- `NUR_RELEASE_KEY_PASSWORD`

The workflow decodes the private keystore only inside GitHub Actions and does not commit it to the repository. If the secrets are absent, only the stable debug APK is produced.

## Migration safeguard before visual expansion

Before adding heavier UI, widgets, NUR AI, or companion modules, every build must pass unit tests for date-scoped completion, recurrence, customizable Journey layout, and backup inspection. A green GitHub Action confirms compilation and JVM tests only; on-device upgrade testing is still required before calling a build safe for daily use.
