package com.example.zitounaproject.model;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import com.example.zitounaproject.R;

/**
 * Cycle de vie d'un lot (diagramme de classes : énumération StatutLot).
 * Le cycle est strictement séquentiel : Réceptionné → En attente → En traitement → Prêt à récupérer.
 */
public enum StatutLot {
    RECEPTIONNE("Réceptionné", "Placer en attente", R.color.sand, R.color.ink),
    EN_ATTENTE("En attente", "Lancer le traitement", R.color.gold_light, R.color.gold_dark),
    EN_TRAITEMENT("En traitement", "Enregistrer l'huile produite", R.color.olive_700, R.color.white),
    PRET_A_RECUPERER("Prêt à récupérer", null, R.color.olive_100, R.color.olive_700);

    private final String libelle;
    @Nullable private final String actionSuivante;
    @ColorRes private final int couleurFond;
    @ColorRes private final int couleurTexte;

    StatutLot(String libelle, @Nullable String actionSuivante, @ColorRes int couleurFond, @ColorRes int couleurTexte) {
        this.libelle = libelle;
        this.actionSuivante = actionSuivante;
        this.couleurFond = couleurFond;
        this.couleurTexte = couleurTexte;
    }

    public String getLibelle() { return libelle; }

    /** Libellé du bouton qui fait passer au statut suivant. */
    @Nullable
    public String getActionSuivante() { return actionSuivante; }

    @ColorRes
    public int getCouleurFond() { return couleurFond; }

    @ColorRes
    public int getCouleurTexte() { return couleurTexte; }

    /** Statut suivant dans le cycle, ou null si le lot est déjà prêt. */
    @Nullable
    public StatutLot suivant() {
        int next = ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }

    /** Un lot est dans la file tant qu'il n'est pas prêt à récupérer. */
    public boolean estDansLaFile() {
        return this != PRET_A_RECUPERER;
    }
}
