package com.example.zitounaproject.model;

/** Rôle d'un utilisateur (diagramme de classes : énumération Role). */
public enum Role {
    AGRICULTEUR("Agriculteur"),
    GESTIONNAIRE("Gestionnaire");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
