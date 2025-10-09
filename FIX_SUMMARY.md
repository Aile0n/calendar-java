# Fix for Calendar Entry Export Bug

## Problem Statement
When users created calendar entries directly in the CalendarFX UI (by clicking on the calendar in day, week, month, or year views), these entries were:
- NOT saved to the ICS file automatically
- NOT available when exporting the calendar
- Lost when the application was closed (unless the exit button was used, which calls `rebuildCurrentEntriesFromUI()`)

## Root Cause
The application had a method `rebuildCurrentEntriesFromUI()` that could collect all entries from the CalendarFX UI, but it was only called:
1. When exporting via the "Export" button (`onExport()`)
2. When closing via the "Exit" button (`onExit()`)

Entries created via the "Neuer Termin" button were directly added to the `currentEntries` list and saved immediately, but entries created by clicking on the calendar views were only added to the CalendarFX visual calendar, not to the persistent `currentEntries` list.

## Solution
Added automatic listeners to all calendars that monitor entry changes and trigger an automatic save operation.

### Changes Made

#### 1. Added Calendar Entry Change Listeners
- **File**: `src/main/java/CalendarProjektController.java`
- **Method**: `setupCalendarListeners()` - Sets up listeners on all calendars during initialization
- **Method**: `addCalendarListener(Calendar)` - Attaches a ListChangeListener to a calendar's entries property
- **Method**: `saveCurrentEntriesToIcs()` - Automatically saves entries to ICS when changes are detected

The listener monitors the `entriesProperty()` of each calendar and triggers a save operation whenever entries are added, modified, or deleted.

#### 2. Enhanced Entry Rebuilding
- **Method**: `rebuildCurrentEntriesFromUI()` - Enhanced to capture:
  - Entry title and description (already present)
  - Entry start and end times (already present)
  - **Category** from the calendar name (new)
  - **Recurrence rule** from the entry's recurrence rule property (new)

#### 3. Integration Points
- `initialize()` - Calls `setupCalendarListeners()` after wiring button actions
- `getOrCreateCalendar()` - Adds listener to newly created category calendars

### Technical Details

The fix uses JavaFX's `ListChangeListener` to monitor the observable list of entries in each calendar:

```java
calendar.entriesProperty().addListener((javafx.collections.ListChangeListener<Entry<?>>) change -> {
    if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.ICS) {
        saveCurrentEntriesToIcs();
    }
});
```

When any change is detected (add, remove, update):
1. `rebuildCurrentEntriesFromUI()` collects all entries from all calendars
2. `IcsUtil.exportIcs()` writes the entries to the configured ICS file
3. Errors are logged but not shown to the user to avoid interrupting workflow

### Behavior

**Before the fix:**
- Entries created by clicking on calendar views: ❌ Not saved automatically
- Entries created via "Neuer Termin" button: ✅ Saved immediately
- Export includes UI-created entries: ❌ No (unless using the exit button)

**After the fix:**
- Entries created by clicking on calendar views: ✅ Saved automatically
- Entries created via "Neuer Termin" button: ✅ Saved immediately (unchanged)
- Export includes UI-created entries: ✅ Yes (always)
- Entries modified by drag/drop: ✅ Saved automatically
- Entries deleted via UI: ✅ Removed from ICS file automatically

### Storage Mode Handling
The auto-save functionality only activates when `ConfigUtil.getStorageMode()` returns `StorageMode.ICS`. In Database mode, the listeners are attached but do not trigger ICS saves.

## Testing
See `MANUAL_TEST_PLAN.md` for comprehensive manual test scenarios covering:
- Creating entries in different views (day, week, month, year)
- Modifying entries via drag and drop
- Deleting entries
- Verifying auto-save behavior
- Confirming database mode is unaffected

## Benefits
1. **User Experience**: Users can now create entries naturally by clicking on the calendar without worrying about data loss
2. **Data Integrity**: All entries are immediately persisted to the ICS file
3. **Consistency**: All entry creation methods (button vs. UI click) now behave the same way
4. **Minimal Changes**: The fix reuses existing infrastructure (`rebuildCurrentEntriesFromUI()`, `IcsUtil.exportIcs()`) with minimal new code

## Limitations
- Auto-save does not capture all possible entry properties (e.g., `reminderMinutesBefore` is not stored by CalendarFX Entry objects by default)
- Auto-save errors are logged but not shown to the user (intentional to avoid interruptions)
- The fix is specific to ICS mode; database mode uses different persistence mechanisms

## Files Modified
1. `src/main/java/CalendarProjektController.java` - Added listeners and auto-save functionality
2. `MANUAL_TEST_PLAN.md` - Created comprehensive test plan
3. `FIX_SUMMARY.md` - This file

## Backward Compatibility
This fix is fully backward compatible:
- No breaking changes to existing APIs
- No changes to database schema
- No changes to ICS file format
- Existing entries and functionality remain unchanged

## UI Improvements (Version 1.0.0)

### Toolbar Layout Enhancement
The application toolbar has been reorganized for better user experience:
- **Left side**: Action buttons (Settings, New Event, Import, Export)
- **Right side**: Status indicator and Exit/Save button
- **Spacer**: A flexible region automatically pushes the status label and exit button to the right edge

This layout provides:
- Clear visual separation between actions and status/controls
- Consistent placement of the save button where users expect it
- Real-time status feedback always visible before the exit action
