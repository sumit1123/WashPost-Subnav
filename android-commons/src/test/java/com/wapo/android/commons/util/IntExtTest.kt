package com.wapo.android.commons.util

import org.junit.Assert
import org.junit.Test

class IntExtTest {

    @Test
    fun testTruncatedString() {
        //  Assert negative number
        Assert.assertNull((-5).truncatedString())

        //  Assert low
        Assert.assertTrue(24.truncatedString() == "24")

        //  Assert hundreds
        Assert.assertTrue(152.truncatedString() == "152")

        //  Assert thousands
        Assert.assertTrue(1_234.truncatedString() == "1k")
        Assert.assertTrue(123_456.truncatedString() == "123k")

        //  Assert millions
        Assert.assertTrue(2_345_678.truncatedString() == "2M")
        Assert.assertTrue(12_345_678.truncatedString() == "12M")
    }
}