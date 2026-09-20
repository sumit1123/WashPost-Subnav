package com.wapo.view.tooltip

/**
 * This enum is used to differentiate in between different screens on which tooltips can be shown.
 * [screenTypeTooltipPriorityPrefKey] is the pref. key that is used to get / store priority of a specific tooltip.
 */
enum class ScreenType(val screenTypeTooltipPriorityPrefKey: String) {
    /**
     * This type is used for any tooltip that's shown in article activity
     */
    ARTICLES_SCREEN("ArticlesScreenTooltipPriorityKey"),
    /**
     * This type is used for any tooltip that's shown in main activity
     */
    HOME_SCREEN("HomeScreenTooltipPriorityKey"),
    /**
     * This type is used for any tooltip that's shown in tts player screen
     */
    TTS_PLAYER_SCREEN("TtsPlayerScreenTooltipPriorityKey"),
    /**
     * This type is used for any tooltip that's shown in podcast player screen
     */
    PODCAST_PLAYER_SCREEN("PodcastPlayerScreenTooltipPriorityKey")
}