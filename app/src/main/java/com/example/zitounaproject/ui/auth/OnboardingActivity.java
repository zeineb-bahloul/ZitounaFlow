package com.example.zitounaproject.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import com.example.zitounaproject.databinding.ActivityOnboardingBinding;
import com.example.zitounaproject.model.Role;
import com.example.zitounaproject.ui.common.BaseActivity;

/** US-02 : écran de présentation (maquette 1). */
public class OnboardingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOnboardingBinding binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.btnStart.setOnClickListener(v -> openRegister(Role.AGRICULTEUR));
        binding.btnGestionnaire.setOnClickListener(v -> openRegister(Role.GESTIONNAIRE));
        binding.txtLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        binding.txtSkip.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void openRegister(Role role) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra(RegisterActivity.EXTRA_ROLE, role.name());
        startActivity(intent);
    }
}
