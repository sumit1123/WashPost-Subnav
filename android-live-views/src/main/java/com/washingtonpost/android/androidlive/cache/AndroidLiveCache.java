package com.washingtonpost.android.androidlive.cache;

import android.text.TextUtils;

import com.washingtonpost.android.androidlive.liveblog.model.LiveBlogFeed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by elamgodilj on 8/31/16.
 */

public class AndroidLiveCache {

    private static Map<String, List<LiveBlogFeed.LiveBlogFeedItem>> liveBlogFeedItemsMap;

    public static void init() {
        if (liveBlogFeedItemsMap == null) {
            liveBlogFeedItemsMap = new HashMap<>();
        }
    }

    public static void clearCache() {
        if (liveBlogFeedItemsMap != null) {
            liveBlogFeedItemsMap.clear();
        }
    }

    public static boolean IS_NIGHT_MODE;

    public static void setLiveBlogFeed(String primetimeURL, List<LiveBlogFeed.LiveBlogFeedItem> feed) {
        if (TextUtils.isEmpty(primetimeURL) || liveBlogFeedItemsMap == null || feed == null || feed.isEmpty()) {
            return;
        }
        liveBlogFeedItemsMap.remove(primetimeURL);
        liveBlogFeedItemsMap.put(primetimeURL, feed);
    }

    public static boolean isLiveBlogFeedAvailable(String primetimeURL) {
        return (liveBlogFeedItemsMap != null && !TextUtils.isEmpty(primetimeURL)) && liveBlogFeedItemsMap.containsKey(primetimeURL);
    }

    public static List<LiveBlogFeed.LiveBlogFeedItem> getLiveBlogCache(String primetimeURL) {
        return liveBlogFeedItemsMap.get(primetimeURL);
    }
}
