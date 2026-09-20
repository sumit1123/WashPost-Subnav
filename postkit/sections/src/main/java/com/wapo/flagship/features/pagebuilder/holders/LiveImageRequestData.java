package com.wapo.flagship.features.pagebuilder.holders;

import com.washingtonpost.android.wapocontent.ImageRequestData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiveImageRequestData extends ImageRequestData {
    public LiveImageRequestData(@NotNull String url, int width, int height, @Nullable String key) {
        super(url, width, height, key);
    }

    public LiveImageRequestData(@NotNull String url, int width, int height) {
        super(url, width, height);
    }
}
