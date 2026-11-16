package com.example.zitounaproject.ui.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.NotificationRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.ActivityRoleMainBinding;
import com.example.zitounaproject.model.Notification;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.util.SystemNotifier;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Conteneur commun aux deux espaces (agriculteur / gestionnaire) :
 * un fragment par onglet de la barre de navigation du bas.
 */
public abstract class RoleMainActivity extends BaseActivity implements TabNavigator {

    protected ActivityRoleMainBinding binding;
    @Nullable private ListenerRegistration notificationsRegistration;

    /** Android 13+ : autorisation d'afficher des notifications. */
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), accordee -> { });

    @MenuRes
    protected abstract int menuRes();

    @IdRes
    protected abstract int defaultTab();

    @NonNull
    protected abstract Fragment createFragment(@IdRes int menuItemId);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.bottomNav.inflateMenu(menuRes());
        binding.bottomNav.setOnItemSelectedListener(this::onTabSelected);
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(defaultTab());
        }

        SystemNotifier.creerCanal(this);
        demanderPermissionNotifications();
        ecouterNotifications();
    }

    private void demanderPermissionNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** US-24 : écoute active tant que l'espace est ouvert (y compris en arrière-plan). */
    private void ecouterNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        boolean gestionnaire = UserSession.get() != null && UserSession.get().getRole() == Role.GESTIONNAIRE;
        notificationsRegistration = new NotificationRepository().ecouter(uid, gestionnaire,
                new Callback<List<Notification>>() {
                    @Override
                    public void onSuccess(List<Notification> result) {
                        SystemNotifier.afficherNouvelles(getApplicationContext(), uid, result);
                    }

                    @Override
                    public void onError(String message) {
                        // Pas de notification système en cas d'erreur
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsRegistration != null) {
            notificationsRegistration.remove();
            notificationsRegistration = null;
        }
    }

    private boolean onTabSelected(@NonNull MenuItem item) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, createFragment(item.getItemId()))
                .commit();
        return true;
    }

    @Override
    public void openTab(@IdRes int menuItemId) {
        binding.bottomNav.setSelectedItemId(menuItemId);
    }
}
