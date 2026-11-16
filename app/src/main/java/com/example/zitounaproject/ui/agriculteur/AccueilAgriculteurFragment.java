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

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentAccueilAgriculteurBinding;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.HeaderActions;
import com.example.zitounaproject.ui.common.TabNavigator;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.List;

/** Tableau de bord de l'agriculteur (maquette 3). Le suivi des lots arrive au sprint 3. */
public class AccueilAgriculteurFragment extends Fragment {

    private FragmentAccueilAgriculteurBinding binding;
    private HeaderActions header;
    @Nullable private ListenerRegistration registration;

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
        binding.txtVoirRdv.setOnClickListener(v -> openTab(R.id.nav_rdv));
        binding.cardProchainRdv.setOnClickListener(v -> openTab(R.id.nav_rdv));
        binding.txtAvatar.setOnClickListener(v -> openTab(R.id.nav_profil));
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
