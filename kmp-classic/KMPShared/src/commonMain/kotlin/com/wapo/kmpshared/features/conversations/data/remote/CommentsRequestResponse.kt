package com.wapo.kmpshared.features.conversations.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CommentsRequest(
    val id: String,
    val query: String,
    val variables: JsonObject,
)

@Serializable
data class CommentsResponse<T>(
    val data: T? = null,
    val errors: List<CommentServiceError>? = null,
)

@Serializable
data class CommentServiceError(
    val message: String,
    val extensions: CommentErrorExtensions? = null,
)

@Serializable
data class CommentErrorExtensions(
    val code: CommentErrorCode = CommentErrorCode.UNKNOWN,
    val type: CommentErrorType = CommentErrorType.UNKNOWN,
)
