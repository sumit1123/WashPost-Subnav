package com.washingtonpost.android.androidlive.liveblog.model;

/**
 * Created by elamgodilj on 8/27/16.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.wapo.android.commons.util.Logger;
import com.washingtonpost.android.androidlive.util.DateUtil;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by elamgodilj on 4/15/16.
 *
 * This class represents response of the Live Blog Grid Service
 */
public class LiveBlogFeed {
    @SerializedName("content")
    private LiveBlogPrimeTimeContent content;

    public LiveBlogPrimeTimeContent getContent() {
        return content;
    }

    public List<LiveBlogFeedItem> getFeed() {
        return (content == null || content.getChildren() == null) ? null : content.getChildren();
    }

    public static LiveBlogFeed parseJson(String jsonStr) {
        Type type = new TypeToken<LiveBlogFeed>() {}.getType();
        Gson gson = new GsonBuilder().create();
        try {
            return gson.fromJson(jsonStr, type);
        } catch(JsonSyntaxException ex) {
            Logger.e("LiveBlogFeed", "Error while parsing Live blog json string ", ex);
        }
        return null;
    }

    public static class LiveBlogPrimeTimeContent {
        @SerializedName("children")
        private List<LiveBlogFeedItem> children;

        public List<LiveBlogFeedItem> getChildren() {
            return children;
        }
    }

    public static class LiveBlogFeedItem {
        @SerializedName("transformed_content")
        private final LiveBlogFeedItemTransformedContent transformedContent=null;
        @SerializedName("cms_title")
        private final String title=null;
        @SerializedName("cms_date")
        private final String date=null;
        @SerializedName("cms_modified")
        private final String modifiedDate=null;
        @SerializedName("canonical_url")
        private final String itemUrl=null;


        /**
         * This method is sending a slug value is used to generate a URL for the live blog list item.
         * The trailing slash is to be removed in the future
         * @return
         */
        public String getLink() {
            return itemUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getDate() {
            return DateUtil.getDateStringInHoursMinutesFormat(DateUtil.getDateISOFormat(date));
        }

        public String getModifiedDate() {
            return modifiedDate;
        }
    }

    public static class LiveBlogFeedItemTransformedContent {
        @SerializedName("slug")
        public final String slug;

        public LiveBlogFeedItemTransformedContent(String slug) {
            this.slug = slug;
        }
    }

}
