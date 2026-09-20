package com.wapo.flagship.features.conversations.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.features.conversations.di.CommentsStoreProvider
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.flagship.features.conversations.viewmodel.CommentsDialogViewModel
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.shared.activities.SimpleWebViewActivity
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.util.tracking.CommentAnalyticsHelper
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSentimentType
import com.wapo.kmpshared.util.createKMPURLfromString
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A bottom sheet fragment that displays comments.
 * All business logic and analytics live here; the composable only receives
 * [CommentsUiState] and a single `onAction` callback.
 */
@AndroidEntryPoint
class CommentBottomSheetFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    @Inject
    lateinit var commentsStoreProvider: CommentsStoreProvider

    private val commentsDialogViewModel: CommentsDialogViewModel by viewModels()

    private var needsReloadOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.CommentsRenderEvent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val storyID = arguments?.getString(ARG_STORY_ID) ?: ""
        val storyUrl = arguments?.getString(ARG_STORY_URL) ?: ""
        val storyTitle = arguments?.getString(ARG_STORY_TITLE) ?: ""
        val commentSource = arguments?.getString(ARG_COMMENT_SOURCE) ?: ""
        val commentID = arguments?.getString(ARG_COMMENT_ID) ?: ""
        val trackingInfo = arguments?.getSerializable(ARG_TRACKING_INFO) as? TrackingInfo

        val deeplinkCommentID = commentID.takeIf {
            commentSource == COMMENT_SOURCE_DEEPLINK && it.isNotEmpty()
        }

        val store = createKMPURLfromString(storyUrl)?.let {
            commentsStoreProvider.makeCommentsStore(it, storyID)
        }
        if (store == null) {
            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.CommentsRenderEvent)
            openCommentsInWebView(storyUrl)
            return View(requireContext())
        }

        commentsDialogViewModel.loadInitialData(
            store = store,
            storyUrl = storyUrl,
            targetCommentID = deeplinkCommentID,
        )
        Measurement.trackCommentView(trackingInfo, commentSource)

        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by commentsDialogViewModel.uiState.collectAsState()
                val snackBarHostState = remember { SnackbarHostState() }

                AndroidClassicTheme {
                    CommentsBottomSheet(
                        uiState = uiState,
                        tabs = DEFAULT_TABS,
                        articleTitle = storyTitle,
                        snackBarHostState = snackBarHostState,
                        onAction = { action ->
                            handleAction(action, trackingInfo)
                        }
                    )
                }

                LaunchedEffect(uiState) {
                    if (!uiState.isLoading) {
                        loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.CommentsRenderEvent)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val trackingInfo = arguments?.getSerializable(ARG_TRACKING_INFO) as? TrackingInfo
        collectViewModelEvents(trackingInfo)
    }


    /**
     * Called when the fragment is no longer visible to the user.
     *
     * Releases the comments store via [commentsDialogViewModel.releaseStore] to cancel any
     * active coroutine jobs and reset state, preventing memory leaks or stale data processing
     * while the fragment is off-screen.
     *
     * Sets [needsReloadOnStart] to `true` so that the next [onStart] knows it must
     * perform a full reload of comments data.
     */
    override fun onStop() {
        super.onStop()
        commentsDialogViewModel.releaseStore()
        needsReloadOnStart = true
    }

    /**
     * Called when the fragment becomes visible to the user.
     *
     * If the story URL is missing or blank, the reload is skipped entirely.
     */
    override fun onStart() {
        super.onStart()
        if (!needsReloadOnStart) return
        needsReloadOnStart = false
        val storyUrl = arguments?.getString(ARG_STORY_URL)?.takeIf { it.isNotEmpty() } ?: return
        val storyID = arguments?.getString(ARG_STORY_ID) ?: ""
        val commentSource = arguments?.getString(ARG_COMMENT_SOURCE) ?: ""
        val commentID = arguments?.getString(ARG_COMMENT_ID) ?: ""
        val deeplinkCommentID = commentID.takeIf {
            commentSource == COMMENT_SOURCE_DEEPLINK && it.isNotEmpty()
        }
        val store = createKMPURLfromString(storyUrl)?.let {
            commentsStoreProvider.makeCommentsStore(it, storyID)
        } ?: run {
            openCommentsInWebView(storyUrl)
            return
        }
        commentsDialogViewModel.loadInitialData(
            store = store,
            storyUrl = storyUrl,
            targetCommentID = deeplinkCommentID,
        )
    }

    // Event handling — every CommentAction is processed here

    private fun handleAction(
        action: CommentAction,
        trackingInfo: TrackingInfo?
    ) {
        val viewerUsername = commentsDialogViewModel.uiState.value.viewerUsername
        val avArcId = commentsDialogViewModel.uiState.value.contextSummary?.data?.stream?.id ?: arguments?.getString(ARG_STORY_ID) ?: ""
        when (action) {
            // --- Navigation / UI ---
            is CommentAction.Dismiss -> dismissAllowingStateLoss()
            is CommentAction.OpenComposeComment -> {
                if (viewerUsername.isNullOrEmpty()) {
                    commentsDialogViewModel.showProfileDialog()
                } else {
                    commentsDialogViewModel.openComposeComment()
                }
            }
            is CommentAction.CloseComposeComment -> commentsDialogViewModel.closeComposeComment()
            is CommentAction.CloseAllReplies -> commentsDialogViewModel.closeAllReplies()
            is CommentAction.CloseFlagComment -> commentsDialogViewModel.closeFlagComment()
            is CommentAction.RequestProfileDialog -> {
                val current = commentsDialogViewModel.uiState.value.showProfileDialog
                if (current) commentsDialogViewModel.hideProfileDialog()
                else commentsDialogViewModel.showProfileDialog()
            }
            is CommentAction.SaveUsername -> {
                commentsDialogViewModel.updateUsername(action.username)
                commentsDialogViewModel.hideProfileDialog()
            }

            // --- Tabs / loading ---
            is CommentAction.SelectTab -> commentsDialogViewModel.selectTab(action.index)
            is CommentAction.LoadMore -> commentsDialogViewModel.loadMoreComments()
            is CommentAction.LoadNewComments -> commentsDialogViewModel.loadNewComments()
            is CommentAction.RefreshComments -> commentsDialogViewModel.refreshComments()
            is CommentAction.LoadMoreReplies ->
                commentsDialogViewModel.loadMoreRepliesIfNeeded(action.reply, action.parentId)

            // --- Compose / reply ---
            is CommentAction.SendComment -> {
                if (viewerUsername.isNullOrEmpty()) {
                    commentsDialogViewModel.showProfileDialog()
                    return
                }
                if (action.parent != null) {
                    commentsDialogViewModel.postReply(body = action.text, parent = action.parent)
                } else {
                    commentsDialogViewModel.postComment(body = action.text)
                }
                commentsDialogViewModel.closeComposeComment()
            }
            is CommentAction.CancelReply -> commentsDialogViewModel.cancelReply()

            // --- Reply from comment list ---
            is CommentAction.Reply -> {
                if (viewerUsername.isNullOrEmpty()) {
                    commentsDialogViewModel.showProfileDialog()
                } else {
                    commentsDialogViewModel.startReply(action.comment)
                    commentsDialogViewModel.openComposeComment()
                }
            }

            // --- Show replies ---
            is CommentAction.ShowReplies -> commentsDialogViewModel.showAllReplies(action.comment)
            is CommentAction.ShowMoreReplies -> commentsDialogViewModel.showAllReplies(action.commentId)

            // --- Voting / reactions ---
            is CommentAction.UpVote -> {
                if (action.comment.viewerAction.sentiment != CommentSentimentType.UPVOTE) {
                    Measurement.trackCommentReactionCreated(
                        trackingInfo,
                        action.comment,
                        CommentAnalyticsHelper.getSentimentMiscellany(CommentSentimentType.UPVOTE),
                        avArcId
                    )
                }
                commentsDialogViewModel.toggleSentiment(action.comment, CommentSentimentType.UPVOTE)
            }
            is CommentAction.DownVote -> {
                if (action.comment.viewerAction.sentiment != CommentSentimentType.DOWNVOTE) {
                    Measurement.trackCommentReactionCreated(
                        trackingInfo,
                        action.comment,
                        CommentAnalyticsHelper.getSentimentMiscellany(CommentSentimentType.DOWNVOTE),
                        avArcId
                    )
                }
                commentsDialogViewModel.toggleSentiment(action.comment, CommentSentimentType.DOWNVOTE)
            }
            is CommentAction.ReactionSelected -> {
                val reactionType = when (action.emoji) {
                    "HELPFUL"     -> CommentReactionType.HELPFUL
                    "CARE"        -> CommentReactionType.CARE
                    "SURPRISING"  -> CommentReactionType.SURPRISING
                    "FUNNY"       -> CommentReactionType.FUNNY
                    "FRUSTRATING" -> CommentReactionType.FRUSTRATING
                    else          -> null
                }
                if (reactionType != null) {
                    if (action.comment.viewerAction.reaction != reactionType) {
                        Measurement.trackCommentReactionCreated(
                            trackingInfo,
                            action.comment,
                            CommentAnalyticsHelper.getReactionMiscellany(reactionType),
                            avArcId
                        )
                    }
                    commentsDialogViewModel.toggleReaction(action.comment, reactionType)
                }
            }

            // --- Flagging ---
            is CommentAction.Flag -> commentsDialogViewModel.openFlagComment(action.comment)
            is CommentAction.CommentFlag -> {
                commentsDialogViewModel.flagComment(comment = action.comment, reason = action.reason)
                commentsDialogViewModel.closeFlagComment()
            }
            is CommentAction.ClearSnackbarMessage -> commentsDialogViewModel.clearSnackbarMessage()
            is CommentAction.ClearError -> commentsDialogViewModel.clearError()

            // --- ViewModel one-shot events ---
            is CommentAction.PostCommentSuccess -> {
                Measurement.trackCommentCreateSuccess(trackingInfo, action.comment, action.isReply, avArcId)
            }
            is CommentAction.PostCommentError -> {
                // errors surface via the snackbar in Compose; nothing extra needed here
            }

            is CommentAction.OpenConversationSettings -> {
                val intent = Intent(requireContext(), SimpleWebViewActivity::class.java)
                    .putExtra(SimpleWebViewActivity.URL_INTENT_PARAM, action.url)
                startActivity(intent)
            }
        }
    }

    // Collect one-shot ViewModel events

    private fun collectViewModelEvents(trackingInfo: TrackingInfo?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentsDialogViewModel.postCommentEvent.collect { result ->
                    val action = commentsDialogViewModel.toCommentAction(result)
                    handleAction(action, trackingInfo)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentsDialogViewModel.uiState.collect { state ->
                    if (state.error != null) handleAction(CommentAction.PostCommentError, trackingInfo)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentsDialogViewModel.openCommentsWebViewEvent.collect { storyUrl ->
                    openCommentsInWebView(storyUrl)
                }
            }
        }
    }

    private fun openCommentsInWebView(storyUrl: String) {
        val commentSource = arguments?.getString(ARG_COMMENT_SOURCE).orEmpty()
        if (storyUrl.isBlank()) {
            EventLog.Builder()
                .setMessage("Comments native load failed, fallback aborted due to empty url")
                .setModule(LogModules.ARTICLES)
                .set("comment_source", commentSource)
                .run {
                    RemoteLog.e(requireContext(), build())
                }
            dismissAllowingStateLoss()
            return
        }
        val webCommentsUrl = buildWebCommentsUrl(storyUrl)
        EventLog.Builder()
            .setMessage("Comments native load failed, fallback to webview")
            .setModule(LogModules.ARTICLES)
            .setContentUrl(storyUrl)
            .set("comment_source", commentSource)
            .run {
                RemoteLog.e(requireContext(), build())
            }
        val intent = Intent(requireContext(), SimpleWebViewActivity::class.java)
            .putExtra(SimpleWebViewActivity.URL_INTENT_PARAM, webCommentsUrl)
            .putExtra(SimpleWebViewActivity.EXTRA_BYPASS_NATIVE_COMMENTS, true)
        startActivity(intent)
        dismissAllowingStateLoss()
    }

    private fun buildWebCommentsUrl(storyUrl: String): String {
        val uri = storyUrl.toUri()
        val builder = uri.buildUpon()
        if (uri.getQueryParameter(DeepLinksProcessor.OUTPUT_TYPE) == null) {
            builder.appendQueryParameter(DeepLinksProcessor.OUTPUT_TYPE, DeepLinksProcessor.COMMENT)
        }
        if (uri.getQueryParameter(DeepLinksProcessor.NO_NAV) == null) {
            builder.appendQueryParameter(DeepLinksProcessor.NO_NAV, true.toString())
        }
        return builder.build().toString()
    }

    companion object {
        const val TAG = "CommentBottomSheetFragment"
        const val ARG_STORY_ID = "context_id"
        const val ARG_STORY_URL = "story_url"
        const val ARG_STORY_TITLE = "story_title"
        const val ARG_COMMENT_SOURCE = "ARG_COMMENT_SOURCE"
        const val ARG_COMMENT_ID = "ARG_COMMENT_ID"
        const val ARG_TRACKING_INFO = "ARG_TRACKING_INFO"
        const val COMMENT_SOURCE_DEEPLINK = "deeplink"
        private val DEFAULT_TABS = listOf("Featured", "Top", "My Comments", "All")

        fun newInstance(
            storyID: String,
            storyUrl: String?,
            storyTitle: String?,
            commentSource: String?,
            trackingInfo: TrackingInfo? = null,
            commentID: String? = null
        ) = CommentBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STORY_ID, storyID)
                putString(ARG_STORY_URL, storyUrl)
                putString(ARG_STORY_TITLE, storyTitle)
                putString(ARG_COMMENT_SOURCE, commentSource)
                putString(ARG_COMMENT_ID, commentID)
                putSerializable(ARG_TRACKING_INFO, trackingInfo)
            }
        }

        fun showComments(
            fragmentManager: FragmentManager,
            storyID: String,
            storyUrl: String?,
            storyTitle: String?,
            commentSource: String?,
            trackingInfo: TrackingInfo? = null,
            commentID: String? = null,
        ): Boolean {
            if (fragmentManager.isStateSaved) return false

            fragmentManager.executePendingTransactions()
            val existingFragment =
                fragmentManager.findFragmentByTag(TAG) as? CommentBottomSheetFragment
            if (existingFragment != null) return true

            newInstance(storyID, storyUrl, storyTitle, commentSource, trackingInfo, commentID)
                .show(fragmentManager, TAG)
            return true
        }
    }
}
