package com.wapo.kmpshared.features.conversations.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CommentFlagReason(
    val message: String,
) {
    @SerialName("COMMENT_REPORTED_OFFENSIVE")
    OFFENSIVE("This comment is offensive."),

    @SerialName("COMMENT_REPORTED_ABUSIVE")
    ABUSIVE("This commenter is being abusive."),

    @SerialName("COMMENT_REPORTED_SPAM")
    SPAM("This looks like an ad or marketing."),

    @SerialName("COMMENT_REPORTED_FALSE_OR_MISLEADING_INFORMATION")
    MISINFORMATION("This comment includes false or misleading information."),

    @SerialName("DISAGREE")
    DISAGREE("I disagree with this comment."),
}
