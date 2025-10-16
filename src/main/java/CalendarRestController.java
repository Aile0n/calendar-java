import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST-Controller für Kalender-Operationen.
 * Stellt REST-Endpunkte bereit für:
 * - Auflisten aller Kalendereinträge
 * - Erstellen neuer Einträge
 * - Aktualisieren bestehender Einträge
 * - Löschen von Einträgen
 * - Import/Export von ICS/VCS-Dateien
 */
@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "*")
public class CalendarRestController {

    private final Path icsPath;

    public CalendarRestController() {
        // ICS-Pfad aus Config oder Default verwenden
        try {
            this.icsPath = ConfigUtil.getIcsPath();
            // Sicherstellen, dass die Datei existiert
            if (!Files.exists(icsPath)) {
                Path parent = icsPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                IcsUtil.exportIcs(icsPath, new ArrayList<>());
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Initialisieren des ICS-Pfads", e);
        }
    }

    /**
     * GET /api/calendar/entries
     * Gibt alle Kalendereinträge zurück
     */
    @GetMapping("/entries")
    public ResponseEntity<List<CalendarEntry>> getAllEntries() {
        try {
            List<CalendarEntry> entries = IcsUtil.importIcs(icsPath);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /api/calendar/entries
     * Erstellt einen neuen Kalendereintrag
     */
    @PostMapping("/entries")
    public ResponseEntity<CalendarEntry> createEntry(@RequestBody CalendarEntry entry) {
        try {
            // Validierung
            if (entry.getTitle() == null || entry.getTitle().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            if (entry.getStart() == null || entry.getEnd() == null) {
                return ResponseEntity.badRequest().build();
            }
            if (entry.getEnd().isBefore(entry.getStart())) {
                return ResponseEntity.badRequest().build();
            }

            // Lade existierende Einträge
            List<CalendarEntry> entries = IcsUtil.importIcs(icsPath);
            entries.add(entry);
            
            // Speichere aktualisierte Liste
            IcsUtil.exportIcs(icsPath, entries);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(entry);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/calendar/entries/{index}
     * Aktualisiert einen existierenden Kalendereintrag
     */
    @PutMapping("/entries/{index}")
    public ResponseEntity<CalendarEntry> updateEntry(@PathVariable int index, @RequestBody CalendarEntry entry) {
        try {
            // Validierung
            if (entry.getTitle() == null || entry.getTitle().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            if (entry.getStart() == null || entry.getEnd() == null) {
                return ResponseEntity.badRequest().build();
            }
            if (entry.getEnd().isBefore(entry.getStart())) {
                return ResponseEntity.badRequest().build();
            }

            List<CalendarEntry> entries = IcsUtil.importIcs(icsPath);
            if (index < 0 || index >= entries.size()) {
                return ResponseEntity.notFound().build();
            }
            
            entries.set(index, entry);
            IcsUtil.exportIcs(icsPath, entries);
            
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/calendar/entries/{index}
     * Löscht einen Kalendereintrag
     */
    @DeleteMapping("/entries/{index}")
    public ResponseEntity<Void> deleteEntry(@PathVariable int index) {
        try {
            List<CalendarEntry> entries = IcsUtil.importIcs(icsPath);
            if (index < 0 || index >= entries.size()) {
                return ResponseEntity.notFound().build();
            }
            
            entries.remove(index);
            IcsUtil.exportIcs(icsPath, entries);
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/calendar/import
     * Importiert Einträge aus einer ICS/VCS-Datei
     */
    @PostMapping("/import")
    public ResponseEntity<String> importFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Keine Datei hochgeladen");
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return ResponseEntity.badRequest().body("Ungültiger Dateiname");
            }
            
            // Temporäre Datei erstellen
            Path tempFile = Files.createTempFile("import-", filename);
            file.transferTo(tempFile.toFile());
            
            // Import durchführen
            List<CalendarEntry> importedEntries = IcsUtil.importAuto(tempFile);
            
            // Mit existierenden Einträgen zusammenführen
            List<CalendarEntry> existingEntries = IcsUtil.importIcs(icsPath);
            existingEntries.addAll(importedEntries);
            IcsUtil.exportIcs(icsPath, existingEntries);
            
            // Temporäre Datei löschen
            Files.deleteIfExists(tempFile);
            
            return ResponseEntity.ok(importedEntries.size() + " Einträge importiert");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Import: " + e.getMessage());
        }
    }

    /**
     * GET /api/calendar/export
     * Exportiert alle Einträge als ICS-Datei
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCalendar() {
        try {
            byte[] content = Files.readAllBytes(icsPath);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/calendar")
                    .header("Content-Disposition", "attachment; filename=calendar-export.ics")
                    .body(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/calendar/config
     * Gibt die aktuelle Konfiguration zurück
     */
    @GetMapping("/config")
    public ResponseEntity<ConfigInfo> getConfig() {
        try {
            ConfigInfo config = new ConfigInfo();
            config.icsPath = ConfigUtil.getIcsPath().toString();
            config.darkMode = ConfigUtil.isDarkMode();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/calendar/config
     * Aktualisiert die Konfiguration
     */
    @PostMapping("/config")
    public ResponseEntity<String> updateConfig(@RequestBody ConfigInfo config) {
        try {
            if (config.icsPath != null) {
                ConfigUtil.setIcsPath(Paths.get(config.icsPath));
            }
            if (config.darkMode != null) {
                ConfigUtil.setDarkMode(config.darkMode);
            }
            ConfigUtil.save();
            return ResponseEntity.ok("Konfiguration gespeichert");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Speichern: " + e.getMessage());
        }
    }

    /**
     * Hilfsklasse für Konfigurationsinformationen
     */
    public static class ConfigInfo {
        public String icsPath;
        public Boolean darkMode;
    }
}
