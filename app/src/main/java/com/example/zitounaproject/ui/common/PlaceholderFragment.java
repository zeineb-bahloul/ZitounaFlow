package com.example.zitounaproject.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zitounaproject.R;
import com.example.zitounaproject.databinding.FragmentPlaceholderBinding;

/** Écran provisoire pour les onglets développés dans les sprints suivants. */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_ICON = "icon";
    private static final String ARG_SPRINT = "sprint";

    public static PlaceholderFragment newInstance(String title, @DrawableRes int icon, int sprint) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_ICON, icon);
        args.putInt(ARG_SPRINT, sprint);
        PlaceholderFragment fragment = new PlaceholderFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentPlaceholderBinding binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        Bundle args = requireArguments();
        binding.txtTitle.setText(args.getString(ARG_TITLE));
        binding.imgIcon.setImageResource(args.getInt(ARG_ICON, R.drawable.ic_leaf));
        binding.txtMessage.setText(getString(R.string.prevu_sprint, args.getInt(ARG_SPRINT)));
        return binding.getRoot();
    }
}
