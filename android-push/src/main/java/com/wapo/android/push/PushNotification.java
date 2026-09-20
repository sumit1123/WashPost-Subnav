/*
 *
 *  *  Copyright (c) 2018. The Washington Post. All rights reserved.
 *
 */
package com.wapo.android.push;

import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PushNotification {

    public static final String PARAM_TITLE = "title";
    public static final String PARAM_MESSAGE = "message";
    public static final String PARAM_HEADLINE = "headline";
    public static final String PARAM_BLURB = "blurb";
    public static final String PARAM_URL = "url";
    public static final String PARAM_IMAGE_URL = "imageURL";
    public static final String PARAM_CATEGORY = "category";
    public static final String PARAM_TARGET_TOPIC = "targetTopic";
    public static final String PARAM_SENT_TIMESTAMP = "datetime";
    public static final String PARAM_PUSH_ID = "pushID";
    public static final String PARAM_ANALYTICS_ID = "analyticsTopic";
    public static final String PARAM_CONTENT_AVAILABLE = "content-available";
    public static final String PARAM_FEED = "feed";
    public static final String PARAM_KICKER = "kicker";
    public static final String PARAM_CAROUSEL_ACTION = "shouldUpdateCarousel";
    public static final String PARAM_EXPIRES = "expirationTime";
    public static final String PARAM_PUBLISHED_TIMESTAMP = "actionTime";
    public static final String PARAM_SEQUENCE = "sequence";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_ACTION = "action";
    public static final String PARAM_SPLIT_TESTING = "splitTestingPushDetails";
    public static final String PARAM_LINK_TYPE = "linkType";
    public static final String PARAM_INTERACTION_TYPE = "interactionType";
    public static final String PARAM_SEGMENTED_IMAGES = "segments";
    public static final String PARAM_DATA = "data";

    public static final String PARAM_TEST_GROUPS = "testGroups";

    private String title;
    private String url;
    private String imageUrl;
    private String headline;
    private String blurb;
    private String type;
    private String feed;
    private String contentAvailableParam;
    private String message;
    private String kicker;
    private Long timestamp;
    private String pushID;
    private String analyticsID;
    private String category;
    private String linkType;
    private Map<String, String> testGroups = new HashMap<>();
    private String interactionType;
    private List<SplitPushTestingDetails> splitPushTestingDetails;
    private List<SegmentedImage> segmentedImages;

    private boolean shouldUpdateCarousel;
    private long publishedTimestamp;
    private long expires;
    private long sequence;
    private String messageType;
    private String action;
    private JSONObject json;

    public enum LinkType {
        WEB, NATIVE, GALLERY
    }

    public enum InteractionType {
        DEFAULT, BREAKING_NEWS, SEGMENTED
    }

    public PushNotification() {

    }

    public PushNotification(JSONObject json) throws JSONException {
        this.title = json.has(PARAM_TITLE) ? json.getString(PARAM_TITLE) : "";
        this.message = json.has(PARAM_MESSAGE) ? json.getString(PARAM_MESSAGE) : "";
        this.headline = json.has(PARAM_HEADLINE) ? json.getString(PARAM_HEADLINE) : message;
        this.blurb = json.has(PARAM_BLURB) ? json.getString(PARAM_BLURB) : null;
        this.url = json.has(PARAM_URL) ? json.getString(PARAM_URL) : null;
        this.imageUrl = json.has(PARAM_IMAGE_URL) ? json.getString(PARAM_IMAGE_URL) : null;
        this.category = json.has(PARAM_CATEGORY) ? json.getString(PARAM_CATEGORY) : null;
        this.type = json.has(PARAM_TARGET_TOPIC) ? json.getString(PARAM_TARGET_TOPIC) : "news-alert";
        this.timestamp = json.has(PARAM_SENT_TIMESTAMP) ? json.getLong(PARAM_SENT_TIMESTAMP) : null;
        this.pushID = json.has(PARAM_PUSH_ID) ? json.getString(PARAM_PUSH_ID) : "";
        this.analyticsID = json.has(PARAM_ANALYTICS_ID) ? json.getString(PARAM_ANALYTICS_ID) : "";
        this.contentAvailableParam = json.has(PARAM_CONTENT_AVAILABLE) ? json.getString(PARAM_CONTENT_AVAILABLE) : null;
        this.feed = json.has(PARAM_FEED) ? json.getString(PARAM_FEED) : null;
        this.kicker = json.has(PARAM_KICKER) ? json.getString(PARAM_KICKER) : null;
        this.shouldUpdateCarousel = json.has(PARAM_CAROUSEL_ACTION) ? json.getBoolean(PARAM_CAROUSEL_ACTION) : false;
        this.expires = json.has(PARAM_EXPIRES) ? json.getLong(PARAM_EXPIRES) : System.currentTimeMillis();
        this.publishedTimestamp = json.has(PARAM_PUBLISHED_TIMESTAMP) ? json.getLong(PARAM_PUBLISHED_TIMESTAMP) : 0;
        this.sequence = json.has(PARAM_SEQUENCE) ? json.getLong(PARAM_SEQUENCE) : 0;
        this.messageType = json.has(PARAM_TYPE) ? json.getString(PARAM_TYPE) : "";
        this.action = json.has(PARAM_ACTION) ? json.getString(PARAM_ACTION) : "";
        this.interactionType = json.has(PARAM_INTERACTION_TYPE) ? json.getString(PARAM_INTERACTION_TYPE) : "";
        this.linkType = json.has(PARAM_LINK_TYPE) ? json.getString(PARAM_LINK_TYPE) : "";
        if (json.has(PARAM_TEST_GROUPS)) {
            JSONObject testGroupsObject = json.getJSONObject(PARAM_TEST_GROUPS);
            if (testGroupsObject != null) {
                Iterator<String> keys = testGroupsObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = testGroupsObject.getString(key);
                    this.testGroups.put(key, value);
                }
            }
        }

        if (json.has(PARAM_SPLIT_TESTING) && json.getJSONArray(PARAM_SPLIT_TESTING) != null) {
            JSONArray splitPushArray = json.getJSONArray(PARAM_SPLIT_TESTING);
            for (int i = 0, len = splitPushArray.length(); i < len; i++) {
                this.splitPushTestingDetails.add(new SplitPushTestingDetails(splitPushArray.getJSONObject(i)));
            }
        }

        if (json.has(PARAM_SEGMENTED_IMAGES) && json.getJSONArray(PARAM_SEGMENTED_IMAGES) != null) {
            JSONArray segmentedImageArray = json.getJSONArray(PARAM_SEGMENTED_IMAGES);
            segmentedImages = new ArrayList<>();
            for (int i = 0, len = segmentedImageArray.length(); i < len; i++) {
                segmentedImages.add(new SegmentedImage(segmentedImageArray.getJSONObject(i)));
            }
        }


        // mmp silent push
        if (json.has(PARAM_DATA)) {
            String dataString = json.getString(PARAM_DATA);
            if (!TextUtils.isEmpty(dataString)) {
                JSONObject data = new JSONObject(dataString);
                this.messageType = data.has(PARAM_TYPE) ? data.getString(PARAM_TYPE) : "";
                this.shouldUpdateCarousel = data.has(PARAM_CAROUSEL_ACTION) ? data.getBoolean(PARAM_CAROUSEL_ACTION) : false;
            }
        }

        this.json = json;
    }

    public static PushNotification generateFallbackMessage(Bundle bundle) {
        PushNotification pushMessage = null;

        if (bundle != null && bundle.containsKey("failover")) {
            pushMessage = new PushNotification();
            pushMessage.title = bundle.containsKey(PARAM_TITLE) ? bundle.getString(PARAM_TITLE) : "Wash Post";
            pushMessage.message = bundle.containsKey(PARAM_MESSAGE) ? bundle.getString(PARAM_MESSAGE) : "Breaking news";
            pushMessage.headline = bundle.containsKey(PARAM_HEADLINE) ? bundle.getString(PARAM_HEADLINE) : pushMessage.message;
            pushMessage.url = bundle.containsKey(PARAM_URL) ? bundle.getString(PARAM_URL) : null;
            pushMessage.category = bundle.containsKey(PARAM_CATEGORY) ? bundle.getString(PARAM_CATEGORY) : null;
            pushMessage.type = bundle.containsKey(PARAM_TARGET_TOPIC) ? bundle.getString(PARAM_TARGET_TOPIC) : "news-alert";
            pushMessage.timestamp = bundle.containsKey(PARAM_SENT_TIMESTAMP) ? bundle.getLong(PARAM_SENT_TIMESTAMP) : null;
            pushMessage.pushID = bundle.containsKey(PARAM_PUSH_ID) ? bundle.getString(PARAM_PUSH_ID) : "";
            pushMessage.analyticsID = bundle.containsKey(PARAM_ANALYTICS_ID) ? bundle.getString(PARAM_ANALYTICS_ID) : "";
            pushMessage.contentAvailableParam = bundle.containsKey(PARAM_CONTENT_AVAILABLE) ? bundle.getString(PARAM_CONTENT_AVAILABLE) : null;
            pushMessage.feed = bundle.containsKey(PARAM_FEED) ? bundle.getString(PARAM_FEED) : null;
            pushMessage.kicker = bundle.containsKey(PARAM_KICKER) ? bundle.getString(PARAM_KICKER) : null;
            pushMessage.shouldUpdateCarousel = bundle.containsKey(PARAM_CAROUSEL_ACTION) ? bundle.getBoolean(PARAM_CAROUSEL_ACTION) : false;
            pushMessage.expires = bundle.containsKey(PARAM_EXPIRES) ? bundle.getLong(PARAM_EXPIRES) : System.currentTimeMillis();
            pushMessage.publishedTimestamp = bundle.containsKey(PARAM_PUBLISHED_TIMESTAMP) ? bundle.getLong(PARAM_PUBLISHED_TIMESTAMP) : 0;
            pushMessage.sequence = bundle.containsKey(PARAM_SEQUENCE) ? bundle.getLong(PARAM_SEQUENCE) : 0;
            pushMessage.messageType = bundle.containsKey(PARAM_TYPE) ? bundle.getString(PARAM_TYPE) : "";
            pushMessage.action = bundle.containsKey(PARAM_ACTION) ? bundle.getString(PARAM_ACTION) : "";
        }

        return pushMessage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String url) {
        this.imageUrl = url;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<SplitPushTestingDetails> getSplitPushTestingDetails() {
        return splitPushTestingDetails;
    }

    public  Map<String, String> getTestGroups() {
        return testGroups;
    }

    public void setTestGroups(Map<String, String> testGroups) {
        this.testGroups = testGroups;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public List<SegmentedImage> getSegmentedImages() {
        return segmentedImages;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getPushID() {
        return pushID;
    }

    public String getFeed() {
        return feed;
    }

    public String getKicker() {
        return kicker;
    }

    public String getContentAvailableParam() {
        return contentAvailableParam;
    }

    public String getAnalyticsID() {
        return analyticsID;
    }

    public String getMessage() {
        return message;
    }

    public boolean isShouldUpdateCarousel() {
        return shouldUpdateCarousel;
    }

    public long getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public long getExpires() {
        return expires;
    }

    public long getSequence() {
        return sequence;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getAction() {
        return action;
    }

    public static class SplitPushTestingDetails {
        int displayChance;
        String title;
        String headline;
        String category;
        String url;

        public SplitPushTestingDetails(JSONObject json) throws JSONException {
            displayChance = json.has("displayChance") ? json.getInt("displayChance") : 0;
            title = json.has("title") ? json.getString("title") : "Wash Post";
            headline = json.has("headline") ? json.getString("headline") : "Breaking news";
            url = json.has("url") ? json.getString("url") : null;
            category = json.has("category") ? json.getString("category") : null;
        }
    }

    public static class SegmentedImage {
        public static final String PARAM_NAME = "name";
        public static final String PARAM_IMAGE_URL = "imageURL";
        public String name;
        public String imageURL;

        public SegmentedImage(JSONObject json) throws JSONException {
            name = json.has(PARAM_NAME) ? json.getString(PARAM_NAME) : null;
            imageURL = json.has(PARAM_IMAGE_URL) ? json.getString(PARAM_IMAGE_URL) : null;
        }
    }

    @Override
    public String toString() {
        if (json != null) {
            return json.toString();
        }
        return null;
    }
}