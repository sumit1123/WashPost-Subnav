package com.wapo.flagship.features.conversations.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapo.android.commons.extensions.toUri
import com.washingtonpost.android.R
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.flagship.features.conversations.model.CommentsUiState
import com.wapo.flagship.features.conversations.util.CommentsConstant.MyCommentsTab
import com.wapo.kmpshared.core.KMPResult
import com.wpds.theme.wpdsColors

/**
 * A bottom sheet that displays comments and allows the user to interact with them.
 *
 * @param uiState The UI state that drives everything the sheet renders.
 * @param tabs The list of tabs to display.
 * @param articleTitle The title of the article.
 * @param snackBarHostState The snackbar host state managed by the caller.
 * @param onAction A single callback for every user-initiated event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    uiState: CommentsUiState,
    tabs: List<String>,
    articleTitle: String,
    snackBarHostState: SnackbarHostState,
    onAction: (CommentAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (uiState.showProfileDialog) {
        ProfileDialog(
            minCharacters = uiState.displayNameMinCharacters,
            initialUsername = uiState.viewerUsername,
            onDismiss = { onAction(CommentAction.RequestProfileDialog) },
            onSave = { username -> onAction(CommentAction.SaveUsername(username)) }
        )
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackBarHostState.showSnackbar(it)
            onAction(CommentAction.ClearSnackbarMessage)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackBarHostState.showSnackbar(it)
            onAction(CommentAction.ClearError)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(CommentAction.Dismiss) },
        sheetState = sheetState,
        containerColor = wpdsColors.surface
    ) {
        //Back press from system back button to close all replies
        BackHandler(enabled = uiState.showAllRepliesForComment != null) {
            onAction(CommentAction.CloseAllReplies)
        }
        Box(Modifier.fillMaxSize()) {
            CommentsSheetContent(
                uiState = uiState,
                tabs = tabs,
                articleTitle = articleTitle,
                onAction = onAction
            )
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * The content of the comments bottom sheet.
 *
 * @param uiState The UI state of the comments bottom sheet.
 * @param tabs The list of tabs to display.
 * @param articleTitle The title of the article.
 * @param onAction A single callback for every user-initiated event.
 */
@Composable
private fun CommentsSheetContent(
    uiState: CommentsUiState,
    tabs: List<String>,
    articleTitle: String,
    onAction: (CommentAction) -> Unit
) {
    // Track whether the list is scrolling to collapse/expand FAB
    var isScrolling by remember { mutableStateOf(false) }

    when {
        // 1. Full-screen compose comment view
        uiState.showComposeCommentView -> {
            ComposeCommentView(
                articleTitle = articleTitle,
                parentComment = uiState.commentItem,
                contextSummary = uiState.contextSummary,
                communityGuidelinesUrl = uiState.communityGuidelinesURL,
                onBack = { onAction(CommentAction.CloseComposeComment) },
                onClose = { onAction(CommentAction.CloseComposeComment) },
                onAction = onAction
            )
        }
        // 2. Full-screen flag comment view
        uiState.flagComment != null -> {
            FlagReportScreen(
                comment = uiState.flagComment,
                onBack = { onAction(CommentAction.CloseFlagComment) },
                onAction = onAction
            )
        }
        // 3. Full-screen replies view
        uiState.showAllRepliesForComment != null -> {
            val allRepliesParent = uiState.showAllRepliesForComment
            val allReplies = (uiState.repliesMap[allRepliesParent.id] as? KMPResult.Success)?.data.orEmpty()
            AllRepliesView(
                parentComment = allRepliesParent,
                replies = allReplies,
                onBack = { onAction(CommentAction.CloseAllReplies) },
                onClose = { onAction(CommentAction.CloseAllReplies) },
                onAction = onAction,
                onLoadMoreReplies = { reply ->
                    onAction(CommentAction.LoadMoreReplies(reply, allRepliesParent.id))
                },
                commentsDisabled = uiState.commentsDisabled,
                isUserLoggedIn = uiState.isUserLoggedIn,
                isSubscriber = uiState.isSubscriber,
            )
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                val totalCommentCounts =
                    uiState.contextSummary?.data?.stream?.commentCounts?.totalPublished
                val settingsUrl = uiState.conversationSettingsURL.toUri()
                    .buildUpon()
                    .appendQueryParameter("storyUrl", uiState.storyUrl)
                    .build()
                    .toString()
                CommentsHeader(
                    commentCount = totalCommentCounts ?: 0,
                    onMyProfileClick = if (uiState.isUserLoggedIn) {
                        { onAction(CommentAction.OpenConversationSettings(settingsUrl)) }
                    } else null
                )

                Box(Modifier.weight(1f)) {
                    when {
                        // Full-screen spinner only on the very first load before
                        // summary/context data has arrived. Once the summary is
                        // available we always show CommentsList (which keeps the
                        // summary card scrollable and tabs sticky).
                        uiState.isLoading && uiState.summary == null && uiState.contextSummary == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> {
                            CommentsList(
                                comments = uiState.commentItems,
                                hasNextPage = uiState.hasNextPage,
                                isLoadingMore = uiState.isLoadingMore || uiState.isLoading,
                                isRefreshing = uiState.isRefreshing,
                                newCommentsAvailable = uiState.newCommentsAvailable,
                                scrollToTopTrigger = uiState.scrollToTopTrigger,
                                onLoadMore = { onAction(CommentAction.LoadMore) },
                                onLoadNewComments = { onAction(CommentAction.LoadNewComments) },
                                onRefresh = if (uiState.selectedTab != MyCommentsTab) {
                                    { onAction(CommentAction.RefreshComments) }
                                } else null,
                                onScrollingChanged = { isScrolling = it },
                                repliesMap = uiState.repliesMap,
                                onAction = onAction,
                                showRepliesInline = false,
                                commentsDisabled = uiState.commentsDisabled,
                                isUserLoggedIn = uiState.isUserLoggedIn,
                                isSubscriber = uiState.isSubscriber,
                                summaryContent = {
                                    Column {
                                        if (uiState.commentsDisabled) {
                                            ClosedConversationBanner()
                                        }
                                        SummaryCard(uiState.summary)
                                    }
                                },
                                tabsContent = {
                                    Surface(color = wpdsColors.surface) {
                                        ConversationTabs(
                                            tabs = tabs,
                                            selectedIndex = uiState.selectedTab,
                                            isUserLoggedIn = uiState.isUserLoggedIn,
                                            onTabSelected = { onAction(CommentAction.SelectTab(it)) },
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // FAB — collapses to icon-only while scrolling, expands when idle
                    // Hidden when the conversation is closed or user is not a subscriber
                    if (!uiState.commentsDisabled && uiState.isSubscriber) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            CommentButton(
                                onClick = { onAction(CommentAction.OpenComposeComment) },
                                isScrolling = isScrolling
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A banner displayed at the top of the conversation when comments are disabled,
 * informing the user that the conversation is now closed.
 */
@Composable
private fun ClosedConversationBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.closed_conversation_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
