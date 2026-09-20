package com.wapo.flagship.features.articles2.navigation_models

/**
 * This data class is used while performing share operation. The instance is created after successful fetch of an article either from the database or network.
 */
data class ShareContent(
    val url: String,
    val content: String?,
    val byLine: String?,
)
