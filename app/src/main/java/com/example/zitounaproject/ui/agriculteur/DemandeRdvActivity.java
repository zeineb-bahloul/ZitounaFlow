package com.example.zitounaproject.ui.agriculteur;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.AuthRepository;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.RendezVousRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.ActivityDemandeRdvBinding;
import com.example.zitounaproject.model.Agriculteur;
import com.example.zitounaproject.model.Creneaux;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.BaseActivity;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.Date;
import java.util.Locale;

/** US-08 : demander un rendez-vous de pressage (maquette 4 « Réserver un créneau »). */
public class DemandeRdvActivity extends BaseActivity {

    private static final double KG_MIN = 50;
    private static final double KG_MAX = 20000;
    private static final double PAS_KG = 50;

    private ActivityDemandeRdvBinding binding;
    private final RendezVousRepository repository = new RendezVousRepository();

    private MaterialCardView[] cartesCreneaux;
    private TextView[] statutsCreneaux;
    private int[] reservations = new int[Creneaux.LIBELLES.length];
    private Date jourChoisi = DateUtils.today();
    private int creneauChoisi = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDemandeRdvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        cartesCreneaux = new MaterialCardView[]{binding.cardSlot0, binding.cardSlot1, binding.cardSlot2, binding.cardSlot3};
        TextView[] heures = {binding.txtSlot0, binding.txtSlot1, binding.txtSlot2, binding.txtSlot3};
        statutsCreneaux = new TextView[]{binding.txtSlot0Statut, binding.txtSlot1Statut, binding.txtSlot2Statut, binding.txtSlot3Statut};
        for (int i = 0; i < cartesCreneaux.length; i++) {
            final int index = i;
            heures[i].setText(Creneaux.LIBELLES[i]);
            cartesCreneaux[i].setOnClickListener(v -> choisirCreneau(index));
        }

        construireJours();
        binding.btnMoins.setOnClickListener(v -> changerQuantite(-PAS_KG));
        binding.btnPlus.setOnClickListener(v -> changerQuantite(PAS_KG));
        binding.editQuantite.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { mettreAJourEstimation(); }
        });
        binding.chipGroupVariete.setOnCheckedStateChangeListener((g, ids) -> binding.txtErreur.setVisibility(View.GONE));
        binding.btnEnvoyer.setOnClickListener(v -> envoyer());

        binding.editQuantite.setText("1000");
        chargerDisponibilites();
    }

    /** Bandeau des 14 prochains jours (puces sélectionnables). */
    private void construireJours() {
        LayoutInflater inflater = LayoutInflater.from(this);
        binding.chipGroupJours.removeAllViews();
        Date jour = DateUtils.today();
        for (int i = 0; i < Creneaux.JOURS_RESERVABLES; i++) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_chip_jour, binding.chipGroupJours, false);
            final Date d = jour;
            chip.setId(View.generateViewId());
            chip.setText(DateUtils.jourSemaine(d) + " " + DateUtils.numeroJour(d));
            chip.setChecked(i == 0);
            chip.setOnClickListener(v -> {
                jourChoisi = d;
                binding.txtMois.setText(DateUtils.moisAnnee(d));
                chargerDisponibilites();
            });
            binding.chipGroupJours.addView(chip);
            jour = DateUtils.addDays(jour, 1);
        }
        binding.txtMois.setText(DateUtils.moisAnnee(jourChoisi));
    }

    private void chargerDisponibilites() {
        creneauChoisi = -1;
        binding.progressCreneaux.setVisibility(View.VISIBLE);
        repository.chargerReservations(DateUtils.jourKey(jourChoisi), new Callback<int[]>() {
            @Override
            public void onSuccess(int[] result) {
                binding.progressCreneaux.setVisibility(View.GONE);
                reservations = result;
                afficherCreneaux();
            }

            @Override
            public void onError(String message) {
                binding.progressCreneaux.setVisibility(View.GONE);
                reservations = new int[Creneaux.LIBELLES.length];
                afficherCreneaux();
                toast(message);
            }
        });
    }

    private boolean estDisponible(int index) {
        boolean passe = Creneaux.debut(jourChoisi, index).before(new Date());
        return !passe && reservations[index] < Creneaux.CAPACITE_PAR_CRENEAU;
    }

    private void afficherCreneaux() {
        for (int i = 0; i < cartesCreneaux.length; i++) {
            int restantes = Creneaux.CAPACITE_PAR_CRENEAU - reservations[i];
            boolean passe = Creneaux.debut(jourChoisi, i).before(new Date());
            String texte;
            int couleur;
            if (passe) {
                texte = getString(R.string.creneau_passe);
                couleur = R.color.muted;
            } else if (restantes <= 0) {
                texte = getString(R.string.creneau_complet);
                couleur = R.color.error;
            } else if (restantes == 1) {
                texte = getString(R.string.creneau_derniere_place);
                couleur = R.color.gold_dark;
            } else {
                texte = getString(R.string.creneau_places, restantes);
                couleur = R.color.olive_700;
            }
            statutsCreneaux[i].setText(texte);
            statutsCreneaux[i].setTextColor(ContextCompat.getColor(this, couleur));
            cartesCreneaux[i].setEnabled(estDisponible(i));
            cartesCreneaux[i].setAlpha(estDisponible(i) ? 1f : 0.5f);
        }
        rafraichirSelection();
    }

    private void choisirCreneau(int index) {
        if (!estDisponible(index)) return;
        creneauChoisi = index;
        binding.txtErreur.setVisibility(View.GONE);
        rafraichirSelection();
        mettreAJourEstimation();
    }

    private void rafraichirSelection() {
        for (int i = 0; i < cartesCreneaux.length; i++) {
            boolean selected = i == creneauChoisi;
            cartesCreneaux[i].setStrokeColor(ContextCompat.getColor(this, selected ? R.color.olive_700 : R.color.stroke));
            cartesCreneaux[i].setStrokeWidth(getResources().getDimensionPixelSize(
                    selected ? R.dimen.stroke_selected : R.dimen.stroke_normal));
            cartesCreneaux[i].setCardBackgroundColor(ContextCompat.getColor(this, selected ? R.color.olive_100 : R.color.white));
        }
    }

    private double quantite() {
        try {
            return Double.parseDouble(binding.editQuantite.getText() == null ? "0"
                    : binding.editQuantite.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void changerQuantite(double delta) {
        double q = Math.max(KG_MIN, Math.min(KG_MAX, quantite() + delta));
        binding.editQuantite.setText(String.format(Locale.US, "%.0f", q));
        binding.editQuantite.setSelection(binding.editQuantite.getText() == null ? 0 : binding.editQuantite.getText().length());
    }

    /** « Calcul prédictif » de la maquette : caisses, durée de traitement et huile estimées. */
    private void mettreAJourEstimation() {
        double kg = quantite();
        int caisses = (int) Math.ceil(kg / Creneaux.KG_PAR_CAISSE);
        binding.txtCaisses.setText(getString(R.string.caisses_estimees, caisses));
        binding.txtDuree.setText(kg > 0 ? Formats.duree(kg / Creneaux.CAPACITE_MACHINE_KG_H) : "—");
        double min = kg * Creneaux.RENDEMENT_MIN / 100;
        double max = kg * Creneaux.RENDEMENT_MAX / 100;
        binding.txtHuile.setText(kg > 0 ? "~" + Formats.nombre(min) + " - " + Formats.nombre(max) + " L" : "—");
    }

    private String varieteChoisie() {
        int id = binding.chipGroupVariete.getCheckedChipId();
        if (id == View.NO_ID) return null;
        Chip chip = binding.chipGroupVariete.findViewById(id);
        return chip != null ? chip.getText().toString() : null;
    }

    private void envoyer() {
        double kg = quantite();
        String variete = varieteChoisie();
        if (creneauChoisi < 0) {
            erreur(getString(R.string.err_creneau));
            return;
        }
        if (variete == null) {
            erreur(getString(R.string.err_variete));
            return;
        }
        if (kg < KG_MIN || kg > KG_MAX) {
            erreur(getString(R.string.err_quantite));
            return;
        }
        Utilisateur u = UserSession.get();
        if (u == null) {
            // Session perdue (redémarrage du processus) : on recharge le profil puis on réessaie
            new AuthRepository().loadCurrentUser(new Callback<Utilisateur>() {
                @Override
                public void onSuccess(Utilisateur result) {
                    envoyer();
                }

                @Override
                public void onError(String message) {
                    toast(message);
                }
            });
            return;
        }

        RendezVous rdv = new RendezVous();
        rdv.setAgriculteurId(u.getId());
        rdv.setAgriculteurNom(u.getNomComplet());
        rdv.setTelephone(u.getTelephone());
        if (u instanceof Agriculteur) rdv.setNomExploitation(((Agriculteur) u).getNomExploitation());
        rdv.setDate(Creneaux.debut(jourChoisi, creneauChoisi));
        rdv.setJour(DateUtils.jourKey(jourChoisi));
        rdv.setCreneau(Creneaux.LIBELLES[creneauChoisi]);
        rdv.setVariete(variete);
        rdv.setQuantiteEstimeeKg(kg);
        String remarque = binding.editRemarque.getText() == null ? "" : binding.editRemarque.getText().toString().trim();
        rdv.setRemarque(remarque.isEmpty() ? null : remarque);

        setLoading(true);
        repository.demander(rdv, new Callback<RendezVous>() {
            @Override
            public void onSuccess(RendezVous result) {
                toast(getString(R.string.rdv_envoye));
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                erreur(message);
                chargerDisponibilites();
            }
        });
    }

    private void erreur(String message) {
        binding.txtErreur.setText(message);
        binding.txtErreur.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEnvoyer.setEnabled(!loading);
    }
}
