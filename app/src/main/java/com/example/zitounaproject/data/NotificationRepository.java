package com.example.zitounaproject.data;

import androidx.annotation.NonNull;

import com.example.zitounaproject.model.Notification;
import com.example.zitounaproject.util.ErrorMessages;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notifications dans l'application (BF-05).
 * Un gestionnaire reçoit ses notifications personnelles et celles adressées à tous les
 * gestionnaires (destinataire « GESTIONNAIRES » : nouvelles demandes, annulations).
 */
public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Notifications de l'utilisateur, les plus récentes en premier, en temps réel. */
    public ListenerRegistration ecouter(@NonNull String uid, boolean gestionnaire,
                                        @NonNull Callback<List<Notification>> callback) {
        final List<Notification> personnelles = new ArrayList<>();
        final List<Notification> collectives = new ArrayList<>();

        ListenerRegistration r1 = ecouterDestinataire(uid, new Callback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> result) {
                personnelles.clear();
                personnelles.addAll(result);
                callback.onSuccess(fusionner(personnelles, collectives));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
        if (!gestionnaire) return r1;

        ListenerRegistration r2 = ecouterDestinataire(Notification.DESTINATAIRE_GESTIONNAIRES,
                new Callback<List<Notification>>() {
                    @Override
                    public void onSuccess(List<Notification> result) {
                        collectives.clear();
                        collectives.addAll(result);
                        callback.onSuccess(fusionner(personnelles, collectives));
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
        return () -> {
            r1.remove();
            r2.remove();
        };
    }

    private ListenerRegistration ecouterDestinataire(String destinataireId, Callback<List<Notification>> callback) {
        return db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("destinataireId", destinataireId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onError(ErrorMessages.from(e));
                        return;
                    }
                    List<Notification> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                n.setId(doc.getId());
                                list.add(n);
                            }
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    private static List<Notification> fusionner(List<Notification> a, List<Notification> b) {
        List<Notification> all = new ArrayList<>(a);
        all.addAll(b);
        Collections.sort(all, (x, y) -> {
            if (x.getDateEnvoi() == null || y.getDateEnvoi() == null) return 0;
            return y.getDateEnvoi().compareTo(x.getDateEnvoi());
        });
        return all;
    }

    public void marquerCommeLues(@NonNull List<Notification> notifications) {
        WriteBatch batch = db.batch();
        int count = 0;
        for (Notification n : notifications) {
            if (!n.isLue() && n.getId() != null) {
                batch.update(db.collection(FirestoreCollections.NOTIFICATIONS).document(n.getId()), "lue", true);
                count++;
            }
        }
        if (count > 0) batch.commit();
    }
}
