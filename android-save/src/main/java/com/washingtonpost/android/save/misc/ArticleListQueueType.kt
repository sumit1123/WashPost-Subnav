package com.washingtonpost.android.save.misc

import androidx.room.TypeConverter

enum class ArticleListQueueType(val value: Int) {
    ADD_ARTICLE(0),
    DELETE_ARTICLE(1);

    companion object {
        @JvmStatic
        @TypeConverter
        fun getArticleListQueueType(value: Int): ArticleListQueueType? {
            return values().find { it.value == value }
        }

        @JvmStatic
        @TypeConverter
        fun getArticleListQueueType(status: ArticleListQueueType): Int? {
            return status.value
        }
    }
}