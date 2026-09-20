package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "contributors")
    val contributors: Any?,
    @Json(name = "coordinates")
    val coordinates: Any?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "display_text_range")
    val displayTextRange: List<Int>?,
    @Json(name = "entities")
    val entities: Entities?,
    @Json(name = "extended_entities")
    val extendedEntities: ExtendedEntities?,
    @Json(name = "favorite_count")
    val favoriteCount: Int?,
    @Json(name = "favorited")
    val favorited: Boolean?,
    @Json(name = "full_text")
    val fullText: String?,
    @Json(name = "geo")
    val geo: Any?,
    @Json(name = "id")
    val id: Long?,
    @Json(name = "id_str")
    val idStr: String?,
    @Json(name = "in_reply_to_screen_name")
    val inReplyToScreenName: Any?,
    @Json(name = "in_reply_to_status_id")
    val inReplyToStatusId: Any?,
    @Json(name = "in_reply_to_status_id_str")
    val inReplyToStatusIdStr: Any?,
    @Json(name = "in_reply_to_user_id")
    val inReplyToUserId: Any?,
    @Json(name = "in_reply_to_user_id_str")
    val inReplyToUserIdStr: Any?,
    @Json(name = "is_quote_status")
    val isQuoteStatus: Boolean?,
    @Json(name = "lang")
    val lang: String?,
    @Json(name = "place")
    val place: Any?,
    @Json(name = "possibly_sensitive")
    val possiblySensitive: Boolean?,
    @Json(name = "possibly_sensitive_appealable")
    val possiblySensitiveAppealable: Boolean?,
    @Json(name = "retweet_count")
    val retweetCount: Int?,
    @Json(name = "retweeted")
    val retweeted: Boolean?,
    @Json(name = "source")
    val source: String?,
    @Json(name = "text")
    val text: String?,
    @Json(name = "truncated")
    val truncated: Boolean?,
    @Json(name = "user")
    val user: User?,
)
