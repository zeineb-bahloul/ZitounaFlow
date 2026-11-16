package com.example.zitounaproject.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Rendez-vous de pressage demandé par un agriculteur (diagramme de classes : RendezVous).
 * Collection Firestore "rendezVous".
 */
public class RendezVous {

    private String id;
    private String agriculteurId;
    private String agriculteurNom;
    private String nomExploitation;
    private String telephone;
    /** Date et heure de début du créneau. */
    private Date date;
    /** Jour au format yyyy-MM-dd (utilisé pour les requêtes par jour). */
    private String jour;
    private String creneau;
    private String variete;
    private double quantiteEstimeeKg;
    private String remarque;
    private StatutRendezVous statut = StatutRendezVous.EN_ATTENTE_CONFIRMATION;
    private String motifRefus;
    private Date dateDemande;
    private Date dateTraitement;

    public RendezVous() {
        // Requis par Firestore
    }

    // --- Méthodes du diagramme de classes ---

    public void confirmer() {
        statut = StatutRendezVous.CONFIRME;
        dateTraitement = new Date();
    }

    public void refuser(String motif) {
        statut = StatutRendezVous.REFUSE;
        motifRefus = motif;
        dateTraitement = new Date();
    }

    public void annuler() {
        statut = StatutRendezVous.ANNULE;
        dateTraitement = new Date();
    }

    /** Un rendez-vous en attente ou confirmé, et pas encore passé, peut être annulé. */
    @Exclude
    public boolean isAnnulable() {
        boolean actif = statut == StatutRendezVous.EN_ATTENTE_CONFIRMATION
                || statut == StatutRendezVous.CONFIRME;
        return actif && date != null && date.after(new Date());
    }

    @Exclude
    public boolean isActif() {
        return statut == StatutRendezVous.EN_ATTENTE_CONFIRMATION || statut == StatutRendezVous.CONFIRME;
    }

    @Exclude
    public String getNomAffichage() {
        if (nomExploitation != null && !nomExploitation.trim().isEmpty()) return nomExploitation;
        return agriculteurNom != null ? agriculteurNom : "";
    }

    // --- Getters / setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAgriculteurId() { return agriculteurId; }
    public void setAgriculteurId(String agriculteurId) { this.agriculteurId = agriculteurId; }

    public String getAgriculteurNom() { return agriculteurNom; }
    public void setAgriculteurNom(String agriculteurNom) { this.agriculteurNom = agriculteurNom; }

    public String getNomExploitation() { return nomExploitation; }
    public void setNomExploitation(String nomExploitation) { this.nomExploitation = nomExploitation; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getJour() { return jour; }
    public void setJour(String jour) { this.jour = jour; }

    public String getCreneau() { return creneau; }
    public void setCreneau(String creneau) { this.creneau = creneau; }

    public String getVariete() { return variete; }
    public void setVariete(String variete) { this.variete = variete; }

    public double getQuantiteEstimeeKg() { return quantiteEstimeeKg; }
    public void setQuantiteEstimeeKg(double quantiteEstimeeKg) { this.quantiteEstimeeKg = quantiteEstimeeKg; }

    public String getRemarque() { return remarque; }
    public void setRemarque(String remarque) { this.remarque = remarque; }

    public StatutRendezVous getStatut() { return statut; }
    public void setStatut(StatutRendezVous statut) { this.statut = statut; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }

    public Date getDateDemande() { return dateDemande; }
    public void setDateDemande(Date dateDemande) { this.dateDemande = dateDemande; }

    public Date getDateTraitement() { return dateTraitement; }
    public void setDateTraitement(Date dateTraitement) { this.dateTraitement = dateTraitement; }
}
