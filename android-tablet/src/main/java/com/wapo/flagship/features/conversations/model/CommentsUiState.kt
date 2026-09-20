package com.wapo.flagship.features.conversations.model

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.TopLevelCommentsResponse


data class CommentsUiState(
    val selectedTab: Int = 0,
    val commentItem: CommentItem? = null,
    val comments: KMPResult<TopLevelCommentsResponse>? = null,
    val commentItems: List<CommentItem> = emptyList(),
    val endCursor: String? = null,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val repliesMap: Map<String, KMPResult<List<CommentItem>>> = emptyMap(),
    val summary: CommentSummaryResponse? = null,
    val contextSummary: ContextSummaryResponse? = null,
    val viewerActionLookup: Map<String, ActionPresence>? = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showAllRepliesForComment: CommentItem? = null,
    val newCommentsAvailable: Int = 0,
    val scrollToTopTrigger: Int = 0,
    val showComposeCommentView: Boolean = false,
    val showAllRepliesStack: List<CommentItem> = emptyList(),
    val flagComment: CommentItem? = null,
    val viewerUsername: String? = null,
    val commentsDisabled: Boolean = false,
    val showProfileDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val communityGuidelinesURL: String = "",
    val conversationSettingsURL: String = "",
    val isUserLoggedIn: Boolean = false,
    val isSubscriber: Boolean = false,
    val storyUrl : String = "",
    val displayNameMinCharacters: Int = 6,
)