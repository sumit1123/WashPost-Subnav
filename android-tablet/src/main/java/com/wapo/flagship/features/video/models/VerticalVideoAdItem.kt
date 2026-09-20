package com.wapo.flagship.features.video.models

/**
 * Item to hold Ad details for Ad items for Vertical Videos FullScreen
 */
data class VerticalVideoAdItem(
    val position: Int,
    override val adUnitId: String = "/701/android.wp.carousel/",
    override val templateId: String = "12123969",
) : VideoAdItem(adUnitId, templateId)
