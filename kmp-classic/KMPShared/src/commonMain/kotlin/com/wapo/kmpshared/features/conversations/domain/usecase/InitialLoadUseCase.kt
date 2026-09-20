package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextSummaryResponse
import com.wapo.kmpshared.util.KMPURL
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Factory

data class InitialLoadResult(
    val username: String?,
    val viewerActions: Map<String, ActionPresence>,
    val context: ContextSummaryResponse? = null,
    val summary: CommentSummaryResponse? = null,
)

@Factory
class InitialLoadUseCase(
    private val repo: CommentsRepository,
    private val platformNetworkProvider: PlatformNetworkProvider,
) {
    suspend operator fun invoke(
        storyURL: KMPURL,
        storyID: String?,
    ): InitialLoadResult =
        coroutineScope {
            val actionsDef =
                if (platformNetworkProvider.getCredentials()?.commentsToken != null) {
                    async { repo.getViewerActions(storyURL) }
                } else {
                    null
                }
            val contextDef = async { repo.getContextSummary(storyURL) }
            val summaryDef = storyID?.let { async { repo.fetchConversationSummary(it) } }

            //  Wrap awaits in try-catch so one failure doesn't kill the whole app
            val viewerResult =
                try {
                    actionsDef?.await()
                } catch (e: Exception) {
                    null
                }
            val contextResult =
                try {
                    contextDef.await()
                } catch (e: Exception) {
                    null
                }
            val summaryResult =
                try {
                    summaryDef?.await()
                } catch (e: Exception) {
                    null
                }

            val actionsMap = mutableMapOf<String, ActionPresence>()
            var username: String? = null

            if (viewerResult is KMPResult.Success) {
                val data = viewerResult.data.data
                username = data.viewer.username
                data.viewerStoryActions?.actions?.forEach {
                    actionsMap[it.commentID] = it.actionPresence
                }
            }

            InitialLoadResult(
                username = username,
                viewerActions = actionsMap, // If API failed, this is just an empty map
                context = (contextResult as? KMPResult.Success)?.data,
                summary = (summaryResult as? KMPResult.Success)?.data,
            )
        }
}
