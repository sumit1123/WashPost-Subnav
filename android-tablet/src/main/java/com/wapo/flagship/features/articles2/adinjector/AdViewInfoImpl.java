package com.wapo.flagship.features.articles2.adinjector;

import com.wapo.adsinf.models.AdSlotType;
import com.wapo.flagship.features.articles.AdViewInfo;

public class AdViewInfoImpl implements AdViewInfo {
    private final String adKey;
    private final String contentUrl;
    private final String title;
    private final int adWidth;
    private final int adHeight;
    private final AdSlotType adSlotType;
    private final String position;
    private final String contentType;

    public AdViewInfoImpl(String adKey, String contentUrl, String title, int adWidth, int adHeight, AdSlotType adSlotType, String position, String contentType) {
        this.adKey = adKey;
        this.contentUrl = contentUrl;
        this.title = title;
        this.adWidth = adWidth;
        this.adHeight = adHeight;
        this.adSlotType = adSlotType;
        this.position = position;
        this.contentType = contentType;
    }

    @Override
    public String getPosition() {
        return position;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public String getAdKey() {
        return adKey;
    }

    @Override
    public String getContentUrl() {
        return contentUrl;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getAdWidth() {
        return adWidth;
    }

    @Override
    public int getAdHeight() {
        return adHeight;
    }

    @Override
    public AdSlotType getAdSlotType() {
        return adSlotType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
