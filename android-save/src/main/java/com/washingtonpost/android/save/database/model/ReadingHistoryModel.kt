package com.washingtonpost.android.save.database.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["lmt"]),
        Index(value = ["contentUrl"], unique = true)
    ]
)
class ReadingHistoryModel(
    @PrimaryKey
    var contentUrl: String,
    var canonicalURL: String? = null,
    var lmt: Long,
    var contentId: String?,
    @NonNull var isListened: Boolean = false
)