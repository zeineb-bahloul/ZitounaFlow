package com.example.zitounaproject.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Statistiques de l'huilerie sur une période (diagramme de classes : Statistiques, BF-28) :
 * lots par jour, poids total, huile produite, rendement moyen, temps moyen d'attente,
 * répartition de la qualité et rendement par variété.
 */
public class Statistiques {

    /** Découpage des graphiques. */
    public enum Granularite { HEURE, JOUR, MOIS }

    /** Une barre de graphique. */
    public static final class Barre {
        public final String label;
        public int nbLots;
        public double poidsKg;

        Barre(String label) {
            this.label = label;
        }
    }

    // --- Attributs du diagramme de classes ---
    public double poidsTotalKg;
    public double huileTotaleL;
    public double rendementMoyen;
    public int tempsAttenteMoyenMin;

    // --- Détails ---
    public int nbLots;
    public int nbLotsPresses;
    /** Poids des lots dont l'huile est connue (base du rendement moyen). */
    public double poidsPresseKg;
    public int nbLotsAvecAttente;
    public final Map<QualiteOlive, Integer> repartitionQualite = new EnumMap<>(QualiteOlive.class);
    public int nbNonAnalyses;
    /** Variété → {huile (L), poids pressé (kg)} */
    public final Map<String, double[]> parVariete = new TreeMap<>();
    public final List<Barre> barres = new ArrayList<>();

    /**
     * Calcule les statistiques des lots reçus entre {@code debut} (inclus) et {@code fin} (exclu).
     */
    public static Statistiques calculer(List<Lot> lots, Date debut, Date fin, Granularite granularite) {
        Statistiques s = new Statistiques();
        for (QualiteOlive q : QualiteOlive.values()) s.repartitionQualite.put(q, 0);
        s.barres.addAll(creerBarres(debut, fin, granularite));

        long attenteTotaleMin = 0;
        for (Lot l : lots) {
            Date reception = l.getDateReception();
            if (reception == null || reception.before(debut) || !reception.before(fin)) continue;

            s.nbLots++;
            s.poidsTotalKg += l.getPoidsKg();
            if (l.getQuantiteHuileL() != null) {
                s.nbLotsPresses++;
                s.huileTotaleL += l.getQuantiteHuileL();
                s.poidsPresseKg += l.getPoidsKg();
                String v = l.getVariete() != null ? l.getVariete() : "Autre";
                double[] cumul = s.parVariete.get(v);
                if (cumul == null) {
                    cumul = new double[2];
                    s.parVariete.put(v, cumul);
                }
                cumul[0] += l.getQuantiteHuileL();
                cumul[1] += l.getPoidsKg();
            }
            if (l.getDateDebutTraitement() != null) {
                long min = (l.getDateDebutTraitement().getTime() - reception.getTime()) / 60_000L;
                if (min >= 0) {
                    attenteTotaleMin += min;
                    s.nbLotsAvecAttente++;
                }
            }
            if (l.getQualite() != null) {
                s.repartitionQualite.put(l.getQualite(), s.repartitionQualite.get(l.getQualite()) + 1);
            } else {
                s.nbNonAnalyses++;
            }
            int index = indexBarre(debut, reception, granularite);
            if (index >= 0 && index < s.barres.size()) {
                s.barres.get(index).nbLots++;
                s.barres.get(index).poidsKg += l.getPoidsKg();
            }
        }
        s.rendementMoyen = s.poidsPresseKg > 0 ? s.huileTotaleL / s.poidsPresseKg * 100 : 0;
        s.tempsAttenteMoyenMin = s.nbLotsAvecAttente > 0 ? (int) (attenteTotaleMin / s.nbLotsAvecAttente) : 0;
        return s;
    }

    /** Rendement (%) de chaque variété, du meilleur au moins bon. */
    public Map<String, Double> rendementParVariete() {
        List<Map.Entry<String, double[]>> entries = new ArrayList<>(parVariete.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue()[0] / b.getValue()[1], a.getValue()[0] / a.getValue()[1]));
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> e : entries) {
            result.put(e.getKey(), e.getValue()[1] > 0 ? e.getValue()[0] / e.getValue()[1] * 100 : 0);
        }
        return result;
    }

    // ---------------------------------------------------------------- Barres

    /** Heures de travail affichées pour la granularité HEURE. */
    public static final int HEURE_DEBUT = 6;
    public static final int HEURE_FIN = 19;

    private static List<Barre> creerBarres(Date debut, Date fin, Granularite g) {
        List<Barre> barres = new ArrayList<>();
        if (g == Granularite.HEURE) {
            for (int h = HEURE_DEBUT; h < HEURE_FIN; h++) barres.add(new Barre(h + "h"));
            return barres;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(debut);
        SimpleDateFormat f = new SimpleDateFormat(g == Granularite.JOUR ? "d/MM" : "MMM", Locale.FRENCH);
        while (c.getTime().before(fin)) {
            barres.add(new Barre(f.format(c.getTime()).replace(".", "")));
            c.add(g == Granularite.JOUR ? Calendar.DAY_OF_YEAR : Calendar.MONTH, 1);
        }
        return barres;
    }

    private static int indexBarre(Date debut, Date date, Granularite g) {
        Calendar d = Calendar.getInstance();
        d.setTime(date);
        if (g == Granularite.HEURE) {
            int h = d.get(Calendar.HOUR_OF_DAY);
            if (h < HEURE_DEBUT) return 0;
            return Math.min(h, HEURE_FIN - 1) - HEURE_DEBUT;
        }
        Calendar s = Calendar.getInstance();
        s.setTime(debut);
        if (g == Granularite.MOIS) {
            return (d.get(Calendar.YEAR) - s.get(Calendar.YEAR)) * 12 + d.get(Calendar.MONTH) - s.get(Calendar.MONTH);
        }
        // JOUR : nombre de jours calendaires entre debut et date
        Calendar a = (Calendar) s.clone();
        int n = 0;
        while (a.get(Calendar.YEAR) < d.get(Calendar.YEAR)
                || (a.get(Calendar.YEAR) == d.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) < d.get(Calendar.DAY_OF_YEAR))) {
            a.add(Calendar.DAY_OF_YEAR, 1);
            n++;
            if (n > 400) break;
        }
        return n;
    }
}
