package com.washingtonpost.android.follow.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AuthorItem(@SerializedName("id") val id: String?,
                      @SerializedName("name") val name: String?,
                      @SerializedName("bio") val bio: String?,
                      @SerializedName("expertise") val expertise: String?,
                      @SerializedName("image") val image: String?,
                      @SerializedName("items") val items: List<ArticleItem>?,
                      @SerializedName("lmt") val lmt: Long) : Serializable