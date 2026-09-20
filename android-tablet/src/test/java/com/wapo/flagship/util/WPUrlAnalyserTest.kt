package com.wapo.flagship.util

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class WPUrlAnalyserTest {
    private val urlAllowList =
        listOf(
            "https://subscribe.washingtonpost.com/",
            "https://subscribe.washingtonpost.com/acq",
            "https://subscribe.washingtonpost.com/acquisition/?promo=mar2020_marty&view=1",
            "https://subscribe.washingtonpost.com/checkout",
            "https://www.washingtonpost.com/subscribe/",
            "https://www.washingtonpost.com/subscribe/checkout",
            "https://www.washingtonpost.com/subscribe/acq",
            "https://subs-stage.washingtonpost.com/",
            "https://subs-stage.washingtonpost.com/acq",
            "https://subs-stage.washingtonpost.com/checkout",
            "https://subscribe.digitalink.com/",
            "https://subscribe.digitalink.com/acq",
            "https://subscribe.digitalink.com/checkout",
        )

    private val urlDenyList =
        listOf(
            "https://subscribe.washingtonpost.com/something",
            "https://subscribe.washingtonpost.com/profile",
            "https://subscribe.washingtonpost.com/signin",
            "https://subs.washingtonpost.com/",
            "https://subs.washingtonpost.com/acq",
            "https://subs.washingtonpost.com/checkout",
        )

    @Test
    fun testIsAcqOrCheckIn() {
        WPUrlAnalyser.init()

        println("Tests for urls that are accepted")
        urlAllowList.forEach {
            val result = WPUrlAnalyser.getWPUrlAnalyser().isAcqOrCheckoutUrl(it)
            println("Result : $result for $it")
            assertTrue(result)
        }
        println("\nTests for urls NOT accepted")
        urlDenyList.forEach {
            val result = WPUrlAnalyser.getWPUrlAnalyser().isAcqOrCheckoutUrl(it)
            println("Result : $result for $it")
            assertFalse(result)
        }
    }
}
