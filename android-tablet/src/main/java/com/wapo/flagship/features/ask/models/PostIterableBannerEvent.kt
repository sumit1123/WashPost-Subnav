package com.wapo.flagship.features.ask.models

sealed class PostIterableBannerEvent {
    data object ContinueShareConvo : PostIterableBannerEvent()
    data object SaveConversation : PostIterableBannerEvent()
    data object Dismiss : PostIterableBannerEvent()
    data object Subscribing : PostIterableBannerEvent()
    data class ShareConversation(val isTurn: Boolean) : PostIterableBannerEvent()
}