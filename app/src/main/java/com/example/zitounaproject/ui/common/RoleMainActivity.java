package com.example.zitounaproject.ui.common;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.databinding.ActivityRoleMainBinding;

/**
 * Conteneur commun aux deux espaces (agriculteur / gestionnaire) :
 * un fragment par onglet de la barre de navigation du bas.
 */
public abstract class RoleMainActivity extends BaseActivity implements TabNavigator {

    protected ActivityRoleMainBinding binding;

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
