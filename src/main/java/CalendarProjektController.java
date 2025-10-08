import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.collections.FXCollections;
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
 * (CalendarFX) mit der Anwendungslogik, lädt Daten aus DB bzw. ICS und bietet
 * Aktionen zum Anlegen, Importieren und Exportieren von Terminen sowie Einstellungen.
 */
public class CalendarProjektController implements Initializable {

    @FXML private AnchorPane calendarContainer;
    @FXML private Button newButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private Button settingsButton;
    @FXML private Button subscribeButton;

    private final java.util.Map<String, Calendar> feedCalendars = new java.util.HashMap<>();
    private final java.util.Map<String, javafx.animation.Timeline> feedTimers = new java.util.HashMap<>();

    private CalendarView calendarView;
    private final Calendar fxCalendar = new Calendar("Allgemein");
    private final java.util.Map<String, Calendar> categoryCalendars = new java.util.HashMap<>();

    private final CalendarEntryDAO dao = new CalendarEntryDAO();
    private final ZoneId zone = ZoneId.systemDefault();

    // Used in ICS mode
    private final List<CalendarEntry> currentEntries = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        calendarView = new CalendarView();
        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);
        // Apply theme after scene is ready
        calendarContainer.sceneProperty().addListener((obs, oldS, newS) -> applyTheme());
        applyTheme();

        AnchorPane.setTopAnchor(calendarView, 0.0);
        AnchorPane.setRightAnchor(calendarView, 0.0);
        AnchorPane.setBottomAnchor(calendarView, 0.0);
        AnchorPane.setLeftAnchor(calendarView, 0.0);
        calendarContainer.getChildren().add(calendarView);

        // Wire actions
        newButton.setOnAction(this::onNewEntry);
        importButton.setOnAction(this::onImport);
        exportButton.setOnAction(this::onExport);
        settingsButton.setOnAction(this::onSettings);
        if (subscribeButton != null) {
            subscribeButton.setOnAction(this::onSubscribe);
        }

        // Load initial data (default ICS)
        reloadData();
        // Restore feed subscriptions
        restoreFeedsFromConfig();
    }

    private void reloadData() {
        fxCalendar.clear();
        if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
            try {
                List<CalendarEntry> items = dao.findAll();
                populateCalendar(items);
                scheduleReminders(items);
            } catch (Exception ex) {
                showError("Fehler beim Laden aus der Datenbank", ex);
            }
        } else {
            try {
                currentEntries.clear();
                var path = ConfigUtil.getIcsPath();
                if (Files.exists(path)) {
                    currentEntries.addAll(IcsUtil.importIcs(path));
                }
                populateCalendar(currentEntries);
                scheduleReminders(currentEntries);
            } catch (Exception ex) {
                showError("Fehler beim Laden aus ICS", ex);
            }
        }
    }

    private void populateCalendar(List<CalendarEntry> items) {
        // clear all calendars
        fxCalendar.clear();
        for (Calendar cal : categoryCalendars.values()) cal.clear();
        for (CalendarEntry ce : items) {
            Entry<String> entry = new Entry<>(ce.getTitle());
            if (ce.getDescription() != null && !ce.getDescription().isBlank()) {
                entry.setLocation(ce.getDescription());
            }
            entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
            if (ce.getRecurrenceRule() != null && !ce.getRecurrenceRule().isBlank()) {
                try { entry.setRecurrenceRule(ce.getRecurrenceRule()); } catch (Exception ignored) {}
            }
            String cat = (ce.getCategory() == null || ce.getCategory().isBlank()) ? "Allgemein" : ce.getCategory();
            Calendar target = getOrCreateCalendar(cat);
            target.addEntry(entry);
        }
    }

    private Calendar getOrCreateCalendar(String category) {
        if ("Allgemein".equalsIgnoreCase(category)) return fxCalendar;
        return categoryCalendars.computeIfAbsent(category, c -> {
            Calendar cal = new Calendar(c);
            // assign style based on hash to spread colors deterministically
            Calendar.Style[] styles = Calendar.Style.values();
            Calendar.Style style = styles[Math.abs(c.hashCode()) % styles.length];
            cal.setStyle(style);
            // add to view
            for (CalendarSource src : calendarView.getCalendarSources()) {
                if (!src.getCalendars().contains(cal)) {
                    src.getCalendars().add(cal);
                    break;
                }
            }
            return cal;
        });
    }

    private void onNewEntry(ActionEvent evt) {
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
        ChoiceBox<String> categoryChoice = new ChoiceBox<>(FXCollections.observableArrayList(
                "Allgemein", "Arbeit", "Privat", "Familie", "Sonstiges"
        ));
        categoryChoice.setValue("Allgemein");
        ChoiceBox<String> recurChoice = new ChoiceBox<>(FXCollections.observableArrayList(
                "Keine", "Täglich", "Wöchentlich", "Monatlich"
        ));
        recurChoice.setValue("Keine");
        TextField recurCount = new TextField();
        recurCount.setPromptText("Anzahl (optional)");
        TextField reminderMinutes = new TextField();
        reminderMinutes.setPromptText("Min. vor Beginn (optional)");

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
        grid.add(new Label("Kategorie:"), 0, 4);
        grid.add(categoryChoice, 1, 4);
        grid.add(new Label("Serie:"), 0, 5);
        grid.add(recurChoice, 1, 5);
        grid.add(recurCount, 2, 5);
        grid.add(new Label("Erinnerung:"), 0, 6);
        grid.add(reminderMinutes, 1, 6);

        dialog.getDialogPane().setContent(grid);

        var saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (!validateInputs(titleField.getText(), startDate.getValue(), startTime.getText(), endDate.getValue(), endTime.getText())) {
                e.consume();
                Alert a = new Alert(Alert.AlertType.WARNING, "Bitte Titel angeben und gültige Zeiten im Format HH:mm eingeben. Ende muss nach Start liegen.", ButtonType.OK);
                a.setHeaderText("Eingaben prüfen");
                a.showAndWait();
                return;
            }
            try {
                LocalDateTime start = LocalDateTime.of(startDate.getValue(), parseTime(startTime.getText()));
                LocalDateTime end = LocalDateTime.of(endDate.getValue(), parseTime(endTime.getText()));
                CalendarEntry ce = new CalendarEntry(titleField.getText().trim(), descField.getText(), start, end);
                // category
                ce.setCategory(categoryChoice.getValue());
                // recurrence
                String rrule = buildRRule(recurChoice.getValue(), recurCount.getText());
                ce.setRecurrenceRule(rrule);
                // reminder
                Integer rem = null;
                try { rem = (reminderMinutes.getText()==null||reminderMinutes.getText().isBlank())? null : Integer.parseInt(reminderMinutes.getText().trim()); } catch (Exception ignore) {}
                ce.setReminderMinutesBefore(rem);

                if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                    dao.save(ce);
                } else {
                    currentEntries.add(ce);
                    IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                }
                reloadData();
            } catch (Exception ex) {
                e.consume();
                showError("Speichern fehlgeschlagen", (ex instanceof Exception) ? (Exception) ex : new Exception(ex));
            }
        });

        dialog.showAndWait();
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
        Stage stage = (Stage) calendarContainer.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(all);
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            List<CalendarEntry> imported = IcsUtil.importAuto(file.toPath());
            if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                for (CalendarEntry ce : imported) {
                    dao.save(ce);
                }
            } else {
                currentEntries.addAll(imported);
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            }
            reloadData();
        } catch (Exception ex) {
            showError("Import fehlgeschlagen", ex);
        }
    }

    private void onExport(ActionEvent evt) {
        Stage stage = (Stage) calendarContainer.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("calendar-export.ics");
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(ics);
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            List<CalendarEntry> items;
            if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                items = dao.findAll();
            } else {
                items = new ArrayList<>(currentEntries);
            }
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

    private void onSettings(ActionEvent evt) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Einstellungen");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        ToggleGroup group = new ToggleGroup();
        RadioButton rbIcs = new RadioButton("Speichern als ICS");
        RadioButton rbDb = new RadioButton("Speichern in Datenbank");
        rbIcs.setToggleGroup(group);
        rbDb.setToggleGroup(group);
        rbIcs.setSelected(ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.ICS);
        rbDb.setSelected(ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB);

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
        grid.add(new Label("Speicher-Modus:"), 0, 0);
        grid.add(rbIcs, 1, 0);
        grid.add(rbDb, 2, 0);
        grid.add(new Label("ICS-Datei:"), 0, 1);
        grid.add(icsPathField, 1, 1);
        grid.add(browse, 2, 1);

        // Dark mode toggle
        CheckBox darkMode = new CheckBox("Dunkelmodus");
        darkMode.setSelected(ConfigUtil.isDarkMode());
        grid.add(new Label("Darstellung:"), 0, 2);
        grid.add(darkMode, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                ConfigUtil.setStorageMode(rbIcs.isSelected() ? ConfigUtil.StorageMode.ICS : ConfigUtil.StorageMode.DB);
                if (rbIcs.isSelected()) {
                    ConfigUtil.setIcsPath(new java.io.File(icsPathField.getText()).toPath());
                }
                // save dark mode and apply immediately
                ConfigUtil.setDarkMode(darkMode.isSelected());
                applyTheme();

                ConfigUtil.save();
                reloadData();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Einstellungen gespeichert.", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
            } catch (Exception ex) {
                showError("Konnte Einstellungen nicht speichern", ex);
            }
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
        String darkCss = getClass().getResource("/dark.css").toExternalForm();
        stylesheets.remove(darkCss);
        // Force-apply dark stylesheet to verify loading and eliminate white areas
        stylesheets.add(darkCss);
        System.out.println("[DEBUG_LOG] Applied Scene stylesheets: " + stylesheets);
    }

    private void applyThemeToDialog(DialogPane pane) {
        if (pane == null) return;
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            pane.getStylesheets().remove(darkCss);
            // Apply unconditionally to ensure dialogs are dark (even before toggle is saved)
            pane.getStylesheets().add(darkCss);
            System.out.println("[DEBUG_LOG] Dialog stylesheets: " + pane.getStylesheets());
        } catch (Exception ignored) {}
    }
    private String buildRRule(String choice, String countText) {
        if (choice == null || choice.equals("Keine")) return null;
        String freq = switch (choice) {
            case "Täglich" -> "DAILY";
            case "Wöchentlich" -> "WEEKLY";
            case "Monatlich" -> "MONTHLY";
            default -> null;
        };
        if (freq == null) return null;
        StringBuilder sb = new StringBuilder("RRULE:FREQ=").append(freq);
        try {
            if (countText != null && !countText.isBlank()) {
                int c = Integer.parseInt(countText.trim());
                if (c > 0) sb.append(";COUNT=").append(c);
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private javafx.animation.Timeline reminderTimeline;
    private final java.util.Set<String> notified = new java.util.HashSet<>();

    private void scheduleReminders(List<CalendarEntry> items) {
        if (reminderTimeline != null) {
            reminderTimeline.stop();
        }
        reminderTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> checkReminders(items))
        );
        reminderTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        reminderTimeline.play();
    }

    private void checkReminders(List<CalendarEntry> items) {
        LocalDateTime now = LocalDateTime.now();
        for (CalendarEntry ce : items) {
            Integer mins = ce.getReminderMinutesBefore();
            if (mins == null || mins <= 0) continue;
            LocalDateTime notifyAt = ce.getStart().minusMinutes(mins);
            if (!now.isBefore(notifyAt) && now.isBefore(ce.getStart())) {
                String key = (ce.getId() != null ? ("ID"+ce.getId()) : ce.getTitle()) + "@" + ce.getStart();
                if (notified.add(key)) {
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "In "+mins+" Minuten: " + ce.getTitle(), ButtonType.OK);
                    a.setHeaderText("Erinnerung");
                    applyThemeToDialog(a.getDialogPane());
                    a.show();
                }
            }
        }
    }

    // -------------------- ICS Feed subscription --------------------
    private void onSubscribe(ActionEvent evt) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("ICS-Feed abonnieren");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        TextField urlField = new TextField();
        urlField.setPromptText("https://…/calendar.ics");
        TextField refreshField = new TextField(Integer.toString(Math.max(1, ConfigUtil.getFeedRefreshMinutes())));
        refreshField.setPromptText("Aktualisierung (Minuten)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");
        grid.add(new Label("Feed-URL:"), 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(new Label("Aktualisierung (Minuten):"), 0, 1);
        grid.add(refreshField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String url = urlField.getText() == null ? "" : urlField.getText().trim();
            int minutes = 60;
            try { minutes = Integer.parseInt(refreshField.getText().trim()); } catch (Exception ignore) {}
            if (minutes <= 0) minutes = 60;
            if (!url.isEmpty()) {
                subscribeToFeed(url, minutes);
                // persist
                List<String> urls = new ArrayList<>(ConfigUtil.getFeedUrls());
                if (!urls.contains(url)) urls.add(url);
                ConfigUtil.setFeedUrls(urls);
                ConfigUtil.setFeedRefreshMinutes(minutes);
                try { ConfigUtil.save(); } catch (Exception ignored) {}
            }
        }
    }

    private void restoreFeedsFromConfig() {
        List<String> urls = ConfigUtil.getFeedUrls();
        int minutes = ConfigUtil.getFeedRefreshMinutes();
        for (String u : urls) {
            subscribeToFeed(u, minutes);
        }
    }

    private void subscribeToFeed(String url, int refreshMinutes) {
        Calendar feedCal = feedCalendars.get(url);
        if (feedCal == null) {
            feedCal = new Calendar(extractCalendarName(url));
            Calendar.Style[] styles = Calendar.Style.values();
            feedCal.setStyle(styles[Math.abs(url.hashCode()) % styles.length]);
            // add to calendar source
            CalendarSource source = calendarView.getCalendarSources().get(0);
            if (!source.getCalendars().contains(feedCal)) {
                source.getCalendars().add(feedCal);
            }
            feedCalendars.put(url, feedCal);
        }
        // immediate refresh
        refreshFeed(url);
        // schedule refresh
        javafx.animation.Timeline old = feedTimers.get(url);
        if (old != null) old.stop();
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.minutes(refreshMinutes), e -> refreshFeed(url))
        );
        tl.setCycleCount(javafx.animation.Animation.INDEFINITE);
        tl.play();
        feedTimers.put(url, tl);
    }

    private void refreshFeed(String url) {
        Calendar feedCal = feedCalendars.get(url);
        if (feedCal == null) return;
        try {
            List<CalendarEntry> items = IcsUtil.importIcsFromUrl(url);
            // clear and repopulate feed calendar only
            feedCal.clear();
            for (CalendarEntry ce : items) {
                Entry<String> entry = new Entry<>(ce.getTitle());
                if (ce.getDescription() != null && !ce.getDescription().isBlank()) {
                    entry.setLocation(ce.getDescription());
                }
                entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
                if (ce.getRecurrenceRule() != null && !ce.getRecurrenceRule().isBlank()) {
                    try { entry.setRecurrenceRule(ce.getRecurrenceRule()); } catch (Exception ignored) {}
                }
                feedCal.addEntry(entry);
            }
        } catch (Exception ex) {
            // Show one-time alert? Keep minimal: log to UI alert
            showError("Aktualisieren des Feeds fehlgeschlagen: " + url, ex);
        }
    }

    private String extractCalendarName(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath();
            String base = (host != null ? host : "Feed") + (path != null ? path.substring(path.lastIndexOf('/')+1) : "");
            if (base.isBlank()) base = "Feed";
            return "Feed: " + base;
        } catch (Exception e) {
            return "Feed";
        }
    }
}
