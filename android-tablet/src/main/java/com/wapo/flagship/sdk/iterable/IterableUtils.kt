// Copyright (c) 2025 The Washington Post. All rights reserved.
@file:JvmName("IterableUtils")

package com.wapo.flagship.sdk.iterable

import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.paywallconf.WallMap2

fun getWallMap(placementId: Long): WallMap2? {
    return ConfigManager.getInstance().config.paywallConf.run {
        metering?.wallMap2?.firstOrNull { it.placement == placementId }
    }
}

// Returns a list of placement ids for walls whose version exactly matches the currently
// supported blocker config version. Placements with a lower version (e.g. v5 and below)
// are excluded so incompatible blocker configs cannot be routed into a v6+ build.
fun getWallPlacementIds(currentSupportedBlockerConfigVersion: Int): List<Long> {
    return ConfigManager.getInstance().config.paywallConf.run {
        metering?.wallMap2
            ?.filter { it.version == currentSupportedBlockerConfigVersion }
            ?.mapNotNull { it.placement }
    } ?: emptyList()
}