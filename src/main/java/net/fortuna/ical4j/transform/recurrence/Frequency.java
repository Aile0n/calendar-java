package net.fortuna.ical4j.transform.recurrence;

/**
 * Minimales Kompatibilitäts-Enum für CalendarFX, das in ical4j 2.x vorhanden war.
 * CalendarFX erwartet zur Laufzeit die Klasse
 * net.fortuna.ical4j.transform.recurrence.Frequency.
 *
 * In ical4j 3.x existiert dieses Enum nicht mehr. Um einen
 * NoClassDefFoundError zur Laufzeit zu vermeiden, stellen wir hier eine
 * einfache, ausreichende Variante bereit.
 */
public enum Frequency {
    SECONDLY,
    MINUTELY,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
