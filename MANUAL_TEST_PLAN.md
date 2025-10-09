# Manual Test Plan for Calendar Entry Auto-Save Fix

## Bug Description
Previously, entries created directly in the CalendarFX UI (by clicking on the calendar in different views) were not saved to the ICS file and would be lost when exporting or restarting the application.

## Fix Description
Added automatic listeners to all calendars that detect when entries are added, modified, or deleted via the CalendarFX UI. When changes are detected, the entries are automatically saved to the configured ICS file (when in ICS storage mode).

## Test Scenarios

### Test 1: Create Entry in Day View
1. Start the application in ICS mode
2. Navigate to the Day view (Tagesansicht)
3. Click on a time slot in the day view to create a new entry
4. Fill in the entry details (title, description, time)
5. Save the entry
6. **Expected Result**: Entry is immediately saved to the ICS file
7. **Verification**: Export the calendar to a new ICS file and verify the entry is present

### Test 2: Create Entry in Week View (Wochenansicht)
1. Start the application in ICS mode
2. Navigate to the Week view (Wochenansicht)
3. Click on a time slot in the week view to create a new entry
4. Fill in the entry details
5. Save the entry
6. **Expected Result**: Entry is immediately saved to the ICS file
7. **Verification**: Close and restart the application - the entry should still be visible

### Test 3: Create Entry in Month View (Monatsansicht)
1. Start the application in ICS mode
2. Navigate to the Month view (Monatsansicht)
3. Click on a day in the month view to create a new entry
4. Fill in the entry details
5. Save the entry
6. **Expected Result**: Entry is immediately saved to the ICS file
7. **Verification**: Export the calendar and verify the entry is in the exported file

### Test 4: Create Entry in Year View (Jahresansicht)
1. Start the application in ICS mode
2. Navigate to the Year view (Jahresansicht)
3. Click on a day in the year view to create a new entry
4. Fill in the entry details
5. Save the entry
6. **Expected Result**: Entry is immediately saved to the ICS file
7. **Verification**: The entry should be visible when switching back to other views

### Test 5: Modify Existing Entry via UI
1. Create an entry via "Neuer Termin" button
2. In the calendar view, drag the entry to a different time/date
3. **Expected Result**: The modified entry is automatically saved to the ICS file
4. **Verification**: Export and check that the entry has the new time/date

### Test 6: Delete Entry via UI
1. Create an entry in any calendar view
2. Right-click and delete the entry (or use CalendarFX's delete functionality)
3. **Expected Result**: The entry is removed from the ICS file
4. **Verification**: Export and verify the entry is not in the exported file

### Test 7: Database Mode - No Auto-Save
1. Switch to Database storage mode via Settings
2. Create entries via the CalendarFX UI
3. **Expected Result**: Entries should NOT be auto-saved to ICS (only to database)
4. **Verification**: Check that no ICS file is created/modified

### Test 8: Multiple Entries
1. Create 5 different entries using different methods:
   - Via "Neuer Termin" button
   - By clicking in Day view
   - By clicking in Week view
   - By clicking in Month view
   - By clicking in Year view
2. **Expected Result**: All 5 entries are saved to the ICS file
3. **Verification**: Export and count entries in the exported file

## Success Criteria
- Entries created in any calendar view (day, week, month, year) are saved automatically
- Entries modified via drag-and-drop are saved automatically
- Entries deleted via the UI are removed from the ICS file
- Auto-save only happens in ICS mode, not in Database mode
- No errors are shown to the user during auto-save operations
- Application remains responsive during auto-save

## Notes
- The auto-save functionality uses `rebuildCurrentEntriesFromUI()` to collect all entries from the CalendarFX UI
- Errors during auto-save are logged to stderr but not shown to the user to avoid interrupting workflow
- Auto-save is only active when `ConfigUtil.getStorageMode()` returns `StorageMode.ICS`
