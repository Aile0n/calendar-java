# Fix Summary: Calendar Java Issues

## Issues Addressed

### 1. Fat JAR Entry Persistence Issue ✅ FIXED
**Problem:** Entries created in the application were not persisted when running from a fat JAR and reopening it.

**Root Cause:** 
- When running from a JAR, the application loaded `config.properties` from the classpath (inside the JAR)
- This config was never saved externally on first run
- On subsequent runs, it would load from classpath again, potentially missing user's data path
- While the ICS file was being created, the config wasn't persisted, leading to potential inconsistencies

**Solution:**
- Modified `ConfigUtil.load()` to automatically save `config.properties` externally if it was loaded from the classpath
- This ensures the config persists in the working directory after first run
- Relative ICS paths are now resolved to absolute paths to prevent working directory issues

**File Changed:** `src/main/java/ConfigUtil.java`

### 2. ICS/VCS Import/Export Robustness ✅ VERIFIED
**Problem:** Import and export of ICS was not working with different versions and different amounts of information.

**Finding:** 
The existing `IcsUtil` implementation already handles various ICS/VCS formats correctly. Added comprehensive tests to verify this.

**Verified Capabilities:**
- Minimal ICS files (basic fields only)
- Full-featured ICS (with recurrence, categories, reminders)
- VCS 1.0 format with proper escaping
- Auto-detection of ICS vs VCS format
- Graceful error handling for invalid data

**Files Added:**
- `src/test/java/IcsUtilEdgeCasesTest.java` - Edge case and error handling tests

## Test Coverage

### Total: 30 Tests (All Passing)

1. **IcsUtilTest (18 tests)** - Original round-trip tests
   - Basic ICS/VCS round-trip
   - Multiple entries
   - Special characters and Unicode
   - Empty/null descriptions
   - Recurrence rules
   - Categories and reminders
   - Auto-detection
   - Edge cases (same start/end time, missing DTEND)

2. **ConfigUtilTest (4 tests)** - New config persistence tests
   - Default loading
   - Save and load cycle
   - Relative path handling
   - Absolute path handling

3. **IcsUtilEdgeCasesTest (8 tests)** - New edge case tests
   - Null/empty entry lists
   - Invalid dates and null entries
   - Missing required fields (DTSTART)
   - Malformed dates in VCS
   - Long descriptions (1000+ lines)
   - Unicode characters (Japanese, Chinese, Arabic, emojis)

## Manual Verification

Tested fat JAR execution in a clean directory:
```bash
cd /tmp/test-jar
java -jar calendar-java-1.0-SNAPSHOT.jar
```

**Results:**
✅ `config.properties` created automatically on first run  
✅ Entries saved to `calendar.ics` in working directory  
✅ Entries loaded correctly on subsequent JAR runs  
✅ Various ICS format imports/exports working correctly  

## Files Modified

1. `src/main/java/ConfigUtil.java`
   - Auto-save external config on first load from classpath
   - Resolve relative ICS paths to absolute

2. `src/test/java/ConfigUtilTest.java` (new)
   - Tests for config persistence

3. `src/test/java/IcsUtilEdgeCasesTest.java` (new)
   - Comprehensive edge case tests

4. `.gitignore`
   - Added `config.properties` (runtime-generated file)

## Build Commands

```bash
# Run tests
mvn test

# Build fat JAR
mvn clean package

# Run fat JAR
java -jar target/calendar-java-1.0-SNAPSHOT.jar
```

## Deployment Notes

- The fat JAR is now ready for deployment
- Config and data files will be created in the working directory on first run
- Users should run the JAR from a consistent directory to maintain their data
- Both ICS file mode and database mode work correctly from JAR
