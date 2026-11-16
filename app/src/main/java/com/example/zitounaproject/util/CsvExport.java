package com.example.zitounaproject.util;

import android.content.Context;
import android.content.Intent;

import com.example.zitounaproject.model.Lot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/** Export du registre des lots au format CSV (maquette « Exporter le registre »). */
public final class CsvExport {

    private CsvExport() {
    }

    public static String lots(List<Lot> lots) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
        StringBuilder sb = new StringBuilder();
        sb.append("Numero;Producteur;Telephone;Variete;Reception;Poids net (kg);Statut;Huile (L);Rendement (%);Recuperation\n");
        for (Lot l : lots) {
            sb.append(c(l.getNumero())).append(';')
                    .append(c(l.getNomProducteur())).append(';')
                    .append(c(l.getTelephone())).append(';')
                    .append(c(l.getVariete())).append(';')
                    .append(l.getDateReception() != null ? f.format(l.getDateReception()) : "").append(';')
                    .append(String.format(Locale.FRENCH, "%.0f", l.getPoidsKg())).append(';')
                    .append(l.getStatut() != null ? l.getStatut().getLibelle() : "").append(';')
                    .append(l.getQuantiteHuileL() != null ? String.format(Locale.FRENCH, "%.1f", l.getQuantiteHuileL()) : "").append(';')
                    .append(l.getQuantiteHuileL() != null ? String.format(Locale.FRENCH, "%.1f", l.calculerRendement()) : "").append(';')
                    .append(l.getDateRecuperation() != null ? f.format(l.getDateRecuperation()) : "")
                    .append('\n');
        }
        return sb.toString();
    }

    private static String c(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ");
    }

    /** Partage le CSV (e-mail, Drive, WhatsApp…). */
    public static void partager(Context context, String titre, List<Lot> lots) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, titre);
        send.putExtra(Intent.EXTRA_TEXT, lots(lots));
        context.startActivity(Intent.createChooser(send, titre));
    }
}
