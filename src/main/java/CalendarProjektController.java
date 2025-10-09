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
 * JavaFX-Controller für die FXML-Ansicht. Verknüpft Buttons und Kalenderansicht
 * (CalendarFX) mit der Anwendungslogik. Persistenz erfolgt ausschließlich über ICS.
 */
public class CalendarProjektController implements Initializable {

    @FXML private AnchorPane calendarContainer;
    @FXML private Button newButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private Button settingsButton;
    @FXML private Button exitButton;
    @FXML private Button infoButton; // Info-Button in der unteren Leiste
    @FXML private Label statusLabel; // neu für Statusanzeige
    @FXML private Label saveStatusLabel; // neu für Speicher-Statusanzeige
    @FXML private Button manualSaveButton; // optionaler manueller Speichern-Button (wird dynamisch angelegt falls nicht vorhanden)

    private CalendarView calendarView;
    private final Calendar<String> fxCalendar = new Calendar<>("Allgemein");
    private final java.util.Map<String, Calendar<String>> categoryCalendars = new java.util.HashMap<>();

    private final ZoneId zone = ZoneId.systemDefault();

    // ICS-State
    private final List<CalendarEntry> currentEntries = new ArrayList<>();

    // Simple UI debug logger
    private void log(String action, String msg) {
        System.out.println("[UI_DEBUG] " + action + " | " + msg);
    }

    private boolean suppressAutoSave = true; // verhindert initiales Leerspeichern
    private boolean initialLoadCompleted = false;
    private int lastSavedCount = -1;
    private boolean calendarEventsHooked = false; // verhindert mehrfaches Registrieren

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log("INIT", "Starte Initialisierung");
        setStatus("Status: Initialisierung");

        // Locale auf Deutsch setzen für CalendarFX
        java.util.Locale.setDefault(java.util.Locale.GERMANY);

        calendarView = new CalendarView();

        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);
        calendarContainer.sceneProperty().addListener((obs, oldS, newS) -> applyTheme());
        applyTheme();

        AnchorPane.setTopAnchor(calendarView, 0.0);
        AnchorPane.setRightAnchor(calendarView, 0.0);
        AnchorPane.setBottomAnchor(calendarView, 0.0);
        AnchorPane.setLeftAnchor(calendarView, 0.0);
        calendarContainer.getChildren().add(calendarView);

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

        setupCalendarListeners();
        reloadData();
        initialLoadCompleted = true;
        suppressAutoSave = false;
        startAutosaveMonitor();
        setStatus("Status: Geladen (" + currentEntries.size() + ")");
        log("INIT", "Initialisierung abgeschlossen");

        calendarContainer.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) {
                javafx.application.Platform.runLater(this::ensureManualSaveButton);
            }
        });
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    private void setSaveStatus(String text) {
        if (saveStatusLabel != null) {
            saveStatusLabel.setText("💾 " + text);
            // Farbe auf Grün setzen bei Erfolg, Rot bei Fehler
            if (text.contains("erfolgreich") || text.equals("Gespeichert")) {
                saveStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
            } else if (text.contains("Fehler")) {
                saveStatusLabel.setStyle("-fx-text-fill: #F44336;");
            } else {
                saveStatusLabel.setStyle("-fx-text-fill: #666;");
            }
        }
    }

    private void ensureManualSaveButton() {
        try {
            if (manualSaveButton != null) return; // bereits vorhanden via FXML
            // Toolbar finden
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

    private void manualSaveAction() {
        log("MANUAL_SAVE", "Benutzer ausgelöst");
        saveCurrentEntriesToIcs();
    }

    private void reloadData() {
        suppressAutoSave = true; // während Laden nichts speichern / syncen
        log("RELOAD", "Lade Daten (ICS)");
        fxCalendar.clear();
        for (Calendar<String> cal : categoryCalendars.values()) cal.clear();
        try {
            currentEntries.clear();
            var path = ConfigUtil.getIcsPath();
            if (!Files.exists(path)) {
                java.nio.file.Path parent = path.getParent();
                if (parent == null) parent = java.nio.file.Paths.get(".");
                if (Files.exists(parent) && Files.isWritable(parent)) {
                    try { IcsUtil.exportIcs(path, new ArrayList<>()); } catch (Exception ex) { log("RELOAD", "Konnte neue ICS nicht erzeugen: " + ex.getMessage()); }
                }
            }
            if (Files.exists(path)) {
                currentEntries.addAll(IcsUtil.importIcs(path));
            }
            log("RELOAD", "ICS-Einträge geladen: " + currentEntries.size());
            populateCalendar(currentEntries);
            scheduleReminders(currentEntries);
        } catch (Exception ex) {
            log("ERROR", "Fehler beim Laden aus ICS: " + ex.getMessage());
            showError("Fehler beim Laden aus ICS", ex);
        }
        suppressAutoSave = false; // nach dem vollständigen Laden wieder erlauben
    }

    @SuppressWarnings("unchecked")
    private void populateCalendar(List<CalendarEntry> items) {
        log("POPULATE", "Übernehme Einträge in CalendarFX: count=" + items.size());
        fxCalendar.clear();
        for (Calendar<String> cal : categoryCalendars.values()) cal.clear();
        for (CalendarEntry ce : items) {
            @SuppressWarnings("unchecked") // CalendarFX addEntry uses raw Entry in some versions
            Entry<String> entry = new Entry<>(ce.getTitle());
            if (ce.getDescription() != null && !ce.getDescription().isBlank()) {
                entry.setLocation(ce.getDescription());
            }
            entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
            String cat = (ce.getCategory() == null || ce.getCategory().isBlank()) ? "Allgemein" : ce.getCategory();
            Calendar<String> target = getOrCreateCalendar(cat);
            target.addEntry(entry);
        }
        setStatus("Status: Kalender geladen (" + items.size() + ")");
    }

    /**
     * Rebuilds the currentEntries list from the CalendarFX UI state.
     * This ensures that any changes made directly in the CalendarFX UI
     * (dragging, resizing, deleting, or editing entries) are captured
     * before exporting to ICS.
     */
    private void rebuildCurrentEntriesFromUI() {
        currentEntries.clear();
        int calendarCount = 0;
        int entryCount = 0;
        for (CalendarSource source : calendarView.getCalendarSources()) {
            for (Calendar<?> calendar : source.getCalendars()) {
                calendarCount++;
                List<Entry<?>> entries = calendar.findEntries("");
                for (Entry<?> entry : entries) {
                    String title = entry.getTitle() != null ? entry.getTitle() : "(Ohne Titel)";
                    String description = entry.getLocation() != null ? entry.getLocation() : "";
                    LocalDateTime start = entry.getStartAsLocalDateTime();
                    LocalDateTime end = entry.getEndAsLocalDateTime();
                    CalendarEntry ce = new CalendarEntry(title, description, start, end);
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

    private Calendar<String> getOrCreateCalendar(String category) {
        if ("Allgemein".equalsIgnoreCase(category)) return fxCalendar;
        return categoryCalendars.computeIfAbsent(category, c -> {
            Calendar<String> cal = new Calendar<>(c);
            Calendar.Style[] styles = Calendar.Style.values();
            Calendar.Style style = styles[Math.abs(c.hashCode()) % styles.length];
            cal.setStyle(style);
            for (CalendarSource src : calendarView.getCalendarSources()) {
                if (!src.getCalendars().contains(cal)) {
                    src.getCalendars().add(cal);
                    addCalendarListener(cal); // ensure listener on new category calendar
                    log("CAL", "Neuer Kategorie-Kalender erstellt: " + c);
                    break;
                }
            }
            return cal;
        });
    }

    /**
     * Sets up listeners to detect when entries are added, modified, or deleted
     * via the CalendarFX UI. Änderungen werden automatisch in die ICS-Datei gespeichert.
     */
    private void setupCalendarListeners() {
        addCalendarListener(fxCalendar);
        for (Calendar<String> cal : categoryCalendars.values()) {
            addCalendarListener(cal);
        }
    }

    /**
     * Adds listeners to detect entry changes and auto-save:
     * - once globally on CalendarView (guarded by calendarEventsHooked)
     * - always on the given Calendar model to catch add/edit/delete reliably
     */
    private void addCalendarListener(Calendar<?> calendar) {
        // Register once on the view to catch propagated events
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
        // Hinweis: Kein Handler mehr direkt am Calendar-Model registrieren, da Calendar in dieser
        // Umgebung kein vollständig unterstütztes EventTarget ist und addEventHandler dort eine
        // UnsupportedOperationException auslösen kann. Die View-Handler decken die Änderungen ab.
    }

    /**
     * Saves the current UI entries to the ICS file.
     * This is called automatically when entries are added, modified, or deleted via CalendarFX.
     */
    private void saveCurrentEntriesToIcs() {
        if (suppressAutoSave) {
            log("SAVE_ICS_SUPPRESS", "Speichern unterdrückt (Initialisierung)");
            return;
        }
        try {
            rebuildCurrentEntriesFromUI();
            if (!initialLoadCompleted && currentEntries.isEmpty() && lastSavedCount > 0) {
                log("SAVE_ICS_SKIP", "Leere Liste erkannt vor initialLoadCompleted – Speichern übersprungen");
                return;
            }
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
            log("SAVE_ICS", "Schreibe Einträge: count=" + currentEntries.size() + " -> " + ConfigUtil.getIcsPath());
            IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            lastSavedCount = currentEntries.size();
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
        if (res != null) {
            String darkCss = res.toExternalForm();
            stylesheets.remove(darkCss);
            stylesheets.add(darkCss);
            log("THEME", "Scene Stylesheets angewendet: " + stylesheets.size());
        } else {
            log("THEME", "dark.css nicht gefunden");
        }
    }

    private void applyThemeToDialog(DialogPane pane) {
        if (pane == null) return;
        try {
            URL res = getClass().getResource("/dark.css");
            if (res != null) {
                String darkCss = res.toExternalForm();
                pane.getStylesheets().remove(darkCss);
                pane.getStylesheets().add(darkCss);
                log("THEME", "Dialog Stylesheets angewendet");
            }
        } catch (Exception ignored) {}
    }

    private javafx.animation.Timeline reminderTimeline;
    private final java.util.Set<String> notified = new java.util.HashSet<>();
    private javafx.animation.Timeline autosaveTimeline; // periodic autosave monitor
    private String lastUiSignature; // snapshot of UI to detect changes

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

    // Computes a stable signature of the UI state to detect changes without heavy diffing
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
        alert.setHeaderText("Calendar Java - Kalenderanwendung");

        StringBuilder info = new StringBuilder();
        info.append("Eine JavaFX-basierte Kalenderanwendung\n\n");
        info.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        info.append("📚 Verwendete Bibliotheken:\n\n");
        info.append("• CalendarFX\n");
        info.append("  https://github.com/dlsc-software-consulting-gmbh/CalendarFX\n");
        info.append("  Apache License 2.0\n");
        info.append("  Moderne Kalenderansicht für JavaFX\n\n");
        info.append("• Biweekly\n");
        info.append("  https://github.com/mangstadt/biweekly\n");
        info.append("  BSD 2-Clause License\n");
        info.append("  iCalendar (ICS) Parser und Generator\n\n");
        info.append("• JavaFX\n");
        info.append("  https://openjfx.io/\n");
        info.append("  GPL v2 + Classpath Exception\n");
        info.append("  UI-Framework\n\n");
        info.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        info.append("👤 Autor: fknittel\n\n");
        info.append("📅 Version: 1.0\n\n");
        info.append("Weitere Informationen finden Sie in:\n");
        info.append("• README.md\n");
        info.append("• THIRD-PARTY-NOTICES.md\n");
        info.append("• LICENSE\n");

        alert.setContentText(info.toString());
        applyThemeToDialog(alert.getDialogPane());

        // Make dialog resizable and set min width
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
