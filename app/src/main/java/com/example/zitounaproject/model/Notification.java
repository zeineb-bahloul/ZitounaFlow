package com.example.zitounaproject.model;

import java.util.Date;

/** Notification reçue par un utilisateur (diagramme de classes : Notification). */
public class Notification {

    public static final String TYPE_RDV_CONFIRME = "RDV_CONFIRME";
    public static final String TYPE_RDV_REFUSE = "RDV_REFUSE";
    public static final String TYPE_LOT = "LOT";
    public static final String TYPE_INFO = "INFO";

    private String id;
    private String destinataireId;
    private String titre;
    private String message;
    private String type = TYPE_INFO;
    private Date dateEnvoi;
    private boolean lue;

    public Notification() {
        // Requis par Firestore
    }

    public Notification(String destinataireId, String titre, String message, String type) {
        this.destinataireId = destinataireId;
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.dateEnvoi = new Date();
        this.lue = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDestinataireId() { return destinataireId; }
    public void setDestinataireId(String destinataireId) { this.destinataireId = destinataireId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(Date dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }
}
