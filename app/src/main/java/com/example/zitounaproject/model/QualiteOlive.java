package com.example.zitounaproject.model;

import androidx.annotation.ColorRes;

import com.example.zitounaproject.R;

/** Classe de qualité visuelle des olives (diagramme de classes : énumération QualiteOlive). */
public enum QualiteOlive {
    EXCELLENTE("Excellente", R.color.olive_700),
    ACCEPTABLE("Acceptable", R.color.gold),
    DEFECTUEUSE("Défectueuse", R.color.error);

    private final String libelle;
    @ColorRes private final int couleur;

    QualiteOlive(String libelle, @ColorRes int couleur) {
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public String getLibelle() { return libelle; }

    @ColorRes
    public int getCouleur() { return couleur; }
}
