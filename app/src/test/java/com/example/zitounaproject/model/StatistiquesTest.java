package com.example.zitounaproject.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Tests des statistiques (US-22) : totaux, rendement moyen, attente moyenne, qualité, barres. */
public class StatistiquesTest {

    private static Date date(int jour, int heure, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.NOVEMBER, jour, heure, minute);
        return c.getTime();
    }

    private static Lot lot(String variete, double kg, Double huile, Date reception, Date debut, QualiteOlive q) {
        Lot l = new Lot();
        l.setVariete(variete);
        l.setPoidsKg(kg);
        l.setQuantiteHuileL(huile);
        l.setDateReception(reception);
        l.setDateDebutTraitement(debut);
        l.setQualite(q);
        return l;
    }

    private List<Lot> lots() {
        List<Lot> lots = new ArrayList<>();
        lots.add(lot("Chétoui", 1000, 220.0, date(16, 8, 0), date(16, 8, 30), QualiteOlive.EXCELLENTE));
        lots.add(lot("Chemlali", 1000, 180.0, date(16, 9, 0), date(16, 10, 30), QualiteOlive.ACCEPTABLE));
        lots.add(lot("Chemlali", 500, null, date(17, 14, 0), null, null));
        lots.add(lot("Chétoui", 800, 160.0, date(10, 9, 0), date(10, 9, 0), null)); // hors période
        return lots;
    }

    @Test
    public void totauxEtMoyennesSurLaPeriode() {
        Statistiques s = Statistiques.calculer(lots(), date(16, 0, 0), date(18, 0, 0), Statistiques.Granularite.JOUR);
        assertEquals(3, s.nbLots);
        assertEquals(2500.0, s.poidsTotalKg, 0.001);
        assertEquals(400.0, s.huileTotaleL, 0.001);
        assertEquals(20.0, s.rendementMoyen, 0.001);       // 400 L / 2000 kg pressés
        assertEquals(60, s.tempsAttenteMoyenMin);          // (30 + 90) / 2
    }

    @Test
    public void repartitionQualiteEtVarietes() {
        Statistiques s = Statistiques.calculer(lots(), date(16, 0, 0), date(18, 0, 0), Statistiques.Granularite.JOUR);
        assertEquals(1, (int) s.repartitionQualite.get(QualiteOlive.EXCELLENTE));
        assertEquals(1, (int) s.repartitionQualite.get(QualiteOlive.ACCEPTABLE));
        assertEquals(0, (int) s.repartitionQualite.get(QualiteOlive.DEFECTUEUSE));
        assertEquals(1, s.nbNonAnalyses);
        assertEquals(22.0, s.rendementParVariete().get("Chétoui"), 0.001);
        assertEquals(18.0, s.rendementParVariete().get("Chemlali"), 0.001);
    }

    @Test
    public void barresParJourEtParHeure() {
        Statistiques jour = Statistiques.calculer(lots(), date(16, 0, 0), date(18, 0, 0), Statistiques.Granularite.JOUR);
        assertEquals(2, jour.barres.size());
        assertEquals(2, jour.barres.get(0).nbLots);
        assertEquals(1, jour.barres.get(1).nbLots);

        Statistiques heure = Statistiques.calculer(lots(), date(16, 0, 0), date(17, 0, 0), Statistiques.Granularite.HEURE);
        assertEquals(1000.0, heure.barres.get(8 - Statistiques.HEURE_DEBUT).poidsKg, 0.001);
        assertEquals(1000.0, heure.barres.get(9 - Statistiques.HEURE_DEBUT).poidsKg, 0.001);
    }
}
