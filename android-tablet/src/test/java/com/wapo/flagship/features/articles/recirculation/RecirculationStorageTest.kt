/*
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.flagship.features.articles.recirculation

import com.wapo.flagship.features.articles.recirculation.model.MRECredits
import com.wapo.flagship.features.articles.recirculation.model.MostReadSubElement
import org.junit.Assert.*
import org.junit.Test

/**
 * Created by Jayesh Elamgodil on 08/29/19.
 */
class RecirculationStorageTest {
    @Test
    fun testByLineGeneration() {
        var credits = MRECredits(listOf(getMRSEWithByName("John Smith")))
        assertEquals("By John Smith", getByline(credits))

        credits =
            MRECredits(
                listOf(getMRSEWithByName("John Smith"), getMRSEWithByName("John Smith")),
            )
        assertEquals("By John Smith and John Smith", getByline(credits))

        credits =
            MRECredits(
                listOf(
                    getMRSEWithByName("John Smith"),
                    getMRSEWithByName("John Smith"),
                    getMRSEWithByName("Agent John"),
                ),
            )
        assertEquals("By John Smith, John Smith and Agent John", getByline(credits))

        credits =
            MRECredits(
                listOf(
                    getMRSEWithByName("John Smith"),
                    getMRSEWithByName("John Smith"),
                    getMRSEWithByName("Agent John"),
                    getMRSEWithByName("Agent"),
                ),
            )
        assertEquals("By John Smith, John Smith, Agent John and Agent", getByline(credits))
    }

    private fun getMRSEWithByName(name: String?) =
        MostReadSubElement(
            null,
            name,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        )

    private fun getByline(credits: MRECredits) =
        if (credits?.by != null) {
            val sb = StringBuilder()
            val size = credits?.by!!.size
            if (size > 0) {
                sb.append("By ")
                credits.by!!.forEachIndexed { index, mostReadSubElement ->
                    sb.append(mostReadSubElement.name)
                    if (index == (size - 2)) {
                        sb.append(" and ")
                    } else if (index < (size - 2)) {
                        sb.append(", ")
                    }
                }
            }
            sb.toString()
        } else {
            ""
        }
}
