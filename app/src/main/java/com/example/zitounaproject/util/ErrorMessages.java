package com.example.zitounaproject.util;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/** Traduit les exceptions Firebase en messages compréhensibles (français). */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static String from(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Mot de passe trop faible (6 caractères minimum).";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Un compte existe déjà avec cette adresse e-mail.";
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Aucun compte actif ne correspond à cette adresse e-mail.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Adresse e-mail ou mot de passe incorrect.";
        }
        if (e instanceof FirebaseNetworkException) {
            return "Pas de connexion internet. Réessayez.";
        }
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Accès refusé (règles de sécurité Firestore).";
            }
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Service indisponible, vérifiez votre connexion.";
            }
        }
        return e != null && e.getMessage() != null ? e.getMessage() : "Une erreur est survenue.";
    }
}
