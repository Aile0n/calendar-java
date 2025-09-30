# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

## [0.1.0] - 2025-09-30

### Added
- VCS (vCalendar 1.0) import/export alongside ICS; UI updated to handle ICS/VCS files ([7f2ec51](https://github.com/Aile0n/calendar-java/commit/7f2ec51)).
- JavaFX launcher `Main` class and executable shaded JAR configuration (manifest via Maven Shade) ([4e6d150](https://github.com/Aile0n/calendar-java/commit/4e6d150)).
- Database mode and ICS file storage support for events ([fa4c8b3](https://github.com/Aile0n/calendar-java/commit/fa4c8b3)).
- Comprehensive README with project overview, setup, and technical details ([288d09d](https://github.com/Aile0n/calendar-java/commit/288d09d)).
- JavaDoc comments for model, DAO, and utility classes ([6a50d33](https://github.com/Aile0n/calendar-java/commit/6a50d33)).

### Changed
- Replaced `SimpleCalendarApp` with `CalendarProjekt`, introducing an MVC structure for the application ([fa4c8b3](https://github.com/Aile0n/calendar-java/commit/fa4c8b3)).

