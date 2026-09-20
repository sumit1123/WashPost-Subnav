package com.washingtonpost.android.paywall

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallReactiveTest {

    @After
    fun tearDown() {
        PaywallReactive.reset()
    }

    @Test
    fun `server attributes retain their values for ad-free checks`() {
        PaywallReactive.updateSubAttributes(mapOf("NOADS" to "1"))

        val attributes = PaywallReactive.subAttributes.value

        assertEquals(setOf("NOADS:1"), attributes)
        assertTrue(PaywallReactive.isAdFreeInAttributes(attributes, "NOADS:1", "EU_NOADS:1"))
    }

    @Test
    fun `ad-free attributes require the enabled value`() {
        PaywallReactive.updateSubAttributes(mapOf("NOADS" to "0"))

        assertFalse(
            PaywallReactive.isAdFreeInAttributes(
                PaywallReactive.subAttributes.value,
                "NOADS:1",
                "EU_NOADS:1",
            ),
        )
    }
}
