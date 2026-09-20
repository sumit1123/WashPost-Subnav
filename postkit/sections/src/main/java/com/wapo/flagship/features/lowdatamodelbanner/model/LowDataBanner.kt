/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.lowdatamodelbanner.model

import com.wapo.flagship.features.grid.model.Item

data class LowDataBanner(
    val isLowDataBannerEnable: Boolean
) : Item()