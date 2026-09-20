@file:JvmName("GridDebugUtils")

package com.wapo.flagship.features.grid

import android.content.Context
import com.wapo.android.commons.util.Utils

fun parseGridJson(context: Context, resourceId: Int) : GridEntity {
    val inputStream = context.resources.openRawResource(resourceId)
    val jsonString = Utils.inputStreamToString(inputStream)
    return FusionMapper.gson.fromJson<GridEntity>(jsonString, GridEntity::class.java)
}