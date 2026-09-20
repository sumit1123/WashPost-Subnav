package com.wapo.flagship.common

/**
 * map this value from one range to another
 */
fun Float.map(
    fromMin: Float,
    fromMax: Float,
    toMin: Float,
    toMax: Float,
): Float = (this - fromMin) * (toMax - toMin) / (fromMax - fromMin) + toMin
