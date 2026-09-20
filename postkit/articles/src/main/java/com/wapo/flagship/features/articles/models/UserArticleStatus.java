package com.wapo.flagship.features.articles.models;

import java.util.Date;

@Deprecated
public class UserArticleStatus {
    private String id;
    private String articleUrl;
    private String headline;
    private String summary;
    private String byLine;
    private String timeStamp;
    private String imageUrl;
    private Type userStatusType;
    private Date activityDate;
    private int type;

    public UserArticleStatus() {
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getByLine() {
        return byLine;
    }

    public void setByLine(String byLine) {
        this.byLine = byLine;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Type getUserStatusType() {
        return userStatusType;
    }

    public void setUserStatusType(Type userStatusType) {
        this.userStatusType = userStatusType;
    }

    public Date getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(Date activityDate) {
        this.activityDate = activityDate;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static enum Type {
        FAVORITE(1),
        READING_HISTORY(2);

        private long _id;

        Type(long id) {
            _id = id;
        }

        public long getId() {
            return _id;
        }

        public static Type fromId(long id) {
            for (Type userStatusType : Type.values()) {
                if (userStatusType.getId() == id) {
                    return userStatusType;
                }
            }
            throw new IllegalArgumentException("Invalid status type.");
        }
    }
}