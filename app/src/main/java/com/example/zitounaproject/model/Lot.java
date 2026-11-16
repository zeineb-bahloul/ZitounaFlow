package com.example.zitounaproject.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Lot d'olives déposé à l'huilerie (diagramme de classes : Lot).
 * Collection Firestore "lots" ; l'historique des statuts est dans la sous-collection "historique".
 */
public class Lot {

    private String id;
    /** Numéro lisible, ex. "LOT-012". */
    private String numero;

    // Propriétaire (un lot est rattaché à un seul agriculteur ; null pour un apport sans compte)
    private String agriculteurId;
    private String agriculteurNom;
    private String nomExploitation;
    private String telephone;
    private String rendezVousId;

    // Pesée et spécifications
    private String variete;
    private String typeContenant;
    private double poidsBrutKg;
    private double tareKg;
    /** Poids net d'olives (kg). */
    private double poidsKg;
    private String origine;
    private String remarque;
    private Date dateReception;

    // File d'attente
    private StatutLot statut = StatutLot.RECEPTIONNE;
    /** Ordre dans la file : plus petit = passe plus tôt. */
    private double ordreFile;
    private boolean prioritaire;
    /** 0 = en traitement, 1 = prochain lot, ... */
    private int positionFile;
    private int tempsAttenteEstimeMin;
    private Date debutEstime;
    private Date finEstimee;
    private Date dateEstimation;
    private Date dateDebutTraitement;

    /** Qualité validée par le gestionnaire après l'analyse IA (sprint 6). */
    private QualiteOlive qualite;

    // Production (sprint 4)
    private Double quantiteHuileL;
    private Double rendementPourcent;
    private Date dateFinTraitement;
    private Date dateRecuperation;

    public Lot() {
        // Requis par Firestore
    }

    // --- Méthodes du diagramme de classes ---

    public void changerStatut(StatutLot nouveau) {
        this.statut = nouveau;
    }

    /** Rendement = litres d'huile / poids d'olives (kg) × 100. */
    public double calculerRendement() {
        if (quantiteHuileL == null || poidsKg <= 0) return 0;
        return quantiteHuileL / poidsKg * 100;
    }

    /** Minutes restantes avant le début estimé du traitement. */
    public int estimerAttente() {
        if (debutEstime == null) return tempsAttenteEstimeMin;
        long ms = debutEstime.getTime() - System.currentTimeMillis();
        return (int) Math.max(0, Math.round(ms / 60000.0));
    }

    // --- Calculs utilitaires (non enregistrés) ---

    /** Durée de traitement estimée (minutes) selon la capacité machine. */
    public int dureeTraitementMin() {
        return (int) Math.round(poidsKg / Creneaux.CAPACITE_MACHINE_KG_H * 60);
    }

    /** Avancement du traitement entre 0 et 1 (uniquement en traitement). */
    public double progressionTraitement() {
        if (statut != StatutLot.EN_TRAITEMENT || dateDebutTraitement == null) return 0;
        double ecoule = (System.currentTimeMillis() - dateDebutTraitement.getTime()) / 60000.0;
        int duree = Math.max(1, dureeTraitementMin());
        return Math.max(0, Math.min(0.99, ecoule / duree));
    }

    /** Minutes restantes de traitement. */
    public int minutesRestantesTraitement() {
        return (int) Math.round(dureeTraitementMin() * (1 - progressionTraitement()));
    }

    @Exclude
    public String getNomProducteur() {
        if (nomExploitation != null && !nomExploitation.trim().isEmpty()) return nomExploitation;
        return agriculteurNom != null ? agriculteurNom : "Producteur";
    }

    /** Prêt mais l'huile n'a pas encore été récupérée. */
    @Exclude
    public boolean isEnAttenteDeRecuperation() {
        return statut == StatutLot.PRET_A_RECUPERER && dateRecuperation == null;
    }

    /** Lot encore « en cours » : dans la file ou huile pas encore récupérée. */
    @Exclude
    public boolean isEnCours() {
        return (statut != null && statut.estDansLaFile()) || isEnAttenteDeRecuperation();
    }

    @Exclude
    public boolean isSansCompte() {
        return agriculteurId == null;
    }

    // --- Getters / setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getAgriculteurId() { return agriculteurId; }
    public void setAgriculteurId(String agriculteurId) { this.agriculteurId = agriculteurId; }

    public String getAgriculteurNom() { return agriculteurNom; }
    public void setAgriculteurNom(String agriculteurNom) { this.agriculteurNom = agriculteurNom; }

    public String getNomExploitation() { return nomExploitation; }
    public void setNomExploitation(String nomExploitation) { this.nomExploitation = nomExploitation; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getRendezVousId() { return rendezVousId; }
    public void setRendezVousId(String rendezVousId) { this.rendezVousId = rendezVousId; }

    public String getVariete() { return variete; }
    public void setVariete(String variete) { this.variete = variete; }

    public String getTypeContenant() { return typeContenant; }
    public void setTypeContenant(String typeContenant) { this.typeContenant = typeContenant; }

    public double getPoidsBrutKg() { return poidsBrutKg; }
    public void setPoidsBrutKg(double poidsBrutKg) { this.poidsBrutKg = poidsBrutKg; }

    public double getTareKg() { return tareKg; }
    public void setTareKg(double tareKg) { this.tareKg = tareKg; }

    public double getPoidsKg() { return poidsKg; }
    public void setPoidsKg(double poidsKg) { this.poidsKg = poidsKg; }

    public String getOrigine() { return origine; }
    public void setOrigine(String origine) { this.origine = origine; }

    public String getRemarque() { return remarque; }
    public void setRemarque(String remarque) { this.remarque = remarque; }

    public Date getDateReception() { return dateReception; }
    public void setDateReception(Date dateReception) { this.dateReception = dateReception; }

    public StatutLot getStatut() { return statut; }
    public void setStatut(StatutLot statut) { this.statut = statut; }

    public double getOrdreFile() { return ordreFile; }
    public void setOrdreFile(double ordreFile) { this.ordreFile = ordreFile; }

    public boolean isPrioritaire() { return prioritaire; }
    public void setPrioritaire(boolean prioritaire) { this.prioritaire = prioritaire; }

    public int getPositionFile() { return positionFile; }
    public void setPositionFile(int positionFile) { this.positionFile = positionFile; }

    public int getTempsAttenteEstimeMin() { return tempsAttenteEstimeMin; }
    public void setTempsAttenteEstimeMin(int tempsAttenteEstimeMin) { this.tempsAttenteEstimeMin = tempsAttenteEstimeMin; }

    @Nullable
    public Date getDebutEstime() { return debutEstime; }
    public void setDebutEstime(Date debutEstime) { this.debutEstime = debutEstime; }

    @Nullable
    public Date getFinEstimee() { return finEstimee; }
    public void setFinEstimee(Date finEstimee) { this.finEstimee = finEstimee; }

    public Date getDateEstimation() { return dateEstimation; }
    public void setDateEstimation(Date dateEstimation) { this.dateEstimation = dateEstimation; }

    public Date getDateDebutTraitement() { return dateDebutTraitement; }
    public void setDateDebutTraitement(Date dateDebutTraitement) { this.dateDebutTraitement = dateDebutTraitement; }

    @Nullable
    public QualiteOlive getQualite() { return qualite; }
    public void setQualite(QualiteOlive qualite) { this.qualite = qualite; }

    @Nullable
    public Double getQuantiteHuileL() { return quantiteHuileL; }
    public void setQuantiteHuileL(Double quantiteHuileL) { this.quantiteHuileL = quantiteHuileL; }

    @Nullable
    public Double getRendementPourcent() { return rendementPourcent; }
    public void setRendementPourcent(Double rendementPourcent) { this.rendementPourcent = rendementPourcent; }

    public Date getDateFinTraitement() { return dateFinTraitement; }
    public void setDateFinTraitement(Date dateFinTraitement) { this.dateFinTraitement = dateFinTraitement; }

    public Date getDateRecuperation() { return dateRecuperation; }
    public void setDateRecuperation(Date dateRecuperation) { this.dateRecuperation = dateRecuperation; }
}
