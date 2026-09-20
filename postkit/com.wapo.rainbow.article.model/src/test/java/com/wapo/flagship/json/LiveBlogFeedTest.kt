package com.wapo.flagship.json

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

class LiveBlogFeedTest {

    @Test
    @Throws(Exception::class)
    fun empty() {
        Mockito.mockStatic(Log::class.java).use {
            val item = LiveBlogFeed.parseJson("{}")
            assertNull(item.getFeed())
        }
    }

    @Test
    @Throws(Exception::class)
    fun basic() {
        val item = LiveBlogFeed.parseJson(resourceToString("liveblog_feed.json"))
        assertEquals(item.getFeed().size, 1)
        assertEquals(item.getFeed()[0].id, "test-id")
        assertEquals(item.getFeed()[0].addedTimestamp, 1000)
        assertEquals(item.getFeed()[0].created, 2000)
        assertEquals(item.getFeed()[0].description, "test-description")
        assertEquals(item.getFeed()[0].mobileHeadline, "test-mobileHeadline")
        assertEquals(item.getFeed()[0].title, "test-title")
        assertEquals(item.getFeed()[0].webHeadline, "test-webHeadline")
    }
}