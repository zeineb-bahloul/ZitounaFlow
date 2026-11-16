package com.example.zitounaproject.ui.agriculteur;

import android.os.Bundle;
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
import com.example.zitounaproject.databinding.FragmentMesLotsBinding;
import com.example.zitounaproject.model.Lot;
import com.example.zitounaproject.ui.common.LotAdapter;
import com.example.zitounaproject.ui.common.LotDetailActivity;
import com.example.zitounaproject.util.Formats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** US-15 / US-16 : l'agriculteur suit ses lots, sa position et son attente estimée (BF-12, BF-13). */
public class MesLotsFragment extends Fragment implements LotAdapter.Listener {

    private FragmentMesLotsBinding binding;
    private LotAdapter adapter;
    private final List<Lot> tous = new ArrayList<>();
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
        binding.chipGroupFiltre.setOnCheckedStateChangeListener((g, ids) -> afficher());
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

    private void afficher() {
        boolean enCours = binding.chipGroupFiltre.getCheckedChipId() == R.id.chipEnCours;
        List<Lot> filtres = new ArrayList<>();
        double kgTotal = 0;
        int actifs = 0;
        for (Lot l : tous) {
            kgTotal += l.getPoidsKg();
            boolean actif = l.getStatut() != null && l.getStatut().estDansLaFile();
            if (actif) actifs++;
            if (enCours && !actif) continue;
            filtres.add(l);
        }
        binding.txtNbLots.setText(String.valueOf(tous.size()));
        binding.txtKgTotal.setText(Formats.nombre(kgTotal));
        binding.txtActifs.setText(String.valueOf(actifs));
        adapter.submit(filtres, null);
        binding.layoutEmpty.setVisibility(filtres.isEmpty() ? View.VISIBLE : View.GONE);
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
