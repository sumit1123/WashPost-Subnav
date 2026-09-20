package com.wapo.flagship.features.grid.model

import android.content.Context
import com.wapo.android.commons.util.UiUtils
import com.wapo.flagship.features.grid.ComponentSize
import kotlin.math.abs

data class WebComponent(
    val url: String?,
    val sizes: List<ComponentSize> = listOf()
) : Item()


