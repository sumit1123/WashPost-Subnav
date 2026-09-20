package com.washpost.airship

import com.google.gson.annotations.SerializedName
import com.wapo.android.push.PushNotification
import java.util.*

data class PushMessageResponse(
        val headline: String?,
        val blurb: String?, // Not using this. Using from custom
        val title: String?, // Not using this. Using from custom
        val custom: CustomPayload?
)

data class CustomPayload(
        @SerializedName("text") val blurb: String?,
        @SerializedName("pushID") val pushId: String?,
        @SerializedName("datetime") val dateTime: Date?,
        @SerializedName("contentURL") val contentUrl: String?,
        @SerializedName("imageURL") val imageUrl: String?,
        @SerializedName("targetTopic") val targetTopic: String?,
        @SerializedName("analyticsTopic") val analyticsTopic: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("interactionType") val interactionType: String?,
        @SerializedName("shouldUpdateCarousel") val shouldUpdateCarousel: String?,
        @SerializedName("type") val type: String?,
        @SerializedName("testGroups") val testGroups: Map<String, String>?,
        @SerializedName("segments") val segments: ArrayList<PushNotification.SegmentedImage>?)