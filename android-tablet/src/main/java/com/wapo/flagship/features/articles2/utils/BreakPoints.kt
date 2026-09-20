package com.wapo.flagship.features.articles2.utils

import com.squareup.moshi.JsonClass

object BreakPoints {
    private val breakPointMap =
        listOf(
            LayoutSpec(0, 767, Layout.SMALL),
            LayoutSpec(768, Int.MAX_VALUE, Layout.LARGE),
        )

    /**
     * Layout types based on type of devices
     * SMALL - Phones
     * LARGE - Tablets
     */
    enum class Layout(
        val value: String,
    ) {
        SMALL("small"),
        LARGE("large"),
    }

    @JsonClass(generateAdapter = true)
    class LayoutSpec(
        val smallestScreenWidth: Int,
        val largestScreenWidth: Int,
        val layout: Layout,
    )

    fun getLayoutSpec(width: Int): LayoutSpec =
        breakPointMap.find { width >= it.smallestScreenWidth && width <= it.largestScreenWidth }
            ?: breakPointMap[0]
}
