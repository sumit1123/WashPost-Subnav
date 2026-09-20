package com.wapo.android.commons.util

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class URLParserTest {

    @Test
    fun testStandardValidUrl() {
        val url =
            "https://www.washingtonpost.com/world/2022/12/16/russia-ukraine-war-latest-updates?tid=tid&wv=wv#link-SZOQPOXSAVEEPHSHXJQ5UKWFWU"
        val urlParser = URLParser(url)
        println(urlParser)
        Assert.assertTrue(urlParser.isValid())
        Assert.assertTrue(urlParser.getScheme() == "https")
        Assert.assertTrue(urlParser.getAuthority() == "www.washingtonpost.com")
        Assert.assertTrue(urlParser.getDomain() == "www.washingtonpost.com")
        Assert.assertTrue(urlParser.getPort() == -1)
        Assert.assertTrue(urlParser.getPath() == "/world/2022/12/16/russia-ukraine-war-latest-updates")
        Assert.assertTrue(urlParser.getParameters() == "tid=tid&wv=wv")
        with(urlParser.getParametersBundle()) {
            requireNotNull(this)
            Assert.assertTrue(getString("tid") == "tid")
            Assert.assertTrue(getString("wv") == "wv")
        }
        Assert.assertTrue(urlParser.getPathAndParameters() == "/world/2022/12/16/russia-ukraine-war-latest-updates?tid=tid&wv=wv")
        Assert.assertTrue(urlParser.getAnchor() == "link-SZOQPOXSAVEEPHSHXJQ5UKWFWU")
        Assert.assertTrue(!urlParser.isWashPostScheme())
    }

    @Test
    fun testWashPostValidUrl() {
        val url = "washpost://www.washingtonpost.com/world/2022/12/16/russia-ukraine-war-latest-updates?tid=tid&wv=wv#link-SZOQPOXSAVEEPHSHXJQ5UKWFWU"
        val urlParser = URLParser(url)
        println(urlParser)
        Assert.assertTrue(urlParser.isValid())
        Assert.assertTrue(urlParser.getScheme() == "https")
        Assert.assertTrue(urlParser.getAuthority() == "www.washingtonpost.com")
        Assert.assertTrue(urlParser.getDomain() == "www.washingtonpost.com")
        Assert.assertTrue(urlParser.getPort() == -1)
        Assert.assertTrue(urlParser.getPath() == "/world/2022/12/16/russia-ukraine-war-latest-updates")
        Assert.assertTrue(urlParser.getParameters() == "tid=tid&wv=wv")
        with(urlParser.getParametersBundle()) {
            requireNotNull(this)
            Assert.assertTrue(getString("tid") == "tid")
            Assert.assertTrue(getString("wv") == "wv")
        }
        Assert.assertTrue(urlParser.getPathAndParameters() == "/world/2022/12/16/russia-ukraine-war-latest-updates?tid=tid&wv=wv")
        Assert.assertTrue(urlParser.getAnchor() == "link-SZOQPOXSAVEEPHSHXJQ5UKWFWU")
        Assert.assertTrue(urlParser.isWashPostScheme())
    }

    @Test
    fun testWashPostCustomValidUrl() {
        val url = "washpost:///settings/contactus"
        val urlParser = URLParser(url)
        println(urlParser)
        Assert.assertTrue(urlParser.isValid())
        Assert.assertTrue(urlParser.getScheme() == "https")
        Assert.assertTrue(urlParser.getAuthority() == "")
        Assert.assertTrue(urlParser.getDomain() == "")
        Assert.assertTrue(urlParser.getPort() == -1)
        Assert.assertTrue(urlParser.getPath() == "/settings/contactus")
        Assert.assertTrue(urlParser.getParameters().isEmpty())
        Assert.assertNull(urlParser.getParametersBundle())
        Assert.assertTrue(urlParser.getPathAndParameters() == "/settings/contactus")
        Assert.assertTrue(urlParser.getAnchor().isEmpty())
        Assert.assertTrue(urlParser.isWashPostScheme())
    }
}