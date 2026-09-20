package com.washingtonpost.android.follow.database.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(indices = [
    Index(value = ["lmt"]),
    Index(value = ["name"]),
    Index(value = ["date_added"])
])
data class AuthorEntity(@PrimaryKey @SerializedName("id") @ColumnInfo(name = "author_id") val authorId: String,
                        @SerializedName("name")
                        @ColumnInfo(name = "name") val name: String,
                        @SerializedName("bio")
                        @ColumnInfo(name = "bio") val bio: String?,
                        @SerializedName("expertise")
                        @ColumnInfo(name = "expertise") val expertise: String?,
                        @SerializedName("image")
                        @ColumnInfo(name = "image") val image: String?,
                        @SerializedName("lmt")
                        @ColumnInfo(name = "lmt") val lmt: Long,
                        @SerializedName("lastUpdated")
                        @ColumnInfo(name = "date_added") val dateAdded: Long) : Parcelable