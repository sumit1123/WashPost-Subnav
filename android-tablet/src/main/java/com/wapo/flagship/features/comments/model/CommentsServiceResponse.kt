package com.wapo.flagship.features.comments.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommentsServiceResponse(
    @Json(name = "comments")
    val comments: Comments?,
)

@JsonClass(generateAdapter = true)
data class Comments(
    @Json(name = "count")
    val count: Int? = 0,

    @Json(name = "source_annotations")
    val sourceAnnotations: List<SourceAnnotation?>?
)

@JsonClass(generateAdapter = true)
data class SourceAnnotation(
    @Json(name = "source_id")
    val sourceId: String?,

    @Json(name = "source_comments")
    val sourceComments: List<SourceComment?>?,
)

@JsonClass(generateAdapter = true)
data class SourceComment(
    @Json(name = "url")
    val url: String?,

    @Json(name = "author")
    val author: Author?,

    @Json(name = "body")
    val body: String?,

    @Json(name = "video")
    val video: Video?,

    @Json(name = "replies")
    val replies: List<SourceComment?>?,

    @Json(name = "reply_count")
    val replyCount: Int?,

    @Json(name = "created")
    val created: String?
)

@JsonClass(generateAdapter = true)
data class Author(
    @Json(name = "name")
    val name: String?,

    @Json(name = "expertise")
    val expertise: String?,

    @Json(name = "image")
    val image: String?
)

@JsonClass(generateAdapter = true)
data class Video(
    /** Video duration in seconds */
    @Json(name = "duration")
    val duration: Double?,

    @Json(name = "promo_image")
    val promoImage: PromoImage?,

    @Json(name = "transcript")
    val transcript: Transcript?
)

@JsonClass(generateAdapter = true)
data class PromoImage(
    @Json(name = "url")
    val url: String?,

    @Json(name = "aspect_ratio")
    val aspectRatio: Double?
)

@JsonClass(generateAdapter = true)
data class Transcript(
    @Json(name = "text")
    val text: String?
)
