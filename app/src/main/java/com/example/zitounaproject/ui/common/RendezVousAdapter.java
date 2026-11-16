package com.example.zitounaproject.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zitounaproject.databinding.ItemRendezVousBinding;
import com.example.zitounaproject.model.Creneaux;
import com.example.zitounaproject.model.RendezVous;
import com.example.zitounaproject.model.StatutRendezVous;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;

import java.util.ArrayList;
import java.util.List;

/** Carte de rendez-vous, affichée côté agriculteur ou côté gestionnaire. */
public class RendezVousAdapter extends RecyclerView.Adapter<RendezVousAdapter.Holder> {

    public enum Mode { AGRICULTEUR, GESTIONNAIRE }

    public interface Listener {
        void onAnnuler(RendezVous rdv);

        void onConfirmer(RendezVous rdv);

        void onRefuser(RendezVous rdv);
    }

    private final Mode mode;
    private final Listener listener;
    private final List<RendezVous> items = new ArrayList<>();

    public RendezVousAdapter(Mode mode, Listener listener) {
        this.mode = mode;
        this.listener = listener;
    }

    public void submit(List<RendezVous> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRendezVousBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RendezVous rdv = items.get(position);
        ItemRendezVousBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();

        StatutRendezVous statut = rdv.getStatut() != null ? rdv.getStatut() : StatutRendezVous.EN_ATTENTE_CONFIRMATION;
        b.txtStatut.setText(statut.getLibelle());
        b.txtStatut.setTextColor(ContextCompat.getColor(ctx, statut.getCouleurTexte()));
        ViewCompat.setBackgroundTintList(b.txtStatut,
                ColorStateList.valueOf(ContextCompat.getColor(ctx, statut.getCouleurFond())));

        b.txtJour.setText(rdv.getDate() != null ? DateUtils.relatif(rdv.getDate()) : "");
        b.txtCreneau.setText(rdv.getCreneau());
        b.txtQuantite.setText(Formats.kg(rdv.getQuantiteEstimeeKg()));
        b.txtVariete.setText(rdv.getVariete());
        int caisses = (int) Math.ceil(rdv.getQuantiteEstimeeKg() / Creneaux.KG_PAR_CAISSE);
        b.txtCaisses.setText("≈ " + caisses + " caisses de 50 kg");

        if (mode == Mode.GESTIONNAIRE) {
            b.txtTitre.setText(rdv.getNomAffichage());
            String sous = rdv.getAgriculteurNom() != null ? rdv.getAgriculteurNom() : "";
            if (rdv.getTelephone() != null) sous += " • +216 " + rdv.getTelephone();
            b.txtSousTitre.setText(sous);
        } else {
            b.txtTitre.setText(DateUtils.jourLong(rdv.getDate()));
            b.txtSousTitre.setText("Demandé le " + DateUtils.dateHeure(rdv.getDateDemande()));
        }

        boolean hasRemarque = rdv.getRemarque() != null && !rdv.getRemarque().trim().isEmpty();
        b.txtRemarque.setVisibility(hasRemarque ? View.VISIBLE : View.GONE);
        if (hasRemarque) b.txtRemarque.setText("« " + rdv.getRemarque().trim() + " »");

        boolean refuse = statut == StatutRendezVous.REFUSE && rdv.getMotifRefus() != null;
        b.txtMotif.setVisibility(refuse ? View.VISIBLE : View.GONE);
        if (refuse) b.txtMotif.setText("Motif du refus : " + rdv.getMotifRefus());

        boolean actionsAgriculteur = mode == Mode.AGRICULTEUR && rdv.isAnnulable();
        boolean actionsGestionnaire = mode == Mode.GESTIONNAIRE && statut == StatutRendezVous.EN_ATTENTE_CONFIRMATION;
        b.btnAnnuler.setVisibility(actionsAgriculteur ? View.VISIBLE : View.GONE);
        b.btnRefuser.setVisibility(actionsGestionnaire ? View.VISIBLE : View.GONE);
        b.btnConfirmer.setVisibility(actionsGestionnaire ? View.VISIBLE : View.GONE);
        b.layoutActions.setVisibility(actionsAgriculteur || actionsGestionnaire ? View.VISIBLE : View.GONE);

        b.btnAnnuler.setOnClickListener(v -> listener.onAnnuler(rdv));
        b.btnConfirmer.setOnClickListener(v -> listener.onConfirmer(rdv));
        b.btnRefuser.setOnClickListener(v -> listener.onRefuser(rdv));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemRendezVousBinding binding;

        Holder(ItemRendezVousBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
