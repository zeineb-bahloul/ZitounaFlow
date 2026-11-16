package com.example.zitounaproject.ui.gestionnaire;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.LotRepository;
import com.example.zitounaproject.databinding.FragmentStatistiquesBinding;
import com.example.zitounaproject.databinding.ItemVarieteRendementBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.model.LotFilter;
import com.example.zitounaproject.model.QualiteOlive;
import com.example.zitounaproject.model.Statistiques;
import com.example.zitounaproject.util.DateUtils;
import com.example.zitounaproject.util.Formats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** US-22 : statistiques de l'huilerie sur une période (BF-28) — maquette « Statistiques & Rendement ». */
public class StatistiquesFragment extends Fragment {

    private FragmentStatistiquesBinding binding;
    private final List<Lot> lots = new ArrayList<>();
    @Nullable private Statistiques stats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatistiquesBinding.inflate(inflater, container, false);
        binding.txtCampagne.setText(getString(R.string.campagne_n_simple, LotFilter.campagneCourante()));
        binding.chipGroupPeriode.setOnCheckedStateChangeListener((g, ids) -> calculer());
        binding.chipGroupSerie.setOnCheckedStateChangeListener((g, ids) -> afficherGraphique());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.progress.setVisibility(View.VISIBLE);
        new LotRepository().chargerHistorique(new Callback<List<Lot>>() {
            @Override
            public void onSuccess(List<Lot> result) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                lots.clear();
                lots.addAll(result);
                calculer();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Statistiques.calculer(période) selon la puce choisie. */
    private void calculer() {
        int id = binding.chipGroupPeriode.getCheckedChipId();
        Date demain = DateUtils.addDays(DateUtils.today(), 1);
        Date debut;
        Statistiques.Granularite g;
        String titre;
        if (id == R.id.chipJour) {
            debut = DateUtils.today();
            g = Statistiques.Granularite.HEURE;
            titre = getString(R.string.chart_heure);
        } else if (id == R.id.chip30) {
            debut = DateUtils.addDays(DateUtils.today(), -29);
            g = Statistiques.Granularite.JOUR;
            titre = getString(R.string.chart_jour);
        } else if (id == R.id.chipCampagneStats) {
            debut = debutCampagne();
            g = Statistiques.Granularite.MOIS;
            titre = getString(R.string.chart_mois);
        } else {
            debut = DateUtils.addDays(DateUtils.today(), -6);
            g = Statistiques.Granularite.JOUR;
            titre = getString(R.string.chart_jour);
        }
        stats = Statistiques.calculer(lots, debut, demain, g);
        binding.txtPeriode.setText(getString(R.string.periode_du_au,
                DateUtils.jourCourt(debut), DateUtils.jourCourt(DateUtils.addDays(demain, -1))));
        binding.txtChartTitre.setText(titre);
        afficher();
    }

    /** 1er septembre de la campagne en cours. */
    private static Date debutCampagne() {
        Calendar c = Calendar.getInstance();
        int debut = Integer.parseInt(LotFilter.campagneCourante().substring(0, 4));
        c.clear();
        c.set(debut, Calendar.SEPTEMBER, 1);
        return c.getTime();
    }

    private void afficher() {
        if (stats == null) return;
        Statistiques s = stats;
        binding.txtKpiHuile.setText(Formats.nombre(s.huileTotaleL));
        binding.txtKpiOlives.setText(String.format(Locale.FRENCH, "%.1f", s.poidsTotalKg / 1000));
        binding.txtKpiLots.setText(String.valueOf(s.nbLots));
        binding.txtKpiPresses.setText(getString(R.string.kpi_presses, s.nbLotsPresses));
        binding.txtKpiRendement.setText(s.poidsPresseKg > 0 ? String.format(Locale.FRENCH, "%.1f", s.rendementMoyen) : "—");
        binding.txtKpiAttente.setText(s.nbLotsAvecAttente > 0 ? Formats.duree(s.tempsAttenteMoyenMin / 60.0) : "—");

        afficherGraphique();
        afficherQualite(s);
        afficherVarietes(s);
    }

    private void afficherGraphique() {
        if (stats == null || binding == null) return;
        boolean kg = binding.chipGroupSerie.getCheckedChipId() == R.id.chipSerieKg;
        List<String> labels = new ArrayList<>();
        List<Double> valeurs = new ArrayList<>();
        for (Statistiques.Barre b : stats.barres) {
            labels.add(b.label);
            valeurs.add(kg ? b.poidsKg : b.nbLots);
        }
        binding.chartBarres.setDonnees(labels, valeurs, kg ? "kg" : "lots");
    }

    /** Répartition de la qualité (analyses IA validées au sprint 6). */
    private void afficherQualite(Statistiques s) {
        int excellente = s.repartitionQualite.get(QualiteOlive.EXCELLENTE);
        int acceptable = s.repartitionQualite.get(QualiteOlive.ACCEPTABLE);
        int defectueuse = s.repartitionQualite.get(QualiteOlive.DEFECTUEUSE);
        int analyses = excellente + acceptable + defectueuse;

        List<Float> valeurs = new ArrayList<>();
        List<Integer> couleurs = new ArrayList<>();
        valeurs.add((float) excellente);
        couleurs.add(ContextCompat.getColor(requireContext(), QualiteOlive.EXCELLENTE.getCouleur()));
        valeurs.add((float) acceptable);
        couleurs.add(ContextCompat.getColor(requireContext(), QualiteOlive.ACCEPTABLE.getCouleur()));
        valeurs.add((float) defectueuse);
        couleurs.add(ContextCompat.getColor(requireContext(), QualiteOlive.DEFECTUEUSE.getCouleur()));
        binding.donutQualite.setDonnees(valeurs, couleurs);

        binding.txtQualiteCentre.setText(analyses > 0 ? Math.round(excellente * 100f / analyses) + "%" : "—");
        binding.txtLegExcellente.setText(legende(QualiteOlive.EXCELLENTE.getLibelle(), excellente, analyses));
        binding.txtLegAcceptable.setText(legende(QualiteOlive.ACCEPTABLE.getLibelle(), acceptable, analyses));
        binding.txtLegDefectueuse.setText(legende(QualiteOlive.DEFECTUEUSE.getLibelle(), defectueuse, analyses));
        binding.txtLegNonAnalyses.setText(getString(R.string.non_analyses, s.nbNonAnalyses));
    }

    private String legende(String libelle, int n, int total) {
        int pct = total > 0 ? Math.round(n * 100f / total) : 0;
        return libelle + " — " + n + " lot" + (n > 1 ? "s" : "") + " (" + pct + " %)";
    }

    /** Rendement par variété, du meilleur au moins bon. */
    private void afficherVarietes(Statistiques s) {
        binding.layoutVarietes.removeAllViews();
        Map<String, Double> rendements = s.rendementParVariete();
        binding.txtVarietesVide.setVisibility(rendements.isEmpty() ? View.VISIBLE : View.GONE);
        int rang = 1;
        for (Map.Entry<String, Double> e : rendements.entrySet()) {
            ItemVarieteRendementBinding item = ItemVarieteRendementBinding.inflate(
                    getLayoutInflater(), binding.layoutVarietes, false);
            double[] cumul = s.parVariete.get(e.getKey());
            item.txtRang.setText(String.valueOf(rang++));
            item.txtVariete.setText(e.getKey());
            item.txtDetail.setText(getString(R.string.variete_detail,
                    Formats.kg(cumul != null ? cumul[1] : 0), Formats.nombre(cumul != null ? cumul[0] : 0)));
            item.txtRendement.setText(String.format(Locale.FRENCH, "%.1f %%", e.getValue()));
            // Barre : 30 % de rendement = barre pleine
            item.progressRendement.setProgress((int) Math.min(100, Math.round(e.getValue() / 30 * 100)));
            binding.layoutVarietes.addView(item.getRoot());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
