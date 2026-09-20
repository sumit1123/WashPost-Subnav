package com.wapo.flagship.features.sections.tracking;

import android.content.Context;

import androidx.annotation.Nullable;

import com.wapo.flagship.features.grid.Tracking;
import com.wapo.flagship.features.grid.model.Grid;
public class TrackerStub implements SectionsTracker {

    @Override
    public void trackLiveImageToggle(String liveImageTrackingName) {

    }

    @Override
    public void onPagerShown(Context context) {

    }

    @Override
    public void onSectionLoadStart(Context context, String sectionBundleName) {

    }

    @Override
    public void onSectionLoadError(Context context, String sectionBundleName, boolean hasCachedContent, Throwable e, Boolean isFusion) {

    }

    @Override
    public void onSectionLoadSuccess(Context context, String sectionBundleName, boolean isFusion) {

    }

    @Override
    public void trackSectionLoadComplete(String sectionBundleName, boolean isFusion) {

    }

    @Override
    public void trackSlideShowSwipe(int position, String navigationBehavior) {

    }

    @Override
    public void trackSlideShowOverlayClick(String overlayLink) {

    }

    @Override
    public void trackSectionPercentage(int percentage, int totalFeatureItems, String sectionDisplayName, String bundleId, @Nullable Grid grid) {

    }

    @Override
    public void trackAudioCarouselNavigation(String swipeDirection) {

    }

    @Override
    public void trackImmersionCarouselSeen(boolean backToFront) {

    }

    @Override
    public void trackExternalCarouselSeen(boolean backToFront) {

    }

    @Override
    public void trackSevenLiveCarouselSeen(boolean backToFront) {

    }

    @Override
    public void trackImmersionCarouselNavigation(String swipeDirection) {

    }

    @Override
    public void trackCommentsCarouselNavigation(String swipeDirection, int position) {

    }

    @Override
    public void trackAudioCarouselSeen(boolean backToFront) {

    }

    @Override
    public void trackLowDataModeTurnedOff(String pageName) {

    }
}
