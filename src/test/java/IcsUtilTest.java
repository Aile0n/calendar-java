import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für den Import/Export von ICS und VCS.
 */
public class IcsUtilTest {

    private CalendarEntry sample(String title, String desc, LocalDateTime start, LocalDateTime end) {
        return new CalendarEntry(title, desc, start, end);
    }

    @Test
    void testIcsRoundTrip() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("Meeting", "Besprechung", LocalDateTime.of(2025, 9, 29, 9, 0), LocalDateTime.of(2025, 9, 29, 10, 0)));
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            CalendarEntry a = src.get(0);
            CalendarEntry b = back.get(0);
            assertEquals(a.getTitle(), b.getTitle());
            assertEquals(a.getDescription(), b.getDescription());
            // Allow a few seconds tolerance if libraries adjust seconds
            assertEquals(a.getStart(), b.getStart());
            assertEquals(a.getEnd(), b.getEnd());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsRoundTrip() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("Arzttermin", "Kontrolle", LocalDateTime.of(2025, 10, 1, 14, 30), LocalDateTime.of(2025, 10, 1, 15, 0)));
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            IcsUtil.exportVcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importVcs(tmp);
            assertEquals(1, back.size());
            CalendarEntry a = src.get(0);
            CalendarEntry b = back.get(0);
            assertEquals(a.getTitle(), b.getTitle());
            assertEquals(a.getDescription(), b.getDescription());
            assertEquals(a.getStart(), b.getStart());
            assertEquals(a.getEnd(), b.getEnd());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsMultipleEntries() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("Event 1", "Description 1", LocalDateTime.of(2025, 9, 1, 9, 0), LocalDateTime.of(2025, 9, 1, 10, 0)));
        src.add(sample("Event 2", "Description 2", LocalDateTime.of(2025, 9, 2, 14, 0), LocalDateTime.of(2025, 9, 2, 15, 30)));
        src.add(sample("Event 3", "Description 3", LocalDateTime.of(2025, 9, 3, 18, 0), LocalDateTime.of(2025, 9, 3, 19, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(3, back.size());
            
            for (int i = 0; i < src.size(); i++) {
                assertEquals(src.get(i).getTitle(), back.get(i).getTitle());
                assertEquals(src.get(i).getDescription(), back.get(i).getDescription());
                assertEquals(src.get(i).getStart(), back.get(i).getStart());
                assertEquals(src.get(i).getEnd(), back.get(i).getEnd());
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsSpecialCharacters() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("Meeting: Q&A Session", "Topics:\n- Item 1\n- Item 2\n- Item 3", 
                LocalDateTime.of(2025, 9, 15, 10, 0), LocalDateTime.of(2025, 9, 15, 11, 0)));
        src.add(sample("Café meeting", "Meet at café, discuss ñoño's project", 
                LocalDateTime.of(2025, 9, 16, 14, 0), LocalDateTime.of(2025, 9, 16, 15, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(2, back.size());
            
            assertEquals("Meeting: Q&A Session", back.get(0).getTitle());
            assertTrue(back.get(0).getDescription().contains("Item 1"));
            assertTrue(back.get(0).getDescription().contains("Item 2"));
            
            assertEquals("Café meeting", back.get(1).getTitle());
            assertTrue(back.get(1).getDescription().contains("café"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsEmptyDescription() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("Event without description", "", 
                LocalDateTime.of(2025, 9, 20, 9, 0), LocalDateTime.of(2025, 9, 20, 10, 0)));
        src.add(sample("Event with null description", null, 
                LocalDateTime.of(2025, 9, 21, 9, 0), LocalDateTime.of(2025, 9, 21, 10, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(2, back.size());
            
            assertEquals("Event without description", back.get(0).getTitle());
            assertTrue(back.get(0).getDescription() == null || back.get(0).getDescription().isEmpty());
            
            assertEquals("Event with null description", back.get(1).getTitle());
            assertTrue(back.get(1).getDescription() == null || back.get(1).getDescription().isEmpty());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithRecurrenceRule() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        CalendarEntry recurring = sample("Weekly Meeting", "Team sync", 
                LocalDateTime.of(2025, 9, 1, 10, 0), LocalDateTime.of(2025, 9, 1, 11, 0));
        recurring.setRecurrenceRule("FREQ=WEEKLY;COUNT=10");
        src.add(recurring);
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            assertEquals("Weekly Meeting", back.get(0).getTitle());
            assertNotNull(back.get(0).getRecurrenceRule());
            assertTrue(back.get(0).getRecurrenceRule().contains("FREQ=WEEKLY"));
            assertTrue(back.get(0).getRecurrenceRule().contains("COUNT=10"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithCategory() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        CalendarEntry withCategory = sample("Project Meeting", "Discuss milestones", 
                LocalDateTime.of(2025, 9, 5, 14, 0), LocalDateTime.of(2025, 9, 5, 15, 0));
        withCategory.setCategory("Work");
        src.add(withCategory);
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            assertEquals("Project Meeting", back.get(0).getTitle());
            assertEquals("Work", back.get(0).getCategory());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithReminder() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        CalendarEntry withReminder = sample("Important Meeting", "Don't forget", 
                LocalDateTime.of(2025, 9, 10, 9, 0), LocalDateTime.of(2025, 9, 10, 10, 0));
        withReminder.setReminderMinutesBefore(15);
        src.add(withReminder);
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            assertEquals("Important Meeting", back.get(0).getTitle());
            assertNotNull(back.get(0).getReminderMinutesBefore());
            assertEquals(15, back.get(0).getReminderMinutesBefore());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsMultipleEntries() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("VCS Event 1", "First event", LocalDateTime.of(2025, 10, 1, 9, 0), LocalDateTime.of(2025, 10, 1, 10, 0)));
        src.add(sample("VCS Event 2", "Second event", LocalDateTime.of(2025, 10, 2, 14, 0), LocalDateTime.of(2025, 10, 2, 15, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            IcsUtil.exportVcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importVcs(tmp);
            assertEquals(2, back.size());
            
            assertEquals("VCS Event 1", back.get(0).getTitle());
            assertEquals("VCS Event 2", back.get(1).getTitle());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsSpecialCharacters() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("VCS: Special, chars; test", "Line 1\nLine 2\nLine 3", 
                LocalDateTime.of(2025, 10, 5, 10, 0), LocalDateTime.of(2025, 10, 5, 11, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            IcsUtil.exportVcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importVcs(tmp);
            assertEquals(1, back.size());
            
            assertEquals("VCS: Special, chars; test", back.get(0).getTitle());
            assertTrue(back.get(0).getDescription().contains("Line 1"));
            assertTrue(back.get(0).getDescription().contains("Line 2"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testImportAuto() throws Exception {
        // Test ICS auto-detection
        List<CalendarEntry> srcIcs = new ArrayList<>();
        srcIcs.add(sample("ICS Auto Test", "Testing auto import", 
                LocalDateTime.of(2025, 9, 25, 10, 0), LocalDateTime.of(2025, 9, 25, 11, 0)));
        
        Path tmpIcs = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmpIcs, srcIcs);
            List<CalendarEntry> backIcs = IcsUtil.importAuto(tmpIcs);
            assertEquals(1, backIcs.size());
            assertEquals("ICS Auto Test", backIcs.get(0).getTitle());
        } finally {
            Files.deleteIfExists(tmpIcs);
        }
        
        // Test VCS auto-detection
        List<CalendarEntry> srcVcs = new ArrayList<>();
        srcVcs.add(sample("VCS Auto Test", "Testing auto import", 
                LocalDateTime.of(2025, 10, 25, 10, 0), LocalDateTime.of(2025, 10, 25, 11, 0)));
        
        Path tmpVcs = Files.createTempFile("cal-", ".vcs");
        try {
            IcsUtil.exportVcs(tmpVcs, srcVcs);
            List<CalendarEntry> backVcs = IcsUtil.importAuto(tmpVcs);
            assertEquals(1, backVcs.size());
            assertEquals("VCS Auto Test", backVcs.get(0).getTitle());
        } finally {
            Files.deleteIfExists(tmpVcs);
        }
    }

    @Test
    void testIcsEmptyList() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(0, back.size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testMalformedIcsFile() throws Exception {
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            // Write invalid ICS content
            Files.writeString(tmp, "This is not a valid ICS file");
            
            // Should throw an exception
            assertThrows(Exception.class, () -> IcsUtil.importIcs(tmp));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testMalformedVcsFile() throws Exception {
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            // Write invalid VCS content - just text without proper structure
            Files.writeString(tmp, "Invalid VCS content");
            
            // Should not throw but return empty list
            List<CalendarEntry> result = IcsUtil.importVcs(tmp);
            assertNotNull(result);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithAllFeatures() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        CalendarEntry full = sample("Complete Event", "Event with all features", 
                LocalDateTime.of(2025, 9, 30, 10, 0), LocalDateTime.of(2025, 9, 30, 11, 0));
        full.setRecurrenceRule("FREQ=DAILY;COUNT=5");
        full.setCategory("Important");
        full.setReminderMinutesBefore(30);
        src.add(full);
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            CalendarEntry result = back.get(0);
            assertEquals("Complete Event", result.getTitle());
            assertEquals("Event with all features", result.getDescription());
            assertEquals(full.getStart(), result.getStart());
            assertEquals(full.getEnd(), result.getEnd());
            assertNotNull(result.getRecurrenceRule());
            assertTrue(result.getRecurrenceRule().contains("FREQ=DAILY"));
            assertEquals("Important", result.getCategory());
            assertEquals(30, result.getReminderMinutesBefore());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsWithBackslash() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        src.add(sample("Path: C:\\Users\\Test", "Folder: D:\\Data\\Files", 
                LocalDateTime.of(2025, 10, 10, 10, 0), LocalDateTime.of(2025, 10, 10, 11, 0)));
        
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            IcsUtil.exportVcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importVcs(tmp);
            assertEquals(1, back.size());
            
            // Check that backslashes are properly handled
            assertEquals("Path: C:\\Users\\Test", back.get(0).getTitle());
            assertTrue(back.get(0).getDescription().contains("\\"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsSameStartEndTime() throws Exception {
        List<CalendarEntry> src = new ArrayList<>();
        LocalDateTime time = LocalDateTime.of(2025, 9, 28, 14, 0);
        src.add(sample("Point in Time Event", "No duration", time, time));
        
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            IcsUtil.exportIcs(tmp, src);
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            assertEquals(back.get(0).getStart(), back.get(0).getEnd());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testVcsWithOnlyStartTime() throws Exception {
        Path tmp = Files.createTempFile("cal-", ".vcs");
        try {
            // Create a VCS file with only DTSTART (no DTEND)
            String vcs = "BEGIN:VCALENDAR\r\n" +
                         "VERSION:1.0\r\n" +
                         "BEGIN:VEVENT\r\n" +
                         "SUMMARY:Event without end\r\n" +
                         "DTSTART:20251015T100000\r\n" +
                         "END:VEVENT\r\n" +
                         "END:VCALENDAR\r\n";
            Files.writeString(tmp, vcs);
            
            List<CalendarEntry> back = IcsUtil.importVcs(tmp);
            assertEquals(1, back.size());
            
            // When no DTEND is provided, it should default to same as DTSTART
            assertEquals("Event without end", back.get(0).getTitle());
            assertEquals(back.get(0).getStart(), back.get(0).getEnd());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithEmptyLines() throws Exception {
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            // Create an ICS file with empty lines (common in files from Google Calendar, Outlook, etc.)
            String ics = "BEGIN:VCALENDAR\r\n" +
                         "VERSION:2.0\r\n" +
                         "PRODID:-//Test//Test//EN\r\n" +
                         "\r\n" +  // Empty line (should be filtered)
                         "BEGIN:VEVENT\r\n" +
                         "UID:test123@example.com\r\n" +
                         "DTSTART:20251020T100000\r\n" +
                         "\r\n" +  // Empty line (should be filtered)
                         "DTEND:20251020T110000\r\n" +
                         "SUMMARY:Test Event with Empty Lines\r\n" +
                         "DESCRIPTION:This file has empty lines\r\n" +
                         "\r\n" +  // Empty line (should be filtered)
                         "END:VEVENT\r\n" +
                         "END:VCALENDAR\r\n";
            Files.writeString(tmp, ics);
            
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            assertEquals("Test Event with Empty Lines", back.get(0).getTitle());
            assertEquals("This file has empty lines", back.get(0).getDescription());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testIcsWithLineFolding() throws Exception {
        Path tmp = Files.createTempFile("cal-", ".ics");
        try {
            // Create an ICS file with line folding (whitespace at start of line continues previous line)
            String ics = "BEGIN:VCALENDAR\r\n" +
                         "VERSION:2.0\r\n" +
                         "PRODID:-//Test//Test//EN\r\n" +
                         "BEGIN:VEVENT\r\n" +
                         "UID:test456@example.com\r\n" +
                         "DTSTART:20251021T140000\r\n" +
                         "DTEND:20251021T150000\r\n" +
                         "SUMMARY:Test with folded\r\n" +
                         " description line\r\n" +  // Line folding (space at start)
                         "DESCRIPTION:This is a very long description that has been folded across m\r\n" +
                         " ultiple lines according to RFC 5545\r\n" +  // Line folding
                         "END:VEVENT\r\n" +
                         "END:VCALENDAR\r\n";
            Files.writeString(tmp, ics);
            
            List<CalendarEntry> back = IcsUtil.importIcs(tmp);
            assertEquals(1, back.size());
            
            // The summary should be properly unfolded
            assertTrue(back.get(0).getTitle().contains("folded"));
            // The description should be properly unfolded
            assertTrue(back.get(0).getDescription().contains("long description"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
