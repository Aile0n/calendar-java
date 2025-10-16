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
    /**
     * Haupteinstiegspunkt der Anwendung.
     * Diese Methode wird automatisch von Java aufgerufen, wenn das Programm startet.
     *
     * @param args Kommandozeilenargumente (z.B. wenn man das Programm mit zusätzlichen Parametern startet)
     */
    public static void main(String[] args) {
        try {
            // Wir versuchen, die richtige JavaFX-Anwendungsklasse zu finden und zu starten

            // Bevorzugt die FXML-basierte App, damit Änderungen im Controller sichtbar sind
            Class<?> appClass; // Hier speichern wir die Klasse, die wir starten wollen

            try {
                // Zuerst versuchen wir, die FXML-Version zu laden (moderne UI-Variante)
                appClass = Class.forName("CalendarFxmlApp");
                System.out.println("[LAUNCH] Starte CalendarFxmlApp (FXML Controller)");
            } catch (ClassNotFoundException nf) {
                // Falls FXML-Version nicht gefunden wird, nutzen wir die programmierte UI-Variante
                appClass = Class.forName("CalendarProjektApp");
                System.out.println("[LAUNCH] CalendarFxmlApp nicht gefunden, starte CalendarProjektApp");
            }

            // Jetzt starten wir die JavaFX-Anwendung mit Reflection (dynamisches Laden)
            // Das ist ein Trick, um JavaFX-Probleme beim JAR-Start zu vermeiden
            Class<?> fxApp = Class.forName("javafx.application.Application");
            fxApp.getMethod("launch", Class.class, String[].class)
                    .invoke(null, appClass, (Object) args);

        } catch (Throwable t) {
            // Falls etwas schiefgeht, zeigen wir die Fehlermeldung an
            t.printStackTrace();
            System.err.println("Konnte JavaFX-Anwendung nicht starten. Stellen Sie sicher, dass JavaFX im JAR enthalten ist.");
        }
    }
}
