package com.example.zitounaproject.model;

import java.util.Date;

/** Trace d'un changement de statut d'un lot (diagramme de classes : HistoriqueStatut). */
public class HistoriqueStatut {

    private String id;
    private StatutLot ancienStatut;
    private StatutLot nouveauStatut;
    private Date dateChangement;
    private String parId;

    public HistoriqueStatut() {
        // Requis par Firestore
    }

    public HistoriqueStatut(StatutLot ancienStatut, StatutLot nouveauStatut, String parId) {
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.parId = parId;
        this.dateChangement = new Date();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public StatutLot getAncienStatut() { return ancienStatut; }
    public void setAncienStatut(StatutLot ancienStatut) { this.ancienStatut = ancienStatut; }

    public StatutLot getNouveauStatut() { return nouveauStatut; }
    public void setNouveauStatut(StatutLot nouveauStatut) { this.nouveauStatut = nouveauStatut; }

    public Date getDateChangement() { return dateChangement; }
    public void setDateChangement(Date dateChangement) { this.dateChangement = dateChangement; }

    public String getParId() { return parId; }
    public void setParId(String parId) { this.parId = parId; }
}
