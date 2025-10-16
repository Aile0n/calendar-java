import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * FXML-basierte Startklasse für die Kalenderanwendung.
 *
 * Diese Klasse lädt die Benutzeroberfläche aus einer XML-Datei (calendar_view.fxml)
 * anstatt sie im Code zu programmieren. Das macht die UI-Gestaltung übersichtlicher
 * und trennt das Design (FXML) von der Logik (Controller).
 *
 * FXML ist wie HTML für JavaFX - es beschreibt, wo welche Buttons und Felder sind.
 */
public class CalendarFxmlApp extends Application {

    /**
     * Die start-Methode wird automatisch von JavaFX aufgerufen, wenn die Anwendung startet.
     * Hier bauen wir das Hauptfenster auf und zeigen es an.
     *
     * @param stage Das Hauptfenster (wie eine Bühne/Stage im Theater)
     * @throws Exception Falls beim Laden der FXML-Datei etwas schiefgeht
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Debug-Ausgabe: Zeigt an, dass wir jetzt die UI laden
        System.out.println("[UI_DEBUG] FXML_APP_START | Lade calendar_view.fxml");

        // FXMLLoader lädt die UI-Beschreibung aus der calendar_view.fxml Datei
        // getResource sucht die Datei im resources-Ordner des Projekts
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/calendar_view.fxml"));

        // load() liest die FXML-Datei und erstellt daraus die UI-Elemente
        // root enthält dann alle Buttons, Textfelder, etc. aus der FXML
        Parent root = loader.load();

        // Eine Scene ist wie die "Szene" auf der Bühne - sie enthält alle UI-Elemente
        // 1000 x 700 ist die Fenstergröße in Pixeln (Breite x Höhe)
        Scene scene = new Scene(root, 1000, 700);

        // Setze den Fenstertitel (wird oben in der Titelleiste angezeigt)
        stage.setTitle("CalendarProjekt (FXML)");

        // Verbinde das Fenster (Stage) mit der Szene
        stage.setScene(scene);

        // Zeige das Fenster an (ohne show() bleibt es unsichtbar)
        stage.show();

        // Debug-Ausgabe: Zeigt an, dass alles fertig ist
        // getController() gibt den Controller zurück, der mit der FXML verbunden ist
        System.out.println("[UI_DEBUG] FXML_APP_STARTED | Controller=" + loader.getController());
    }

    /**
     * Alternative Hauptmethode zum direkten Starten dieser Klasse.
     * Normalerweise wird die App aber über org.example.Main gestartet.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        launch(args); // Startet die JavaFX-Anwendung
    }
}
