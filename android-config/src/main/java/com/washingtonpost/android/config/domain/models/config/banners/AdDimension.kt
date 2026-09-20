package com.washingtonpost.android.config.domain.models.config.banners

sealed class AdDimension(open val w: Int, open val h: Int) {
    data object Fluid : AdDimension(0, 0)
    data object Medium : AdDimension(300, 250)
    data object Tall : AdDimension(300, 600)
    data object Banner620x250 : AdDimension(620, 250)
    data object Banner728x90 : AdDimension(728, 90)
    data object Banner970x250 : AdDimension(970, 250)

    companion object {
        fun values() = listOf(Fluid, Medium, Tall, Banner620x250, Banner728x90, Banner970x250)
    }
}