/*
 * Copyright (c) 2018 The Washington Post. All rights reserved.
 */

package com.washingtonpost.android.gdpr;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by adkinsj on 5/9/18.
 */
public class ConsentWallFragment extends DialogFragment {

    private final String TAG = "ConsentWallFragment";

    public static final String NORMAL = "Franklin-ITC-Pro-Light.otf";
    public static final String BOLD = "Franklin-ITC-Pro-Bold.otf";
    public static final String ITALIC = "FranklinITCStd-LightItalic.otf";
    public static final String HEADER = "PostoniWide-Bold.otf";

    public static final String HEADER_TYPEFACE = "header_typeface";
    public static final String DESCRIPTION_TYPEFACE = "description_typeface";
    public static final String CONTENT_TYPEFACE = "content_typeface";
    public static final String SWITCH_TYPEFACE = "switch_typeface";
    public static final String BUTTON_TYPEFACE = "button_typeface";
    public static final String PRIVACY_POLICY_TYPEFACE = "privacy_policy_typeface";
    public static final String TOS_TYPEFACE = "tos_typeface";
    public static final String PARTNERS_TYPEFACE = "partners_typeface";

    private View.OnClickListener consentButtonClickListener;
    private View.OnClickListener privacyPolicyButtonClickListener;
    private View.OnClickListener termsOfServiceButtonClickListener;
    private View.OnClickListener thirdPartyPartnersButtonClickListener;

    private TextView header;
    private TextView description;
    private TextView content;
    private Switch agreeSwitch;
    private Button button;
    private TextView privacyPolicy;
    private TextView tos;
    private TextView partners;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        Dialog d = getDialog();

        if(d != null) {
            ViewGroup.LayoutParams params = d.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_gdpr_wall, container, false);

        header = (TextView) view.findViewById(R.id.tv_privacy_policy_wall_header);
        description = (TextView) view.findViewById(R.id.tv_privacy_policy_wall_description);
        content = (TextView) view.findViewById(R.id.tv_privacy_policy_wall_main_text);
        agreeSwitch = (Switch) view.findViewById(R.id.sw_privacy_policy_wall_switch);
        button = (Button) view.findViewById(R.id.btn_privacy_policy_wall_button);
        privacyPolicy = (TextView) view.findViewById(R.id.tv_privacy_policy_wall_bottom_button_left);
        tos = (TextView) view.findViewById(R.id.tv_privacy_policy_wall_bottom_button_middle);
        partners = (TextView) view.findViewById(R.id.tv_privacy_policy_wall_third_bottom_button_right);

        Bundle bundle = getArguments();

        if (bundle != null && getActivity() != null && !getActivity().isFinishing()) {
            header.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(HEADER_TYPEFACE, NORMAL)));
            description.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(DESCRIPTION_TYPEFACE, NORMAL)));
            content.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(CONTENT_TYPEFACE, NORMAL)));
            agreeSwitch.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(SWITCH_TYPEFACE, NORMAL)));
            button.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(BUTTON_TYPEFACE, NORMAL)));
            privacyPolicy.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(PRIVACY_POLICY_TYPEFACE, NORMAL)));
            tos.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(TOS_TYPEFACE, NORMAL)));
            partners.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                    bundle.getString(PARTNERS_TYPEFACE, NORMAL)));
        }

        agreeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                button.setEnabled(isChecked);
            }
        });

        if(consentButtonClickListener != null )
            button.setOnClickListener(consentButtonClickListener);

        if(privacyPolicyButtonClickListener != null)
            privacyPolicy.setOnClickListener(privacyPolicyButtonClickListener);

        if(termsOfServiceButtonClickListener != null)
            tos.setOnClickListener(termsOfServiceButtonClickListener);

        if(thirdPartyPartnersButtonClickListener != null)
            partners.setOnClickListener(thirdPartyPartnersButtonClickListener);

        return view;
    }

    public void setConsentButtonClickListener(View.OnClickListener listener) {
        consentButtonClickListener = listener;
    }

    public void setPrivacyPolicyClickListener(View.OnClickListener listener) {
        privacyPolicyButtonClickListener = listener;
    }

    public void setTermsOfServiceButtonClickListener(View.OnClickListener listener) {
        termsOfServiceButtonClickListener = listener;
    }

    public void setThirdPartyPartnersButtonClickListener(View.OnClickListener listener) {
        thirdPartyPartnersButtonClickListener = listener;
    }
}
