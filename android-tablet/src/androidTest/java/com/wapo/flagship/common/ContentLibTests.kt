package com.wapo.flagship.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentLibTests {
    @Test
    fun basicTest() {
        val uri = Uri.parse("https://www.mysite.com?q=123&vital=muststay")
        val processedUri = removeQueryParams(uri, arrayOf("q"))
        Assert.assertEquals(processedUri.toString(), "https://www.mysite.com?vital=muststay")
    }

    @Test
    fun badParams() {
        val uri = Uri.parse("https://www.mysite.com?q=123&vital=muststay")
        val processedUri = removeQueryParams(uri, null)
        Assert.assertEquals(processedUri.toString(), "https://www.mysite.com?q=123&vital=muststay")
    }

    @Test
    fun badUrl() {
        val processedUri = removeQueryParams(null, arrayOf("q", "z"))
        Assert.assertNull(processedUri)
    }

    @Test
    fun removeAllAparams() {
        val uri = Uri.parse("https://www.mysite.com?q=123&x=whatever")
        val processedUri = removeQueryParams(uri, arrayOf("q", "x"))
        Assert.assertEquals(processedUri.toString(), "https://www.mysite.com")
    }
}
