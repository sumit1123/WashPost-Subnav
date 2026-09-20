package com.wapo.flagship.features.posttv.listeners;

import androidx.annotation.NonNull;

import com.google.ads.interactivemedia.v3.api.AdEvent;

public class AdEventListener implements AdEvent.AdEventListener {

    @NonNull
    private final VideoListener mListener;

    public AdEventListener(@NonNull VideoListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        switch (adEvent.getType()) {
            case STARTED:
                mListener.onAdEvent(VideoListener.AdEvent.STARTED);
                break;
            case COMPLETED:
                mListener.onAdEvent(VideoListener.AdEvent.COMPLETED);
                break;
        }
    }
}