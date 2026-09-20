package com.washingtonpost.android.follow.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ArticleItem(@SerializedName("id") val id: String,
                       @SerializedName("url") val url: String,
                       @SerializedName("headline") val headline: String?,
                       @SerializedName("byline") val byline: String?,
                       @SerializedName("story_type") val storyType: String?,
                       @SerializedName("display_date") val displayDate: String?,
                       @SerializedName("image") val image: String?,
                       @SerializedName("blurb") val blurb: String?,
                       @SerializedName("lmt") val lmt: Long?) : Serializable