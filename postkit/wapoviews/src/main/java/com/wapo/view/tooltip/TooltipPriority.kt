package com.wapo.view.tooltip

/**
 * This class represents the data holder for the priority of every single tooltip in the application.
 * [screenType] is the screen onw which this priority should be calculated.
 * [priority] is the actual priority with 0 being the highest priority.
 * Note - If we want to add more priorities we need to make sure we increment the priority by one than the existing smallest priority (bigger int)
 * e.g. if we ever add another priority with [screenType] = [ScreenType.ARTICLES_SCREEN] then next in line priority will be 2
 */
sealed class TooltipPriority(open val screenType: ScreenType, open val priority: Int) {

    /**
     * Priority for Games tooltip. This was on home screen and shown first in order hence priority = 0
     */
    data class GamesTooltipPriority(override val screenType: ScreenType = ScreenType.HOME_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)

    /**
     * Priority for TTS tooltip. This was on article screen and shown first in order hence priority = 0
     */
    data class TtsTooltipPriority(override val screenType: ScreenType = ScreenType.ARTICLES_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)

    /**
     * Priority for TTS tooltip. This was on article screen and shown second in order (after TtsTooltip) hence priority = 1
     */
    data class FollowTooltipPriority(override val screenType: ScreenType = ScreenType.ARTICLES_SCREEN, override val priority: Int = 1)
        : TooltipPriority(screenType, priority)

    /**
     * Priority for podcast player tooltip. This was on podcast screen and shown first in order hence priority = 0
     */
    data class PodcastPlayerTooltipPriority(override val screenType: ScreenType = ScreenType.PODCAST_PLAYER_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)

    /**
     * Priority for Summary tooltip. This was on article screen and shown first in order hence priority = 0
     */
    data class SummaryTooltipPriority(val prefKey: String = "summary_action_tooltip", override val screenType: ScreenType = ScreenType.ARTICLES_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)

    data class SearchToolTipPriority(val prefKey: String = "search_action_tooltip", override val screenType: ScreenType = ScreenType.ARTICLES_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)


    data class AskToolTipPriority(val prefKey: String = "ask_action_tooltip", override val screenType: ScreenType = ScreenType.HOME_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)

    data class AudioToolTipPriority(val prefKey: String = "audio_action_tooltip", override val screenType: ScreenType = ScreenType.PODCAST_PLAYER_SCREEN, override val priority: Int = 0)
        : TooltipPriority(screenType, priority)
}