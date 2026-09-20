package com.wapo.flagship.json;

import com.wapo.android.commons.util.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class LiveBlogFeed implements Serializable {

    private static final String TAG = LiveBlogFeed.class.getName();

    List<LiveBlogItem> feed;

    public List<LiveBlogItem> getFeed() {
        return feed;
    }

    public void setFeed(List<LiveBlogItem> feed) {
        this.feed = feed;
    }

    public static LiveBlogFeed parseJson(String jsonStr) {
        Type listType = new TypeToken<List<LiveBlogFeed.LiveBlogItem>>() {}.getType();
        LiveBlogFeed liveBlogFeed = new LiveBlogFeed();
        Gson gson = new Gson();
        try {
            List<LiveBlogFeed.LiveBlogItem> items = gson.fromJson(jsonStr, listType);
            liveBlogFeed.setFeed(items);
        } catch(JsonSyntaxException ex) {
            Logger.e(TAG, "Error while parsing Live blog json string", ex);
        }
        return liveBlogFeed;
    }


    public static class LiveBlogItem {
        private final String title;
        private final String description;
        private final String id;
        private final long addedTimestamp;

        private final String mobileHeadline;
        private final String webHeadline;
        private final long created;

        public LiveBlogItem(String title, String description, String id, long addedTimestamp, String mobileHeadline, String webHeadline, long created) {
            this.title = title;
            this.description = description;
            this.id = id;
            this.addedTimestamp = addedTimestamp;
            this.mobileHeadline = mobileHeadline;
            this.webHeadline = webHeadline;
            this.created = created;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public long getAddedTimestamp() {
            return addedTimestamp;
        }

        public String getMobileHeadline() {
            return mobileHeadline;
        }

        public String getWebHeadline() {
            return webHeadline;
        }

        public long getCreated() {
            return created;
        }
    }
}
