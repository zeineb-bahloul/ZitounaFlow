package com.example.zitounaproject.ui.gestionnaire;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.data.UtilisateurRepository;
import com.example.zitounaproject.databinding.ActivityEnregistrerLotBinding;
import com.example.zitounaproject.databinding.DialogProducteurDirectBinding;
import com.example.zitounaproject.model.Agriculteur;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.StatutRendezVous;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.BaseActivity;
import com.example.zitounaproject.ui.common.LotDetailActivity;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** US-12 : enregistrer un lot à l'arrivée au quai (BF-21) — maquette « Enregistrer un lot ». */
public class EnregistrerLotActivity extends BaseActivity {

    private ActivityEnregistrerLotBinding binding;
    private final LotRepository lotRepository = new LotRepository();

    // Producteur choisi
    @Nullable private String agriculteurId;
    @Nullable private String agriculteurNom;
    @Nullable private String nomExploitation;
    @Nullable private String telephone;
    @Nullable private String rendezVousId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEnregistrerLotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnChoisirProducteur.setOnClickListener(v -> choisirProducteur());
        binding.btnEnregistrer.setOnClickListener(v -> enregistrer());
        binding.chipGroupContenant.setOnCheckedStateChangeListener((g, ids) -> proposerTare());
        binding.inputNbContenants.getEditText().addTextChangedListener(watcher(this::proposerTare));
        binding.inputPoidsBrut.getEditText().addTextChangedListener(watcher(this::mettreAJourPoidsNet));
        binding.inputTare.getEditText().addTextChangedListener(watcher(this::mettreAJourPoidsNet));

        afficherProducteur();
        mettreAJourPoidsNet();
        chargerRendezVousDuJour();
    }

    // ---------------------------------------------------------------- Producteur

    /** Raccourcis : rendez-vous confirmés du jour qui n'ont pas encore de lot. */
    private void chargerRendezVousDuJour() {
        new RendezVousRepository().chargerParJour(DateUtils.jourKey(DateUtils.today()), new Callback<List<RendezVous>>() {
            @Override
            public void onSuccess(List<RendezVous> result) {
                binding.chipGroupRdv.removeAllViews();
                int n = 0;
                for (RendezVous rdv : result) {
                    if (rdv.getStatut() != StatutRendezVous.CONFIRME || rdv.getLotId() != null) continue;
                    Chip chip = (Chip) LayoutInflater.from(EnregistrerLotActivity.this)
                            .inflate(R.layout.item_chip_jour, binding.chipGroupRdv, false);
                    chip.setId(View.generateViewId());
                    chip.setText(rdv.getNomAffichage() + " • " + rdv.getCreneau().substring(0, 5));
                    chip.setOnClickListener(v -> appliquerRendezVous(rdv));
                    binding.chipGroupRdv.addView(chip);
                    n++;
                }
                binding.txtAucunRdv.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.txtAucunRdv.setVisibility(View.VISIBLE);
            }
        });
    }

    private void appliquerRendezVous(RendezVous rdv) {
        agriculteurId = rdv.getAgriculteurId();
        agriculteurNom = rdv.getAgriculteurNom();
        nomExploitation = rdv.getNomExploitation();
        telephone = rdv.getTelephone();
        rendezVousId = rdv.getId();
        cocherVariete(rdv.getVariete());
        if (Validators.text(binding.inputPoidsBrut).isEmpty() && binding.inputPoidsBrut.getEditText() != null) {
            binding.inputPoidsBrut.getEditText().setText(String.valueOf((long) rdv.getQuantiteEstimeeKg()));
        }
        afficherProducteur();
    }

    private void choisirProducteur() {
        binding.progress.setVisibility(View.VISIBLE);
        new UtilisateurRepository().listerAgriculteurs(new Callback<List<Utilisateur>>() {
            @Override
            public void onSuccess(List<Utilisateur> result) {
                binding.progress.setVisibility(View.GONE);
                List<String> libelles = new ArrayList<>();
                libelles.add(getString(R.string.producteur_sans_compte));
                for (Utilisateur u : result) {
                    String tel = u.getTelephone() != null ? " • " + u.getTelephone() : "";
                    libelles.add(u.getNomAffichage() + tel);
                }
                new MaterialAlertDialogBuilder(EnregistrerLotActivity.this)
                        .setTitle(R.string.choisir_producteur)
                        .setItems(libelles.toArray(new String[0]), (d, which) -> {
                            if (which == 0) {
                                saisirProducteurDirect();
                            } else {
                                appliquerUtilisateur(result.get(which - 1));
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
    }

    private void appliquerUtilisateur(Utilisateur u) {
        agriculteurId = u.getId();
        agriculteurNom = u.getNomComplet();
        nomExploitation = u instanceof Agriculteur ? ((Agriculteur) u).getNomExploitation() : null;
        telephone = u.getTelephone();
        rendezVousId = null;
        afficherProducteur();
    }

    /** « Ajout sans app » : apport direct d'un producteur qui n'a pas de compte. */
    private void saisirProducteurDirect() {
        DialogProducteurDirectBinding d = DialogProducteurDirectBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.producteur_sans_compte)
                .setView(d.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.valider, (dialog, w) -> {
                    String nom = Validators.text(d.inputNomDirect);
                    if (nom.isEmpty()) {
                        toast(getString(R.string.err_required));
                        return;
                    }
                    agriculteurId = null;
                    agriculteurNom = nom;
                    nomExploitation = null;
                    String tel = Validators.text(d.inputTelDirect);
                    telephone = tel.isEmpty() ? null : tel;
                    rendezVousId = null;
                    afficherProducteur();
                })
                .show();
    }

    private void afficherProducteur() {
        if (agriculteurNom == null) {
            binding.txtProducteurNom.setText(R.string.aucun_producteur);
            binding.txtProducteurInfo.setText(R.string.aucun_producteur_sub);
            binding.btnChoisirProducteur.setText(R.string.choisir);
            return;
        }
        binding.txtProducteurNom.setText(nomExploitation != null && !nomExploitation.isEmpty() ? nomExploitation : agriculteurNom);
        String info = agriculteurNom;
        if (telephone != null) info += " • +216 " + telephone;
        if (agriculteurId == null) info += " • " + getString(R.string.sans_compte);
        if (rendezVousId != null) info += " • RDV du jour";
        binding.txtProducteurInfo.setText(info);
        binding.btnChoisirProducteur.setText(R.string.changer);
    }

    // ---------------------------------------------------------------- Pesée

    private double nombre(String s) {
        try {
            return s.isEmpty() ? 0 : Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Tare proposée selon le contenant (caisse 2,5 kg, palox 40 kg, benne : tare saisie). */
    private void proposerTare() {
        int id = binding.chipGroupContenant.getCheckedChipId();
        double nb = nombre(Validators.text(binding.inputNbContenants));
        double unitaire = id == R.id.chipCaisses ? 2.5 : id == R.id.chipPalox ? 40 : 0;
        binding.inputNbContenants.setVisibility(id == R.id.chipVrac ? View.GONE : View.VISIBLE);
        if (unitaire > 0 && nb > 0 && binding.inputTare.getEditText() != null) {
            binding.inputTare.getEditText().setText(String.valueOf(Math.round(nb * unitaire)));
        }
    }

    private double poidsNet() {
        return nombre(Validators.text(binding.inputPoidsBrut)) - nombre(Validators.text(binding.inputTare));
    }

    private void mettreAJourPoidsNet() {
        double net = poidsNet();
        binding.txtPoidsNet.setText(net > 0 ? Formats.nombre(net) : "0");
    }

    private void cocherVariete(@Nullable String variete) {
        if (variete == null) return;
        for (int i = 0; i < binding.chipGroupVariete.getChildCount(); i++) {
            View v = binding.chipGroupVariete.getChildAt(i);
            if (v instanceof Chip && variete.equals(((Chip) v).getText().toString())) {
                ((Chip) v).setChecked(true);
            }
        }
    }

    private String chipTexte(int id) {
        if (id == View.NO_ID) return null;
        Chip chip = findViewById(id);
        return chip != null ? chip.getText().toString() : null;
    }

    // ---------------------------------------------------------------- Enregistrement

    private void enregistrer() {
        double brut = nombre(Validators.text(binding.inputPoidsBrut));
        double tare = nombre(Validators.text(binding.inputTare));
        double net = brut - tare;
        String variete = chipTexte(binding.chipGroupVariete.getCheckedChipId());
        String contenant = chipTexte(binding.chipGroupContenant.getCheckedChipId());

        if (agriculteurNom == null) {
            erreur(getString(R.string.err_producteur));
            return;
        }
        boolean ok = Validators.check(binding.inputPoidsBrut, brut > 0, getString(R.string.err_poids));
        ok &= Validators.check(binding.inputTare, tare >= 0 && net > 0, getString(R.string.err_tare));
        if (!ok) return;
        if (variete == null) {
            erreur(getString(R.string.err_variete));
            return;
        }

        Lot lot = new Lot();
        lot.setAgriculteurId(agriculteurId);
        lot.setAgriculteurNom(agriculteurNom);
        lot.setNomExploitation(nomExploitation);
        lot.setTelephone(telephone);
        lot.setRendezVousId(rendezVousId);
        lot.setPoidsBrutKg(brut);
        lot.setTareKg(tare);
        lot.setPoidsKg(net);
        lot.setVariete(variete);
        lot.setTypeContenant(contenant);
        String origine = Validators.text(binding.inputOrigine);
        lot.setOrigine(origine.isEmpty() ? null : origine);
        String remarque = Validators.text(binding.inputRemarqueLot);
        lot.setRemarque(remarque.isEmpty() ? null : remarque);

        binding.txtErreur.setVisibility(View.GONE);
        setLoading(true);
        lotRepository.enregistrer(lot, new Callback<Lot>() {
            @Override
            public void onSuccess(Lot result) {
                toast(getString(R.string.lot_enregistre, result.getNumero()));
                startActivity(LotDetailActivity.intent(EnregistrerLotActivity.this, result.getId()));
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                erreur(message);
            }
        });
    }

    private void erreur(String message) {
        binding.txtErreur.setText(message);
        binding.txtErreur.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEnregistrer.setEnabled(!loading);
    }

    private static TextWatcher watcher(Runnable action) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { action.run(); }
        };
    }
}
