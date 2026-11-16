package com.example.zitounaproject.ui.agriculteur;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentAccueilAgriculteurBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.HeaderActions;
import com.example.zitounaproject.ui.common.LotAdapter;
import com.example.zitounaproject.ui.common.LotDetailActivity;
import com.example.zitounaproject.ui.common.TabNavigator;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Tableau de bord de l'agriculteur (maquette 3) : suivi en direct, apports du jour, prochain rendez-vous. */
public class AccueilAgriculteurFragment extends Fragment {

    private FragmentAccueilAgriculteurBinding binding;
    private HeaderActions header;
    @Nullable private ListenerRegistration registration;
    @Nullable private ListenerRegistration lotsRegistration;
    private LotAdapter apportsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccueilAgriculteurBinding.inflate(inflater, container, false);

        Utilisateur utilisateur = UserSession.get();
        binding.txtHello.setText(getString(R.string.hello, utilisateur != null ? utilisateur.getNomAffichage() : ""));
        binding.txtAvatar.setText(utilisateur != null ? utilisateur.getInitiales() : "?");
        binding.txtDateDuJour.setText(DateUtils.jourLong(new Date()));

        header = new HeaderActions(this, binding.btnNotifications, binding.txtBadge, binding.btnLogout);
        binding.btnDemanderCreneau.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DemandeRdvActivity.class)));
        binding.txtVoirRdv.setOnClickListener(v -> openTab(R.id.nav_lots));
        binding.cardProchainRdv.setOnClickListener(v -> openTab(R.id.nav_rdv));
        binding.txtAvatar.setOnClickListener(v -> openTab(R.id.nav_profil));
        binding.cardSuivi.setOnClickListener(v -> openTab(R.id.nav_lots));

        apportsAdapter = new LotAdapter(LotAdapter.Mode.AGRICULTEUR,
                lot -> startActivity(LotDetailActivity.intent(requireContext(), lot.getId())));
        binding.recyclerApports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerApports.setAdapter(apportsAdapter);
        binding.recyclerApports.setNestedScrollingEnabled(false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        header.start();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        registration = new RendezVousRepository().ecouterMesRendezVous(uid, new Callback<List<RendezVous>>() {
            @Override
            public void onSuccess(List<RendezVous> result) {
                if (binding != null) afficherProchainRdv(result);
            }

            @Override
            public void onError(String message) {
                if (binding != null) binding.cardProchainRdv.setVisibility(View.GONE);
            }
        });
        lotsRegistration = new LotRepository().ecouterMesLots(uid, new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                if (binding != null) afficherLots(result);
            }

            @Override
            public void onError(String message) {
                // Le suivi reste vide
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
        if (lotsRegistration != null) {
            lotsRegistration.remove();
            lotsRegistration = null;
        }
    }

    /** « Suivi en direct » (lot en traitement ou prochain en file) et « Mes apports aujourd'hui ». */
    private void afficherLots(List<Lot> lots) {
        Lot enTraitement = null;
        Lot prochain = null;
        int dansLaFile = 0;
        List<Lot> aujourdhui = new ArrayList<>();
        for (Lot l : lots) {
            if (l.getStatut() == StatutLot.EN_TRAITEMENT && enTraitement == null) enTraitement = l;
            if (l.getStatut() != null && l.getStatut().estDansLaFile()) {
                dansLaFile++;
                if (l.getStatut() != StatutLot.EN_TRAITEMENT
                        && (prochain == null || l.getPositionFile() < prochain.getPositionFile())) {
                    prochain = l;
                }
            }
            if (l.getDateReception() != null && DateUtils.isSameDay(l.getDateReception(), new Date())) {
                aujourdhui.add(l);
            }
        }

        Lot suivi = enTraitement != null ? enTraitement : prochain;
        binding.layoutSuiviVide.setVisibility(suivi == null ? View.VISIBLE : View.GONE);
        binding.layoutSuiviActif.setVisibility(suivi == null ? View.GONE : View.VISIBLE);
        if (suivi != null) {
            if (suivi.getStatut() == StatutLot.EN_TRAITEMENT) {
                int pct = (int) Math.round(suivi.progressionTraitement() * 100);
                binding.progressSuivi.setProgress(pct);
                binding.txtSuiviPourcent.setText(pct + "%");
                binding.txtSuiviEtat.setText(R.string.etape_traitement);
                binding.txtSuiviSub.setText("≈ " + Formats.duree(suivi.minutesRestantesTraitement() / 60.0) + " restant");
            } else {
                int attente = suivi.estimerAttente();
                binding.progressSuivi.setProgress(0);
                binding.txtSuiviPourcent.setText(suivi.getPositionFile() > 0 ? "#" + suivi.getPositionFile() : "—");
                binding.txtSuiviEtat.setText(suivi.getStatut().getLibelle());
                binding.txtSuiviSub.setText(attente <= 0 ? getString(R.string.imminent)
                        : "Attente ≈ " + Formats.duree(attente / 60.0));
            }
            int pos = prochain != null ? prochain.getPositionFile() : 0;
            binding.txtRang.setText(enTraitement != null ? getString(R.string.en_traitement_court)
                    : pos == 1 ? "1re position" : pos > 0 ? pos + "e position" : "—");
            binding.txtLotActif.setText("#" + suivi.getNumero() + " (" + suivi.getVariete() + ")");
        }

        binding.txtApportsSub.setText(aujourdhui.isEmpty() ? getString(R.string.aucun_apport)
                : getResources().getQuantityString(R.plurals.lots_deposes, aujourdhui.size(), aujourdhui.size()));
        apportsAdapter.submit(aujourdhui, null);
    }

    /** Affiche le prochain rendez-vous actif (en attente ou confirmé). La liste est triée par date. */
    private void afficherProchainRdv(List<RendezVous> list) {
        Date maintenant = new Date();
        RendezVous prochain = null;
        for (RendezVous r : list) {
            if (r.isActif() && r.getDate() != null && r.getDate().after(maintenant)) {
                prochain = r;
                break;
            }
        }
        if (prochain == null) {
            binding.cardProchainRdv.setVisibility(View.GONE);
            return;
        }
        binding.cardProchainRdv.setVisibility(View.VISIBLE);
        binding.txtProchainJour.setText(DateUtils.relatif(prochain.getDate()) + " • " + prochain.getCreneau());
        binding.txtProchainDetails.setText(prochain.getVariete() + " • " + Formats.kg(prochain.getQuantiteEstimeeKg()));
        binding.txtProchainStatut.setText(prochain.getStatut().getLibelle());
        binding.txtProchainStatut.setTextColor(ContextCompat.getColor(requireContext(), prochain.getStatut().getCouleurTexte()));
        ViewCompat.setBackgroundTintList(binding.txtProchainStatut, ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), prochain.getStatut().getCouleurFond())));
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
