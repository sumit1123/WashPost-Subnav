package com.wapo.flagship.features.articles2.navigation_models

/**
 * This class holds the required information for dispatching the tracking event for User behavior.
 * [articleId] id/url of the article that is being tracked.
 * [sourceSection] section of the article e.g. Politics.
 */
data class UserBehaviorTrackingModel(
    val articleId: String,
    val sourceSection: String?,
    val hasBeenTracked: Boolean = false,
)
