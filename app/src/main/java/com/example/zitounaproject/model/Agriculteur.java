package com.example.zitounaproject.model;

import com.google.firebase.firestore.Exclude;

/** Agriculteur : demande des rendez-vous et suit ses lots. */
public class Agriculteur extends Utilisateur {

    private String adresse;
    private String nomExploitation;

    public Agriculteur() {
        super(Role.AGRICULTEUR);
    }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getNomExploitation() { return nomExploitation; }
    public void setNomExploitation(String nomExploitation) { this.nomExploitation = nomExploitation; }

    @Exclude
    @Override
    public String getNomAffichage() {
        return nomExploitation != null && !nomExploitation.trim().isEmpty()
                ? nomExploitation.trim()
                : super.getNomAffichage();
    }
}
