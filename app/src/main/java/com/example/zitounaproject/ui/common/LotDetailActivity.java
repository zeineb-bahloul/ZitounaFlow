package com.example.zitounaproject.ui.common;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.ActivityLotDetailBinding;
import com.example.zitounaproject.model.HistoriqueStatut;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Suivi détaillé d'un lot (maquette « Lot #LOT-408 ») : informations de pesée, position et attente,
 * étapes du cycle de vie et historique des statuts. Le gestionnaire peut faire avancer le lot.
 */
public class LotDetailActivity extends BaseActivity {

    private static final String EXTRA_LOT_ID = "lot_id";

    public static Intent intent(Context context, String lotId) {
        return new Intent(context, LotDetailActivity.class).putExtra(EXTRA_LOT_ID, lotId);
    }

    private ActivityLotDetailBinding binding;
    private final LotRepository repository = new LotRepository();
    @Nullable private ListenerRegistration lotRegistration;
    @Nullable private ListenerRegistration histRegistration;
    @Nullable private Lot lot;
    private List<HistoriqueStatut> historique = new ArrayList<>();
    private String lotId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLotDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        lotId = getIntent().getStringExtra(EXTRA_LOT_ID);
        if (lotId == null) {
            finish();
            return;
        }
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAvancer.setOnClickListener(v -> avancer());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (lotId == null) return;
        lotRegistration = repository.ecouterLot(lotId, new Callback<Lot>() {
            @Override
            public void onSuccess(Lot result) {
                lot = result;
                afficher();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
        histRegistration = repository.ecouterHistorique(lotId, new Callback<List<HistoriqueStatut>>() {
            @Override
            public void onSuccess(List<HistoriqueStatut> result) {
                historique = result;
                afficher();
            }

            @Override
            public void onError(String message) {
                // L'historique reste vide
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (lotRegistration != null) lotRegistration.remove();
        if (histRegistration != null) histRegistration.remove();
        lotRegistration = null;
        histRegistration = null;
    }

    private boolean estGestionnaire() {
        return UserSession.get() != null && UserSession.get().getRole() == Role.GESTIONNAIRE;
    }

    private void afficher() {
        if (lot == null) return;
        StatutLot statut = lot.getStatut() != null ? lot.getStatut() : StatutLot.RECEPTIONNE;

        binding.txtTitre.setText("Lot #" + lot.getNumero());
        binding.txtStatut.setText(statut.getLibelle());
        binding.txtStatut.setTextColor(ContextCompat.getColor(this, statut.getCouleurTexte()));
        ViewCompat.setBackgroundTintList(binding.txtStatut,
                ColorStateList.valueOf(ContextCompat.getColor(this, statut.getCouleurFond())));

        binding.txtVariete.setText(lot.getVariete());
        String producteur = lot.getNomProducteur();
        if (lot.getOrigine() != null) producteur += " • " + lot.getOrigine();
        binding.txtProducteur.setText(producteur);
        binding.txtReception.setText(getString(R.string.recu_le, DateUtils.dateHeure(lot.getDateReception())));

        binding.txtPoidsNet.setText(Formats.nombre(lot.getPoidsKg()));
        binding.txtPesee.setText(getString(R.string.pesee_detail, Formats.kg(lot.getPoidsBrutKg()),
                Formats.kg(lot.getTareKg()), lot.getTypeContenant() != null ? lot.getTypeContenant() : "—"));
        binding.txtDuree.setText(Formats.duree(lot.dureeTraitementMin() / 60.0));

        afficherFile(statut);
        afficherEtapes(statut);
        afficherHistorique();

        // Huile (sprint 4)
        boolean huile = lot.getQuantiteHuileL() != null;
        binding.cardHuile.setVisibility(huile ? View.VISIBLE : View.GONE);
        if (huile) {
            binding.txtHuile.setText(Formats.nombre(lot.getQuantiteHuileL()) + " L");
            binding.txtRendement.setText(String.format(Locale.FRENCH, "%.1f %%", lot.calculerRendement()));
        }

        // Action du gestionnaire
        StatutLot suivant = statut.suivant();
        boolean peutAvancer = estGestionnaire() && suivant != null && suivant != StatutLot.PRET_A_RECUPERER;
        binding.btnAvancer.setVisibility(peutAvancer ? View.VISIBLE : View.GONE);
        if (peutAvancer) binding.btnAvancer.setText(statut.getActionSuivante());
    }

    /** Position, attente estimée ou avancement du traitement. */
    private void afficherFile(StatutLot statut) {
        binding.cardFile.setVisibility(statut.estDansLaFile() ? View.VISIBLE : View.GONE);
        if (!statut.estDansLaFile()) return;
        if (statut == StatutLot.EN_TRAITEMENT) {
            int pct = (int) Math.round(lot.progressionTraitement() * 100);
            binding.lblFile1.setText(R.string.avancement);
            binding.txtFile1.setText(pct + " %");
            binding.lblFile2.setText(R.string.temps_restant);
            binding.txtFile2.setText("≈ " + Formats.duree(lot.minutesRestantesTraitement() / 60.0));
            binding.progressFile.setVisibility(View.VISIBLE);
            binding.progressFile.setProgress(pct);
            binding.txtFileNote.setText(R.string.traitement_en_cours);
        } else {
            int attente = lot.estimerAttente();
            int pos = lot.getPositionFile();
            binding.lblFile1.setText(R.string.rang_file);
            binding.txtFile1.setText(pos <= 0 ? "—" : pos == 1 ? "1er" : pos + "e");
            binding.lblFile2.setText(R.string.attente_estimee);
            binding.txtFile2.setText(attente <= 0 ? getString(R.string.imminent) : "≈ " + Formats.duree(attente / 60.0));
            binding.progressFile.setVisibility(View.GONE);
            binding.txtFileNote.setText(lot.getDebutEstime() != null
                    ? getString(R.string.debut_prevu, heure(lot.getDebutEstime()))
                    : getString(R.string.estimation_en_cours));
        }
    }

    /** Les 4 étapes du cycle de vie (maquette « Ligne d'extraction »). */
    private void afficherEtapes(StatutLot statut) {
        ImageView[] icones = {binding.stepIcon0, binding.stepIcon1, binding.stepIcon2, binding.stepIcon3};
        TextView[] heures = {binding.stepTime0, binding.stepTime1, binding.stepTime2, binding.stepTime3};
        StatutLot[] etapes = StatutLot.values();
        for (int i = 0; i < etapes.length; i++) {
            boolean faite = i < statut.ordinal() || (i == statut.ordinal() && statut == StatutLot.PRET_A_RECUPERER);
            boolean courante = i == statut.ordinal() && statut != StatutLot.PRET_A_RECUPERER;
            icones[i].setImageResource(faite ? R.drawable.ic_check : courante ? R.drawable.ic_clock : R.drawable.ic_circle);
            icones[i].setColorFilter(ContextCompat.getColor(this,
                    faite ? R.color.olive_700 : courante ? R.color.gold_dark : R.color.stroke));
            Date date = dateEtape(etapes[i]);
            heures[i].setText(date != null ? heure(date) : courante ? getString(R.string.en_cours) : "");
            heures[i].setTextColor(ContextCompat.getColor(this, courante ? R.color.gold_dark : R.color.muted));
        }
    }

    @Nullable
    private Date dateEtape(StatutLot etape) {
        if (etape == StatutLot.RECEPTIONNE) return lot.getDateReception();
        for (HistoriqueStatut h : historique) {
            if (h.getNouveauStatut() == etape) return h.getDateChangement();
        }
        return null;
    }

    private void afficherHistorique() {
        StringBuilder sb = new StringBuilder();
        for (HistoriqueStatut h : historique) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("• ").append(DateUtils.dateHeure(h.getDateChangement())).append(" — ");
            sb.append(h.getAncienStatut() == null ? "Création : " : h.getAncienStatut().getLibelle() + " → ");
            sb.append(h.getNouveauStatut() != null ? h.getNouveauStatut().getLibelle() : "");
        }
        binding.txtHistorique.setText(sb.length() == 0 ? getString(R.string.aucun_historique) : sb.toString());
    }

    private void avancer() {
        if (lot == null || lot.getStatut() == null || lot.getStatut().suivant() == null) return;
        Lot courant = lot;
        new MaterialAlertDialogBuilder(this)
                .setTitle(courant.getStatut().getActionSuivante())
                .setMessage(getString(R.string.avancer_message, courant.getNumero(), courant.getStatut().suivant().getLibelle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.valider, (d, w) -> repository.avancerStatut(courant, new Callback<StatutLot>() {
                    @Override
                    public void onSuccess(StatutLot result) {
                        toast(getString(R.string.statut_mis_a_jour, courant.getNumero(), result.getLibelle()));
                    }

                    @Override
                    public void onError(String message) {
                        toast(message);
                    }
                }))
                .show();
    }

    private static String heure(Date date) {
        return new SimpleDateFormat("HH'h'mm", Locale.FRENCH).format(date);
    }
}
