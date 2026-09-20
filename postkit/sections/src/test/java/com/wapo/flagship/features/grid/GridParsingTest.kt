/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid

import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStreamReader
import java.lang.reflect.Type

@RunWith(RobolectricTestRunner::class)
class GridParsingTests {

    @Test
    fun testBrightsJson() {
        val grid = FusionMapper.gson.fromJsonRes<GridEntity>("brights.json", GridEntity::class.java)

        val carouselItems = grid.regions
            .asSequence()
            .flatMap { it.items }
            .filterIsInstance<ChainEntity>()
            .flatMap { it.items }
            .filterNotNull()
            .flatMap { it.items }
            .filterIsInstance<CarouselItemEntity>()
            .toList()

        assertTrue(carouselItems.isNotEmpty())

        carouselItems.forEach { carousel ->
            carousel.items?.let { assertTrue(it.isNotEmpty()) }
        }
    }
}

fun <T> Gson.fromJsonRes(name: String, type: Type): T = fromJson<T>(InputStreamReader(javaClass.classLoader?.getResourceAsStream(name)), type)
