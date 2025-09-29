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
            // Versuche, die Main-Klasse im Standard-Package zu finden
            Class<?> appClass = Class.forName("CalendarProjektApp");
            Class<?> fxApp = Class.forName("javafx.application.Application");
            fxApp.getMethod("launch", Class.class, String[].class)
                .invoke(null, appClass, (Object) args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Konnte JavaFX-Anwendung nicht starten. Stellen Sie sicher, dass JavaFX im JAR enthalten ist.");
        }
    }
}
