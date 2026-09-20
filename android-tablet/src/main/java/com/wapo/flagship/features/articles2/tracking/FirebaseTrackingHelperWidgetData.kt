package com.wapo.flagship.features.articles2.tracking

/**
 * This class holds necessary information required for tracking articles that are widget originated.
 * [isWidgetOriginated] If the article was widget orifginated.
 * [widgetType] Type of the widget.
 */
data class FirebaseTrackingHelperWidgetData(
    val isWidgetOriginated: Boolean = false,
    val widgetType: String? = null,
)
