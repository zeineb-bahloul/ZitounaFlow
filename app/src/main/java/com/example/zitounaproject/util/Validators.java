package com.example.zitounaproject.util;

import android.util.Patterns;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

/** Validation des formulaires (inscription, connexion, profil). */
public final class Validators {

    private Validators() {
    }

    public static String text(TextInputLayout layout) {
        if (layout.getEditText() == null || layout.getEditText().getText() == null) {
            return "";
        }
        return layout.getEditText().getText().toString().trim();
    }

    public static boolean isEmail(@Nullable String value) {
        return value != null && Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }

    /** Numéro tunisien : 8 chiffres (espaces ignorés). */
    public static boolean isPhone(@Nullable String value) {
        return value != null && value.replace(" ", "").matches("\\d{8}");
    }

    public static boolean isPassword(@Nullable String value) {
        return value != null && value.length() >= 6;
    }

    /** Affiche l'erreur si la condition est fausse ; renvoie la condition. */
    public static boolean check(TextInputLayout layout, boolean condition, String error) {
        layout.setError(condition ? null : error);
        return condition;
    }
}
