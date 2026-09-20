package com.wapo.flagship.features.articles2.interfaces

sealed class ArticlesTopAppBarInteractionEvent {
    object BackClickEvent : ArticlesTopAppBarInteractionEvent()
    object ListenClickEvent : ArticlesTopAppBarInteractionEvent()
    object CommentsClickEvent : ArticlesTopAppBarInteractionEvent()
    class SummaryClickEvent(val url: String) : ArticlesTopAppBarInteractionEvent()
    class ShareClickEvent(val url: String) : ArticlesTopAppBarInteractionEvent()
    class SaveClickEvent(val url: String) : ArticlesTopAppBarInteractionEvent()
    class GiftClickEvent(val url: String) : ArticlesTopAppBarInteractionEvent()
    class PlaylistClickEvent(val currentlyInPlaylist: Boolean) : ArticlesTopAppBarInteractionEvent()
}
