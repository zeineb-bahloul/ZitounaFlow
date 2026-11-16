package com.example.zitounaproject.ui.gestionnaire;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentConsoleBinding;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.TabNavigator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Console du gestionnaire (maquette 7). Les indicateurs seront alimentés au sprint 5. */
public class ConsoleFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentConsoleBinding binding = FragmentConsoleBinding.inflate(inflater, container, false);

        Utilisateur utilisateur = UserSession.get();
        binding.txtAvatar.setText(utilisateur != null ? utilisateur.getInitiales() : "?");
        binding.txtHello.setText(getString(R.string.hello,
                utilisateur != null ? utilisateur.getNomAffichage() : ""));

        String date = new SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(new Date());
        binding.txtDate.setText(date.substring(0, 1).toUpperCase(Locale.FRENCH) + date.substring(1));

        binding.btnGererRdv.setOnClickListener(v -> openTab(R.id.nav_rdv));
        binding.btnEnregistrerLot.setOnClickListener(v -> openTab(R.id.nav_lots));
        binding.txtAvatar.setOnClickListener(v -> openTab(R.id.nav_profil));
        return binding.getRoot();
    }

    private void openTab(int id) {
        if (getActivity() instanceof TabNavigator) {
            ((TabNavigator) getActivity()).openTab(id);
        }
    }
}
