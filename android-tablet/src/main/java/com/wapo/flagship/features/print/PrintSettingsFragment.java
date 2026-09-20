package com.wapo.flagship.features.print;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.data.ArchiveManager;
import com.washingtonpost.android.R;

import java.util.Arrays;

public class PrintSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String FRAGMENT_TAG = PrintSettingsFragment.class.getSimpleName() + ".printSettingsFragmentTag";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_print);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(R.color.settings_bg));
    }

    public static Fragment create() {
        return new PrintSettingsFragment();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        ArchiveManager am = FlagshipApplication.getInstance().getArchiveManager();
        if (am != null && Arrays.asList(am.getSectionsList()).contains(s)) {
            am.lockSection(s);
        }
    }
}
