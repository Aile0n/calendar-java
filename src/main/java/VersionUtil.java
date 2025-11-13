// Dienstprogramm zum konsistenten Ermitteln der Anwendungsversion zur Laufzeit.
public final class VersionUtil {
    private VersionUtil() {}

    /**
     * Liefert die Anwendungsversion. Versucht zuerst Manifest Implementation-Version,
     * dann Maven pom.properties, danach "1.0.3" als Fallback.
     */
    public static String getVersion() {
        // 1) Manifest Implementation-Version prüfen (im shaded JAR über manifestEntries vorhanden)
        try {
            Package p = VersionUtil.class.getPackage();
            if (p != null) {
                String impl = p.getImplementationVersion();
                if (impl != null && !impl.isBlank()) return impl;
            }
        } catch (Throwable ignored) {}

        // 2) Maven pom.properties versuchen (in vielen Builds vorhanden)
        try (java.io.InputStream is = VersionUtil.class.getResourceAsStream("/META-INF/maven/org.example/calendar-java/pom.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String v = props.getProperty("version");
                if (v != null && !v.isBlank()) return v;
            }
        } catch (Throwable ignored) {}

        // 3) Fallback für IDE-/Test-Läufe
        return "1.0.3";
    }
}
