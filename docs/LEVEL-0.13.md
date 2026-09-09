# Level 0.13 — Insights and review

Status: source committed for review. No APK build or on-device validation has been performed for this level.

## Scope

This level continues the professional UI pass with a read-only Insights experience. It retains the successful 0.12 appearance and display customization source, the existing five-tab navigation, stable debug signing identity, application ID, Room version 3 and backup schema 3.

- Adds a pure, bounded review model with selectable 7-, 30- and 90-day windows and activity-type filters.
- Uses the first known saved creation or completion date rather than inventing an installation date or displaying a long false history.
- Separates actual recorded check-ins (including archived entries) from scheduled completion rates derived from currently active definitions.
- Excludes future dates and deduplicates records by entry/date for review counts.
- Adds a responsive review ring, summary metrics, real daily activity cards, bounded Show more pagination, and a searchable Rhythm section.
- Allows archived habits to be reviewed without claiming they have a current streak. Their historical streak calculations use the saved recurrence definition because an archive timestamp is not yet recorded.
- Shares the selected progress style, NUR theme, typography, reduced-motion controls, panels and accessible controls with the rest of the application.
- Adds regression tests for empty history, date boundaries, older restored records, archive/future handling, duplicate records, valid summaries, and input bounds.

## Data and verification boundaries

No entry, completion, Dhikr, backup, migration, signing or notification code is modified. Review settings are local UI state and do not change stored activity. This level does not add a timer, AI, background service, or fabricated religious content.

The current database does not store an installation timestamp, archive timestamp, or historical copies of recurrence definitions. Therefore scheduled historical rates cannot reconstruct every past schedule change. The interface distinguishes this limitation from genuine recorded check-ins and keeps the original History screen available for exact records.

The committed tests have not been run. Before a future preview APK, compile the complete source graph, run unit tests, inspect narrow and large-text layouts, verify TalkBack and reduced motion, and test archived/restored records on a real device. Do not uninstall an existing app or replace its database to resolve an update issue.
