package com.example.zitounaproject.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcul de la file d'attente (BF-13, règle de gestion) :
 * le temps d'attente d'un lot dépend du poids des lots qui le précèdent et de la
 * capacité moyenne de la machine (kg/h). Une seule ligne d'extraction traite les lots un par un.
 */
public final class FileAttenteCalculator {

    /** Résultat pour un lot. */
    public static final class Estimation {
        /** 0 = en traitement, 1 = prochain lot, ... */
        public final int position;
        public final Date debut;
        public final Date fin;
        public final int attenteMin;

        Estimation(int position, Date debut, Date fin, int attenteMin) {
            this.position = position;
            this.debut = debut;
            this.fin = fin;
            this.attenteMin = attenteMin;
        }
    }

    private FileAttenteCalculator() {
    }

    /**
     * Ordre de passage : les lots en traitement d'abord (par heure de début),
     * puis les lots réceptionnés / en attente par ordreFile croissant. Les lots prêts sont exclus.
     */
    public static List<Lot> ordonner(List<Lot> lots) {
        List<Lot> enTraitement = new ArrayList<>();
        List<Lot> enFile = new ArrayList<>();
        for (Lot l : lots) {
            if (l.getStatut() == StatutLot.EN_TRAITEMENT) enTraitement.add(l);
            else if (l.getStatut() != null && l.getStatut().estDansLaFile()) enFile.add(l);
        }
        Collections.sort(enTraitement, (a, b) -> {
            if (a.getDateDebutTraitement() == null || b.getDateDebutTraitement() == null) return 0;
            return a.getDateDebutTraitement().compareTo(b.getDateDebutTraitement());
        });
        Collections.sort(enFile, (a, b) -> Double.compare(a.getOrdreFile(), b.getOrdreFile()));
        List<Lot> result = new ArrayList<>(enTraitement);
        result.addAll(enFile);
        return result;
    }

    public static Map<String, Estimation> calculer(List<Lot> lots, Date maintenant) {
        return calculer(lots, maintenant, Creneaux.CAPACITE_MACHINE_KG_H);
    }

    public static Map<String, Estimation> calculer(List<Lot> lots, Date maintenant, double capaciteKgH) {
        Map<String, Estimation> result = new HashMap<>();
        long curseur = maintenant.getTime();
        int position = 0;
        for (Lot lot : ordonner(lots)) {
            long dureeMs = Math.round(lot.getPoidsKg() / capaciteKgH * 3600_000L);
            if (lot.getStatut() == StatutLot.EN_TRAITEMENT) {
                long debut = lot.getDateDebutTraitement() != null
                        ? lot.getDateDebutTraitement().getTime() : maintenant.getTime();
                long fin = Math.max(debut + dureeMs, maintenant.getTime());
                curseur = Math.max(curseur, fin);
                result.put(lot.getId(), new Estimation(0, new Date(debut), new Date(fin), 0));
            } else {
                position++;
                long debut = curseur;
                long fin = debut + dureeMs;
                int attente = (int) Math.round((debut - maintenant.getTime()) / 60000.0);
                result.put(lot.getId(), new Estimation(position, new Date(debut), new Date(fin), attente));
                curseur = fin;
            }
        }
        return result;
    }
}
