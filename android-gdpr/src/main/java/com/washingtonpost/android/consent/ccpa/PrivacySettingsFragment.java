/*
 * Copyright (c) 2019. The Washington Post
 */

package com.washingtonpost.android.consent.ccpa;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.MetricAffectingSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wapo.text.WpLinkAppearanceSpan;
import com.washingtonpost.android.config.domain.models.config.PrivacyConsentConfig;
import com.washingtonpost.android.gdpr.R;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

abstract public class PrivacySettingsFragment extends Fragment {

    private PrivacySettingsListener privacySettingsListener;
    private TextView account;
    boolean isUserSignedIn;
    private SwitchCompat switchView;
    private TextView switchText;
    private PrivacyConsentConfig privacyConsentConfig;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        privacySettingsListener = getPrivacySettingsListener();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy_settings, container, false);
        privacyConsentConfig = PrivacyConsentConfig.getConfig(requireContext(), requireArguments().getParcelable("config"));
        isUserSignedIn = requireArguments().getBoolean("isSignedIn", false);
        CharSequence headerText = privacyConsentConfig.getTitleText();
        CharSequence bodyText = setLinkClickCallback(Html.fromHtml(privacyConsentConfig.getBodyText()));
        CharSequence accountText = setSignInLinkClickCallback(Html.fromHtml(privacyConsentConfig.getAccountText()), getString(com.washingtonpost.android.config.R.string.privacy_settings_account_span));
        final CharSequence switchTextOn = privacyConsentConfig.getSwitchTextOn();
        final CharSequence switchTextOff = privacyConsentConfig.getSwitchTextOff();
        CharSequence bottomText = setLinkClickCallback(Html.fromHtml(privacyConsentConfig.getBottomText()));

        TextView toolbarText = view.findViewById(R.id.tv_privacy_settings_toolbar_text);
        TextView header = view.findViewById(R.id.tv_privacy_settings_header);
        TextView body = view.findViewById(R.id.tv_privacy_settings_body);
        account = view.findViewById(R.id.tv_privacy_settings_account);
        switchView = view.findViewById(R.id.sw_privacy_settings_switch);
        switchText = view.findViewById(R.id.tv_privacy_settings_switch_text);
        TextView bottomParagraph = view.findViewById(R.id.tv_privacy_settings_bottom_text);

        header.setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "PostoniWide-Bold.otf"));
        body.setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "Georgia-Regular.otf"));
        account.setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "Franklin-ITC-Pro-Light.otf"));
        switchText.setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "Franklin-ITC-Pro-Bold.otf"));
        bottomParagraph.setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "Georgia-Regular.otf"));

        header.setText(headerText);
        body.setMovementMethod(LinkMovementMethod.getInstance());
        body.setText(bodyText);
        account.setText(accountText);
        account.setMovementMethod(LinkMovementMethod.getInstance());
        bottomParagraph.setMovementMethod(LinkMovementMethod.getInstance());
        bottomParagraph.setText(bottomText);

        if (privacySettingsListener != null) {
            updateSwitch(privacySettingsListener.getOptOutStatus());
        }

        toolbarText.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchText.setText(switchTextOn);
            } else {
                switchText.setText(switchTextOff);
            }
            if (privacySettingsListener != null) {
                privacySettingsListener.privacySettingsSwitchChanged(isChecked);
            }
        });

        switchText.setOnClickListener(v -> switchView.setChecked(!switchView.isChecked()));

        updateViewsOnUserSignIn(isUserSignedIn);

        return view;
    }

    private void updateSwitch(boolean optOutStatus) {
        if (optOutStatus) {
            switchView.setChecked(false);
            switchText.setText(privacyConsentConfig.getSwitchTextOff());
        } else {
            switchView.setChecked(true);
            switchText.setText(privacyConsentConfig.getSwitchTextOn());
        }
    }

    public void updateViewsOnUserSignIn(boolean isUserSingedIn) {
        account.setVisibility(isUserSingedIn ? View.GONE : View.VISIBLE);
        if (privacySettingsListener != null) {
            updateSwitch(privacySettingsListener.getOptOutStatus());
        }
    }

    public interface PrivacySettingsListener {
        void privacySettingsUrlClicked(String url);
        void privacySettingsSwitchChanged(boolean isChecked);
        boolean getOptOutStatus();
        void privacySettingsSignInClicked();
    }

    private CharSequence setLinkClickCallback(CharSequence charSequence) {
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(charSequence);
        URLSpan[] urls = strBuilder.getSpans(0, charSequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            final String url = span.getURL();
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            WpLinkAppearanceSpan clickable = new WpLinkAppearanceSpan(getContext(), true) {
                public void onClick(@NonNull View view) {
                    if (privacySettingsListener != null) {
                        privacySettingsListener.privacySettingsUrlClicked(url);
                    }
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
        return strBuilder;
    }

    private CharSequence setSignInLinkClickCallback(CharSequence charSequence, CharSequence textToSpan) {
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(charSequence);
        int start = charSequence.toString().indexOf(textToSpan.toString());
        int end = start + textToSpan.toString().length();
        if (start >= 0 && end <= charSequence.toString().length()) {
            WpLinkAppearanceSpan clickable = new WpLinkAppearanceSpan(getContext(), true){
                public void onClick(@NonNull View view) {
                    if (privacySettingsListener != null) {
                        privacySettingsListener.privacySettingsSignInClicked();
                    }
                }
            };
            strBuilder.setSpan(new CustomTypefaceSpan(Typeface.createFromAsset(requireActivity().getAssets(), "Franklin-ITC-Pro-Bold.otf")), start, end, SPAN_EXCLUSIVE_EXCLUSIVE);
            strBuilder.setSpan(clickable, start, end, SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return strBuilder;
    }

    private static class CustomTypefaceSpan extends MetricAffectingSpan {
        private final Typeface typeface;

        public CustomTypefaceSpan(Typeface typeface) {
            this.typeface = typeface;
        }

        @Override
        public void updateMeasureState(@NonNull TextPaint textPaint) {
            textPaint.setTypeface(typeface);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setTypeface(typeface);
        }
    }

    /**
     * Sub Classes extend this class and implement this method to provide [PrivacySettingsListener] object
     * for the callbacks.
     * @return PrivacySettingsListener
     */
    abstract public PrivacySettingsListener getPrivacySettingsListener();
}