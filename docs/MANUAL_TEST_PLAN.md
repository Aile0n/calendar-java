# Manual Test Plan for Auto-Save and ICS/VCS
Version: 1.0.3 — Stand: 2025-11-13

## Summary
This plan verifies that entries created or edited directly in the CalendarFX UI are saved automatically to the configured ICS file and that import/export (ICS and VCS) works end-to-end.

## Preconditions
- App runs in ICS-only mode (default)
- A writable ICS path is configured in settings (default: `calendar.ics`)

## Test Scenarios

### Test 1: Create Entry in Day View
1. Start the application
2. Switch to the Day view
3. Click a time slot to create a new entry
4. Fill in title and times and save
5. Expected: Entry is shown immediately; status shows saved
6. Verify: Close and reopen the app — the entry is still present

### Test 2: Create Entry in Week View
1. Switch to the Week view
2. Create a new entry via click
3. Expected: Entry is saved automatically
4. Verify: Export to a new ICS file; open it in a text editor — the entry exists

### Test 3: Create Entry in Month View
1. Switch to the Month view
2. Click a day to create a new entry
3. Expected: Entry is saved automatically
4. Verify: Restart the app — the entry is present

### Test 4: Modify Existing Entry via Drag/Drop
1. Create an entry (via New button or click)
2. Drag it to a different time/date
3. Expected: Status indicates save; no errors
4. Verify: Export; the exported ICS contains the updated times

### Test 5: Delete Entry via UI
1. Create an entry
2. Delete via UI (context menu or keyboard)
3. Expected: Status indicates save
4. Verify: Export; the entry is no longer present

### Test 6: Import ICS
1. Prepare a small `.ics` file with 1–2 events
2. Use Import to load it
3. Expected: Imported events appear and are persisted to the configured ICS path

### Test 7: Import/Export VCS
1. Prepare a small `.vcs` file (vCalendar 1.0)
2. Import it
3. Export current calendar as `.vcs`
4. Expected: Round-trip retains summary/description and start/end times

### Test 8: Reminders (if ICS contains VALARM)
1. Import an ICS that has an event with a VALARM trigger (e.g., 15 minutes prior)
2. Wait until within the reminder window
3. Expected: An informational reminder dialog appears for the upcoming event

## Success Criteria
- UI-created entries are saved automatically without manual export
- Drag/drop edits are persisted
- Deletions are persisted
- Import works for ICS and basic VCS
- Export works for ICS and VCS
- Reminder dialogs appear for events that have reminderMinutesBefore set via import

## Notes
- ICS handling uses Biweekly; VCS is handled by a minimal custom parser/writer
- The application writes to the configured ICS path; ensure the directory is writable
- See also: BIWEEKLY_MIGRATION.md and FIX_SUMMARY.md
