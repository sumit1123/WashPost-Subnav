package com.wapo.flagship.features.sections.tracking;

import android.content.Context;
import androidx.annotation.Nullable;

public class SectionTrackerFactory {

    public static SectionsTracker get(@Nullable Context context) {
        SectionsTracker tracker = null;
        if (context instanceof SectionsTrackerProvider) {
            tracker = ((SectionsTrackerProvider) context).getSectionTracker();
        }
        if (tracker == null) {
            tracker = new TrackerStub();
        }
        return tracker;
    }
}
