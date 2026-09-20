package com.wapo.flagship.features.find.events

import com.wapo.flagship.features.find.model.HighlightBoxType
import com.wapo.flagship.util.tracking.states.NavigationBehavior

sealed interface FindClickEvent {
    object SearchBarClick : FindClickEvent

    class HighlightClick(
        val type: HighlightBoxType,
        val navigationBehavior: NavigationBehavior = NavigationBehavior.FIND_TAB_HIGHLIGHT,
    ) : FindClickEvent

    class SectionClick(
        val bundleId: String,
        val navType: NavigationBehavior? = null,
    ) : FindClickEvent

    data class QuestionItemClick(
        val id: String,
        val question: String,
    ) : FindClickEvent

    data class DeepLinkItemClick(
        val link: String,
    ) : FindClickEvent
}
