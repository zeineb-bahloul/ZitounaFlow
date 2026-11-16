package com.example.zitounaproject.ui.common;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.agriculteur.AgriculteurMainActivity;
import com.example.zitounaproject.ui.gestionnaire.GestionnaireMainActivity;

/** Activité de base : affichage bord à bord, messages, redirection selon le rôle. */
public abstract class BaseActivity extends AppCompatActivity {

    /** Active l'affichage bord à bord et ajoute les marges des barres système à la vue racine. */
    protected void applyEdgeToEdge(View root) {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /** US-07 : ouvre l'espace correspondant au rôle et vide la pile d'écrans. */
    protected void openHomeFor(Utilisateur utilisateur) {
        Class<?> target = utilisateur.getRole() == Role.GESTIONNAIRE
                ? GestionnaireMainActivity.class
                : AgriculteurMainActivity.class;
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
