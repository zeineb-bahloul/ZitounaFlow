package com.example.zitounaproject.ui.common;

import androidx.annotation.IdRes;

/** Permet à un fragment d'ouvrir un autre onglet de la barre de navigation. */
public interface TabNavigator {
    void openTab(@IdRes int menuItemId);
}
