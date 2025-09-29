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
}
