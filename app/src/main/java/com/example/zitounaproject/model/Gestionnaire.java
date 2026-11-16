package com.example.zitounaproject.model;

/** Gestionnaire de l'huilerie : confirme les RDV, enregistre les lots, gère la file. */
public class Gestionnaire extends Utilisateur {

    private String matricule;

    public Gestionnaire() {
        super(Role.GESTIONNAIRE);
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
}
