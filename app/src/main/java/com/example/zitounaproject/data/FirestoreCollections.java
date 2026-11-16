package com.example.zitounaproject.data;

/** Noms des collections Cloud Firestore. */
public final class FirestoreCollections {
    public static final String UTILISATEURS = "utilisateurs";
    public static final String RENDEZ_VOUS = "rendezVous";
    public static final String NOTIFICATIONS = "notifications";
    /** Un document par jour (yyyy-MM-dd) : nombre de réservations par créneau. */
    public static final String PLANNING = "planning";
    public static final String LOTS = "lots";
    /** Sous-collection de chaque lot. */
    public static final String HISTORIQUE = "historique";
    /** Compteurs (numérotation des lots). */
    public static final String COMPTEURS = "compteurs";

    private FirestoreCollections() {
    }
}
