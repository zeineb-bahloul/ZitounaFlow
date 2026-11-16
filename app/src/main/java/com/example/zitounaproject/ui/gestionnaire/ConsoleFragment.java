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
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentConsoleBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.Statistiques;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.HeaderActions;
import com.example.zitounaproject.ui.common.TabNavigator;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** US-23 : console du gestionnaire (maquette 7) avec les indicateurs du jour en temps réel. */
public class ConsoleFragment extends Fragment {

    private FragmentConsoleBinding binding;
    private HeaderActions header;
    @Nullable private ListenerRegistration registration;
    @Nullable private ListenerRegistration recusRegistration;
    @Nullable private ListenerRegistration traitesRegistration;
    private final List<Lot> recusAujourdhui = new ArrayList<>();
    private final List<Lot> traitesAujourdhui = new ArrayList<>();

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

        // Indicateurs du jour : lots reçus depuis minuit et lots pressés depuis minuit
        LotRepository lotRepository = new LotRepository();
        recusRegistration = lotRepository.ecouterDepuis("dateReception", DateUtils.today(), new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                recusAujourdhui.clear();
                recusAujourdhui.addAll(result);
                afficherIndicateurs();
            }

            @Override
            public void onError(String message) {
                // Les indicateurs restent à « — »
            }
        });
        traitesRegistration = lotRepository.ecouterDepuis("dateFinTraitement", DateUtils.today(), new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                traitesAujourdhui.clear();
                traitesAujourdhui.addAll(result);
                afficherIndicateurs();
            }

            @Override
            public void onError(String message) {
                // Les indicateurs restent à « — »
            }
        });
    }

    private void afficherIndicateurs() {
        if (binding == null) return;
        Date demain = DateUtils.addDays(DateUtils.today(), 1);
        Statistiques recus = Statistiques.calculer(recusAujourdhui, DateUtils.today(), demain, Statistiques.Granularite.HEURE);
        double kgTraites = 0;
        double huile = 0;
        for (Lot l : traitesAujourdhui) {
            kgTraites += l.getPoidsKg();
            if (l.getQuantiteHuileL() != null) huile += l.getQuantiteHuileL();
        }
        binding.txtKpiLots.setText(String.valueOf(recus.nbLots));
        binding.txtKpiLotsSub.setText(getString(R.string.kpi_kg_recus, Formats.kg(recus.poidsTotalKg)));
        binding.txtKpiOlives.setText(String.format(Locale.FRENCH, "%.1f T", kgTraites / 1000));
        binding.txtKpiOlivesSub.setText(getString(R.string.kpi_presses, traitesAujourdhui.size()));
        binding.txtKpiHuile.setText(Formats.nombre(huile) + " L");
        binding.txtKpiHuileSub.setText(R.string.kpi_aujourdhui);
        binding.txtKpiRendement.setText(kgTraites > 0 ? String.format(Locale.FRENCH, "%.1f %%", huile / kgTraites * 100) : "—");
        binding.txtKpiRendementSub.setText(R.string.kpi_rendement_jour);

        List<String> labels = new ArrayList<>();
        List<Double> valeurs = new ArrayList<>();
        for (Statistiques.Barre b : recus.barres) {
            labels.add(b.label);
            valeurs.add(b.poidsKg);
        }
        binding.chartActivite.setDonnees(labels, valeurs, "kg");
    }

    @Override
    public void onStop() {
        super.onStop();
        header.stop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        if (recusRegistration != null) {
            recusRegistration.remove();
            recusRegistration = null;
        }
        if (traitesRegistration != null) {
            traitesRegistration.remove();
            traitesRegistration = null;
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
