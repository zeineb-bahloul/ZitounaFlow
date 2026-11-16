package com.example.zitounaproject.model;

import androidx.annotation.ColorRes;

import com.example.zitounaproject.R;

/** États d'un rendez-vous (diagramme de classes : énumération StatutRendezVous). */
public enum StatutRendezVous {
    EN_ATTENTE_CONFIRMATION("En attente", R.color.gold_light, R.color.gold_dark),
    CONFIRME("Confirmé", R.color.olive_100, R.color.olive_700),
    REFUSE("Refusé", R.color.red_light, R.color.error),
    ANNULE("Annulé", R.color.sand, R.color.muted);

    private final String libelle;
    @ColorRes private final int couleurFond;
    @ColorRes private final int couleurTexte;

    StatutRendezVous(String libelle, @ColorRes int couleurFond, @ColorRes int couleurTexte) {
        this.libelle = libelle;
        this.couleurFond = couleurFond;
        this.couleurTexte = couleurTexte;
    }

    public String getLibelle() { return libelle; }

    @ColorRes
    public int getCouleurFond() { return couleurFond; }

    @ColorRes
    public int getCouleurTexte() { return couleurTexte; }
}
