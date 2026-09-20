package com.washingtonpost.android.androidlive.liveblog.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A handy structure that combines a URL, subtypes and cacheKey
 */
public class PrimeTimeUrl {
    private final String url;
    private final List<String> subtypes;

    public PrimeTimeUrl(String url, List<String> subtypes) {
        this.url = url;
        this.subtypes = new ArrayList<>();
        if (subtypes != null) {
            this.subtypes.addAll(subtypes);
        }
    }

    public String getUrl() {
        return url;
    }

    public List<String> getSubtypes() {
        return new ArrayList<>(subtypes);
    }

    public String getCacheKey() {
        if (subtypes != null) {
            return url + "#" + TextUtils.join(",", subtypes);
        }
        return url;
    }
}
