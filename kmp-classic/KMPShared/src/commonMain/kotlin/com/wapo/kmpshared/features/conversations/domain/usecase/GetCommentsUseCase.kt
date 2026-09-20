package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.config.ConversationsConfig
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.features.conversations.data.mapper.NormalizedCommentsPage
import com.wapo.kmpshared.features.conversations.domain.CommentsPaginationManager
import com.wapo.kmpshared.features.conversations.domain.CommentsTab
import com.wapo.kmpshared.features.conversations.domain.GetCommentRequest
import com.wapo.kmpshared.features.conversations.domain.GetReplyRequest
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import com.wapo.kmpshared.features.conversations.presentation.CommentsUpdateMonitor
import com.wapo.kmpshared.util.KMPURL
import org.koin.core.annotation.Factory

@Factory
class GetCommentsUseCase(
    private val repo: CommentsRepository,
    private val paginationManager: CommentsPaginationManager,
    private val monitor: CommentsUpdateMonitor,
    private val config: ConversationsConfig,
) {
    suspend operator fun invoke(
        storyURL: KMPURL,
        tab: CommentsTab,
        parentId: String?,
        viewerActionLookup: Map<String, ActionPresence>,
    ): KMPResult<List<CommentItem>> {
        val (cursor, _) = paginationManager.getRequestParams(parentId)

        val result: KMPResult<NormalizedCommentsPage> =
            if (tab == CommentsTab.MY_COMMENTS) {
                val request =
                    GetCommentRequest(storyURL, config.topLevelPageSize, cursor, tab, paginationManager.isFreshLoad())
                repo.getMyComments(request, viewerActionLookup)
            } else if (parentId != null) {
                val request =
                    GetReplyRequest(
                        parentId,
                        config.replyPageSize,
                        cursor,
                        CommentsTab.OLDEST.queryString,
                        paginationManager.isInitialLoadForThread(parentId),
                    )
                repo.getReplies(request, viewerActionLookup)
            } else {
                val request =
                    GetCommentRequest(storyURL, config.topLevelPageSize, cursor, tab, paginationManager.isFreshLoad())
                repo.getTopLevelComments(request, viewerActionLookup)
            }

        return when (result) {
            is KMPResult.Success -> {
                val page = result.data

                // Update Pagination Metadata
                paginationManager.update(parentId, page.pageInfo.endCursor, page.pageInfo.hasNextPage)

                // Manage the background update monitor (Only for Top-Level 'LATEST')
                if (tab == CommentsTab.ALL && parentId == null) {
                    monitor.startMonitoring(storyURL, page.items.firstOrNull()?.id)
                } else if (parentId == null) {
                    monitor.stop()
                }

                // Return new comments but remove any flagged comments
                val newComments = page.items.filterNot { it.viewerAction.flagged }
                KMPResult.Success(newComments)
            }

            is KMPResult.Error -> {
                KMPResult.Error(result.message)
            }
        }
    }
}
