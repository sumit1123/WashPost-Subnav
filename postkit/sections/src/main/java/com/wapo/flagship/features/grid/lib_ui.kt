@file:JvmName("GridUtils")

package com.wapo.flagship.features.grid


internal fun Int.toDp(density: Float): Int {
    return (this / density).toInt()
}