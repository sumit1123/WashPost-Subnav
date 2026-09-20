package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This is a model class for the response body for an api call to get the available content packs
 * POST /static/my-post/content-pack.json
 * Check [ContentPacksService.getContentPackItems] for more details
 */
@JsonClass(generateAdapter = true)
data class ContentPacksUiResponseBody(
    @SerializedName("contentPacks")
    @Json(name = "contentPacks")
    val contentPacks: List<ContentPackUiItem?>? = null,
)

@JsonClass(generateAdapter = true)
data class ContentPackUiItem(
    @SerializedName("trackingId")
    @Json(name = "trackingId")
    val trackingId: String? = null,
    @SerializedName("heading")
    @Json(name = "heading")
    val heading: String? = null,
    @SerializedName("description")
    @Json(name = "description")
    val description: String? = null,
    @SerializedName("image")
    @Json(name = "image")
    val image: String? = null,
    @SerializedName("transparentImage")
    @Json(name = "transparentImage")
    val transparentImage: String? = null,
    @SerializedName("transparentSmall")
    @Json(name = "transparentSmall")
    val transparentSmall: String? = null,
    @SerializedName("id")
    @Json(name = "id")
    val id: String? = null,
    @SerializedName("referenceId")
    @Json(name = "referenceId")
    val referenceId: String? = null,
    @SerializedName("priority")
    @Json(name = "priority")
    val priority: Int? = null,
    @SerializedName("disabled")
    @Json(name = "disabled")
    val disabled: Boolean? = null,
    @SerializedName("onboardingDisabled")
    @Json(name = "onboardingDisabled")
    val onboardingDisabled: Boolean? = null,
    @SerializedName("emoji")
    @Json(name = "emoji")
    val emoji: String? = null,
    @SerializedName("emojiUrl")
    @Json(name = "emojiUrl")
    val emojiUrl: String? = null,
    @SerializedName("destination_url")
    @Json(name = "destination_url")
    val destinationUrl: String? = null,
    @SerializedName("myPostPriority")
    @Json(name = "myPostPriority")
    val myPostPriority: Int? = null,
    @SerializedName("followable")
    @Json(name = "followable")
    val followable: Followable? = null,
)

// TODO create custom adapter that returns null Followable if certain items are null/missing
@JsonClass(generateAdapter = true)
data class Followable(
    @SerializedName("disabled")
    @Json(name = "disabled")
    val disabled: Boolean? = false,
    @SerializedName("heading")
    @Json(name = "heading")
    val heading: String?,
    @SerializedName("registration_heading")
    @Json(name = "registration_heading")
    val registrationHeading: String? = "Follow $heading",
    @SerializedName("unfollowed_affordance_title")
    @Json(name = "unfollowed_affordance_title")
    val unfollowedAffordanceTitle: String? = heading,
    @SerializedName("followed_affordance_title")
    @Json(name = "followed_affordance_title")
    val followedAffordanceTitle: String? = heading,
    @SerializedName("image")
    @Json(name = "image")
    val image: String? = null,
    @SerializedName("unfollowed_prompt")
    @Json(name = "unfollowed_prompt")
    val unfollowedPrompt: String? = "Follow to see more coverage on this topic in your recommendations.",
    @SerializedName("followed_prompt")
    @Json(name = "followed_prompt")
    val followedPrompt: String? = "You'll see more coverage on this topic in your recommendations.",
    @SerializedName("newsletters")
    @Json(name = "newsletters")
    val newsletters: List<Newsletter?>? = null,
    @SerializedName("notifications")
    @Json(name = "notifications")
    val notifications: List<Notification?>? = null,
    @SerializedName("bullets")
    @Json(name = "bullets")
    val bullets: List<Bullet?>? = null,
)

@JsonClass(generateAdapter = true)
data class Newsletter(
    @SerializedName("id")
    @Json(name = "id")
    val id: String?,
    @SerializedName("name")
    @Json(name = "name")
    val name: String?,
    @SerializedName("description")
    @Json(name = "description")
    val description: String?,
)

@JsonClass(generateAdapter = true)
data class Notification(
    @SerializedName("id")
    @Json(name = "id")
    val id: String?,
    @SerializedName("name")
    @Json(name = "name")
    val name: String?,
    @SerializedName("description")
    @Json(name = "description")
    val description: String?,
)

@JsonClass(generateAdapter = true)
data class Bullet(
    @SerializedName("icon")
    @Json(name = "icon")
    val icon: String? = null,
    @SerializedName("icon_image")
    @Json(name = "icon_image")
    val iconImage: String? = null,
    @SerializedName("text")
    @Json(name = "text")
    val text: String? = null,
)
