package com.example.zitounaproject.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zitounaproject.databinding.ItemLotBinding;
import com.example.zitounaproject.model.FileAttenteCalculator;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Carte d'un lot : file du moulin (gestionnaire) ou suivi de ses lots (agriculteur). */
public class LotAdapter extends RecyclerView.Adapter<LotAdapter.Holder> {

    public enum Mode { GESTIONNAIRE, AGRICULTEUR }

    public interface Listener {
        void onOuvrir(Lot lot);

        default void onAvancer(Lot lot) { }

        default void onMonter(Lot lot) { }

        default void onDescendre(Lot lot) { }

        default void onPrioriser(Lot lot) { }
    }

    private final Mode mode;
    private final Listener listener;
    private final List<Lot> items = new ArrayList<>();
    private Map<String, FileAttenteCalculator.Estimation> estimations = new HashMap<>();

    public LotAdapter(Mode mode, Listener listener) {
        this.mode = mode;
        this.listener = listener;
    }

    /** @param estimations estimations calculées en direct (gestionnaire) ; null = valeurs enregistrées. */
    public void submit(List<Lot> list, @Nullable Map<String, FileAttenteCalculator.Estimation> estimations) {
        items.clear();
        items.addAll(list);
        this.estimations = estimations != null ? estimations : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLotBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Lot lot = items.get(position);
        ItemLotBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();
        StatutLot statut = lot.getStatut() != null ? lot.getStatut() : StatutLot.RECEPTIONNE;

        b.txtNumero.setText("#" + lot.getNumero());
        b.txtStatut.setText(statut.getLibelle());
        b.txtStatut.setTextColor(ContextCompat.getColor(ctx, statut.getCouleurTexte()));
        ViewCompat.setBackgroundTintList(b.txtStatut,
                ColorStateList.valueOf(ContextCompat.getColor(ctx, statut.getCouleurFond())));
        b.imgPrioritaire.setVisibility(lot.isPrioritaire() && statut.estDansLaFile() ? View.VISIBLE : View.GONE);

        if (mode == Mode.GESTIONNAIRE) {
            b.txtTitre.setText(lot.getNomProducteur());
        } else {
            b.txtTitre.setText(lot.getVariete() != null ? lot.getVariete() : "Lot d'olives");
        }
        String recu = lot.getDateReception() != null
                ? "Reçu " + DateUtils.relatif(lot.getDateReception()).toLowerCase(Locale.FRENCH)
                + " à " + heure(lot.getDateReception()) : "";
        b.txtSousTitre.setText(mode == Mode.GESTIONNAIRE ? lot.getVariete() + " • " + recu : recu);
        b.txtPoids.setText(Formats.kg(lot.getPoidsKg()));

        // Position / attente : estimation en direct si disponible, sinon valeurs enregistrées
        FileAttenteCalculator.Estimation e = estimations.get(lot.getId());
        int pos = e != null ? e.position : lot.getPositionFile();
        Date debut = e != null ? e.debut : lot.getDebutEstime();
        int attente = e != null ? Math.max(0, e.attenteMin) : lot.estimerAttente();

        b.txtPosition.setVisibility(statut.estDansLaFile() ? View.VISIBLE : View.GONE);
        b.txtPosition.setText(statut == StatutLot.EN_TRAITEMENT ? "▶" : String.valueOf(pos));

        if (statut == StatutLot.EN_TRAITEMENT) {
            int pct = (int) Math.round(lot.progressionTraitement() * 100);
            b.layoutProgression.setVisibility(View.VISIBLE);
            b.progressTraitement.setProgress(pct);
            b.txtProgression.setText(pct + " % • ≈ " + Formats.duree(lot.minutesRestantesTraitement() / 60.0) + " restant");
            b.lblInfo2.setText("FIN ESTIMÉE");
            b.txtInfo2.setText(lot.getDateDebutTraitement() != null
                    ? heure(new Date(lot.getDateDebutTraitement().getTime() + lot.dureeTraitementMin() * 60_000L)) : "—");
            b.lblInfo3.setText("DURÉE");
            b.txtInfo3.setText(Formats.duree(lot.dureeTraitementMin() / 60.0));
        } else if (statut.estDansLaFile()) {
            b.layoutProgression.setVisibility(View.GONE);
            b.lblInfo2.setText("PRÉVU");
            b.txtInfo2.setText(debut != null ? heure(debut) : "—");
            b.lblInfo3.setText("ATTENTE");
            b.txtInfo3.setText(attente <= 0 ? "Imminent" : "≈ " + Formats.duree(attente / 60.0));
        } else {
            b.layoutProgression.setVisibility(View.GONE);
            b.lblInfo2.setText("HUILE");
            b.txtInfo2.setText(lot.getQuantiteHuileL() != null ? Formats.nombre(lot.getQuantiteHuileL()) + " L" : "—");
            b.lblInfo3.setText("RENDEMENT");
            b.txtInfo3.setText(lot.getQuantiteHuileL() != null
                    ? String.format(Locale.FRENCH, "%.1f %%", lot.calculerRendement()) : "—");
        }

        // Phase actuelle et phase suivante, visibles directement sur la carte
        String etape = "Étape " + statut.etape() + "/4 : " + statut.getLibelle();
        if (statut == StatutLot.PRET_A_RECUPERER) {
            etape += lot.getDateRecuperation() != null
                    ? " • récupéré le " + DateUtils.dateHeure(lot.getDateRecuperation())
                    : " • en attente de récupération";
        } else if (statut.suivant() != null) {
            etape += " → prochaine : " + statut.suivant().getLibelle();
        }
        b.txtEtape.setText(etape);

        // Actions du gestionnaire
        boolean gestion = mode == Mode.GESTIONNAIRE && LotActions.aUneActionSuivante(lot);
        boolean enFile = gestion && statut != StatutLot.EN_TRAITEMENT && statut.estDansLaFile();
        b.layoutActions.setVisibility(gestion ? View.VISIBLE : View.GONE);
        b.btnMonter.setVisibility(enFile ? View.VISIBLE : View.GONE);
        b.btnDescendre.setVisibility(enFile ? View.VISIBLE : View.GONE);
        b.btnPrioriser.setVisibility(enFile ? View.VISIBLE : View.GONE);
        b.btnAvancer.setVisibility(gestion ? View.VISIBLE : View.GONE);
        if (gestion) b.btnAvancer.setText(statut.getActionSuivante());

        b.getRoot().setOnClickListener(v -> listener.onOuvrir(lot));
        b.btnAvancer.setOnClickListener(v -> listener.onAvancer(lot));
        b.btnMonter.setOnClickListener(v -> listener.onMonter(lot));
        b.btnDescendre.setOnClickListener(v -> listener.onDescendre(lot));
        b.btnPrioriser.setOnClickListener(v -> listener.onPrioriser(lot));
    }

    private static String heure(Date date) {
        return new SimpleDateFormat("HH'h'mm", Locale.FRENCH).format(date);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemLotBinding binding;

        Holder(ItemLotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
