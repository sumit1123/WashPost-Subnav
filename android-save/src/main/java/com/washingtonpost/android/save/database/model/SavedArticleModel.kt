package com.washingtonpost.android.save.database.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [
    Index(value = ["lmt"]),
    Index(value = ["contentURL"], unique = true)
])
class SavedArticleModel(
        @NonNull var contentURL: String,
        @NonNull var lmt: Long,

        @ColumnInfo(name = "isListened", defaultValue = "0")
        @NonNull var isListened:Boolean  = false

) {
    @PrimaryKey(autoGenerate = true)
    @NonNull var id: Long = 0
}