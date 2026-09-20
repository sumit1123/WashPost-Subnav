package com.wapo.android.commons.util

import org.junit.Assert
import org.junit.Test

class UtilsTest {

    @Test
    fun extractPrice() {
        val input = "$9.99";
        val output = "9.99";
        Assert.assertEquals(output, Utils.removeCurrencySignFromPrice(input));


        Assert.assertEquals("9.99", Utils.removeCurrencySignFromPrice("9.99"));
        Assert.assertEquals("9.99", Utils.removeCurrencySignFromPrice("9.99$"));
        Assert.assertEquals("10", Utils.removeCurrencySignFromPrice("10"));
        Assert.assertEquals("100", Utils.removeCurrencySignFromPrice("100"));
        Assert.assertEquals("12.34", Utils.removeCurrencySignFromPrice("$12.34"));
        Assert.assertEquals("12.34", Utils.removeCurrencySignFromPrice("US$12.34"));
        Assert.assertEquals("12.34", Utils.removeCurrencySignFromPrice("US\$US12.34"));
        Assert.assertEquals("99.99", Utils.removeCurrencySignFromPrice("$99.99"));
        Assert.assertEquals("149.99", Utils.removeCurrencySignFromPrice("$149.99"));
        Assert.assertEquals("0.0", Utils.removeCurrencySignFromPrice(null));
        Assert.assertEquals("0.0", Utils.removeCurrencySignFromPrice("price"));
        Assert.assertEquals("0.0", Utils.removeCurrencySignFromPrice("USD"));
        Assert.assertEquals("149.99", Utils.removeCurrencySignFromPrice("$149.99"));
        Assert.assertEquals("14999", Utils.removeCurrencySignFromPrice("$149,99"));
        Assert.assertEquals("1000000.889", Utils.removeCurrencySignFromPrice("$1,000,000.889"));
        Assert.assertEquals("1499999.99", Utils.removeCurrencySignFromPrice("$1499999.99"));
    }
}