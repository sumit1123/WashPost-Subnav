package com.wapo.flagship.features.personalizedpodcasts.events

sealed class UserEvent {
    class TryToOpenTalkToThePost(val timestamp: Float, val transcript: String): UserEvent()
    class TalkToThePostOpening: UserEvent()
    class TalkToThePostPlayPauseTapped: UserEvent()
    class TalkToThePostClose: UserEvent()
}
