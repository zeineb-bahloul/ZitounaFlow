package com.example.zitounaproject.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zitounaproject.model.Agriculteur;
import com.example.zitounaproject.model.Gestionnaire;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.util.ErrorMessages;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/**
 * Accès à Firebase Authentication et au profil Firestore de l'utilisateur
 * (BF-01 à BF-04).
 */
public class AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /** BF-01 : créer un compte Firebase Auth puis le profil Firestore. */
    public void register(@NonNull Utilisateur utilisateur, @NonNull String password,
                         @NonNull Callback<Utilisateur> callback) {
        auth.createUserWithEmailAndPassword(utilisateur.getEmail(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Création du compte impossible.");
                        return;
                    }
                    utilisateur.setId(firebaseUser.getUid());
                    utilisateur.setDateCreation(new Date());
                    db.collection(FirestoreCollections.UTILISATEURS)
                            .document(firebaseUser.getUid())
                            .set(utilisateur)
                            .addOnSuccessListener(unused -> {
                                UserSession.set(utilisateur);
                                callback.onSuccess(utilisateur);
                            })
                            .addOnFailureListener(e -> {
                                // On supprime le compte Auth pour ne pas laisser un compte sans profil
                                firebaseUser.delete();
                                callback.onError(ErrorMessages.from(e));
                            });
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** BF-02 : connexion puis chargement du profil. */
    public void login(@NonNull String email, @NonNull String password,
                      @NonNull Callback<Utilisateur> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> loadCurrentUser(callback))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** Charge le profil Firestore de l'utilisateur connecté. */
    public void loadCurrentUser(@NonNull Callback<Utilisateur> callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("Aucun utilisateur connecté.");
            return;
        }
        db.collection(FirestoreCollections.UTILISATEURS)
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    Utilisateur utilisateur = fromDocument(doc);
                    if (utilisateur == null) {
                        callback.onError("Profil introuvable pour ce compte.");
                    } else {
                        UserSession.set(utilisateur);
                        callback.onSuccess(utilisateur);
                    }
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** BF-03 : envoi de l'e-mail de réinitialisation. */
    public void resetPassword(@NonNull String email, @NonNull Callback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** BF-04 : mise à jour du profil (l'e-mail et le rôle ne changent pas). */
    public void updateProfile(@NonNull Utilisateur utilisateur, @NonNull Callback<Utilisateur> callback) {
        if (utilisateur.getId() == null) {
            callback.onError("Profil invalide.");
            return;
        }
        db.collection(FirestoreCollections.UTILISATEURS)
                .document(utilisateur.getId())
                .set(utilisateur)
                .addOnSuccessListener(unused -> {
                    UserSession.set(utilisateur);
                    callback.onSuccess(utilisateur);
                })
                .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
    }

    /** BF-02 : déconnexion. */
    public void logout() {
        auth.signOut();
        UserSession.clear();
    }

    /** Convertit un document Firestore en Agriculteur ou Gestionnaire selon le champ "role". */
    @Nullable
    public static Utilisateur fromDocument(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }
        String role = doc.getString("role");
        Utilisateur utilisateur = Role.GESTIONNAIRE.name().equals(role)
                ? doc.toObject(Gestionnaire.class)
                : doc.toObject(Agriculteur.class);
        if (utilisateur != null) {
            utilisateur.setId(doc.getId());
        }
        return utilisateur;
    }
}
