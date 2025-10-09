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
}
