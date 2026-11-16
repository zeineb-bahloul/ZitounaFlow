package com.example.zitounaproject.ui.common;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.databinding.DialogHuileBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.StatutLot;
import com.example.zitounaproject.util.Formats;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/**
 * Action « phase suivante » d'un lot, partagée par la file du moulin et le détail du lot :
 * Réceptionné → En attente → En traitement → (saisie de l'huile) Prêt → récupération.
 */
public final class LotActions {

    private LotActions() {
    }

    /** Le gestionnaire peut-il encore agir sur ce lot ? */
    public static boolean aUneActionSuivante(Lot lot) {
        return lot.getStatut() != null && (lot.getStatut().estDansLaFile() || lot.isEnAttenteDeRecuperation());
    }

    public static void executer(Context context, LotRepository repository, Lot lot) {
        StatutLot statut = lot.getStatut();
        if (statut == null) return;
        if (statut == StatutLot.EN_TRAITEMENT) {
            saisirHuile(context, repository, lot);
        } else if (statut == StatutLot.PRET_A_RECUPERER) {
            confirmerRecuperation(context, repository, lot);
        } else {
            confirmerAvancement(context, repository, lot);
        }
    }

    private static void confirmerAvancement(Context context, LotRepository repository, Lot lot) {
        StatutLot suivant = lot.getStatut().suivant();
        if (suivant == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle(lot.getStatut().getActionSuivante())
                .setMessage(context.getString(R.string.avancer_message, lot.getNumero(), suivant.getLibelle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.valider, (d, w) -> repository.avancerStatut(lot, new Callback<StatutLot>() {
                    @Override
                    public void onSuccess(StatutLot result) {
                        toast(context, context.getString(R.string.statut_mis_a_jour, lot.getNumero(), result.getLibelle()));
                    }

                    @Override
                    public void onError(String message) {
                        toast(context, message);
                    }
                }))
                .show();
    }

    /** US-17 : saisie des litres, rendement affiché en direct. */
    private static void saisirHuile(Context context, LotRepository repository, Lot lot) {
        DialogHuileBinding b = DialogHuileBinding.inflate(LayoutInflater.from(context));
        b.txtInfoLot.setText(context.getString(R.string.huile_info_lot, lot.getNumero(),
                Formats.kg(lot.getPoidsKg()), lot.getVariete()));
        b.txtRendement.setText(R.string.rendement_vide);
        if (b.inputLitres.getEditText() != null) {
            b.inputLitres.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
                @Override public void onTextChanged(CharSequence s, int st, int bf, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    double litres = litres(s.toString());
                    b.txtRendement.setText(litres > 0 && lot.getPoidsKg() > 0
                            ? String.format(Locale.FRENCH, "Rendement : %.1f %%", litres / lot.getPoidsKg() * 100)
                            : context.getString(R.string.rendement_vide));
                }
            });
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.huile_titre)
                .setView(b.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.valider, null)
                .create();
        dialog.setOnShowListener(di -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double litres = litres(Validators.text(b.inputLitres));
            // Rendement plausible : au plus 40 L pour 100 kg d'olives
            boolean ok = Validators.check(b.inputLitres, litres > 0 && litres <= lot.getPoidsKg() * 0.4,
                    context.getString(R.string.err_litres));
            if (!ok) return;
            dialog.dismiss();
            repository.enregistrerHuile(lot, litres, new Callback<Lot>() {
                @Override
                public void onSuccess(Lot result) {
                    toast(context, context.getString(R.string.huile_ok, result.getNumero(),
                            String.format(Locale.FRENCH, "%.1f", result.calculerRendement())));
                }

                @Override
                public void onError(String message) {
                    toast(context, message);
                }
            });
        }));
        dialog.show();
    }

    /** US-18 : date de récupération effective. */
    private static void confirmerRecuperation(Context context, LotRepository repository, Lot lot) {
        if (!lot.isEnAttenteDeRecuperation()) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.recuperation_titre)
                .setMessage(context.getString(R.string.recuperation_message, lot.getNumero(),
                        lot.getQuantiteHuileL() != null ? Formats.nombre(lot.getQuantiteHuileL()) : "—",
                        lot.getNomProducteur()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.valider, (d, w) -> repository.enregistrerRecuperation(lot, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        toast(context, context.getString(R.string.recuperation_ok, lot.getNumero()));
                    }

                    @Override
                    public void onError(String message) {
                        toast(context, message);
                    }
                }))
                .show();
    }

    private static double litres(@Nullable String s) {
        try {
            return s == null || s.trim().isEmpty() ? 0 : Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
