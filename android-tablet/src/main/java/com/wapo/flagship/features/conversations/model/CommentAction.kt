package com.wapo.flagship.features.conversations.model

import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem


sealed interface CommentAction {
    // ViewModel → Fragment one-shot events surfaced as actions
    data class PostCommentSuccess(val comment: CommentItem, val isReply: Boolean) : CommentAction
    data object PostCommentError : CommentAction

    data class Reply(val comment: CommentItem) : CommentAction
    data class ShowReplies(val commentId: String, val comment: CommentItem) : CommentAction
    data class ShowMoreReplies(val commentId: String) : CommentAction
    data class SendComment(val text: String, val parent: CommentItem?) : CommentAction
    data object CancelReply : CommentAction
    data class UpVote(val comment: CommentItem, val isUp: Boolean) : CommentAction
    data class DownVote(val comment: CommentItem, val isDown: Boolean) : CommentAction
    data class ReactionSelected(val comment: CommentItem, val emoji: String) : CommentAction
    data class CommentFlag(val comment: CommentItem, val reason: CommentFlagReason) : CommentAction
    data class Flag(val comment: CommentItem) : CommentAction
    data object ClearSnackbarMessage : CommentAction
    data object ClearError : CommentAction


    // Navigation / UI-level actions
    data object OpenComposeComment : CommentAction
    data object CloseComposeComment : CommentAction
    data object CloseAllReplies : CommentAction
    data object CloseFlagComment : CommentAction
    data class SelectTab(val index: Int) : CommentAction
    data object LoadMore : CommentAction
    data object LoadNewComments : CommentAction
    data object RefreshComments : CommentAction
    data class LoadMoreReplies(val reply: CommentItem, val parentId: String) : CommentAction
    data object RequestProfileDialog : CommentAction
    data class SaveUsername(val username: String) : CommentAction
    data object Dismiss : CommentAction
    data class OpenConversationSettings(val url: String) : CommentAction
}
