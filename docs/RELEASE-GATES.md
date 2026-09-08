# NUR-M3 complete experience — release gates

This branch extends the last green 0.3.0 build. The original NUR repositories and the installed 0.1.0 app are not modified.

## Required implementation

- Working backup export/import with schema validation, a preview, an explicit merge/replace choice, and transaction-based rollback safety. No import may silently delete existing records.
- Real privacy enforcement, not a cosmetic switch. App credentials and network API keys must not be committed or included in exported backups.
- Daily completion records remain date-scoped. Definitions survive midnight, archives preserve historical records, and database changes use explicit migrations.
- One source of truth for progress, recurring schedules, history and insights. No manufactured dates, completion records, or religious quotations.
- Material 3 navigation, accessible controls, responsive editors, animations with reduced-motion support, and independent progress indicators.
- Optional AI and online features must remain disabled until configured and must not block the offline app. User-owned credentials remain private.

## Release and signing

Keep applicationId com.nshd.nurm3 and increase versionCode. The public test certificate is only for development. Production release signing must use a privately owned keystore and must fail closed when release credentials are absent. Do not publish any private key or API key. A differently signed early install cannot be updated directly. Preserve it until a usable export/import bridge has been tested.

A successful CI run establishes compilation and automated tests, not an on-device upgrade guarantee. A release is ready for daily use only after migration, backup restore, background/foreground date changes, small-screen layout, accessibility, and real-device install/update tests are completed. Any unfinished module must be clearly identified rather than exposed as a working toggle.