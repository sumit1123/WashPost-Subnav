// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.android.commons.iterable

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttributionInfo(
    val placementId: Long = -1,
    val campaignId: Int,
    val messageId: String
) : Parcelable

const val ARG_KEY_ATTRIBUTION_INFO = "ATTRIBUTION_INFO"

fun AttributionInfo?.toBundle(): Bundle? {
    return this?.let {
        Bundle().apply { putParcelable(ARG_KEY_ATTRIBUTION_INFO, it) }
    }
}

fun AttributionInfo?.addTo(bundle: Bundle?) {
    this?.run {
        bundle?.apply { putParcelable(ARG_KEY_ATTRIBUTION_INFO, this) }
    }
}

fun Bundle?.getAttributionInfo(): AttributionInfo? {
    return this?.getParcelable(ARG_KEY_ATTRIBUTION_INFO)
}
