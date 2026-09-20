/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.android.commons.logs

/**
 * Constants to identify the source modules / features from where the RemoteLogs are reporting.
 * [LogKeys.MODULE] is the Key that the app modules can set with their relevant module
 * names in the RemoteLog.
 * (for ex: EventLog.Builder().setModule(LogModules.PRINT) for PrintEdition module logs).
 */
enum class LogModules {
    APP,
    ONBOARDING,
    WIDGET,
    BACKEND_HEALTH,
    SECTIONS,
    ARTICLES,
    ALERTS,
    SAVE,
    PRINT,
    VIDEO,
    AUDIO,
    ADS,
    VIDEO_ADS,
    DEEPLINK,
    PRIVACY,
    SEARCH,
    SYNC,
    SETTINGS,
    METRICS,
    PAYWALL,
    UNIFIED_MIGRATION,
    FOR_YOU,
    ANALYTICS,
    FOLLOW,
    TOPIC_FOLLOW,
    SEARCH_RECIPE,
    PREFERENCES,
    VOLLEY,
    COMICS,
    GLIDE,
    POST_ANSWERS,
    ASK_THE_POST,
    COMMENT_COUNT,
    AUTO,
    WATCH_VIDEO,
    PURCHASED_ARTICLES,
    READING_HISTORY,
    ITERABLE,
    CONFIG,
    ENGAGEMENT_METRICS,
    PERSONALIZED_PODCASTS,
    NEXT_VIDEO,
    FEEDBACK,
    ATP_SHARE,
    AGE_RESTRICTION,
    DISCLAIMER_INFO,
}

