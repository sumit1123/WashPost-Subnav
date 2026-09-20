package com.wapo.flagship.features.pagebuilder;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ClassicAdViewFactory implements AdViewFactory {

    private final LayoutInflater inflater;

    public ClassicAdViewFactory(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getAdContainer(ViewGroup parent) {
        return inflater.inflate(com.wapo.adsinf.R.layout.ad_layout, parent, false);
    }
}