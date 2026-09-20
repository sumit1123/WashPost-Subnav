package com.wapo.flagship.util.tracking.states

import com.wapo.flagship.util.tracking.Measurement

enum class NavigationBehavior(
    val value: String,
) {
    /**
     * sign-in / subscribe from SettingsActivity
     */
    SETTINGS("settings"),

    /**
     * sign-in / subscribe from MyPostActivity
     */
    MY_POST(Measurement.MISCELLANY_UNIFIED_SIGN_IN_IMPRESSION),

    /**
     * sign-in / subscribe link from top banner on Home Page (MainActivity)
     */
    GLOBAL_SUBSCRIBE_BUTTON("global_subscribe_button"),

    /**
     * sign-in / subscribe from Acquisition Dialog
     */
    ACQUISITION_MESSAGE("direct_acquisition_message"),

    /**
     * sign-in / subscribe from Acquisition Dialog
     */
    SUBSEQUENT_ACQUISITION_MESSAGE("subsequent_acq_message"),

    /**
     * sign-in / subscribe from gift article sender dialog
     */
    GIFT_SEND_DIALOG("gift_article"),

    /**
     * sign-in / subscribe from gift article sender dialog from action buttons
     */
    GIFT_SEND_DIALOG_ACTION_BUTTONS("action_button__gift_article"),

    /**
     * sign-in / subscribe from paywall shown through deeplink article
     */
    DEEPLINK(Measurement.PATH_TO_VIEW_DEEP_LINK),

    /**
     * sign-in / subscribe from paywall shown from article save attempt
     */
    SAVE_CLICK("save_attempt"),

    /**
     * sign-in / subscribe from a link within an article (Some articles have these links)
     */
    INLINE_LINKS("article-inline-links"),

    /**
     * sign-in / subscribe from Airship message
     */
    IN_APP_PROMPT(Measurement.PATH_TO_VIEW_IN_APP_PROMPT),

    /**
     * sign-in / subscribe from OneLink
     */
    ONELINK(Measurement.PATH_TO_VIEW_ONELINK),

    /**
     * sign-in / subscribe from onboarding screen (Shown on fresh install)
     */
    ONBOARDING("universal_save_onboarding"),

    /**
     * sign-in from save paywall
     * @Deprecated - This paywall is no longer shown on Android
     */
    SAVE_PAYWALL("savepaywall"),

    /**
     * subscribe from metered paywall
     */
    STANDARD_WALL("hp-top-table-main"),

    /**
     * sign-in from metered paywall
     */
    STANDARD_WALL_SIGN_IN("hp-opinions"),

    /**
     * sign-in / subscribe from irregular paywall, e.g. from a special promotion, identified by name
     */
    NAMED_PAYWALL("apps_paywall"),

    /**
     * sign-in / subscribe from print edition
     */
    EPAPER("apps_epaper"),

    /**
     * sign-in / subscribe from regwall
     */
    REGWALL("hp-top-table-main"),

    /**
     * resume from pause wall
     */
    PAUSEWALL("apps_wall_pause"),

    /**
     * resume from pause banner
     */
    PAUSE_BANNER("apps_pause_banner"),

    /**
     * sign-in / subscribe from webview paywall
     */
    WEBVIEW("hp-top-table-main"),

    /**
     * sign-in / subscribe from paywall show after widget click
     */
    WIDGET_SMALL(Measurement.PATH_TO_VIEW_WIDGET_SMALL),
    WIDGET(Measurement.PATH_TO_VIEW_WIDGET),

    /**
     * sign-in / subscribe from default bottom cta on articles (Articles2Activity)
     */
    BOTTOM_CTA("article_subscribe_button"),

    /**
     * sign-in / subscribe from gift bottom cta on valid / not expired gift articles
     */
    GIFT_BOTTOM_CTA(Measurement.GIFT_ARTICLE_BOTTOM_CTA),

    /**
     * sign-in / subscribe from expired / invalid gift articles paywall
     */
    GIFT_INVALID(Measurement.PATH_TO_VIEW_GIFT_ARTICLE),

    /**
     * When user clicks sign in as a different user on unification on boarding screen.
     */
    UNIFICATION_ONBOARDING("unification_onboarding"),

    /**
     * sign-in from a section front feature regwall (e.g. action button, audio carousel)
     */
    FRONT("front"),

    /**
     * sign-in from the action button regwall
     */
    ACTION_BUTTON("action_button"),

    /**
     * sign-in from ccpa settings
     */
    CCPA("ccpa_settings"),
    AUDIO_CAROUSEL("audio_carousel_open_"),
    COMMENTS("comments"),
    BACK_TO_FRONT("back_to_front"),

    /**
     * find-tab nav behavior
     */
    FIND_TAB_HIGHLIGHT("ft_top_highlights"),
    FIND_TAB_RECENT("ft_recent_visited"),
    FIND_TAB_FEATURED("ft_feature"),
    FIND_TAB_AZ("ft_all_topics"),
    FIND_TAB_QUESTIONS("ft_ask_the_post"),

    /**
     * change bottom nav tab behavior
     */
    CHANGE_BOTTOM_TAB("change_tab"),

    /**
     * Top Ribbon change
     */
    SECTION_RIBBON_SWIPE("swipe_"),
    SECTION_RIBBON_CLICK("top_ribbon_"),

    /**
     * App open
     */
    APP_OPEN("app_open"),

    /**
     * Section Link
     */
    LINK("link"),

    /**
     * Section Push
     */
    PUSH("push"),

    /*
     * Section Inline Offers
     */
    SECTION_INLINE_OFFER("hp_inline_link"),

    /*
     * Article Inline Offers
     */
    ARTICLE_INLINE_OFFER("article_inline_link"),

    /**
     * For cases unaccounted for. This should raise a red flag.
     */
    UNKNOWN("unknown"),
}
