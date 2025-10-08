import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for edge cases and error handling in ICS import/export.
 */
public class IcsUtilEdgeCasesTest {

    @Test
    void testExportNullList() throws Exception {
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, null);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(0, back.size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testExportWithNullEntry() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(new CalendarEntry("Valid Entry", "Description", 
                LocalDateTime.of(2025, 9, 1, 10, 0), 
                LocalDateTime.of(2025, 9, 1, 11, 0)));
        src.add(null); // Null entry should be skipped
        src.add(new CalendarEntry("Another Valid", "Description", 
                LocalDateTime.of(2025, 9, 2, 10, 0), 
                LocalDateTime.of(2025, 9, 2, 11, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            // Should have only 2 valid entries, null is skipped
            assertEquals(2, back.size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testExportWithInvalidDates() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        CalendarEntry invalid = new CalendarEntry();
        invalid.setTitle("Invalid Entry");
        invalid.setStart(null); // Invalid: null start
        invalid.setEnd(LocalDateTime.of(2025, 9, 1, 11, 0));
        src.add(invalid);
        
        CalendarEntry valid = new CalendarEntry("Valid", "OK", 
                LocalDateTime.of(2025, 9, 1, 10, 0), 
                LocalDateTime.of(2025, 9, 1, 11, 0));
        src.add(valid);
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            // Should have only 1 valid entry, invalid is skipped
            assertEquals(1, back.size());
            assertEquals("Valid", back.get(0).getTitle());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testImportIcsWithMissingDtstart() throws Exception {
        // ICS with event missing DTSTART should be skipped
        String icsContent = 
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//Test//EN\n" +
            "BEGIN:VEVENT\n" +
            "SUMMARY:No Start Date\n" +
            "DTEND:20251015T110000Z\n" +
            "UID:test@example.com\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            Files.writeString(tmp, icsContent);
            List<CalendarEntry> result = IcsUtil.importIcs(tmp);
            // Event without DTSTART should be skipped
            assertEquals(0, result.size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testImportVcsWithMalformedDates() throws Exception {
        // VCS with malformed dates should not crash
        String vcsContent = 
            "BEGIN:VCALENDAR\n" +
            "VERSION:1.0\n" +
            "BEGIN:VEVENT\n" +
            "SUMMARY:Bad Dates\n" +
            "DTSTART:INVALID\n" +
            "DTEND:ALSO_INVALID\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            Files.writeString(tmp, vcsContent);
            List<CalendarEntry> result = IcsUtil.importVcs(tmp);
            // Should handle gracefully and use fallback (now) or skip
            assertNotNull(result);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsWithOnlyTitle() throws Exception {
        String vcsContent = 
            "BEGIN:VCALENDAR\n" +
            "VERSION:1.0\n" +
            "BEGIN:VEVENT\n" +
            "SUMMARY:Only Title\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            Files.writeString(tmp, vcsContent);
            List<CalendarEntry> result = IcsUtil.importVcs(tmp);
            // Without dates, should not create entry
            assertEquals(0, result.size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsLongDescription() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longDesc.append("Line ").append(i).append(": This is a very long description with lots of content.\n");
        }
        src.add(new CalendarEntry("Long Description Event", longDesc.toString(), 
                LocalDateTime.of(2025, 9, 15, 10, 0), 
                LocalDateTime.of(2025, 9, 15, 11, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            assertTrue(back.get(0).getDescription().length() > 10000);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsUnicodeCharacters() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(new CalendarEntry(
                "日本語タイトル 中文标题 العربية", 
                "Emojis: 😀 🎉 🚀 ✨ Special: € £ ¥ ₹ Math: ∑ ∫ ∂ √", 
                LocalDateTime.of(2025, 9, 20, 10, 0), 
                LocalDateTime.of(2025, 9, 20, 11, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            assertTrue(back.get(0).getTitle().contains("日本語"));
            assertTrue(back.get(0).getDescription().contains("😀"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithEmptyLines() throws Exception {
        // ICS file with empty lines between properties and events
        String icsContent = 
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//Test//EN\n" +
            "\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART:20251015T100000Z\n" +
            "DTEND:20251015T110000Z\n" +
            "SUMMARY:Event with empty lines\n" +
            "\n" +
            "UID:test1@example.com\n" +
            "END:VEVENT\n" +
            "\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART:20251016T140000Z\n" +
            "DTEND:20251016T150000Z\n" +
            "SUMMARY:Second event\n" +
            "UID:test2@example.com\n" +
            "\n" +
            "END:VEVENT\n" +
            "\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            Files.writeString(tmp, icsContent);
            List<CalendarEntry> result = IcsUtil.importIcs(tmp);
            assertEquals(2, result.size());
            assertEquals("Event with empty lines", result.get(0).getTitle());
            assertEquals("Second event", result.get(1).getTitle());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithMinimalInformation() throws Exception {
        // ICS with only required fields (DTSTART, SUMMARY, UID)
        String icsContent = 
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//Test//EN\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART:20251020T100000Z\n" +
            "SUMMARY:Minimal Event\n" +
            "UID:minimal@example.com\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            Files.writeString(tmp, icsContent);
            List<CalendarEntry> result = IcsUtil.importIcs(tmp);
            assertEquals(1, result.size());
            assertEquals("Minimal Event", result.get(0).getTitle());
            assertNotNull(result.get(0).getStart());
            // When no DTEND, should default to same as DTSTART
            assertEquals(result.get(0).getStart(), result.get(0).getEnd());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsFromGoogleCalendar() throws Exception {
        // Simulated Google Calendar format with various properties
        String icsContent = 
            "BEGIN:VCALENDAR\n" +
            "PRODID:-//Google Inc//Google Calendar 70.9054//EN\n" +
            "VERSION:2.0\n" +
            "CALSCALE:GREGORIAN\n" +
            "METHOD:PUBLISH\n" +
            "X-WR-CALNAME:Test Calendar\n" +
            "X-WR-TIMEZONE:Europe/Berlin\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART:20251025T090000Z\n" +
            "DTEND:20251025T100000Z\n" +
            "DTSTAMP:20251001T120000Z\n" +
            "UID:google-event@google.com\n" +
            "CREATED:20251001T120000Z\n" +
            "DESCRIPTION:Meeting notes here\n" +
            "LAST-MODIFIED:20251001T120000Z\n" +
            "LOCATION:Conference Room A\n" +
            "SEQUENCE:0\n" +
            "STATUS:CONFIRMED\n" +
            "SUMMARY:Google Calendar Event\n" +
            "TRANSP:OPAQUE\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            Files.writeString(tmp, icsContent);
            List<CalendarEntry> result = IcsUtil.importIcs(tmp);
            assertEquals(1, result.size());
            assertEquals("Google Calendar Event", result.get(0).getTitle());
            assertEquals("Meeting notes here", result.get(0).getDescription());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsFromOutlook() throws Exception {
        // Simulated Outlook format with different property ordering
        String icsContent = 
            "BEGIN:VCALENDAR\n" +
            "PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN\n" +
            "VERSION:2.0\n" +
            "METHOD:PUBLISH\n" +
            "X-MS-OLK-FORCEINSPECTOROPEN:TRUE\n" +
            "BEGIN:VEVENT\n" +
            "CLASS:PUBLIC\n" +
            "CREATED:20251001T100000Z\n" +
            "DESCRIPTION:Outlook meeting description\n" +
            "DTEND:20251030T160000Z\n" +
            "DTSTAMP:20251001T100000Z\n" +
            "DTSTART:20251030T150000Z\n" +
            "LAST-MODIFIED:20251001T100000Z\n" +
            "PRIORITY:5\n" +
            "SEQUENCE:0\n" +
            "SUMMARY:Outlook Meeting\n" +
            "TRANSP:OPAQUE\n" +
            "UID:outlook-event@outlook.com\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\n" +
            "X-MICROSOFT-CDO-IMPORTANCE:1\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            Files.writeString(tmp, icsContent);
            List<CalendarEntry> result = IcsUtil.importIcs(tmp);
            assertEquals(1, result.size());
            assertEquals("Outlook Meeting", result.get(0).getTitle());
            assertEquals("Outlook meeting description", result.get(0).getDescription());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsWithEmptyLines() throws Exception {
        // VCS file with empty lines
        String vcsContent = 
            "BEGIN:VCALENDAR\n" +
            "VERSION:1.0\n" +
            "\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART:20251101T100000\n" +
            "DTEND:20251101T110000\n" +
            "\n" +
            "SUMMARY:VCS Event with empty lines\n" +
            "DESCRIPTION:Test description\n" +
            "END:VEVENT\n" +
            "\n" +
            "END:VCALENDAR\n";
        
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            Files.writeString(tmp, vcsContent);
            List<CalendarEntry> result = IcsUtil.importVcs(tmp);
            assertEquals(1, result.size());
            assertEquals("VCS Event with empty lines", result.get(0).getTitle());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testMultipleRoundTripsPreserveData() throws Exception {
        // Test that multiple export/import cycles preserve data
        List<CalendarEntry> original = new ArrayList<>();
        CalendarEntry event = new CalendarEntry(
            "Round Trip Test", 
            "This should survive multiple round trips", 
            LocalDateTime.of(2025, 11, 5, 14, 30), 
            LocalDateTime.of(2025, 11, 5, 16, 0)
        );
        event.setCategory("Work");
        event.setRecurrenceRule("FREQ=WEEKLY;COUNT=3");
        event.setReminderMinutesBefore(15);
        original.add(event);
        
        List<CalendarEntry> current = original;
        Path tmp = Files.createTempFile("cal-", ".ics");
        
        try {
            // Do 3 round trips
            for (int i = 0; i < 3; i++) {
                IcsUtil.exportIcs(tmp, current);
                current = IcsUtil.importIcs(tmp);
            }
            
            // Verify data is still intact
            assertEquals(1, current.size());
            CalendarEntry result = current.get(0);
            assertEquals("Round Trip Test", result.getTitle());
            assertEquals("This should survive multiple round trips", result.getDescription());
            assertEquals("Work", result.getCategory());
            assertNotNull(result.getRecurrenceRule());
            assertTrue(result.getRecurrenceRule().contains("FREQ=WEEKLY"));
            assertEquals(15, result.getReminderMinutesBefore());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
