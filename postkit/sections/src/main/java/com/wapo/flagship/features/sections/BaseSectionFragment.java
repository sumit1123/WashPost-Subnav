package com.wapo.flagship.features.sections;

import com.wapo.android.commons.engagement.EngagementTracker;
import com.wapo.android.commons.engagement.EngagementTrackerHelper;
import com.wapo.android.commons.engagement.PageEngagementTrace;
import com.wapo.android.commons.util.Logger;
import com.wapo.android.commons.util.ViewPagerUtils;
import com.wapo.flagship.features.grid.Tracking;

import java.util.UUID;

public abstract class BaseSectionFragment extends PageFragment implements ViewPagerUtils.WapoPageCallbacks {
    private static final String TAG = "BaseSectionFragment";

    private final EngagementTracker engagementTracker = EngagementTracker.Companion.getInstance();
    private PageEngagementTrace engagementTrace;

    public abstract String getSectionDisplayName();

    public abstract String getAdKey();

    public abstract void scrollToTop();

    public abstract void smoothScrollToTop();

    public abstract Tracking getTracking();

    public abstract String getBundleName();

    public Boolean enableStopTrack = true;

    public void onPageSelected() {
        Logger.v(TAG, "onPageSelected invoked! displayName=" + getSectionDisplayName());
        startEngagementTrace();
    }

    public void onPageUnselected() {
        Logger.v(TAG, "onPageUnselected invoked! displayName=" + getSectionDisplayName());
        stopAndTrackEngagementTrace();
    }

    @Override
    public void onPause() {
        Logger.v(TAG, "onStop invoked! displayName=" + getSectionDisplayName());
        if (enableStopTrack) {
            stopAndTrackEngagementTrace();
        }
        super.onPause();
    }

    public void startEngagementTrace() {
        if (shouldTrackEngagement() && engagementTrace == null) {
            engagementTrace = new PageEngagementTrace(UUID.randomUUID().toString(), null, null);
            engagementTrace.startTrace();
        }
    }

    public void startEngagementTrace(String key, String owner) {
        if (shouldTrackEngagement() && engagementTrace == null) {
            EngagementTrackerHelper.INSTANCE.trackPage(key, new PageEngagementTrace(UUID.randomUUID().toString(), null, null), owner);
        }
    }

    private void stopAndTrackEngagementTrace() {
        if (shouldTrackEngagement() && engagementTrace != null) {
            engagementTrace.stopTrace();
            updateEngagementTrackingInfo();
            engagementTracker.trackEngagement(engagementTrace);
            engagementTrace = null;
        }
    }

    public void stopAndTrackEngagementTrace(String key, String owner, Boolean force) {
        if (shouldTrackEngagement()) {
            PageEngagementTrace trace = EngagementTrackerHelper.INSTANCE.stopPageTrack(key, owner, force);
            if (trace != null) {
                Tracking tracking = getTracking();
                if (tracking != null) {
                    trace.updateTrackingInfo(tracking.getPageName(), tracking.getContentType(), getSectionDisplayName());
                }
                engagementTracker.trackEngagement(trace);
            }
            engagementTrace = null;
        }
    }

    private void updateEngagementTrackingInfo() {
        Tracking tracking = getTracking();
        if (tracking != null && engagementTrace != null) {
            engagementTrace.updateTrackingInfo(tracking.getPageName(), tracking.getContentType(), getSectionDisplayName());
        }
    }

    protected boolean shouldTrackEngagement() {
        return true;
    }
}
