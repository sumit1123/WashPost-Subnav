package com.wapo.flagship.features.sections.tracking;

import android.content.Context;

import androidx.annotation.Nullable;

import com.wapo.flagship.features.grid.Tracking;
import com.wapo.flagship.features.grid.model.Grid;

public interface SectionsTracker {

    void trackLiveImageToggle(String liveImageTrackingName);

    void onPagerShown(Context context);

    void onSectionLoadStart(Context context, String sectionBundleName);

    void onSectionLoadError(Context context, String sectionBundleName, boolean hasCachedContent, Throwable e, Boolean isFusion);

    void onSectionLoadSuccess(Context context, String sectionBundleName, boolean isFusion);

    void trackSectionLoadComplete(String sectionBundleName, boolean isFusion);

    void trackSlideShowSwipe(int position, String navigationBehavior);

    void trackSlideShowOverlayClick(String overlayLink);

    void trackSectionPercentage(int percentage, int totalFeatureItems, String sectionDisplayName, String bundleId, @Nullable Grid grid);

    void trackAudioCarouselNavigation(String swipeDirection);

    void trackImmersionCarouselSeen(boolean backToFront);

    void trackImmersionCarouselNavigation(String swipeDirection);

    void trackCommentsCarouselNavigation(String swipeDirection, int position);

    void trackAudioCarouselSeen(boolean backToFront);

    void trackExternalCarouselSeen(boolean backToFront);

    void trackSevenLiveCarouselSeen(boolean backToFront);
    void trackLowDataModeTurnedOff(String pageName);
}
