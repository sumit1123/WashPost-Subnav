package com.washingtonpost.android.save.database.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Index
import com.washingtonpost.android.save.misc.ArticleListQueueType

@Entity(
        indices = [
            Index(value = ["lmt"])
        ],
        primaryKeys = [
            "contentURL",
            "articleListQueueType"
        ]
)
class ArticleListQueue(
        @NonNull val contentURL: String,
        @NonNull val lmt: Long,
        @NonNull val articleListQueueType: ArticleListQueueType
)