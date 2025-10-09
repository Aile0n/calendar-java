# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

## [Unreleased]

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

