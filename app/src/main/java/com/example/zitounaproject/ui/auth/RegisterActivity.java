package com.example.zitounaproject.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.zitounaproject.R;
import com.example.zitounaproject.data.AuthRepository;
import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.databinding.ActivityRegisterBinding;
import com.example.zitounaproject.model.Agriculteur;
import com.example.zitounaproject.model.Gestionnaire;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.model.Utilisateur;
import com.example.zitounaproject.ui.common.BaseActivity;
import com.example.zitounaproject.util.Validators;

/** US-03 : création de compte (BF-01). */
public class RegisterActivity extends BaseActivity {

    public static final String EXTRA_ROLE = "extra_role";

    private ActivityRegisterBinding binding;
    private final AuthRepository repository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        String roleExtra = getIntent().getStringExtra(EXTRA_ROLE);
        binding.toggleRole.check(Role.GESTIONNAIRE.name().equals(roleExtra)
                ? R.id.btnRoleGestionnaire : R.id.btnRoleAgriculteur);
        binding.toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateRoleFields();
        });
        updateRoleFields();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.txtLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private Role selectedRole() {
        return binding.toggleRole.getCheckedButtonId() == R.id.btnRoleGestionnaire
                ? Role.GESTIONNAIRE : Role.AGRICULTEUR;
    }

    private void updateRoleFields() {
        boolean gestionnaire = selectedRole() == Role.GESTIONNAIRE;
        binding.groupAgriculteur.setVisibility(gestionnaire ? View.GONE : View.VISIBLE);
        binding.groupGestionnaire.setVisibility(gestionnaire ? View.VISIBLE : View.GONE);
    }

    private void attemptRegister() {
        String nom = Validators.text(binding.inputNom);
        String prenom = Validators.text(binding.inputPrenom);
        String telephone = Validators.text(binding.inputTelephone).replace(" ", "");
        String email = Validators.text(binding.inputEmail);
        String password = Validators.text(binding.inputPassword);
        String confirm = Validators.text(binding.inputPasswordConfirm);
        Role role = selectedRole();

        String required = getString(R.string.err_required);
        boolean ok = Validators.check(binding.inputNom, !nom.isEmpty(), required);
        ok &= Validators.check(binding.inputPrenom, !prenom.isEmpty(), required);
        ok &= Validators.check(binding.inputTelephone, Validators.isPhone(telephone), getString(R.string.err_phone));
        ok &= Validators.check(binding.inputEmail, Validators.isEmail(email), getString(R.string.err_email));
        ok &= Validators.check(binding.inputPassword, Validators.isPassword(password), getString(R.string.err_password));
        ok &= Validators.check(binding.inputPasswordConfirm, password.equals(confirm), getString(R.string.err_password_match));

        Utilisateur utilisateur;
        if (role == Role.GESTIONNAIRE) {
            String matricule = Validators.text(binding.inputMatricule);
            ok &= Validators.check(binding.inputMatricule, !matricule.isEmpty(), required);
            Gestionnaire g = new Gestionnaire();
            g.setMatricule(matricule);
            utilisateur = g;
        } else {
            String exploitation = Validators.text(binding.inputExploitation);
            String adresse = Validators.text(binding.inputAdresse);
            ok &= Validators.check(binding.inputExploitation, !exploitation.isEmpty(), required);
            ok &= Validators.check(binding.inputAdresse, !adresse.isEmpty(), required);
            Agriculteur a = new Agriculteur();
            a.setNomExploitation(exploitation);
            a.setAdresse(adresse);
            utilisateur = a;
        }
        if (!ok) return;

        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setTelephone(telephone);
        utilisateur.setEmail(email);

        setLoading(true);
        repository.register(utilisateur, password, new Callback<Utilisateur>() {
            @Override
            public void onSuccess(Utilisateur result) {
                openHomeFor(result);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                toast(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }
}
