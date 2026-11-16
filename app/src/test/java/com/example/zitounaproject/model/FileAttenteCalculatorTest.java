package com.example.zitounaproject.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Tests de la règle de gestion : attente = poids des lots précédents / capacité machine. */
public class FileAttenteCalculatorTest {

    private static final double CAPACITE = 1000; // kg/h
    private final Date maintenant = new Date(1_800_000_000_000L);

    private Lot lot(String id, StatutLot statut, double poids, double ordre) {
        Lot l = new Lot();
        l.setId(id);
        l.setStatut(statut);
        l.setPoidsKg(poids);
        l.setOrdreFile(ordre);
        return l;
    }

    @Test
    public void filePositionsEtAttenteSansTraitementEnCours() {
        List<Lot> lots = new ArrayList<>();
        lots.add(lot("B", StatutLot.EN_ATTENTE, 500, 2));
        lots.add(lot("A", StatutLot.RECEPTIONNE, 1000, 1));
        lots.add(lot("C", StatutLot.EN_ATTENTE, 250, 3));

        Map<String, FileAttenteCalculator.Estimation> e = FileAttenteCalculator.calculer(lots, maintenant, CAPACITE);

        assertEquals(1, e.get("A").position);
        assertEquals(0, e.get("A").attenteMin);
        assertEquals(2, e.get("B").position);
        assertEquals(60, e.get("B").attenteMin);   // 1000 kg à 1000 kg/h
        assertEquals(3, e.get("C").position);
        assertEquals(90, e.get("C").attenteMin);   // + 500 kg = 30 min
    }

    @Test
    public void lotEnTraitementCompteSeulementPourSonTempsRestant() {
        Lot enCours = lot("T", StatutLot.EN_TRAITEMENT, 1000, 0);
        // Démarré il y a 15 min : il reste 45 min
        enCours.setDateDebutTraitement(new Date(maintenant.getTime() - 15 * 60_000L));
        List<Lot> lots = new ArrayList<>();
        lots.add(enCours);
        lots.add(lot("A", StatutLot.EN_ATTENTE, 500, 1));

        Map<String, FileAttenteCalculator.Estimation> e = FileAttenteCalculator.calculer(lots, maintenant, CAPACITE);

        assertEquals(0, e.get("T").position);
        assertEquals(1, e.get("A").position);
        assertEquals(45, e.get("A").attenteMin);
    }

    @Test
    public void lotsPretsExclusDeLaFile() {
        List<Lot> lots = new ArrayList<>();
        lots.add(lot("P", StatutLot.PRET_A_RECUPERER, 2000, 0));
        lots.add(lot("A", StatutLot.EN_ATTENTE, 500, 1));

        Map<String, FileAttenteCalculator.Estimation> e = FileAttenteCalculator.calculer(lots, maintenant, CAPACITE);

        assertEquals(null, e.get("P"));
        assertEquals(1, e.get("A").position);
        assertEquals(0, e.get("A").attenteMin);
    }

    @Test
    public void rendementLitresSurKilos() {
        Lot l = lot("A", StatutLot.PRET_A_RECUPERER, 1450, 0);
        l.setQuantiteHuileL(304.5);
        assertEquals(21.0, l.calculerRendement(), 0.001);
    }
}
