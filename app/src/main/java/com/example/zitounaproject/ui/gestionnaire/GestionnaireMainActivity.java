package com.example.zitounaproject.ui.gestionnaire;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.ui.common.PlaceholderFragment;
import com.example.zitounaproject.ui.common.ProfilFragment;
import com.example.zitounaproject.ui.common.RoleMainActivity;

/** Espace gestionnaire : Console / RDV / Lots / Stats / Profil. */
public class GestionnaireMainActivity extends RoleMainActivity {

    @Override
    protected int menuRes() {
        return R.menu.menu_gestionnaire;
    }

    @Override
    protected int defaultTab() {
        return R.id.nav_console;
    }

    @NonNull
    @Override
    protected Fragment createFragment(int menuItemId) {
        if (menuItemId == R.id.nav_rdv) {
            return new GestionRdvFragment();
        } else if (menuItemId == R.id.nav_lots) {
            return PlaceholderFragment.newInstance(getString(R.string.nav_lots), R.drawable.ic_lots, 3);
        } else if (menuItemId == R.id.nav_stats) {
            return PlaceholderFragment.newInstance(getString(R.string.nav_stats), R.drawable.ic_stats, 5);
        } else if (menuItemId == R.id.nav_profil) {
            return new ProfilFragment();
        }
        return new ConsoleFragment();
    }
}
