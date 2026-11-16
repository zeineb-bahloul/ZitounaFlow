package com.example.zitounaproject.ui.common;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.NotificationRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Boutons de l'en-tête des tableaux de bord : cloche des notifications (avec badge
 * du nombre de non lues) et déconnexion.
 */
public class HeaderActions {

    private final Fragment fragment;
    private final TextView badge;
    @Nullable private ListenerRegistration registration;

    public HeaderActions(Fragment fragment, ImageButton btnNotifications, TextView badge, ImageButton btnLogout) {
        this.fragment = fragment;
        this.badge = badge;
        btnNotifications.setOnClickListener(v ->
                fragment.startActivity(new Intent(fragment.requireContext(), NotificationsActivity.class)));
        btnLogout.setOnClickListener(v -> SessionActions.confirmerDeconnexion(fragment.requireActivity()));
        badge.setVisibility(View.GONE);
    }

    /** À appeler dans onStart(). */
    public void start() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        registration = new NotificationRepository().ecouter(uid, estGestionnaire(), new Callback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> result) {
                if (!fragment.isAdded()) return;
                int nonLues = 0;
                for (Notification n : result) if (!n.isLue()) nonLues++;
                badge.setVisibility(nonLues > 0 ? View.VISIBLE : View.GONE);
                badge.setText(nonLues > 9 ? "9+" : String.valueOf(nonLues));
            }

            @Override
            public void onError(String message) {
                // Le badge reste masqué en cas d'erreur
            }
        });
    }

    /** À appeler dans onStop(). */
    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private static boolean estGestionnaire() {
        return UserSession.get() != null && UserSession.get().getRole() == Role.GESTIONNAIRE;
    }
}
