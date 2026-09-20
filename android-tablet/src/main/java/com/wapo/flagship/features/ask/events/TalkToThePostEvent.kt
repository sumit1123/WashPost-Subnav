package com.wapo.flagship.features.ask.events

sealed class TalkToThePostEvent {
    class RelatedArticleClick(
        val url: String
    ) : TalkToThePostEvent()
}
