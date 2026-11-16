package com.example.zitounaproject.ui.agriculteur;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentMesLotsBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.LotFilter;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.LotAdapter;
import com.example.zitounaproject.ui.common.LotDetailActivity;
import com.example.zitounaproject.util.CsvExport;
import com.example.zitounaproject.util.Formats;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * « Mes Campagnes & Lots » (maquette 6) :
 * US-15 suivi des lots, US-19 huile obtenue et récupération, US-20 historique et bilan de campagne.
 */
public class MesLotsFragment extends Fragment implements LotAdapter.Listener {

    private FragmentMesLotsBinding binding;
    private LotAdapter adapter;
    private final List<Lot> tous = new ArrayList<>();
    private final List<Lot> affiches = new ArrayList<>();
    /** null = toutes les campagnes. */
    @Nullable private String campagneChoisie = LotFilter.campagneCourante();
    private List<String> campagnesAffichees = new ArrayList<>();
    @Nullable private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMesLotsBinding.inflate(inflater, container, false);
        adapter = new LotAdapter(LotAdapter.Mode.AGRICULTEUR, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.recycler.setNestedScrollingEnabled(false);

        Utilisateur u = UserSession.get();
        if (u != null) binding.txtExploitation.setText(u.getNomAffichage());
        binding.chipGroupFiltre.setOnCheckedStateChangeListener((g, ids) -> afficher());
        if (binding.inputRecherche.getEditText() != null) {
            binding.inputRecherche.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) { afficher(); }
            });
        }
        binding.btnExporter.setOnClickListener(v ->
                CsvExport.partager(requireContext(), getString(R.string.registre_titre), affiches));
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        binding.progress.setVisibility(View.VISIBLE);
        registration = new LotRepository().ecouterMesLots(uid, new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                tous.clear();
                tous.addAll(result);
                construireCampagnes();
                afficher();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
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

    /** Puces « Toutes » + une puce par campagne présente (avec le nombre de lots). */
    private void construireCampagnes() {
        List<String> campagnes = LotFilter.campagnes(tous);
        if (!campagnes.contains(LotFilter.campagneCourante())) campagnes.add(0, LotFilter.campagneCourante());
        if (campagneChoisie != null && !campagnes.contains(campagneChoisie)) campagneChoisie = null;
        campagnesAffichees = campagnes;

        binding.chipGroupCampagne.removeAllViews();
        ajouterChipCampagne(null, getString(R.string.toutes_campagnes, tous.size()));
        for (String c : campagnes) {
            int n = 0;
            for (Lot l : tous) if (l.getDateReception() != null && c.equals(LotFilter.campagne(l.getDateReception()))) n++;
            ajouterChipCampagne(c, getString(R.string.campagne_n, c, n));
        }
    }

    private void ajouterChipCampagne(@Nullable String campagne, String texte) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_jour, binding.chipGroupCampagne, false);
        chip.setId(View.generateViewId());
        chip.setText(texte);
        chip.setTypeface(null);
        boolean selectionnee = campagne == null ? campagneChoisie == null : campagne.equals(campagneChoisie);
        binding.chipGroupCampagne.addView(chip);
        chip.setChecked(selectionnee);
        chip.setOnClickListener(v -> {
            campagneChoisie = campagne;
            afficher();
        });
    }

    private void afficher() {
        if (binding == null) return;
        String recherche = Validators.text(binding.inputRecherche);
        int filtre = binding.chipGroupFiltre.getCheckedChipId();

        // Lots de la campagne choisie (pour le bilan)
        List<Lot> campagne = new ArrayList<>();
        for (Lot l : tous) {
            if (campagneChoisie != null && (l.getDateReception() == null
                    || !campagneChoisie.equals(LotFilter.campagne(l.getDateReception())))) continue;
            campagne.add(l);
        }
        LotFilter.Bilan bilan = LotFilter.bilan(campagne);
        binding.txtBilanTitre.setText(campagneChoisie == null
                ? getString(R.string.bilan_toutes) : getString(R.string.bilan_campagne, campagneChoisie));
        binding.txtBilanStatut.setText(campagneChoisie != null && campagneChoisie.equals(LotFilter.campagneCourante())
                ? getString(R.string.en_cours_maj) : getString(R.string.cloturee));
        binding.txtOlivesPesees.setText(Formats.nombre(bilan.poidsKg));
        binding.txtHuileExtraite.setText(Formats.nombre(bilan.huileL));
        binding.txtRendementMoyen.setText(bilan.poidsAvecHuileKg > 0
                ? String.format(Locale.FRENCH, "%.1f", bilan.rendementMoyen()) : "—");
        binding.txtNbLots.setText(String.valueOf(bilan.nbLots));

        // Liste : recherche + filtre de statut
        affiches.clear();
        for (Lot l : campagne) {
            if (!LotFilter.correspond(l, recherche)) continue;
            if (filtre == R.id.chipEnCours && !l.isEnCours()) continue;
            if (filtre == R.id.chipTermines && l.isEnCours()) continue;
            affiches.add(l);
        }
        adapter.submit(affiches, null);
        binding.txtNbAffiches.setText(getResources().getQuantityString(R.plurals.lots_affiches, affiches.size(), affiches.size()));
        binding.layoutEmpty.setVisibility(affiches.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnExporter.setEnabled(!affiches.isEmpty());
    }

    @Override
    public void onOuvrir(Lot lot) {
        startActivity(LotDetailActivity.intent(requireContext(), lot.getId()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
