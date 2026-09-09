# NUR 0.4 — Power features and motion

This branch continues the 0.3 foundation. It preserves the application ID, stable test signing identity, and explicit database migrations. No original NUR repository is modified.

## Delivery requirements

- Complete backup export, validation, preview, and transactional import before any destructive restore. Preserve entry identifiers, schedules, completion history, and archive state. Do not export credentials or PIN material.
- Implement real app-lock behavior rather than a placeholder switch, with private notification/widget previews and a recoverable security setup flow.
- Add genuine date-scoped insights and live widget data, including date rollover and privacy-safe updates.
- Add optional NUR AI using a user-supplied Gemini credential stored outside ordinary settings/backups. Obtain network consent, bound conversation history, and distinguish verified religious references from generated text.
- Add offline companion tools with accurate source attribution and persistent user data.
- Refine Daily Light, progress indicators, completion motion, navigation transitions, and loading. Respect reduced-motion settings and never use animation as a data mutation.

## Release gate

Compilation, unit tests, migration tests, signing verification, and an on-device upgrade/restore smoke test are required before a daily-use release. The 0.3 APK remains the fallback. No production signing key belongs in Git, and early ephemeral-key installations must not be uninstalled before their data is recoverable.
