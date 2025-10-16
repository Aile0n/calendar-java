# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

## [Unreleased]

## [1.0.2] - 2025-10-16

### Documentation
- README.md
  - Clarified Tech Stack with exact components and versions and reinforced that ical4j is retained only during transition
  - Added clearer Troubleshooting for JavaFX native modules and empty ICS files
  - Expanded Run/Build instructions (Windows-friendly) and highlighted shaded JAR launcher `org.example.Main`
  - Added explicit Acknowledgements and clarified that CalendarFX is open source under Apache-2.0 with upstream license link
  - Refined Project Structure overview for quick navigation
- CODE_EXPLANATION.md
  - Expanded “Big Picture” and “Data Flow”, including autosave behavior and status indicator
  - Added “Where to start reading the code” and a concise glossary for newcomers
- BIWEEKLY_MIGRATION.md
  - Added before/after code snippets (ical4j → Biweekly) for import/export
  - Clarified versions (Biweekly 0.6.8, ical4j 4.0.2) and test commands
- FIX_SUMMARY.md
  - Detailed the autosave pipeline (global handler + diff monitor) and reminder handling
  - Linked to related documents (migration, manual test plan, architecture overview)
- MANUAL_TEST_PLAN.md
  - Documented end-to-end manual scenarios for auto-save, ICS/VCS round-trips, drag/drop, delete, and reminders
- PROJEKT_ERSTELLUNG.md
  - Consolidated links to all docs, updated build/run sections with Windows cmd examples
  - Added CI (GitHub Actions) overview and set project header to 1.0.1 for historical accuracy
- THIRD-PARTY-NOTICES.md
  - Clarified CalendarFX licensing (Apache-2.0), added concise license guidance, and included a version history + Maven command to regenerate notices

### Notes
- No functional code changes in this release; this is a documentation alignment/polish release to keep all Markdown guides consistent with the current implementation (Biweekly-based ICS, auto-save pipeline, UI layout).

## [1.0.1] - 2025-10-09

### Fixed
- **Dark Mode Toggle**: Fixed bug where dark mode could not be turned off
  - Updated `applyTheme()` and `applyThemeToDialog()` methods in `CalendarProjektController.java` to conditionally apply dark.css stylesheet only when dark mode is enabled
  - Updated `applyTheme()` and `applyThemeToDialog()` methods in `CalendarProjektApp.java` to conditionally apply dark.css stylesheet only when dark mode is enabled
  - Dark mode stylesheet is now properly removed when dark mode is disabled in settings
  - Previously, the stylesheet was always applied regardless of the dark mode setting

## [1.0.0] - 2025-10-09

### Changed
- **ICS Library Migration**: Migrated from ical4j to Biweekly library for ICS file handling
  - Replaced ical4j dependencies with Biweekly for improved iCalendar parsing and generation
  - Updated `IcsUtil.java` to use Biweekly API for reading and writing ICS files
  - Fixed type compatibility issues with Categories property handling
  - Maintained ical4j dependency in pom.xml for backward compatibility during transition
  - Improved robustness of ICS import/export functionality
- **UI Improvement**: Moved "Beenden und Speichern" (Exit and Save) button to the right side of the toolbar for better visual organization and workflow
- **Toolbar Reorganization**: Complete restructuring of toolbar layout:
  - Left side: Action buttons (Einstellungen/Settings, Neuer Termin/New Event, Importieren/Import, Exportieren/Export)
  - Right side: Status indicator label and Exit/Save button
  - Added flexible spacer (JavaFX Region with HBox.setHgrow) between left and right sections
  - Added visual separator before Exit button for better visual grouping
- **Status Label Position**: Status indicator now appears on the right side before the exit button, making save status always visible before closing
- **Improved Visual Hierarchy**: Clear separation between primary actions (left) and application controls (right)

### Added
- Flexible spacer component in toolbar using `javafx.scene.layout.Region` with `HBox.setHgrow(spacer, Priority.ALWAYS)`
- Visual separator (javafx.scene.control.Separator) between status label and exit button

### Removed
- **Database Functionality**: Removed database persistence layer to simplify architecture
  - Removed all DAO (Data Access Object) classes and database-related code
  - Application now exclusively uses ICS file-based storage for calendar events
  - Eliminated database dependencies and configuration
  - Removed unused database mode from config.properties
  - Simplified codebase by focusing on single file-based persistence mechanism
- **Unused Code Cleanup**: Removed obsolete classes and methods that were no longer used after database removal

### Files Modified
- `src/main/java/CalendarProjektApp.java`:
  - Reorganized toolbar item order
  - Added spacer region for flexible layout
  - Added separator for visual grouping
  - Toolbar items order: `settingsButton, createBtn, importBtn, exportBtn, spacer, statusLabel, separator, exitBtn`

### Documentation Updates
- `README.md`: Updated toolbar description to reflect new layout with right-aligned status and exit button
- `CODE_EXPLANATION.md`: 
  - Added explanation of toolbar organization in CalendarProjektApp section
  - Described spacer mechanism and UI layout logic
  - Added status indicator to Data Flow section
- `FIX_SUMMARY.md`: 
  - Added new section "UI Improvements (Version 1.0.0)"
  - Documented toolbar reorganization technical details
  - Listed benefits of new layout
- `PROJEKT_ERSTELLUNG.md`: 
  - Created comprehensive project creation documentation
  - Added complete version history from 0.1.0 to 1.0.0
  - Documented build instructions, project structure, and technologies
  - Added feature list and known limitations
- `THIRD-PARTY-NOTICES.md`:
  - Updated version reference to 1.0.0 (2025-10-09)
  - Added Version History section with all releases
  - Added Maven command for license verification

### Technical Details
- **Layout Implementation**: Uses JavaFX HBox layout with Priority.ALWAYS on spacer Region to push right-side elements to the edge
- **Button Order**: Maintains logical grouping: settings → create → import → export | [spacer] | status → separator → exit
- **Backward Compatibility**: No breaking changes to existing functionality, only UI layout improvements

### User Experience Improvements
- More intuitive workflow: actions on the left, status/exit on the right
- Status always visible before closing the application
- Better visual balance in the toolbar
- Consistent with common UI patterns (status bars on the right)

## [0.2.0] - 2025-10-08

### Fixed
- Fixed critical persistence bug where CalendarFX UI changes (drag, resize, delete, edit) were lost on save/exit because only the stale `currentEntries` list was exported, not the current UI state from `fxCalendar`.
- Added `rebuildCurrentEntriesFromUI()` helper method to both `CalendarProjektApp` and `CalendarProjektController` to sync UI changes back to the persistent list before export.
- Updated "Beenden & Speichern" (Exit & Save) button and export functionality to rebuild entries from CalendarFX UI before writing to ICS file.
- Improved default ICS file path logic to fallback to user home directory if working directory is not writable, preventing startup failures in read-only installation directories.
- Added write permission checks before attempting to auto-create ICS files, preventing errors when launched from read-only locations like Program Files.

## [0.1.1] - 2025-10-08

### Added
- CODE_EXPLANATION.md: beginner-friendly overview of the codebase and main flows.
- Light inline comments across key classes (controller, DAO, utilities, launcher) to help starters.

### Changed
- README: clarified setup/run instructions and added a link to the code explanation document.
- Guidelines: aligned with current features and referenced CODE_EXPLANATION.md.

## [0.1.0] - 2025-09-30

### Added
- VCS (vCalendar 1.0) import/export alongside ICS; UI updated to handle ICS/VCS files ([7f2ec51](https://github.com/Aile0n/calendar-java/commit/7f2ec51)).
- JavaFX launcher `Main` class and executable shaded JAR configuration (manifest via Maven Shade) ([4e6d150](https://github.com/Aile0n/calendar-java/commit/4e6d150)).
- Database mode and ICS file storage support for events ([fa4c8b3](https://github.com/Aile0n/calendar-java/commit/fa4c8b3)).
- Comprehensive README with project overview, setup, and technical details ([288d09d](https://github.com/Aile0n/calendar-java/commit/288d09d)).
- JavaDoc comments for model, DAO, and utility classes ([6a50d33](https://github.com/Aile0n/calendar-java/commit/6a50d33)).

### Changed
- Replaced `SimpleCalendarApp` with `CalendarProjekt`, introducing an MVC structure for the application ([fa4c8b3](https://github.com/Aile0n/calendar-java/commit/fa4c8b3)).
