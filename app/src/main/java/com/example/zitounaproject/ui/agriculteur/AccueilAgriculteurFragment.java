package com.example.zitounaproject.ui.agriculteur;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentAccueilAgriculteurBinding;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.TabNavigator;

/** Tableau de bord de l'agriculteur (maquette 3). Le suivi des lots arrive au sprint 3. */
public class AccueilAgriculteurFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentAccueilAgriculteurBinding binding =
                FragmentAccueilAgriculteurBinding.inflate(inflater, container, false);

        Utilisateur utilisateur = UserSession.get();
        String nom = utilisateur != null ? utilisateur.getNomAffichage() : "";
        binding.txtHello.setText(getString(R.string.hello, nom));
        binding.txtAvatar.setText(utilisateur != null ? utilisateur.getInitiales() : "?");

        View.OnClickListener openRdv = v -> {
            if (getActivity() instanceof TabNavigator) {
                ((TabNavigator) getActivity()).openTab(R.id.nav_rdv);
            }
        };
        binding.btnDemanderCreneau.setOnClickListener(openRdv);
        binding.txtAvatar.setOnClickListener(v -> {
            if (getActivity() instanceof TabNavigator) {
                ((TabNavigator) getActivity()).openTab(R.id.nav_profil);
            }
        });
        return binding.getRoot();
    }
}
