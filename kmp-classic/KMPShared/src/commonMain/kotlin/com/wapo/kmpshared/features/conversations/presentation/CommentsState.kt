package com.wapo.kmpshared.features.conversations.presentation

import com.wapo.kmpshared.features.conversations.domain.CommentsTab
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextData

sealed interface CommentsEvent {
    // Pagination
    data class ItemsAdded(
        val items: List<CommentItem>,
        val position: CommentsAppendPosition,
    ) : CommentsEvent

    // Mutation like reactions / flag (wholesale reload)
    data class ItemChanged(
        val item: CommentItem,
    ) : CommentsEvent

    // Comment
    data class IncrementReplyCount(
        val commentID: String,
    ) : CommentsEvent

    // Pull to refresh or tab change
    data class ItemsReplaced(
        val items: List<CommentItem>,
    ) : CommentsEvent

    data class ViewStateChanged(
        val state: CommentsViewState,
    ) : CommentsEvent
}

enum class CommentsAppendPosition {
    TOP,
    BOTTOM,
}

data class CommentsViewState(
    val uiStatus: CommentsUIState = CommentsUIState.Idle,
    val currentTab: CommentsTab = CommentsTab.FEATURED,
    val hasUpdates: Boolean = false,
    val conversationSummary: String? = null,
    val contextData: ContextData? = null,
    val username: String? = null,
)

sealed class CommentsUIState {
    object Idle : CommentsUIState()

    // Use this for background data retrieval (initial load, pagination)
    object Fetching : CommentsUIState()

    // Use this for explicit user actions that require a "Wait..." moment
    data class Loading(
        val message: String? = null,
    ) : CommentsUIState()

    data class Success(
        val message: String,
    ) : CommentsUIState()

    data class Error(
        val message: String,
    ) : CommentsUIState()
}
