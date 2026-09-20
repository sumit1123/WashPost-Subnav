package com.washingtonpost.android.comics.model

import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import java.io.Serializable

open class ComicItem(
    open var id: String? = ""
) : Serializable

data class AdItem(
    val commercialNode: String,
    val adPosition: String,
    val adSlotType: AdSlotType = AdSlotType.SHORT,
    val adDimension: AdDimension = AdDimension.Medium
) : ComicItem(commercialNode)