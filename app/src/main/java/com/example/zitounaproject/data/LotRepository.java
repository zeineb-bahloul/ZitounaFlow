package com.example.zitounaproject.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zitounaproject.model.FileAttenteCalculator;
import com.example.zitounaproject.model.HistoriqueStatut;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.Notification;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.util.ErrorMessages;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lots d'olives et file d'attente (BF-12, BF-13, BF-21, BF-23, BF-24).
 * Après chaque modification de la file, {@link #recalculerFile} met à jour la position
 * et le temps d'attente estimé de chaque lot, que l'agriculteur lit ensuite.
 */
public class LotRepository {

    /** Statuts des lots présents dans la file. */
    public static final List<String> STATUTS_FILE = Arrays.asList(
            StatutLot.RECEPTIONNE.name(), StatutLot.EN_ATTENTE.name(), StatutLot.EN_TRAITEMENT.name());

    /** Écart minimal (ms) avant de réécrire une estimation, pour limiter les écritures. */
    private static final long TOLERANCE_MS = 2 * 60_000L;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------------------------------------------------------------- Lecture

    /** File d'attente (lots réceptionnés, en attente et en traitement), en temps réel. */
    public ListenerRegistration ecouterFile(@NonNull Callback<List<Lot>> callback) {
        return db.collection(FirestoreCollections.LOTS)
                .whereIn("statut", STATUTS_FILE)
                .addSnapshotListener((snap, e) -> deliver(snap, e, callback));
    }

    /** US-15 : lots d'un agriculteur, en temps réel (plus récents d'abord). */
    public ListenerRegistration ecouterMesLots(@NonNull String agriculteurId, @NonNull Callback<List<Lot>> callback) {
        return db.collection(FirestoreCollections.LOTS)
                .whereEqualTo("agriculteurId", agriculteurId)
                .addSnapshotListener((snap, e) -> deliver(snap, e, callback));
    }

    public ListenerRegistration ecouterLot(@NonNull String lotId, @NonNull Callback<Lot> callback) {
        return db.collection(FirestoreCollections.LOTS).document(lotId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        callback.onError(ErrorMessages.from(e));
                        return;
                    }
                    Lot lot = doc != null && doc.exists() ? doc.toObject(Lot.class) : null;
                    if (lot == null) {
                        callback.onError("Lot introuvable.");
                        return;
                    }
                    lot.setId(doc.getId());
                    callback.onSuccess(lot);
                });
    }

    public ListenerRegistration ecouterHistorique(@NonNull String lotId,
                                                  @NonNull Callback<List<HistoriqueStatut>> callback) {
        return db.collection(FirestoreCollections.LOTS).document(lotId)
                .collection(FirestoreCollections.HISTORIQUE)
                .orderBy("dateChangement", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onError(ErrorMessages.from(e));
                        return;
                    }
                    List<HistoriqueStatut> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            HistoriqueStatut h = doc.toObject(HistoriqueStatut.class);
                            if (h != null) {
                                h.setId(doc.getId());
                                list.add(h);
                            }
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    // ---------------------------------------------------------------- Écriture (gestionnaire)

    /**
     * US-12 : enregistre un lot à l'arrivée. Numéro séquentiel (compteur Firestore),
     * statut « Réceptionné », placé en fin de file, historique + notification de l'agriculteur.
     */
    public void enregistrer(@NonNull Lot lot, @NonNull Callback<Lot> callback) {
        DocumentReference compteurRef = db.collection(FirestoreCollections.COMPTEURS).document("lots");
        DocumentReference lotRef = db.collection(FirestoreCollections.LOTS).document();
        DocumentReference histRef = lotRef.collection(FirestoreCollections.HISTORIQUE).document();
        DocumentReference notifRef = db.collection(FirestoreCollections.NOTIFICATIONS).document();
        String uid = FirebaseAuth.getInstance().getUid();

        lot.setId(lotRef.getId());
        lot.setStatut(StatutLot.RECEPTIONNE);
        lot.setDateReception(new Date());
        lot.setOrdreFile(System.currentTimeMillis());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot compteur = transaction.get(compteurRef);
                    Long dernier = compteur.exists() ? compteur.getLong("dernier") : null;
                    long numero = (dernier == null ? 0 : dernier) + 1;
                    lot.setNumero(String.format(Locale.US, "LOT-%03d", numero));

                    Map<String, Object> compteurUpdate = new HashMap<>();
                    compteurUpdate.put("dernier", numero);
                    transaction.set(compteurRef, compteurUpdate, SetOptions.merge());
                    transaction.set(lotRef, lot);
                    transaction.set(histRef, new HistoriqueStatut(null, StatutLot.RECEPTIONNE, uid));
                    if (lot.getRendezVousId() != null) {
                        transaction.update(db.collection(FirestoreCollections.RENDEZ_VOUS)
                                .document(lot.getRendezVousId()), "lotId", lot.getId());
                    }
                    if (lot.getAgriculteurId() != null) {
                        transaction.set(notifRef, new Notification(lot.getAgriculteurId(),
                                "Lot " + lot.getNumero() + " réceptionné",
                                "Vos olives (" + Formats.kg(lot.getPoidsKg()) + " net, " + lot.getVariete()
                                        + ") ont été pesées et ajoutées à la file d'attente.",
                                Notification.TYPE_LOT));
                    }
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(lot);
                    recalculerFile(null);
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /**
     * US-14 : fait avancer le lot d'une étape. Le cycle est strictement séquentiel ;
     * le passage à « Prêt à récupérer » se fait avec la saisie de l'huile (sprint 4).
     */
    public void avancerStatut(@NonNull Lot lot, @NonNull Callback<StatutLot> callback) {
        StatutLot attendu = lot.getStatut();
        StatutLot suivant = attendu != null ? attendu.suivant() : null;
        if (suivant == null || suivant == StatutLot.PRET_A_RECUPERER) {
            callback.onError("Ce passage se fait en enregistrant la quantité d'huile produite.");
            return;
        }
        DocumentReference lotRef = db.collection(FirestoreCollections.LOTS).document(lot.getId());
        DocumentReference histRef = lotRef.collection(FirestoreCollections.HISTORIQUE).document();
        DocumentReference notifRef = db.collection(FirestoreCollections.NOTIFICATIONS).document();
        String uid = FirebaseAuth.getInstance().getUid();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot actuel = transaction.get(lotRef);
                    if (!attendu.name().equals(actuel.getString("statut"))) {
                        throw new IllegalStateException("Le statut de ce lot a déjà changé.");
                    }
                    Map<String, Object> update = new HashMap<>();
                    update.put("statut", suivant.name());
                    if (suivant == StatutLot.EN_TRAITEMENT) update.put("dateDebutTraitement", new Date());
                    transaction.update(lotRef, update);
                    transaction.set(histRef, new HistoriqueStatut(attendu, suivant, uid));
                    if (lot.getAgriculteurId() != null) {
                        String message = suivant == StatutLot.EN_TRAITEMENT
                                ? "Le traitement de vos olives a commencé (lavage, broyage, malaxage, centrifugation)."
                                : "Votre lot attend son tour dans la file. Suivez votre position dans l'onglet Lots.";
                        transaction.set(notifRef, new Notification(lot.getAgriculteurId(),
                                "Lot " + lot.getNumero() + " : " + suivant.getLibelle().toLowerCase(Locale.FRENCH),
                                message, Notification.TYPE_LOT));
                    }
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(suivant);
                    recalculerFile(null);
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** US-13 : place le lot en tête des lots qui attendent (hors lots en traitement). */
    public void prioriser(@NonNull Lot lot, @NonNull List<Lot> file, @NonNull Callback<Void> callback) {
        double min = Double.MAX_VALUE;
        for (Lot l : file) {
            if (l.getStatut() != StatutLot.EN_TRAITEMENT) min = Math.min(min, l.getOrdreFile());
        }
        if (min == Double.MAX_VALUE) min = lot.getOrdreFile();
        Map<String, Object> update = new HashMap<>();
        update.put("ordreFile", min - 1000);
        update.put("prioritaire", true);
        db.collection(FirestoreCollections.LOTS).document(lot.getId()).update(update)
                .addOnSuccessListener(unused -> recalculerFile(callback))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** US-13 : échange la place de deux lots voisins dans la file. */
    public void echanger(@NonNull Lot a, @NonNull Lot b, @NonNull Callback<Void> callback) {
        WriteBatch batch = db.batch();
        batch.update(db.collection(FirestoreCollections.LOTS).document(a.getId()), "ordreFile", b.getOrdreFile());
        batch.update(db.collection(FirestoreCollections.LOTS).document(b.getId()), "ordreFile", a.getOrdreFile());
        batch.commit()
                .addOnSuccessListener(unused -> recalculerFile(callback))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /**
     * US-16 : recalcule et enregistre la position et l'attente estimée de chaque lot de la file.
     * N'écrit que les lots dont l'estimation a réellement changé.
     */
    public void recalculerFile(@Nullable Callback<Void> callback) {
        db.collection(FirestoreCollections.LOTS)
                .whereIn("statut", STATUTS_FILE)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Lot> lots = toLots(snap);
                    Date maintenant = new Date();
                    Map<String, FileAttenteCalculator.Estimation> estimations =
                            FileAttenteCalculator.calculer(lots, maintenant);
                    WriteBatch batch = db.batch();
                    int ecritures = 0;
                    for (Lot lot : lots) {
                        FileAttenteCalculator.Estimation e = estimations.get(lot.getId());
                        if (e == null) continue;
                        boolean change = lot.getPositionFile() != e.position
                                || !proche(lot.getDebutEstime(), e.debut)
                                || !proche(lot.getFinEstimee(), e.fin);
                        if (!change) continue;
                        Map<String, Object> update = new HashMap<>();
                        update.put("positionFile", e.position);
                        update.put("tempsAttenteEstimeMin", e.attenteMin);
                        update.put("debutEstime", e.debut);
                        update.put("finEstimee", e.fin);
                        update.put("dateEstimation", maintenant);
                        batch.update(db.collection(FirestoreCollections.LOTS).document(lot.getId()), update);
                        ecritures++;
                    }
                    if (ecritures == 0) {
                        if (callback != null) callback.onSuccess(null);
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (callback != null) callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(ErrorMessages.from(e));
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(ErrorMessages.from(e));
                });
    }

    // ---------------------------------------------------------------- Interne

    private static boolean proche(@Nullable Date a, @Nullable Date b) {
        if (a == null || b == null) return a == b;
        return Math.abs(a.getTime() - b.getTime()) < TOLERANCE_MS;
    }

    private static List<Lot> toLots(@Nullable QuerySnapshot snap) {
        List<Lot> list = new ArrayList<>();
        if (snap == null) return list;
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Lot lot = doc.toObject(Lot.class);
            if (lot != null) {
                lot.setId(doc.getId());
                list.add(lot);
            }
        }
        return list;
    }

    private static void deliver(@Nullable QuerySnapshot snap, @Nullable Exception e, Callback<List<Lot>> callback) {
        if (e != null) {
            callback.onError(ErrorMessages.from(e));
            return;
        }
        List<Lot> list = toLots(snap);
        // Plus récents d'abord (l'ordre de la file est calculé à part par FileAttenteCalculator)
        Collections.sort(list, (a, b) -> {
            if (a.getDateReception() == null || b.getDateReception() == null) return 0;
            return b.getDateReception().compareTo(a.getDateReception());
        });
        callback.onSuccess(list);
    }
}
