package com.example.zitounaproject.ui.gestionnaire;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.databinding.ActivityHistoriqueGlobalBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.LotFilter;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.ui.common.BaseActivity;
import com.example.zitounaproject.ui.common.LotAdapter;
import com.example.zitounaproject.ui.common.LotDetailActivity;
import com.example.zitounaproject.util.CsvExport;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** US-21 : historique global des lots avec recherche et filtres (BF-27). */
public class HistoriqueGlobalActivity extends BaseActivity implements LotAdapter.Listener {

    private ActivityHistoriqueGlobalBinding binding;
    private final LotRepository repository = new LotRepository();
    private LotAdapter adapter;
    private final List<Lot> tous = new ArrayList<>();
    private final List<Lot> affiches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoriqueGlobalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        adapter = new LotAdapter(LotAdapter.Mode.GESTIONNAIRE, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.recycler.setNestedScrollingEnabled(false);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.chipGroupStatut.setOnCheckedStateChangeListener((g, ids) -> afficher());
        binding.chipGroupPeriode.setOnCheckedStateChangeListener((g, ids) -> afficher());
        binding.chipGroupVariete.setOnCheckedStateChangeListener((g, ids) -> afficher());
        if (binding.inputRecherche.getEditText() != null) {
            binding.inputRecherche.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) { afficher(); }
            });
        }
        binding.btnExporter.setOnClickListener(v ->
                CsvExport.partager(this, getString(R.string.registre_titre), affiches));
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.progress.setVisibility(View.VISIBLE);
        repository.chargerHistorique(new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                binding.progress.setVisibility(View.GONE);
                tous.clear();
                tous.addAll(result);
                afficher();
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
    }

    private void afficher() {
        String recherche = Validators.text(binding.inputRecherche);
        int statut = binding.chipGroupStatut.getCheckedChipId();
        int periode = binding.chipGroupPeriode.getCheckedChipId();
        int varieteId = binding.chipGroupVariete.getCheckedChipId();
        String variete = null;
        if (varieteId != View.NO_ID && varieteId != R.id.chipToutesVarietes) {
            Chip chip = findViewById(varieteId);
            if (chip != null) variete = chip.getText().toString();
        }

        Date debutPeriode = null;
        if (periode == R.id.chipAujourdhui) debutPeriode = DateUtils.today();
        else if (periode == R.id.chip7Jours) debutPeriode = DateUtils.addDays(DateUtils.today(), -6);
        else if (periode == R.id.chip30Jours) debutPeriode = DateUtils.addDays(DateUtils.today(), -29);
        String campagne = periode == R.id.chipCampagne ? LotFilter.campagneCourante() : null;

        affiches.clear();
        for (Lot l : tous) {
            Date reception = l.getDateReception();
            if (debutPeriode != null && (reception == null || reception.before(debutPeriode))) continue;
            if (campagne != null && (reception == null || !campagne.equals(LotFilter.campagne(reception)))) continue;
            if (variete != null && !variete.equals(l.getVariete())) continue;
            if (statut == R.id.chipEnCoursHist && !l.isEnCours()) continue;
            if (statut == R.id.chipPretsHist && !l.isEnAttenteDeRecuperation()) continue;
            if (statut == R.id.chipRecuperes && l.getDateRecuperation() == null) continue;
            if (statut == R.id.chipTraitementHist && l.getStatut() != StatutLot.EN_TRAITEMENT) continue;
            if (!LotFilter.correspond(l, recherche)) continue;
            affiches.add(l);
        }

        LotFilter.Bilan bilan = LotFilter.bilan(affiches);
        binding.txtTotLots.setText(String.valueOf(bilan.nbLots));
        binding.txtTotKg.setText(Formats.nombre(bilan.poidsKg));
        binding.txtTotHuile.setText(Formats.nombre(bilan.huileL));
        binding.txtTotRendement.setText(bilan.poidsAvecHuileKg > 0
                ? String.format(Locale.FRENCH, "%.1f %%", bilan.rendementMoyen()) : "—");

        adapter.submit(affiches, null);
        binding.layoutEmpty.setVisibility(affiches.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnExporter.setEnabled(!affiches.isEmpty());
    }

    @Override
    public void onOuvrir(Lot lot) {
        startActivity(LotDetailActivity.intent(this, lot.getId()));
    }
}
