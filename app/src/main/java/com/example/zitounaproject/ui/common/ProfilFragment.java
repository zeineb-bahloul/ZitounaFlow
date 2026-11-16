package com.example.zitounaproject.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.AuthRepository;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.UserSession;
import com.example.zitounaproject.databinding.FragmentProfilBinding;
import com.example.zitounaproject.model.Agriculteur;
import com.example.zitounaproject.model.Gestionnaire;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.auth.OnboardingActivity;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** US-06 : consulter et modifier son profil (BF-04), se déconnecter (BF-02). */
public class ProfilFragment extends Fragment {

    private FragmentProfilBinding binding;
    private final AuthRepository repository = new AuthRepository();
    private Utilisateur utilisateur;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfilBinding.inflate(inflater, container, false);

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
        binding.btnResetPassword.setOnClickListener(v -> sendReset());

        utilisateur = UserSession.get();
        if (utilisateur != null) {
            bind(utilisateur);
        } else {
            setLoading(true);
            repository.loadCurrentUser(new Callback<Utilisateur>() {
                @Override
                public void onSuccess(Utilisateur result) {
                    if (binding == null) return;
                    setLoading(false);
                    utilisateur = result;
                    bind(result);
                }

                @Override
                public void onError(String message) {
                    if (binding == null) return;
                    setLoading(false);
                    toast(message);
                }
            });
        }
        return binding.getRoot();
    }

    private void bind(Utilisateur u) {
        binding.txtAvatar.setText(u.getInitiales());
        binding.txtNomComplet.setText(u.getNomComplet());
        binding.txtRole.setText(u.getRole() != null ? u.getRole().getLibelle() : "");
        binding.txtEmail.setText(u.getEmail());
        if (u.getDateCreation() != null) {
            String date = new SimpleDateFormat("d MMMM yyyy", Locale.FRENCH).format(u.getDateCreation());
            binding.txtMembreDepuis.setText(getString(R.string.membre_depuis, date));
        }

        setText(binding.inputNom, u.getNom());
        setText(binding.inputPrenom, u.getPrenom());
        setText(binding.inputTelephone, u.getTelephone());

        boolean isAgriculteur = u instanceof Agriculteur;
        binding.groupAgriculteur.setVisibility(isAgriculteur ? View.VISIBLE : View.GONE);
        binding.groupGestionnaire.setVisibility(isAgriculteur ? View.GONE : View.VISIBLE);
        if (u instanceof Agriculteur) {
            setText(binding.inputExploitation, ((Agriculteur) u).getNomExploitation());
            setText(binding.inputAdresse, ((Agriculteur) u).getAdresse());
        } else if (u instanceof Gestionnaire) {
            setText(binding.inputMatricule, ((Gestionnaire) u).getMatricule());
        }
    }

    private void save() {
        if (utilisateur == null) return;
        String nom = Validators.text(binding.inputNom);
        String prenom = Validators.text(binding.inputPrenom);
        String telephone = Validators.text(binding.inputTelephone).replace(" ", "");
        String required = getString(R.string.err_required);

        boolean ok = Validators.check(binding.inputNom, !nom.isEmpty(), required);
        ok &= Validators.check(binding.inputPrenom, !prenom.isEmpty(), required);
        ok &= Validators.check(binding.inputTelephone, Validators.isPhone(telephone), getString(R.string.err_phone));

        String exploitation = Validators.text(binding.inputExploitation);
        String adresse = Validators.text(binding.inputAdresse);
        String matricule = Validators.text(binding.inputMatricule);
        if (utilisateur instanceof Agriculteur) {
            ok &= Validators.check(binding.inputExploitation, !exploitation.isEmpty(), required);
            ok &= Validators.check(binding.inputAdresse, !adresse.isEmpty(), required);
        } else {
            ok &= Validators.check(binding.inputMatricule, !matricule.isEmpty(), required);
        }
        if (!ok) return;

        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setTelephone(telephone);
        if (utilisateur instanceof Agriculteur) {
            ((Agriculteur) utilisateur).setNomExploitation(exploitation);
            ((Agriculteur) utilisateur).setAdresse(adresse);
        } else if (utilisateur instanceof Gestionnaire) {
            ((Gestionnaire) utilisateur).setMatricule(matricule);
        }

        setLoading(true);
        repository.updateProfile(utilisateur, new Callback<Utilisateur>() {
            @Override
            public void onSuccess(Utilisateur result) {
                if (binding == null) return;
                setLoading(false);
                bind(result);
                toast(getString(R.string.profil_saved));
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                setLoading(false);
                toast(message);
            }
        });
    }

    private void sendReset() {
        if (utilisateur == null || utilisateur.getEmail() == null) return;
        String email = utilisateur.getEmail();
        repository.resetPassword(email, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) toast(getString(R.string.reset_sent, email));
            }

            @Override
            public void onError(String message) {
                if (isAdded()) toast(message);
            }
        });
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    repository.logout();
                    Intent intent = new Intent(requireContext(), OnboardingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private void setText(TextInputLayout layout, @Nullable String value) {
        if (layout.getEditText() != null) {
            layout.getEditText().setText(value == null ? "" : value);
        }
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
