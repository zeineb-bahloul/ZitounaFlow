package com.example.zitounaproject.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.zitounaproject.R;
import com.example.zitounaproject.model.Notification;
import com.example.zitounaproject.ui.common.NotificationsActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * US-24 : affiche les nouvelles notifications ZitounaFlow dans la barre de notifications Android
 * (tant que l'application est ouverte ou en arrière-plan). Chaque notification n'est affichée qu'une fois.
 */
public final class SystemNotifier {

    public static final String CANAL_ID = "zitouna_suivi";
    private static final String PREFS = "zitouna_notifications";
    private static final String KEY_AFFICHEES = "affichees";
    private static final String KEY_INITIALISE = "initialise_";

    private SystemNotifier() {
    }

    public static void creerCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL_ID,
                    context.getString(R.string.canal_notifications), NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription(context.getString(R.string.canal_notifications_desc));
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    /**
     * Affiche les notifications non lues jamais affichées.
     * Au tout premier chargement pour un utilisateur, l'existant est seulement mémorisé (pas d'avalanche).
     */
    public static void afficherNouvelles(Context context, String uid, List<Notification> notifications) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> affichees = new HashSet<>(prefs.getStringSet(KEY_AFFICHEES, new HashSet<>()));
        boolean initialise = prefs.getBoolean(KEY_INITIALISE + uid, false);

        for (Notification n : notifications) {
            if (n.getId() == null || affichees.contains(n.getId())) continue;
            affichees.add(n.getId());
            if (initialise && !n.isLue()) afficher(context, n);
        }
        prefs.edit().putStringSet(KEY_AFFICHEES, affichees).putBoolean(KEY_INITIALISE + uid, true).apply();
    }

    private static void afficher(Context context, Notification n) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent intent = new Intent(context, NotificationsActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, n.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_leaf)
                .setColor(ContextCompat.getColor(context, R.color.olive_700))
                .setContentTitle(n.getTitre())
                .setContentText(n.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(n.getMessage()))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(n.getId().hashCode(), builder.build());
        } catch (SecurityException ignored) {
            // Permission retirée entre-temps
        }
    }
}
