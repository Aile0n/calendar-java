import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX-Anwendung, die die Oberfläche aus FXML (calendar_view.fxml) lädt.
 */
public class CalendarFxmlApp extends Application {

    /** Startet die JavaFX-Bühne und lädt die FXML-Szene. */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/calendar_view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("CalendarProjekt (FXML)");
        stage.setScene(scene);
        stage.show();
        System.out.println("[UI_DEBUG] FXML_APP_STARTED | Controller=" + loader.getController());
    }

    /** Starter (Launcher) für diese FXML-Variante. */
    public static void main(String[] args) {
        launch(args);
    }
}
