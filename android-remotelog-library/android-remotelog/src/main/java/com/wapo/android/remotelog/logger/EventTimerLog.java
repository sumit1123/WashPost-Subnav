/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

import android.content.Context;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;

import java.util.Locale;
import java.util.Map;

public class EventTimerLog {

    public final static String TAG = "EventTimerLog";
    //    private final static String STORY_VISIBLE_CLOSE= "story_visible_close";
    private static final String INFO_MESSAGE = "message";


    private final static String TOTAL_TIME = "timer_total";

    public final static String STORY_RENDER_LOAD = "story_render_load";
    public final static String STORY_RENDER_DRAW = "story_render_draw";
    public final static String FRONT_LAUNCH_TIME = "front_launch";
    public final static String FRONT_RENDER_LOAD = "front_render_load";
    public final static String ALL_SECTION_REFRESH = "front_refresh_all-sections";
    public final static String SECTION_BASE_REFRESH = "front_refresh_";
    public final static String SUPER_JSON_REFRESH = "super_json_refresh";
    public final static String SYNC_EVENT = "sync_time";

    public static final String ARTICLE_METRICS = "article_metrics";
    public static final String ARTICLE_DOWNLOAD_TIME = "story_render_download";
    public static final String ARTICLE_ENGAGEMENT_TIME = "story_visible_close";
    public static final String PRINT_EDITION_LOAD = "archive_load";
    public static final String ARCHIVE_OPEN_PDF = "archive_open_pdf";
    public final static String DATA_RENDER_LOAD = "cm_data_load";

    public static final String ASK_SCREEN = "ask_screen";
    public static final String WATCH_SCREEN = "watch_screen";
    public static final String SEARCH_SCREEN = "search_screen";
    public static final String SEARCH_RESULT_SCREEN = "search_result_screen";
    public static final String RECIPES_SCREEN = "recipes_screen";
    public static final String ALERTS_SCREEN = "alerts_screen";
    public static final String COMMENTS_SCREEN = "comments_screen";
    public static final String FOR_YOU_SCREEN = "for_you_screen";
    public static final String PAYWALL_SCREEN = "paywall_screen";
    public static final String SIGN_IN_SCREEN = "sign_in_screen";
    public static final String COMICS_SCREEN = "comics_screen";
    public static final String ANDROID_AUTO_FOR_YOU_SCREEN = "android_auto_for_you_screen";
    public static final String ANDROID_AUTO_PODCAST_SCREEN = "android_auto_podcast_screen";
    public static final String ASK_QUESTION_SCREEN = "ask_question_screen";

    public final static String SECTION_LOAD_METRICS_MESSAGE = "Section Load Metrics";

    public final static String SECTION_NAME_FIELD = "section_name";
    public final static String IS_CURRENTLY_VIEWED_SECTION_FIELD = "is_currently_viewed_section";

    public static final String WEB_PAGE_SCREEN = "web_page_screen";
    public final static String WEB_PAGE_TIME = "web_page_time";
    public static final String WEB_PAGE_URL_FILED = "web_page_url";
    public final static String WEB_PAGE_TIME_METRICS_MESSAGE = "Web page time Metrics";

// These are not currently tracked by android flagship.
//    “line”
//            “story_visible_open”
//            "front_refresh_top-stories"
//            “front_refresh_indeterminate"
// This is not used because articles are fetched via a bundle, not individually
//    public final static String STORY_RENDER_DOWNLOAD = "story_render_download"; - not used  since we

    // README: currently methods swallow exceptions.  If we have critical events to timing, we may consider
    // surfacing exceptions.

    public static void startTimingEvent(String baseEvent, String concreteEvent, String extraMessage) {
        if (isMetricsLoggingActive()) {

            try {
                StopWatchFactory.getInstance().startStopWatch(baseEvent, concreteEvent, extraMessage);
            } catch (Exception ex) {
            }
        }
    }

    public static void startTimingEvent(String baseEvent, String concreteEvent) {
        startTimingEvent(baseEvent, concreteEvent, null);
    }

    public static void restartTimingEvent(String baseEvent, String concreteEvent) {
        try {
            StopWatchFactory.getInstance().restartStopWatch(baseEvent, concreteEvent);
        } catch (Exception ex) {

        }
    }

    public static void appendNewMessageTimingEvent(String baseEvent, String concreteEvent, String extraMessage) {
        if (isMetricsLoggingActive()) {

            try {
                StopWatchFactory.getInstance().addMessageStopWatch(baseEvent, concreteEvent, extraMessage);
            } catch (Exception ex) {
            }
        }
    }

    public static void stopTimingEvent(String baseEvent, String concreteEvent) {
        if (isMetricsLoggingActive()) {

            try {
                StopWatchFactory.getInstance().stopStopWatch(baseEvent, concreteEvent);
            } catch (Exception ex) {
            }
        }
    }

    public static void stopTimingEventAndLog(String baseEvent, String stopEvent, Context context, boolean includeCalculatedTotal) {
        stopTimingEventAndLog(baseEvent, stopEvent, context, includeCalculatedTotal, null);
    }

    public static void stopTimingEventAndLog(String baseEvent, String stopEvent, Context context, boolean includeCalculatedTotal, String message) {
        stopTimingEventAndLog(baseEvent, stopEvent, context, includeCalculatedTotal, null, message);
    }

    public static void stopTimingEventAndLog(String baseEvent, String stopEvent, Context context, boolean includeCalculatedTotal, Map<String, String> additionalFields, String message) {
        if (isMetricsLoggingActive()) {

            try {
                StopWatchFactory watchFactory = StopWatchFactory.getInstance();
                if (!watchFactory.isStopWatchRunning(baseEvent, stopEvent)) {
                    return;//Ignore if the event is not running
                }
                Map<String, StopWatch> sWatch = watchFactory.stopStopWatchAndDumpStopWatches(baseEvent, stopEvent);
                EventTimerLog.logStopWatchEvent(baseEvent, sWatch, context, includeCalculatedTotal, additionalFields, message);
            } catch (Exception ex) {
            }
        }
    }

    public static void logTimingEvents(String baseEvent, Context context, boolean includeCalculatedTotal) {
        logTimingEvents(baseEvent, context, includeCalculatedTotal, null);
    }

    public static void logTimingEvents(String baseEvent, Context context, boolean includeCalculatedTotal, String message) {
        logTimingEvents(baseEvent, context, includeCalculatedTotal, null, message);
    }

    public static void logTimingEvents(String baseEvent, Context context, boolean includeCalculatedTotal, Map<String, String> additionalFields, String message) {
        if (isMetricsLoggingActive()) {

            try {
                Map<String, StopWatch> stopWatches = StopWatchFactory.getInstance().dumpStopWatches(baseEvent);
                EventTimerLog.logStopWatchEvent(baseEvent, stopWatches, context, includeCalculatedTotal, additionalFields, message);
            } catch (Exception ex) {

            }

        }
    }

    private static void logStopWatchEvent(String baseEvent, Map<String, StopWatch> stopWatches, Context context, boolean includeCalculatedTotal, Map<String, String> additionalFields, String message) {
        EventLog.Builder eventLogBuilder = new EventLog.Builder();
        eventLogBuilder.setModule(LogModules.METRICS);

        Double totalTime = 0.0;

        for (String key : stopWatches.keySet()) {
            Double elapsedTime = stopWatches.get(key).getTotalTimeInMillis();
            totalTime += elapsedTime;
            eventLogBuilder.set(key, String.format(Locale.US, "%.2f", elapsedTime));
            if (stopWatches.get(key).getExtraMessage() != null) {
                eventLogBuilder.set("keyExtraMessage", stopWatches.get(key).getExtraMessage());
            }
        }
        if (includeCalculatedTotal) {
            eventLogBuilder.set(EventTimerLog.TOTAL_TIME, String.format(Locale.US, "%.2f", totalTime));
        }

        if (additionalFields != null) {
            for (String field : additionalFields.keySet()) {
                String fieldValue = additionalFields.get(field);
                eventLogBuilder.set(field, fieldValue);
            }
        }

        if (message == null) {
            eventLogBuilder.setMessage(baseEvent);
        } else {
            eventLogBuilder.setMessage(message);
        }

        RemoteLog.m(context, eventLogBuilder.build(), RemoteLog.METRICS);
    }

    private static boolean isMetricsLoggingActive() {

        return RemoteLog.getInstance().config.isRemoteLoggingActive() && RemoteLog.getInstance().config.isMetricsLoggingActive();
    }

    public static Double getStopTime(String baseEvent, String concreteEvent) {
        try {
            return StopWatchFactory.getInstance().getStopTimeInMills(baseEvent, concreteEvent);
        } catch (Exception ex) {
        }
        return 0.0;
    }

    public static void dumpTimers(String baseEvent) {
        try {
            StopWatchFactory.getInstance().dumpStopWatches(baseEvent);
        } catch (Exception e) {
        }
    }
}
