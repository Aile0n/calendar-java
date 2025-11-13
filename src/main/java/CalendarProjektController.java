import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * JavaFX-Controller für die FXML-Ansicht.
 *
 * Diese Klasse ist das "Gehirn" der Benutzeroberfläche.
 * Sie verbindet die Buttons und UI-Elemente aus der calendar_view.fxml Datei
 * mit der eigentlichen Programmlogik.
 *
 * Wichtige Aufgaben:
 * - Lädt Termine aus der ICS-Datei und zeigt sie im Kalender an
 * - Reagiert auf Button-Klicks (Neuer Termin, Import, Export, etc.)
 * - Speichert Änderungen automatisch zurück in die ICS-Datei
 * - Verwaltet Kategorien und Erinnerungen
 */
public class CalendarProjektController implements Initializable {

    // --- UI-Elemente aus der FXML-Datei ---
    // @FXML bedeutet: Diese Elemente werden automatisch mit der FXML-Datei verknüpft

    /** Der Container, in dem die Kalenderansicht angezeigt wird */
    @FXML private AnchorPane calendarContainer;

    /** Button zum Erstellen eines neuen Termins */
    @FXML private Button newButton;

    /** Button zum Importieren von Kalenderdateien */
    @FXML private Button importButton;

    /** Button zum Exportieren von Kalenderdateien */
    @FXML private Button exportButton;

    /** Button für die Einstellungen */
    @FXML private Button settingsButton;

    /** Button zum Beenden der Anwendung */
    @FXML private Button exitButton;

    /** Info-Button in der unteren Leiste */
    @FXML private Button infoButton;

    /** Label für allgemeine Statusmeldungen */
    @FXML private Label statusLabel;

    /** Label speziell für Speicher-Status */
    @FXML private Label saveStatusLabel;

    /** Optionaler Button zum manuellen Speichern (wird ggf. dynamisch angelegt) */
    @FXML private Button manualSaveButton;

    // --- Interne Variablen ---

    /** Die CalendarFX-Komponente, die den Kalender visuell darstellt */
    private CalendarView calendarView;

    /** Der Standard-Kalender für allgemeine Termine */
    private final Calendar<String> fxCalendar = new Calendar<>("Allgemein");

    /** Eine Map (Zuordnung) von Kategorie-Namen zu Kalendern */
    private final java.util.Map<String, Calendar<String>> categoryCalendars = new java.util.HashMap<>();

    /** Die Zeitzone des Systems (z.B. "Europe/Berlin") */
    private final ZoneId zone = ZoneId.systemDefault();

    /** Liste aller aktuell geladenen Termine (wird in ICS-Datei gespeichert) */
    private final List<CalendarEntry> currentEntries = new ArrayList<>();

    // --- Debug und Status-Flags ---

    /** Verhindert, dass beim Laden der Daten versehentlich alles gelöscht wird */
    private boolean suppressAutoSave = true;

    /** Wurde die initiale Datenladung abgeschlossen? */
    private boolean initialLoadCompleted = false;

    /** Wie viele Einträge wurden zuletzt gespeichert? (zum Vergleich) */
    private int lastSavedCount = -1;

    /** Verhindert, dass Listener mehrfach registriert werden */
    private boolean calendarEventsHooked = false;

    /**
     * Hilfsmethode zum Loggen von Debug-Meldungen.
     * Schreibt formatierte Meldungen in die Konsole.
     *
     * @param action Was passiert gerade (z.B. "INIT", "SAVE")
     * @param msg Die detaillierte Nachricht
     */
    private void log(String action, String msg) {
        System.out.println("[UI_DEBUG] " + action + " | " + msg);
    }

    /**
     * Initialize wird automatisch von JavaFX aufgerufen, nachdem die FXML geladen wurde.
     * Hier bauen wir die Benutzeroberfläche auf und laden die gespeicherten Termine.
     *
     * @param location Die URL der FXML-Datei (wird automatisch übergeben)
     * @param resources Sprachressourcen (wird automatisch übergeben)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log("INIT", "Starte Initialisierung");
        setStatus("Status: Initialisierung");

        // Stelle die Sprache auf Deutsch um (für Datums- und Zeitformate)
        java.util.Locale.setDefault(java.util.Locale.GERMANY);

        // Erstelle die CalendarFX-Komponente (die eigentliche Kalenderansicht)
        calendarView = new CalendarView();

        // Erstelle eine "Kalenderquelle" und füge unseren Standard-Kalender hinzu
        // (CalendarFX kann mehrere Kalender gleichzeitig anzeigen)
        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);

        // Wenn das Design geändert wird (Hell-/Dunkelmodus), passe es an
        calendarContainer.sceneProperty().addListener((obs, oldS, newS) -> applyTheme());
        applyTheme();

        // Füge die Kalenderansicht zum Container hinzu und lasse sie den ganzen Platz nutzen
        AnchorPane.setTopAnchor(calendarView, 0.0);
        AnchorPane.setRightAnchor(calendarView, 0.0);
        AnchorPane.setBottomAnchor(calendarView, 0.0);
        AnchorPane.setLeftAnchor(calendarView, 0.0);
        calendarContainer.getChildren().add(calendarView);

        // Verbinde alle Buttons mit ihren Aktionen (was passiert beim Klick?)
        newButton.setOnAction(this::onNewEntry);
        importButton.setOnAction(this::onImport);
        exportButton.setOnAction(this::onExport);
        settingsButton.setOnAction(this::onSettings);
        if (exitButton != null) {
            exitButton.setOnAction(this::onExit);
        }
        if (infoButton != null) {
            infoButton.setOnAction(this::onInfo);
        }

        // Richte Listener ein, die automatisch speichern, wenn sich etwas ändert
        setupCalendarListeners();

        // Lade die gespeicherten Termine aus der ICS-Datei
        reloadData();

        // Initialisierung abgeschlossen - ab jetzt automatisch speichern
        initialLoadCompleted = true;
        suppressAutoSave = false;
        startAutosaveMonitor();
        setStatus("Status: Geladen (" + currentEntries.size() + ")");
        log("INIT", "Initialisierung abgeschlossen");

        // Versuche, einen manuellen Speichern-Button hinzuzufügen
        calendarContainer.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) {
                javafx.application.Platform.runLater(this::ensureManualSaveButton);
            }
        });
    }

    /**
     * Aktualisiert die Statusanzeige im UI.
     *
     * @param text Der anzuzeigende Text (z.B. "Status: Geladen (5)")
     */
    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    /**
     * Aktualisiert die Speicher-Statusanzeige und färbt sie entsprechend ein.
     *
     * @param text Der anzuzeigende Text (z.B. "Gespeichert" oder "Fehler")
     */
    private void setSaveStatus(String text) {
        if (saveStatusLabel != null) {
            saveStatusLabel.setText(text);
            // Farbe anpassen: Grün bei Erfolg, Rot bei Fehler
            if (text.contains("erfolgreich") || text.equals("Gespeichert")) {
                saveStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
            } else if (text.contains("Fehler")) {
                saveStatusLabel.setStyle("-fx-text-fill: #F44336;");
            } else {
                saveStatusLabel.setStyle("-fx-text-fill: #666;");
            }
        }
    }

    /**
     * Stellt sicher, dass ein manueller Speichern-Button vorhanden ist.
     * Falls er in der FXML fehlt, wird er dynamisch hinzugefügt.
     */
    private void ensureManualSaveButton() {
        try {
            if (manualSaveButton != null) return; // Bereits vorhanden via FXML

            // Suche die Toolbar im UI
            var tb = calendarContainer.getScene().lookup("#toolBar");
            if (tb instanceof ToolBar toolBar) {
                manualSaveButton = new Button("Speichern");
                manualSaveButton.setOnAction(e -> manualSaveAction());
                toolBar.getItems().add(toolBar.getItems().size() - 1, manualSaveButton);
                log("UI", "Manueller Speichern-Button hinzugefügt");
            }
        } catch (Exception ex) {
            log("ERROR", "Konnte manuellen Speichern-Button nicht hinzufügen: " + ex.getMessage());
        }
    }

    /**
     * Wird aufgerufen, wenn der Benutzer auf den manuellen Speichern-Button klickt.
     */
    private void manualSaveAction() {
        log("MANUAL_SAVE", "Benutzer ausgelöst");
        saveCurrentEntriesToIcs();
    }

    /**
     * Lädt alle Termine neu aus der ICS-Datei.
     * Dies geschieht beim Start und nach dem Ändern der Einstellungen.
     */
    private void reloadData() {
        suppressAutoSave = true; // Während dem Laden nichts automatisch speichern!
        log("RELOAD", "Lade Daten (ICS)");

        // Lösche alle aktuell angezeigten Termine
        fxCalendar.clear();
        for (Calendar<String> cal : categoryCalendars.values()) cal.clear();

        try {
            currentEntries.clear();
            var path = ConfigUtil.getIcsPath();

            // Falls die ICS-Datei noch nicht existiert, erstelle eine leere
            if (!Files.exists(path)) {
                java.nio.file.Path parent = path.getParent();
                if (parent == null) parent = java.nio.file.Paths.get(".");
                if (Files.exists(parent) && Files.isWritable(parent)) {
                    try {
                        IcsUtil.exportIcs(path, new ArrayList<>());
                    } catch (Exception ex) {
                        log("RELOAD", "Konnte neue ICS nicht erzeugen: " + ex.getMessage());
                    }
                }
            }

            // Lade die Termine aus der ICS-Datei
            if (Files.exists(path)) {
                currentEntries.addAll(IcsUtil.importIcs(path));
            }

            log("RELOAD", "ICS-Einträge geladen: " + currentEntries.size());

            // Zeige die geladenen Termine im Kalender an
            populateCalendar(currentEntries);

            // Plane Erinnerungen für die Termine
            scheduleReminders(currentEntries);

        } catch (Exception ex) {
            log("ERROR", "Fehler beim Laden aus ICS: " + ex.getMessage());
            showError("Fehler beim Laden aus ICS", ex);
        }

        suppressAutoSave = false; // Nach dem Laden wieder automatisch speichern erlauben
    }

    /**
     * Füllt die Kalenderansicht mit den geladenen Terminen.
     *
     * @param items Die Liste der anzuzeigenden Termine
     */
    @SuppressWarnings("unchecked")
    private void populateCalendar(List<CalendarEntry> items) {
        log("POPULATE", "Übernehme Einträge in CalendarFX: count=" + items.size());

        // Lösche alle bestehenden Einträge
        fxCalendar.clear();
        for (Calendar<String> cal : categoryCalendars.values()) cal.clear();

        // Gehe durch jeden Termin und füge ihn zum passenden Kalender hinzu
        for (CalendarEntry ce : items) {
            // Erstelle ein CalendarFX-Entry-Objekt
            @SuppressWarnings("unchecked")
            Entry<String> entry = new Entry<>(ce.getTitle());

            // Setze die Beschreibung (in CalendarFX heißt das "Location")
            if (ce.getDescription() != null && !ce.getDescription().isBlank()) {
                entry.setLocation(ce.getDescription());
            }

            // Setze Start- und Endzeit
            entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));

            // Finde den richtigen Kalender basierend auf der Kategorie
            String cat = (ce.getCategory() == null || ce.getCategory().isBlank()) ? "Allgemein" : ce.getCategory();
            Calendar<String> target = getOrCreateCalendar(cat);
            target.addEntry(entry);
        }

        setStatus("Status: Kalender geladen (" + items.size() + ")");
    }

    /**
     * Erstellt eine aktuelle Liste aller Termine aus dem UI.
     * Dies ist wichtig, weil der Benutzer Termine direkt im CalendarFX-View
     * verschieben, ändern oder löschen kann.
     */
    private void rebuildCurrentEntriesFromUI() {
        currentEntries.clear();
        int calendarCount = 0;
        int entryCount = 0;

        // Gehe durch alle Kalender-Quellen
        for (CalendarSource source : calendarView.getCalendarSources()) {
            // Gehe durch alle Kalender in dieser Quelle
            for (Calendar<?> calendar : source.getCalendars()) {
                calendarCount++;

                // Finde alle Einträge in diesem Kalender (leerer String = alle)
                List<Entry<?>> entries = calendar.findEntries("");

                // Wandle jeden CalendarFX-Entry in ein CalendarEntry um
                for (Entry<?> entry : entries) {
                    String title = entry.getTitle() != null ? entry.getTitle() : "(Ohne Titel)";
                    String description = entry.getLocation() != null ? entry.getLocation() : "";
                    LocalDateTime start = entry.getStartAsLocalDateTime();
                    LocalDateTime end = entry.getEndAsLocalDateTime();

                    CalendarEntry ce = new CalendarEntry(title, description, start, end);

                    // Speichere die Kategorie (Kalendername)
                    String calendarName = calendar.getName();
                    if (calendarName != null && !calendarName.isEmpty() && !"Allgemein".equalsIgnoreCase(calendarName)) {
                        ce.setCategory(calendarName);
                    }

                    currentEntries.add(ce);
                    entryCount++;
                }
            }
        }

        log("REBUILD", "Kalender geprüft=" + calendarCount + ", Einträge gesammelt=" + entryCount);
    }

    /**
     * Holt einen Kalender für eine bestimmte Kategorie oder erstellt ihn, falls er noch nicht existiert.
     *
     * @param category Der Name der Kategorie (z.B. "Arbeit", "Privat")
     * @return Der Kalender für diese Kategorie
     */
    private Calendar<String> getOrCreateCalendar(String category) {
        // "Allgemein" ist der Standard-Kalender
        if ("Allgemein".equalsIgnoreCase(category)) return fxCalendar;

        // Suche oder erstelle einen Kalender für diese Kategorie
        return categoryCalendars.computeIfAbsent(category, c -> {
            Calendar<String> cal = new Calendar<>(c);

            // Wähle eine Farbe für den Kalender (basierend auf dem Namen)
            Calendar.Style[] styles = Calendar.Style.values();
            Calendar.Style style = styles[Math.abs(c.hashCode()) % styles.length];
            cal.setStyle(style);

            // Füge den neuen Kalender zur Ansicht hinzu
            for (CalendarSource src : calendarView.getCalendarSources()) {
                if (!src.getCalendars().contains(cal)) {
                    src.getCalendars().add(cal);
                    addCalendarListener(cal); // Überwache Änderungen in diesem Kalender
                    log("CAL", "Neuer Kategorie-Kalender erstellt: " + c);
                    break;
                }
            }
            return cal;
        });
    }

    /**
     * Richtet Listener ein, die automatisch speichern, wenn sich Termine ändern.
     * Das umfasst: Hinzufügen, Bearbeiten, Löschen und Verschieben von Terminen.
     */
    private void setupCalendarListeners() {
        addCalendarListener(fxCalendar);
        for (Calendar<String> cal : categoryCalendars.values()) {
            addCalendarListener(cal);
        }
    }

    /**
     * Fügt Listener zu einem Kalender hinzu, die bei Änderungen reagieren.
     *
     * @param calendar Der zu überwachende Kalender
     */
    private void addCalendarListener(Calendar<?> calendar) {
        // Registriere einmal global an der CalendarView
        if (calendarView != null && !calendarEventsHooked) {
            calendarView.addEventHandler(com.calendarfx.model.CalendarEvent.ANY, event -> {
                if (suppressAutoSave) {
                    log("CAL_EVENT_SUPPRESS", "Eintragsänderung während Initialisierung ignoriert");
                    return;
                }
                log("CAL_EVENT_VIEW", "Änderung erkannt (View), Auto-Save");
                saveCurrentEntriesToIcs();
            });
            calendarEventsHooked = true;
            log("CAL", "Globaler CalendarFX-Listener registriert");
        }
    }

    /**
     * Speichert alle aktuellen Termine in die ICS-Datei.
     * Dies wird automatisch aufgerufen, wenn sich etwas ändert.
     */
    private void saveCurrentEntriesToIcs() {
        if (suppressAutoSave) {
            log("SAVE_ICS_SUPPRESS", "Speichern unterdrückt (Initialisierung)");
            return;
        }

        try {
            // Sammle alle Termine aus dem UI
            rebuildCurrentEntriesFromUI();

            // Sicherheitscheck: Verhindere versehentliches Löschen aller Daten
            if (!initialLoadCompleted && currentEntries.isEmpty() && lastSavedCount > 0) {
                log("SAVE_ICS_SKIP", "Leere Liste erkannt vor initialLoadCompleted – Speichern übersprungen");
                return;
            }

            // Erstelle ein Backup, falls wir von vielen Terminen auf 0 gehen würden
            if (lastSavedCount > 0 && currentEntries.isEmpty()) {
                try {
                    java.nio.file.Path path = ConfigUtil.getIcsPath();
                    if (java.nio.file.Files.exists(path)) {
                        java.nio.file.Path backup = path.resolveSibling(path.getFileName().toString() + ".bak");
                        java.nio.file.Files.copy(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        log("SAVE_ICS_BACKUP", "Backup angelegt: " + backup);
                    }
                } catch (Exception exb) {
                    log("ERROR", "Backup fehlgeschlagen: " + exb.getMessage());
                }
            }

            // Schreibe die Termine in die ICS-Datei
            log("SAVE_ICS", "Schreibe Einträge: count=" + currentEntries.size() + " -> " + ConfigUtil.getIcsPath());
            IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            lastSavedCount = currentEntries.size();

            // Aktualisiere die Statusanzeige
            setStatus("Status: Gespeichert (" + lastSavedCount + ")");
            setSaveStatus("Speichern erfolgreich");

        } catch (Exception ex) {
            setStatus("Status: Fehler beim Speichern");
            setSaveStatus("Fehler beim Speichern");
            log("ERROR", "Auto-save fehlgeschlagen: " + ex.getMessage());
        }
    }

    private void onNewEntry(ActionEvent evt) {
        log("NEW_DIALOG", "Öffne Dialog für neuen Termin");
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Neuer Termin");
        ButtonType saveType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");

        TextField titleField = new TextField();
        titleField.setPromptText("Titel");
        TextField descField = new TextField();
        descField.setPromptText("Beschreibung (optional)");
        DatePicker startDate = new DatePicker(LocalDate.now());
        TextField startTime = new TextField("09:00");
        DatePicker endDate = new DatePicker(LocalDate.now());
        TextField endTime = new TextField("10:00");

        grid.add(new Label("Titel:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Beschreibung:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Start (Datum / Zeit):"), 0, 2);
        grid.add(startDate, 1, 2);
        grid.add(startTime, 2, 2);
        grid.add(new Label("Ende (Datum / Zeit):"), 0, 3);
        grid.add(endDate, 1, 3);
        grid.add(endTime, 2, 3);

        dialog.getDialogPane().setContent(grid);

        var saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (!validateInputs(titleField.getText(), startDate.getValue(), startTime.getText(), endDate.getValue(), endTime.getText())) {
                log("NEW_VALIDATE", "Validierung fehlgeschlagen für Titel='" + titleField.getText() + "'");
                e.consume();
                Alert a = new Alert(Alert.AlertType.WARNING, "Bitte Titel angeben und gültige Zeiten im Format HH:mm eingeben. Ende muss nach Start liegen.", ButtonType.OK);
                a.setHeaderText("Eingaben prüfen");
                a.showAndWait();
                return;
            }
            try {
                LocalTime parsedStart = parseTime(startTime.getText());
                LocalTime parsedEnd = parseTime(endTime.getText());
                if (parsedStart == null || parsedEnd == null) {
                    throw new IllegalArgumentException("Ungültige Zeitangaben");
                }
                LocalDateTime start = LocalDateTime.of(startDate.getValue(), parsedStart);
                LocalDateTime end = LocalDateTime.of(endDate.getValue(), parsedEnd);
                CalendarEntry ce = new CalendarEntry(titleField.getText().trim(), descField.getText(), start, end);
                log("NEW_SAVE", "Neuer Eintrag (vor Persist) title='" + ce.getTitle() + "'");
                // ICS-only
                rebuildCurrentEntriesFromUI();
                currentEntries.add(ce);
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                lastSavedCount = currentEntries.size();
                setStatus("Status: Eintrag gespeichert (" + ce.getTitle() + ")");
                reloadData();
            } catch (Exception ex) {
                log("ERROR", "Speichern neuer Eintrag fehlgeschlagen: " + ex.getMessage());
                e.consume();
                showError("Speichern fehlgeschlagen", ex);
            }
        });

        dialog.showAndWait();
        log("NEW_DIALOG", "Dialog geschlossen");
    }

    private boolean validateInputs(String title, LocalDate sd, String st, LocalDate ed, String et) {
        if (title == null || title.isBlank() || sd == null || ed == null) return false;
        LocalTime ltStart = parseTime(st);
        LocalTime ltEnd = parseTime(et);
        if (ltStart == null || ltEnd == null) return false;
        LocalDateTime start = LocalDateTime.of(sd, ltStart);
        LocalDateTime end = LocalDateTime.of(ed, ltEnd);
        return end.isAfter(start);
    }

    private LocalTime parseTime(String text) {
        try {
            String t = text == null ? "" : text.trim();
            if (t.matches("^\\d{1}:\\d{2}$")) {
                t = "0" + t; // normalize 9:00 -> 09:00
            }
            return LocalTime.parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private void onImport(ActionEvent evt) {
        log("IMPORT", "Starte Import-Dialog");
        Stage stage = (Stage) calendarContainer.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(all);
        File file = chooser.showOpenDialog(stage);
        if (file == null) { log("IMPORT", "Abgebrochen"); return; }
        try {
            List<CalendarEntry> imported = IcsUtil.importAuto(file.toPath());
            log("IMPORT", "Datei='" + file.getName() + "' -> Einträge=" + imported.size());
            currentEntries.addAll(imported);
            IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            reloadData();
            setStatus("Status: Import fertig (ICS)");
        } catch (Exception ex) {
            log("ERROR", "Import fehlgeschlagen: " + ex.getMessage());
            showError("Import fehlgeschlagen", ex);
        }
    }

    private void onExport(ActionEvent evt) {
        log("EXPORT", "Starte Export-Dialog");
        Stage stage = (Stage) calendarContainer.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("calendar-export.ics");
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(ics);
        File file = chooser.showSaveDialog(stage);
        if (file == null) { log("EXPORT", "Abgebrochen"); return; }
        try {
            // Rebuild currentEntries from UI to capture any changes made via CalendarFX
            rebuildCurrentEntriesFromUI();
            List<CalendarEntry> items = new ArrayList<>(currentEntries);
            log("EXPORT", "Exportiere count=" + items.size() + " -> Datei='" + file.getName() + "'");
            java.nio.file.Path out = file.toPath();
            String lower = file.getName().toLowerCase();
            if (!lower.endsWith(".ics") && !lower.endsWith(".vcs")) {
                var sel = chooser.getSelectedExtensionFilter();
                if (sel != null && sel.getExtensions().contains("*.vcs")) {
                    out = out.resolveSibling(file.getName() + ".vcs");
                } else {
                    out = out.resolveSibling(file.getName() + ".ics");
                }
            }
            if (out.toString().toLowerCase().endsWith(".vcs")) {
                IcsUtil.exportVcs(out, items);
            } else {
                IcsUtil.exportIcs(out, items);
            }
            log("EXPORT", "Export erfolgreich -> " + out);
            setStatus("Status: Exportiert -> " + out.getFileName());
        } catch (Exception ex) {
            log("ERROR", "Export fehlgeschlagen: " + ex.getMessage());
            showError("Export fehlgeschlagen", ex);
        }
    }

    private void onSettings(ActionEvent evt) {
        log("SETTINGS", "Öffne Einstellungen");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Einstellungen");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        TextField icsPathField = new TextField(ConfigUtil.getIcsPath().toString());
        Button browse = new Button("…");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(icsPathField.getText().isBlank() ? "calendar.ics" : icsPathField.getText());
            File f = chooser.showSaveDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) icsPathField.setText(f.getAbsolutePath());
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");
        grid.add(new Label("ICS-Datei:"), 0, 0);
        grid.add(icsPathField, 1, 0);
        grid.add(browse, 2, 0);

        CheckBox darkMode = new CheckBox("Dunkelmodus");
        darkMode.setSelected(ConfigUtil.isDarkMode());
        grid.add(new Label("Darstellung:"), 0, 1);
        grid.add(darkMode, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                ConfigUtil.setIcsPath(new java.io.File(icsPathField.getText()).toPath());
                ConfigUtil.setDarkMode(darkMode.isSelected());
                applyTheme();
                ConfigUtil.save();
                reloadData();
                log("SETTINGS", "Gespeichert -> ICS=" + ConfigUtil.getIcsPath());
                setStatus("Status: Einstellungen gespeichert");
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Einstellungen gespeichert.", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
            } catch (Exception ex) {
                log("ERROR", "Einstellungen speichern fehlgeschlagen: " + ex.getMessage());
                showError("Konnte Einstellungen nicht speichern", ex);
            }
        } else {
            log("SETTINGS", "Abgebrochen");
        }
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyTheme() {
        if (calendarContainer.getScene() == null) return;
        var stylesheets = calendarContainer.getScene().getStylesheets();
        URL res = getClass().getResource("/dark.css");
        String darkCss = res != null ? res.toExternalForm() : null;
        stylesheets.removeIf(s -> s.endsWith("dark.css"));
        if (ConfigUtil.isDarkMode() && darkCss != null) {
            stylesheets.add(darkCss);
            log("THEME", "Dark mode stylesheet applied");
        } else {
            log("THEME", "Dark mode stylesheet removed");
        }
    }

    private void applyThemeToDialog(DialogPane pane) {
        if (pane == null) return;
        try {
            URL res = getClass().getResource("/dark.css");
            String darkCss = res != null ? res.toExternalForm() : null;
            pane.getStylesheets().removeIf(s -> s.endsWith("dark.css"));
            if (ConfigUtil.isDarkMode() && darkCss != null) {
                pane.getStylesheets().add(darkCss);
                log("THEME", "Dialog dark mode stylesheet applied");
            }
        } catch (Exception ignored) {}
    }

    private javafx.animation.Timeline reminderTimeline;
    private final java.util.Set<String> notified = new java.util.HashSet<>();
    private javafx.animation.Timeline autosaveTimeline; // periodischer Auto-Speicher-Monitor
    private String lastUiSignature; // UI-Snapshot zur Erkennung von Änderungen

    private void scheduleReminders(List<CalendarEntry> items) {
        if (reminderTimeline != null) reminderTimeline.stop();
        reminderTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> checkReminders(items))
        );
        reminderTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        reminderTimeline.play();
        log("REMINDER", "Reminder geplant für Einträge: " + items.size());
    }

    private void checkReminders(List<CalendarEntry> items) {
        LocalDateTime now = LocalDateTime.now();
        for (CalendarEntry entry : items) {
            if (entry.getReminderMinutesBefore() != null && entry.getReminderMinutesBefore() > 0) {
                LocalDateTime reminderTime = entry.getStart().minusMinutes(entry.getReminderMinutesBefore());
                String key = entry.getTitle() + "|" + entry.getStart();
                if (!notified.contains(key) && now.isAfter(reminderTime) && now.isBefore(entry.getStart())) {
                    notified.add(key);
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION,
                                "Termin: " + entry.getTitle() + "\nStart: " + entry.getStart(),
                                javafx.scene.control.ButtonType.OK
                        );
                        alert.setTitle("Erinnerung");
                        alert.setHeaderText("Bevorstehender Termin");
                        applyThemeToDialog(alert.getDialogPane());
                        alert.show();
                    });
                    log("REMINDER", "Erinnerung angezeigt für: " + entry.getTitle());
                }
            }
        }
    }

    // Erzeugt eine stabile Signatur des UI-Zustands, um Änderungen ohne aufwändiges Diffen zu erkennen
    private String computeUiSignature() {
        StringBuilder sb = new StringBuilder();
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (CalendarSource source : calendarView.getCalendarSources()) {
            for (Calendar<?> calendar : source.getCalendars()) {
                String calName = calendar.getName() == null ? "" : calendar.getName();
                for (Entry<?> entry : calendar.findEntries("")) {
                    String title = entry.getTitle() == null ? "" : entry.getTitle();
                    String desc = entry.getLocation() == null ? "" : entry.getLocation();
                    java.time.LocalDateTime s = entry.getStartAsLocalDateTime();
                    java.time.LocalDateTime e = entry.getEndAsLocalDateTime();
                    parts.add(calName + "|" + title + "|" + desc + "|" + s + "|" + e);
                }
            }
        }
        java.util.Collections.sort(parts);
        for (String p : parts) sb.append(p).append('\n');
        return sb.toString();
    }

    private void startAutosaveMonitor() {
        if (autosaveTimeline != null) autosaveTimeline.stop();
        lastUiSignature = computeUiSignature();
        autosaveTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400), e -> {
                    if (suppressAutoSave || !initialLoadCompleted) return;
                    String sig = computeUiSignature();
                    if (!sig.equals(lastUiSignature)) {
                        log("AUTOSAVE", "Änderung erkannt (diff)");
                        saveCurrentEntriesToIcs();
                        lastUiSignature = sig;
                    }
                })
        );
        autosaveTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        autosaveTimeline.play();
        log("AUTOSAVE", "Monitor gestartet");
    }

    private void onExit(ActionEvent evt) {
        log("EXIT", "Beenden angefordert");
        try {
            suppressAutoSave = true; // verhinder parallele Events
            if (autosaveTimeline != null) autosaveTimeline.stop();
            if (reminderTimeline != null) reminderTimeline.stop();
            // ICS speichern
            rebuildCurrentEntriesFromUI();
            log("EXIT", "Speichere vor Beenden: count=" + currentEntries.size());
            if (!currentEntries.isEmpty()) {
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            }
            ConfigUtil.save();
            javafx.application.Platform.exit();
            log("EXIT", "Anwendung beendet");
        } catch (Exception ex) {
            log("ERROR", "Fehler beim Speichern vor Exit: " + ex.getMessage());
            showError("Fehler beim Speichern", ex);
        }
    }

    private void onInfo(ActionEvent evt) {
        log("INFO", "Öffne Info-Dialog");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Über Calendar Java");
        alert.setHeaderText("Calendar Java – Kalenderanwendung");

        StringBuilder info = new StringBuilder();
        info.append("JavaFX-basierte Kalenderanwendung\n\n");
        info.append("Verwendete Bibliotheken:\n");
        info.append("- CalendarFX (Apache-2.0)\n");
        info.append("- Biweekly (BSD-2-Clause)\n");
        info.append("- JavaFX (GPLv2 + Classpath Exception)\n\n");
        info.append("Autoren:\n");
        info.append("- Jan Erdmann\n");
        info.append("- Kerim Talha Morca\n");
        info.append("- Florian Alexander Knittel\n\n");
        info.append("Version: ").append(VersionUtil.getVersion()).append("\n\n");
        info.append("Weitere Informationen:\n");
        info.append("- README.md\n");
        info.append("- THIRD-PARTY-NOTICES.md\n");
        info.append("- LICENSE\n");

        alert.setContentText(info.toString());
        applyThemeToDialog(alert.getDialogPane());

        alert.getDialogPane().setMinWidth(600);
        alert.setResizable(true);

        alert.showAndWait();
        log("INFO", "Info-Dialog geschlossen");
    }

    /**
     * Test-Hilfsmethode: Zugriff auf die CalendarView, um in UI-Tests mit dem Kalendermodell zu arbeiten.
     */
    public CalendarView getCalendarViewForTest() {
        return calendarView;
    }
}
