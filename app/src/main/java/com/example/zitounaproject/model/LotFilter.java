package com.example.zitounaproject.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Recherche, campagnes oléicoles et bilans sur une liste de lots (BF-15, BF-27). */
public final class LotFilter {

    private LotFilter() {
    }

    /**
     * Campagne oléicole d'une date : de septembre à août.
     * Ex. 16/11/2026 → "2026/2027", 10/02/2027 → "2026/2027".
     */
    public static String campagne(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int annee = c.get(Calendar.YEAR);
        int debut = c.get(Calendar.MONTH) >= Calendar.SEPTEMBER ? annee : annee - 1;
        return debut + "/" + (debut + 1);
    }

    public static String campagneCourante() {
        return campagne(new Date());
    }

    /** Campagnes présentes dans la liste, la plus récente d'abord. */
    public static List<String> campagnes(List<Lot> lots) {
        Set<String> set = new TreeSet<>();
        for (Lot l : lots) if (l.getDateReception() != null) set.add(campagne(l.getDateReception()));
        List<String> list = new ArrayList<>(set);
        Collections.reverse(list);
        return list;
    }

    /** Recherche sans tenir compte des majuscules ni des accents. */
    public static boolean correspond(Lot lot, String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) return true;
        String q = normaliser(recherche);
        String[] champs = {lot.getNumero(), lot.getVariete(), lot.getNomExploitation(), lot.getAgriculteurNom(),
                lot.getTelephone(), lot.getOrigine(), lot.getTypeContenant()};
        for (String champ : champs) {
            if (champ != null && normaliser(champ).contains(q)) return true;
        }
        return false;
    }

    static String normaliser(String s) {
        String sansAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).trim();
    }

    /** Totaux d'une sélection de lots (bilan de campagne / historique global). */
    public static final class Bilan {
        public int nbLots;
        public double poidsKg;
        public double huileL;
        /** Poids des seuls lots dont l'huile est connue (pour le rendement moyen). */
        public double poidsAvecHuileKg;

        /** Rendement moyen = huile totale / poids des lots pressés × 100. */
        public double rendementMoyen() {
            return poidsAvecHuileKg > 0 ? huileL / poidsAvecHuileKg * 100 : 0;
        }
    }

    public static Bilan bilan(List<Lot> lots) {
        Bilan b = new Bilan();
        for (Lot l : lots) {
            b.nbLots++;
            b.poidsKg += l.getPoidsKg();
            if (l.getQuantiteHuileL() != null) {
                b.huileL += l.getQuantiteHuileL();
                b.poidsAvecHuileKg += l.getPoidsKg();
            }
        }
        return b;
    }
}
