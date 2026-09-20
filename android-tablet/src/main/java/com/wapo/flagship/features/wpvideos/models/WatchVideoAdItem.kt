package com.wapo.flagship.features.wpvideos.models

import com.wapo.flagship.features.video.models.VideoAdItem

data class WatchVideoAdItem(
    val position: Int,
    override val adUnitId: String = "/701/android.wp.watch/",
    override val templateId: String = "12123969",
) : VideoAdItem(adUnitId, templateId)