package com.wapo.kmpshared.features.conversations.presentation

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.config.ConversationsConfig
import com.wapo.kmpshared.features.conversations.domain.CommentsPaginationManager
import com.wapo.kmpshared.features.conversations.domain.CommentsTab
import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSentimentType
import com.wapo.kmpshared.features.conversations.domain.models.response.incrementTotalPublished
import com.wapo.kmpshared.features.conversations.domain.usecase.ConversationSummaryUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.FlagCommentUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.GetCommentsUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.GetSingleCommentUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.InitialLoadUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.PostCommentUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.ReactToCommentUseCase
import com.wapo.kmpshared.features.conversations.domain.usecase.UpdateDisplayNameUseCase
import com.wapo.kmpshared.util.KMPURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.coroutines.cancellation.CancellationException

@Factory
class CommentsStore(
    @InjectedParam val storyURL: KMPURL,
    @InjectedParam val arcID: String?,
    val config: ConversationsConfig,
    private val initialLoadUseCase: InitialLoadUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val postCommentUseCase: PostCommentUseCase,
    private val reactionsUseCase: ReactToCommentUseCase,
    private val flagUseCase: FlagCommentUseCase,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase,
    private val conversationSummaryUseCase: ConversationSummaryUseCase,
    private val getSingleCommentUseCase: GetSingleCommentUseCase,
    private val paginationManager: CommentsPaginationManager,
    private val monitor: CommentsUpdateMonitor,
) {
    private var fetchJob: Job? = null
    private var monitorCollectorJob: Job? = null

    // The single scope for this Store instance
    private var storeJob = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.Main + storeJob)

    private val _viewState = MutableStateFlow(CommentsViewState())
    val viewState: StateFlow<CommentsViewState> = _viewState.asStateFlow()

    // Android consumes this
    private val _events = Channel<CommentsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // iOS consumes this
    private val pendingEvents = mutableListOf<CommentsEvent>()
    var onEvent: ((CommentsEvent) -> Unit)? = null
        set(value) {
            field = value
            // drain any buffered events
            val drained = pendingEvents.toList()
            pendingEvents.clear()
            drained.forEach { value?.invoke(it) }
        }

    // Local cache for viewer actions
    private val viewerActionLookup = mutableMapOf<String, ActionPresence>()

    // ID used for posting comments
    private var commentsStoryID: String? = null

    private fun updateState(block: (CommentsViewState) -> CommentsViewState) {
        _viewState.update(block)

        emitEvent(CommentsEvent.ViewStateChanged(_viewState.value))
    }

    private fun emitEvent(event: CommentsEvent) {
        // Feeds Android
        val sendResult = _events.trySend(event)
        if (sendResult.isFailure && !sendResult.isClosed) {
            scope.launch { _events.send(event) }
        }

        // Feeds iOS
        onEvent?.let {
            it.invoke(event)
        } ?: run {
            pendingEvents.add(event) // buffer until iOS wires up
        }
    }

    /**
     * Initial snapshot:
     * Fetch viewer actions before fetching comments
     * This makes it easier to mutate the comment as they're coming in
     */
    @Throws(Throwable::class)
    suspend fun fetchInitialSnapshot(
        tab: CommentsTab,
        targetCommentID: String?,
    ): CommentItem? {
        updateState {
            it.copy(
                currentTab = tab,
                uiStatus = CommentsUIState.Fetching,
                hasUpdates = false,
            )
        }
        monitorCollectorJob?.cancel()
        monitorCollectorJob =
            scope.launch {
                try {
                    monitor.hasUpdates.collect { hasUpdates ->
                        updateState { it.copy(hasUpdates = hasUpdates) }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }

        try {
            val initialData = initialLoadUseCase(storyURL, arcID)
            commentsStoryID =
                initialData.context
                    ?.data
                    ?.stream
                    ?.id

            viewerActionLookup.clear()
            viewerActionLookup.putAll(initialData.viewerActions)

            updateState {
                it.copy(
                    username = initialData.username,
                    contextData = initialData.context?.data,
                    conversationSummary = initialData.summary?.answer,
                    uiStatus = CommentsUIState.Idle,
                )
            }
        } catch (e: Exception) {
        }

        fetchJob?.cancel()
        fetchJob = null
        fetchMoreComments()

        return if (targetCommentID != null) {
            runCatching { getComment(targetCommentID) }.getOrNull()
        } else {
            null
        }
    }

    /**
     * Fetch a single comment
     * Primarily used for deep-linking
     */
    @Throws(Throwable::class)
    private suspend fun getComment(commentId: String): CommentItem? {
        updateState { it.copy(uiStatus = CommentsUIState.Loading("Fetching comment")) }
        val result = getSingleCommentUseCase.invoke(commentId, viewerActionLookup.toMap())

        return when (result) {
            is KMPResult.Success -> {
                updateState { it.copy(uiStatus = CommentsUIState.Idle) }
                result.data
            }

            is KMPResult.Error -> {
                if (result.message.contains("cancel", ignoreCase = true)) {
                    updateState { it.copy(uiStatus = CommentsUIState.Idle) }
                    return null
                } else {
                    updateState { it.copy(uiStatus = CommentsUIState.Error("Unable to fetch comment. Please try again later.")) }
                    throw Exception(result.message)
                }
            }
        }
    }

    /**
     * Cleans up all background work. Call this from iOS/Android deinit.
     */
    fun clear() {
        monitor.stop() // Immediately stop background polling
        monitorCollectorJob?.cancel()
        fetchJob?.cancel() // Stop any active pagination/tab-switch fetch
        storeJob.cancel()
        paginationManager.reset()
        pendingEvents.clear()
        onEvent = null
        _events.close()
    }

    /**
     * Get conversation summary
     */
    @Throws(Throwable::class)
    suspend fun fetchConversationSummary(storyID: String): String? {
        val result = conversationSummaryUseCase.invoke(storyID)

        return when (result) {
            is KMPResult.Success -> {
                result.data
            }

            is KMPResult.Error -> {
                if (result.message.contains("cancel", ignoreCase = true)) {
                    updateState { it.copy(uiStatus = CommentsUIState.Idle) }
                    return null
                } else {
                    updateState {
                        it.copy(
                            uiStatus = CommentsUIState.Error("Unable to fetch conversation summary. Please try again later."),
                        )
                    }
                    throw Exception(result.message)
                }
            }
        }
    }

    /**
     * Checks whether the next page should be fetched based on the viewer's current position.
     *
     * @param currentIndex The index of the comment currently visible to the user within [total],
     *  * or null to trigger an initial load.
     * @param total Pre-filtered comment IDs relevant to the current context -
     * unfiltered for the top-level feed, filtered by [parentId] for threads.
     * @param parentId The parent comment ID when paginating a thread, or null for the top-level feed.
     */
    @Throws(Throwable::class)
    suspend fun fetchMoreIfNeeded(
        currentIndex: Int?,
        total: Int,
        parentId: String?,
    ) {
        if (paginationManager.shouldFetchMore(currentIndex, total, parentId)) {
            fetchMoreComments(parentId)
        }
    }

    @Throws(Throwable::class)
    suspend fun fetchMoreComments(parentId: String? = null) {
        if (viewState.value.uiStatus is CommentsUIState.Fetching) return

        val (_, hasMore) = paginationManager.getRequestParams(parentId)
        if (!hasMore) return

        paginationManager.markAsFetching(parentId)
        updateState { it.copy(uiStatus = CommentsUIState.Fetching) }

        val isFreshTabLoad = parentId == null && paginationManager.isFreshLoad()
        try {
            // Top-level uses selected tab; Replies use a fixed sort (usually OLDEST)
            val tab = if (parentId == null) viewState.value.currentTab else CommentsTab.OLDEST
            val result =
                getCommentsUseCase.invoke(
                    storyURL,
                    tab,
                    parentId,
                    viewerActionLookup.toMap(),
                )
            handleGetCommentsUseCaseResult(result, isFreshTabLoad)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            updateState {
                it.copy(uiStatus = CommentsUIState.Error("Unable to fetch more comments. Please try again later"))
            }
            if (isFreshTabLoad) {
                emitEvent(CommentsEvent.ItemsReplaced(emptyList()))
            }
            throw e
        } finally {
            paginationManager.markAsFinished(parentId)
        }
    }

    @Throws(Throwable::class)
    private fun handleGetCommentsUseCaseResult(
        result: KMPResult<List<CommentItem>>,
        isFreshTabLoad: Boolean,
    ) {
        when (result) {
            is KMPResult.Success -> {
                updateState {
                    it.copy(uiStatus = CommentsUIState.Idle)
                }

                if (isFreshTabLoad) {
                    emitEvent(CommentsEvent.ItemsReplaced(result.data.filterNot { it.viewerAction.flagged }))
                } else {
                    emitEvent(
                        CommentsEvent.ItemsAdded(
                            result.data.filterNot { it.viewerAction.flagged },
                            CommentsAppendPosition.BOTTOM,
                        ),
                    )
                }
            }

            is KMPResult.Error -> {
                updateState {
                    if (result.message.contains("cancel", ignoreCase = true)) {
                        it.copy(uiStatus = CommentsUIState.Idle)
                    } else {
                        it.copy(uiStatus = CommentsUIState.Error("Unable to fetch more comments. Please try again later"))
                    }
                }
            }
        }
        if (result is KMPResult.Error && !result.message.contains("cancel", ignoreCase = true)) {
            throw Exception(result.message)
        }
    }

    /**
     * Posts a top-level comment.
     */
    @Throws(Throwable::class)
    suspend fun postTopLevelComment(body: String) {
        val commentsStoryID =
            commentsStoryID ?: run {
                val errorMsg = "Something went wrong. Please try again later"
                updateState { it.copy(uiStatus = CommentsUIState.Error(errorMsg)) }
                throw Exception(errorMsg)
            }
        if (viewState.value.uiStatus is CommentsUIState.Loading) return

        updateState { it.copy(uiStatus = CommentsUIState.Loading()) }

        try {
            val result = postCommentUseCase.invoke(body, commentsStoryID)
            handlePostCommentsUseCaseResult(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            updateState { it.copy(uiStatus = CommentsUIState.Error("Unable to post comment. Please try again later.")) }
            throw e
        }
    }

    /**
     * Posts a reply to an existing comment.
     */
    @Throws(Throwable::class)
    suspend fun postReply(
        body: String,
        parentId: String,
        parentRevision: String,
        parentDepth: Int,
    ) {
        val commentsStoryID =
            commentsStoryID ?: run {
                val errorMsg = "Something went wrong. Please try again later"
                updateState { it.copy(uiStatus = CommentsUIState.Error(errorMsg)) }
                throw Exception(errorMsg)
            }
        if (viewState.value.uiStatus is CommentsUIState.Loading) return

        updateState { it.copy(uiStatus = CommentsUIState.Loading("Sending your reply...")) }

        try {
            val result =
                postCommentUseCase.invoke(
                    body,
                    commentsStoryID,
                    parentId,
                    parentRevision,
                    parentDepth,
                )
            handlePostCommentsUseCaseResult(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            updateState { it.copy(uiStatus = CommentsUIState.Error("Unable to post comment. Please try again later.")) }
            throw e
        }
    }

    private fun handlePostCommentsUseCaseResult(result: KMPResult<CommentItem>) {
        when (result) {
            is KMPResult.Success -> {
                updateState {
                    it.copy(
                        uiStatus = CommentsUIState.Success("Your post was submitted."),
                        contextData = it.contextData?.incrementTotalPublished(),
                    )
                }

                val appendPosition =
                    if (result.data.parentId != null) {
                        // Replies are fetched in OLDEST order
                        CommentsAppendPosition.BOTTOM
                    } else if (viewState.value.currentTab == CommentsTab.ALL || viewState.value.currentTab == CommentsTab.MY_COMMENTS) {
                        CommentsAppendPosition.TOP
                    } else {
                        CommentsAppendPosition.BOTTOM
                    }

                emitEvent(
                    CommentsEvent.ItemsAdded(
                        listOf(result.data),
                        appendPosition,
                    ),
                )
                result.data.parentId?.let { parentId ->
                    emitEvent(
                        CommentsEvent.IncrementReplyCount(
                            parentId,
                        ),
                    )
                }
            }

            is KMPResult.Error -> {
                val displayMessage =
                    when (result.message) {
                        "MODERATION_REJECTED" -> "Your comment was flagged for violating community guidelines."
                        "EMPTY_CONTENT_ERROR" -> "Unable to post comment. Please try again later."
                        else -> "Unable to post comment. Please try again later."
                    }
                updateState {
                    if (result.message.contains("cancel", ignoreCase = true)) {
                        it.copy(uiStatus = CommentsUIState.Idle)
                    } else {
                        it.copy(uiStatus = CommentsUIState.Error(displayMessage))
                    }
                }
            }
        }
        if (result is KMPResult.Error && !result.message.contains("cancel", ignoreCase = true)) {
            throw Exception(result.message)
        }
    }

    /**
     * Toggles a sentiment (e.g., Like/Dislike) on a comment.
     */
    @Throws(Throwable::class)
    suspend fun toggleSentiment(
        comment: CommentItem,
        sentiment: CommentSentimentType,
    ) {
        val isRemoving = comment.viewerAction.sentiment == sentiment
        val typeValue = if (isRemoving) null else sentiment.value
        return updateReaction(comment = comment, typeValue = typeValue, groupValue = "VOTE")
    }

    /**
     * Toggles a reaction (e.g., Funny, Frustrating, Helpful) on a comment.
     */
    @Throws(Throwable::class)
    suspend fun toggleReaction(
        comment: CommentItem,
        reaction: CommentReactionType,
    ) {
        val isRemoving = comment.viewerAction.reaction == reaction
        val typeValue = if (isRemoving) null else reaction.value
        return updateReaction(comment = comment, typeValue = typeValue, groupValue = "REACTION")
    }

    /**
     * Common logic for updating reactions/sentiments.
     */
    @Throws(Throwable::class)
    private suspend fun updateReaction(
        comment: CommentItem,
        typeValue: String?,
        groupValue: String,
    ) {
        if (viewState.value.uiStatus is CommentsUIState.Loading) return
        updateState { it.copy(uiStatus = CommentsUIState.Loading("")) }
        try {
            val result = reactionsUseCase.invoke(comment, typeValue, groupValue)
            when (result) {
                is KMPResult.Success -> {
                    updateState { it.copy(uiStatus = CommentsUIState.Idle) }
                    emitEvent(CommentsEvent.ItemChanged(result.data))
                }

                is KMPResult.Error -> {
                    updateState { it.copy(uiStatus = CommentsUIState.Error(result.message)) }
                }
            }
            if (result is KMPResult.Error) {
                throw Exception(result.message)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            updateState { it.copy(uiStatus = CommentsUIState.Error("Unable to react to this comment. Please try again later.")) }
            throw e
        }
    }

    /**
     * Flags a comment for a specific reason and removes it from the user's view.
     */
    suspend fun flagComment(
        comment: CommentItem,
        reason: CommentFlagReason,
    ) {
        updateState { it.copy(uiStatus = CommentsUIState.Loading("Reporting comment")) }
        val result = flagUseCase.invoke(comment, reason)

        return when (result) {
            is KMPResult.Success -> {
                updateState { it.copy(uiStatus = CommentsUIState.Success("Report submitted")) }
            }

            is KMPResult.Error -> {
                updateState { it.copy(uiStatus = CommentsUIState.Error("Unable to flag comment. Please try again later.")) }
            }
        }
    }

    /**
     * For user to set a display name or change an existing one
     */
    @Throws(Throwable::class)
    suspend fun postDisplayName(newName: String) {
        updateState { it.copy(uiStatus = CommentsUIState.Loading("Updating display name")) }
        val result = updateDisplayNameUseCase.invoke(newName)

        return when (result) {
            is KMPResult.Success -> {
                updateState { it.copy(username = newName, uiStatus = CommentsUIState.Success("Display name updated")) }
            }

            is KMPResult.Error -> {
                updateState {
                    it.copy(
                        uiStatus =
                            CommentsUIState.Error("Unable to update display name. Please verify your email and check your connectivity."),
                    )
                }
                throw Exception(result.message)
            }
        }
    }

    fun handleTabChange(newTab: CommentsTab) {
        fetchJob?.cancel()
        paginationManager.reset()
        monitor.stop()
        updateState { it.copy(currentTab = newTab) }
        fetchJob =
            scope.launch {
                try {
                    fetchMoreComments()
                } catch (e: Exception) {
                    // fetchMoreComments already updated uiStatus to Error.
                }
            }
    }

    // User has tapped on "Load More"
    fun refresh() {
        paginationManager.reset()
        monitor.stop()
        fetchJob =
            scope.launch {
                try {
                    fetchMoreComments()
                } catch (e: Exception) {
                    // Error is already handled in the uiStatus state
                }
            }
    }
}
