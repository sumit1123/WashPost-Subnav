package com.wapo.flagship.features.articles2.utils

object KeyHelper {
    fun createKey(
        position: Int,
        text: String,
    ): String? = "pos: $position::$text"
}
