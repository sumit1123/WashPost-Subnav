package com.wapo.flagship.domain.repository

interface PrefUtilsRepo {

    fun getLastVisitedBottomTab(): String?

    fun shouldShowAlertsOnboarding(): Boolean

    fun shouldShowContentPacksOnboarding(): Boolean

    fun shouldShowAudioOnboarding(): Boolean

    fun shouldShowVideoScreenTooltip(): Boolean
    fun setShowVideoScreenTooltipViewed()

    companion object {
        const val PREF_LAST_VISITED_BOTTOM_TAB: String = "pref.LAST_VISITED_BOTTOM_TAB"
        const val PREF_SHOW_ALERTS_ONBOARDING: String = "pref.show_alerts_onboarding"
        const val PREF_SHOW_CONTENT_PACKS_ONBOARDING = "pref.show_content_packs_onboarding"
        const val PREF_SHOW_AUDIO_ONBOARDING: String = "pref.show_audio_onboarding"
        const val PREF_SHOW_VIDEO_SCREEN_TOOLTIP: String = "pref.show_video_screen_tooltip"
    }
}