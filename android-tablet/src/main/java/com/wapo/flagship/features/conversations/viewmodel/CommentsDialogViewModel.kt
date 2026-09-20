package com.wapo.flagship.features.conversations.viewmodel

import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.Logger
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.conversations.model.CommentsUiState
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.flagship.features.conversations.util.CommentsConstant.MyCommentsTab
import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.domain.CommentsTab
import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason as FlagReason
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType as ReactionType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSentimentType as SentimentType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextSummaryResponse
import com.wapo.kmpshared.features.conversations.presentation.CommentsStore
import com.wapo.kmpshared.features.conversations.presentation.CommentsUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.washingtonpost.android.paywall.PaywallService
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.presentation.CommentsAppendPosition
import com.wapo.kmpshared.features.conversations.presentation.CommentsEvent
import com.wapo.kmpshared.features.conversations.presentation.CommentsViewState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class CommentsDialogViewModel @Inject constructor() : ViewModel() {

    private var currentStoryUrl: String = ""
    private var commentsStore: CommentsStore? = null
    private var storeCollectionJob: Job? = null

    private val _uiState = MutableStateFlow(
        CommentsUiState(
            isUserLoggedIn = isUserLoggedIn(),
            isSubscriber = isSubscriber()
        )
    )

    val uiState: StateFlow<CommentsUiState> = _uiState

    fun isUserLoggedIn(): Boolean =
        PaywallService.initialized() && PaywallService.getInstance()?.isWpUserLoggedIn == true

    fun isSubscriber(): Boolean =
        PaywallService.initialized() && PaywallService.getInstance()?.isPremiumUser == true

    private val _postCommentEvent = MutableSharedFlow<KMPResult<CommentItem>>()
    val postCommentEvent = _postCommentEvent.asSharedFlow()
    private val _openCommentsWebViewEvent = Channel<String>(Channel.BUFFERED)
    val openCommentsWebViewEvent = _openCommentsWebViewEvent.receiveAsFlow()

    // -------------------------------------------------------------------------
    // Tab helpers
    // -------------------------------------------------------------------------

    private fun tabIndexToCommentsTab(index: Int): CommentsTab = when (index) {
        0 -> CommentsTab.FEATURED
        1 -> CommentsTab.TOP
        MyCommentsTab -> CommentsTab.MY_COMMENTS
        3 -> CommentsTab.ALL
        else -> CommentsTab.OLDEST
    }

    private fun commentsTabToIndex(tab: CommentsTab): Int = when (tab) {
        CommentsTab.FEATURED -> 0
        CommentsTab.TOP -> 1
        CommentsTab.MY_COMMENTS -> MyCommentsTab
        CommentsTab.ALL -> 3
        CommentsTab.OLDEST -> 4
    }

    // KMPResult → CommentAction bridge

    fun toCommentAction(result: KMPResult<CommentItem>): CommentAction = when (result) {
        is KMPResult.Success -> CommentAction.PostCommentSuccess(
            comment = result.data,
            isReply = result.data.parentId != null
        )
        else -> CommentAction.PostCommentError
    }

    // UI navigation helpers

    fun startReply(comment: CommentItem) {
        _uiState.update { it.copy(commentItem = comment) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(commentItem = null) }
    }

    fun showAllReplies(comment: CommentItem) {
        if (_uiState.value.showAllRepliesForComment?.id == comment.id) return
        val stack = if (_uiState.value.showAllRepliesForComment != null) {
            _uiState.value.showAllRepliesStack + _uiState.value.showAllRepliesForComment!!
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(showAllRepliesForComment = comment, showAllRepliesStack = stack)
        }
        loadReplies(comment.id)
    }

    fun showAllReplies(commentId: String) {
        val comment = findCommentById(commentId) ?: return
        showAllReplies(comment)
    }

    private fun findCommentById(commentId: String): CommentItem? {
        val state = _uiState.value
        return state.commentItems.find { it.id == commentId }
            ?: state.repliesMap.values
                .asSequence()
                .mapNotNull { (it as? KMPResult.Success)?.data }
                .flatten()
                .find { it.id == commentId }
    }

    fun closeAllReplies() {
        val stack = _uiState.value.showAllRepliesStack
        if (stack.isNotEmpty()) {
            val previousParent = stack.last()
            _uiState.update {
                it.copy(
                    showAllRepliesForComment = previousParent,
                    showAllRepliesStack = stack.dropLast(1),
                    commentItem = null // Clear any pending reply when navigating back in the stack
                )
            }
        } else {
            val closedId = _uiState.value.showAllRepliesForComment?.id
            _uiState.update {
                it.copy(
                    showAllRepliesForComment = null,
                    showAllRepliesStack = emptyList(),
                    repliesMap = if (closedId != null) it.repliesMap - closedId else it.repliesMap,
                    commentItem = null // Clearing the commentItem ref
                )
            }
        }
    }

    fun openComposeComment() {
        _uiState.update { it.copy(showComposeCommentView = true) }
    }

    fun closeComposeComment() {
        _uiState.update { it.copy(showComposeCommentView = false, commentItem = null) }
    }

    fun openFlagComment(comment: CommentItem) {
        _uiState.update { it.copy(flagComment = comment) }
    }

    fun closeFlagComment() {
        _uiState.update { it.copy(flagComment = null) }
    }

    fun showProfileDialog() {
        _uiState.update { it.copy(showProfileDialog = true) }
    }

    fun hideProfileDialog() {
        _uiState.update { it.copy(showProfileDialog = false) }
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    fun loadInitialData(
        store: CommentsStore,
        storyUrl: String,
        targetCommentID: String? = null,
    ) {
        // If the same story is already loaded, don't reload unless we need deeplink targeting.
        if (currentStoryUrl == storyUrl && _uiState.value.commentItems.isNotEmpty() && targetCommentID == null) return

        // Cancel previous collection
        storeCollectionJob?.cancel()
        storeCollectionJob = null
        commentsStore = null

        // Fully reset UI state so stale data is never shown
        currentStoryUrl = storyUrl
        _uiState.value = CommentsUiState(
            isLoading = true,
            isUserLoggedIn = isUserLoggedIn(),
            isSubscriber = isSubscriber(),
            storyUrl = storyUrl,
            hasNextPage = true,
            communityGuidelinesURL = _uiState.value.communityGuidelinesURL,
            conversationSettingsURL = _uiState.value.conversationSettingsURL,
            displayNameMinCharacters = _uiState.value.displayNameMinCharacters
        )

        storeCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            commentsStore = store

            _uiState.update {
                it.copy(
                    communityGuidelinesURL = store.config.communityGuidelinesURL.toString(),
                    conversationSettingsURL = store.config.conversationSettingsURL.toString(),
                    displayNameMinCharacters = store.config.displayNameMinCharacters
                )
            }

            // Start observing live updates BEFORE triggering any data load.
            // Launch collection and yield execution briefly so collector attaches to store.events
            // before fetchInitialSnapshot/handleTabChange emits ItemsReplaced.
            val collectionJob = launch { collectStoreFlows(store) }
            kotlinx.coroutines.yield()

            try {
                // Reset pagination + items
                val initialTab = tabIndexToCommentsTab(_uiState.value.selectedTab)
                store.handleTabChange(initialTab)

                val deeplinkedComment = try {
                    store.fetchInitialSnapshot(
                        initialTab,
                        targetCommentID = targetCommentID
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) return@launch
                    collectionJob.cancel()
                    _uiState.update { it.copy(isLoading = false) }
                    _openCommentsWebViewEvent.trySend(storyUrl)
                    return@launch
                }
                if (store.viewState.value.contextData == null) {
                    collectionJob.cancel()
                    _uiState.update { it.copy(isLoading = false) }
                    _openCommentsWebViewEvent.trySend(storyUrl)
                    return@launch
                }

                // Sync UI metadata after successful snapshot
                syncFromStore()

                // Safely dismiss loading without clobbering commentItems populated by ItemsReplaced event
                _uiState.update { it.copy(isLoading = false) }

                if (deeplinkedComment != null) {
                    withContext(Dispatchers.Main) {
                        val parentComment = deeplinkedComment.parentId?.let { parentId ->
                            findCommentById(parentId)
                        }
                        showAllReplies(parentComment ?: deeplinkedComment)
                    }
                }

            } catch (t: Throwable) {
                if (t is CancellationException) return@launch
                collectionJob.cancel()
                syncFromStore()
                val errorMessage = (store.viewState.value.uiStatus as? CommentsUIState.Error)?.message ?: ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    private fun syncFromStore(storeState: CommentsViewState? = null) {
        val resolvedState = storeState ?: commentsStore?.viewState?.value ?: return
        val storeUiState = resolvedState.uiStatus

        val isBusy = storeUiState is CommentsUIState.Fetching ||
                storeUiState is CommentsUIState.Loading

        val error = (storeUiState as? CommentsUIState.Error)?.message
        val success = (storeUiState as? CommentsUIState.Success)?.message
        val contextSummary = resolvedState.contextData?.let { ContextSummaryResponse(it) }
        val summary = resolvedState.conversationSummary?.let { CommentSummaryResponse(it) }

        _uiState.update { state ->
            state.copy(
                selectedTab = commentsTabToIndex(resolvedState.currentTab),
                isLoading = isBusy && state.commentItems.isEmpty(),
                isLoadingMore = isBusy && state.commentItems.isNotEmpty(),
                isRefreshing = state.isRefreshing && isBusy,
                error = error ?: state.error,
                snackbarMessage = success ?: state.snackbarMessage,
                newCommentsAvailable = if (resolvedState.hasUpdates) 1 else state.newCommentsAvailable,
                viewerUsername = resolvedState.username,
                contextSummary = contextSummary,
                commentsDisabled = contextSummary?.data?.stream?.commentsDisabled ?: false,
                summary = summary
            )
        }
    }

    private suspend fun collectStoreFlows(store: CommentsStore) = coroutineScope {
        launch {
            store.viewState.collect { state -> syncFromStore(state) }
        }

        launch {
            store.events.collect { event ->
                when (event) {
                    is CommentsEvent.ItemsReplaced -> {
                        _uiState.update {
                            it.copy(
                                commentItems = event.items.filter { item -> item.parentId == null },
                                repliesMap = emptyMap(),
                                isLoading = false,
                                isLoadingMore = false,
                                isRefreshing = false,
                            )
                        }
                    }

                    is CommentsEvent.ItemsAdded -> {
                        // A Success uiStatus is only set by the store after a successful post,
                        // never after a pagination fetch (which ends with Idle).  Emit the new
                        // comment so Fragment analytics fire even though postComment / postReply
                        // themselves no longer hold a reference to the returned item.
                        if (store.viewState.value.uiStatus is CommentsUIState.Success) {
                            event.items.firstOrNull()?.let { newComment ->
                                _postCommentEvent.emit(KMPResult.Success(newComment))
                            }
                        }

                        _uiState.update { state ->
                            val topLevelItems = event.items.filter { it.parentId == null }
                            val replyItemsByParent = event.items
                                .filter { it.parentId != null }
                                .groupBy { it.parentId!! }
                            val updatedItems =
                                when (event.position) {
                                    CommentsAppendPosition.TOP -> topLevelItems + state.commentItems
                                    CommentsAppendPosition.BOTTOM -> state.commentItems + topLevelItems
                                }.distinctBy { it.id }
                            val updatedRepliesMap = replyItemsByParent.entries.fold(state.repliesMap) { repliesMap, entry ->
                                val existingReplies = (repliesMap[entry.key] as? KMPResult.Success)?.data.orEmpty()
                                val updatedReplies = when (event.position) {
                                    CommentsAppendPosition.TOP -> entry.value + existingReplies
                                    CommentsAppendPosition.BOTTOM -> existingReplies + entry.value
                                }.distinctBy { it.id }

                                repliesMap + (entry.key to KMPResult.Success(updatedReplies))
                            }

                            state.copy(
                                commentItems = updatedItems,
                                repliesMap = updatedRepliesMap,
                                isLoading = false,
                                isLoadingMore = false,
                                hasNextPage = if (
                                    event.position == CommentsAppendPosition.BOTTOM &&
                                    replyItemsByParent.isEmpty() &&
                                    topLevelItems.size < store.config.topLevelPageSize
                                ) { false }
                                else {
                                    state.hasNextPage
                                }
                            )
                        }
                    }

                    is CommentsEvent.ItemChanged -> {
                        _uiState.update { state ->
                            state.copy(
                                commentItems = state.commentItems.map {
                                    if (it.id == event.item.id) event.item else it
                                },
                                repliesMap = state.repliesMap.mapValues { (_, result) ->
                                    val replies = (result as? KMPResult.Success)?.data
                                        ?: return@mapValues result

                                    KMPResult.Success(
                                        replies.map {
                                            if (it.id == event.item.id) event.item else it
                                        }
                                    )
                                },
                                showAllRepliesForComment = state.showAllRepliesForComment?.let {
                                    if (it.id == event.item.id) event.item else it
                                },
                                showAllRepliesStack = state.showAllRepliesStack.map {
                                    if (it.id == event.item.id) event.item else it
                                }
                            )
                        }
                    }

                    is CommentsEvent.IncrementReplyCount -> {
                        _uiState.update { state ->
                            state.copy(
                                commentItems = state.commentItems.map {
                                    if (it.id == event.commentID) it.copy(replyCount = it.replyCount + 1) else it
                                },
                                repliesMap = state.repliesMap.mapValues { (_, result) ->
                                    val replies = (result as? KMPResult.Success)?.data
                                        ?: return@mapValues result

                                    KMPResult.Success(
                                        replies.map {
                                            if (it.id == event.commentID) it.copy(replyCount = it.replyCount + 1) else it
                                        }
                                    )
                                },
                                showAllRepliesForComment = state.showAllRepliesForComment?.let {
                                    if (it.id == event.commentID) it.copy(replyCount = it.replyCount + 1) else it
                                },
                                showAllRepliesStack = state.showAllRepliesStack.map {
                                    if (it.id == event.commentID) it.copy(replyCount = it.replyCount + 1) else it
                                }
                            )
                        }
                    }

                    is CommentsEvent.ViewStateChanged -> syncFromStore(event.state)

                }
            }
        }
    }

    // Tab / scroll actions

    fun selectTab(index: Int) {
        _uiState.update {
            it.copy(
                selectedTab = index,
                commentItems = emptyList(),
                endCursor = null,
                newCommentsAvailable = 0,
                hasNextPage = true
            )
        }
        commentsStore?.handleTabChange(tabIndexToCommentsTab(index))
    }

    fun refreshComments() {
        _uiState.update {
            it.copy(
                isRefreshing = true,
                error = null,
                newCommentsAvailable = 0,
                hasNextPage = true
            )
        }
        commentsStore?.refresh()
    }

    fun loadMoreComments() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            executeStoreAction {
                commentsStore?.fetchMoreComments()
            }
        }
    }

    fun loadReplies(commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            executeStoreAction { commentsStore?.fetchMoreComments(commentId) }
        }
    }

    fun loadMoreRepliesIfNeeded(reply: CommentItem, parentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val replies = (_uiState.value.repliesMap[parentId] as? KMPResult.Success)?.data.orEmpty()
            val currentIndex = replies.indexOfFirst { it.id == reply.id }.takeIf { it >= 0 }
            executeStoreAction {
                commentsStore?.fetchMoreIfNeeded(
                    currentIndex = currentIndex,
                    total = replies.size,
                    parentId = parentId
                )
            }
        }
    }

    fun loadNewComments() {
        _uiState.update {
            it.copy(
                newCommentsAvailable = 0,
                scrollToTopTrigger = it.scrollToTopTrigger + 1,
                isRefreshing = true,
                hasNextPage = true
            )
        }
        commentsStore?.refresh()
    }

    // -------------------------------------------------------------------------
    // Reactions / sentiment
    // -------------------------------------------------------------------------

    fun toggleSentiment(comment: CommentItem, type: SentimentType) {
        viewModelScope.launch(Dispatchers.IO) {
            executeStoreAction { commentsStore?.toggleSentiment(comment, type) }
        }
    }

    fun toggleReaction(comment: CommentItem, type: ReactionType) {
        viewModelScope.launch(Dispatchers.IO) {
            executeStoreAction { commentsStore?.toggleReaction(comment, type) }
        }
    }

    // -------------------------------------------------------------------------
    // Post / reply
    // -------------------------------------------------------------------------

    fun postComment(body: String, parent: CommentItem? = null) {
        if (parent != null) {
            postReply(body, parent)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val store = commentsStore ?: return@launch

            try {
                store.postTopLevelComment(body)
                syncFromStore()

                _uiState.update {
                    it.copy(
                        showComposeCommentView = false,
                        commentItem = null
                    )
                }
            } catch (e: Exception) {
                syncFromStore()
            }
        }
    }

    fun postReply(body: String, parent: CommentItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = commentsStore ?: return@launch

            try {
                store.postReply(
                    body = body,
                    parentId = parent.id,
                    parentRevision = parent.revisionId ?: "",
                    parentDepth = parent.depth
                )

                syncFromStore()

                _uiState.update {
                    it.copy(
                        showComposeCommentView = false,
                        commentItem = null
                    )
                }
            } catch (e: Exception) {
                syncFromStore()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Flag / username
    // -------------------------------------------------------------------------


    fun flagComment(comment: CommentItem, reason: FlagReason) {
        viewModelScope.launch(Dispatchers.IO) {
            executeStoreAction { commentsStore?.flagComment(comment, reason) }
        }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            executeStoreAction { commentsStore?.postDisplayName(username) }
        }
    }

    // -------------------------------------------------------------------------

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        storeCollectionJob?.cancel()
        storeCollectionJob = null
        _openCommentsWebViewEvent.close()
        // scope must remain alive for future sessions (handleTabChange / jumpToNewest
        // use scope.launch internally and cannot recover from a cancelled scope).
        commentsStore = null
        super.onCleared()
    }

    /**
     * Releases the active store state and cancels any ongoing data collection.
     *
     * This function acts as a "reset to uninitialized state" cleanup method, intended to be called
     * when navigating away from a screen, switching stories, or when the associated lifecycle
     * owner is destroyed.
     *
     * - Cancels the active [storeCollectionJob] coroutine to stop observing store data and
     *   prevent memory leaks or stale data processing.
     * - Nullifies [storeCollectionJob] so the reference can be garbage collected and future
     *   null-checks behave correctly.
     * - Clears [commentsStore] so the active store state is released and can be recreated for a future session.
     * - Resets [currentStoryUrl] to an empty string, which signals that the next call to
     *   [loadInitialData] should perform a full reload rather than an incremental/partial update.
     */
    fun releaseStore() {
        storeCollectionJob?.cancel()
        storeCollectionJob = null
        commentsStore = null
        currentStoryUrl = ""  // Force full reload on next loadInitialData call
    }

    private suspend fun executeStoreAction(action: suspend () -> Unit) {
        try {
            action()
            syncFromStore()
        } catch (e: CancellationException) {
            Logger.d("CommentsDialogViewModel", "executeStoreAction: cancelled")
            _uiState.update { it.copy(isLoading = false, isLoadingMore = false, isRefreshing = false) }
            throw e
        } catch (e: Exception) {
            syncFromStore()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    isRefreshing = false,
                    error = (commentsStore?.viewState?.value?.uiStatus as? CommentsUIState.Error)?.message ?:""
                )
            }
        }
    }
}
