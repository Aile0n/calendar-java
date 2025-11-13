package org.example;

/**
 * Starter, der eine JavaFX-Anwendung per Reflection startet, um Modul-/Laufzeitprobleme
 * beim Starten von "shaded" JARs auf einigen Plattformen zu vermeiden.
 */
public final class Main {
    /** Einstiegspunkt der Anwendung. */
    public static void main(String[] args) {
        try {
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
