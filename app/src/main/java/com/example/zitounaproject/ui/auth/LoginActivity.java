package com.example.zitounaproject.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.AuthRepository;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.databinding.ActivityLoginBinding;
import com.example.zitounaproject.databinding.DialogResetPasswordBinding;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.BaseActivity;
import com.example.zitounaproject.util.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** US-04 (connexion) et US-05 (mot de passe oublié) — maquette 2. */
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private final AuthRepository repository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.toggleRole.check(R.id.btnRoleAgriculteur);
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.txtForgot.setOnClickListener(v -> showResetDialog());
        binding.txtRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra(RegisterActivity.EXTRA_ROLE, selectedRole().name());
            startActivity(intent);
        });
    }

    private Role selectedRole() {
        return binding.toggleRole.getCheckedButtonId() == R.id.btnRoleGestionnaire
                ? Role.GESTIONNAIRE : Role.AGRICULTEUR;
    }

    private void attemptLogin() {
        String email = Validators.text(binding.inputEmail);
        String password = Validators.text(binding.inputPassword);

        boolean ok = Validators.check(binding.inputEmail, Validators.isEmail(email), getString(R.string.err_email));
        ok &= Validators.check(binding.inputPassword, !password.isEmpty(), getString(R.string.err_required));
        if (!ok) return;

        setLoading(true);
        Role expected = selectedRole();
        repository.login(email, password, new Callback<Utilisateur>() {
            @Override
            public void onSuccess(Utilisateur utilisateur) {
                if (utilisateur.getRole() != expected) {
                    // Le compte existe mais l'utilisateur a choisi l'autre profil
                    repository.logout();
                    setLoading(false);
                    toast(getString(R.string.err_wrong_role, expected.getLibelle().toLowerCase()));
                    return;
                }
                openHomeFor(utilisateur);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                toast(message);
            }
        });
    }

    private void showResetDialog() {
        DialogResetPasswordBinding dialogBinding =
                DialogResetPasswordBinding.inflate(LayoutInflater.from(this));
        String typed = Validators.text(binding.inputEmail);
        if (dialogBinding.inputResetEmail.getEditText() != null) {
            dialogBinding.inputResetEmail.getEditText().setText(typed);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_message)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset_send, (dialog, which) -> {
                    String email = Validators.text(dialogBinding.inputResetEmail);
                    if (!Validators.isEmail(email)) {
                        toast(getString(R.string.err_email));
                        return;
                    }
                    repository.resetPassword(email, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            toast(getString(R.string.reset_sent, email));
                        }

                        @Override
                        public void onError(String message) {
                            toast(message);
                        }
                    });
                })
                .show();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }
}
