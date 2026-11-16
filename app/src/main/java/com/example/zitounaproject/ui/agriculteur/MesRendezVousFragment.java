package com.example.zitounaproject.ui.agriculteur;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.databinding.FragmentMesRdvBinding;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.StatutRendezVous;
import com.example.zitounaproject.ui.common.RendezVousAdapter;
import com.example.zitounaproject.util.DateUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** US-09 : l'agriculteur consulte l'état de ses rendez-vous et annule une demande (BF-11). */
public class MesRendezVousFragment extends Fragment implements RendezVousAdapter.Listener {

    private FragmentMesRdvBinding binding;
    private final RendezVousRepository repository = new RendezVousRepository();
    private RendezVousAdapter adapter;
    private final List<RendezVous> tous = new ArrayList<>();
    @Nullable private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMesRdvBinding.inflate(inflater, container, false);
        adapter = new RendezVousAdapter(RendezVousAdapter.Mode.AGRICULTEUR, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.recycler.setNestedScrollingEnabled(false);

        binding.btnNouveau.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DemandeRdvActivity.class)));
        binding.chipGroupFiltre.setOnCheckedStateChangeListener((group, ids) -> appliquerFiltre());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        binding.progress.setVisibility(View.VISIBLE);
        registration = repository.ecouterMesRendezVous(uid, new Callback<List<RendezVous>>() {
            @Override
            public void onSuccess(List<RendezVous> result) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                tous.clear();
                tous.addAll(result);
                appliquerFiltre();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void appliquerFiltre() {
        int checked = binding.chipGroupFiltre.getCheckedChipId();
        List<RendezVous> filtres = new ArrayList<>();
        Date debutJour = DateUtils.today();
        for (RendezVous r : tous) {
            StatutRendezVous s = r.getStatut();
            boolean aVenir = r.isActif() && r.getDate() != null && !r.getDate().before(debutJour);
            if (checked == R.id.chipAVenir && !aVenir) continue;
            if (checked == R.id.chipEnAttente && s != StatutRendezVous.EN_ATTENTE_CONFIRMATION) continue;
            if (checked == R.id.chipHistorique && aVenir) continue;
            filtres.add(r);
        }
        // À venir : du plus proche au plus lointain ; historique : du plus récent au plus ancien
        if (checked == R.id.chipHistorique) Collections.reverse(filtres);
        adapter.submit(filtres);

        int aVenirCount = 0;
        int enAttente = 0;
        for (RendezVous r : tous) {
            if (r.isActif() && r.getDate() != null && !r.getDate().before(debutJour)) aVenirCount++;
            if (r.getStatut() == StatutRendezVous.EN_ATTENTE_CONFIRMATION) enAttente++;
        }
        binding.txtResume.setText(getString(R.string.rdv_resume, aVenirCount, enAttente));
        binding.layoutEmpty.setVisibility(filtres.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAnnuler(RendezVous rdv) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rdv_annuler_titre)
                .setMessage(getString(R.string.rdv_annuler_message, DateUtils.jourLong(rdv.getDate()), rdv.getCreneau()))
                .setNegativeButton(R.string.non, null)
                .setPositiveButton(R.string.rdv_annuler, (d, w) -> repository.annuler(rdv, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        toast(getString(R.string.rdv_annule_ok));
                    }

                    @Override
                    public void onError(String message) {
                        toast(message);
                    }
                }))
                .show();
    }

    @Override
    public void onConfirmer(RendezVous rdv) {
        // Action réservée au gestionnaire
    }

    @Override
    public void onRefuser(RendezVous rdv) {
        // Action réservée au gestionnaire
    }

    private void toast(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
