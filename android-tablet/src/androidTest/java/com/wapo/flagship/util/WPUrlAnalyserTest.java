package com.wapo.flagship.util;

import com.wapo.flagship.FlagshipApplication;

import org.junit.Test;

import static org.junit.Assert.*;

public class WPUrlAnalyserTest {

    @Test
    public void test_canTryToOpenArticleNatively() {

        // Article with comment query param, expected false
        assertEquals(false,
                WPUrlAnalyser
                        .getWPUrlAnalyser()
                        .canTryToOpenArticleNatively(
                                "https://www.washingtonpost.com/opinions/2020/02/02/i-wish-youd-never-happened-donald-trump-im-grateful-you-too/?commentId=bd88a01a-3fac-44e8-b0cf-ff7c086649de&outputType=comment&wpisrc=in_comments&wpmm=1",
                                FlagshipApplication.getInstance()));

        // Article with comment query param, expected false
        assertEquals(false,
                WPUrlAnalyser
                        .getWPUrlAnalyser()
                        .canTryToOpenArticleNatively(
                                "https://www.wapo.st/opinions/2020/02/02/i-wish-youd-never-happened-donald-trump-im-grateful-you-too/?commentId=bd88a01a-3fac-44e8-b0cf-ff7c086649de&outputType=comment&wpisrc=in_comments&wpmm=1",
                                FlagshipApplication.getInstance()));


        // Article, expected true
        assertEquals(true,
                WPUrlAnalyser
                        .getWPUrlAnalyser()
                        .canTryToOpenArticleNatively(
                                "https://www.washingtonpost.com/opinions/2020/02/02/i-wish-youd-never-happened-donald-trump-im-grateful-you-too/?wpmm=1",
                                FlagshipApplication.getInstance()));


        // non article url with comment query param, expected false
        assertEquals(false,
                WPUrlAnalyser
                        .getWPUrlAnalyser()
                        .canTryToOpenArticleNatively(
                                "https://www.washingtonpost.com/?commentId=bd88a01a-3fac-44e8-b0cf-ff7c086649de&outputType=comment&wpisrc=in_comments&wpmm=1",
                                FlagshipApplication.getInstance()));


        // non washpost domain url, expected false
        assertEquals(false,
                WPUrlAnalyser
                        .getWPUrlAnalyser()
                        .canTryToOpenArticleNatively(
                                "https://www.bing.com/",
                                FlagshipApplication.getInstance()));

        // non washpost domain url with comment query param, expected false
        assertEquals(false,
                WPUrlAnalyser
                        .getWPUrlAnalyser()
                        .canTryToOpenArticleNatively(
                                "https://www.bing.com/?commentId=bd88a01a-3fac-44e8-b0cf-ff7c086649de&outputType=comment&wpisrc=in_comments&wpmm=1",
                                FlagshipApplication.getInstance()));
    }

}