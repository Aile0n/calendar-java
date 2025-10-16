# Fix Summary: UI-Created Entries Not Persisted Automatically

## Problem
Entries created directly in the CalendarFX UI (by clicking in day/week/month/year views) were not saved to the ICS file automatically. They disappeared on export or after restarting the app unless the explicit exit/save path was used.

## Root Cause
The persistent list (`currentEntries`) was not rebuilt from the CalendarFX UI after UI-driven edits (drag, resize, delete, create). Only explicit export/exit paths collected the UI state.

## Solution
Introduce automatic persistence for UI changes:
- Add a global CalendarView event handler that triggers a save after any CalendarFX change.
- Add a lightweight diff-based autosave monitor that periodically snapshots the UI state and persists when changes are detected.
- Centralize the save path to always rebuild `currentEntries` from the current UI before writing ICS.

## Implementation Details
- Rebuild helper: `rebuildCurrentEntriesFromUI()` converts CalendarFX entries (across all calendars/categories) into `CalendarEntry` objects.
- Auto-save pipeline:
  1) Global CalendarFX event handler at `calendarView.addEventHandler(CalendarEvent.ANY, ...)`
  2) Diff-based monitor via a short-interval JavaFX Timeline (`startAutosaveMonitor()`)
  3) `saveCurrentEntriesToIcs()` calls `rebuildCurrentEntriesFromUI()` and then `IcsUtil.exportIcs(...)`
- ICS Read/Write uses Biweekly (see BIWEEKLY_MIGRATION.md). VCS support remains via a small custom parser/writer.

## Behavior
Before:
- UI-created entries: not saved automatically
- Export: might miss UI-only changes

After:
- UI-created/edited/deleted entries: saved automatically to ICS
- Export: always includes the current UI state
- Exit: explicitly rebuilds and saves before closing

## Files Modified
- `src/main/java/CalendarProjektController.java`
  - Added global CalendarView event hook
  - Implemented diff-based autosave monitor
  - Hardened save path and exit handling
  - Completed simple reminder scheduling (`checkReminders`)
- `src/main/java/IcsUtil.java`
  - Switched ICS implementation to Biweekly (import/export)
- `MANUAL_TEST_PLAN.md`
  - Updated test scenarios (ICS-only, auto-save validation)

## Tests & Verification
See `MANUAL_TEST_PLAN.md` for manual scenarios covering:
- Create/modify/delete via UI across views
- Import/export round-trips (ICS and VCS)
- Status updates and exit/save behavior

Automated unit tests (`IcsUtilTest`) cover ICS/VCS round-trips and special characters.

## Limitations
- CalendarFX entries do not expose all metadata (e.g., per-entry reminders) by default; only mapped fields are persisted.
- Recurrence (RRULE) is not yet supported (planned).

## Related Docs
- BIWEEKLY_MIGRATION.md — details on the migration from ical4j to Biweekly
- CODE_EXPLANATION.md — high-level architecture and data flow
- PROJEKT_ERSTELLUNG.md — project creation history and build steps
