package com.washingtonpost.foryou.data

enum class ForYouContentType(val type: String) {
    ARTICLE("text"),
    VIDEO("video");

    companion object {
        fun from(value: String?): ForYouContentType? =
            entries.firstOrNull { it.type.equals(value, ignoreCase = true) }
    }
}