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
    private final Calendar fxCalendar = new Calendar("Termine");
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
        trackedEntries.add(entry); // ensure tracked
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

        // Global CalendarFX event handler (creation, edits, deletes fire CalendarEvent)
        calendarView.addEventHandler(com.calendarfx.model.CalendarEvent.ANY, evt -> {
            Entry<?> e = evt.getEntry();
            if (e != null) {
                // Remove if entry got detached (calendar null) to avoid stale entries
                if (e.getCalendar() == null) {
                    trackedEntries.remove(e);
                } else {
                    attachEntryListeners(e);
                }
            }
            scheduleDebouncedSave();
        });

        // Load entries into the CalendarFX calendar (ICS-only)
        loadEntries();
        // Attach listeners for already loaded entries
        try {
            List<Entry<?>> existing = fxCalendar.findEntries("");
            for (Entry<?> e : existing) attachEntryListeners(e);
        } catch (Exception ignored) {}
        // Create a custom toolbar
        ToolBar toolBar = new ToolBar();
        // Statuslabel initialisieren (rechts später gefüllt)
        statusLabel = new javafx.scene.control.Label("Status: Initialisierung");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");

        // Settings button
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

            // Dark mode toggle
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

        // Create new event button
        Button createBtn = new Button("Neuer Termin");
        createBtn.setOnAction(e -> showCreateDialog(primaryStage));

        // Import/Export buttons
        Button importBtn = new Button("Importieren (ICS/VCS)");
        importBtn.setOnAction(e -> doImport(primaryStage));
        Button exportBtn = new Button("Exportieren (ICS/VCS)");
        exportBtn.setOnAction(e -> doExport(primaryStage));

        // Exit and Save button
        Button exitBtn = new Button("Beenden und Speichern");
        exitBtn.setTooltip(new Tooltip("Kalender speichern und Anwendung beenden"));
        exitBtn.setOnAction(e -> {
            try {
                // Save current entries to ICS file
                // Rebuild currentEntries from UI to capture any changes made via CalendarFX
                rebuildCurrentEntriesFromUI();
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                // Save configuration
                ConfigUtil.save();
                // Close application
                Platform.exit();
            } catch (Exception ex) {
                showError("Fehler beim Speichern", ex);
            }
        });

        // Spacer to push status label and exit button to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        toolBar.getItems().addAll(settingsButton, createBtn, importBtn, exportBtn, spacer, statusLabel, new javafx.scene.control.Separator(), exitBtn);
        updateStatus("Status: Geladen", "-fx-font-size:11;-fx-text-fill:#2c7;");

        // Place toolbar above calendar view and add footer with Info button
        Button infoBtn = new Button("Info");
        infoBtn.setTooltip(new Tooltip("Informationen zu Bibliotheken und Autor"));
        infoBtn.setOnAction(e -> showInfoDialog(primaryStage));
        ToolBar footer = new ToolBar(infoBtn);

        VBox root = new VBox(toolBar, calendarView, footer);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        applyTheme(scene);
        primaryStage.setTitle("CalendarProjekt");
        primaryStage.show();

        // Localize CalendarFX built-in controls (e.g., Today/Day/Week/Month/Year, Search)
        Platform.runLater(() -> {
            localizeNode(root);
            // Periodically look for a sidebar mini calendar (month-view or date-picker) after user expands it
            javafx.animation.Timeline poll = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), ev -> tryDumpMiniCalendar(root))
            );
            poll.setCycleCount(5);
            poll.play();

            // Also scan headers to learn the exact label classes used for the large date titles
            javafx.animation.Timeline headerScan = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), ev -> scanHeaderLabels(root))
            );
            headerScan.setCycleCount(3);
            headerScan.play();
        });
        // Attach listeners for automatic persistence of UI-created / modified entries.
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

    // ...existing code...
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
            // Remove entries that are no longer attached to any calendar
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
                // Location (hier Beschreibung) mit in Snapshot aufnehmen damit reine Text-Änderungen erkannt werden
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

    @SuppressWarnings("unchecked") // CalendarFX addEntry accepts raw Entry type
    private void loadEntries() {
        // Clear UI + tracking first
        fxCalendar.clear();
        trackedEntries.clear();
        try {
            currentEntries.clear();
            var path = ConfigUtil.getIcsPath();
            // Auto-create ICS file if it doesn't exist
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
            // Rebuild currentEntries from UI to capture any changes made via CalendarFX
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
        // Apply dark stylesheet to dialog
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showInfoDialog(Stage owner) {
        StringBuilder sb = new StringBuilder();
        sb.append("CalendarProjekt\n\n");
        sb.append("Verwendete Bibliotheken:\n");
        sb.append("- CalendarFX 12.0.1\n");
        sb.append("- JavaFX 22.0.1\n");
        sb.append("- ical4j 3.2.7 (mit Kompatibilitäts-Shim für Frequency)\n\n");
        sb.append("Autor: Florian Knittel\n");

        Alert alert = new Alert(AlertType.INFORMATION);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Info");
        alert.setHeaderText("Über dieses Projekt");
        alert.setContentText(sb.toString());
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    // Debug / Lokalisierung Hilfen (leicht gekürzt, funktional identisch)
    private javafx.scene.control.Labeled findLabeledByText(Parent root, String text) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof javafx.scene.control.Labeled l) {
                if (text.equals(l.getText())) return l;
            }
            if (child instanceof Parent p) {
                var res = findLabeledByText(p, text);
                if (res != null) return res;
            }
        }
        return null;
    }
    private Parent findSidebarByTitle(Parent root, String title) {
        javafx.scene.control.Labeled labeled = findLabeledByText(root, title);
        if (labeled == null) return null;
        Node p = labeled;
        for (int i = 0; i < 6 && p != null; i++) p = p.getParent();
        if (p instanceof Parent) return (Parent) p;
        return labeled.getParent() instanceof Parent ? (Parent) labeled.getParent() : null;
    }
    private void dumpNodeTree(Node node, int depth, int[] counter) {
        if (node == null) return;
        if (counter[0] > 400) return;
        String indent = " ".repeat(Math.min(depth, 30));
        String styleClasses = (node.getStyleClass() == null) ? "" : node.getStyleClass().toString();
        String id = node.getId();
        System.out.println("[DEBUG_LOG] " + indent + node.getClass().getName() + (id != null ? ("#"+id) : "") + " " + styleClasses);
        counter[0]++;
        if (node instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                dumpNodeTree(c, depth + 1, counter);
                if (counter[0] > 400) break;
            }
        }
    }
    private boolean miniCalendarDumped = false;
    private void tryDumpMiniCalendar(Parent root) {
        if (miniCalendarDumped) return;
        Node n = findFirstByStyleClass(root, "month-view");
        if (n == null) n = findFirstByStyleClass(root, "date-picker");
        if (n == null) n = findFirstByStyleClass(root, "tray");
        if (n == null) n = findFirstByStyleClass(root, "drawer");
        Parent toDumpParent = null;
        if (n != null) {
            Node base = n;
            for (int i = 0; i < 4 && base != null; i++) base = base.getParent();
            toDumpParent = (base instanceof Parent) ? (Parent) base : null;
        }
        if (toDumpParent == null) {
            toDumpParent = findSidebarByTitle(root, "Kalender");
        }
        if (toDumpParent != null) {
            dumpNodeTree(toDumpParent, 0, new int[]{0});
            miniCalendarDumped = true;
        }
    }
    private Node findFirstByStyleClass(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child.getStyleClass() != null && child.getStyleClass().contains(styleClass)) return child;
            if (child instanceof Parent p) {
                Node res = findFirstByStyleClass(p, styleClass);
                if (res != null) return res;
            }
        }
        return null;
    }
    private void scanHeaderLabels(Parent root) {}
    private void scanHeaderRecursive(Node node, int[] count) {}
    private boolean belongsToPage(Node node) { return false; }
    private String pageType(Node node) { return null; }

    private void localizeNode(Node node) {
        if (node == null) return;
        if (node instanceof ButtonBase b) {
            String t = b.getText();
            if (t != null) {
                switch (t) {
                    case "Today": b.setText("Heute"); break;
                    case "Day": b.setText("Tag"); break;
                    case "Week": b.setText("Woche"); break;
                    case "Month": b.setText("Monat"); break;
                    case "Year": b.setText("Jahr"); break;
                    case "Print": b.setText("Drucken"); break;
                    case "Add Calendar": b.setText("Kalender hinzufügen"); break;
                }
            }
        }
        if (node instanceof TextField tf) {
            String p = tf.getPromptText();
            if (p != null && p.equals("Search")) {
                tf.setPromptText("Suche");
            }
        }
        if (node instanceof javafx.scene.control.Labeled l) {
            String t2 = l.getText();
            if (t2 != null) {
                if (t2.equals("Calendars")) l.setText("Kalender");
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                localizeNode(child);
            }
        }
    }

    private void showCreateDialog(Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Neuer Termin");
        ButtonType saveType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setStyle("-fx-padding: 10;");

        TextField titleField = new TextField(); titleField.setPromptText("Titel");
        TextField descField = new TextField(); descField.setPromptText("Beschreibung (optional)");
        DatePicker startDate = new DatePicker(LocalDate.now());
        TextField startTime = new TextField("09:00");
        DatePicker endDate = new DatePicker(LocalDate.now());
        TextField endTime = new TextField("10:00");

        grid.add(new Label("Titel:"), 0, 0); grid.add(titleField, 1, 0);
        grid.add(new Label("Beschreibung:"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("Start (Datum / Zeit):"), 0, 2); grid.add(startDate, 1, 2); grid.add(startTime, 2, 2);
        grid.add(new Label("Ende (Datum / Zeit):"), 0, 3); grid.add(endDate, 1, 3); grid.add(endTime, 2, 3);

        dialog.getDialogPane().setContent(grid);

        var saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            if (!validateInputs(titleField.getText(), startDate.getValue(), startTime.getText(), endDate.getValue(), endTime.getText())) {
                evt.consume();
                Alert a = new Alert(AlertType.WARNING, "Bitte Titel angeben und gültige Zeiten im Format HH:mm eingeben. Ende muss nach Start liegen.", ButtonType.OK);
                a.initOwner(owner);
                a.setHeaderText("Eingaben prüfen");
                applyThemeToDialog(a.getDialogPane());
                a.showAndWait();
                return;
            }
            try {
                LocalDateTime start = LocalDateTime.of(startDate.getValue(), parseTime(startTime.getText()));
                LocalDateTime end = LocalDateTime.of(endDate.getValue(), parseTime(endTime.getText()));
                CalendarEntry ce = new CalendarEntry(titleField.getText().trim(), descField.getText(), start, end);
                currentEntries.add(ce);
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                loadEntries();
            } catch (Exception ex) {
                evt.consume();
                showError("Speichern fehlgeschlagen", ex);
            }
        });
        dialog.showAndWait();
    }

    private boolean validateInputs(String title, LocalDate sd, String st, LocalDate ed, String et) {
        if (title == null || title.isBlank() || sd == null || ed == null) return false;
        LocalTime ltStart = parseTime(st); LocalTime ltEnd = parseTime(et);
        if (ltStart == null || ltEnd == null) return false;
        LocalDateTime start = LocalDateTime.of(sd, ltStart); LocalDateTime end = LocalDateTime.of(ed, ltEnd);
        return end.isAfter(start);
    }
    private LocalTime parseTime(String text) {
        try {
            String t = text == null ? "" : text.trim();
            if (t.matches("^\\d{1}:\\d{2}$")) t = "0" + t;
            return LocalTime.parse(t);
        } catch (Exception e) { return null; }
    }

    private void applyThemeToDialog(javafx.scene.control.DialogPane pane) {
        if (pane == null) return;
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            var sheets = pane.getStylesheets(); sheets.remove(darkCss);
            if (ConfigUtil.isDarkMode()) sheets.add(darkCss);
        } catch (Exception ignored) {}
    }
    private void applyTheme(Scene scene) {
        if (scene == null) return;
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            var stylesheets = scene.getStylesheets(); stylesheets.remove(darkCss);
            if (ConfigUtil.isDarkMode()) stylesheets.add(darkCss);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
