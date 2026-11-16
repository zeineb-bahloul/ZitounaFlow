package com.example.zitounaproject.ui.common;

import android.app.Activity;
import android.content.Intent;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.AuthRepository;
import com.example.zitounaproject.ui.auth.OnboardingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Actions de session partagées par les écrans (déconnexion). */
public final class SessionActions {

    private SessionActions() {
    }

    /** BF-02 : demande confirmation puis déconnecte et revient à l'écran d'accueil. */
    public static void confirmerDeconnexion(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    new AuthRepository().logout();
                    Intent intent = new Intent(activity, OnboardingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .show();
    }
}
