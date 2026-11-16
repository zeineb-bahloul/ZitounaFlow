package com.example.zitounaproject.util;

import java.util.Locale;

/** Formatage des quantités (kg, litres, durées). */
public final class Formats {

    private Formats() {
    }

    /** "1 450 kg" */
    public static String kg(double kg) {
        return String.format(Locale.FRENCH, "%,.0f kg", kg);
    }

    public static String nombre(double value) {
        return String.format(Locale.FRENCH, "%,.0f", value);
    }

    /** "1h 45" ou "45 min" */
    public static String duree(double heures) {
        int minutes = (int) Math.round(heures * 60);
        if (minutes < 60) return minutes + " min";
        return String.format(Locale.FRENCH, "%dh %02d", minutes / 60, minutes % 60);
    }
}
