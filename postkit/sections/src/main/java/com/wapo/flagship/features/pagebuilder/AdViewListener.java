package com.wapo.flagship.features.pagebuilder;

public interface AdViewListener {

    void onAdClosed(String adUnitId);
    void onAdClicked(String adUnitId);
    void onAdImpression();


}
