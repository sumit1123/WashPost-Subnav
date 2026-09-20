package com.washingtonpost.android.follow.network

import com.google.gson.annotations.SerializedName
import com.washingtonpost.android.follow.database.model.AuthorEntity

data class FollowAuthor(
    @SerializedName("resources") val authorList: List<AuthorData>
)

data class AuthorData(
    @SerializedName("id")
    val id: String?,
    @SerializedName("following")
    val following: Boolean?,
    @SerializedName("lastUpdated")
    val lastUpdated: Long? = 0

)
data class AuthorMetaData(
    @SerializedName("id")
    val id: String?,
    @SerializedName("following")
    val following: Boolean?,
    @SerializedName("lastUpdated")
    val lastUpdated: Long? = 0
)

data class AuthorMetaDataFromRemote(
    @SerializedName("resources") val authorList: List<AuthorEntity>?
)