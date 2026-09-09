# Level 0.12 — Appearance and display

Adds persisted AMOLED, four progression styles, grouped appearance settings, preview selectors, and an Android display-mode request limited to compatible modes at or below 120 Hz. All counting and progress data remain unchanged. The request is app-scoped and Android may choose a lower refresh rate for thermal, battery, or system policy. No global display override or privileged permission is used.

Source is staged for a new preview build. Preserve the stable debug signing certificate and use versionCode 5 for the next preview. Unit tests, compilation, signing verification, and on-device visual/refresh validation must be performed before declaring the preview ready. Existing data must not be deleted.
