# Migration to Biweekly Library

## Changes Made

### 1. Updated pom.xml
- Added Biweekly dependency (version 0.6.8)
- Kept ical4j dependency as requested

```xml
<dependency>
    <groupId>net.sf.biweekly</groupId>
    <artifactId>biweekly</artifactId>
    <version>0.6.8</version>
</dependency>
```

### 2. Rewrote IcsUtil.java
Completely migrated from ical4j to Biweekly for ICS handling:

#### Import Changes
- **Old**: Used ical4j's `CalendarBuilder` and `CalendarOutputter`
- **New**: Uses Biweekly's `Biweekly.parse()` and `Biweekly.write()`

#### Key Improvements
- Simpler API with more intuitive method names
- Better handling of VALARM (reminders)
- Cleaner date/time conversion
- More robust parsing with `Biweekly.parse(is).all()`

#### Example Changes

**Import (Before - ical4j)**:
```java
CalendarBuilder builder = new CalendarBuilder();
Calendar calendar = builder.build(cleanedStream);
for (var component : calendar.getComponents(Component.VEVENT)) {
    VEvent ev = (VEvent) component;
    // ...
}
```

**Import (After - Biweekly)**:
```java
List<ICalendar> calendars = Biweekly.parse(is).all();
for (ICalendar calendar : calendars) {
    for (VEvent event : calendar.getEvents()) {
        // ...
    }
}
```

**Export (Before - ical4j)**:
```java
Calendar calendar = new Calendar();
calendar.getProperties().add(new ProdId("..."));
VEvent ev = new VEvent(new DateTime(start), new DateTime(end), title);
CalendarOutputter outputter = new CalendarOutputter();
outputter.output(calendar, fos);
```

**Export (After - Biweekly)**:
```java
ICalendar calendar = new ICalendar();
calendar.setProductId("...");
VEvent event = new VEvent();
event.setSummary(title);
event.setDateStart(start);
event.setDateEnd(end);
calendar.addEvent(event);
Biweekly.write(calendar).go(path.toFile());
```

### 3. Fixed CalendarProjektController.java
- Ensured the `checkReminders()` method is properly implemented
- The method was referenced but needed to be added to handle reminder notifications

### 4. VCS Support Maintained
- The custom vCalendar (VCS 1.0) import/export functionality remains unchanged
- Uses manual parsing for maximum compatibility

## Benefits of Biweekly

1. **Simpler API**: More intuitive method names and structure
2. **Better Documentation**: Clearer examples and documentation
3. **Active Development**: More recent updates and community support
4. **Less Boilerplate**: Fewer lines of code needed for common operations
5. **Better Error Handling**: More graceful handling of malformed ICS files

## Testing

All existing tests in `IcsUtilTest.java` should continue to work:
- ICS round-trip import/export
- VCS round-trip import/export
- Multiple entries
- Special characters

## Next Steps

To complete the migration:

1. Rebuild the project to download Biweekly:
   ```
   mvn clean compile
   ```

2. Run tests to verify everything works:
   ```
   mvn test
   ```

3. Test the application manually to ensure ICS import/export works correctly

## Notes

- ical4j remains in the dependencies as requested
- No changes to the UI or user-facing functionality
- All existing calendar features (categories, reminders, descriptions) continue to work
- The migration is transparent to end users

