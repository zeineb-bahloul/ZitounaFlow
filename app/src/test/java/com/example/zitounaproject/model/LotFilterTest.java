package com.example.zitounaproject.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Tests des campagnes oléicoles, de la recherche et du bilan (US-20, US-21). */
public class LotFilterTest {

    private static Date date(int annee, int mois, int jour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(annee, mois, jour);
        return c.getTime();
    }

    private static Lot lot(String numero, String variete, double kg, Double huile) {
        Lot l = new Lot();
        l.setNumero(numero);
        l.setVariete(variete);
        l.setPoidsKg(kg);
        l.setQuantiteHuileL(huile);
        return l;
    }

    @Test
    public void campagneDeSeptembreAAout() {
        assertEquals("2026/2027", LotFilter.campagne(date(2026, Calendar.NOVEMBER, 16)));
        assertEquals("2026/2027", LotFilter.campagne(date(2027, Calendar.FEBRUARY, 10)));
        assertEquals("2025/2026", LotFilter.campagne(date(2026, Calendar.AUGUST, 31)));
        assertEquals("2026/2027", LotFilter.campagne(date(2026, Calendar.SEPTEMBER, 1)));
    }

    @Test
    public void rechercheSansAccentsNiMajuscules() {
        Lot l = lot("LOT-012", "Chétoui", 1000, null);
        l.setNomExploitation("Domaine Ben Salem");
        assertEquals(true, LotFilter.correspond(l, "chetoui"));
        assertEquals(true, LotFilter.correspond(l, "ben sal"));
        assertEquals(true, LotFilter.correspond(l, "lot-012"));
        assertEquals(false, LotFilter.correspond(l, "picholine"));
        assertEquals(true, LotFilter.correspond(l, ""));
    }

    @Test
    public void bilanRendementSurLesLotsPresses() {
        List<Lot> lots = new ArrayList<>();
        lots.add(lot("A", "Chétoui", 1000, 210.0));
        lots.add(lot("B", "Chemlali", 1000, 190.0));
        lots.add(lot("C", "Chemlali", 500, null)); // pas encore pressé
        LotFilter.Bilan b = LotFilter.bilan(lots);
        assertEquals(3, b.nbLots);
        assertEquals(2500.0, b.poidsKg, 0.001);
        assertEquals(400.0, b.huileL, 0.001);
        assertEquals(20.0, b.rendementMoyen(), 0.001); // 400 L / 2000 kg pressés
    }
}
