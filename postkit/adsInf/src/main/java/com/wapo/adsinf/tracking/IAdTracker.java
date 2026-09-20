package com.wapo.adsinf.tracking;

import android.view.View;

import com.google.android.gms.ads.admanager.AdManagerAdRequest;

public interface IAdTracker {
    void startTracking(String uniqueId, View view);

    void stopTracking(String uniqueId);

    void addCustomTargeting(AdManagerAdRequest.Builder adManagerAdRequestBuilder);
}
