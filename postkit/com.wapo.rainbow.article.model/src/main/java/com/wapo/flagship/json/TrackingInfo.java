package com.wapo.flagship.json;

import java.io.Serializable;
import java.util.Date;

public class TrackingInfo implements Serializable {
    private String pageName = null;
    private String pagePath = null;
    private String pageNumber = null;
    private String channel = null;
    private String contentSubsection = null;
    private String contentType = null;
    private String contentAuthor = null;

    private String authorType = null;
    private String searchKeywords = null;
    private String pageFormat = null;
    private String blogName = null;
    private String contentSource = null;
    private String contentURL = null;
    private String interfaceType = null;
    private String contentId = null;
    private String source = null;
    private String primarySection = null;
    private String secondarySection = null;
    private String subSection = null;
    private String arcId = null;
    private String title =null;
    private String authorId = null;
    private String newsroomDesk = null;
    private String newsroomSubdesk = null;
    private Date firstPublished;

    private Long firstPublishedTime;

    private Long lastModifiedTime;
    private String contentTopics;
    private String trackingTags = null;
    private String commercialNode = null;
    private String contentCategory = null;
    private String headline = null;
    private String hierarchy = null;
    private String audioFirstPublishDate = null;

    // value from "permutive_dict" key
    private Object targetingDict;

    // pv events attributes to attribute ad request to a specific page view per device.
    private String jUcid = null;
    private Long jTid = 0L;

    private TrackingInfoPageType pageType = null;
    private Boolean playAd = false;
    private String tetroAction = null;
    private String actionCode = null;

    public String getTetroAction() {
        return tetroAction;
    }

    public void setTetroAction(String tetroAction) {
        this.tetroAction = tetroAction;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public Boolean getPlayAd() {
        return playAd;
    }

    public void setPlayAd(Boolean playAd) {
        this.playAd = playAd;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getContentSubsection() {
        return contentSubsection;
    }

    public void setContentSubsection(String contentSubsection) {
        this.contentSubsection = contentSubsection;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentAuthor() {
        return contentAuthor;
    }

    public void setContentAuthor(String contentAuthor) {
        this.contentAuthor = contentAuthor;
    }

    public String getAuthorType() {
        return authorType;
    }

    public void setAuthorType(String authorType) {
        this.authorType = authorType;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String getPageFormat() {
        return pageFormat;
    }

    public void setPageFormat(String pageFormat) {
        this.pageFormat = pageFormat;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(String contentSource) {
        this.contentSource = contentSource;
    }

    public String getContentURL() {
        return contentURL;
    }

    public void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPrimarySection() {
        return primarySection;
    }

    public void setPrimarySection(String primarySection) {
        this.primarySection = primarySection;
    }

    public String getSecondarySection() {
        return secondarySection;
    }

    public void setSecondarySection(String secondarySection) {
        this.secondarySection = secondarySection;
    }

    public String getSubSection() {
        return subSection;
    }

    public void setSubSection(String subSection) {
        this.subSection = subSection;
    }

    public String getArcId() {
        return arcId;
    }

    public String setArcId(String arcId) {
        return this.arcId = arcId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getNewsroomDesk() {
        return newsroomDesk;
    }

    public void setNewsroomDesk(String newsroomDesk) {
        this.newsroomDesk = newsroomDesk;
    }

    public String getNewsroomSubdesk() {
        return newsroomSubdesk;
    }

    public void setNewsroomSubdesk(String newsroomSubdesk) {
        this.newsroomSubdesk = newsroomSubdesk;
    }

    public void setFirstPublishedDate(Date firstPublished) {
        this.firstPublished = firstPublished;
    }

    public Date getFirstPublishedDate() {
        return firstPublished;
    }

    public void setFirstPublishedTime(Long firstPublishedTime) {
        this.firstPublishedTime = firstPublishedTime;
    }

    public Long getFirstPublishedTime() {
        return firstPublishedTime;
    }

    public void setLastModifiedTime(Long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setContentTopics(String contentTopics) {
        this.contentTopics = contentTopics;
    }

    public String getContentTopics() {
        return contentTopics;
    }

    public String getTrackingTags() {
        return trackingTags;
    }

    public void setTrackingTags(String tags){
        this.trackingTags = tags;
    }

    public String getjUcid() {
        return jUcid;
    }

    public void setjUcid(String jUcid) {
        this.jUcid = jUcid;
    }

    public Long getjTid() {
        return jTid;
    }

    public void setjTid(Long jTid) {
        this.jTid = jTid;
    }

    public String getCommercialNode() {
        return commercialNode;
    }

    public void setCommercialNode(String commercialNode) {
        this.commercialNode = commercialNode;
    }

    public String getContentCategory() {
        return contentCategory;
    }

    public void setContentCategory(String contentCategory) {
        this.contentCategory = contentCategory;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public String getAudioFirstPublishDate() {
        return audioFirstPublishDate;
    }

    public void setAudioFirstPublishDate(String audioFirstPublishDate) {
        this.audioFirstPublishDate = audioFirstPublishDate;
    }

    public void setTargetingDict(Object value) {
        targetingDict = value;
    }

    public Object getTargetingDict() {
        return targetingDict;
    }

    public void setPageType(TrackingInfoPageType pageType) {
        this.pageType = pageType;
    }

    public TrackingInfoPageType getPageType() {
        return pageType;
    }
}
