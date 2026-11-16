package com.example.zitounaproject.ui.gestionnaire;

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
import com.example.zitounaproject.databinding.DialogMotifRefusBinding;
import com.example.zitounaproject.databinding.FragmentGestionRdvBinding;
import com.example.zitounaproject.model.Creneaux;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.StatutRendezVous;
import com.example.zitounaproject.ui.common.RendezVousAdapter;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** US-10 : le gestionnaire consulte les demandes par jour et les confirme ou refuse (BF-20). Maquette 8. */
public class GestionRdvFragment extends Fragment implements RendezVousAdapter.Listener {

    private FragmentGestionRdvBinding binding;
    private final RendezVousRepository repository = new RendezVousRepository();
    private RendezVousAdapter adapter;
    private final List<RendezVous> rdvDuJour = new ArrayList<>();
    private Date jour = DateUtils.today();
    @Nullable private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGestionRdvBinding.inflate(inflater, container, false);
        adapter = new RendezVousAdapter(RendezVousAdapter.Mode.GESTIONNAIRE, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.recycler.setNestedScrollingEnabled(false);

        binding.btnJourPrecedent.setOnClickListener(v -> changerJour(DateUtils.addDays(jour, -1)));
        binding.btnJourSuivant.setOnClickListener(v -> changerJour(DateUtils.addDays(jour, 1)));
        binding.btnAujourdhui.setOnClickListener(v -> changerJour(DateUtils.today()));
        binding.txtJour.setOnClickListener(v -> ouvrirCalendrier());
        binding.chipGroupStatut.setOnCheckedStateChangeListener((g, ids) -> afficher());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ecouter();
    }

    @Override
    public void onStop() {
        super.onStop();
        arreterEcoute();
    }

    private void changerJour(Date nouveau) {
        jour = DateUtils.startOfDay(nouveau);
        ecouter();
    }

    private void ouvrirCalendrier() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.choisir_date)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> changerJour(DateUtils.fromPickerSelection(selection)));
        picker.show(getChildFragmentManager(), "date");
    }

    private void ecouter() {
        arreterEcoute();
        binding.txtJour.setText(DateUtils.relatif(jour) + " • " + DateUtils.jourCourt(jour));
        binding.progress.setVisibility(View.VISIBLE);
        registration = repository.ecouterParJour(DateUtils.jourKey(jour), new Callback<List<RendezVous>>() {
            @Override
            public void onSuccess(List<RendezVous> result) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                rdvDuJour.clear();
                rdvDuJour.addAll(result);
                afficher();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
    }

    private void arreterEcoute() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /** Capacité du quai + liste filtrée par statut. */
    private void afficher() {
        double kgConfirmes = 0;
        double kgEnAttente = 0;
        int nbEnAttente = 0;
        for (RendezVous r : rdvDuJour) {
            if (r.getStatut() == StatutRendezVous.CONFIRME) kgConfirmes += r.getQuantiteEstimeeKg();
            if (r.getStatut() == StatutRendezVous.EN_ATTENTE_CONFIRMATION) {
                kgEnAttente += r.getQuantiteEstimeeKg();
                nbEnAttente++;
            }
        }
        double reserve = kgConfirmes + kgEnAttente;
        int pourcent = (int) Math.round(reserve * 100 / Creneaux.CAPACITE_JOUR_KG);
        binding.txtPourcent.setText(pourcent + "%");
        binding.progressCapacite.setProgress(Math.min(100, pourcent));
        binding.txtCapacite.setText(getString(R.string.capacite_reservee,
                Formats.nombre(reserve), Formats.nombre(Creneaux.CAPACITE_JOUR_KG)));
        binding.txtReste.setText(getString(R.string.capacite_reste,
                Formats.kg(Math.max(0, Creneaux.CAPACITE_JOUR_KG - reserve))));
        binding.txtConfirme.setText(getString(R.string.capacite_confirme, Formats.kg(kgConfirmes)));
        binding.txtEnAttente.setText(getString(R.string.capacite_attente, Formats.kg(kgEnAttente)));
        binding.chipEnAttente.setText(getString(R.string.filtre_en_attente_n, nbEnAttente));

        int checked = binding.chipGroupStatut.getCheckedChipId();
        List<RendezVous> filtres = new ArrayList<>();
        for (RendezVous r : rdvDuJour) {
            if (checked == R.id.chipEnAttente && r.getStatut() != StatutRendezVous.EN_ATTENTE_CONFIRMATION) continue;
            if (checked == R.id.chipConfirmes && r.getStatut() != StatutRendezVous.CONFIRME) continue;
            filtres.add(r);
        }
        adapter.submit(filtres);
        binding.layoutEmpty.setVisibility(filtres.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onConfirmer(RendezVous rdv) {
        repository.confirmer(rdv, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                toast(getString(R.string.rdv_confirme_ok, rdv.getNomAffichage()));
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    @Override
    public void onRefuser(RendezVous rdv) {
        DialogMotifRefusBinding dialogBinding = DialogMotifRefusBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.refus_titre)
                .setMessage(getString(R.string.refus_message, rdv.getNomAffichage()))
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.refuser, (d, w) -> {
                    String motif = Validators.text(dialogBinding.inputMotif);
                    if (motif.isEmpty()) {
                        toast(getString(R.string.err_motif));
                        return;
                    }
                    repository.refuser(rdv, motif, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            toast(getString(R.string.rdv_refuse_ok));
                        }

                        @Override
                        public void onError(String message) {
                            toast(message);
                        }
                    });
                })
                .show();
    }

    @Override
    public void onAnnuler(RendezVous rdv) {
        // Action réservée à l'agriculteur
    }

    private void toast(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        arreterEcoute();
        binding = null;
    }
}
