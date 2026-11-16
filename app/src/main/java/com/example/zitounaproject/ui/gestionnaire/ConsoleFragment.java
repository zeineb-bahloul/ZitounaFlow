package com.example.zitounaproject.ui.gestionnaire;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentConsoleBinding;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.HeaderActions;
import com.example.zitounaproject.ui.common.TabNavigator;
import com.example.zitounaproject.util.DateUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.List;

/** Console du gestionnaire (maquette 7). Les indicateurs de production arrivent au sprint 5. */
public class ConsoleFragment extends Fragment {

    private FragmentConsoleBinding binding;
    private HeaderActions header;
    @Nullable private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConsoleBinding.inflate(inflater, container, false);

        Utilisateur utilisateur = UserSession.get();
        binding.txtAvatar.setText(utilisateur != null ? utilisateur.getInitiales() : "?");
        binding.txtHello.setText(getString(R.string.hello, utilisateur != null ? utilisateur.getNomAffichage() : ""));
        binding.txtDate.setText(DateUtils.jourLong(new Date()));

        header = new HeaderActions(this, binding.btnNotifications, binding.txtBadge, binding.btnLogout);
        binding.btnGererRdv.setOnClickListener(v -> openTab(R.id.nav_rdv));
        binding.btnPlanning.setOnClickListener(v -> openTab(R.id.nav_rdv));
        binding.btnEnregistrerLot.setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), EnregistrerLotActivity.class)));
        binding.txtAvatar.setOnClickListener(v -> openTab(R.id.nav_profil));
        binding.btnHistorique.setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), HistoriqueGlobalActivity.class)));
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        header.start();
        // Bannière « N demandes de pressage à valider »
        registration = new RendezVousRepository().ecouterEnAttente(new Callback<List<RendezVous>>() {
            @Override
            public void onSuccess(List<RendezVous> result) {
                if (binding == null) return;
                int n = result.size();
                binding.txtDemandes.setText(n == 0
                        ? getString(R.string.aucune_demande)
                        : getResources().getQuantityString(R.plurals.demandes_a_valider, n, n));
            }

            @Override
            public void onError(String message) {
                if (binding != null) binding.txtDemandes.setText(message);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        header.stop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void openTab(int id) {
        if (getActivity() instanceof TabNavigator) {
            ((TabNavigator) getActivity()).openTab(id);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
