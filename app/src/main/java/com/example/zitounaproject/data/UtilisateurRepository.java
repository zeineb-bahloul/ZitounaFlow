package com.example.zitounaproject.data;

import androidx.annotation.NonNull;

import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.util.ErrorMessages;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lecture des comptes utilisateurs par le gestionnaire. */
public class UtilisateurRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Liste des agriculteurs inscrits, triés par nom d'exploitation. */
    public void listerAgriculteurs(@NonNull Callback<List<Utilisateur>> callback) {
        db.collection(FirestoreCollections.UTILISATEURS)
                .whereEqualTo("role", Role.AGRICULTEUR.name())
                .get()
                .addOnSuccessListener(snap -> {
                    List<Utilisateur> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Utilisateur u = AuthRepository.fromDocument(doc);
                        if (u != null) list.add(u);
                    }
                    Collections.sort(list, (a, b) -> a.getNomAffichage().compareToIgnoreCase(b.getNomAffichage()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }
}
