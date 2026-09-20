package com.wapo.flagship.util

import java.util.*

object UtilsKt {
    /**
     * Analytics uses a variant of snake_case.
     * - Non-alphanumeric characters are replaced with an underscore.
     * - Consecutive underscores are replaced with a single underscore.
     */
    fun toAnalyticsSnakeCase(text: String): String =
        text
            .lowercase(Locale.US)
            .replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replace("[_]+".toRegex(), "_")
}
