package com.example.zitounaproject.model;

import java.util.Calendar;
import java.util.Date;

/** Paramètres de l'huilerie : créneaux de pressage, capacités, variétés. */
public final class Creneaux {

    public static final String[] LIBELLES = {
            "08:00 - 10:00", "10:30 - 12:30", "14:00 - 16:00", "16:30 - 18:30"
    };
    private static final int[][] DEBUTS = {{8, 0}, {10, 30}, {14, 0}, {16, 30}};

    /** Nombre maximal de rendez-vous (en attente + confirmés) par créneau. */
    public static final int CAPACITE_PAR_CRENEAU = 4;
    /** Capacité de réception du quai par jour (kg). */
    public static final double CAPACITE_JOUR_KG = 22000;
    /** Capacité moyenne de la machine (kg/h) — sert à estimer les durées. */
    public static final double CAPACITE_MACHINE_KG_H = 1000;
    public static final double KG_PAR_CAISSE = 50;
    /** Fourchette de rendement habituel (litres d'huile pour 100 kg d'olives). */
    public static final double RENDEMENT_MIN = 18;
    public static final double RENDEMENT_MAX = 22;
    /** Nombre de jours réservables à l'avance. */
    public static final int JOURS_RESERVABLES = 14;

    public static final String[] VARIETES = {"Chétoui", "Chemlali", "Picholine", "Mixte"};

    private Creneaux() {
    }

    /** Clé du compteur de réservations dans le document planning/{jour}. */
    public static String cle(int index) {
        return "s" + index;
    }

    public static int indexOf(String libelle) {
        for (int i = 0; i < LIBELLES.length; i++) {
            if (LIBELLES[i].equals(libelle)) return i;
        }
        return -1;
    }

    /** Date et heure de début du créneau {@code index} pour le jour donné. */
    public static Date debut(Date jour, int index) {
        Calendar c = Calendar.getInstance();
        c.setTime(jour);
        c.set(Calendar.HOUR_OF_DAY, DEBUTS[index][0]);
        c.set(Calendar.MINUTE, DEBUTS[index][1]);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
