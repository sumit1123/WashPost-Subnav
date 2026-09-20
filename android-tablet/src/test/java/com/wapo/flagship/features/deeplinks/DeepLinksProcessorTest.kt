package com.wapo.flagship.features.deeplinks

import com.wapo.android.commons.util.URLParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinksProcessorTest {
    @Test
    fun isTopStoriesNavMatchesHomeTabWithOrWithoutTrailingSlash() {
        assertTrue(DeepLinksProcessor.isTopStoriesNav(URLParser("washpost:///tab/home")))
        assertTrue(DeepLinksProcessor.isTopStoriesNav(URLParser("washpost:///tab/home/")))
    }

    @Test
    fun isAskNavMatchesAskTabWithOrWithoutTrailingSlash() {
        assertTrue(DeepLinksProcessor.isAskNav(URLParser("washpost:///tab/ask")))
        assertTrue(DeepLinksProcessor.isAskNav(URLParser("washpost:///tab/ask/")))
    }

    @Test
    fun isGamesNavMatchesGamesTabAndPlayTabWithOrWithoutTrailingSlash() {
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///tab/games")))
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///tab/games/")))
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///tab/play")))
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///tab/play/")))
    }

    @Test
    fun isGamesNavMatchesLegacyGamesPathsWithOrWithoutTrailingSlash() {
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///games")))
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///games/")))
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///play")))
        assertTrue(DeepLinksProcessor.isGamesNav(URLParser("washpost:///play/")))
    }

    @Test
    fun isWatchNavMatchesWatchTabWithOrWithoutTrailingSlash() {
        assertTrue(DeepLinksProcessor.isWatchNav(URLParser("washpost:///tab/watch")))
        assertTrue(DeepLinksProcessor.isWatchNav(URLParser("washpost:///tab/watch/")))
    }

    @Test
    fun isWatchNavMatchesLegacyWatchPaths() {
        assertTrue(DeepLinksProcessor.isWatchNav(URLParser("washpost:///watch")))
        assertTrue(DeepLinksProcessor.isWatchNav(URLParser("washpost:///watch/")))
        assertTrue(DeepLinksProcessor.isWatchNav(URLParser("washpost:///classic-apps/watch")))
        assertTrue(DeepLinksProcessor.isWatchNav(URLParser("washpost:///classic-apps/watch/")))
    }

    @Test
    fun isListenNavMatchesListenTab() {
        assertTrue(DeepLinksProcessor.isListenNav(URLParser("washpost:///tab/listen")))
        assertFalse(DeepLinksProcessor.isListenNav(URLParser("washpost:///tab/listen/")))
    }

    @Test
    fun isPrintEditionNavMatchesPrintTabWithOrWithoutTrailingSlash() {
        assertTrue(DeepLinksProcessor.isPrintEditionNav(URLParser("washpost:///tab/print")))
        assertTrue(DeepLinksProcessor.isPrintEditionNav(URLParser("washpost:///tab/print/")))
    }
}
