package com.wapo.flagship.wapomain

object MainConstants {
    const val SHORTCUT_MY_POST = "android.intent.action.my.post"
    const val SHORTCUT_ALERTS = "android.intent.action.alerts"
    const val SHORTCUT_POLITICS = "android.intent.action.politics"
    const val SAVED_STATE_CONTAINER_KEY = "ContainerKey"
    const val SAVED_STATE_CURRENT_TAB_KEY = "CurrentTabKey"
    const val SAVED_STATE_CURRENT_TAB_ROUTE_KEY = "CurrentTabRouteKey"
    const val SAVED_STATE_NAV_KEY = "SavedStateNavKey"

    const val RATE_APP_MESSAGE_INTERVAL = 10
    const val ACTION_SHOW_PAYWALL = "android.intent.action.paywall"
    const val ACTION_SHOW_PAYWALL_REASON = "android.intent.action.paywallReason"
    const val ACTION_OPEN_SECTION = "android.intent.action.section"
    const val ACTION_OPEN_SECTION_FIND = "android.intent.action.section.find"
    const val ACTION_OPEN_SECTION_DEEPLINK = "android.intent.action.section.deeplink"
    const val ACTION_OPEN_LOGIN_REDIRECT = "android.intent.action.login_redirect"
    const val ACTION_OPEN_MAGIC_LINK = "android.intent.magic.link"
    const val ACTION_OPEN_SIGN_IN = "android.intent.login.iaa"
    const val ACTION_OPEN_SECTION_RIBBON = "android.intent.action.sectionribbon"
    const val EXTRAS_SECTION_URL = "android.intent.extras.section.url"
    const val EXTRAS_SECTION_BUNDLE_NAME = "android.intent.extras.section.bundle.name"
    const val EXTRAS_SECTION_TITLE = "android.intent.extras.section.title"
    const val EXTRAS_SECTION_ID = "android.intent.extras.section.id"

    const val EXTRAS_SECTION_OPEN_WITHOUT_STACK = "android.intent.extras.section.without.stack"
    const val EXTRAS_AUDIO_SUBTYPE = "android.intent.extras.audio.subtype"
    const val EXTRAS_AUDIO_ID = "android.intent.extras.audio.id"
    const val PREF_FOR_YOU_TAB_NOTIFICATION = "pref_for_you_tab_notification"
    const val AUTH_SCHEME = com.washingtonpost.android.BuildConfig.AUTH_SCHEME
    const val ARG_LAUNCH_INTENT_STATUS = "argLaunchIntentStatus"
    const val SECTION_ID_KEY = "section_id"
    const val SECTION_LIST_KEY = "sections_list"
    const val ATP_SHARE_ID = "ASK_SHARE_ID"

    // DeepLinks Actions
    const val ACTION_TOP_STORIES = "ACTION_TOP_STORIES"
    const val ACTION_LISTEN = "ACTION_LISTEN"
    const val ACTION_GAMES = "ACTION_GAMES"
    const val ACTION_WATCH = "ACTION_WATCH"
    const val ACTION_MY_POST = "ACTION_MY_POST"
    const val ACTION_PRINT_EDITION = "ACTION_PRINT_EDITION"
    const val ACTION_ASK = "ACTION_ASK"
    const val ACTION_FIND = "ACTION_FIND"
    const val ACTION_PROMOCODE_OFFER = "ACTION_PROMOCODE_OFFER"
    const val ACTION_OPEN_AUDIO_PLAYER = "ACTION_OPEN_AUDIO_PLAYER"
    const val EXTRA_WALL_NAME = "EXTRA_WALL_NAME"
    const val EXTRA_PAYWALL_TYPE = "EXTRA_PAYWALL_TYPE"
    const val EXTRA_CAMPAIGN_ENTRANCE_TYPE = "EXTRA_CAMPAIGN_ENTRANCE_TYPE"

    // Comment deep link
    const val ACTION_OPEN_COMMENTS_DEEPLINK = "ACTION_OPEN_COMMENTS_DEEPLINK"
    const val EXTRAS_COMMENT_STORY_URL = "android.intent.extras.comment.story.url"
    const val EXTRAS_COMMENT_ID = "android.intent.extras.comment.id"
}