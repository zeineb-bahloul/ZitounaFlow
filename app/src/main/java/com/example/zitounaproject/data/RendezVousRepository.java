package com.example.zitounaproject.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zitounaproject.model.Creneaux;
import com.example.zitounaproject.model.Notification;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.StatutRendezVous;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.ErrorMessages;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion des rendez-vous (BF-10, BF-11, BF-20).
 * Le document planning/{jour} compte les réservations par créneau pour éviter la surréservation ;
 * il est mis à jour dans une transaction avec le rendez-vous.
 */
public class RendezVousRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------------------------------------------------------------- Agriculteur

    /** Nombre de réservations actives par créneau pour un jour donné. */
    public void chargerReservations(@NonNull String jour, @NonNull Callback<int[]> callback) {
        db.collection(FirestoreCollections.PLANNING).document(jour).get()
                .addOnSuccessListener(doc -> {
                    int[] counts = new int[Creneaux.LIBELLES.length];
                    for (int i = 0; i < counts.length; i++) {
                        Long v = doc.exists() ? doc.getLong(Creneaux.cle(i)) : null;
                        counts[i] = v == null ? 0 : v.intValue();
                    }
                    callback.onSuccess(counts);
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** US-08 : demande de rendez-vous (statut « En attente de confirmation »). */
    public void demander(@NonNull RendezVous rdv, @NonNull Callback<RendezVous> callback) {
        int index = Creneaux.indexOf(rdv.getCreneau());
        if (index < 0) {
            callback.onError("Créneau invalide.");
            return;
        }
        DocumentReference planningRef = db.collection(FirestoreCollections.PLANNING).document(rdv.getJour());
        DocumentReference rdvRef = db.collection(FirestoreCollections.RENDEZ_VOUS).document();
        DocumentReference notifRef = db.collection(FirestoreCollections.NOTIFICATIONS).document();
        rdv.setId(rdvRef.getId());
        rdv.setStatut(StatutRendezVous.EN_ATTENTE_CONFIRMATION);
        rdv.setDateDemande(new Date());
        Notification notif = new Notification(Notification.DESTINATAIRE_GESTIONNAIRES,
                "Nouvelle demande de rendez-vous",
                rdv.getNomAffichage() + " demande le créneau " + rdv.getCreneau() + " du "
                        + DateUtils.jourLong(rdv.getDate()) + " (" + rdv.getVariete() + ", "
                        + Formats.kg(rdv.getQuantiteEstimeeKg()) + ").",
                Notification.TYPE_RDV_DEMANDE);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot planning = transaction.get(planningRef);
                    String cle = Creneaux.cle(index);
                    Long v = planning.exists() ? planning.getLong(cle) : null;
                    long count = v == null ? 0 : v;
                    if (count >= Creneaux.CAPACITE_PAR_CRENEAU) {
                        throw new IllegalStateException("Ce créneau est complet, choisissez-en un autre.");
                    }
                    Map<String, Object> update = new HashMap<>();
                    update.put(cle, count + 1);
                    transaction.set(planningRef, update, SetOptions.merge());
                    transaction.set(rdvRef, rdv);
                    transaction.set(notifRef, notif);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onSuccess(rdv))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** US-09 : rendez-vous d'un agriculteur, en temps réel. */
    public ListenerRegistration ecouterMesRendezVous(@NonNull String agriculteurId,
                                                     @NonNull Callback<List<RendezVous>> callback) {
        Query query = db.collection(FirestoreCollections.RENDEZ_VOUS)
                .whereEqualTo("agriculteurId", agriculteurId);
        return query.addSnapshotListener((snap, e) -> deliver(snap, e, callback));
    }

    /** US-09 : annulation par l'agriculteur (libère la place du créneau). */
    public void annuler(@NonNull RendezVous rdv, @NonNull Callback<Void> callback) {
        Notification notif = new Notification(Notification.DESTINATAIRE_GESTIONNAIRES,
                "Rendez-vous annulé",
                rdv.getNomAffichage() + " a annulé son créneau " + rdv.getCreneau() + " du "
                        + DateUtils.jourLong(rdv.getDate()) + ". La place est de nouveau disponible.",
                Notification.TYPE_RDV_ANNULE);
        changerStatutEtLiberer(rdv, StatutRendezVous.ANNULE, null, notif, callback);
    }

    // ---------------------------------------------------------------- Gestionnaire

    /** US-10 : rendez-vous d'un jour donné, en temps réel. */
    public ListenerRegistration ecouterParJour(@NonNull String jour,
                                               @NonNull Callback<List<RendezVous>> callback) {
        return db.collection(FirestoreCollections.RENDEZ_VOUS)
                .whereEqualTo("jour", jour)
                .addSnapshotListener((snap, e) -> deliver(snap, e, callback));
    }

    /** Rendez-vous d'un jour (lecture unique). */
    public void chargerParJour(@NonNull String jour, @NonNull Callback<List<RendezVous>> callback) {
        db.collection(FirestoreCollections.RENDEZ_VOUS)
                .whereEqualTo("jour", jour)
                .get()
                .addOnSuccessListener(snap -> deliver(snap, null, callback))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** Demandes en attente de confirmation (tous jours confondus). */
    public ListenerRegistration ecouterEnAttente(@NonNull Callback<List<RendezVous>> callback) {
        return db.collection(FirestoreCollections.RENDEZ_VOUS)
                .whereEqualTo("statut", StatutRendezVous.EN_ATTENTE_CONFIRMATION.name())
                .addSnapshotListener((snap, e) -> deliver(snap, e, callback));
    }

    /** US-10 / US-11 : confirmation + notification de l'agriculteur. */
    public void confirmer(@NonNull RendezVous rdv, @NonNull Callback<Void> callback) {
        DocumentReference rdvRef = db.collection(FirestoreCollections.RENDEZ_VOUS).document(rdv.getId());
        Map<String, Object> update = new HashMap<>();
        update.put("statut", StatutRendezVous.CONFIRME.name());
        update.put("dateTraitement", new Date());

        Notification notif = new Notification(rdv.getAgriculteurId(),
                "Rendez-vous confirmé",
                "Votre créneau du " + DateUtils.jourLong(rdv.getDate()) + " (" + rdv.getCreneau()
                        + ") est confirmé. Présentez-vous au quai de réception avec vos olives.",
                Notification.TYPE_RDV_CONFIRME);
        DocumentReference notifRef = db.collection(FirestoreCollections.NOTIFICATIONS).document();

        WriteBatch batch = db.batch();
        batch.update(rdvRef, update);
        batch.set(notifRef, notif);
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** US-10 / US-11 : refus avec motif + notification de l'agriculteur. */
    public void refuser(@NonNull RendezVous rdv, @NonNull String motif, @NonNull Callback<Void> callback) {
        Notification notif = new Notification(rdv.getAgriculteurId(),
                "Rendez-vous refusé",
                "Votre demande du " + DateUtils.jourLong(rdv.getDate()) + " (" + rdv.getCreneau()
                        + ") a été refusée. Motif : " + motif,
                Notification.TYPE_RDV_REFUSE);
        changerStatutEtLiberer(rdv, StatutRendezVous.REFUSE, motif, notif, callback);
    }

    // ---------------------------------------------------------------- Interne

    private void changerStatutEtLiberer(RendezVous rdv, StatutRendezVous nouveau, @Nullable String motif,
                                        @Nullable Notification notif, Callback<Void> callback) {
        int index = Creneaux.indexOf(rdv.getCreneau());
        DocumentReference rdvRef = db.collection(FirestoreCollections.RENDEZ_VOUS).document(rdv.getId());
        DocumentReference planningRef = db.collection(FirestoreCollections.PLANNING).document(rdv.getJour());
        DocumentReference notifRef = db.collection(FirestoreCollections.NOTIFICATIONS).document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot actuel = transaction.get(rdvRef);
                    DocumentSnapshot planning = transaction.get(planningRef);

                    String statut = actuel.getString("statut");
                    boolean actif = StatutRendezVous.EN_ATTENTE_CONFIRMATION.name().equals(statut)
                            || StatutRendezVous.CONFIRME.name().equals(statut);
                    if (!actif) {
                        throw new IllegalStateException("Ce rendez-vous a déjà été traité.");
                    }
                    if (index >= 0) {
                        Long v = planning.exists() ? planning.getLong(Creneaux.cle(index)) : null;
                        long count = v == null ? 0 : v;
                        Map<String, Object> planningUpdate = new HashMap<>();
                        planningUpdate.put(Creneaux.cle(index), Math.max(0, count - 1));
                        transaction.set(planningRef, planningUpdate, SetOptions.merge());
                    }
                    Map<String, Object> update = new HashMap<>();
                    update.put("statut", nouveau.name());
                    update.put("dateTraitement", new Date());
                    if (motif != null) update.put("motifRefus", motif);
                    transaction.update(rdvRef, update);
                    if (notif != null) transaction.set(notifRef, notif);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    private static void deliver(@Nullable QuerySnapshot snap, @Nullable Exception e,
                                Callback<List<RendezVous>> callback) {
        if (e != null) {
            callback.onError(ErrorMessages.from(e));
            return;
        }
        List<RendezVous> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                RendezVous rdv = doc.toObject(RendezVous.class);
                if (rdv != null) {
                    rdv.setId(doc.getId());
                    list.add(rdv);
                }
            }
        }
        // Tri chronologique (évite un index composite Firestore)
        Collections.sort(list, (a, b) -> {
            if (a.getDate() == null || b.getDate() == null) return 0;
            return a.getDate().compareTo(b.getDate());
        });
        callback.onSuccess(list);
    }
}
