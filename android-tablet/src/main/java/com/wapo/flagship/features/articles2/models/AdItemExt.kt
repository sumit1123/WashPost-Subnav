// Copyright (c) 2023 The Washington Post. All rights reserved.
package com.wapo.flagship.features.articles2.models

import com.wapo.adsinf.models.AdSlotType
import com.wapo.flagship.features.articles2.models.constants.AdKind
import com.wapo.flagship.features.articles2.models.deserialized.Ad

fun AdItem.toAd(): Ad {
    val size =
        if (adKey?.adSlotType == AdSlotType.TALL) {
            AdSlotType.TALL.value
        } else {
            AdSlotType.SHORT.value
        }
    return Ad(
        "ad",
        AdKind.BLOCK.value,
        size,
        adKey?.adKey,
        null,
        null,
        null
    )
}
