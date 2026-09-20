package com.washingtonpost.android.config.utils

import org.junit.Assert
import org.junit.Test

class MapUtilsTest {

    @Test
    fun mergeMapsShouldOverridePrimitiveValues() {
        val base = mapOf(
            "key1" to "value1",
            "key2" to 123,
        )
        val override = mapOf(
            "key2" to 456,
            "key3" to true,
        )

        val result = MapUtils.mergeMaps(base, override)

        Assert.assertEquals("value1", result["key1"])
        Assert.assertEquals(456, result["key2"])
        Assert.assertEquals(true, result["key3"])
    }

    @Test
    fun mergeMapsShouldDeepMergeNestedMaps() {
        val base = mutableMapOf(
            "config" to mapOf(
                "theme" to "light",
                "timeout" to 30
            )
        )
        val override = mapOf(
            "config" to mapOf(
                "timeout" to 60
            )
        )

        val result = MapUtils.mergeMaps(base, override)
        val config = result["config"] as Map<*, *>

        Assert.assertEquals("light", config["theme"])
        Assert.assertEquals(60, config["timeout"])
    }

    @Test
    fun mergeMapsShouldOverrideLists() {
        val base = mutableMapOf(
            "features" to listOf("A", "B")
        )
        val override = mapOf(
            "features" to listOf("C")
        )

        val result = MapUtils.mergeMaps(base, override)

        Assert.assertEquals(listOf("C"), result["features"])
    }

    @Test
    fun mergeMapsShouldApplyMultipleOverridesInOrder() {
        val source = mapOf(
            "value" to 1,
            "config" to mapOf(
                "enabled" to false,
                "timeout" to 30
            )
        )
        val override1 = mapOf(
            "config" to mapOf(
                "enabled" to true
            )
        )
        val override2 = mapOf(
            "value" to 2,
            "config" to mapOf(
                "timeout" to 60
            )
        )

        val result = MapUtils.mergeMaps(source, listOf(override1, override2))

        Assert.assertEquals(2, result["value"])
        val config = result["config"] as Map<*, *>
        Assert.assertEquals(true, config["enabled"])
        Assert.assertEquals(60, config["timeout"])
    }
}