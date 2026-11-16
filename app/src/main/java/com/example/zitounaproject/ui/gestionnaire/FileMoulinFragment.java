package com.example.zitounaproject.ui.gestionnaire;

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
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.databinding.FragmentFileMoulinBinding;
import com.example.zitounaproject.model.Creneaux;
import com.example.zitounaproject.model.FileAttenteCalculator;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.ui.common.LotActions;
import com.example.zitounaproject.ui.common.LotAdapter;
import com.example.zitounaproject.ui.common.LotDetailActivity;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** US-13 / US-14 : file du moulin — consulter, réordonner, prioriser, faire avancer les lots (BF-23, BF-24). */
public class FileMoulinFragment extends Fragment implements LotAdapter.Listener {

    private FragmentFileMoulinBinding binding;
    private final LotRepository repository = new LotRepository();
    private LotAdapter adapter;
    /** Lots de la file dans l'ordre de passage. */
    private List<Lot> file = new ArrayList<>();
    /** Lots prêts dont l'huile n'a pas encore été récupérée. */
    private final List<Lot> prets = new ArrayList<>();
    private Map<String, FileAttenteCalculator.Estimation> estimations;
    @Nullable private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFileMoulinBinding.inflate(inflater, container, false);
        adapter = new LotAdapter(LotAdapter.Mode.GESTIONNAIRE, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.recycler.setNestedScrollingEnabled(false);

        binding.btnEnregistrerLot.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EnregistrerLotActivity.class)));
        binding.chipGroupFiltre.setOnCheckedStateChangeListener((g, ids) -> afficher());
        binding.btnHistorique.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoriqueGlobalActivity.class)));
        binding.txtCapacite.setText(Formats.nombre(Creneaux.CAPACITE_MACHINE_KG_H) + " kg/h");
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.progress.setVisibility(View.VISIBLE);
        registration = repository.ecouterFile(new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                file = FileAttenteCalculator.ordonner(result);
                estimations = FileAttenteCalculator.calculer(result, new Date());
                prets.clear();
                for (Lot l : result) if (l.isEnAttenteDeRecuperation()) prets.add(l);
                afficher();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
        // Remet à jour les estimations enregistrées (lues par les agriculteurs)
        repository.recalculerFile(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void afficher() {
        int enTraitement = 0;
        int enAttente = 0;
        double kgEnAttente = 0;
        long finFile = System.currentTimeMillis();
        for (Lot l : file) {
            if (l.getStatut() == StatutLot.EN_TRAITEMENT) enTraitement++;
            else {
                enAttente++;
                kgEnAttente += l.getPoidsKg();
            }
            FileAttenteCalculator.Estimation e = estimations != null ? estimations.get(l.getId()) : null;
            if (e != null) finFile = Math.max(finFile, e.fin.getTime());
        }
        double heuresRestantes = (finFile - System.currentTimeMillis()) / 3_600_000.0;
        binding.txtEnTraitement.setText(String.valueOf(enTraitement));
        binding.txtEnAttenteCount.setText(String.valueOf(enAttente));
        binding.txtAttenteTotale.setText(file.isEmpty() ? "—" : "≈ " + Formats.duree(heuresRestantes));
        binding.txtKgAttente.setText(getString(R.string.file_kg_attente, Formats.kg(kgEnAttente)));
        binding.chipTous.setText(getString(R.string.filtre_tous_n, file.size()));
        binding.chipEnTraitement.setText(getString(R.string.filtre_en_traitement_n, enTraitement));
        binding.chipEnAttente.setText(getString(R.string.filtre_en_attente_n, enAttente));
        binding.chipPrets.setText(getString(R.string.filtre_prets_n, prets.size()));

        int checked = binding.chipGroupFiltre.getCheckedChipId();
        List<Lot> filtres = new ArrayList<>();
        if (checked == R.id.chipPrets) {
            filtres.addAll(prets);
            adapter.submit(filtres, estimations);
            binding.layoutEmpty.setVisibility(filtres.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        for (Lot l : file) {
            boolean traitement = l.getStatut() == StatutLot.EN_TRAITEMENT;
            if (checked == R.id.chipEnTraitement && !traitement) continue;
            if (checked == R.id.chipEnAttente && traitement) continue;
            filtres.add(l);
        }
        adapter.submit(filtres, estimations);
        binding.layoutEmpty.setVisibility(filtres.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Lots qui attendent (hors traitement), dans l'ordre. */
    private List<Lot> lotsEnFile() {
        List<Lot> list = new ArrayList<>();
        for (Lot l : file) if (l.getStatut() != StatutLot.EN_TRAITEMENT) list.add(l);
        return list;
    }

    @Override
    public void onOuvrir(Lot lot) {
        startActivity(LotDetailActivity.intent(requireContext(), lot.getId()));
    }

    @Override
    public void onAvancer(Lot lot) {
        LotActions.executer(requireContext(), repository, lot);
    }

    @Override
    public void onMonter(Lot lot) {
        List<Lot> enFile = lotsEnFile();
        int i = enFile.indexOf(lot);
        if (i > 0) repository.echanger(lot, enFile.get(i - 1), erreurSeulement());
    }

    @Override
    public void onDescendre(Lot lot) {
        List<Lot> enFile = lotsEnFile();
        int i = enFile.indexOf(lot);
        if (i >= 0 && i < enFile.size() - 1) repository.echanger(lot, enFile.get(i + 1), erreurSeulement());
    }

    @Override
    public void onPrioriser(Lot lot) {
        repository.prioriser(lot, file, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                toast(getString(R.string.lot_priorise, lot.getNumero()));
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    private Callback<Void> erreurSeulement() {
        return new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        };
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
