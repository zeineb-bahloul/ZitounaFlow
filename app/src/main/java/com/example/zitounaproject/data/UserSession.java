package com.example.zitounaproject.data;

import androidx.annotation.Nullable;

import com.example.zitounaproject.model.Utilisateur;

/** Garde en mémoire le profil de l'utilisateur connecté. */
public final class UserSession {

    private static Utilisateur current;

    private UserSession() {
    }

    @Nullable
    public static Utilisateur get() {
        return current;
    }

    public static void set(@Nullable Utilisateur utilisateur) {
        current = utilisateur;
    }

    public static void clear() {
        current = null;
    }
}
