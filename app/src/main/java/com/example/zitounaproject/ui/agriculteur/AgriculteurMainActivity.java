package com.example.zitounaproject.ui.agriculteur;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.ui.common.ProfilFragment;
import com.example.zitounaproject.ui.common.RoleMainActivity;

/** Espace agriculteur : Accueil / RDV / Lots / Profil. */
public class AgriculteurMainActivity extends RoleMainActivity {

    @Override
    protected int menuRes() {
        return R.menu.menu_agriculteur;
    }

    @Override
    protected int defaultTab() {
        return R.id.nav_accueil;
    }

    @NonNull
    @Override
    protected Fragment createFragment(int menuItemId) {
        if (menuItemId == R.id.nav_rdv) {
            return new MesRendezVousFragment();
        } else if (menuItemId == R.id.nav_lots) {
            return new MesLotsFragment();
        } else if (menuItemId == R.id.nav_profil) {
            return new ProfilFragment();
        }
        return new AccueilAgriculteurFragment();
    }
}
