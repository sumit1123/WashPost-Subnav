package com.wapo.kmpshared.features.conversations.data.remote

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.config.ConversationsConfig
import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.features.conversations.data.TokenValidator
import com.wapo.kmpshared.features.conversations.domain.CreateCommentRequest
import com.wapo.kmpshared.features.conversations.domain.CreateReplyRequest
import com.wapo.kmpshared.features.conversations.domain.FlagRequest
import com.wapo.kmpshared.features.conversations.domain.GetCommentRequest
import com.wapo.kmpshared.features.conversations.domain.GetReplyRequest
import com.wapo.kmpshared.features.conversations.domain.GetSingleCommentRequest
import com.wapo.kmpshared.features.conversations.domain.SentimentRequest
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentRepliesResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.CreateCommentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.CreateReplyResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.FlagCommentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.SetSentimentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.SingleCommentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.TopLevelCommentsResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ViewerActionsResponse
import com.wapo.kmpshared.logger.WPLogger
import com.wapo.kmpshared.util.KMPURL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class CommentsService(
    @Named(Qualifiers.CONVERSATIONS) private val client: HttpClient,
    coder: Json,
    @Named(Qualifiers.APPLICATION_SCOPE) private val externalScope: CoroutineScope,
    private val config: ConversationsConfig,
    private val appLifecycle: AppLifecycle,
    private val platformNetworkProvider: PlatformNetworkProvider,
    private val tokenValidator: TokenValidator,
    private val logger: WPLogger,
) : CommentsBaseService(externalScope, coder, appLifecycle, platformNetworkProvider, logger) {
    companion object {
        private const val PERSISTED_QUERY = "PERSISTED_QUERY"
    }

    // --- Story & Viewer Context ---
    suspend fun fetchStoryContext(storyURL: KMPURL): KMPResult<ContextSummaryResponse> =
        executeCommentsRequest(
            "StoryContext",
            storyURL.toString(),
        ) {
            gqlGet(
                "StoryContext",
                buildJsonObject {
                    put("storyURL", storyURL.toString())
                },
            )
        }

    suspend fun fetchConversationSummary(storyID: String): KMPResult<CommentSummaryResponse> =
        toKMPResult(
            "ConversationSummary",
            storyID,
        ) {
            val body = coder.encodeToString(mapOf("id" to storyID))
            client
                .post(config.conversationSummaryURL.toString()) {
                    header("Referer", "https://www.washingtonpost.com/")
                    header("Cache-Control", "no-cache")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()
        }

    suspend fun fetchViewerActions(storyURL: KMPURL): KMPResult<ViewerActionsResponse> =
        executeCommentsRequest(
            "ViewerContext",
            storyURL.toString(),
        ) {
            gqlGet(
                "ViewerContext",
                buildJsonObject {
                    put("storyURL", storyURL.toString())
                },
                true,
            )
        }

    // --- Fetching Comments & Replies ---

    suspend fun fetchTopLevelComments(request: GetCommentRequest): KMPResult<TopLevelCommentsResponse> =
        executeCommentsRequest(
            "CommentsPublic",
            "${if (request.isInitial) "Initial" else "Subsequent"} Request: ${request.storyURL}",
        ) {
            gqlGet(
                "CommentsPublic",
                buildJsonObject {
                    put("storyURL", request.storyURL.toString())
                    put("first", request.limit)
                    request.cursor?.let { put("after", it) }
                    put("orderBy", request.sortBy.queryString)
                },
            )
        }

    suspend fun fetchReplies(request: GetReplyRequest): KMPResult<CommentRepliesResponse> =
        executeCommentsRequest(
            "RepliesPublic",
            "${if (request.isInitial) "Initial" else "Subsequent"} Request: ${request.commentID}",
        ) {
            gqlGet(
                "RepliesPublic",
                buildJsonObject {
                    put("commentID", request.commentID)
                    put("first", request.limit)
                    request.cursor?.let { put("after", it) }
                    put("orderBy", request.sortBy)
                },
            )
        }

    suspend fun fetchMyComments(request: GetCommentRequest): KMPResult<TopLevelCommentsResponse> =
        executeCommentsRequest(
            "MyComments",
            request.storyURL.toString(),
        ) {
            gqlGet(
                "MyComments",
                buildJsonObject {
                    put("storyURL", request.storyURL.toString())
                    put("first", request.limit)
                    request.cursor?.let { put("after", it) }
                    put("orderBy", request.sortBy.queryString)
                },
                authenticated = true,
            )
        }

    suspend fun fetchComment(request: GetSingleCommentRequest): KMPResult<SingleCommentResponse> =
        executeCommentsRequest(
            "CommentWithViewer",
            request.commentID.toString(),
        ) {
            gqlGet(
                "CommentWithViewer",
                buildJsonObject {
                    put("commentID", request.commentID)
                },
                authenticated = false,
            )
        }

    // --- Mutations (POST) ---
    suspend fun postComment(request: CreateCommentRequest): KMPResult<CreateCommentResponse> =
        executeCommentsRequest(
            "CreateComment",
            request.storyID,
        ) {
            gqlPost(
                "CreateComment",
                buildJsonObject {
                    put("storyID", request.storyID)
                    put("body", request.body)
                    put("clientMutationId", request.clientMutationID)
                },
            )
        }

    suspend fun postReply(request: CreateReplyRequest): KMPResult<CreateReplyResponse> =
        executeCommentsRequest(
            "CreateCommentReply",
            request.storyID,
        ) {
            gqlPost(
                "CreateCommentReply",
                buildJsonObject {
                    put("storyID", request.storyID)
                    put("parentID", request.parentID)
                    put("parentRevisionID", request.parentRevision)
                    put("body", request.body)
                    put("clientMutationId", request.clientMutationID)
                },
            )
        }

    suspend fun postSentiment(request: SentimentRequest): KMPResult<SetSentimentResponse> =
        executeCommentsRequest(
            "SetSentiment",
            request.commentID,
        ) {
            gqlPost(
                "SetSentiment",
                buildJsonObject {
                    put("commentID", request.commentID)
                    put("commentRevisionID", request.commentRevisionID)
                    put("type", request.type)
                    put("group", request.group)
                    put("clientMutationId", request.clientMutationID)
                },
            )
        }

    suspend fun flagComment(request: FlagRequest): KMPResult<FlagCommentResponse> =
        executeCommentsRequest(
            "ReportComment",
            request.commentID,
        ) {
            gqlPost(
                "ReportComment",
                buildJsonObject {
                    put("commentID", request.commentID)
                    put("commentRevisionID", request.commentRevisionID)
                    put("reason", Json.encodeToJsonElement(request.reason))
                    put("additionalDetails", request.additionalDetails)
                    put("clientMutationId", request.clientMutationID)
                },
            )
        }

    // --- Generic Internal Helpers ---

    /**
     * Executes a GraphQL GET request.
     *
     * The Ktor [client.get] call is wrapped in [withContext(NonCancellable)] so that the
     * underlying HTTP request is **not** aborted if the caller's coroutine is cancelled
     * (e.g. iOS Swift Task dismissed on navigation, or Android viewModelScope cleared).
     * This is the KMP equivalent of UIBackgroundTaskIdentifier on iOS.
     */
    private suspend fun gqlGet(
        operationID: String,
        variables: JsonObject,
        authenticated: Boolean = false,
    ): HttpResponse {
        var token: String? = null
        if (authenticated) {
            token = getValidToken()
            if (token.isNullOrBlank()) {
                throw IllegalStateException("Something went wrong. Please try again later")
            }
        }
        return withContext(NonCancellable) {
            client.get(config.baseURL.toString()) {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                url {
                    parameters.append("id", operationID)
                    parameters.append("query", PERSISTED_QUERY)
                    parameters.append("variables", coder.encodeToString(variables))
                }
            }
        }
    }

    /**
     * Executes a GraphQL POST mutation.
     *
     * The Ktor [client.post] call is wrapped in [withContext(NonCancellable)] so that the
     * underlying HTTP request is **not** aborted if the caller's coroutine is cancelled
     * (e.g. iOS Swift Task dismissed on navigation, or Android viewModelScope cleared).
     * This is the KMP equivalent of UIBackgroundTaskIdentifier on iOS.
     */
    private suspend fun gqlPost(
        operationID: String,
        variables: JsonObject,
    ): HttpResponse {
        val token = getValidToken()
        if (token.isNullOrBlank()) {
            throw IllegalStateException("Something went wrong. Please try again later")
        }
        return withContext(NonCancellable) {
            client.post(config.baseURL.toString()) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(CommentsRequest(operationID, PERSISTED_QUERY, variables))
            }
        }
    }

    private suspend fun getValidToken(): String? {
        val currentToken = platformNetworkProvider.getCredentials()?.commentsToken

        return if (tokenValidator.isTokenExpired(currentToken)) {
            platformNetworkProvider.refreshCredentials()?.commentsToken
        } else {
            currentToken
        }
    }
}
