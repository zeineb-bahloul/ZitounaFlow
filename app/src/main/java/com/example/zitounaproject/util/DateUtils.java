package com.example.zitounaproject.util;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formatage des dates en français et calculs sur les jours. */
public final class DateUtils {

    private static final Locale FR = Locale.FRENCH;

    private DateUtils() {
    }

    /** Clé de jour "yyyy-MM-dd" (ex. 2026-11-17). */
    public static String jourKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
    }

    public static Date startOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date today() {
        return startOfDay(new Date());
    }

    public static Date addDays(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }

    public static boolean isSameDay(Date a, Date b) {
        return jourKey(a).equals(jourKey(b));
    }

    /** Convertit la sélection UTC d'un MaterialDatePicker en date locale à minuit. */
    public static Date fromPickerSelection(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local.getTime();
    }

    /** "Mardi 17 novembre" */
    public static String jourLong(@Nullable Date date) {
        if (date == null) return "";
        return capitalize(new SimpleDateFormat("EEEE d MMMM", FR).format(date));
    }

    /** "mar. 17 nov." */
    public static String jourCourt(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("EEE d MMM", FR).format(date);
    }

    /** "MAR" */
    public static String jourSemaine(Date date) {
        return new SimpleDateFormat("EEE", FR).format(date).replace(".", "").toUpperCase(FR);
    }

    /** "17" */
    public static String numeroJour(Date date) {
        return new SimpleDateFormat("d", FR).format(date);
    }

    /** "Novembre 2026" */
    public static String moisAnnee(Date date) {
        return capitalize(new SimpleDateFormat("MMMM yyyy", FR).format(date));
    }

    /** "17 nov. 2026, 14:05" */
    public static String dateHeure(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("d MMM yyyy, HH:mm", FR).format(date);
    }

    /** "Aujourd'hui", "Demain" ou "mar. 17 nov." */
    public static String relatif(Date date) {
        Date jour = startOfDay(date);
        if (isSameDay(jour, today())) return "Aujourd'hui";
        if (isSameDay(jour, addDays(today(), 1))) return "Demain";
        if (isSameDay(jour, addDays(today(), -1))) return "Hier";
        return jourCourt(date);
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }
}
