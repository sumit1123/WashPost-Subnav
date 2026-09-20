/*
 * Copyright (C) 2014 Washington Post Android Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.flagship.util.tracking;

import com.google.firebase.analytics.FirebaseAnalytics;

public enum Evars {
    PAGE_NAME("page_name"),
    SITE_SECTION("content_section"),
    PUSH_ACTION("push_interaction_type"),
    CONTENT_TYPE("content_type"),
    SOCIAL_SHARE("share_url"),
    APP_VERSION_NUMBER("app_version_number"),
    PUSH_URL("push_url"),
    EXTERNAL_LINK("external_link"),
    BLOG_NAME("blog_name"),
    CONTENT_URL("content_url"),
    USER_ATTRIBUTES("user_attributes"),
    METER_COUNT_RULE2("meter_count_rule2"),
    METER_REASON("meter_reason"),
    PAGINATION("pagination"),
    USER_AGENT("user_agent"),
    NIGHT_MODE("dark_mode_status"),
    APP_SECTION("app_section"),
    MISCELLANY("miscellany"),
    ANDROID_VERSION("android_version"),
    ORIENTATION("orientation"),
    SOCIAL_NETWORK("share_to_network"),
    NAVIGATION_BEHAVIOR("navigation_behavior"),
    PUSH_TYPE("push_type"),
    PUSH_TITLE("push_title"),
    BANNERS("banner_type") /* Using this eVar for breaking-news banners */,
    SIGNIN_MEDIUM("login_type"),
    PUSH_NOTIFICATION_ID("push_id"),
    PUSH_HEADLINE("push_headline"),
    VIDEO_GRID_TYPE("video_grid_type"),
    PREV_PAGE("previous_page"),
    CONTENT_SOURCE("content_source"),
    VIDEO_SECTION("av_section"),
    VIDEO_SOURCE("av_source"),
    ARC_ID("arc_id"),
    AV_ARC_ID("av_arc_id"),
    VIDEO_CATEGORY("av_category"),
    AV_EXP("av_exp"),
    EVENT_LABEL("event_label"),
    CONTENT_SUBSECTION("content_subsection"),
    COMICS_CONTENT_SUBSECTION("comics_content_subsection"),
    IDENTITY_UUID("identity_uuid"),
    USER_ID("user_id"),
    USER_LOGIN_STATUS("login_status"),
    ACQ_ENTRANCE_TYPE("acq_entrance_type"),
    SUB_START_LOCATION("sub_start_location"),
    ENTRANCE_TYPE("entrance_type"),
    METER_COUNT("free_content_count"),
    METER_COUNT_1("meter_count"),
    METER_COUNT_RULE1("meter_count_rule1"),
    ACTMGMT_ARRAY("actmgmt_array"),
    PAYWALL_SOURCE("sub_source"),
    SUB_PRICE_FLAG("sub_price_flag"),
    METERED("metered_unmetered_reason"),
    STORE_TYPE("store_type"),
    GALLERY_IMAGE_INDEX("gallery_image_index"),
    TAB_NAME("tab_name"),
    GOOGLE_INDEXING("google_indexing"),
    APP_LAUNCH_SOURCE("app_launch_source"),
    AV_NAME("av_name"),
    PROPERTY_NAME("property_name"),
    AB_TESTING_VARIANT("test_group"),
    ACCOUNTHOLD_EVENT_LABEL("eventLabel"),
    PRODUCTS("acq_product"),
    AUDIO_FEED("feed"),
    ENGAGED_TIME("engaged_time"),
    PROMO_CODE("promo_code"),
    CONTENT_AUTHOR("content_author"),
    SEARCHED_KEYWORD("search_keywords"),
    SEARCH_VOICE("search_voice"),
    SEARCH_RECENT_SEARCH("search_recent_search"),
    SEARCH_NOT_FOUND("search_not_found"),
    CONNECTION_TYPE("connection_type"),
    PUSH_TOPIC_PLATFORM("push_topic_platform"),
    PUSH_TIMESTAMP("push_sent_timestamp"),
    USER_SUBSCRIBER_STATUS("subscriber_status"),
    SUB_SECTION("content_subsection"),
    AUTHOR_ID("author_id"),
    NEWSROOM_DESK("newsroom_desk"),
    NEWSROOM_SUB_DESK("newsroom_subdesk"),

    USER_NAME("user_name"),
    PAGE_FORMAT("page_format"),
    PUBLISHED_DATE("publishdate"),
    CONTENT_TOPICS("content_topics"),
    TRACKING_TAGS("tracking_tags"),
    TETRO_FREE_CONTENT_COUNT("free_content_count"),
    TETRO_METERED_UNMETERED_REASON("metered_unmetered_reason"),
    TETRO_CONTENT_WEIGHT("content_weight"),
    SUBSCRIBER_ATTRIBUTES_ARRAY("subscriber_attributes_array"),
    LUF_NAVIGATION("luf_navigation"),

    EVENT_MESSAGE_USE_CASE("message_use_case"),
    PROMO_PURCHASE_INFO("promo_purchase_info"),
    BRIGHTS_POSITION("brights_position"),
    ARTICLE_POSITION("article_position"),
    INLINE_PUSH_TOGGLE_FLAG("inline_push_toggle_flag"),
    RAINBOW_MIGRATION_ARRAY("rainbow_migration_array"),
    TOTAL_FEATURES("total_features"),
    SUPPORT_ID("support_id"),
    HARDWARE_ID("hardware_id"),
    PROGRESS_THRESHOLD("progress_threshold"),
    AV_TYPE("av_type"),
    AV_TAGS("av_tags"),
    AV_PLAYER_TYPE("av_player_type"),
    AV_DURATION("av_length"),
    VIDEO_START_ID("videostart_id"),
    COMMERCIAL_NODE("commercial_node"),
    CONTENT_CATEGORY("content_category"),
    HEADLINE("headline"),
    HIERARCHY("hierarchy"),
    PRIMARY_SECTION("primary_section"),
    TITLE("title"),
    CONTENT_SECTION("content_section"),
    TRAFFICSOURCE_SOURCE(FirebaseAnalytics.Param.SOURCE),
    TRAFFICSOURCE_MEDIUM(FirebaseAnalytics.Param.MEDIUM),
    TRAFFICSOURCE_CAMPAIGN(FirebaseAnalytics.Param.CAMPAIGN),


//    Ads analytics
    J_UCID("j_ucid"),
    J_TID("j_tid"),
    GAM_LINE_ID("gam_line_id"),
    GAM_CREATIVE_ID("gam_creative_id"),
    AV_META_DATA("av_meta_data"),
    DETAILS("details"),
    GEN_EVENT_DIMENSION("gen_event_dimension"),
    COOKIES_JS("cookies_js"),
    TETRO_ACTION("tetro_action"),
    TETRO_ACTION_CODE("action_code");
    private final String value;

    Evars(final String value) {
        this.value = value;
    }

    public String getVariable() {
        return value;
    }
}
