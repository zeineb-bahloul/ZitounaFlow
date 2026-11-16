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

/** Notifications dans l'application (BF-05). */
public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Notifications de l'utilisateur, les plus récentes en premier, en temps réel. */
    public ListenerRegistration ecouter(@NonNull String uid, @NonNull Callback<List<Notification>> callback) {
        return db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("destinataireId", uid)
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
                    Collections.sort(list, (a, b) -> {
                        if (a.getDateEnvoi() == null || b.getDateEnvoi() == null) return 0;
                        return b.getDateEnvoi().compareTo(a.getDateEnvoi());
                    });
                    callback.onSuccess(list);
                });
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
