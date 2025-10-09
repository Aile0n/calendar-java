# Dependency Update Summary

## Date
2025-10-08

## Objective
Update Maven dependencies and build plugins to their latest patch/minor versions as recommended in the dependency audit (dependency-audit-2025-09-30.md).

## Changes Made

### Dependencies Updated

| Dependency | Old Version | New Version | Type |
|------------|-------------|-------------|------|
| org.openjfx:javafx-controls | 22.0.1 | 22.0.2 | Patch |
| org.openjfx:javafx-fxml | 22.0.1 | 22.0.2 | Patch |
| org.xerial:sqlite-jdbc | 3.42.0.0 | 3.46.1.3 | Minor |
| org.mnode.ical4j:ical4j | 3.2.7 | 3.2.19 | Patch |
| org.junit.jupiter:junit-jupiter | 5.10.2 | 5.10.5 | Patch |

### Build Plugins Updated

| Plugin | Old Version | New Version | Type |
|--------|-------------|-------------|------|
| maven-surefire-plugin | 3.2.5 | 3.3.1 | Minor |
| maven-shade-plugin | 3.5.1 | 3.6.1 | Minor |

## API Compatibility Fix

### ical4j 3.2.19 VAlarm Issue
When updating from ical4j 3.2.7 to 3.2.19, a breaking API change was discovered in how VAlarm components are added to VEvent:

**Old API (3.2.7):**
```java
VAlarm alarm = new VAlarm();
// ... configure alarm ...
ev.getAlarms().add(alarm);  // This worked in 3.2.7
```

**New API (3.2.19):**
```java
VAlarm alarm = new VAlarm();
// ... configure alarm ...
ev.getComponents().add(alarm);  // Required in 3.2.19
```

**Issue:** In ical4j 3.2.19, calling `ev.getAlarms().add(alarm)` returns `true` but doesn't actually add the alarm to the event. The alarm must be added using `ev.getComponents().add(alarm)` instead.

**Fix Applied:** Updated `IcsUtil.java` line 137 to use `ev.getComponents().add(alarm)` instead of `ev.getAlarms().add(alarm)`.

## Testing
All 18 unit tests pass successfully:
- ✅ ICS import/export tests
- ✅ VCS import/export tests
- ✅ Special character handling
- ✅ Recurrence rule support
- ✅ Category support
- ✅ Reminder/alarm support (fixed with API change)
- ✅ Multi-entry handling
- ✅ Edge cases (empty descriptions, same start/end times, etc.)

## Build Verification
- ✅ Clean compile successful
- ✅ All tests pass
- ✅ Package (fat JAR) builds successfully

## Deferred Updates
As per the audit recommendations, the following major version updates were deferred to avoid breaking changes:
- JavaFX 23.x (current: 22.0.2)
- CalendarFX 17.x (current: 12.0.1)
- JUnit 6.x (current: 5.10.5)

## Security Considerations
- Updated sqlite-jdbc from 3.42.0.0 to 3.46.1.3 - brings SQLite engine updates and potential security fixes
- All other updates are within the same major version, minimizing risk
- No known CVEs addressed in this update (as per offline assessment in the audit)

## Recommendation for Next Steps
1. Run OWASP Dependency-Check to get authoritative CVE information:
   ```bash
   mvn org.owasp:dependency-check-maven:check -Dformat=ALL -DfailOnError=false
   ```
2. Address any moderate+ severity findings that appear
3. Consider major version upgrades in a separate task after validating compatibility
