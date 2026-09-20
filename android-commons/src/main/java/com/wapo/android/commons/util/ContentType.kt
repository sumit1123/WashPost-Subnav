package com.wapo.android.commons.util

/**
 * This enum class is a common content type that is used throughout history data that
 * distinguish between content types of article and podcast
 */
enum class ContentType(val type: String) {
    ARTICLE("article"),
    PODCAST("podcast"),
    UNKNOWN("unknown")
}
