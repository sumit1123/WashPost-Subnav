package com.wapo.kmpshared.features.conversations.domain.models.response
import kotlinx.serialization.Serializable

@Serializable
data class ContextSummaryResponse(
    val data: ContextData,
)

@Serializable
data class ContextData(
    val stream: ContextStream,
    val settings: Settings,
)

fun ContextData.incrementTotalPublished(): ContextData =
    this.copy(
        stream =
            this.stream.copy(
                commentCounts =
                    this.stream.commentCounts.copy(
                        totalPublished = this.stream.commentCounts.totalPublished + 1,
                    ),
            ),
    )

@Serializable
data class ContextStream(
    val id: String,
    val commentsDisabled: Boolean,
    val closedAt: String? = null,
    val commentCounts: CommentCounts,
)

@Serializable
data class CommentCounts(
    val totalPublished: Int,
)

@Serializable
data class Settings(
    val communityGuidelines: CommunityGuidelines,
    val charCount: CharCount,
)

@Serializable
data class CommunityGuidelines(
    val enabled: Boolean,
    val content: String,
)

@Serializable
data class CharCount(
    val enabled: Boolean,
    val min: Int,
    val max: Int,
)
