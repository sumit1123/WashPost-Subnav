package com.wapo.flagship.json;

import java.io.Serializable;

/**
 * @author davisas3 on 3/17/15.
 */
public class AdConfig implements Serializable {

    private String adSetCode;
    private String commercialAdNode;
    private boolean playAds;
    private boolean enableAutoPreview;
    private boolean forceAd;
    private boolean playVideoAds;
    private String adZone;
    private boolean autoPlayPreroll;
    private AdSetUrls adSetUrls;
    private AdSetConfig adSetConfig;

    public String getAdSetCode() {
        return adSetCode;
    }

    public void setAdSetCode(String adSetCode) {
        this.adSetCode = adSetCode;
    }

    public String getCommercialAdNode() {
        return commercialAdNode;
    }

    public void setCommercialAdNode(String commercialAdNode) {
        this.commercialAdNode = commercialAdNode;
    }

    public boolean isPlayAds() {
        return playAds;
    }

    public void setPlayAds(boolean playAds) {
        this.playAds = playAds;
    }

    public boolean isEnableAutoPreview() {
        return enableAutoPreview;
    }

    public void setEnableAutoPreview(boolean enableAutoPreview) {
        this.enableAutoPreview = enableAutoPreview;
    }

    public boolean isForceAd() {
        return forceAd;
    }

    public void setForceAd(boolean forceAd) {
        this.forceAd = forceAd;
    }

    public boolean isPlayVideoAds() {
        return playVideoAds;
    }

    public void setPlayVideoAds(boolean playVideoAds) {
        this.playVideoAds = playVideoAds;
    }

    public String getAdZone() {
        return adZone;
    }

    public void setAdZone(String adZone) {
        this.adZone = adZone;
    }

    public boolean isAutoPlayPreroll() {
        return autoPlayPreroll;
    }

    public void setAutoPlayPreroll(boolean autoPlayPreroll) {
        this.autoPlayPreroll = autoPlayPreroll;
    }

    public AdSetUrls getAdSetUrls() {
        return adSetUrls;
    }

    public AdSetConfig getAdSetConfig() {
        return adSetConfig;
    }
}
