package com.wapo.flagship.content.notifications

import com.google.gson.annotations.SerializedName
import java.util.*

data class NotificationData(val alert: String?) {
    var id : Int = -1
    var notifId: String? = null
    var isRead: Boolean = false
    @Deprecated("No longer passed in the json")
    var feed : String? = null
    var storyUrl : String? = null
    var headline : String? = null
    var kicker : String? = null
    var type : String? = null
    var imageUrl: String? = null
    var pushIconImageUrl: String? = null
    var timestamp: String? = null
    var notifArticleType: String? = NotificationArticleType.ARTICLE.name

    fun prettyTitle() = headline ?: type

    companion object {
        @JvmField
        val STORY_URL = "story_url"
        @JvmField
        val ALERT = "alert"
        @JvmField
        val HEADLINE = "headline"
        @JvmField
        val FEED = "feed"
        @JvmField
        val KICKER = "kicker"
        @JvmField
        val TYPE = "type"
        @JvmField
        val IMAGE_URL = "image_url"
        @JvmField
        val PUSH_ICON_IMAGE_URL = "push_icon_image_url"
        @JvmField
        val TIMESTAMP = "timestamp"
        @JvmField
        val NOTIF_ID = "notifId"
        @JvmField
        val NOTIF_ARTICLE_TYPE = "notifArticleType"
    }
}


fun NotificationData.toMap(): HashMap<String, String?> {
    return hashMapOf(
            NotificationData.ALERT to alert,
            NotificationData.STORY_URL to storyUrl,
            NotificationData.HEADLINE to headline,
            NotificationData.FEED to feed,
            NotificationData.KICKER to kicker,
            NotificationData.TYPE to type,
            NotificationData.IMAGE_URL to imageUrl,
            NotificationData.PUSH_ICON_IMAGE_URL to pushIconImageUrl,
            NotificationData.TIMESTAMP to timestamp,
            NotificationData.NOTIF_ID to notifId,
            NotificationData.NOTIF_ARTICLE_TYPE to notifArticleType
    )
}

fun NotificationData?.fromMap(map: Map<String, String?>) : NotificationData {
    val alert = map[NotificationData.ALERT] ?: ""
    return NotificationData(alert).apply {
        storyUrl = map[NotificationData.STORY_URL]
        headline = map[NotificationData.HEADLINE]
        feed = map[NotificationData.FEED]
        kicker = map[NotificationData.KICKER]
        type = map[NotificationData.TYPE]
        imageUrl = map[NotificationData.IMAGE_URL]
        pushIconImageUrl = map[NotificationData.PUSH_ICON_IMAGE_URL]
        timestamp = map[NotificationData.TIMESTAMP]
        notifId = map[NotificationData.NOTIF_ID]
        notifArticleType = map[NotificationData.NOTIF_ARTICLE_TYPE]
    }
}

enum class NotificationArticleType {
    @SerializedName("article") ARTICLE,
    @SerializedName("video") VIDEO
}