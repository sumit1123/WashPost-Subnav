package com.wapo.flagship.util.tracking.providers.permutive

/**
 * Permutive Property names to configure page views data
 * Ref: https://dash.permutive.com/events
 */
enum class PermutiveEventProperties(
    val propertyName: String,
) {
    PLATFORM("platform"),

    SECTION("section"),
    SUBSECTION("subsection"),

    ARTICLE("article"),
    ARTICLE_AUTHOR_NAME("authorName"),
    ARTICLE_AUTHOR_TYPE("authorType"),
    ARTICLE_CONTENT_TYPE("contentType"),
    ARTICLE_LANGUAGE("language"),
    ARTICLE_PUBLISHED_DATE("publishDate"),
    ARTICLE_NEWS_ROOM_DESK("newsroomDesk"),
    ARTICLE_NEWS_ROOM_SUB_DESK("newsroomSubdesk"),
    ARTICLE_WORD_COUNT("wordCount"),
    ARTICLE_TARGETING_CUSTOM_TOPICS("customTopics"),
    ARTICLE_TARGETING_WAPO_TOPICS("wapoTopics"),

    COMMERCIAL_NODE("commercialNode"),
    CANONICAL_URL("canonicalUrl"),

    SDK_ISP_INFO("isp_info"),
    SDK_GEO_INFO("geo_info"),
}
