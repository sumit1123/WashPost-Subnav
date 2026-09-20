package com.wapo.kmpshared.features.conversations.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CommentErrorType {
    @SerialName("INVALID_REQUEST_ERROR")
    INVALID_REQUEST,

    @SerialName("MODERATION_NUDGE_ERROR")
    MODERATION_NUDGE,
    UNKNOWN,
}

@Serializable
enum class CommentErrorCode {
    // Authentication & System
    @SerialName("AUTHENTICATION_ERROR")
    AUTHENTICATION_ERROR,

    @SerialName("PERSISTED_QUERY_NOT_FOUND")
    PERSISTED_QUERY_NOT_FOUND,

    @SerialName("RATE_LIMIT_EXCEEDED")
    RATE_LIMIT_EXCEEDED,

    // Not Found
    @SerialName("STORY_NOT_FOUND")
    STORY_NOT_FOUND,

    @SerialName("STORY_URL_NOT_PERMITTED")
    STORY_URL_NOT_PERMITTED,

    @SerialName("COMMENT_NOT_FOUND")
    COMMENT_NOT_FOUND,

    // Story State
    @SerialName("STORY_CLOSED")
    STORY_CLOSED,

    @SerialName("COMMENTING_DISABLED")
    COMMENTING_DISABLED,

    @SerialName("CANNOT_CREATE_COMMENT_ON_ARCHIVED_STORY")
    ARCHIVED_STORY,

    // User State
    @SerialName("USER_BANNED")
    USER_BANNED,

    @SerialName("USER_SITE_BANNED")
    USER_SITE_BANNED,

    @SerialName("USER_SUSPENDED")
    USER_SUSPENDED,

    @SerialName("USER_WARNED")
    USER_WARNED,

    // Validation
    @SerialName("COMMENT_BODY_TOO_SHORT")
    BODY_TOO_SHORT,

    @SerialName("COMMENT_BODY_EXCEEDS_MAX_LENGTH")
    BODY_TOO_LONG,

    // Moderation
    @SerialName("TOXIC_COMMENT")
    TOXIC_COMMENT,

    @SerialName("SPAM_COMMENT")
    SPAM_COMMENT,

    @SerialName("REPEAT_POST")
    REPEAT_POST,

    UNKNOWN,
}
