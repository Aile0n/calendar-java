package org.example;

/**
 * Launcher-Klasse ohne Erben von javafx.application.Application.
 *
 * Hintergrund: Beim Start eines JARs, dessen Main-Class direkt von
 * javafx.application.Application erbt, meldet der Java-Launcher oft
 * "JavaFX runtime components are missing". Dieses kleine Wrapper-Main
 * umgeht die spezielle JavaFX-Startlogik, indem es die Anwendung
 * programmgesteuert startet.
 */
public final class Main {
    public static void main(String[] args) {
        try {
            // Bevorzugt die FXML-basierte App, damit Änderungen im Controller sichtbar sind
            Class<?> appClass;
            try {
                appClass = Class.forName("CalendarFxmlApp");
                System.out.println("[LAUNCH] Starte CalendarFxmlApp (FXML Controller)");
            } catch (ClassNotFoundException nf) {
                appClass = Class.forName("CalendarProjektApp");
                System.out.println("[LAUNCH] CalendarFxmlApp nicht gefunden, starte CalendarProjektApp");
            }
            Class<?> fxApp = Class.forName("javafx.application.Application");
            fxApp.getMethod("launch", Class.class, String[].class)
                    .invoke(null, appClass, (Object) args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Konnte JavaFX-Anwendung nicht starten. Stellen Sie sicher, dass JavaFX im JAR enthalten ist.");
        }
    }
}
