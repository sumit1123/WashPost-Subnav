package com.wapo.kmpshared.features.conversations.domain.models.response

import com.wapo.kmpshared.util.KMPURL
import com.wapo.kmpshared.util.createKMPURLfromString
import kotlinx.serialization.*

@Serializable
data class TopLevelCommentsResponse(
    val data: DataPayload,
)

@Serializable
data class DataPayload(
    val stream: Stream,
)

@Serializable
data class Stream(
    val id: String,
    val comments: CommentConnection,
)

@Serializable
data class CommentConnection(
    val pageInfo: PageInfo,
    val nodes: List<CommentNode>,
)

@Serializable
data class PageInfo(
    val hasNextPage: Boolean,
    val endCursor: String?,
    @SerialName("displayDepth") val depth: Int? = 0,
)

@Serializable
data class CommentNode(
    val id: String,
    val bodyNodes: List<CommentBodyNode>,
    val status: CommentStatus? = CommentStatus.UNKNOWN,
    val createdAt: String,
    val replyCount: Int = 0,
    val revision: Revision? = null,
    val author: CommentAuthor? = null,
    val actionCounts: CommentActionCounts = CommentActionCounts(),
    val tags: List<CommentTag> = emptyList(),
    val extra: CommentExtra? = null,
) {
    fun hasVideo(): CommentVideoItem? {
        val metadata = extra?.videoMetadata ?: return null
        val stream = metadata.streams.firstOrNull() ?: return null
        val url = createKMPURLfromString(stream.url) ?: return null
        return CommentVideoItem(
            id = metadata.id,
            url = url,
            width = stream.width,
            height = stream.height,
        )
    }
}

@Serializable
data class CommentBodyNode(
    val type: CommentBodyNodeType,
    val content: String?,
    val children: List<CommentBodyNode>? = null,
)

@Serializable
enum class CommentBodyNodeType {
    @SerialName("PARAGRAPH")
    PARAGRAPH,

    @SerialName("BLOCKQUOTE")
    BLOCKQUOTE,
}

@Serializable
enum class CommentStatus {
    @SerialName("APPROVED")
    APPROVED,

    @SerialName("REJECTED")
    REJECTED,

    @SerialName("PREMOD")
    PENDING,

    // Often used for "Pre-moderation"
    @SerialName("SYSTEM_WITHDRAWN")
    WITHDRAWN,

    @SerialName("NONE")
    NONE,
    UNKNOWN, // Fallback for safety
}

@Serializable
data class CommentTag(
    val code: CommentTagCode,
)

@Serializable
enum class CommentTagCode {
    @SerialName("VIDEO_COMMENT")
    VIDEO_COMMENT,

    @SerialName("SOURCE_REPLY")
    SOURCE_REPLY,

    @SerialName("FEATURED")
    FEATURED,

    @SerialName("STAFF")
    STAFF,

    @SerialName("ADMIN")
    ADMIN,

    @SerialName("MODERATOR")
    MODERATOR,
    UNKNOWN,
}

@Serializable
data class Revision(
    val id: String,
)

@Serializable
data class CommentAuthor(
    val id: String,
    val username: String,
    val avatar: String? = null,
)

enum class CommentReactionType(
    val value: String,
) {
    HELPFUL("HELPFUL"),
    CARE("CARE"),
    SURPRISING("SURPRISING"),
    FRUSTRATING("FRUSTRATING"),
    FUNNY("FUNNY"),
}

enum class CommentSentimentType(
    val value: String,
) {
    UPVOTE("UPVOTE"),
    DOWNVOTE("DOWNVOTE"),
}

@Serializable
data class CommentActionCounts(
    val sentimentHelpful: CommentCount = CommentCount(0),
    val sentimentCare: CommentCount = CommentCount(0),
    val sentimentSurprising: CommentCount = CommentCount(0),
    val sentimentFunny: CommentCount = CommentCount(0),
    val sentimentFrustrating: CommentCount = CommentCount(0),
    val sentimentUpVote: CommentCount = CommentCount(0),
    val sentimentDownVote: CommentCount = CommentCount(0),
)

@Serializable
data class CommentCount(
    val total: Int = 0,
)

data class CommentDomainActionCounts(
    val helpful: Int = 0,
    val care: Int = 0,
    val surprising: Int = 0,
    val funny: Int = 0,
    val frustrating: Int = 0,
    val upVotes: Int = 0,
    val downVotes: Int = 0,
) {
    fun likeNet(): Int = upVotes - downVotes

    fun reactionsTotal(): Int = helpful + care + surprising + funny + frustrating
}

// -- Video Specific Models

data class CommentVideoItem(
    val id: String,
    val url: KMPURL,
    val width: Int,
    val height: Int,
)

@Serializable
data class CommentExtra(
    val videoMetadata: CommentVideoMetadata? = null,
)

@Serializable
data class CommentVideoMetadata(
    @SerialName("_id") val id: String,
    val duration: Long,
    @SerialName("promo_image") val promoImage: CommentImageInfo,
    val streams: List<CommentVideoStream>,
)

@Serializable
data class CommentImageInfo(
    val url: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class CommentVideoStream(
    val url: String,
    @SerialName("stream_type") val streamType: String,
    val width: Int,
    val height: Int,
)
