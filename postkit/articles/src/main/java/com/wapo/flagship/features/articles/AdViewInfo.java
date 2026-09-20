package com.wapo.flagship.features.articles;

import com.wapo.adsinf.models.AdSlotType;

public interface AdViewInfo {
    enum AdType {
        INLINE, INLINE_FOOTER
    }

    int getType();

    String getAdKey();

    String getContentUrl();

    String getTitle();

    int getAdWidth();

    int getAdHeight();

    AdSlotType getAdSlotType();

    String getPosition();

    String getContentType();
}
