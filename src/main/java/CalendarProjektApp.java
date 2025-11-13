import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.calendarfx.view.CalendarView;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.Entry;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.nio.file.Files;

/**
 * Hauptanwendung des Projekts (ohne FXML). Persistenz erfolgt ausschließlich per ICS.
 */
public class CalendarProjektApp extends Application {
    private final Calendar<String> fxCalendar = new Calendar<>("Termine");
    private final java.util.List<CalendarEntry> currentEntries = new ArrayList<>();
    private String lastEntriesSnapshot = "";
    private javafx.animation.Timeline autoSaveTimeline;
    private javafx.animation.PauseTransition debounceSave;
    private javafx.animation.Timeline periodicFullSave; // Fallback-Voll-Speicher
    private final java.util.Set<Entry<?>> trackedEntries = new java.util.HashSet<>();
    // Neues UI-Statuslabel
    private javafx.scene.control.Label statusLabel;
    // Diagnose-Logging Schalter
    private static final boolean DIAG_VERBOSE = false; // falls true mehr Details
    private static final boolean DIAG_SNAPSHOT = true; // Snapshot/Save Logs aktiv
    private volatile boolean dirty = false; // markiert ausstehende Änderungen unabhängig vom Snapshot

    private void logDiag(String msg) {
        if (DIAG_SNAPSHOT) {
            System.out.println("[DIAG] " + msg);
        }
    }
    private void logVerbose(String msg) {
        if (DIAG_VERBOSE) {
            System.out.println("[VERBOSE] " + msg);
        }
    }
    private void updateStatus(String text, String style) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setStyle(style);
        }
    }

    private void scheduleDebouncedSave() {
        dirty = true;
        updateStatus("Status: Änderungen – Speichern...", "-fx-font-size:11;-fx-text-fill:#d90;");
        if (debounceSave == null) {
            debounceSave = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            debounceSave.setOnFinished(e -> autoSaveIfChanged());
        }
        debounceSave.stop();
        debounceSave.playFromStart();
    }

    private void attachEntryListeners(Entry<?> entry) {
        if (entry == null) return;
        trackedEntries.add(entry); // sicherstellen, dass getrackt wird
        String[] propMethods = {"titleProperty", "locationProperty", "startDateProperty", "startTimeProperty", "endDateProperty", "endTimeProperty"};
        for (String pm : propMethods) {
            try {
                var m = entry.getClass().getMethod(pm);
                Object prop = m.invoke(entry);
                if (prop instanceof javafx.beans.value.ObservableValue<?> ov) {
                    ov.addListener((obs, o, n) -> scheduleDebouncedSave());
                } else if (prop instanceof javafx.beans.Observable o) {
                    o.addListener(ob -> scheduleDebouncedSave());
                }
            } catch (NoSuchMethodException ignore) {
            } catch (Exception ex) {
                System.out.println("[DEBUG_LOG] Listener-Reflektion fehlgeschlagen für " + pm + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Locale auf Deutsch setzen, bevor UI erzeugt wird
        java.util.Locale.setDefault(java.util.Locale.GERMANY);
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, java.util.Locale.GERMANY);

        CalendarView calendarView = new CalendarView();

        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);

        // Globaler CalendarFX-Event-Handler (Erstellen, Bearbeiten, Löschen löst CalendarEvent aus)
        calendarView.addEventHandler(com.calendarfx.model.CalendarEvent.ANY, evt -> {
            Entry<?> e = evt.getEntry();
            if (e != null) {
                // Entfernen, wenn Eintrag getrennt wurde (Kalender null), um veraltete Einträge zu vermeiden
                if (e.getCalendar() == null) {
                    trackedEntries.remove(e);
                } else {
                    attachEntryListeners(e);
                }
            }
            scheduleDebouncedSave();
        });

        // Einträge in den CalendarFX-Kalender laden (nur ICS)
        loadEntries();
        // Listener für bereits geladene Einträge anheften
        try {
            List<Entry<?>> existing = fxCalendar.findEntries("");
            for (Entry<?> e : existing) attachEntryListeners(e);
        } catch (Exception ignored) {}
        // Eigene Werkzeugleiste erstellen
        ToolBar toolBar = new ToolBar();
        // Statuslabel initialisieren (rechts später gefüllt)
        statusLabel = new javafx.scene.control.Label("Status: Initialisierung");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");

        // Einstellungen-Button
        Button settingsButton = new Button();
        settingsButton.setTooltip(new Tooltip("Einstellungen"));

        var iconUrl = getClass().getResource("/icons/settings.png");
        if (iconUrl != null) {
            ImageView iv = new ImageView(new Image(iconUrl.toExternalForm()));
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            settingsButton.setGraphic(iv);
        } else {
            settingsButton.setText("⚙");
        }

        settingsButton.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Einstellungen");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            applyThemeToDialog(dialog.getDialogPane());

            TextField icsPathField = new TextField(ConfigUtil.getIcsPath().toString());
            Button browse = new Button("…");
            browse.setOnAction(ev -> {
                FileChooser chooser2 = new FileChooser();
                chooser2.setInitialFileName(icsPathField.getText().isBlank() ? "calendar.ics" : icsPathField.getText());
                File f = chooser2.showSaveDialog(((Button) ev.getSource()).getScene().getWindow());
                if (f != null) icsPathField.setText(f.getAbsolutePath());
            });

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setStyle("-fx-padding: 10;");
            grid.add(new Label("ICS-Datei:"), 0, 0);
            grid.add(icsPathField, 1, 0);
            grid.add(browse, 2, 0);

            // Dark-Mode-Umschalter
            Label displayLbl = new Label("Darstellung:");
            CheckBox darkMode = new CheckBox("Dunkelmodus");
            darkMode.setSelected(ConfigUtil.isDarkMode());
            grid.add(displayLbl, 0, 1);
            grid.add(darkMode, 1, 1);

            dialog.getDialogPane().setContent(grid);

            var res = dialog.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                try {
                    ConfigUtil.setIcsPath(new java.io.File(icsPathField.getText()).toPath());
                    ConfigUtil.setDarkMode(darkMode.isSelected());
                    applyTheme(primaryStage.getScene());

                    ConfigUtil.save();
                    loadEntries();
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Einstellungen gespeichert.", ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                } catch (Exception ex) {
                    showError("Konnte Einstellungen nicht speichern", ex);
                }
            }
        });

        // Button: Neuer Termin
        Button createBtn = new Button("Neuer Termin");
        createBtn.setOnAction(e -> showCreateDialog(primaryStage));

        // Import/Export-Buttons
        Button importBtn = new Button("Importieren (ICS/VCS)");
        importBtn.setOnAction(e -> doImport(primaryStage));
        Button exportBtn = new Button("Exportieren (ICS/VCS)");
        exportBtn.setOnAction(e -> doExport(primaryStage));

        // Beenden- und Speichern-Button
        Button exitBtn = new Button("Beenden und Speichern");
        exitBtn.setTooltip(new Tooltip("Kalender speichern und Anwendung beenden"));
        exitBtn.setOnAction(e -> {
            try {
                // Aktuelle Einträge in ICS-Datei speichern
                // currentEntries aus UI neu aufbauen, um Änderungen aus CalendarFX zu übernehmen
                rebuildCurrentEntriesFromUI();
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                // Konfiguration speichern
                ConfigUtil.save();
                // Anwendung schließen
                Platform.exit();
            } catch (Exception ex) {
                showError("Fehler beim Speichern", ex);
            }
        });

        // Abstandhalter, um Statuslabel und Beenden-Button nach rechts zu schieben
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        toolBar.getItems().addAll(settingsButton, createBtn, importBtn, exportBtn, spacer, statusLabel, new javafx.scene.control.Separator(), exitBtn);
        updateStatus("Status: Geladen", "-fx-font-size:11;-fx-text-fill:#2c7;");

        // Toolbar über der Kalenderansicht platzieren und Fußzeile mit Info-Button hinzufügen
        Button infoBtn = new Button("Info");
        infoBtn.setTooltip(new Tooltip("Informationen zu Bibliotheken und Autoren"));
        infoBtn.setOnAction(e -> showInfoDialog(primaryStage));
        ToolBar footer = new ToolBar(infoBtn);

        VBox root = new VBox(toolBar, calendarView, footer);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        applyTheme(scene);
        primaryStage.setTitle("CalendarProjekt");
        primaryStage.show();

        // CalendarFX-eigene Bedienelemente lokalisieren (z. B. Today/Day/Week/Month/Year, Search)
        Platform.runLater(() -> {
            localizeNode(root);
            // In regelmäßigen Abständen nach einem Mini-Kalender (Monatsansicht/DatePicker) suchen, falls der Nutzer ihn öffnet
            javafx.animation.Timeline poll = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), ev -> tryDumpMiniCalendar(root))
            );
            poll.setCycleCount(5);
            poll.play();

            // Auch Überschriften scannen, um die Klassen der großen Datumstitel zu ermitteln
            javafx.animation.Timeline headerScan = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), ev -> scanHeaderLabels(root))
            );
            headerScan.setCycleCount(3);
            headerScan.play();
        });
        // Listener für automatische Persistenz von im UI erstellten/geänderten Einträgen anheften.
        attachAutoPersistence();
        startPeriodicFullSave();

        // Speichern auch bei Fensterschließen (X)
        primaryStage.setOnCloseRequest(evt -> {
            try {
                rebuildCurrentEntriesFromUI();
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                System.out.println("[DEBUG_LOG] Vollspeicher beim Fenster-Schließen ausgeführt.");
                ConfigUtil.save();
            } catch (Exception ex) {
                System.out.println("[DEBUG_LOG] Fehler beim Speichern beim Close-Request: " + ex.getMessage());
            }
        });
    }

    // --- Hilfsfunktionen, die für die UI-Lokalisierung/Diagnose verwendet werden (minimal implementiert) ---
    private void localizeNode(javafx.scene.Parent root) {
        // Platzhalter: Hier könnte man Buttons/Labels eindeutschen.
        // Aktuell übernimmt CalendarFX vieles automatisch über das Locale.
    }
    private void tryDumpMiniCalendar(javafx.scene.Parent root) {
        // Platzhalter: Könnte verwendet werden, um einen eingeblendeten Mini-Monatskalender zu untersuchen.
    }
    private void scanHeaderLabels(javafx.scene.Parent root) {
        // Platzhalter: Könnte verwendet werden, um Überschriften/Labels für weitere Lokalisierungen zu scannen.
    }

    // Dialog zum Erstellen eines neuen Termins (einfache Variante)
    private void showCreateDialog(Stage owner) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Neuer Termin");
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");

        var title = new javafx.scene.control.TextField();
        title.setPromptText("Titel");
        var desc = new javafx.scene.control.TextField();
        desc.setPromptText("Beschreibung (optional)");
        var startDate = new javafx.scene.control.DatePicker(java.time.LocalDate.now());
        var startTime = new javafx.scene.control.TextField("09:00");
        var endDate = new javafx.scene.control.DatePicker(java.time.LocalDate.now());
        var endTime = new javafx.scene.control.TextField("10:00");

        grid.add(new javafx.scene.control.Label("Titel:"), 0, 0);
        grid.add(title, 1, 0);
        grid.add(new javafx.scene.control.Label("Beschreibung:"), 0, 1);
        grid.add(desc, 1, 1);
        grid.add(new javafx.scene.control.Label("Start (Datum/Zeit):"), 0, 2);
        grid.add(startDate, 1, 2);
        grid.add(startTime, 2, 2);
        grid.add(new javafx.scene.control.Label("Ende (Datum/Zeit):"), 0, 3);
        grid.add(endDate, 1, 3);
        grid.add(endTime, 2, 3);

        dialog.getDialogPane().setContent(grid);
        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                java.time.LocalTime st = java.time.LocalTime.parse(startTime.getText().trim());
                java.time.LocalTime et = java.time.LocalTime.parse(endTime.getText().trim());
                java.time.LocalDateTime sdt = java.time.LocalDateTime.of(startDate.getValue(), st);
                java.time.LocalDateTime edt = java.time.LocalDateTime.of(endDate.getValue(), et);
                if (!edt.isAfter(sdt)) throw new IllegalArgumentException("Ende muss nach Start liegen");

                String t = (title.getText() == null || title.getText().isBlank()) ? "(Ohne Titel)" : title.getText().trim();
                String d = desc.getText() == null ? "" : desc.getText();

                // In UI hinzufügen
                var entry = new com.calendarfx.model.Entry<String>(t);
                entry.setLocation(d);
                entry.setInterval(sdt.atZone(java.time.ZoneId.systemDefault()), edt.atZone(java.time.ZoneId.systemDefault()));
                fxCalendar.addEntry(entry);
                trackedEntries.add(entry);

                // Persistieren
                rebuildCurrentEntriesFromUI();
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                updateStatus("Status: Gespeichert (" + currentEntries.size() + ")", "-fx-font-size:11;-fx-text-fill:#2c7;");
            } catch (Exception ex) {
                showError("Speichern fehlgeschlagen", ex);
            }
        }
    }
    private void startPeriodicFullSave() {
        if (periodicFullSave != null) return;
        periodicFullSave = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(15), e -> {
                    try {
                        rebuildCurrentEntriesFromUI();
                        IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                        lastEntriesSnapshot = computeSnapshot(); // Snapshot angleichen
                        System.out.println("[DEBUG_LOG] Periodischer Vollspeicher (Interval 15s) ausgeführt, Einträge=" + currentEntries.size());
                    } catch (Exception ex) {
                        System.out.println("[DEBUG_LOG] Periodischer Vollspeicher fehlgeschlagen: " + ex.getMessage());
                    }
                })
        );
        periodicFullSave.setCycleCount(javafx.animation.Animation.INDEFINITE);
        periodicFullSave.play();
    }

    private void attachAutoPersistence() {
        try {
            autoSaveTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> autoSaveIfChanged())
            );
            autoSaveTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            autoSaveTimeline.play();
            updateSnapshot();
        } catch (Exception ex) {
            System.out.println("[DEBUG_LOG] Auto-Persistenz Init Fehler: " + ex.getMessage());
        }
    }

    private void autoSaveIfChanged() {
        String snap = computeSnapshot();
        boolean snapshotChanged = !snap.equals(lastEntriesSnapshot);
        if (snapshotChanged || dirty) {
            logDiag("Änderungen erkannt. snapshotChanged=" + snapshotChanged + " dirty=" + dirty + " alterSnapshotLen=" + lastEntriesSnapshot.length() + " neuerSnapshotLen=" + snap.length());
            try {
                rebuildCurrentEntriesFromUI();
                logDiag(buildEntryDiagnostic("Vor Export", currentEntries));
                long beforeSize = getFileSizeSafe(ConfigUtil.getIcsPath());
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                long afterSize = getFileSizeSafe(ConfigUtil.getIcsPath());
                int veventCount = countVevents(ConfigUtil.getIcsPath());
                lastEntriesSnapshot = snap;
                dirty = false;
                updateStatus("Status: Gespeichert (" + currentEntries.size() + ", VEVENT=" + veventCount + ")", "-fx-font-size:11;-fx-text-fill:#2c7;");
                logDiag("Auto-Save OK entries=" + currentEntries.size() + " fileBefore=" + beforeSize + " fileAfter=" + afterSize + " vevents=" + veventCount);
            } catch (Exception ex) {
                updateStatus("Status: Fehler beim Speichern", "-fx-font-size:11;-fx-text-fill:#c33;font-weight:bold;");
                logDiag("Fehler beim Auto-Speichern: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            logVerbose("Keine Änderungen erkannt (Snapshot gleich, dirty=false)");
            updateStatus("Status: Keine Änderungen", "-fx-font-size:11;-fx-text-fill:#888;");
        }
    }

    private void updateSnapshot() { lastEntriesSnapshot = computeSnapshot(); }

    private void refreshTrackingFromCalendar() {
        try {
            List<Entry<?>> all = fxCalendar.findEntries("");
            for (Entry<?> e : all) {
                if (e != null && e.getCalendar() != null) trackedEntries.add(e);
            }
            // Einträge entfernen, die nicht mehr an einen Kalender angehängt sind
            trackedEntries.removeIf(e -> e == null || e.getCalendar() == null);
        } catch (Exception ex) {
            System.out.println("[DEBUG_LOG] refreshTrackingFromCalendar Fehler: " + ex.getMessage());
        }
    }

    private String computeSnapshot() {
        try {
            refreshTrackingFromCalendar();
            java.util.List<Entry<?>> alive = new java.util.ArrayList<>();
            for (Entry<?> e : trackedEntries) {
                if (e != null && e.getCalendar() != null) alive.add(e);
            }
            alive.sort((a, b) -> {
                int cmp = a.getStartAsLocalDateTime().compareTo(b.getStartAsLocalDateTime());
                if (cmp != 0) return cmp;
                return String.valueOf(a.getTitle()).compareTo(String.valueOf(b.getTitle()));
            });
            StringBuilder sb = new StringBuilder();
            int i=0;
            for (Entry<?> e : alive) {
                // Location (hier Beschreibung) mit in Snapshot aufnehmen, damit reine Text-Änderungen erkannt werden
                sb.append('|').append(e.getTitle()).append('|')
                  .append(e.getLocation()).append('|')
                  .append(e.getStartAsLocalDateTime()).append('|')
                  .append(e.getEndAsLocalDateTime());
                if (i++ < 5 && DIAG_VERBOSE) {
                    logVerbose("SnapshotEntry: " + e.getTitle() + " loc=" + e.getLocation());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            logDiag("Snapshot Fehler: " + e.getMessage());
            return "";
        }
    }

    private String buildEntryDiagnostic(String prefix, java.util.List<CalendarEntry> list) {
        StringBuilder sb = new StringBuilder(prefix).append(" | count=").append(list.size());
        int i = 0;
        for (CalendarEntry ce : list) {
            if (i++ > 15) { sb.append(" ..."); break; }
            sb.append("\n  - ").append(ce.getTitle())
              .append(" [").append(ce.getStart()).append(" -> ").append(ce.getEnd()).append("]");
        }
        return sb.toString();
    }
    private long getFileSizeSafe(java.nio.file.Path p) {
        try { return java.nio.file.Files.exists(p) ? java.nio.file.Files.size(p) : -1; } catch (Exception e) { return -2; }
    }
    private int countVevents(java.nio.file.Path p) {
        try {
            if (!java.nio.file.Files.exists(p)) return 0;
            int c = 0;
            for (String line : java.nio.file.Files.readAllLines(p)) {
                if (line.startsWith("BEGIN:VEVENT")) c++;
            }
            return c;
        } catch (Exception e) { return -1; }
    }

    @SuppressWarnings("unchecked") // CalendarFX addEntry akzeptiert rohen Entry-Typ
    private void loadEntries() {
        // Zunächst UI und Tracking leeren
        fxCalendar.clear();
        trackedEntries.clear();
        try {
            currentEntries.clear();
            var path = ConfigUtil.getIcsPath();
            // ICS-Datei automatisch erstellen, falls sie nicht existiert
            if (!java.nio.file.Files.exists(path)) {
                java.nio.file.Path parent = path.getParent();
                if (parent == null) parent = java.nio.file.Paths.get(".");
                if (java.nio.file.Files.exists(parent) && java.nio.file.Files.isWritable(parent)) {
                    try { IcsUtil.exportIcs(path, new java.util.ArrayList<>()); } catch (Exception ignore) {}
                }
            }
            if (java.nio.file.Files.exists(path)) {
                currentEntries.addAll(IcsUtil.importIcs(path));
            }
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            for (CalendarEntry ce : currentEntries) {
                Entry<String> entry = new Entry<>(ce.getTitle());
                entry.setLocation(ce.getDescription());
                entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
                fxCalendar.addEntry(entry);
                trackedEntries.add(entry);
            }
        } catch (Exception ex) {
            showError("Fehler beim Laden aus ICS", ex);
        }
    }

    /**
     * Baut currentEntries aus dem aktuellen UI-Zustand neu auf (vor dem Speichern nach ICS verwenden).
     */
    @SuppressWarnings("unchecked")
    private void rebuildCurrentEntriesFromUI() {
        currentEntries.clear();
        try {
            refreshTrackingFromCalendar();
            int skipped = 0;
            for (Entry<?> entry : trackedEntries) {
                if (entry == null || entry.getCalendar() == null) { skipped++; continue; }
                String title = entry.getTitle() != null ? entry.getTitle() : "(Ohne Titel)";
                String description = entry.getLocation() != null ? entry.getLocation() : "";
                java.time.LocalDateTime start = entry.getStartAsLocalDateTime();
                java.time.LocalDateTime end = entry.getEndAsLocalDateTime();
                currentEntries.add(new CalendarEntry(title, description, start, end));
            }
            logDiag("Rebuild UI -> currentEntries=" + currentEntries.size() + " skippedRemoved=" + skipped);
        } catch (Exception ex) {
            logDiag("Konnte UI-Einträge nicht lesen: " + ex.getMessage());
        }
    }

    private void doImport(Stage owner) {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(all);
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;
        try {
            List<CalendarEntry> imported = IcsUtil.importAuto(file.toPath());
            currentEntries.addAll(imported);
            IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            loadEntries();
        } catch (Exception ex) {
            showError("Import fehlgeschlagen", ex);
        }
    }

    private void doExport(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("calendar-export.ics");
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(ics);
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;
        try {
            // currentEntries aus UI neu aufbauen, um Änderungen aus CalendarFX zu übernehmen
            rebuildCurrentEntriesFromUI();
            List<CalendarEntry> items = new ArrayList<>(currentEntries);
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
        } catch (Exception ex) {
            showError("Export fehlgeschlagen", ex);
        }
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        // Dunkles Stylesheet auf Dialog anwenden
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showInfoDialog(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Über Calendar Java");
        alert.setHeaderText("Calendar Java – Kalenderanwendung");

        StringBuilder sb = new StringBuilder();
        sb.append("JavaFX-basierte Kalenderanwendung\n\n");
        sb.append("Verwendete Bibliotheken:\n");
        sb.append("- CalendarFX (Apache-2.0)\n");
        sb.append("- Biweekly (BSD-2-Clause)\n");
        sb.append("- JavaFX (GPLv2 + Classpath Exception)\n\n");
        sb.append("Autoren:\n");
        sb.append("- Jan Erdmann\n");
        sb.append("- Kerim Talha Morca\n");
        sb.append("- Florian Alexander Knittel\n\n");
        sb.append("Version: ").append(VersionUtil.getVersion()).append("\n\n");
        sb.append("Weitere Informationen:\n");
        sb.append("- README.md\n");
        sb.append("- THIRD-PARTY-NOTICES.md\n");
        sb.append("- LICENSE\n");

        alert.setContentText(sb.toString());
        applyThemeToDialog(alert.getDialogPane());
        alert.getDialogPane().setMinWidth(600);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void applyThemeToDialog(DialogPane pane) {
        if (pane == null) return;
        try {
            URL res = getClass().getResource("/dark.css");
            String darkCss = res != null ? res.toExternalForm() : null;
            pane.getStylesheets().removeIf(s -> s.endsWith("dark.css"));
            if (ConfigUtil.isDarkMode() && darkCss != null) {
                pane.getStylesheets().add(darkCss);
            }
        } catch (Exception ignored) {}
    }
    private void applyTheme(Scene scene) {
        if (scene == null) return;
        try {
            URL res = getClass().getResource("/dark.css");
            String darkCss = res != null ? res.toExternalForm() : null;
            scene.getStylesheets().removeIf(s -> s.endsWith("dark.css"));
            if (ConfigUtil.isDarkMode() && darkCss != null) {
                scene.getStylesheets().add(darkCss);
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
