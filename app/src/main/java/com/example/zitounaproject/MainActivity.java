package com.example.zitounaproject;

import android.content.Intent;
import android.os.Bundle;

import com.example.zitounaproject.data.AuthRepository;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.auth.OnboardingActivity;
import com.example.zitounaproject.ui.common.BaseActivity;

/**
 * Point d'entrée : vérifie la session Firebase et redirige
 * vers l'accueil (non connecté) ou vers l'espace du rôle (connecté).
 */
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyEdgeToEdge(findViewById(R.id.main));

        AuthRepository repository = new AuthRepository();
        if (!repository.isLoggedIn()) {
            openOnboarding();
            return;
        }
        repository.loadCurrentUser(new Callback<Utilisateur>() {
            @Override
            public void onSuccess(Utilisateur utilisateur) {
                openHomeFor(utilisateur);
            }

            @Override
            public void onError(String message) {
                repository.logout();
                toast(message);
                openOnboarding();
            }
        });
    }

    private void openOnboarding() {
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }
}
