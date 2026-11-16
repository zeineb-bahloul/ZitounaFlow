package com.example.zitounaproject.ui.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.zitounaproject.data.Callback;
import com.example.zitounaproject.data.NotificationRepository;
import com.example.zitounaproject.databinding.ActivityNotificationsBinding;
import com.example.zitounaproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/** US-11 : liste des notifications reçues (BF-05). */
public class NotificationsActivity extends BaseActivity {

    private ActivityNotificationsBinding binding;
    private final NotificationRepository repository = new NotificationRepository();
    private final NotificationAdapter adapter = new NotificationAdapter();
    @Nullable private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }
        registration = repository.ecouter(uid, new Callback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> result) {
                adapter.submit(result);
                binding.txtEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
                // Ouvrir l'écran marque les notifications comme lues
                repository.marquerCommeLues(result);
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
