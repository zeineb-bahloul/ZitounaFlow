package com.example.zitounaproject.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zitounaproject.R;
import com.example.zitounaproject.databinding.ItemNotificationBinding;
import com.example.zitounaproject.model.Notification;
import com.example.zitounaproject.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {

    private final List<Notification> items = new ArrayList<>();

    public void submit(List<Notification> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Notification n = items.get(position);
        ItemNotificationBinding b = holder.binding;
        b.txtTitre.setText(n.getTitre());
        b.txtMessage.setText(n.getMessage());
        b.txtDate.setText(DateUtils.dateHeure(n.getDateEnvoi()));
        b.dotNonLue.setVisibility(n.isLue() ? View.INVISIBLE : View.VISIBLE);
        boolean refus = Notification.TYPE_RDV_REFUSE.equals(n.getType())
                || Notification.TYPE_RDV_ANNULE.equals(n.getType());
        boolean lot = Notification.TYPE_LOT.equals(n.getType())
                || Notification.TYPE_RDV_DEMANDE.equals(n.getType());
        b.imgIcon.setImageResource(refus ? R.drawable.ic_close
                : Notification.TYPE_RDV_DEMANDE.equals(n.getType()) ? R.drawable.ic_calendar
                : lot ? R.drawable.ic_leaf : R.drawable.ic_check);
        b.iconBg.setBackgroundResource(refus ? R.drawable.bg_circle_red_light
                : lot ? R.drawable.bg_circle_gold_light : R.drawable.bg_circle_olive_light);
        b.imgIcon.setColorFilter(b.getRoot().getContext().getColor(
                refus ? R.color.error : lot ? R.color.gold_dark : R.color.olive_700));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemNotificationBinding binding;

        Holder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
