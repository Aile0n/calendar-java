# Migration to Biweekly Library

## Summary
This project migrated ICS handling from ical4j to the Biweekly library while keeping ical4j as a dependency for compatibility during transition. The application code now uses Biweekly for reading and writing .ics files.

- Biweekly version: 0.6.8 (in pom.xml)
- ical4j version (retained): 4.0.2 (in pom.xml)

## Changes Made

### 1. Updated pom.xml
- Added Biweekly dependency (0.6.8)
- Kept ical4j dependency (4.0.2) as requested

```xml
<dependency>
    <groupId>net.sf.biweekly</groupId>
    <artifactId>biweekly</artifactId>
    <version>0.6.8</version>
</dependency>
```

### 2. Rewrote IcsUtil.java
Completely migrated from ical4j to Biweekly for ICS handling.

#### Import Changes
- Old: ical4j `CalendarBuilder` / component iteration
- New: Biweekly `Biweekly.parse(is).all()` and `ICalendar.getEvents()`

#### Export Changes
- Old: ical4j `CalendarOutputter`
- New: Biweekly `Biweekly.write(calendar).go(...)`

#### Key Improvements
- Simpler API with intuitive method names
- Built-in VALARM support for reminders
- Straightforward date/time mapping
- Robust parsing of multi-calendar ICS files

#### Example Changes

Before (ical4j):
```java
// Import
CalendarBuilder builder = new CalendarBuilder();
Calendar calendar = builder.build(cleanedStream);
for (var component : calendar.getComponents(Component.VEVENT)) {
    VEvent ev = (VEvent) component;
    // ...
}

// Export
Calendar calendar = new Calendar();
VEvent ev = new VEvent(new DateTime(start), new DateTime(end), title);
new CalendarOutputter().output(calendar, fos);
```

After (Biweekly):
```java
// Import
List<ICalendar> calendars = Biweekly.parse(is).all();
for (ICalendar cal : calendars) {
    for (VEvent event : cal.getEvents()) {
        // ...
    }
}

// Export
ICalendar calendar = new ICalendar();
VEvent event = new VEvent();
event.setSummary(title);
event.setDateStart(start);
event.setDateEnd(end);
calendar.addEvent(event);
Biweekly.write(calendar).go(path.toFile());
```

### 3. CalendarProjektController.java
- Verified and completed `checkReminders()` implementation to show simple reminder alerts based on VALARM-like data in entries.
- Auto-save now uses a global CalendarView event handler plus a lightweight diff-based monitor to persist UI changes to ICS.

### 4. VCS Support Maintained
- The custom vCalendar (VCS 1.0) import/export remains unchanged
- Manual parsing is kept for maximum compatibility

## Benefits of Biweekly
1. Simpler API and less boilerplate
2. Clear documentation and examples
3. Robust parsing across real-world ICS files
4. Convenient support for alarms and categories

## Testing
Existing tests in `IcsUtilTest.java` cover:
- ICS round-trip import/export
- VCS round-trip import/export
- Multiple entries
- Special characters

Run:
```
mvn clean compile
mvn test
```

## Documentation and Changelog
- Documentation updated to reference Biweekly (README, CODE_EXPLANATION, THIRD-PARTY-NOTICES)
- This migration guide added/updated
- Changelog notes Biweekly usage under 1.0.0 and docs updates under 1.0.1

## Notes
- ical4j remains on the classpath (4.0.2) but ICS read/write in the app is handled by Biweekly
- No user-facing changes are required; the migration is transparent to end users
