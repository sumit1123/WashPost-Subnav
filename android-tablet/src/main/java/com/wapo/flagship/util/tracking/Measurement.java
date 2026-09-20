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

import static com.wapo.flagship.features.notification.AlertsSettings.AlertTopicInfo;
import static com.wapo.flagship.features.notification.AlertsSettings.EntryPoint;
import static com.washingtonpost.android.paywall.util.PaywallConstants.EMPTY;
import static java.lang.Integer.parseInt;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider;
import com.wapo.android.commons.engagement.EngagementTrace;
import com.wapo.android.commons.engagement.PageEngagementTrace;
import com.wapo.android.commons.engagement.SessionEngagementTrace;
import com.wapo.android.commons.util.AppContextUtils;
import com.wapo.android.commons.util.DeviceUtils;
import com.wapo.android.commons.util.Logger;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.external.storage.WidgetType;
import com.wapo.flagship.features.articles2.models.Article2;
import com.wapo.flagship.features.articles2.models.OmnitureX;
import com.wapo.flagship.features.articles2.tracking.ArticlePageEngagementTrace;
import com.wapo.flagship.features.articles2.tracking.PushArticleTrackingHelperData;
import com.wapo.flagship.features.deeplinks.InAppMessageData;
import com.wapo.flagship.features.grid.Tracking;
import com.wapo.flagship.features.onetrust.OneTrustHelper;
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo;
import com.wapo.flagship.features.posttv.VideoTracker2;
import com.wapo.flagship.features.search2.model.PostAnswerCarouselTrackingHelper;
import com.wapo.flagship.features.search2.model.QueryFilter;
import com.wapo.flagship.features.sections.utils.JTidTracker;
import com.wapo.flagship.features.settings.AppPreferences;
import com.wapo.flagship.json.TrackingInfo;
import com.wapo.flagship.json.TrackingInfoPageType;
import com.wapo.flagship.navigation.ui.BottomTab;
import com.wapo.flagship.util.JUcidTracker;
import com.wapo.flagship.util.PrefUtils;
import com.wapo.flagship.util.PushIdTracker;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.UtilsKt;
import com.wapo.flagship.util.tracking.providers.permutive.PermutiveProvider;
import com.wapo.flagship.util.tracking.providers.permutive.PermutiveTranslator;
import com.wapo.flagship.util.tracking.states.AcquisitionEntranceTypeBuilder;
import com.wapo.flagship.util.tracking.states.NavigationBehavior;
import com.wapo.flagship.util.tracking.states.TrafficSource;
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem;
import com.washingtonpost.android.BuildConfig;
import com.washingtonpost.android.R;
import com.washingtonpost.android.follow.database.model.FollowEntity;
import com.washingtonpost.android.follow.misc.FollowTrackingInfo;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.features.ccpa.CCPAUtils;
import com.washingtonpost.android.paywall.util.PaywallUtil;
import com.washingtonpost.android.save.types.MyPostSection;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import kotlin.Pair;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Measurement {
    public static final Boolean OMNITURE_DEBUG = false;

    private static final String TAG = Measurement.class.getSimpleName();
    private static final String PREFS_NAME_APP_MEASUREMENT_CACHE = "APP_MEASUREMENT_CACHE";
    private static final String PREF_APP_MEASUREMENT_VISITOR_ID = "APP_MEASUREMENT_VISITOR_ID";
    private static final String PREF_SUB_ACCT_MGMT = "PREF_APP_MEASUREMENT_PAYWALL_SUBSCRIBER_TYPE";
    private static final String TRACKING_SERVER = "wpni.112.2o7.net";

    public static final String PAGE_HOMEPAGE = "homepage";
    public static final String PAGE_SEARCH = "search";
    public static final String PAGE_SEARCH_MAIN = "search_main";
    public static final String PAGE_SEARCH_RESULTS = "search results";
    public static final String PAGE_SEARCH_FILTER = "search_filter";
    public static final String PAGE_COMICS = "comics";
    public static final String PAGE_PDFPREVIEW = "front - epaper/";

    public static final String PAGE_WATCH_VIDEO = "front - watch";
    public static final String PAGE_PDFFULL = "epaper - ";
    private static final String PAGE_GALLERY = "gallery";
    private static final String PAGE_MAIN = "main";
    private static final String PAGE_FAQ = "faq";
    private static final String PAGE_CONTACTUS = "contactus";
    private static final String PAGE_HELPCENTER = "helpcenter";
    private static final String PAGE_PRIVACY_POLICY = "privacypolicy";
    private static final String PAGE_TERMS_OF_USE = "termsofuse";

    public static final String ELECTION_INLINE_SEARCH_NAVIGATION = "hp_chain_tiling_inline_search";
    private static final String PAGE_APP_REVIEW = "appreview";
    private static final String PAGE_FRONT_TOP_STORIES = "front - top-stories";
    private static final String PAGE_FRONT_WATCH = "front - watch";
    private static final String PAGE_TOP_STORIES = "top-stories";
    private static final String PAGE_FRONT = "front";
    public static final String PAGE_FRONT_PREFIX = "front - ";
    public static final String PAGE_BIOPAGE_AUTHOR_PREFIX = "biopage - ";
    public static final String PAGE_FRONT_MY_POST_PREFIX = "front - my-post - ";
    public static final String PAGE_FRONT_MY_POST = "front - my-post";
    public static final String PAGE_FRONT_MY_POST_ALL = "front - my-post - all";
    public static final String PAGE_FRONT_MY_POST_SAVED_STORIES = "front - my-post - reading-list";
    public static final String PAGE_FRONT_MY_POST_TOPICS = "front - my-post - topics";
    public static final String PAGE_FRONT_MY_POST_FOLLOWING = "front - my-post - following";
    public static final String PAGE_FRONT_MY_POST_READING_HISTORY = "front - my-post - history";
    public static final String PAGE_FRONT_FIND = "front - find";
    public static final String PAGE_FRONT_MY_POST_PURCHASE = "front - my-post - purchase";
    public static final String APP_SECTION_MY_POST_SAVED_STORIES = "reading-list";
    public static final String APP_SECTION_MY_POST_TOPICS = "topics";
    public static final String APP_SECTION_MY_POST_FOLLOWING = "following";
    public static final String APP_SECTION_MY_POST_READING_HISTORY = "history";
    public static final String APP_SECTION_MY_POST_PURCHASE = "history";
    public static final String PAGE_RECIPE_FINDER_LANDING = "front - food - recipes";
    public static final String MY_POST_READING_HISTORY_ARTICLE_TOP = "my_post_reading_history_article_top";
    public static final String MY_POST_READING_HISTORY_CAROUSEL = "my_post_reading_history_carousel";
    public static final String MY_POST_READING_HISTORY = "my_post_reading_history";
    public static final String CONTENT_TYPE_MAIN = "today's paper";
    public static final String CONTENT_TYPE_BLOG = "blog";
    public static final String CONTENT_TYPE_ARTICLE = "article";
    public static final String CONTENT_TYPE_GALLERY = "gallery";
    public static final String CONTENT_TYPE_COMICS = "comics";
    public static final String INTERFACE_TYPE_PRINT = "epaper";
    public static final String INTERFACE_TYPE_DIGITAL = "digital";
    private static final String CONTENT_TYPE_PDF = "pdf";
    public static final String CONTENT_TYPE_BIOPAGE = "biopage";
    public static final String CONTENT_TYPE_FRONT = "front";
    public static final String CHANNEL_COMICS = "entertainment";

    private static final DateFormat comicsDateFormat = new SimpleDateFormat("yyyMMdd");
    private static final String GALLERY_IMAGE_PREFIX = "gallery-image";
    private static String version;
    public static final String PATH_TO_VIEW_SWIPE = "swipe";
    public static final String PATH_TO_VIEW_TOP_RIBBON = "top_ribbon";
    public static final String PATH_TO_VIEW_PUSH_NOTIFICATION = "push";
    public static final String PATH_TO_VIEW_PUSH_BREAKING = "banner_breaking-news";
    public static final String PATH_TO_VIEW_FRONT = "front";
    public static final String PATH_TO_VIEW_BACK = "back";
    public static final String PATH_TO_VIEW_MENU_SECTIONS = "menu_sections";
    public static final String PATH_TO_VIEW_APP_OPEN = "app-open";
    public static final String PATH_TO_VIEW_RECIRCULATION_MOST_READ = "recircmodule_article_end_%d";
    public static final String PATH_TO_VIEW_RECIRCULATION_INLINE_CAROUSEL = "recirc_inline_%s_%d";
    public static final String PATH_TO_VIEW_RECIRCULATION_FOR_YOU = "recircmodule_for_you_%d";
    public static final String PATH_TO_VIEW_FRONTS_STACK = "brights_stack_open";
    public static final String PATH_TO_VIEW_FRONTS_CAROUSEL = "brights_carousel_open";
    public static final String PATH_TO_VIEW_FRONTS_CAROUSEL_IMMERSION = "immersion_carousel_open";
    public static final String PATH_TO_VIEW_FRONTS_CAROUSEL_EXTERNAL = "external_carousel_open";
    public static final String PATH_TO_VIEW_FRONTS_CAROUSEL_RECIPE = "recipe_carousel_open";
    public static final String PATH_TO_VIEW_FRONTS_CAROUSEL_SEVEN_LIVE = "seven_live_carousel_open";
    public static final String PATH_TO_VIEW_MY_POST_BANNER = "my_post_banner";
    public static final String PATH_TO_VIEW_FOR_YOU_SECTION = "sf_for_you_article_open_%d";
    public static final String PATH_TO_VIEW_SECTION_PREFIX = "sf";
    public static final String NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_FORWARD = "brights_stack_forward";
    public static final String NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_BACK = "brights_stack_back";
    public static final String NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_CAROUSEL = "bright_carousel_swipe";
    public static final String NAVIGATION_BEHAVIOR_COMMENTS_SWIPE_CAROUSEL = "commenting_carousel_swipe";
    public static final String AUDIO_CAROUSEL_PLAYER_TYPE = "audio_carousel";
    public static final String AUDIO_PERSO_PODCAST_PLAYER_TYPE = "audio_perso_podcast";
    public static final String PERSO_PODCAST = "perso_podcast";
    public static final String AUDIO_FLEX_PLAYER_TYPE = "audio_flexfeature";
    public static final String AUDIO_ACTION_BUTTON_TYPE = "audio_action_button";
    public static final String NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_SWIPE = "audio_carousel_swipe";
    public static final String NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_PLAY = "audio_carousel_play";
    public static final String NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_OPEN = "audio_carousel_open_";
    public static final String NAVIGATION_BEHAVIOR_SEARCH_NOT_FOUND = "search_not_found";
    public static final String NAVIGATION_AUDIO_CAROUSEL_FORWARD = "audio_carousel_forward";
    public static final String NAVIGATION_AUDIO_CAROUSEL_BACK = "audio_carousel_back";
    public static final String NAVIGATION_AUDIO_CAROUSEL_ROLL_THROUGH = "audio_article_carousel_rollthrough";
    public static final String NAVIGATION_AUDIO_RECOMMENDATION = "audio_article_recommended";
    public static final String NAVIGATION_AUDIO_AUTO = "audio_article_carplay";
    public static final String NAVIGATION_AUDIO_AUTO_ROLLTHROUGH = "audio_article_carplay_rollthrough";
    public static final String NAVIGATION_AUDIO_RECOMMENDATION_ROLLTHROUGH = "audio_article_recommended_rollthrough";
    public static final String PATH_TO_VIEW_DEEP_LINK = "deep_link";
    public static final String PATH_TO_VIEW_IN_APP_PROMPT = "in_app_prompt";
    public static final String PATH_TO_VIEW_ONELINK = "appsflyer_link";
    public static final String PATH_TO_VIEW_WIDGET_SMALL = "fusion_widget_small";
    public static final String PATH_TO_VIEW_WIDGET = "fusion_widget";
    public static final String PATH_TO_VIEW_FOR_YOU_WIDGET_LARGE = "for_you_widget_large";
    public static final String PATH_TO_VIEW_UNKNOWN_WIDGET = "unknown_widget";
    public static final String EVAR_CHANNEL = "&&channel";
    public static final String PATH_TO_VIEW_AUTHOR_CARD = "author_card";
    public static final String PATH_TO_VIEW_FOLLOWING = "my_post_following";
    public static final String PATH_TO_VIEW_MENU_RECENT = "menu_recent";
    public static final String PATH_TO_VIEW_AUDIO_INLINE = "audio_article_inline";
    public static final String PATH_TO_VIEW_AUDIO_TOP_BAR = "audio_article_topbar";
    public static final String PATH_TO_VIEW_AUDIO_CAROUSEL = "audio_article_carousel";
    public static final String PATH_TO_VIEW_AUDIO_FLEX = "audio_article_flexfeature";
    public static final String PATH_TO_VIEW_AUDIO_ACTION_BUTTON = "audio_article_action_button";
    public static final String PATH_TO_VIEW_AUDIO_STANDALONE = "standalone";
    public static final String ASK_THE_POST = "ask";
    public static final String ASK_THE_POST_ATP = "ask_the_post";
    public static final String ASK_THE_POST_THREAD = "atp_thread";
    public static final String ASK_THE_POST_BANNER = "atp_banner";
    public static final String ASK_THE_POST_HISTORY = "atp_history_icon";
    public static final String ASK_THE_POST_TOGGLE = "tab_toggle";
    public static final String ASK_THE_POST_SHARE_COPY = "share-copylink";
    public static final String ASK_THE_POST_SHARE_DELETE = "delete-link";
    public static final String ASK_THE_POST_SCREENSHOT = "screenshot-prompt";
    public static final String ASK_THE_POST_DEEPLINK = "atp_deeplink";
    public static final String ASK_THE_POST_HISTORY_SAVE = "atp_history_save";
    public static final String ASK_THE_POST_HISTORY_SAVE_SEEN = "atp_history_save_prompt_seen";
    public static final String ASK_THE_POST_HISTORY_SHARE = "atp_history_share";
    public static final String ASK_THE_POST_HISTORY_SHARE_DISMISSED = "atp_history_share_dismiss";
    public static final String ASK_THE_POST_HISTORY_SHARE_SEEN = "atp_history_share_seen";
    public static final String ASK_THE_POST_HISTORY_SAVE_DISMISSED = "atp_history_save_dismiss";
    public static final String PATH_TO_VIEW_INLINE_LINK = "lk_inline";
    public static final String PATH_TO_VIEW_LUF_OUTCOME_POST = "luf_outcome_post";
    public static final String PATH_TO_VIEW_GIFT_ARTICLE = "gifta";
    public static final String PATH_TO_VIEW_BACK_TO_FRONT = "back_to_front";
    public static final String PATH_TO_VIEW_MY_POST_CAROUSEL = "my_post_%s_carousel%s";
    public static final String PATH_TO_VIEW_MY_POST_ARTICLE_TOP = "my_post_%s_article_top";
    public static final String PATH_TO_VIEW_MY_POST_ARTICLE_DETAIL = "my_post_%s%s";
    public static final String PATH_TO_VIEW_MY_POST_CROSSWORD = "my_post_crossword";
    public static final String NAVIGATION_BEHAVIOR_CHANGE_TAB = "change_tab";
    public static final String NAVIGATION_BEHAVIOR_MY_POST_TOPIC_FOLLOW = "my_post_topic_follow";
    public static final String NAVIGATION_BEHAVIOR_TOP_NAV = "top_nav_icon";
    public static final String NAVIGATION_BEHAVIOR_RELATED_ARTICLE = "recirc_video";

    public static final String APP_SECTION_FOLLOWING = "following";
    public static final String APP_SECTION_MY_POST = "my-post";

    public static int ARTICLE_POSITION = -1;

    public static final String RATING_PROMPT_INLINE = "prompt: in-line rating";
    public static final String RATING_PROMPT_INLINE_NO = "answer: in-line rating - not quite";
    public static final String RATING_PROMPT_INLINE_YES = "answer: in-line rating - yes";
    public static final String RATING_PROMPT_FEEDBACK = "prompt: feedback";
    public static final String RATING_PROMPT_FEEDBACK_NO = "answer: feedback - no thanks";
    public static final String RATING_PROMPT_FEEDBACK_YES = "answer: feedback -sure";
    public static final String RATING_PROMPT_APP_STORE = "prompt: app store rating";
    public static final String RATING_PROMPT_APP_STORE_NO = "answer: app store rating - no thanks";
    public static final String RATING_PROMPT_APP_STORE_YES = "answer: app store rating - sure";

    /* Settings Pages */
    public static final String PAGE_SETTINGS = "front - settings";
    public static final String PAGE_SETTINGS_TITLE = "settings";
    private static final String PAGE_SETTINGS_ACCOUNT = "front - settings_account";
    private static final String PAGE_SETTINGS_ACCOUNT_EDIT_EMAIL = "front - settings_account - edit_email_password";
    private static final String PAGE_SETTINGS_ACCOUNT_EDIT_NAME = "front - settings_account - edit_name_photo";
    private static final String PAGE_SETTINGS_ACCOUNT_MANAGE_NAME = "front - settings_account - manage subscription";
    public static final String PAGE_SETTINGS_ACCOUNT_BENEFITS = "front - settings_account - benefits";
    private static final String NAVIGATION_BEHAVIOR_BENEFIT = "benefit";

    /* Custom Nav */
    public static final String PAGE_CUSTOM_NAV = "top preferences";
    public static final String CUSTOM_NAV_RESET = "topic_preferences_reset";
    public static final String CUSTOM_NAV_PREFIX = "topic_preferences_";

    /* Push Notifications */
    public static final String PAGE_ALERT_SETTINGS = "alerts settings";
    public static final String ALERTS = "alerts";

    /* Live Video and Breaking news banners */
    public static final String BREAKING_NEWS_BANNER = "breaking-news";

    /* Tracking ID */
    public static final String TRACKING_ID = "tid=a_classic-android";

    /* Universal Save */
    public static final String SAVE_ONBOARDING = "universal_save_onboarding";
    public static final String SAVE_ONBOARDING_SKIPPED = "universal_save_onboarding_skip";
    public static final String SAVE_ONBOARDING_CONTINUE = "universal_save_onboarding_continue";

    /* Onboarding 2 */
    public static final String PROFILE_PERSONALIZE = "profile_personalize";
    public static final String PROFILE_PERSONALIZE_DISMISS = "profile_personalize_dismiss";
    public static final String PROFILE_PERSONALIZE_START = "profile_personalize_start";
    public static final String PROFILE_PREFERENCE_ALERTS = "profile_preference_alerts";
    public static final String PROFILE_PREFERENCE_ALERTS_DISMISS = "profile_preference_alerts_dismiss";
    public static final String PROFILE_PREFERENCE_CONTENT_PACK = "profile_preference_content_pack";
    public static final String PROFILE_PREFERENCE_CONTENT_PACK_DISMISS = "profile_preference_content_pack_dismiss";
    public static final String PROFILE_PREFERENCE_AUDIO = "profile_preference_personalized_audio";

    /* Paywall */
    public static final String PAYWALL_STANDARD = "standard-wall";
    public static final String PAYWALL_ROLLING = "rolling-meter-wall";
    public static final String PAYWALL_GROUP = "politics-opinions-wall";
    public static final String PAYWALL_SAVE = "universal_save_attempt";
    public static final String PAYWALL_SETTINGS = "settings";
    public static final String PAYWALL_ARTICLE_URL = "article-inline-links";
    public static final String PAYWALL_IN_APP_PROMPT = "in_app_prompt";
    public static final String PAYWALL_GIFT_NO_SUB = "gift_article_share";

    /* Login */
    public static final String SIGN_IN_FROM_ONBOARDING = "onboarding_save";
    public static final String SIGN_IN_FROM_METERED_PAYWALL = "paywall";
    public static final String SIGN_IN_FROM_SAVE_PAYWALL = "savepaywall";
    public static final String SIGN_IN_OR_OUT_FROM_SETTINGS = "settings";
    public static final String SIGN_IN_FROM_GLOBAL_SUBSCRIBE_BUTTON = "global_subscribe_button";
    public static final String SIGN_IN_HOMEPAGE = "homepage_signin";

    /* Global Subscribe Button */
    public static final String GLOBAL_SUBSCRIBE_BUTTON = "global_subscribe_button";

    /* Deep Link */
    public static final String PAYWALL_DEEP_LINK = "deep_link";

    /* Widget */
    public static final String PAYWALL_WIDGET_SMALL = PATH_TO_VIEW_WIDGET_SMALL;
    public static final String PAYWALL_WIDGET = PATH_TO_VIEW_WIDGET;

    /* Webview Articles */
    public static final String PAYWALL_WEBVIEW = "app_webview";

    public static final String ACQUISITION_ENTRANCE_TYPE = "article_subscribe_button";

    public static final String GIFT_ARTICLE_BOTTOM_CTA = "gift_article";

    public static final String GIFT_ARTICLE_INVALID = "gift_article_invalid";
    /* APP Names */
    public static final String CLASSIC_GOOGLE = "app-classic-android:google";
    public static final String CLASSIC_AMAZON = "app-rainbow-android:amazon";

    /* Audio */
    public static final String AUDIO_TYPE_POLLY = "polly";
    public static final String AUDIO_TYPE_HUMAN = "human-read";
    public static final String AUDIO_TYPE_STANDALONE = "standalone";
    public static final String AUDIO_TYPE_PODCAST = "podcast";
    public static final String PAGE_CAR_PODCAST = "carplay_podcasts";

    public static final String PATH_TO_VIEW_AUDIO_PLAYLIST = "audio_article_playlist";

    public static final String NAVIGATION_AUDIO_PLAYLIST_ROLL_THROUGH = "audio_article_playlist_rollthrough";

    public static final String GEN_DIMEN_ACTION_BUTTON = "action_button";

    public static final String SOURCE_APP = "sourceApp";

    public static final String RECIPE_FINDER_PAGE = "recipe_finder";

    public static final String RECIPE_FINDER_NAVIGATION = "sr_recipe-finder";
    public static final String RECIPE_FINDER_INLINE_SEARCH_NAVIGATION = "sf_lifestyle_food_recipes_search_bar_inline_search";
    public static final String RECIPE_FINDER_DEEPLINK_NAVIGATION = "sr_recipe-finder_deep_link";

    public static final String TOPIC_FOLLOW = "topic_follow";
    public static final String ITID = "itId";
    public static final String INLINE_TOPIC_FOLLOW = "inline_topic_follow";
    public static final String INLINE_HOMEPAGE_EXTRA_ACCOUNT = "hp_inline_link";
    public static final String INLINE_ARTICLE_EXTRA_ACCOUNT = "article_inline_link";
    public static final String ATP_INLINE_MAIN_PANEL_DISPLAY = "atp_inline_main_panel_display";
    public static final String ATP_INLINE_PANEL_DISMISS = "atp_inline_panel_dismiss";
    public static final String ASK_THE_POST_PREFIX = "atp_";
    public static final String ASK_THE_POST_PANEL = "tab_";
    public static final String ASK_THE_POST_SOURCE_PANEL_DISPLAY = "_source_panel_display";
    public static final String ASK_THE_POST_FEEDBACK_SEEN = "seen_feedback_ask_tab";
    public static final String ASK_THE_POST_FEEDBACK_SUBMITTED = "submit_feedback_ask_tab";

    public static final String MISCELLANY_UNIFIED_SIGN_IN_IMPRESSION = "unified_sign_in_impression";

    // Article Summaries
    public static final String MISCELLANY_SUMMARY_SEEN = "seen_summary_ai";
    public static final String MISCELLANY_FEEDBACK_SEEN = "seen_feedback_ai";
    public static final String MISCELLANY_SUBMIT_FEEDBACK = "submit_feedback_ai";

    private static final String AI_OVERVIEW_SEEN_PREFIX = "_aio:";
    private static final String AI_OVERVIEW_SEEN_YES = AI_OVERVIEW_SEEN_PREFIX + "Y";
    private static final String AI_OVERVIEW_SEEN_NO = AI_OVERVIEW_SEEN_PREFIX + "N";
    private static final String AI_OVERVIEW_EXPAND = "search_aio_expand";
    private static final String AI_OVERVIEW_CAROUSEL_NAV_BEHAVIOR_PREFIX = "search_aio_carousel_";

    public static final String MAP_MENU = "map_menu";
    public static final String MAP_LISTENER_SCROLL = "map_listener_scroll";
    public static final String MAP_RECIRC_DISPLAY = "map_recirc_display";
    public static final String MAP_RECIRC_DISPLAY_RECOMMEND = "map_recirc_display_recommend";
    private static final String MAP_RECIRC_DISMISS = "map_recirc_dismiss";
    public static final String MAP_RECIRC = "map_recirc";
    public static final String MAP_MENU_ICON_CLICK = "map_menu_icon_click";
    public static final String MAP_MENU_RECOMMEND_CLICK = "map_menu_recommend_click";
    public static final String MAP_MENU_SNOOZE_CLICK = "map_menu_snooze_click";
    public static final String FTS_INLINE = "fts_inline";
    public static final String FTS_INLINE_VIDEO = "fts_inline_video";
    public static final String FTS_BOTTOM_LINK_ = "fts_bottom_link_";
    public static final String FTS_BOTTOM_VIDEO_ = "fts_bottom_video_";
    public static final String VOICE_CLICK = "voice-click";
    public static final String VOICE_OPEN = "voice-open";
    public static final String SUBMIT_QUESTION = "submit-question";
    public static final String VOICE_QUESTION = "voice-question";
    public static final String ATP_RESPONSE = "atp-response";
    public static final String VOICE_RESPONSE = "voice-response";
    public static final String SUBMIT_FOLLOWUP = "submit-followup";
    public static final String VOICE_FOLLOWUP = "voice-followup";
    public static final String VOICE_ERROR = "voice-error";
    public static final String SPEECH_REC_ERROR = "speech-rec-error";
    public static final String TRY_TEXT = "try-text";
    public static final String INCREMENT_VIDEO_PROGRESS = "increment_video_progress";
    public static final String PAYWALL_KIND = "paywall";
    private static final Set<String> COOKIES_JS_KEYS = Set.of("rct");
    private static boolean isSectionSwipe = false;
    public static String oneLinkNavigationBehavior = "";
    public static String autoNavigationBehavior = "carplay_app_open";
    public static String autoTopRibbonNavigationBehavior = "top_ribbon";
    private static String prevPageName = null;
    private static String prevEntryPoint = null; // Used to persist entry point in magic link / social redirect flow
    private static String paywallArticle = null;
    private static String paywallArcId = null;
    private static String appVersion;
    private static String connectionType;
    private static String userAgent;
    private static String androidVersion;
    private static Context context; // Ignore warning. No memory leak because uses app context only.
    private static String hardwareId;
    private static String buildType;
    private static String visitorId;
    private static MeasurementMap defaultMap;
    private static MeasurementMap previousMap = new MeasurementMap();

    private static MeasurementMap userPropertyMap = new MeasurementMap();

    private static MeasurementMap videoShareMap = new MeasurementMap();

    private static MeasurementMap audioCarouselMap = new MeasurementMap();

    private static MeasurementMap articleContentMap = new MeasurementMap();

    private static MeasurementMap searchTrackingMap = new MeasurementMap();

    private static final DebouncedAnalytics analytics = new DebouncedAnalytics();
    private static boolean isAudioCarouselOriginated = false;
    private static boolean isSearchOriginated = false;
    private static boolean isAutoOriginated = false;
    private static boolean isAutoOpen = false;
    private static boolean isTopRibbonOriginated = false;
    private static boolean isPushOriginated = false;
    private static boolean isWidgetOriginated = false;
    public static boolean isInAppMessageOriginated = false;
    private static PostAnswerCarouselTrackingHelper postAnswersCarouselInfo = null;
    private static String widgetType = null;

    private static String askThePostOrigination = null;

    private static FirebaseTrackingManager firebaseTrackingManager; // Ignore warning. No memory leak because uses app context only.
    private static PermutiveProvider permutiveProvider;
    private static String wpmmArticleContentId;
    private static String appName;
    private static String tabName;
    private static String abTestGroup = "";
    private static InAppMessageData currentInAppMessageData = null;

    // Consent State
    private static Boolean targetingEnabled = false;
    private static Boolean ccpaAdsOptedOut = true;

    private static String habitTilesNavigationBehavior = null;

    private static String navigationBehaviorCache = null;
    private static volatile TrafficSource trafficSourceCache = null;

    private static Boolean firedResponseTracking = false;

    private enum TRACKING_TAB_NAMES {

        TOP_STORIES("top_stories"),
        SECTION_LIST("section_list"),
        ALERTS("alerts"),
        MY_POST("my_post"),
        EPAPER("epaper");

        private final String trackingTabName;

        TRACKING_TAB_NAMES(String trackingTabName) {
            this.trackingTabName = trackingTabName;
        }
    }

    public static void configureAppMeasurement(Context appContext) {
        appVersion = detectAppVersion(appContext);
        version = appVersion;
        connectionType = detectConnectionType(appContext);
        buildType = BuildConfig.STORE_TYPE;
        androidVersion = detectAndroidVersion();
        appName = detectAppName();
        context = appContext;
        hardwareId = DeviceUtils.getDeviceSerialId(appContext);
        initializeTargetingProperties();
        firebaseTrackingManager = new FirebaseTrackingManager(appContext);
        firebaseTrackingManager.setUserId(getVisitorIdFromPrefs(appContext));
        userAgent = "";
        Observable.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return detectUserAgent(appContext);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String value) {
                        userAgent = value;
                    }
                });
        initializeProviders();
    }


    /**
     * Initializes analytics providers based on consent state.
     */
    private static void initializeProviders() {
        if (isTargetingEnabled()) {
            Logger.d(TAG, "Initializing Providers");
            initializePermutiveSdk();
        } else {
            Logger.d(TAG, "Releasing Providers");
            releasePermutiveSdk();
        }
    }

    /**
     * Initializes Permutive SDK and set identity
     */
    private static void initializePermutiveSdk() {
        if (isPermutiveSdkInitialized() || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;
        permutiveProvider = new PermutiveProvider(context,
                WapoSecDataProvider.INSTANCE.getPermutive(), new PermutiveTranslator());
        permutiveProvider.initialize();
        setIdentityToPermutiveSdk(getLoginUUID());
    }

    /**
     * Releases Permutive SDK
     */
    private static void releasePermutiveSdk() {
        if (!isPermutiveSdkInitialized()) return;
        try {
            permutiveProvider.release();
        } catch (IOException e) {
            e.printStackTrace();
            Logger.e(TAG, "Exception in release PermutiveSdk. Error Message=" + e.getMessage());
        }
        permutiveProvider = null;
    }

    private static boolean isPermutiveSdkInitialized() {
        return permutiveProvider != null && permutiveProvider.isSdkInitialized();
    }

    private static void setIdentityToPermutiveSdk(String loginId) {
        if (!isPermutiveSdkInitialized()) return;
        permutiveProvider.setLoginIdentity(loginId);
    }

    private static Boolean getIsLowDataMode() {
        return AppPreferences.INSTANCE.isLowDataModeEnabled();
    }

    /**
     * Initializes targetingEnabled and ccpaAdsOptedOut members.
     * Providers can be initialized or released based on these consent values.
     */
    private static void initializeTargetingProperties() {
        targetingEnabled = OneTrustHelper.INSTANCE.isTargetingEnabled();
        ccpaAdsOptedOut = CCPAUtils.hasUserOptedOutCCPAAdsTracking(context);
    }

    /**
     * This method returns Firebase analytics if properly initialized. The app has cases
     * where it won't be initialized, such as GDPR consent.
     *
     * @return Firebase Analytics instance if available
     */
    @Nullable
    public static FirebaseAnalytics getFirebaseAnalytics() {
        if (firebaseTrackingManager != null) {
            return firebaseTrackingManager.getFirebaseAnalytics();
        }
        return null;
    }

    public static String getVisitorIdFromPrefs(Context ctx) {
        visitorId = loadVisitorId(ctx);
        if (visitorId == null) {
            visitorId = generateUUID();
            saveVisitorId(ctx, visitorId);
        }
        return visitorId;
    }

    protected static void saveVisitorId(Context ctx, String vid) {
        if (ctx != null) {
            SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NAME_APP_MEASUREMENT_CACHE, 0).edit();
            editor.putString(PREF_APP_MEASUREMENT_VISITOR_ID, vid);
            editor.apply();
        }
    }

    protected static String loadVisitorId(Context ctx) {
        if (ctx != null) {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME_APP_MEASUREMENT_CACHE, 0);
            return prefs.getString(PREF_APP_MEASUREMENT_VISITOR_ID, null);
        }
        return null;
    }

    public static void trackSectionScrollingPercentage(int percentage, int totalFeatureItems, String sectionDisplayName) {
        Events pctEvent = getSectionScrollEventMapping(percentage);
        if (pctEvent != null) {
            if (!fusionMapEventsSent.containsKey(sectionDisplayName)) {
                fusionMapEventsSent.put(sectionDisplayName, EnumSet.noneOf(Events.class));
            }
            EnumSet<Events> events = fusionMapEventsSent.get(sectionDisplayName);
            if (events != null && !events.contains(pctEvent)) {
                MeasurementMap map = getPreviousMap();
                setNightModeStatus(map);
                fusionEventsSent.add(pctEvent);
                fusionMapEventsSent.put(sectionDisplayName, fusionEventsSent);
                map.setEvar(Evars.TOTAL_FEATURES.getVariable(), totalFeatureItems);
                setMiscellany(map, String.valueOf(totalFeatureItems));
                trackEvents(map, pctEvent.getKey());
            }
        }
    }

    public static void trackSearchPageRecipeQuerySubmitted(String query, String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        map.setEvar("subsection", "food");
        setLoginSubscriptionStatus(map);
        map.setEvar(Evars.CONTENT_TYPE.getVariable(), RECIPE_FINDER_PAGE);
        Measurement.setPageName(map, "recipe_finder_result");
        map.setEvar(Evars.CONTENT_SECTION.getVariable(), "lifestyle");
        map.setEvar(Evars.SUB_SECTION.getVariable(), "food");
        setSearchKeywords(map, query);
        setNavigationBehavior(map, navigationBehavior);
        searchTrackingMap.putAll(map);
        map.setEvar(Evars.PAGE_NAME.getVariable(), "recipe_finder_result");
        Measurement.trackEvent(map, Events.RECIPE_FINDER_SEARCH_SUBMIT);
    }

    public static void trackSearchPageElectionQuerySubmitted(String query, String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        isSearchOriginated = true;
        Measurement.setPageName(map, "election_search_result");
        setSearchKeywords(map, query);
        setNavigationBehavior(map, navigationBehavior);
        searchTrackingMap.putAll(map);
        setUUID(map);
        searchTrackingMap.putAll(map);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackSubNavItemClick(String navigationBehaviour) {
        final MeasurementMap map = getDefaultMap();
        setNavigationBehavior(map, navigationBehaviour);
        setPageName(map, "election_search_main");
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackSearchElectionPageView(String pageName, QueryFilter query, String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        isSearchOriginated = true;
        setLoginSubscriptionStatus(map);
        setPageName(map, pageName);
        if (query != null) {
            setSearchKeywords(map, query.getQuery());
        }
        setNavigationBehavior(map, navigationBehavior);
        searchTrackingMap.putAll(map);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackSearchRecipePageView(String pageName, String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        Measurement.setPageName(map, RECIPE_FINDER_PAGE);
        map.setEvar(Evars.CONTENT_SECTION.getVariable(), "lifestyle");
        map.setEvar(Evars.SUB_SECTION.getVariable(), "food");
        map.setEvar(Evars.CONTENT_TYPE.getVariable(), RECIPE_FINDER_PAGE);
        setPageName(map, pageName);
        setNavigationBehavior(map, navigationBehavior);
        searchTrackingMap.putAll(map);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static Events getSectionScrollEventMapping(float percentWatched) {
        if (percentWatched >= 99) {
            return Events.EVENT_PERCENTAGE_SECTION_100;
        } else if (percentWatched >= 75) {
            return Events.EVENT_PERCENTAGE_SECTION_75;
        } else if (percentWatched >= 50) {
            return Events.EVENT_PERCENTAGE_SECTION_50;
        } else if (percentWatched >= 25) {
            return Events.EVENT_PERCENTAGE_SECTION_25;
        } else {
            return null;
        }
    }

    public static void trackCommentCreateSuccess(
            @Nullable TrackingInfo trackingInfo,
            CommentItem newComment,
            boolean isReply, String avArcId) {
        MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        if (trackingInfo != null) {
            setSubsection(map, trackingInfo.getContentSubsection());
            setContentUrl(map, trackingInfo.getContentURL());
            setAuthorId(map, trackingInfo.getAuthorId());
            setContentAuthor(map, trackingInfo.getContentAuthor());
            setContentTopics(map, trackingInfo.getContentTopics());
            setContentType(map, trackingInfo.getContentType());
            setFirstPublishedDate(map, trackingInfo.getFirstPublishedDate());
            setPageName(map, trackingInfo.getPageName());
            setArcId(map, trackingInfo.getArcId());
        }
        setAvArcId(map, avArcId);
        setNavigationBehavior(NavigationBehavior.COMMENTS);
        String miscellany = isReply ? "commenting-create-comment-reply" : "commenting-create-comment";
        setMiscellany(map, miscellany);
        String genEventDimension = CommentAnalyticsHelper.INSTANCE.getCommenterType(newComment);
        setGenEventDimension(map, genEventDimension);
        trackEvents(map, Events.EVENT_COMMENTING_CREATE_COMMENT.getKey());
    }

    public static void trackCommentReactionCreated(
            @Nullable TrackingInfo trackingInfo,
            CommentItem comment,
            String miscellany, String avArcId) {
        MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        if (trackingInfo != null) {
            setSubsection(map, trackingInfo.getContentSubsection());
            setContentUrl(map, trackingInfo.getContentURL());
            setAuthorId(map, trackingInfo.getAuthorId());
            setContentAuthor(map, trackingInfo.getContentAuthor());
            setContentTopics(map, trackingInfo.getContentTopics());
            setContentType(map, trackingInfo.getContentType());
            setFirstPublishedDate(map, trackingInfo.getFirstPublishedDate());
            setPageName(map, trackingInfo.getPageName());
            setArcId(map, trackingInfo.getArcId());
        }
        setAvArcId(map, avArcId);
        setNavigationBehavior(NavigationBehavior.COMMENTS);
        setMiscellany(map, miscellany);
        map.setEvar(Evars.GEN_EVENT_DIMENSION.getVariable(), "comment_sentiment");
        trackEvents(map, Events.EVENT_COMMENTING_INTERACTION.getKey());
    }

    public static void trackCommentView(
            @Nullable TrackingInfo trackingInfo,
            String category) {
        MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        if (trackingInfo != null) {
            setSubsection(map, trackingInfo.getContentSubsection());
            setContentUrl(map, trackingInfo.getContentURL());
            setAuthorId(map, trackingInfo.getAuthorId());
            setContentAuthor(map, trackingInfo.getContentAuthor());
            setContentTopics(map, trackingInfo.getContentTopics());
            setArcId(map, trackingInfo.getArcId());
            setTitle(map, trackingInfo.getTitle());
            setContentType(map, trackingInfo.getContentType());
            setFirstPublishedDate(map, trackingInfo.getFirstPublishedDate());
            setPageName(map, trackingInfo.getPageName());
        }
        setNavigationBehavior(NavigationBehavior.COMMENTS);
        setMiscellany(map, "commenting-load");
        map.setEvar(Evars.GEN_EVENT_DIMENSION.getVariable(), category);
        trackEvents(map, Events.EVENT_COMMENTING_VIEW.getKey());
    }



    private static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void setAndroidVersion(final MeasurementMap map, String version) {
        map.setEvar(Evars.ANDROID_VERSION.getVariable(), version);
    }

    public static void setAppVersion(final MeasurementMap map, String appVersion) {
        map.setEvar(Evars.APP_VERSION_NUMBER.getVariable(), appVersion);
    }

    public static String getABTestGroup() {
        return abTestGroup;
    }

    public static void setAppName(final MeasurementMap map, String appName) {
        map.setEvar(Evars.PROPERTY_NAME.getVariable(), appName);
    }

    public static void setBlogName(final MeasurementMap map, String blogName) {
        map.setEvar(Evars.BLOG_NAME.getVariable(), blogName);
    }

    public static void setComicsSubsection(final MeasurementMap map, String subsection) {
        map.setEvar(Evars.COMICS_CONTENT_SUBSECTION.getVariable(), subsection);
    }

    public static void setConnectionType(final MeasurementMap map, String connectionType) {
        map.setEvar(Evars.CONNECTION_TYPE.getVariable(), connectionType);
    }

    public static void setContentAuthor(final MeasurementMap map, String contentAuthor) {
        map.setEvar(Evars.CONTENT_AUTHOR.getVariable(), contentAuthor);
    }

    public static void setContentSource(final MeasurementMap map, String contentSource) {
        map.setEvar(Evars.CONTENT_SOURCE.getVariable(), contentSource);
    }

    public static void setContentUrl(String contentUrl) {
        getDefaultMap().setEvar(Evars.CONTENT_URL.getVariable(), contentUrl);
    }

    public static void setContentUrl(final MeasurementMap map, String contentUrl) {
        map.setEvar(Evars.CONTENT_URL.getVariable(), contentUrl);
    }

    public static void setContentType(final MeasurementMap map, String contentType) {
        if (contentType != null) {
            contentType = contentType.replace("homepage", "front");
        }
        map.setEvar(Evars.CONTENT_TYPE.getVariable(), contentType);
    }

    public static void setExternalLink(final MeasurementMap map, String url) {
        map.setEvar(Evars.EXTERNAL_LINK.getVariable(), url);
    }

    public static void setUserName(final MeasurementMap map) {
        String loginId = "";
        final PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            loginId = paywallService.getLoggedInUser().getUserId();
        }
        map.setEvar(Evars.USER_NAME.getVariable(), loginId);
    }

    public static void setUUIDInDefaultMap(String uuid) {
        final MeasurementMap defaults = getDefaultMap();
        defaults.setEvar(Evars.IDENTITY_UUID.getVariable(), uuid);
    }

    public static void setUUID(final MeasurementMap map) {
        String uuid = "";
        final PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            uuid = paywallService.getLoggedInUser().getUuid();
        }
        map.setEvar(Evars.IDENTITY_UUID.getVariable(), uuid);
    }

    public static void setUserId(final MeasurementMap map) {
        String uuid = "";
        final PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            uuid = paywallService.getLoggedInUser().getUuid();
            map.setEvar(Evars.USER_ID.getVariable(), uuid != null ? uuid : getVisitorIdFromPrefs(context));
        } else {
            map.setEvar(Evars.USER_ID.getVariable(), getVisitorIdFromPrefs(context));
        }
    }

    public static void setSignInMedium(final MeasurementMap map) {
        String signInMedium = "";
        final PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            signInMedium = paywallService.getLoggedInUser().getSignedInThrough();
        }
        map.setEvar(Evars.SIGNIN_MEDIUM.getVariable(), signInMedium);
        getDefaultMap().setEvar(Evars.SIGNIN_MEDIUM.getVariable(), signInMedium);
    }

    public static void setMiscellany(MeasurementMap map, String miscellany) {
        map.setEvar(Evars.MISCELLANY.getVariable(), miscellany);
    }

    // TODO: Reconcile with getNewMap() below.
    public static MeasurementMap getDefaultMap() {
        if (defaultMap == null) {
            defaultMap = new MeasurementMap();
            setNavigationBehavior(defaultMap, PATH_TO_VIEW_APP_OPEN);
            setAppName(defaultMap, appName);
        }
        setStandardParams(defaultMap);
        return defaultMap;
    }

    public static MeasurementMap getNewMap() {
        MeasurementMap map = (MeasurementMap) getDefaultMap().clone();
        setConnectionType(map, connectionType);
        setUserAgent(map, userAgent);
        setAppVersion(map, appVersion);
        setAndroidVersion(map, androidVersion);
        setAppName(map, appName);
        setSupportId(map);
        setHardwareId(map);
        return map;
    }

    private static void setStandardParams(MeasurementMap defaultMap) {
        setUnificationParams(defaultMap);
        setTabName(defaultMap, tabName);
        setUserAttributes(defaultMap);
        setLoginSubscriptionStatus(defaultMap);
        setNightModeStatus(defaultMap);
        setUUID(defaultMap);
        setUserId(defaultMap);
        setABTestingVariants(defaultMap);
        setTrafficSourceValuesFromCache(defaultMap);
        applyCurrentPushId(defaultMap);
    }

    /**
     * Attach the currently tracked push id (if any) to every outgoing event
     * The value is set in {@link #fillMapWithTrackingInfo} when the user opens an article from
     * a push notification, and cleared on session end in {@link #trackEngagement}. When no push
     * id is active we explicitly remove any stale value so events emitted after session end do
     * not continue to persist.
     */
    private static void applyCurrentPushId(MeasurementMap map) {
        String pushId = PushIdTracker.INSTANCE.getPushId();
        if (pushId != null) {
            map.setEvar(Evars.PUSH_NOTIFICATION_ID.getVariable(), pushId);
        } else {
            map.remove(Evars.PUSH_NOTIFICATION_ID.getVariable());
        }
    }

    public static MeasurementMap getArticleContentMap() {
        return articleContentMap;
    }

    /**
     * Amazon Unification -> We are letting analytics know if user has updated from old Rainbow App to Unified Amazon App
     * Playstore Unification -> We are setting this for letting analytics get data on how many users have successfuly
     * migrated to classic and how many of them have moved their subscription over.
     *
     * @param map - default map
     */
    // TODO remove this in https://arcpublishing.atlassian.net/browse/AWA-7614 as it is no longer needed
    public static void setUnificationParams(final MeasurementMap map) {
        if (Utils.isProductFlavorAmazon() && PrefUtils.getHasAmazonClassic(context)) {
            boolean hasAmazonClassic = PrefUtils.getHasAmazonClassic(context);
            String migrated = PrefUtils.getHasMigratedFromAmazonClassic(context);
            StringBuilder sb = new StringBuilder();
            sb.append("has_rainbow_app:").append(hasAmazonClassic).append(";");
            sb.append("migrated:").append(migrated).append(";");

            // Adding null check because PaywallService and BillingHelper may not be initialized for some Analytics calls.
            if (PaywallService.getBillingHelper() != null) {
                String migratedAmazonClassicIAP = hasAmazonClassic ? (PaywallService.getBillingHelper().getMigratedAmazonClassicSubscription() != null ? "true" : "false") : "na";
                sb.append("moved_rainbow_in_app_sub:").append(migratedAmazonClassicIAP);
            }
            map.setEvar(Evars.RAINBOW_MIGRATION_ARRAY.getVariable(), sb.toString());
        } else if (Utils.isProductFlavorAmazon()) {
            boolean isAppUpdated = PrefUtils.getIsExistingUser(context);
            map.setEvar(Evars.RAINBOW_MIGRATION_ARRAY.getVariable(), "updated:" + isAppUpdated + ";");
        } else {
            boolean hasRainbowApp = PrefUtils.getHasRainbow(context);
            String migrated = PrefUtils.getHasMigratedFromRainbow(context);
            StringBuilder sb = new StringBuilder();
            sb.append("has_rainbow_app:").append(hasRainbowApp).append(";");
            sb.append("migrated:").append(migrated).append(";");

            // Adding null check because PaywallService and BillingHelper may not be initialized for some Analytics calls.
            if (PaywallService.getBillingHelper() != null) {
                String migratedRainbowIAP = hasRainbowApp ? (PaywallService.getBillingHelper().getMigratedRainbowSubscription() != null ? "true" : "false") : "na";
                sb.append("moved_rainbow_in_app_sub:").append(migratedRainbowIAP);
            }
            map.setEvar(Evars.RAINBOW_MIGRATION_ARRAY.getVariable(), sb.toString());
        }
    }

    public static void setTrafficSourceValues(@NonNull final MeasurementMap map, @NonNull TrafficSource trafficSource) {
        map.setEvar(Evars.TRAFFICSOURCE_SOURCE.getVariable(), trafficSource.getSource());
        map.setEvar(Evars.TRAFFICSOURCE_MEDIUM.getVariable(), trafficSource.getMedium());
        map.setEvar(Evars.TRAFFICSOURCE_CAMPAIGN.getVariable(), trafficSource.getCampaign());
    }

    public static void setTrafficSourceValuesFromUri(@NonNull final MeasurementMap map, @NonNull Uri uri) {
        setTrafficSourceValues(map, TrafficSource.Companion.fromDeepLinkUri(uri));
    }

    public static void setTrafficSourceValuesFromCache(@NonNull final MeasurementMap map) {
        TrafficSource trafficSource = getTrafficSourceCache();
        if (trafficSource != null) {
            setTrafficSourceValues(map, trafficSource);
        }
    }

    public static void mark(@Nullable TrafficSource trafficSource) {
        //  Persist trafficSource data
        trafficSourceCache = trafficSource;

        if (trafficSource != null && !trafficSource.isEmpty()) {
            //  Log the built-in Firebase event for campaign attribution
            MeasurementMap map = getNewMap();
            setTrafficSourceValues(map, trafficSource);
            trackEvent(map, Events.EVENT_CAMPAIGN_DETAILS);
        }
    }

    @Nullable
    public static TrafficSource getTrafficSourceCache() {
        return trafficSourceCache;
    }

    public static MeasurementMap getPreviousMap() {
        return previousMap;
    }

    public static void setPushAction(MeasurementMap map, String pushAction) {
        map.setEvar(Evars.PUSH_ACTION.getVariable(), pushAction);
    }

    public static void setPushUrl(MeasurementMap map, String pushUrl) {
        map.setEvar(Evars.PUSH_URL.getVariable(), pushUrl);
    }

    public static void setPushNotificationId(MeasurementMap map, String pushId) {
        map.setEvar(Evars.PUSH_NOTIFICATION_ID.getVariable(), pushId);
    }

    public static void setPushHeadline(MeasurementMap map, String headline) {
        map.setEvar(Evars.PUSH_HEADLINE.getVariable(), headline);
    }

    public static void setLufNavigationScroll(MeasurementMap map, String lufIndex) {
        map.setEvar(Evars.LUF_NAVIGATION.getVariable(), lufIndex);
    }

    public static void setLufNavigationTap(MeasurementMap map, String lufIndex) {
        map.setEvar(Evars.LUF_NAVIGATION.getVariable(), "luf_latest_updates_" + lufIndex);
    }

    public static void setUserAttributes(MeasurementMap map) {
        StringBuilder value = new StringBuilder();
        value.append(getPushStatus()).append(";");
        value.append(getPushList()).append(";");
        value.append(getCCPAOptInInfo());
        value.append(getAuthorsFollowings());
        map.setEvar(Evars.USER_ATTRIBUTES.getVariable(), value.toString());
    }

    public static void setPushTitle(MeasurementMap map, String pushTitle) {
        map.setEvar(Evars.PUSH_TITLE.getVariable(), pushTitle);
    }

    public static void setPushType(MeasurementMap map, String pushType) {
        map.setEvar(Evars.PUSH_TYPE.getVariable(), pushType);
    }

    public static void setPushTopicPlatform(MeasurementMap map, String pushTopicPlatform) {
        map.setEvar(Evars.PUSH_TOPIC_PLATFORM.getVariable(), pushTopicPlatform);
    }

    public static void setPushTimestamp(MeasurementMap map, String timestamp) {
        map.setEvar(Evars.PUSH_TIMESTAMP.getVariable(), timestamp);
    }

    public static void dispatchEventsNow() {
        if (firebaseTrackingManager != null) {
            firebaseTrackingManager.dispatchEventsNow();
        }
    }

    public static void setOrientation(final MeasurementMap map, String orientation) {
        map.setEvar(Evars.ORIENTATION.getVariable(), orientation);
    }

    public static void setPageFormat(final MeasurementMap map, String pageFormat) {
        map.setEvar(Evars.PAGE_FORMAT.getVariable(), pageFormat);
    }

    // TODO: Clean this up.
    public static void setPageName(final MeasurementMap map, String pageName) {
        if (pageName == null) {
            pageName = PAGE_FRONT_TOP_STORIES;
        }
        //remove occurences of / in some page names. not sure where this / is coming from
        // add any other exceptions to the slash removal
        if (!pageName.contains(PAGE_PDFPREVIEW)) {
            if (pageName.contains("/video")) {
                pageName = PAGE_FRONT_WATCH;
            } else {
                pageName = pageName.replace("/", "");
            }
        }

        //also not sure where wp - homepage is getting set
        if (pageName.equalsIgnoreCase("wp - homepage")) {
            pageName = PAGE_FRONT_TOP_STORIES;
        }
        if (!pageName.equals(prevPageName)) {
            setPreviousPageName(map);
            prevPageName = pageName;
        }

        map.setEvar(Evars.PAGE_NAME.getVariable(), pageName);
    }

    public static void setPreviousPageName(final MeasurementMap map) {
        if (prevPageName != null) {
            map.setEvar(Evars.PREV_PAGE.getVariable(), prevPageName);
        }
    }

    public static void setNavigationBehavior(final MeasurementMap map, String navigationBehavior) {
        if (navigationBehavior == null || navigationBehavior.isEmpty()) {
            return;
        }
        if (getIsLowDataMode()) {
            navigationBehavior = navigationBehavior + "_lite";
        }
        setNavigationBehaviorCache(navigationBehavior);
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), navigationBehavior);
    }

    public static void setNavigationBehavior(NavigationBehavior navigationBehavior) {
        setNavigationBehavior(navigationBehavior, -1);
    }

    private static void setNavigationBehaviorCache(String value) {
        if (value != null) {
            String baseValue = value;
            if (baseValue.endsWith("_lite")) {
                baseValue = baseValue.substring(0, baseValue.length() - "_lite".length());
            }
            if (!Objects.equals(baseValue, NavigationBehavior.BACK_TO_FRONT.getValue()) &&
                    !Objects.equals(baseValue, NavigationBehavior.STANDARD_WALL.getValue()) &&
                    !baseValue.contains(NavigationBehavior.SECTION_RIBBON_SWIPE.getValue())) {
                navigationBehaviorCache = value;
            }
        }
    }

    public static String getNavigationBehaviorCache() {
        return navigationBehaviorCache;
    }

    public static void setNavigationBehaviorInDefaultMap(String navigationBehavior) {
        final MeasurementMap defaults = getDefaultMap();
        setNavigationBehaviorCache(navigationBehavior);
        defaults.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), navigationBehavior);
    }

    public static void setNavigationBehavior(NavigationBehavior navigationBehavior, int carouselPos) {
        setNavigationBehaviorCache(navigationBehavior.getValue());
        switch (navigationBehavior) {
            case SAVE_CLICK: {
                String currentValue = (String) getDefaultMap().getEvar(Evars.NAVIGATION_BEHAVIOR.getVariable());
                getDefaultMap().setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), currentValue + "__save_attempt");
                break;
            }
            case GIFT_SEND_DIALOG: {
                String currentValue = (String) getDefaultMap().getEvar(Evars.NAVIGATION_BEHAVIOR.getVariable());
                getDefaultMap().setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), currentValue + "__gift_article");
                break;
            }
            case AUDIO_CAROUSEL:
            case SECTION_RIBBON_SWIPE:
            case SECTION_RIBBON_CLICK:
                getDefaultMap().setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), navigationBehavior.getValue() + (carouselPos + 1));
                break;
            default:
                getDefaultMap().setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), navigationBehavior.getValue());
                break;
        }
    }

    public static void setChangeTab(boolean isLaunch) {
        if (!isLaunch) {
            Measurement.setNavigationBehavior(Measurement.getDefaultMap(), NAVIGATION_BEHAVIOR_CHANGE_TAB);
        }
    }

    public static void setAudioFeed(final MeasurementMap map, String feed) {
        map.setEvar(Evars.AUDIO_FEED.getVariable(), feed);
    }

    public static void setSearchKeywords(final MeasurementMap map, String searchText) {
        map.setEvar(Evars.SEARCHED_KEYWORD.getVariable(), searchText);
    }

    // TODO: Merge into the generic setNavigationBehavior() as they are identical
    public static void setSearchNavigationBehavior(final MeasurementMap map, String type) {
        setNavigationBehaviorCache(type);
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), type);
    }

    public static void setCommercialNode(final MeasurementMap map, String commercialNode) {
        map.setEvar(Evars.COMMERCIAL_NODE.getVariable(), commercialNode);
    }

    public static void setContentCategory(final MeasurementMap map, String contentCategory) {
        map.setEvar(Evars.CONTENT_CATEGORY.getVariable(), contentCategory);
    }

    public static void setHeadline(final MeasurementMap map, String headline) {
        map.setEvar(Evars.HEADLINE.getVariable(), headline);
    }

    public static void setHierarchy(final MeasurementMap map, String hierarchy) {
        map.setEvar(Evars.HIERARCHY.getVariable(), hierarchy);
    }

    public static void setPrimarySection(final MeasurementMap map, String primarySection) {
        map.setEvar(Evars.CONTENT_SECTION.getVariable(), primarySection);
    }

    public static void setTitle(final MeasurementMap map, String title) {
        map.setEvar(Evars.TITLE.getVariable(), title);
    }

    public static void setAudioCarouselPublishDate(final MeasurementMap map, String publishDate) {
        map.setEvar(Evars.PUBLISHED_DATE.getVariable(), publishDate);
    }

    public static void setSiteSection(final MeasurementMap map, String siteSection) {
        map.setEvar(Evars.SITE_SECTION.getVariable(), siteSection);
    }

    public static void setSocialShare(final MeasurementMap map, String socialShare) {
        map.setEvar(Evars.SOCIAL_SHARE.getVariable(), socialShare);
    }

    public static void setEngagedTime(final MeasurementMap map, String engagedTime) {
        map.setEvar(Evars.ENGAGED_TIME.getVariable(), engagedTime);
    }

    public static void setEngagedTime(final MeasurementMap map, int engagedTime) {
        map.setEvar(Evars.ENGAGED_TIME.getVariable(), engagedTime);
    }

    public static void setSubsection(final MeasurementMap map, String subsection) {
        map.setEvar(Evars.CONTENT_SUBSECTION.getVariable(), subsection);
    }

    public static void setUserAgent(final MeasurementMap map, String userAgent) {
        map.setEvar(Evars.USER_AGENT.getVariable(), userAgent);
    }

    public static void setAvName(final MeasurementMap map, String videoName) {
        map.setEvar(Evars.AV_NAME.getVariable(), videoName);
    }

    public static void setAvDuration(final MeasurementMap map, Long duration) {
        map.setEvar(Evars.AV_DURATION.getVariable(), duration);
    }

    public static void setAvArcId(final MeasurementMap map, String id) {
        map.setEvar(Evars.AV_ARC_ID.getVariable(), id);
    }

    public static void setVideoSection(final MeasurementMap map, String videoSection) {
        map.setEvar(Evars.VIDEO_SECTION.getVariable(), videoSection);
    }

    public static void setVideoSource(final MeasurementMap map, String videoSource) {
        map.setEvar(Evars.VIDEO_SOURCE.getVariable(), videoSource);
    }

    public static void setVideoCategory(final MeasurementMap map, String videoCategory) {
        map.setEvar(Evars.VIDEO_CATEGORY.getVariable(), videoCategory);
    }

    public static void setChannel(final MeasurementMap map, String channel) {
        if (channel == null) {
            return;
        }
        String prefix = "atab -";
        if (channel.trim().startsWith(prefix)) {
            channel = channel.replace(prefix, "").trim();

        }
        if (channel.equalsIgnoreCase("wp - homepage")) {
            channel = channel.replace("wp", "front").replace("homepage", "top-stories");
        }
        getDefaultMap().setEvar(EVAR_CHANNEL, channel);
        map.setEvar(EVAR_CHANNEL, channel);
        setSiteSection(map, channel);
    }

    public static void setArcId(final MeasurementMap map, String arcId) {
        map.setEvar(Evars.ARC_ID.getVariable(), arcId);
    }

    public static void setVideoArcId(final MeasurementMap map, String arcId) {
        map.setEvar(Evars.AV_ARC_ID.getVariable(), arcId);
    }

    public static void setEventLabel(final MeasurementMap map, String eventLabel) {
        map.setEvar(Evars.EVENT_LABEL.getVariable(), eventLabel);
    }

    public static void setAvExp(final MeasurementMap map, String avExp) {
        map.setEvar(Evars.AV_EXP.getVariable(), avExp);
    }

    public static void setProgressThreshold(final MeasurementMap map, int threshold) {
        map.setEvar(Evars.PROGRESS_THRESHOLD.getVariable(), threshold);
    }

    public static void clearProgressThreshold(final MeasurementMap map) {
        map.setEvar(Evars.PROGRESS_THRESHOLD.getVariable(), null);
    }

    public static void setPagesViewed(final MeasurementMap map, int pagesViewed, int totalPages) {
        String progress = pagesViewed + "_" + totalPages;
        map.setEvar(Evars.PROGRESS_THRESHOLD.getVariable(), progress);
    }

    public static void setAvType(final MeasurementMap map, String avType) {
        map.setEvar(Evars.AV_TYPE.getVariable(), avType);
    }

    public static void setAvTags(final MeasurementMap map, String avTags) {
        map.setEvar(Evars.AV_TAGS.getVariable(), avTags);
    }

    public static void setAvPlayerType(final MeasurementMap map, String avPlayerType) {
        map.setEvar(Evars.AV_PLAYER_TYPE.getVariable(), avPlayerType);
    }

    public static void setVideoStartId(final MeasurementMap map, String videoStartId) {
        map.setEvar(Evars.VIDEO_START_ID.getVariable(), videoStartId);
    }

    public static void setGAMCreativeId(final MeasurementMap map, String gamCreativeId) {
        map.setEvar(Evars.GAM_CREATIVE_ID.getVariable(), gamCreativeId);
    }

    public static void setGAMLineItemid(final MeasurementMap map, String gamLineItemId) {
        map.setEvar(Evars.GAM_LINE_ID.getVariable(), gamLineItemId);
    }

    public static void setAppSection(final MeasurementMap map, String appSection) {
        map.setEvar(Evars.APP_SECTION.getVariable(), appSection);
    }

    public static void updateTabName(String newTabName) {
        tabName = getTrackingTabName(newTabName);
    }

    public static String getTabName() {
        return tabName;
    }

    public static void setTabName(final MeasurementMap map, String tabName) {
        Measurement.tabName = tabName;
        final String trackingTabName = getTrackingTabName(tabName);
        map.setEvar(Evars.TAB_NAME.getVariable(), trackingTabName);
    }

    private static String getPushStatus() {
        boolean isSysNoteEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        String status = "pn_status:";

        if (isSysNoteEnabled) {
            status += "on";
        } else {
            status += "off";
        }

        return status;
    }

    private static String getPushList() {
        List<AlertTopicInfo> list = FlagshipApplication.getInstance().getAlertsSettings().getAlertsTopicsList();
        StringBuilder listString = new StringBuilder();
        listString.append("pn_list:");
        int count = 0;

        for (AlertTopicInfo val : list) {
            if (!val.isEnabled())
                continue;

            if (count > 0) {
                listString.append("|");
            }
            listString.append(val.component1().getAlias());
            count++;
        }

        if (count == 0) {
            listString.append("none");
        }

        return listString.toString();
    }

    private static String getCCPAOptInInfo() {
        StringBuilder ccpaOptOutValue = new StringBuilder("ccpa:");
        final String optOutValue = "off";
        final String optInValue = "on";
        if (PaywallService.getInstance() != null) {
            ccpaOptOutValue.append(CCPAUtils.isCCPAOptedOut() ? optOutValue : optInValue);
        } else if (context != null) {
            ccpaOptOutValue.append(CCPAUtils.hasUserOptedOutCCPAAdsTracking(context) ? optOutValue : optInValue);
        }
        return ccpaOptOutValue.toString();
    }

    /**
     * Converts a Tab Name into its Analytics "tab_name" analogue (e.g. "for you" -> "for_you")
     */
    private static String getTrackingTabName(String tabName) {
        if (TextUtils.isEmpty(tabName)) return null;
        if ("print".equals(tabName)) {
            return TRACKING_TAB_NAMES.EPAPER.trackingTabName;
        } else {
            return UtilsKt.INSTANCE.toAnalyticsSnakeCase(tabName);
        }
    }

    /**
     * Converts a Section or Tab name into an Analytics Page Name.
     * Tab Name: "for you" -> "front - for-you"
     * Section Display Name: "For You" -> "front - for-you"
     */
    public static String getTrackingPageName(String pageName, String tabName) {
        if (TextUtils.isEmpty(pageName)) return null;

        if (isBioPage(pageName)) {
            return pageName;
        }
        switch (pageName) {
            case "READING_HISTORY":
                return PAGE_FRONT_MY_POST_READING_HISTORY;
            case "PURCHASE":
                return PAGE_FRONT_MY_POST_PURCHASE;
            case "ALL":
                return PAGE_FRONT_MY_POST_ALL;
            default:
                return PAGE_FRONT_PREFIX + pageName.toLowerCase(Locale.US).replaceAll(" ", "-");
        }
    }

    public static String getTrackingPageName(String pageName) {
        return getTrackingPageName(pageName, null);
    }

    public static void setPaywallArticle(String articleTitle, String arcId) {
        paywallArticle = articleTitle;
        paywallArcId = arcId;
    }

    public static String getPaywallArticle() {
        return paywallArticle != null ? paywallArticle : "";
    }

    public static String getPaywallArcId() {
        return paywallArcId;
    }

    public static void setMeterCount(final MeasurementMap map) {
        PaywallService paywallService = PaywallService.getInstance();
        String meterCount = paywallService == null ? "0" :
                paywallService.getCurrentArticleCount() % 1 == 0 ? String.format("%.0f", paywallService.getCurrentArticleCount()) : String.valueOf(paywallService.getCurrentArticleCount());
        String meterCountRule1 = paywallService == null ? "0" : Integer.toString(paywallService.getCurrentArticleCountForRule1());
        String meterCountRule2 = paywallService == null ? "0" : Integer.toString(paywallService.getCurrentArticleCountForRule2());

        Object arcId = map.getEvar(Evars.ARC_ID.getVariable());
        if (!TextUtils.isEmpty(wpmmArticleContentId)
                && arcId instanceof String
                && wpmmArticleContentId.contains(arcId.toString())
        ) {
            meterCount = "not-metered";
        }
        map.setEvar(Evars.METER_COUNT.getVariable(), meterCount);
        map.setEvar(Evars.METER_COUNT_1.getVariable(), meterCount);
        map.setEvar(Evars.METER_COUNT_RULE1.getVariable(), "politics-opinions=" + meterCountRule1);
        map.setEvar(Evars.METER_COUNT_RULE2.getVariable(), "rolling-meter=" + meterCountRule2);
    }

    public static void setMeterReason(final MeasurementMap map) {
        PaywallService paywallService = PaywallService.getInstance();
        String meterState = paywallService == null ? "0" : Integer.toString(paywallService.getCurrentArticleMeterReason());
        map.setEvar(Evars.METER_REASON.getVariable(), meterState);
    }

    public static void setLoginSubscriptionStatus(final MeasurementMap map) {
        String loginStatus = "logged-out";
        String subscriptionStatus = "0";
        final PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            loginStatus = "logged-in";
            map.setEvar(Evars.IDENTITY_UUID.getVariable(), paywallService.getLoggedInUser().getUuid());
        }
        if (paywallService != null && paywallService.isPremiumUser() && !paywallService.isWpUserLoggedIn()) {
            loginStatus = "logged-iap";
        }
        if (paywallService != null) {
            if (paywallService.isSubscriptionPaused()) {
                subscriptionStatus = "3";
            } else if (paywallService.isSubscriptionPauseScheduled()) {
                subscriptionStatus = "2";
            } else if (paywallService.isPremiumUser()) {
                subscriptionStatus = "1";
            }
        }
        map.setEvar(Evars.USER_LOGIN_STATUS.getVariable(), loginStatus);
        map.setEvar(Evars.USER_SUBSCRIBER_STATUS.getVariable(), subscriptionStatus);
        setSubAccountAnalytics(map);

        // Set User Property of Login Status and Subscription Status
        userPropertyMap.setEvar(Evars.USER_LOGIN_STATUS.getVariable(), loginStatus);
        userPropertyMap.setEvar(Evars.USER_SUBSCRIBER_STATUS.getVariable(), subscriptionStatus);
    }

    private static void setPaywallSource(MeasurementMap map, String source) {
        if (TextUtils.isEmpty(source)) {
            return;
        }
        map.setEvar(Evars.PAYWALL_SOURCE.getVariable(), source);
    }

    private static void setPriceFlag(MeasurementMap map, String priceFlag) {
        if (TextUtils.isEmpty(priceFlag)) {
            return;
        }
        map.setEvar(Evars.SUB_PRICE_FLAG.getVariable(), priceFlag);
    }

    private static void setSubAccountAnalytics(MeasurementMap map) {
        String subAccountAnalytics = null;
        if (PaywallService.getInstance() != null) {
            subAccountAnalytics = PaywallService.getConnector().getSubAccountAnalytics();
        }
        map.setEvar(Evars.ACTMGMT_ARRAY.getVariable(), TextUtils.isEmpty(subAccountAnalytics) ? "isub:0" : subAccountAnalytics);
        map.setEvar(Evars.SUBSCRIBER_ATTRIBUTES_ARRAY.getVariable(), TextUtils.isEmpty(subAccountAnalytics) ? "isub:0" : subAccountAnalytics);
    }

    public static String getSubAcctMgmtFromPrefs(Context context) {
        String subAcctMgmt = null;
        if (context != null) {
            subAcctMgmt = context.getSharedPreferences(PREFS_NAME_APP_MEASUREMENT_CACHE,
                    Context.MODE_PRIVATE).getString(PREF_SUB_ACCT_MGMT, null);
        }
        return subAcctMgmt;
    }

    public static void saveSubAcctMgmtIntoPrefs(Context context, String subAcctMgmt) {
        if (context == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME_APP_MEASUREMENT_CACHE, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_SUB_ACCT_MGMT, subAcctMgmt);
        editor.apply();
    }

    public static void setNightModeStatus(final MeasurementMap map) {
        String value = "";

        String isLowDataModeState = "";
        if (getIsLowDataMode()) {
            isLowDataModeState = ";lite:on";
        } else {
            isLowDataModeState = ";lite:off";
        }

        //TODO: break this dependency
        if (FlagshipApplication.getInstance().getNightModeManager().getImmediateNightModeStatus()) {
            value = "night mode";
        } else {
            value = "day mode";
        }
        map.setEvar(Evars.NIGHT_MODE.getVariable(), value + isLowDataModeState);
    }

    public static void trackGalleryImage(TrackingInfo trackingInfo, int imageIndex) {
        MeasurementMap map = getNewMap();
        map.setEvar(Evars.GALLERY_IMAGE_INDEX.getVariable(), String.format("%s-%03d", GALLERY_IMAGE_PREFIX, imageIndex));
        setPageName(map, trackingInfo.getPageName());
        setChannel(map, trackingInfo.getChannel());
        setContentType(map, trackingInfo.getContentType());
        setContentSource(map, trackingInfo.getContentSource());
        trackEvent(map, Events.EVENT_GALLERY_IMAGE_VIEWED);
    }

    public static void trackArticleScrollEvent(TrackingInfo trackingInfo, String tabName, Events event, PushArticleTrackingHelperData pushArticleTrackingHelperData) {
        MeasurementMap map = getNewMap();
        fillMapWithTrackingInfo(map, trackingInfo, tabName, "", pushArticleTrackingHelperData);
        trackEvent(map, event, trackingInfo);
    }

    public static void trackWithTrackingInfo(TrackingInfo trackingInfo, int position, String tabName, String appSection) {
        ARTICLE_POSITION = position;
        trackWithTrackingInfo_(getNewMap(), trackingInfo, tabName, appSection, null, null);
    }

    /**
     * FusionEventSet.Clear will clear the event set for the Fusion analytics events .
     * This method is called when the new section is loaded / switch sections tab .
     */
    public static void trackWithTrackingInfo(TrackingInfo trackingInfo, String tabName, String appSection) {
        trackWithTrackingInfo_(getNewMap(), trackingInfo, tabName, appSection, null, null);
    }

    public static void trackWithTrackingInfo(TrackingInfo trackingInfo, int position, String tabName, String appSection, PushArticleTrackingHelperData pushArticleTrackingHelperData, MeasurementMap measurementMap) {
        ARTICLE_POSITION = position;
        trackWithTrackingInfo_(getNewMap(), trackingInfo, tabName, appSection, pushArticleTrackingHelperData, measurementMap);
    }

    public static void trackWithTrackingInfo_(final MeasurementMap map, TrackingInfo trackingInfo, String tabName, String appSection, PushArticleTrackingHelperData pushArticleTrackingHelperData, MeasurementMap measurementMap) {
        fillMapWithTrackingInfo(map, trackingInfo, tabName, appSection, pushArticleTrackingHelperData);
        if (measurementMap != null) {
            map.putAll(measurementMap);
        }
        if (isWidgetOriginated) {
            String navigationBehavior = getPathToViewOf(widgetType);
            map.setEvar(Evars.APP_LAUNCH_SOURCE.getVariable(), navigationBehavior);
            setNavigationBehavior(map, navigationBehavior);
            isWidgetOriginated = false;
            widgetType = null;
        }

        if (isAudioCarouselOriginated) {
            isAudioCarouselOriginated = false;
            map.putAll(audioCarouselMap);
            clearProgressThreshold(map);
            audioCarouselMap = new MeasurementMap();
        }

        if (isSearchOriginated) {
            isSearchOriginated = false;
            map.putAll(searchTrackingMap);
            map.setEvar(Evars.PAGE_NAME.getVariable(), trackingInfo.getPageName());
            setSignInMedium(searchTrackingMap);
            if (measurementMap != null && !TextUtils.isEmpty((String) measurementMap.get(Evars.NAVIGATION_BEHAVIOR.getVariable()))) {
                setNavigationBehavior(map, (String) measurementMap.get(Evars.NAVIGATION_BEHAVIOR.getVariable()));
            }

            searchTrackingMap = new MeasurementMap();
        }

        if (!oneLinkNavigationBehavior.isEmpty()) {
            setNavigationBehavior(map, oneLinkNavigationBehavior);
            oneLinkNavigationBehavior = "";
        }

        if (postAnswersCarouselInfo != null) {
            /* Not an ideal approach, but there's not really a good way for a standard pageview
               to know if it originated from a Post Answer carousel */
            @Nullable String queryId = postAnswersCarouselInfo.getQueryId();
            if (queryId != null && queryId.equals("atp_article")) {
                setNavigationBehavior(map, "atp_inline");
            } else {
                setNavigationBehavior(map, AI_OVERVIEW_CAROUSEL_NAV_BEHAVIOR_PREFIX + postAnswersCarouselInfo.getPosition());
                setSearchKeywords(map, !TextUtils.isEmpty(postAnswersCarouselInfo.getQueryId()) ? postAnswersCarouselInfo.getQueryId() : postAnswersCarouselInfo.getQuery());
            }
            postAnswersCarouselInfo = null;
        }

        if (trackingInfo.getTetroAction() != null) {
            map.setEvar(Evars.TETRO_ACTION.getVariable(), trackingInfo.getTetroAction());

        }
        if (trackingInfo.getActionCode() != null) {
            map.setEvar(Evars.TETRO_ACTION_CODE.getVariable(), trackingInfo.getActionCode());
        }

        if (askThePostOrigination != null) {
            setNavigationBehavior(map, ASK_THE_POST_PREFIX + askThePostOrigination);
        }

        if (isAutoOriginated && isTopRibbonOriginated) {
            setNavigationBehavior(map, autoTopRibbonNavigationBehavior);
        } else if (isAutoOriginated) {
            setNavigationBehavior(map, autoNavigationBehavior);
        }

        trackEvent(map, Events.EVENT_PAGE_VIEW, trackingInfo);
    }

    private static void fillMapWithTrackingInfo(MeasurementMap map, TrackingInfo trackingInfo, String tabName, String appSection) {
        fillMapWithTrackingInfo(map, trackingInfo, tabName, appSection, null);
    }

    private static void fillMapWithTrackingInfo(MeasurementMap map, TrackingInfo trackingInfo, String tabName, String appSection, PushArticleTrackingHelperData pushArticleTrackingHelperData) {
        setBlogName(map, trackingInfo.getBlogName());
        setContentAuthor(map, trackingInfo.getContentAuthor());

        // Store for downstream use
        setContentType(articleContentMap, trackingInfo.getContentType());
        setContentAuthor(articleContentMap, trackingInfo.getContentAuthor());

        //Track featured content as Articles instead of featured (MOBATAB-1405)
        if ("featured".equals(trackingInfo.getContentType())) {
            setContentType(map, CONTENT_TYPE_ARTICLE);
        } else {
            setContentType(map, trackingInfo.getContentType());
        }
        setContentSource(map, trackingInfo.getContentSource());
        setContentUrl(map, trackingInfo.getContentURL());
        setSubsection(map, trackingInfo.getContentSubsection());
        setPageFormat(map, trackingInfo.getPageFormat());
        setSearchKeywords(map, trackingInfo.getSearchKeywords());
        setCommercialNode(map, trackingInfo.getCommercialNode());
        setContentCategory(map, trackingInfo.getContentCategory());
        setHeadline(map, trackingInfo.getHeadline());
        setHierarchy(map, trackingInfo.getHierarchy());
        setPrimarySection(map, trackingInfo.getPrimarySection());
        setSubsection(map, trackingInfo.getSubSection());
        setTitle(map, trackingInfo.getTitle());
        setAudioCarouselPublishDate(map, trackingInfo.getAudioFirstPublishDate());
        //JUcid is a unique identifier for pvs and ad requests.
        //JTid is a time based identifier for section page view that will be send in ad requests as well.
        setJUcid(map, trackingInfo.getjUcid());
//        LogUtil.d("JUCID ", "from front pageView: " + trackingInfo.getjUcid());
        if (trackingInfo.getjTid() != null && trackingInfo.getjTid() != 0L) {
            setJTid(map, trackingInfo.getjTid());
//            LogUtil.d("JTID ", "from front pageView: " + trackingInfo.getjTid());
        }
        if (isPushOriginated) {
            setNavigationBehavior(map, "push");
            isPushOriginated = false;
        }
        if (trackingInfo.getPageName() != null) setPageName(map, trackingInfo.getPageName());
        setChannel(map, trackingInfo.getChannel());
        setSubSection(map, trackingInfo.getSubSection());
        setArcId(map, trackingInfo.getArcId() != null ? trackingInfo.getArcId() : trackingInfo.getContentId());
        setVideoArcId(map, trackingInfo.getArcId() != null ? trackingInfo.getArcId() : trackingInfo.getContentId());
        setAuthorId(map, trackingInfo.getAuthorId());
        setNewsroomDesk(map, trackingInfo.getNewsroomDesk());
        setNewsroomSubDesk(map, trackingInfo.getNewsroomSubdesk());
        setAppSection(map, appSection);
        setTabName(map, tabName);
        Measurement.tabName = tabName;
        setMeterCount(map);
        setMeterReason(map);
        setLoginSubscriptionStatus(map);
        setUserName(map);
        setUUID(map);
        setNightModeStatus(map);
        setSignInMedium(map);
        map.setEvar(Evars.ACQ_ENTRANCE_TYPE.getVariable(), null);
        map.setEvar(Evars.ENTRANCE_TYPE.getVariable(), null);
        setFirstPublishedDate(map, trackingInfo.getFirstPublishedDate());
        setContentTopics(map, trackingInfo.getContentTopics());
        setTrackingTags(map, trackingInfo.getTrackingTags());
        if (pushArticleTrackingHelperData != null) {
            // User opened an article from a push notification: persist the push id so it is
            // attached to every subsequent event in this session.
            // Guard against overwriting a previously-tracked push id with null/blank when the
            // helper object is present but the id is missing (e.g. when the same helper wrapper
            // is reused for non-push article opens from other sections, which was wiping the
            // tracker and causing PushID to become null in Analytics on subsequent events).
            final String incomingPushId = pushArticleTrackingHelperData.getPushId();
            if (!TextUtils.isEmpty(incomingPushId)) {
                PushIdTracker.INSTANCE.setPushId(incomingPushId);
            }
            setPushNotificationId(map, pushArticleTrackingHelperData.getPushId());
            setPushHeadline(map, pushArticleTrackingHelperData.getPushHeadline());
            setPushAction(map, "read_standard");
            setPushTopicPlatform(map, pushArticleTrackingHelperData.getPushTopicPlatform());
            setPushTimestamp(map, pushArticleTrackingHelperData.getPushSentTimestamp());
            setPushTitle(map, pushArticleTrackingHelperData.getPushTitle());
        }
    }

    public static void setPositionInStack(MeasurementMap map, int positionInStack) {
        String value = "brights_stack_#".replace("#", Integer.toString(positionInStack + 1));
        map.setEvar(Evars.BRIGHTS_POSITION.getVariable(), value);
    }

    public static void setBrightsCarouselPosition(MeasurementMap map, int positionInCarousel) {
        String value = "brights_carousel_#".replace("#", Integer.toString(positionInCarousel + 1));
        map.setEvar(Evars.BRIGHTS_POSITION.getVariable(), value);
    }

    private static void setTrackingTags(MeasurementMap map, String trackingTags) {
        if (!TextUtils.isEmpty(trackingTags)) {
            map.setEvar(Evars.TRACKING_TAGS.getVariable(), trackingTags);
        }
    }

    private static void setContentTopics(MeasurementMap map, String contentTopics) {
        if (!TextUtils.isEmpty(contentTopics)) {
            map.setEvar(Evars.CONTENT_TOPICS.getVariable(), contentTopics);
        }
    }

    private static void setSubSection(MeasurementMap map, String subsection) {
        if (TextUtils.isEmpty(subsection)) {
            return;
        }
        map.setEvar(Evars.SUB_SECTION.getVariable(), subsection);
    }

    private static void setAuthorId(MeasurementMap map, String authorId) {
        if (TextUtils.isEmpty(authorId)) {
            return;
        }
        map.setEvar(Evars.AUTHOR_ID.getVariable(), authorId);
    }

    private static void setNewsroomDesk(MeasurementMap map, String desk) {
        if (TextUtils.isEmpty(desk)) {
            return;
        }
        map.setEvar(Evars.NEWSROOM_DESK.getVariable(), desk);
    }

    private static void setNewsroomSubDesk(MeasurementMap map, String subDesk) {
        if (TextUtils.isEmpty(subDesk)) {
            return;
        }
        map.setEvar(Evars.NEWSROOM_SUB_DESK.getVariable(), subDesk);
    }

    public static void trackSuccessfulShare(String conversationId, String turnId) {
        final MeasurementMap map = getNewMap();

        String shareType;
        String avArcId;
        if (turnId != null) {
            shareType = "turn";
            avArcId = conversationId + "_" + turnId;
        } else {
            shareType = "conversation";
            avArcId = conversationId;
        }
        Measurement.setMiscellany(map, ASK_THE_POST_SHARE_COPY);
        Measurement.setGenEventDimension(map, shareType);
        Measurement.setAvArcId(map, avArcId);

        Events event = Events.EVENT_ANSWERBOT_SHARE;
        Measurement.trackEvent(map, event);
    }

    public static void trackPersoPodShare(String avName, String navBehavior, String miscellany) {
        final MeasurementMap map = getDefaultMap();

        setMiscellany(map, miscellany);
        setGenEventDimension(map, PERSO_PODCAST);
        setNavigationBehavior(map, navBehavior);
        setAvName(map, avName);

        Events event = Events.EVENT_SHARE;
        Measurement.trackEvent(map, event);
    }

    public static void trackPersoMenuOpen(String avName, String navBehavior, String miscellany) {
        final MeasurementMap map = getDefaultMap();

        setMiscellany(map, miscellany);
        setGenEventDimension(map, PERSO_PODCAST);
        setNavigationBehavior(map, navBehavior);
        setAvName(map, avName);

        Measurement.trackEvent(map, Events.EVENT_UTILITY_MENU_OPEN);
    }

    public static void trackUserDeletesLink(String stagedDeleteId) {
        final MeasurementMap map = getNewMap();

        Measurement.setMiscellany(map, ASK_THE_POST_SHARE_DELETE);
        Measurement.setAvArcId(map, stagedDeleteId);
        Events event = Events.EVENT_ANSWERBOT_SHARE;
        Measurement.trackEvent(map, event);
    }

    public static void trackShareScreenShotTaken(String conversationId, String turnId) {
        final MeasurementMap map = getNewMap();

        Measurement.setMiscellany(map, ASK_THE_POST_SCREENSHOT);
        String id = conversationId;
        if (turnId != null) {
            id = id + "-" + turnId;
        }
        Measurement.setAvArcId(map, id);
        Events event = Events.EVENT_ANSWERBOT_SHARE;
        Measurement.trackEvent(map, event);
    }


    public static void trackGoogleAppIndexing(String pageName) {
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.GOOGLE_INDEXING.getVariable(), pageName);
        trackEvents(map, Events.EVENT_GOOGLE_INDEXING.getKey());
    }

    public static void trackDeepLinkOpen(Uri uri) {
        // Process uri
        String wpisrc = uri != null ? uri.getQueryParameter("wpisrc") : null;
        String tid = uri != null ? uri.getQueryParameter("tid") : null;
        StringBuilder sb = new StringBuilder("deeplink:");
        if (!TextUtils.isEmpty(wpisrc)) {
            sb.append(wpisrc);
        } else if (!TextUtils.isEmpty(tid)) {
            sb.append(tid);
        } else {
            sb.append("noreferral");
        }
        getDefaultMap().clear();
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.APP_LAUNCH_SOURCE.getVariable(), sb.toString());
        if (uri != null) {
            map.setEvar(Evars.CONTENT_URL.getVariable(), uri.toString());
            setTrafficSourceValuesFromUri(map, uri);
        }
        trackEvents(map, Events.EVENT_DEEP_LINK_OPEN.getKey());
    }

    public static void trackAsTrackingInfo(Tracking tracking, String tabName, String appSection, Long jTid, String jUcid) {
        //TODO: The Tracking class has more info that is unused by TrackingInfo.  Worth looking at later.
        final TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setPageType(TrackingInfoPageType.FRONT);
        trackingInfo.setBlogName(tracking.getBlogName());
        trackingInfo.setChannel(tracking.getChannel());
        trackingInfo.setContentAuthor(tracking.getAuthor());
        trackingInfo.setContentSource(tracking.getSource());
        trackingInfo.setContentSubsection(tracking.getSubsection());
        trackingInfo.setContentType(tracking.getContentType());
        trackingInfo.setPageFormat(tracking.getPageType());
        if (isAutoOpen) {
            trackingInfo.setPageName(appSection);
        } else {
            trackingInfo.setPageName(tracking.getPageName());
        }
        trackingInfo.setTitle(tracking.getPageTitle());
        trackingInfo.setPagePath(tracking.getPagePath());
        trackingInfo.setPageNumber(tracking.getPageNum());
        trackingInfo.setContentId(tracking.getContentID());
        trackingInfo.setPrimarySection(tracking.getSection());
        trackingInfo.setSource(tracking.getSource());
        trackingInfo.setContentTopics(tracking.getContentTopics());
        trackingInfo.setjUcid(jUcid);
        trackingInfo.setjTid(jTid);
        trackWithTrackingInfo(trackingInfo, tabName, appSection);
    }

    public static void trackBottomTabNavigation(String tabName) {

        final MeasurementMap map = getNewMap();

        setTabName(map, tabName);
        Measurement.tabName = tabName;
        setContentType(map, "front");
        setPageName(map, getTrackingPageName(tabName));
        setMeterCount(map);
        setLoginSubscriptionStatus(map);
        setUserName(map);
        setUUID(map);
        setNightModeStatus(map);
        setSignInMedium(map);
        trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackSaveOnboardingShown() {
        MeasurementMap map = getNewMap();
        map.setEvar(Evars.MISCELLANY.getVariable(), SAVE_ONBOARDING);
        setNavigationBehavior(map, SAVE_ONBOARDING);
        setNavigationBehavior(getDefaultMap(), SAVE_ONBOARDING);
        trackEvents(map, Events.EVENT_ONBOARDING_SEEN.getKey());
        map.setEvar(Evars.MISCELLANY.getVariable(), null);
    }

    public static void trackOnboardingSeen(String miscellany) {
        trackOnboardingSeen(miscellany, null);
    }

    public static void trackOnboardingSeen(String miscellany, String pageName) {
        MeasurementMap map = getNewMap();
        map.setEvar(Evars.MISCELLANY.getVariable(), miscellany);
        setNavigationBehavior(map, miscellany);
        setNavigationBehavior(getDefaultMap(), miscellany);
        setNightModeStatus(map);
        if (pageName != null) {
            setPageName(map, pageName);
        }
        trackEvents(map, Events.EVENT_ONBOARDING_SEEN.getKey());
        map.setEvar(Evars.MISCELLANY.getVariable(), null);
    }

    public static void trackOnboardingClick(String miscellany) {
        trackOnboardingClick(miscellany, null);
    }

    public static void trackOnboardingClick(String miscellany, String pageName) {
        MeasurementMap map = getNewMap();
        map.setEvar(Evars.MISCELLANY.getVariable(), miscellany);
        setNightModeStatus(map);
        setPageName(map, pageName);
        trackEvents(map, Events.EVENT_ONBOARDING_CLICK.getKey());
        map.setEvar(Evars.MISCELLANY.getVariable(), null);
    }

    public static void trackMyPostBannerClick(String contentUrl, MyPostSection myPostSection) {
        MeasurementMap map = getMyPostMap(myPostSection);
        setNavigationBehavior(map, PATH_TO_VIEW_MY_POST_BANNER);
        setContentUrl(map, contentUrl);
        trackEvents(map, Events.EVENT_MY_POST_BANNER_CLICK.getKey());
    }

    public static void trackEvent(final MeasurementMap map, final Events eventType) {
        trackEvent(map, eventType, null);
    }

    public static void trackEvent(final MeasurementMap map, final Events eventType, @Nullable final TrackingInfo trackingInfo) {
        trackEvent(map, eventType.getKey(), trackingInfo, false);
    }

    private static void trackEvent(final MeasurementMap map, final String eventKey, @Nullable final TrackingInfo trackingInfo, boolean isRawEvent) {
        boolean portrait = UIUtil.isPortrait(FlagshipApplication.getInstance());

        setOrientation(map, portrait ? "portrait" : "landscape");
        setAppVersion(map, detectAppVersion(FlagshipApplication.getInstance()));

        String pageName = (String) map.getEvar(Evars.PAGE_NAME.getVariable());
        if (pageName == null) {
            setPageName(getDefaultMap(), PAGE_FRONT_TOP_STORIES);
        }
        String prevPageName = (String) map.getEvar(Evars.PREV_PAGE.getVariable());
        if (prevPageName != null && prevPageName.equals(PAGE_SETTINGS_ACCOUNT_BENEFITS)) {
            setNavigationBehavior(map, NAVIGATION_BEHAVIOR_BENEFIT);
        }
        if (eventKey.equals(Events.EVENT_PAGE_VIEW.getKey()) && isInAppMessageOriginated) {
            setNavigationBehavior(map, PATH_TO_VIEW_IN_APP_PROMPT);
            isInAppMessageOriginated = false;
        }
        if (eventKey.equals(Events.EVENT_PAGE_VIEW.getKey()) && habitTilesNavigationBehavior != null) {
            setNavigationBehavior(map, habitTilesNavigationBehavior);
            habitTilesNavigationBehavior = null;
        }

        setSubAccountAnalytics(map);
        setPaywallSource(map, PrefUtils.getPrefPaywallSource(context));
        setPriceFlag(map, PrefUtils.getPrefPriceFlag(context));
        if (eventKey.contains(Events.EVENT_PAGE_VIEW.getKey())) {
            trackAppState(map, eventKey, trackingInfo, isRawEvent);
        } else {
            trackEvents(map, eventKey, trackingInfo, isRawEvent);
        }
    }

    private static void trackAppState(final MeasurementMap map, final String appState) {
        trackAppState(map, appState, null, false);
    }

    private static void trackAppState(final MeasurementMap map, final String appState, @Nullable final TrackingInfo trackingInfo, boolean isRawEvent) {
        if (BuildConfig.DEBUG) {
            StringBuilder builder = new StringBuilder();
            builder.append("trackAppState =>> ").append(appState);
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof String && !((String) value).isEmpty()) {
                    builder.append("\n ").append(key).append(" =>> ").append(value);
                }
            }
            builder.append("\n\n");
            Logger.d(TAG, builder.toString());
        }
        if (firebaseTrackingManager != null) {
            firebaseTrackingManager.trackState(appState, map, isRawEvent);
            firebaseTrackingManager.setUserProperties(userPropertyMap);
        }
        if (isPermutiveSdkInitialized()) {
            permutiveProvider.trackState(appState, map, trackingInfo);
        }
        previousMap = map;
    }

    public static void trackEvents(final MeasurementMap map, final String key) {
        trackEvents(map, key, null, false);
    }

    public static void trackEvents(
            final MeasurementMap map,
            final String key,
            @Nullable final TrackingInfo trackingInfo,
            final boolean isRawEvent
    ) {
        if (BuildConfig.DEBUG) {
            StringBuilder builder = new StringBuilder();
            builder.append("trackEvent =>> ").append(key);
            for (String mapKey : map.keySet()) {
                Object value = map.get(mapKey);
                if (value instanceof String && !((String) value).isEmpty()) {
                    builder.append("\n ").append(mapKey).append(" =>> ").append(value);
                }
            }
            builder.append("\n\n");
            Logger.d(TAG, builder.toString());
        }
        if (firebaseTrackingManager != null) {
            firebaseTrackingManager.trackAction(key, map, isRawEvent);
            firebaseTrackingManager.setUserProperties(userPropertyMap);
        }
        if (isPermutiveSdkInitialized()) {
            permutiveProvider.trackEvent(key, map, trackingInfo);
        }
    }

    public static void trackExternalLink(String externalLink) {
        final MeasurementMap map = getNewMap();
        setExternalLink(map, externalLink);
        trackEvents(map, Events.EVENT_EXTERNAL_LINK.getKey());
    }

    public static void trackLiveImageToggle(String liveImage) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, liveImage);
        trackEvents(map, Events.EVENT_MENU.getKey());
    }

    public static void trackScoreboardsGameDetailsClick(String scoreboard) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, scoreboard);
        trackEvents(map, Events.EVENT_MENU.getKey());
    }

    public static void trackSaveOnboardingContinue(boolean isSkipped) {
        final MeasurementMap map = getNewMap();
        if (isSkipped) {
            setMiscellany(map, SAVE_ONBOARDING_SKIPPED);
        } else {
            setMiscellany(map, SAVE_ONBOARDING_CONTINUE);
        }
        map.setEvar(Evars.BANNERS.getVariable(), SAVE_ONBOARDING);
        trackEvents(map, Events.EVENT_MENU.getKey());
    }

    public static void trackAppLaunch() {
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.APP_LAUNCH_SOURCE.getVariable(), "app_icon");
        setMeterCount(map);
        setLoginSubscriptionStatus(map);
        setUserName(map);
        setUUID(map);
        setNightModeStatus(map);
        setPageName(map, prevPageName != null ? prevPageName : (getNewMap().getEvar(EVAR_CHANNEL) != null ? PAGE_FRONT_PREFIX + getNewMap().getEvar(EVAR_CHANNEL) : PAGE_FRONT_TOP_STORIES));
        setSignInMedium(map);
        trackEvent(map, Events.EVENT_APP_LAUNCH);
    }

    public static void trackFeatureOnboardingDismiss(int pagesViewed, int totalPages, String navigationBehavior, String engagedTime) {
        MeasurementMap map = getNewMap();
        setPagesViewed(map, pagesViewed, totalPages);
        setNavigationBehavior(map, navigationBehavior);
        setEngagedTime(map, engagedTime);
        trackEvents(map, Events.EVENT_FEATURE_ONBOARDING_DISMISS.getKey());
    }

    public static void trackSaveUnsave(
            String pageName,
            String arcId,
            String contentType,
            String tabName,
            boolean isActionButton,
            boolean isSaving,
            boolean isMapMenu,
            String contentUrl
    ) {
        MeasurementMap map = getNewMap();

        if (pageName != null && !pageName.isEmpty()) {
            setPageName(map, pageName);
        } else {
            setPageName(map, prevPageName);
        }

        if (arcId != null && !arcId.isEmpty()) {
            setArcId(map, arcId);
        }

        if (contentUrl != null && !contentUrl.isEmpty()) {
            setContentUrl(map, contentUrl);
        }

        if (contentType != null && !contentType.isEmpty()) {
            setContentType(map, contentType);
        }

        if (tabName != null && !tabName.isEmpty()) {
            setTabName(map, tabName);
            Measurement.tabName = tabName;
        }

        if (isActionButton) {
            setGenEventDimension(map, GEN_DIMEN_ACTION_BUTTON);
        }

        if (isMapMenu) {
            setGenEventDimension(map, MAP_MENU);
        }

        setLoginSubscriptionStatus(map);
        String eventKey = isSaving ? Events.EVENT_SAVE_ARTICLE.getKey() : Events.EVENT_REMOVE_SAVED_ARTICLE.getKey();
        trackEvents(map, eventKey);
    }

    public static void trackShare(String sharedUrl, String socialName, String title, boolean isPushOriginated, String arcId, String appSection, boolean isActionButton, boolean isVideoShare, boolean isSourceIAM) {
        final MeasurementMap map = getNewMap();
        if (isVideoShare) {
            setGenEventDimension(map, "video");
        }
        setPageName(map, title);
        setSocialShare(map, sharedUrl);
        setSocialNetwork(map, socialName);
        setArcId(map, arcId);
        setAppSection(map, appSection);
        if (isPushOriginated) {
            setPushAction(map, "share_expanded");
        }
        if (isActionButton)
            setGenEventDimension(map, GEN_DIMEN_ACTION_BUTTON);
        if (isSourceIAM)
            setNavigationBehavior(map, PATH_TO_VIEW_IN_APP_PROMPT);
        trackEvents(map, Events.EVENT_SHARE.getKey());
    }

    public static void trackVerticalVideoShare(String sharedUrl, String socialName, String title, boolean isPushOriginated, String socialShareName) {
        final MeasurementMap map = videoShareMap;
        setPageName(map, title);
        setSocialShare(map, sharedUrl);
        setSocialNetwork(map, socialName);
        setGenEventDimension(map, "video");
        setMiscellany(map, "video_social_share_" + socialShareName);
        if (isPushOriginated) {
            setPushAction(map, "share_expanded");
        }
        trackEvents(map, Events.EVENT_INTERACTION.getKey());
        videoShareMap = new MeasurementMap();
    }

    public static void trackGiftSendClicked(@Nullable String contentUrl,
                                            @Nullable OmnitureX trackingInfo,
                                            String details) {
        trackGiftSendClicked(contentUrl, trackingInfo, details, null, false);
    }

    public static void trackGiftSendClicked(@Nullable String contentUrl,
                                            @Nullable OmnitureX trackingInfo,
                                            String details,
                                            String appSection,
                                            boolean isActionButton) {
        final MeasurementMap map = getNewMap();

        // For Native Articles TrackingInfo will be available. Not for Web type articles
        if (trackingInfo != null) {
            setPageName(map, trackingInfo.getPageName());
            setSiteSection(map, trackingInfo.getChannel());
            setSubsection(map, trackingInfo.getSubSection());
            setContentType(map, trackingInfo.getContentType());
            setContentSource(map, trackingInfo.getContentSource());
            setContentAuthor(map, trackingInfo.getContentAuthor());
            setAuthorId(map, trackingInfo.getAuthorId());
            setNewsroomDesk(map, trackingInfo.getNewsroomDesk());
            setNewsroomSubDesk(map, trackingInfo.getNewsroomSubdesk());
            setArcId(map, trackingInfo.getArcId() != null ? trackingInfo.getArcId() : trackingInfo.getContentId());
            setTrackingTags(map, trackingInfo.getTrackingTags());
        } else {
            // These are being set directly because associated function perfomrs null check on value
            map.setEvar(Evars.PAGE_NAME.getVariable(), null);
            map.setEvar(Evars.SUB_SECTION.getVariable(), null);
            map.setEvar(Evars.ACQ_ENTRANCE_TYPE.getVariable(), null);
            map.setEvar(Evars.ENTRANCE_TYPE.getVariable(), null);

            // These functions don't perform a null check and can be used to set map item to null
            setArcId(map, null);
            setSiteSection(map, null);
            setPaywallArticle(null, null);
        }

        if (contentUrl != null) {
            setContentUrl(map, contentUrl);
        }
        setLoginSubscriptionStatus(map);
        setSignInMedium(map);
        setDetails(map, details);
        setMiscellany(map, details);
        if (isActionButton) setGenEventDimension(map, GEN_DIMEN_ACTION_BUTTON);
        setAppSection(map, appSection);
        trackEvents(map, Events.EVENT_GIFT_SEND.getKey());
    }

    private static void setDetails(final MeasurementMap map, String details) {
        map.setEvar(Evars.DETAILS.getVariable(), details);
    }

    private static void setSocialNetwork(final MeasurementMap map, String socialName) {
        map.setEvar(Evars.SOCIAL_NETWORK.getVariable(), socialName);
    }

    public static void trackSearchResults(String queryId, String queryStr, String type, String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        isSearchOriginated = true;
        setLoginSubscriptionStatus(map);
        setSignInMedium(map);
        Measurement.setPageName(map, Measurement.PAGE_SEARCH_RESULTS);
        Measurement.setSearchNavigationBehavior(map, TextUtils.isEmpty(navigationBehavior) ? type : navigationBehavior);
        Measurement.setSearchKeywords(map, TextUtils.isEmpty(queryId) ? queryStr : queryId);
        searchTrackingMap.putAll(map);
        setPreviousPageName(searchTrackingMap);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackMainSearchPage() {
        final MeasurementMap map = getNewMap();
        isSearchOriginated = true;
        setLoginSubscriptionStatus(map);
        setSignInMedium(map);
        setPreviousPageName(map);
        Measurement.setSearchNavigationBehavior(map, Measurement.PAGE_SEARCH);
        Measurement.setPageName(map, Measurement.PAGE_SEARCH_MAIN);
        searchTrackingMap.putAll(map);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackBackToSearchResults(QueryFilter queryFilter, String originalNavBehavior) {
        final MeasurementMap map = getNewMap();
        isSearchOriginated = true;
        setLoginSubscriptionStatus(map);
        setSignInMedium(map);
        setPreviousPageName(map);
        setSearchKeywords(map, !TextUtils.isEmpty(queryFilter.getQueryId()) ? queryFilter.getQueryId() : queryFilter.getQuery());
        Measurement.setSearchNavigationBehavior(map, Measurement.PATH_TO_VIEW_BACK_TO_FRONT);
        Measurement.setPageName(map, Measurement.PAGE_SEARCH_RESULTS);
        searchTrackingMap.putAll(map);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
        setNavigationBehaviorToSearchMap(originalNavBehavior);
    }

    public static void trackBackToSectionFromSearch(String pageName) {
        final MeasurementMap map = getNewMap();
        isSearchOriginated = true;
        setLoginSubscriptionStatus(map);
        setSignInMedium(map);
        setPreviousPageName(map);
        setPageName(map, pageName);
        Measurement.setSearchNavigationBehavior(map, Measurement.PATH_TO_VIEW_BACK_TO_FRONT);
        searchTrackingMap.putAll(map);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackResultsFilterClosed(QueryFilter queryFilter) {
        final MeasurementMap map = getNewMap();
        isSearchOriginated = true;
        setLoginSubscriptionStatus(map);
        setSignInMedium(map);
        setPageName(map, Measurement.PAGE_SEARCH_RESULTS);
        Measurement.setSearchKeywords(map, queryFilter.getQuery());
        setPreviousPageName(map);
        setMiscellany(map, Measurement.PAGE_SEARCH_FILTER);
        Measurement.trackEvent(map, Events.EVENT_MENU);
    }

    public static void trackVideoAdStart(String videoName, String pageName, String videoSection, String videoSource, String contentId, String videoCategory, int progressThreshold,String gamCreativeId, String gamLineItemId, String avMetaData) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setGAMCreativeId(map, gamCreativeId);
        setGAMLineItemid(map, gamLineItemId);
        setAvMetaData(map, avMetaData);
        setProgressThreshold(map, progressThreshold);
        if(pageName != null && pageName.equals(PAGE_WATCH_VIDEO)){
            setAvType(map, "vertical");
            setAvPlayerType(map, VideoTracker2.AV_PLAYER_TYPE_WATCH);
        }
        trackEvents(map, Events.EVENT_VIDEO_AD_START.getKey());
    }

    public static void trackVideoStart(final MeasurementMap map, String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, String avType) {
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setEventLabel(map, eventLabel);
        setAvExp(map, avExp);
        setArcId(map, arcId);
        setVideoStartId(map, videoStartId);
        if (tabName != null) {
            setTabName(map, tabName);
        } else {
            setTabName(map, BottomTab.Home.getTrackingName());
        }
        if (avType != null) {
            setAvType(map, avType);
        } else {
            setAvType(map, "vertical");
        }
        if (avPlayerType != null) {
            setAvPlayerType(map, avPlayerType);
        } else {
            setAvPlayerType(map, "vertical_carousel");
        }
        trackEvents(map, Events.EVENT_VIDEO_START.getKey());
        setAvName(map, null);
    }

    public static void trackVideoAutoplay(final MeasurementMap map, String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, String avType) {
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setAvArcId(map, contentId);
        setEventLabel(map, eventLabel);
        setAvExp(map, avExp);
        if (avPlayerType != null) {
            setAvPlayerType(map, avPlayerType);
        } else {
            setAvPlayerType(map, "vertical_carousel");
        }
        setArcId(map, arcId);
        setAvType(map, avType);
        setProgressThreshold(map, progressThreshold);
        setVideoStartId(map, videoStartId);
        trackEvents(map, Events.EVENT_VIDEO_AUTOPLAY.getKey());
        setAvName(map, null);
    }

    public static void trackVerticalVideoStart(final MeasurementMap map, String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String gamCreativeId, String gamLineItemId, String arcId, String avExp, String videoStartId, @Nullable String avPlayerType, @Nullable String tabName) {
        trackVerticalVideoEvents(videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, "", gamCreativeId, gamLineItemId, Events.EVENT_VIDEO_START, arcId, avExp, videoStartId, null, avPlayerType, tabName);
        setAvName(map, null);
    }

    public static void trackVerticalVideoAutoplay(final MeasurementMap map, String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String miscellany, String arcId, String videoStartId, @Nullable String tabName) {
        trackVerticalVideoEvents(videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, miscellany, null, null, Events.EVENT_VIDEO_AUTOPLAY, arcId, "", videoStartId, null, null, tabName);
        setAvName(map, null);
    }

    public static void trackVideoComplete(final String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, @Nullable String avType) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setEventLabel(map, eventLabel);
        setAvExp(map, avExp);
        if (avType != null) {
            setAvType(map, avType);
        } else {
            setAvType(map, "vertical");
        }
        if (avPlayerType != null) {
            setAvPlayerType(map, avPlayerType);
        } else {
            setAvPlayerType(map, "vertical_carousel");
        }
        if (tabName != null) {
            setTabName(map, tabName);
        } else {
            setTabName(map, BottomTab.Home.getTrackingName());
        }
        setArcId(map, arcId);
        setVideoStartId(map, videoStartId);
        trackEvents(map, Events.EVENT_VIDEO_COMPLETE.getKey());
    }

    public static void trackVideoInteraction(final String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String eventLabel, String avExp, String avPlayerType, String arcId, String miscellany) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setAvPlayerType(map, avPlayerType);
        setAvExp(map, avExp);
        setArcId(map, arcId);
        setEventLabel(map, eventLabel);
        setMiscellany(map, miscellany);
        trackEvents(map, Events.EVENT_INTERACTION.getKey());
    }

    public static void trackVerticalVideoComplete(final String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String gamCreativeId, String gamLineItemId, String arcId, String avExp, String videoStartId, @Nullable String avPlayerType, String tabName) {
        trackVerticalVideoEvents(videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, "", gamCreativeId, gamLineItemId, Events.EVENT_VIDEO_COMPLETE, arcId, avExp, videoStartId, null, avPlayerType, tabName);
    }

    public static void trackVerticalVideoInteraction(final String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String miscellany, String gamCreativeId, String gamLineItemId, String arcId, String avExp, @Nullable String avPlayerType, String tabName) {
        trackVerticalVideoEvents(videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, miscellany, gamCreativeId, gamLineItemId, Events.EVENT_INTERACTION, arcId, avExp, null, null, avPlayerType, tabName);
    }

    public static void trackVideoAdComplete(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId,  int progressThreshold,String gamCreativeId, String gamLineItemId, String avMetaData) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setGAMCreativeId(map, gamCreativeId);
        setGAMLineItemid(map, gamLineItemId);
        setAvMetaData(map, avMetaData);
        setProgressThreshold(map, progressThreshold);
        if(pageName != null && pageName.equals(PAGE_WATCH_VIDEO)){
            setAvType(map, "vertical");
            setAvPlayerType(map, VideoTracker2.AV_PLAYER_TYPE_WATCH);
        }
        trackEvents(map, Events.EVENT_VIDEO_AD_COMPLETE.getKey());
    }

    private static void setAvMetaData(MeasurementMap map, String avMetaData) {
        map.setEvar(Evars.AV_META_DATA.getVariable(), avMetaData);
    }

    public static void trackVideoEvents(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, Events event, @Nullable String miscellany, String eventLabel, String avExp, String avPlayerType, String arcId, @Nullable String engagedTime, String videoStartId, int progressThreshold, String avType) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setAvName(map, videoName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setEventLabel(map, eventLabel);
        setAvExp(map, avExp);
        setArcId(map, arcId);
        if (avPlayerType != null) {
            setAvPlayerType(map, avPlayerType);
        } else {
            setAvPlayerType(map, "vertical_carousel");
        }
        if (miscellany != null) {
            setMiscellany(map, miscellany);
        }
        if (engagedTime != null) {
            setEngagedTime(map, engagedTime);
        }
        if (tabName != null) {
            setTabName(map, tabName);
        } else {
            setTabName(map, BottomTab.Home.getTrackingName());
        }
        setVideoStartId(map, videoStartId);
        setProgressThreshold(map, progressThreshold);
        setAvType(map, avType);
        trackEvents(map, event.getKey());
    }

    public static void trackVerticalVideoEvents(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String miscellany, String gamCreativeId, String gamLineItemId, Events event, String arcId, String avExp, @Nullable String videoStartId, @Nullable String engagedTime, @Nullable String avPlayerType, @Nullable String tab) {
        final MeasurementMap map = getNewMap();
        setAvName(map, videoName);
        setPageName(map, pageName);
        setVideoSection(map, videoSection);
        setVideoSource(map, videoSource);
        setVideoCategory(map, videoCategory);
        setVideoArcId(map, contentId);
        setProgressThreshold(map, progressThreshold);
        setMiscellany(map, miscellany);
        setArcId(map, arcId);
        setAvType(map, "vertical");
        if (avPlayerType != null) {
            setAvPlayerType(map, avPlayerType);
        } else {
            setAvPlayerType(map, "vertical_carousel");
        }
        setAvExp(map, avExp);
        setGAMCreativeId(map, gamCreativeId);
        setGAMLineItemid(map, gamLineItemId);
        if (VideoTracker2.VIDEO_SOCIAL_SHARE_START.equals(miscellany)) {
            videoShareMap = map;
        }
        if (videoStartId != null) {
            setVideoStartId(map, videoStartId);
        }
        if (engagedTime != null) {
            setEngagedTime(map, engagedTime);
        }
        if (tabName != null) {
            setTabName(map, tabName);
        } else if (tab != null) {
            setTabName(map, tab);
        } else {
            setTabName(map, BottomTab.Home.getTrackingName());
        }
        trackEvents(map, event.getKey());
    }

    public static void trackVideoProgress(
        String videoName,
        String pageName,
        String videoSection,
        String videoSource,
        String videoCategory,
        String arcId,
        String contentId,
        int progressThreshold,
        String gamCreativeId,
        String gamLineItemId,
        String avExp,
        @Nullable String avPlayerType,
        String engagedTime,
        String videoStartId,
        boolean isVertical,
        @Nullable String tabName,
        String avType
    ) {
        String miscellany = INCREMENT_VIDEO_PROGRESS;
        if (isVertical) {
            trackVerticalVideoEvents(
                videoName,
                pageName,
                videoSection,
                videoSource,
                videoCategory,
                contentId,
                progressThreshold,
                miscellany,
                gamCreativeId,
                gamLineItemId,
                Events.EVENT_VIDEO_PROGRESS,
                arcId,
                avExp,
                videoStartId,
                engagedTime,
                avPlayerType,
                tabName
            );
        } else {
            trackVideoEvents(
                videoName,
                pageName,
                videoSection,
                videoSource,
                videoCategory,
                contentId,
                Events.EVENT_VIDEO_PROGRESS,
                miscellany,
                null,
                avExp,
                avPlayerType,
                arcId,
                engagedTime,
                videoStartId,
                progressThreshold,
                avType
            );
        }
    }

    public static void trackPIPEnter(final String videoConfig) {
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.VIDEO_GRID_TYPE.getVariable(), videoConfig);
        trackEvents(map, Events.EVENT_PIP_ENTERED.getKey());
    }

    public static void trackPIPExit(final String videoConfig) {
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.VIDEO_GRID_TYPE.getVariable(), videoConfig);
        trackEvents(map, Events.EVENT_PIP_EXIT.getKey());
    }

    public static String detectAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String detectAppName() {
        if ("playstore".equals(BuildConfig.STORE_TYPE)) { // Ignore warning. BuildConfig.STORE_TYPE changes depending on product flavor
            return CLASSIC_GOOGLE;
        } else if ("amazon".equals(BuildConfig.STORE_TYPE)) {
            return CLASSIC_AMAZON;
        } else {
            return appName;
        }
    }

    public static String detectAppVersion(Context ctx) {
        if (version != null) {
            return version;
        }
        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            version = pInfo.versionName;
            return version;
        } catch (Exception e) {
            Logger.e(TAG, "Cannot read app version. " + e.getMessage());
            return "unknown";
        }
    }

    public static String detectConnectionType(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo != null) {
            switch (netInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return "wifi";
                case ConnectivityManager.TYPE_MOBILE:
                    return "cellular";
                default:
                    return "unknown";
            }
        }
        return "offline";
    }

    private static String detectOrientation(Context ctx) {
        switch (ctx.getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return "landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "portrait";
            default:
                return "unknown";
        }
    }

    public static String getUserAgent() {
        return userAgent;
    }

    public static String getPrevEntryPoint() {
        String entryPoint = prevEntryPoint;
        prevEntryPoint = null;
        return entryPoint;
    }

    public static void setPrevEntryPoint(String entryPoint) {
        prevEntryPoint = entryPoint;
    }

    private static String detectUserAgent(Context ctx) {
        return AppContextUtils.INSTANCE.getAppApiUserAgent();
    }

    private static void setSupportId(MeasurementMap map) {
        map.setEvar(Evars.SUPPORT_ID.getVariable(), DeviceUtils.getUniqueDeviceId(context));
    }

    private static void setHardwareId(MeasurementMap map) {
        if (hardwareId != null) {
            map.setEvar(Evars.HARDWARE_ID.getVariable(), hardwareId);
        }
    }

    public static void trackComicsSection(String navigation) {
        final MeasurementMap map = getNewMap();
        setNavigationBehavior(map, navigation);
        setPageName(map, "front - comics");
        setContentType(map, "front");
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackPrintEditionSection(String navigation) {
        final MeasurementMap map = getNewMap();
        setNavigationBehavior(map, navigation);
        setPageName(map, "front - " + BottomTab.Print.getRoute());
        setContentType(map, "front");
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackHoroscopesSection(String navigation) {
        final MeasurementMap map = getNewMap();
        setNavigationBehavior(map, navigation);
        setPageName(map, "front - horoscopes");
        setContentType(map, "front");
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackComics(String url, String author, String title, String pubDate) {
        final MeasurementMap map = getNewMap();
        String pageName = title != null ? title.toLowerCase() + " - " : "";
        if (pubDate != null) {
            pageName = pageName + pubDate;
        } else {
            pageName = pageName + url.hashCode();
        }
        pageName = pageName.toLowerCase();
        Measurement.setPageName(map, pageName);
        Measurement.setContentSource(map, url);
        Measurement.setContentAuthor(map, author);
        String comicFeed = "comic - " + (title != null ? title.toLowerCase() : "unknown");
        comicFeed = comicFeed.toLowerCase();
        Measurement.setComicsSubsection(map, comicFeed);
        Measurement.setChannel(map, comicFeed);
        Measurement.setContentType(map, Measurement.CONTENT_TYPE_COMICS);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackMyPost() {
        final MeasurementMap map = getNewMap();
        setNavigationBehavior(map, Measurement.NAVIGATION_BEHAVIOR_TOP_NAV);
        trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackFind() {
        final MeasurementMap map = getNewMap();
        setNavigationBehavior(map, Measurement.NAVIGATION_BEHAVIOR_TOP_NAV);
        trackEvent(map, Events.EVENT_ONPAGE_TAP);
        final MeasurementMap map1 = getFindMap();
        setNavigationBehavior(map1, Measurement.NAVIGATION_BEHAVIOR_TOP_NAV);
        trackEvent(map1, Events.EVENT_PAGE_VIEW);
    }

    /**
     * @param isInitialOpen - true sets previous page name to "front - print".
     */
    public static void trackPrint(String url, String title, String pubDateOrSerial, boolean isInitialOpen) {
        final MeasurementMap map = getNewMap();
        String pageName = title != null ? title : PAGE_PDFFULL;
        if (pubDateOrSerial != null) {
            pageName = pageName + pubDateOrSerial;
        } else if (url != null) {
            pageName = pageName + url.hashCode();
        }
        pageName = pageName.toLowerCase();
        if (isInitialOpen) {
            prevPageName = getTrackingPageName("print");
        }
        Measurement.setPageName(map, pageName);
        Measurement.setContentSource(map, url);
        Measurement.setChannel(map, "epaper");
        Measurement.setTabName(map, "print");
        Measurement.tabName = "print";
        boolean portrait = UIUtil.isPortrait(FlagshipApplication.getInstance());

        setOrientation(map, portrait ? "portrait" : "landscape");
        setAppVersion(map, detectAppVersion(FlagshipApplication.getInstance()));
        setSubAccountAnalytics(map);
        setPaywallSource(map, PrefUtils.getPrefPaywallSource(context));
        setPriceFlag(map, PrefUtils.getPrefPriceFlag(context));
        setNavigationBehavior(NavigationBehavior.EPAPER);
        setNavigationBehavior(map, NavigationBehavior.EPAPER.getValue());
        trackAppState(map, Events.EVENT_PAGE_VIEW.getKey());
    }

    public static void trackPrintDownload(String url, String title, String pubDateOrSerial) {
        final MeasurementMap map = getNewMap();
        String pageName = title != null ? title.toLowerCase() + " - " + "epaper" : "epaper";
        if (pubDateOrSerial != null) {
            pageName = pageName + pubDateOrSerial;
        } else if (url != null) {
            pageName = pageName + url.hashCode();
        }
        pageName = pageName.toLowerCase();
        Measurement.setPageName(map, pageName);
        Measurement.setContentSource(map, url);
        Measurement.setChannel(map, "epaper");

        Measurement.trackEvent(map, Events.EVENT_PRINT_DOWNLOAD);
    }

    public static void trackPrintTutorial() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, "tutorial");
        Measurement.setChannel(map, "epaper");

        Measurement.trackEvent(map, Events.EVENT_PRINT_TUTORIAL_VIEWED);
    }

    public static void trackPrintDeeplink() {
        final MeasurementMap map = getNewMap();
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        setPageName(map, PAGE_PDFPREVIEW + today);

        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static String archiveDateSectionPageNum(long archiveDate, String sectionLetter, Integer pageNumber) {
        return String.format("%s:%s%02d", archiveDate, sectionLetter == null ? "" : sectionLetter.toUpperCase(),
                pageNumber == null ? 0 : pageNumber);
    }

    public static String archiveDateSectionWithPageNumReplaced(String formattedSection, Integer pageNumber) {
        String dateAndSection = (formattedSection == null || formattedSection.length() < 3) ? "epaper:" : formattedSection.substring(0, formattedSection.length() - 2);
        return String.format("%s%02d", dateAndSection, pageNumber == null ? 0 : pageNumber);
    }

    public static void trackContactUs() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_CONTACTUS);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackHelpCenter() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_HELPCENTER);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static final EnumSet<Events> fusionEventsSent = EnumSet.noneOf(Events.class);

    public static final Map<String, EnumSet<Events>> fusionMapEventsSent = new HashMap<String, EnumSet<Events>>();

    public static void playVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String videoStartId, String avType) {
        playVideo(videoName, pageName, videoSection, videoSource, videoCategory, contentId, null, null, null, null, videoStartId, avType);
    }

    public static void playVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, String avType) {
        final MeasurementMap map = getNewMap();
        trackVideoStart(map, videoName, pageName, videoSection, videoSource, videoCategory, contentId, eventLabel, avExp, avPlayerType, arcId, videoStartId, avType);
    }

    public static void autoplayVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, String avType) {
        final MeasurementMap map = getNewMap();
        trackVideoAutoplay(map, videoName, pageName, videoSection, videoSource, videoCategory, contentId, 0, eventLabel, avExp, avPlayerType, arcId, videoStartId, avType);
    }

    public static void autoplayWatchVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, String avType) {
        final MeasurementMap map = getNewMap();
        trackVideoAutoplay(map, videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, eventLabel, avExp, avPlayerType, arcId, videoStartId, avType);
    }

    public static void playVerticalVideo(String videoName, String pageName, String videoSection, String videoSource,
                                         String videoCategory, String contentId, int progressThreshold, boolean isCarouselVideo, String miscellany,
                                         String gamCreativeId, String gamLineItemId, String arcId, String avExp, String videoStartId, @Nullable String videoPlayerType, @Nullable String tabName) {
        final MeasurementMap map = getNewMap();
        if (isCarouselVideo) {
            trackVerticalVideoAutoplay(map, videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, miscellany, arcId, videoStartId, tabName);
        } else {
            trackVerticalVideoStart(map, videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, gamCreativeId, gamLineItemId, arcId, avExp, videoStartId, videoPlayerType, tabName);
        }
    }

    public static void stopVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String videoStartId, @Nullable String avType) {
        stopVideo(videoName, pageName, videoSection, videoSource, videoCategory, contentId, null, null, null, null, videoStartId, avType);
    }

    public static void stopVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, @Nullable String avType) {
        trackVideoComplete(videoName, pageName, videoSection, videoSource, videoCategory, contentId, eventLabel, avExp, avPlayerType, arcId, videoStartId, avType);
    }

    public static void stopVerticalVideo(String videoName, String pageName, String videoSection, String videoSource, String videoCategory, String contentId, int progressThreshold, String gamCreativeId, String gamLineItemId, String arcId, String avExp, String videoStartId, @Nullable String avPlayerType, String tabName) {
        trackVerticalVideoComplete(videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, gamCreativeId, gamLineItemId, arcId, avExp, videoStartId, avPlayerType, tabName);
    }

    public static void trackReviewApp() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_APP_REVIEW);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    private static MeasurementMap setSettingsPageViewMap(String pageName) {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, pageName);
        Measurement.setChannel(map, "settings");
        Measurement.setContentType(map, "front");
        return map;
    }

    private static void trackSettingsPageView(String pageName) {
        final MeasurementMap map = setSettingsPageViewMap(pageName);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackSettingsPageViewToggleEvent(Boolean isToggleOn, String miscellany) {
        final MeasurementMap map = setSettingsPageViewMap(PAGE_SETTINGS);
        if (!miscellany.isEmpty()) {
            Measurement.setMiscellany(map, miscellany);
        }
        Events event = Events.EVENT_TOGGLE_OFF;
        if (isToggleOn) {
            event = Events.EVENT_TOGGLE_ON;
        }
        Measurement.trackEvent(map, event);
    }

    public static void trackSettingsPageView() {
        trackSettingsPageView(PAGE_SETTINGS);
    }

    public static void trackSettingsAccountPageView() {
        trackSettingsPageView(PAGE_SETTINGS_ACCOUNT);
    }

    public static void trackSettingsAccountEditEmailPageView() {
        trackSettingsPageView(PAGE_SETTINGS_ACCOUNT_EDIT_EMAIL);
    }

    public static void trackSettingsAccountEditNamePageView() {
        trackSettingsPageView(PAGE_SETTINGS_ACCOUNT_EDIT_NAME);
    }

    public static void trackSettingsAccountManageSubPageView() {
        trackSettingsPageView(PAGE_SETTINGS_ACCOUNT_MANAGE_NAME);
    }

    public static void trackSettingsAccountBenefitsPageView() {
        trackSettingsPageView(PAGE_SETTINGS_ACCOUNT_BENEFITS);
    }

    public static void trackAlertSettingsPageView(String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_ALERT_SETTINGS);
        Measurement.setChannel(map, ALERTS);
        Measurement.setContentType(map, "settings");
        Measurement.setPushType(map, PAGE_FRONT);
        Measurement.setNavigationBehavior(map, navigationBehavior);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackATPArticleClick(TrackingInfo trackingInfo, Boolean onOpen) {
        final MeasurementMap map = getNewMap();
        if (onOpen) {
            Measurement.setMiscellany(map, ATP_INLINE_MAIN_PANEL_DISPLAY);

        } else {
            Measurement.setMiscellany(map, ATP_INLINE_PANEL_DISMISS);
        }
        if (trackingInfo != null) {
            Measurement.fillMapWithTrackingInfo(map, trackingInfo, tabName, trackingInfo.getChannel());
        }
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackSubmittedQuestionClick(String type, Boolean successful, TrackingInfo trackingInfo, String query) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "submit-question");
        Measurement.setGenEventDimension(map, "suggested");
        if (trackingInfo != null) {
            Measurement.fillMapWithTrackingInfo(map, trackingInfo, tabName, trackingInfo.getChannel());
        }
        if (Objects.equals(type, "submitted")) {
            setGenEventDimension(map, "custom-question");
            setSearchKeywords(map, query);
        }
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackShowSubmitFeedback(TrackingInfo trackingInfo, Boolean submit) {
        final MeasurementMap map = getNewMap();
        if (trackingInfo != null) {
            Measurement.fillMapWithTrackingInfo(map, trackingInfo, tabName, trackingInfo.getChannel());
        }
        if (submit) {
            Measurement.setMiscellany(map, context.getString(R.string.submit_feedback_atp_inline));
        } else {
            Measurement.setMiscellany(map, context.getString(R.string.seen_feedback_atp_inline));
        }
        Measurement.trackEvent(map, Events.EVENT_ARTICLE_SUMMARY_FEEDBACK);
    }

    public static void trackAskThePostTabToggle(String tabName) {
        final MeasurementMap map = getNewMap();
        Measurement.setGenEventDimension(map, tabName);
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, ASK_THE_POST_TOGGLE);
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackAskThePostSuggestedQuestionClicked(String tabName, String type, TrackingInfo trackingInfo) {
        firedResponseTracking = false;
        final MeasurementMap map = getNewMap();
        if (type != null) {
            Measurement.setPageName(map, trackingInfo.getPageName());
            Measurement.setArcId(map, trackingInfo.getArcId());
        } else {
            Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        }
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, "submit-question");
        if (type != null) {
            Measurement.setGenEventDimension(map, "suggested-" + type + "-inline");
        } else {
            Measurement.setGenEventDimension(map, "suggested-" + tabName);
        }
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackEnteredAskQuestionEvent(String query, String type, TrackingInfo trackingInfo) {
        firedResponseTracking = false;
        final MeasurementMap map = getNewMap();
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, "submit-question");
        if (type != null) {
            Measurement.setArcId(map, trackingInfo.getArcId());
            Measurement.setGenEventDimension(map, "custom-question-inline");
            Measurement.setPageName(map, trackingInfo.getPageName());
        } else {
            Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
            Measurement.setGenEventDimension(map, "custom-question");
        }
        setSearchKeywords(map, query);

        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackFollowUpQuestion(String fullConversationId) {
        firedResponseTracking = false;
        String updatedConversationId;
        if (fullConversationId != null && fullConversationId.contains("_")) {
            String[] splitConversationId = fullConversationId.split("_");

            if (splitConversationId.length == 2) {
                String conversationId = splitConversationId[0];
                try {
                    int turnId = parseInt(splitConversationId[1]);
                    updatedConversationId = conversationId + "_" + (turnId + 1);
                } catch (NumberFormatException e) {
                    Logger.d("Measurement", "The turn ID is not a valid integer. Using original ID.");
                    updatedConversationId = fullConversationId;
                }
            } else {
                updatedConversationId = fullConversationId;
            }
        } else {
            updatedConversationId = fullConversationId;
        }

        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, "submit-followup");
        Measurement.setAvArcId(map, updatedConversationId);

        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackConversationResponse(String turnId, TrackingInfo trackingInfo) {
        final MeasurementMap map = getNewMap();
        if (trackingInfo != null) {
            Measurement.fillMapWithTrackingInfo(map, trackingInfo, tabName, trackingInfo.getChannel());
            Measurement.setPageName(map, trackingInfo.getPageName());
        } else {
            Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
            Measurement.setTabName(map, ASK_THE_POST);
        }
        Measurement.setMiscellany(map, ATP_RESPONSE);
        Measurement.setAvArcId(map, turnId);

        analytics.fireEvent(map);
    }

    public static void trackOpenedSources(String conversationId) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "sources-opened");
        Measurement.setGenEventDimension(map, conversationId);
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackViewPassage(String itemClicked) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "passage-viewed");
        Measurement.setGenEventDimension(map, itemClicked);
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);

    }

    public static void trackBackFromPassage() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setMiscellany(map, "sources-nav-back");
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackCloseSources() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setMiscellany(map, "sources-closed");
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackCopyResponse(String conversationId) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "answer-copy");
        Measurement.setGenEventDimension(map, conversationId);
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackNewChat() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setMiscellany(map, "new-chat");
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackDeleteChat(String conversationId) {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setMiscellany(map, "delete-chat");
        Measurement.setGenEventDimension(map, conversationId);
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackViewHistory(Boolean isAnonymous) {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        if (isAnonymous) {
            Measurement.setMiscellany(map, ASK_THE_POST_HISTORY);
            Measurement.setAppSection(map, ASK_THE_POST);
            Measurement.trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
        }
        Measurement.setMiscellany(map, "view-history");
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void trackHistorySavePrompt() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setAppSection(map, ASK_THE_POST);
        Measurement.setMiscellany(map, ASK_THE_POST_HISTORY_SAVE);
        Measurement.trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void trackATPProfileInteraction(String navBehavior) {
        final MeasurementMap map = getNewMap();
        setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        setMiscellany(map, navBehavior);
        trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void trackShareChatModalSeen() {
        final MeasurementMap map = getNewMap();
        Measurement.setAppSection(map, ASK_THE_POST);
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setMiscellany(map, ASK_THE_POST_HISTORY_SHARE);
        Measurement.trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void trackShareChatModalDismissed() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setAppSection(map, ASK_THE_POST);
        Measurement.setMiscellany(map, ASK_THE_POST_HISTORY_SHARE_DISMISSED);
        Measurement.trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void trackAnonymousMode(Boolean isAnonymous) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "anon-toggle");
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        if (isAnonymous) {
            Measurement.setGenEventDimension(map, "anon-on");
        } else {
            Measurement.setGenEventDimension(map, "anon-off");
        }
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);

    }

    public static void trackOpenPreviousConversation(String conversationId) {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, "thread-open");
        Measurement.setGenEventDimension(map, conversationId);
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }

    public static void setAskThePostOrigination(String tabName) {
        askThePostOrigination = tabName;
    }

    public static void resetAskThePostOrigination() {
        askThePostOrigination = null;
    }

    public static void trackAskThePostFeedBackSubmitted() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, ASK_THE_POST_FEEDBACK_SUBMITTED);
        Measurement.trackEvent(map, Events.EVENT_ARTICLE_SUMMARY_FEEDBACK);
    }

    public static void trackAskThePostFeedBackOpened() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        Measurement.setTabName(map, ASK_THE_POST);
        Measurement.setMiscellany(map, ASK_THE_POST_FEEDBACK_SEEN);
        Measurement.trackEvent(map, Events.EVENT_ARTICLE_SUMMARY_FEEDBACK);

    }

    public static void trackInlineOfferHomepageClick(String url) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, INLINE_HOMEPAGE_EXTRA_ACCOUNT);
        Measurement.setNavigationBehavior(map, INLINE_HOMEPAGE_EXTRA_ACCOUNT);
        Measurement.setContentUrl(map, url);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackInlineOfferArticleClick(String url) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, INLINE_ARTICLE_EXTRA_ACCOUNT);
        Measurement.setNavigationBehavior(map, INLINE_ARTICLE_EXTRA_ACCOUNT);
        Measurement.setContentUrl(map, url);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackForYouScroll() {
        final MeasurementMap map = getNewMap();
        Measurement.setPageName(map, "front - for-you");
        Measurement.setAppSection(map, "For You");
        Measurement.trackEvent(map, Events.EVENT_SCROLL_IMPRESSION);
    }

    public static void trackLiveUpdateNotification(Article2 article) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "luf_new_update");
        if (article.getOmniture() != null && article.getOmniture().getPageName() != null) {
            setPageName(map, article.getOmniture().getPageName());
        }
        setArcId(map, article.getArcId());
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_IMPRESSION);
    }

    public static void trackLowDataModeNotification(String pageName) {
        final MeasurementMap map = getDefaultMap();
        Measurement.setMiscellany(map, "lite_mode_snack_bar");
        Measurement.setPageName(map, pageName);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_IMPRESSION);
    }

    public static void trackLowDataModeNotificationOn(String pageName) {
        trackLowDataModeNotificationTap(pageName, "lite_mode_on");
    }

    public static void trackLowDataModeNotificationDismiss(String pageName) {
        trackLowDataModeNotificationTap(pageName, "lite_mode_dismiss");
    }

    public static void trackLowDataModeOff(String pageName) {
        trackLowDataModeNotificationTap(pageName, "lite_mode_off");
    }

    public static void trackLowDataModeNotificationTap(String pageName, String action) {
        final MeasurementMap map = getDefaultMap();
        Measurement.setMiscellany(map, action);
        Measurement.setPageName(map, pageName);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
        Measurement.setMiscellany(map, EMPTY);
    }

    public static void trackLowDataModeUnhideMedia(String pageName, String mediaType) {
        final MeasurementMap map = getDefaultMap();
        Measurement.setMiscellany(map, "lite_mode_" + mediaType);
        Measurement.setPageName(map, pageName);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackLowDataModeModalEvent(String pageName, String action) {
        final MeasurementMap map = getDefaultMap();
        Measurement.setMiscellany(map, "lite_mode_" + action);
        Measurement.setPageName(map, pageName);
        Measurement.trackEvent(map, Events.EVENT_IN_APP_PROMPT);
    }

    public static void trackLowDataModeModalSeen(String pageName) {
        trackLowDataModeModalEvent(pageName, "seen");
    }

    public static void trackLowDataModeModalAllow(String pageName) {
        trackLowDataModeModalEvent(pageName, "allow");
    }

    public static void trackLowDataModeModalSnooze(String pageName) {
        trackLowDataModeModalEvent(pageName, "soonze");
    }

    public static void trackLowDataModeModalNotAllow(String pageName) {
        trackLowDataModeModalEvent(pageName, "not_allow");
    }

    public static void trackOpenNotification(String pushId, String url, String headline, String kicker, String analyticsId, String timestamp) {
        final MeasurementMap map = getNewMap();
        Measurement.setPushNotificationId(map, pushId);
        Measurement.setPushTopicPlatform(map, analyticsId);
        Measurement.setPushTimestamp(map, timestamp);
        Measurement.setPushUrl(map, url);
        Measurement.setPushHeadline(map, headline);
        Measurement.setPushTitle(map, kicker);
        Measurement.setNavigationBehavior(map, "push");
        Measurement.setPushAction(map, "read_standard");
        Measurement.trackEvent(map, Events.EVENT_PUSH_OPEN);
    }

    public static void trackLiveUpdateTap(String lufIndex) {
        final MeasurementMap map = getNewMap();
        Measurement.setLufNavigationTap(map, lufIndex);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackLiveUpdateScroll(String lufIndex) {
        final MeasurementMap map = getNewMap();
        Measurement.setLufNavigationScroll(map, lufIndex);
        Measurement.trackEvent(map, Events.EVENT_LUF_SCROLL);
    }

    public static void trackLiveUpdateOnClick(Article2 article) {
        final MeasurementMap map = getNewMap();
        Measurement.setMiscellany(map, "luf_new_update");
        if (article.getOmniture() != null && article.getOmniture().getPageName() != null) {
            setPageName(map, article.getOmniture().getPageName());
        }
        setArcId(map, article.getArcId());
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void enablePushOrigination() {
        isPushOriginated = true;
    }

    public static void enableWidgetOrigination(String widgetType) {
        isWidgetOriginated = true;
        Measurement.widgetType = widgetType;
    }

    public static void enableAutoOrigination() {
        isAutoOriginated = true;
    }

    public static void enableIsAutoOpen() {
        isAutoOpen = true;
    }

    public static void disableIsAutoOpen() {
        isAutoOpen = false;
    }

    public static void enableTopRibbon() {
        isTopRibbonOriginated = true;
    }

    public static void trackAlertPrompt() {
        final MeasurementMap mapApp = getNewMap();
        trackEvent(mapApp, Events.EVENT_PUSH_PROMPT_SHOWN);
    }

    public static void trackPushPromptDismiss() {
        final MeasurementMap mapApp = getNewMap();
        trackEvent(mapApp, Events.PUSH_PROMPT_DISMISS);
    }

    public static void trackAlertTopicEnroll(String topic, String entryPoint, String pageName, boolean isEnroll) {
        _trackAlertTopicEnroll(topic, entryPoint, pageName, isEnroll, null, false);
    }

    public static void trackAlertTopicEnroll(String topic, String entryPoint, boolean isEnroll) {
        _trackAlertTopicEnroll(topic, entryPoint, null, isEnroll, null, false);
    }

    public static void trackAlertTopicEnroll(String topic, String entryPoint, boolean isEnroll, String contentUrl, Boolean isIAMOriginated) {
        _trackAlertTopicEnroll(topic, entryPoint, null, isEnroll, contentUrl, isIAMOriginated);
    }

    public static void _trackAlertTopicEnroll(String topic, String entryPoint, String pageName, boolean isEnroll, String contentUrl, Boolean isIAMOriginated) {
        Events trackEvent = isEnroll ? Events.EVENT_PUSH_TOPIC_ENROLL : Events.EVENT_PUSH_TOPIC_DISENROLL;
        final MeasurementMap mapApp = getNewMap();
        Measurement.setMiscellany(mapApp, EntryPoint.getTrackingString(entryPoint));
        Measurement.setPushTopicPlatform(mapApp, topic);
        if (contentUrl != null) {
            setContentUrl(mapApp, contentUrl);
        }
        if (entryPoint.equals(Measurement.PROFILE_PREFERENCE_ALERTS)) { // onboarding
            setUUID(mapApp);
            setLoginSubscriptionStatus(mapApp);
        } else {
            if (pageName != null) {
                Measurement.setPageName(mapApp, pageName);
            } else {
                Measurement.setPageName(mapApp, PAGE_ALERT_SETTINGS);
            }
            Measurement.setChannel(mapApp, ALERTS);
            if (entryPoint.equals(EntryPoint.SETTINGS.name()) || entryPoint.equals(EntryPoint.ALERTS_TAB.name())) {
                Measurement.setContentType(mapApp, "settings");
            }
        }
        if (isIAMOriginated)
            setNavigationBehavior(mapApp, PATH_TO_VIEW_IN_APP_PROMPT);
        Measurement.trackEvent(mapApp, trackEvent);
    }

    public static void trackAlertsPageView(String navigationBehavior) {
        final MeasurementMap map = getNewMap();
        String pageName = PAGE_FRONT_PREFIX + ALERTS;
        Measurement.setPageName(map, pageName);
        Measurement.setChannel(map, ALERTS);
        Measurement.setContentType(map, PAGE_FRONT);
        Measurement.setNavigationBehavior(map, navigationBehavior);
        Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackEnableNotifications() {
        final MeasurementMap map = getNewMap();
        String pageName = PAGE_FRONT_PREFIX + ALERTS;
        Measurement.setPageName(map, pageName);
        Measurement.setChannel(map, ALERTS);
        Measurement.setContentType(map, PAGE_FRONT);
        Measurement.setPushAction(map, "turn-on"); // TODO: Underscore is proper format instead of dash. Check with Analytics to see what they are expecting.
        Measurement.trackEvent(map, Events.EVENT_TOGGLE_NOTIFICATIONS);
    }

    public static void trackNewsletterEnroll(String entrypoint, String initiative, String newsletterName, boolean isEnroll) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, String.format("%s;%s;%s", entrypoint, initiative, newsletterName));
        setPageName(map, initiative);
        if (isEnroll) {
            trackEvent(map, Events.EVENT_EMAIL_ENROLL);
        } else {
            trackEvent(map, Events.EVENT_EMAIL_UNENROLL);
        }
    }

    public static void trackCustomNavPageView() {
        final MeasurementMap map = getNewMap();
        setPageName(map, PAGE_FRONT_PREFIX + PAGE_CUSTOM_NAV);
        trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackCustomNavEnroll(String sectionDisplayName, boolean isEnroll) {
        String sectionName = Measurement.CUSTOM_NAV_PREFIX + UtilsKt.INSTANCE.toAnalyticsSnakeCase(sectionDisplayName);
        Events trackEvent = isEnroll ? Events.EVENT_CUSTOM_NAV_ENROLL : Events.EVENT_CUSTOM_NAV_DISENROLL;
        final MeasurementMap map = getNewMap();
        setMiscellany(map, sectionName);
        setPageName(map, PAGE_FRONT_PREFIX + PAGE_CUSTOM_NAV);
        trackEvent(map, trackEvent);
    }

    public static void trackCustomNavReset() {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, CUSTOM_NAV_RESET);
        setPageName(map, PAGE_FRONT_PREFIX + PAGE_CUSTOM_NAV);
        trackEvents(map, Events.EVENT_MENU.getKey());
    }

    public static Events getVideoMilestoneMapping(float percentWatched) {
        if (percentWatched >= 75) {
            return Events.EVENT_VIDEO_PLAYED_75;
        } else if (percentWatched >= 50) {
            return Events.EVENT_VIDEO_PLAYED_50;
        } else if (percentWatched >= 25) {
            return Events.EVENT_VIDEO_PLAYED_25;
        } else {
            return null;
        }
    }

    public static void trackCurrentVideoPercentage(String videoName, String pageName, String videoSection, String videoSource,
                                                   String videoCategory, String contentId, final int playheadPercentage, String videoStartId, int progressThreshold, String avType) {
        trackCurrentVideoPercentage(videoName, pageName, videoSection, videoSource, videoCategory, contentId, playheadPercentage, null, null, null, null, videoStartId, progressThreshold, avType);
    }

    public static void trackCurrentVideoPercentage(String videoName, String pageName, String videoSection, String videoSource,
                                                   String videoCategory, String contentId, final int playheadPercentage,
                                                   String eventLabel, String avExp, String avPlayerType, String arcId, String videoStartId, int progressThreshold, String avType) {
        Events pctEvent = getVideoMilestoneMapping(playheadPercentage);
        if (pctEvent != null) {
            Measurement.trackVideoEvents(videoName, pageName, videoSection, videoSource, videoCategory, contentId, pctEvent, null, eventLabel, avExp, avPlayerType, arcId, null, videoStartId, progressThreshold, avType);
        }
    }

    public static void trackCurrentVerticalVideoPercentage(String videoName, String pageName, String videoSection, String videoSource,
                                                           String videoCategory, String contentId, int progressThreshold, final int playheadPercentage,
                                                           String gamCreativeId, String gamLineItemId, String arcId, String avExp, @Nullable String avPlayerType, String tabName, String videoStartId) {
        Events pctEvent = getVideoMilestoneMapping(playheadPercentage);
        if (pctEvent != null) {
            Measurement.trackVerticalVideoEvents(videoName, pageName, videoSection, videoSource, videoCategory, contentId, progressThreshold, "", gamCreativeId, gamLineItemId, pctEvent, arcId, avExp, videoStartId, null, avPlayerType, tabName);
        }
    }

    public static String getPageNameForSection(String section) {
        String sectionName = section == null ? "unknown" : section.toLowerCase();
        return Measurement.PAGE_FRONT_PREFIX + sectionName;
    }

    public static void trackSignInAttempt() {
        final MeasurementMap map = getNewMap();
        map.putAll(articleContentMap);
        trackEvents(map, Events.EVENT_CLICK_SIGNIN_ATTEMPT.getKey());
    }

    public static void trackSignInComplete(String entryPoint) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, entryPoint);
        setSignInMedium(map);
        map.putAll(articleContentMap);
        trackEvents(map, Events.EVENT_REG_SIGN_IN_SUCCESS.getKey());
    }

    public static void trackSignOutAttempt(String eventLabel) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, eventLabel);
        trackEvents(map, Events.EVENT_CLICK_SIGN_OUT_ATTEMPT.getKey());
    }

    public static void trackSignOutComplete(String eventLabel) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, eventLabel);
        trackEvents(map, Events.EVENT_REG_SIGN_OUT_SUCCESS.getKey());
    }

    public static void trackAccountCreationComplete(String entryPoint) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, entryPoint);
        trackEvents(map, Events.EVENT_REG_REGISTER_SUCCESS.getKey());
    }

    public static void resumeCollection(Activity activity) {
        if (firebaseTrackingManager != null) {
            firebaseTrackingManager.resumeCollection(activity);
        }
    }

    public static void trackSearchMenuIconClick() {
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.MISCELLANY.getVariable(), PAGE_SEARCH);
        Measurement.trackEvent(map, Events.EVENT_MENU);
    }

    public static void trackArticleLaunchFromSearch() {
        final MeasurementMap map = getNewMap();
        setPushType(map, Measurement.PAGE_SEARCH);
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), Measurement.PAGE_SEARCH);
        map.setEvar(Evars.PREV_PAGE.getVariable(), Measurement.PAGE_SEARCH);
    }

    public static void trackSectionFrontClickFromSearch() {
        final MeasurementMap map = getNewMap();
        setNavigationBehavior(map, PAGE_SEARCH);
    }

    public static void trackPodcastPlay(@NonNull String seriesSlug, @NonNull String podcastSlug, @Nullable String avName,
                                        @NonNull String date, @Nullable String appSection,
                                        boolean isFlexAudio, boolean isCarousel, Long duration, PersoPodTrackingInfo tracking, boolean isPersoPod) {
        final MeasurementMap map = getNewMap();
        if (isPersoPod) {
            String navBehavior;
            if (tracking != null) {
                navBehavior = tracking.getTouchpoint();
                map.setEvar(Evars.AV_NAME.getVariable(), String.format(Locale.US, "podcasts:%s:%s:%s",
                        "perso-" + tracking.getPodcastType(), podcastSlug.toLowerCase(), tracking.getDate()));
                setAvArcId(map, tracking.getId());
                setAvTags(map, tracking.getTags());
            } else {
                navBehavior = NAVIGATION_AUDIO_AUTO;
            }
            setNavigationBehavior(map, navBehavior);
            setAvName(map, podcastSlug);
            setAvPlayerType(map, AUDIO_PERSO_PODCAST_PLAYER_TYPE);
            setAvType(map, PERSO_PODCAST);
        } else {
            map.setEvar(
                    Evars.AV_NAME.getVariable(),
                    avName != null ? avName : String.format(Locale.US, "podcasts:%s:%s:%s", seriesSlug.toLowerCase(), date, podcastSlug.toLowerCase())
            );
            if (isFlexAudio) setAvPlayerType(map, AUDIO_FLEX_PLAYER_TYPE);
            if (isCarousel) setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
        }
        setAppSection(map, appSection);
        if (duration != null && duration > 0L) {
            setAvDuration(map, duration);
        }
        trackEvents(map, Events.EVENT_AUDIO_START.getKey());
    }

    public static void trackPodcastProgress(@NonNull String seriesSlug, @NonNull String podcastSlug, @Nullable String avName,
                                            @NonNull String date, byte percent,
                                            @Nullable String appSection, boolean isFlexAudio, boolean isCarousel, Long duration, PersoPodTrackingInfo tracking, boolean isPersoPod) {
        final MeasurementMap map = getNewMap();
        if (isPersoPod) {
            if (tracking != null) {
                setNavigationBehavior(map, tracking.getTouchpoint());
                setAvArcId(map, tracking.getId());
            }
            setAvPlayerType(map, AUDIO_PERSO_PODCAST_PLAYER_TYPE);
            setAvType(map, PERSO_PODCAST);
        } else {
            if (isFlexAudio) setAvPlayerType(map, AUDIO_FLEX_PLAYER_TYPE);
            if (isCarousel) setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
        }
        map.setEvar(Evars.PAGE_NAME.getVariable(), "topic:podcast");
        Events pctEvent = getPodcastProgressEvent(percent);
        setAppSection(map, appSection);
        if (duration != null && duration > 0L) {
            setAvDuration(map, duration);
        }
        if (pctEvent != null) {
            if (isPersoPod && tracking != null) {
                map.setEvar(Evars.AV_NAME.getVariable(), String.format(Locale.US, "podcasts:%s:%s:%s",
                        "perso-" + tracking.getPodcastType(), podcastSlug.toLowerCase(), tracking.getDate()));
            } else {
                map.setEvar(
                        Evars.AV_NAME.getVariable(),
                        avName != null ? avName : String.format(Locale.US, "podcasts:%s:%s:%s", seriesSlug.toLowerCase(), date, podcastSlug.toLowerCase())
                );
            }
            trackEvents(map, pctEvent.getKey());
        }
    }

    public static void trackPodcastProgressIncrement(@NonNull String seriesSlug, @NonNull String podcastSlug, @Nullable String avName,
                                                     @NonNull String date, int percent,
                                                     @Nullable String appSection, boolean isFlexAudio, boolean isCarousel, Long duration, PersoPodTrackingInfo tracking, boolean isPersoPod) {
        final MeasurementMap map = getNewMap();
        if (isPersoPod) {
            String navBehavior;
            if(tracking != null) {
               navBehavior = tracking.getTouchpoint();
                map.setEvar(Evars.AV_NAME.getVariable(), String.format(Locale.US, "podcasts:%s:%s:%s",
                        "perso-" + tracking.getPodcastType(), podcastSlug.toLowerCase(), tracking.getDate()));
                setAvArcId(map, tracking.getId());
                setAvTags(map, tracking.getTags());
            } else {
                navBehavior = NAVIGATION_AUDIO_AUTO;
            }
            setNavigationBehavior(map, navBehavior);
            setAvPlayerType(map, AUDIO_PERSO_PODCAST_PLAYER_TYPE);
            setAvType(map, PERSO_PODCAST);
        } else {
            if (isFlexAudio) setAvPlayerType(map, AUDIO_FLEX_PLAYER_TYPE);
            if (isCarousel) setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
            map.setEvar(
                    Evars.AV_NAME.getVariable(),
                    avName != null ? avName : String.format(Locale.US, "podcasts:%s:%s:%s", seriesSlug.toLowerCase(), date, podcastSlug.toLowerCase())
            );
        }
        map.setEvar(Evars.PAGE_NAME.getVariable(), "topic:podcast");
        setAppSection(map, appSection);
        setProgressThreshold(map, percent);

        if (duration != null && duration > 0L) {
            setAvDuration(map, duration);
        }
        trackEvents(map, Events.EVENT_AUDIO_PROGRESS.getKey());
    }

    public static void trackPodcastSubscribe(@NonNull String podcastName, @NonNull String menuAppName) {
        final MeasurementMap map = getNewMap();
        map.setEvar(Evars.MISCELLANY.getVariable(), String.format(Locale.US, "podsubscribe-%s_%s",
                podcastName.toLowerCase(), menuAppName.toLowerCase()));
        trackEvents(map, Events.EVENT_MENU.getKey());
    }

    @Nullable
    private static Events getPodcastProgressEvent(byte percentWatched) {
        if (percentWatched >= 100) {
            return Events.EVENT_AUDIO_COMPLETE;
        } else if (percentWatched >= 75) {
            return Events.EVENT_AUDIO_PLAYED_75;
        } else if (percentWatched >= 50) {
            return Events.EVENT_AUDIO_PLAYED_50;
        } else if (percentWatched >= 25) {
            return Events.EVENT_AUDIO_PLAYED_25;
        }
        return null;
    }

    public static void setWpmmArticleContentId(String contentId) {
        wpmmArticleContentId = contentId;
    }

    /**
     * Builds the value for the "test_group" Evar from the full list of AB test parameters and values coming from Firebase.
     */
    public static String getABTestingVariants(Context context) {
        StringBuilder abVariants = new StringBuilder();
        for (Map.Entry<String, String> entry : PrefUtils.getABParametersMap(context).entrySet()) {
            abVariants.append(entry.getKey()).append("|").append(entry.getValue()).append(";");
        }
        return abVariants.toString();
    }

    public static void setABTestingVariants(final MeasurementMap map) {
        StringBuilder testGroupVariants = new StringBuilder();
        StringBuilder cookiesJsVariants = new StringBuilder();

        for (Map.Entry<String, String> entry : PrefUtils.getABParametersMap(context).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String formatted = key + "|" + value + ";";

            if (COOKIES_JS_KEYS.contains(key)) {
                cookiesJsVariants.append(formatted);
            } else {
                testGroupVariants.append(formatted);
            }
        }

        String testGroupVariantStr = testGroupVariants.toString();
        map.setEvar(Evars.COOKIES_JS.getVariable(), cookiesJsVariants.toString());
        map.setEvar(Evars.AB_TESTING_VARIANT.getVariable(), testGroupVariantStr);
        abTestGroup = testGroupVariantStr;
    }


    public static void trackSpeechEvent(TrackingInfo trackingInfo, String tabName, String appSection, String navigationBehavior, String audioType, Events event, Float speed, String voice, String feed, Long duration, String avArcId, String avName, String playAd) {
        MeasurementMap map = getNewMap();
        setTabName(map, tabName);
        setAppSection(map, appSection);
        setNavigationBehavior(map, navigationBehavior);
        setAvName(map, avName);
        // Omniture object from feeds can be null.
        if (trackingInfo != null) {
            setArcId(map, trackingInfo.getArcId());
            setPageName(map, trackingInfo.getPageName());
        }
        String miscellany = audioType;
        if (navigationBehavior.equals(PATH_TO_VIEW_AUDIO_CAROUSEL) || navigationBehavior.equals(NAVIGATION_AUDIO_CAROUSEL_ROLL_THROUGH)) {
            setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
        } else if (navigationBehavior.equals(PATH_TO_VIEW_AUDIO_FLEX)) {
            setAvPlayerType(map, AUDIO_FLEX_PLAYER_TYPE);
        } else if (navigationBehavior.equals(PATH_TO_VIEW_AUDIO_ACTION_BUTTON)) {
            setAvPlayerType(map, AUDIO_ACTION_BUTTON_TYPE);
        } else if (navigationBehavior.equals(PATH_TO_VIEW_AUDIO_PLAYLIST) || navigationBehavior.equals(NAVIGATION_AUDIO_PLAYLIST_ROLL_THROUGH)) {
            setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
        } else if (navigationBehavior.equals(NAVIGATION_AUDIO_AUTO) && audioType != null && audioType.equals(AUDIO_TYPE_PODCAST)) {
            setAvPlayerType(map, AUDIO_TYPE_PODCAST);
            setAvType(map, AUDIO_TYPE_PODCAST);
            setPageName(map, PAGE_CAR_PODCAST);
        } else if (navigationBehavior.equals(PATH_TO_VIEW_AUDIO_STANDALONE)) {
            setAvPlayerType(map, AUDIO_TYPE_STANDALONE);
            setAvType(map, AUDIO_TYPE_STANDALONE);
        }
        if (speed != null) {
            String speedFormatted = new DecimalFormat("#.##").format(speed);
            miscellany += ";";
            miscellany += "speed:" + speedFormatted + "x";
        }
        if (!TextUtils.isEmpty(voice)) {
            miscellany += ";";
            miscellany += "voice:" + voice;
        }

        if (playAd != null) {
            miscellany += ";ads:" + playAd;
        }
        if (duration != null && duration > 0) {
            setAvDuration(map, duration);
        }
        setAvArcId(map, avArcId);
        setMiscellany(map, miscellany);
        setAudioFeed(map, feed);
        trackEvent(map, event);
    }

    public static void trackSpeechProgress(Events event, TrackingInfo trackingInfo, String tabName, String appSection, String navigationBehavior, String audioType, Float speed, String voice, Integer progress, Long duration, String avArcId, String avName) {
        MeasurementMap map = getNewMap();
        String miscellany = audioType;
        setTabName(map, tabName);
        setAppSection(map, appSection);
        setNavigationBehavior(map, navigationBehavior);
        switch (navigationBehavior) {
            case PATH_TO_VIEW_AUDIO_CAROUSEL:
            case NAVIGATION_AUDIO_CAROUSEL_ROLL_THROUGH:
            case PATH_TO_VIEW_AUDIO_PLAYLIST:
                setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
                break;
            case PATH_TO_VIEW_AUDIO_FLEX:
                setAvPlayerType(map, AUDIO_FLEX_PLAYER_TYPE);
                break;
            case PATH_TO_VIEW_AUDIO_ACTION_BUTTON:
                setAvPlayerType(map, AUDIO_ACTION_BUTTON_TYPE);
                break;
            case NAVIGATION_AUDIO_AUTO:
                if (audioType != null && audioType.equals(AUDIO_TYPE_PODCAST)) {
                    setPageName(map, PAGE_CAR_PODCAST);
                    setAvPlayerType(map, audioType);
                    setAvType(map, audioType);
                }
                break;
            case PATH_TO_VIEW_AUDIO_STANDALONE:
                setAvPlayerType(map, AUDIO_TYPE_STANDALONE);
                setAvType(map, AUDIO_TYPE_STANDALONE);
                break;
        }
        if (speed != null) {
            String speedFormatted = new DecimalFormat("#.##").format(speed);
            miscellany += ";";
            miscellany += "speed:" + speedFormatted + "x";
        }
        if (!TextUtils.isEmpty(voice)) {
            miscellany += ";";
            miscellany += "voice:" + voice;
        }
        if (duration != null && duration > 0) {
            setAvDuration(map, duration);
        }
        if (progress != null) {
            setProgressThreshold(map, progress);
        }
        setAvArcId(map, avArcId);
        setAvName(map, avName);
        // Omniture object from feeds can be null.
        if (trackingInfo != null) {
            setArcId(map, trackingInfo.getArcId());
            setPageName(map, trackingInfo.getPageName());
        }
        setMiscellany(map, miscellany);
        trackEvent(map, event);
    }

    public static void trackActionButtonAddToPlaylist(String appSection, String arcID) {
        MeasurementMap map = getDefaultMap();
        map.setEvar(Evars.ARC_ID.getVariable(), arcID);
        trackActionButtonEvent(Events.EVENT_ADD_TO_PLAYLIST, appSection);
    }

    public static void trackActionButtonRemoveFromPlaylist(String appSection) {
        trackActionButtonEvent(Events.EVENT_REMOVE_FROM_PLAYLIST, appSection);
    }

    public static void trackPlayListButtonClick(String appSection) {
        trackActionButtonEvent(Events.EVENT_PLAYLIST_CLICK, appSection);
    }

    public static void trackActionButtonArticleAddToPlaylist() {
        MeasurementMap map = getDefaultMap();
        setNavigationBehavior(map, "playlist_banner");
        trackActionButtonEvent(Events.EVENT_PAGE_VIEW, "");
    }

    public static void trackLaunchFromWidget(String widgetType) {
        getDefaultMap().clear();
        final MeasurementMap map = getNewMap();
        String navigationBehavior = getPathToViewOf(widgetType);
        map.setEvar(Evars.APP_LAUNCH_SOURCE.getVariable(), navigationBehavior);
        setNavigationBehavior(map, navigationBehavior);
        trackEvents(map, Events.EVENT_WIDGET.getKey());
    }

    public static void trackAuthorCardOpen() {
        final MeasurementMap map = new MeasurementMap();
        setPageName(map, FollowTrackingInfo.followTracking.pageName);
        setChannel(map, FollowTrackingInfo.followTracking.channel);
        setContentAuthor(map, FollowTrackingInfo.followTracking.contentAuthor);
        setAppSection(map, FollowTrackingInfo.followTracking.appSection);
        setTabName(map, FollowTrackingInfo.followTracking.tabName);
        setAppName(map, appName);
        setABTestingVariants(map);
        setLoginSubscriptionStatus(map);
        trackEvents(map, Events.EVENT_AUTHOR_CARD_OPEN.getKey());
    }

    public static void trackAuthorFollowOrUnfollow(boolean follow) {
        final MeasurementMap map = new MeasurementMap();
        setPageName(map, bioPageAuthorName());
        setChannel(map, FollowTrackingInfo.followTracking.channel);
        setContentAuthor(map, FollowTrackingInfo.followTracking.contentAuthor);
        setAppSection(map, FollowTrackingInfo.followTracking.appSection);
        setTabName(map, FollowTrackingInfo.followTracking.tabName);
        map.setEvar(Evars.MISCELLANY.getVariable(), FollowTrackingInfo.followTracking.miscellany);
        setAppName(map, appName);
        setABTestingVariants(map);
        setLoginSubscriptionStatus(map);
        if (!follow) {
            trackEvents(map, Events.EVENT_AUTHOR_UNFOLLOW.getKey());
        } else {
            trackEvents(map, Events.EVENT_AUTHOR_FOLLOW.getKey());
        }
    }

    static String concatAuthorName(String authorName) {
        return authorName != null ? authorName.toLowerCase().replaceAll(" ", "-") : "";
    }

    public static String bioPageAuthorName() {
        return PAGE_BIOPAGE_AUTHOR_PREFIX + concatAuthorName(FollowTrackingInfo.followTracking.contentAuthor);
    }

    public static void trackAuthorPageOpenFromAuthorCard() {
        final MeasurementMap map = new MeasurementMap();
        setPageName(map, bioPageAuthorName());
        setSiteSection(map, Measurement.CONTENT_TYPE_BIOPAGE);
        setContentType(map, Measurement.CONTENT_TYPE_BIOPAGE);
        setContentAuthor(map, FollowTrackingInfo.followTracking.contentAuthor);
        setAppSection(map, FollowTrackingInfo.followTracking.appSection);
        setTabName(map, FollowTrackingInfo.followTracking.tabName);
        setAppName(map, appName);
        setLoginSubscriptionStatus(map);
        setABTestingVariants(map);
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), PATH_TO_VIEW_AUTHOR_CARD);
        trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackReadArticleFromFollowingFeed() {
        final MeasurementMap map = getDefaultMap();
        setNavigationBehavior(map, PATH_TO_VIEW_FOLLOWING);
    }

    public static void trackNavigateToAuthorPageFromAuthorItems() {
        final MeasurementMap map = new MeasurementMap();
        setPageName(map, bioPageAuthorName());
        setSiteSection(map, Measurement.CONTENT_TYPE_BIOPAGE);
        setContentType(map, Measurement.CONTENT_TYPE_BIOPAGE);
        setContentAuthor(map, FollowTrackingInfo.followTracking.contentAuthor);
        setAppSection(map, APP_SECTION_FOLLOWING);
        map.setEvar(Evars.TAB_NAME.getVariable(), TRACKING_TAB_NAMES.MY_POST.trackingTabName);
        setNavigationBehavior(map, PATH_TO_VIEW_FOLLOWING);
        setAppName(map, appName);
        setLoginSubscriptionStatus(map);
        setABTestingVariants(map);
        trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackNavigateToArticleFromReadingHistory(String navigationBehavior) {
        final MeasurementMap map = getDefaultMap();
        setNavigationBehavior(map, navigationBehavior);
    }

    public static void trackSlideShowSwipe(int position, String navigationBehavior) {
        MeasurementMap map = getPreviousMap();
        map.setEvar(Evars.ARTICLE_POSITION.getVariable(), "slideshow_" + position);
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), navigationBehavior);
        map.remove(Evars.PUSH_TITLE);
        map.remove(Evars.PUSH_NOTIFICATION_ID);
        map.remove(Evars.PUSH_ACTION);
        map.remove(Evars.PUSH_HEADLINE);
        trackEvents(map, Events.EVENT_SLIDESHOW_INTERACTION.getKey());
        map.setEvar(Evars.MISCELLANY.getVariable(), null);
    }

    public static void trackSlideShowOverlayClick(String overlayLink) {
        MeasurementMap map = getPreviousMap();
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.getVariable(), "slideshow");
        map.setEvar(Evars.CONTENT_URL.getVariable(), overlayLink);
        trackEvents(map, Events.EVENT_SLIDESHOW_OVERLAY.getKey());
    }

    public static void trackUserHitsPaywallFromFollowedArticle() {
        final MeasurementMap map = getDefaultMap();
        String pageName = getPreviousMap().getEvar(Evars.PAGE_NAME.getVariable()) != null ? getPreviousMap().getEvar(Evars.PAGE_NAME.getVariable()).toString() : "";
        String siteSection = getPreviousMap().getEvar(Evars.SITE_SECTION.getVariable()) != null ? getPreviousMap().getEvar(Evars.SITE_SECTION.getVariable()).toString() : "";
        String subSection = getPreviousMap().getEvar(Evars.SUB_SECTION.getVariable()) != null ? getPreviousMap().getEvar(Evars.SUB_SECTION.getVariable()).toString() : "";
        setAppSection(map, APP_SECTION_FOLLOWING);
        setPageName(map, pageName);
        setSiteSection(map, siteSection);
        setSubSection(map, subSection);
        setContentAuthor(map, FollowTrackingInfo.followTracking.contentAuthor);
    }

    public static String getAuthorsFollowings() {
        StringBuilder authorsFollowings = new StringBuilder(";following:off");
        if (FollowTrackingInfo.authors != null) {
            authorsFollowings = new StringBuilder();
            for (FollowEntity author : FollowTrackingInfo.authors) {
                authorsFollowings.append(author.getAuthorId()).append("|");
            }
            if (authorsFollowings.length() > 0) {
                authorsFollowings = new StringBuilder(";following:" + authorsFollowings.substring(0, authorsFollowings.length() - 1));
            }
        }
        return authorsFollowings.toString();
    }

    public static void trackBackFromActivity(String pageName) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setPageName(map, getTrackingPageName(pageName));
        setNavigationBehavior(map, PATH_TO_VIEW_BACK_TO_FRONT);
        trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void setFirstPublishedDate(final MeasurementMap map, Date firstPublishedDate) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        if (firstPublishedDate != null) {
            map.setEvar(Evars.PUBLISHED_DATE.getVariable(), dateFormat.format(firstPublishedDate));
        }
    }

    public static void trackCommentsButtonClick(TrackingInfo trackingInfo, String appSection) {
        MeasurementMap map = getNewMap();
        setSubsection(map, trackingInfo.getContentSubsection());
        setLoginSubscriptionStatus(map);
        setContentUrl(map, trackingInfo.getContentURL());
        setSignInMedium(map);
        setAppSection(map, appSection);
        setAuthorId(map, trackingInfo.getAuthorId());
        setContentAuthor(map, trackingInfo.getContentAuthor());
        setContentTopics(map, trackingInfo.getContentTopics());
        setContentType(map, trackingInfo.getContentType());
        setFirstPublishedDate(map, trackingInfo.getFirstPublishedDate());
        setNavigationBehavior(NavigationBehavior.COMMENTS);
        trackEvent(map, Events.EVENT_COMMENTS);
    }

    @Nullable
    public static String getPathToViewOf(String widgetType) {
        if (WidgetType.WIDGET.name().equalsIgnoreCase(widgetType)) {
            return PATH_TO_VIEW_WIDGET_SMALL;
        } else if (WidgetType.TABLET_WIDGET.name().equalsIgnoreCase(widgetType)) {
            return PATH_TO_VIEW_WIDGET;
        } else if (WidgetType.FOR_YOU_WIDGET.name().equalsIgnoreCase(widgetType)) {
            return PATH_TO_VIEW_FOR_YOU_WIDGET_LARGE;
        } else {
            return null;
        }
    }

    public static void setTetroAttributes(Float contentWeight) {
        MeasurementMap defaultMap = getDefaultMap();
        defaultMap.setEvar(Evars.TETRO_CONTENT_WEIGHT.getVariable(), contentWeight % 1 == 0 ? String.format(Locale.US, "%.0f", contentWeight) : String.valueOf(contentWeight));
    }

    public static void updateTetroEvent(float meterCount, int meterReason) {
        MeasurementMap defaults = getNewMap();
        String formattedMeterCount = meterCount % 1 == 0 ? String.format("%.0f", meterCount) : String.valueOf(meterCount);

        defaults.setEvar(Evars.TETRO_METERED_UNMETERED_REASON.getVariable(), Integer.toString(meterReason));
        defaults.setEvar(Evars.METER_COUNT.getVariable(), formattedMeterCount);
        defaults.setEvar(Evars.METER_COUNT_1.getVariable(), formattedMeterCount);

        // Store for downstream use
        articleContentMap.setEvar(Evars.TETRO_METERED_UNMETERED_REASON.getVariable(), Integer.toString(meterReason));
        articleContentMap.setEvar(Evars.METER_COUNT.getVariable(), formattedMeterCount);
        articleContentMap.setEvar(Evars.METER_COUNT_1.getVariable(), formattedMeterCount);

        trackEvent(defaults, Events.EVENT_TETRO_RESPONSE);
    }

    public static void trackCaSettlementShown() {
        MeasurementMap defaults = getNewMap();
        setPageName(defaults, "settlement_notice");
        trackEvent(defaults, Events.EVENT_PAGE_VIEW);
    }

    /**
     * @param navigationBehavior one of {@link Measurement#NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_CAROUSEL},
     *                           {@link Measurement#NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_FORWARD},
     *                           or {@link Measurement#NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_BACK}
     */
    public static void trackBrightInteraction(String navigationBehavior, int newPosition) {
        final MeasurementMap map = getNewMap();
        Measurement.setNavigationBehavior(map, navigationBehavior);
        if (newPosition >= 0) {
            Measurement.setPositionInStack(map, newPosition);
        }
        Measurement.trackEvent(map, Events.EVENT_BRIGHT_INTERACTION);
    }

    /**
     * Tracks an interaction event when the user swipes between items in an audio carousel
     *
     * @param swipeDirection the direction that the user swiped -
     *                       {@link Measurement#NAVIGATION_AUDIO_CAROUSEL_FORWARD} or {@link Measurement#NAVIGATION_AUDIO_CAROUSEL_BACK}
     */
    public static void trackAudioCarouselSwipeNavigation(String swipeDirection) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setMiscellany(map, swipeDirection);
        setNavigationBehavior(map, NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_SWIPE);
        setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
        trackEvents(map, Events.EVENT_INTERACTION.getKey());
    }

    public static void trackAudioPlayerNextPrevious(String direction, int progress, String feed) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setMiscellany(map, direction);
        setAudioFeed(map, feed);
        setProgressThreshold(map, progress);
        if (isAutoOriginated) {
            setNavigationBehavior(map, NAVIGATION_AUDIO_AUTO);
        } else {
            setNavigationBehavior(map, NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_PLAY);
        }
        setAvPlayerType(map, AUDIO_CAROUSEL_PLAYER_TYPE);
        trackEvents(map, Events.EVENT_INTERACTION.getKey());
    }

    /**
     * Sets the appropriate page view values for a page view originating from an audio carousel
     *
     * @param positionInCarousel the item's position in the audio carousel (starting at 1)
     * @param pageName           the page name value to track
     */
    public static void setAudioCarouselPageViewValues(int positionInCarousel, String pageName) {
        isAudioCarouselOriginated = true;
        setLoginSubscriptionStatus(audioCarouselMap);
        setNavigationBehavior(audioCarouselMap, NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_OPEN + positionInCarousel);
        setNavigationBehavior(getDefaultMap(), NAVIGATION_BEHAVIOR_AUDIO_CAROUSEL_OPEN + positionInCarousel);
        setPageName(audioCarouselMap, pageName);
    }

    public static void setMessageUseCase(MeasurementMap map, String messageUseCase) {
        map.setEvar(Evars.EVENT_MESSAGE_USE_CASE.getVariable(), messageUseCase);
    }

    public static void trackInAppMessage(Events event, InAppMessageData inAppMessageData) {
        currentInAppMessageData = inAppMessageData;
        setNavigationBehaviorInDefaultMap(PATH_TO_VIEW_IN_APP_PROMPT);
        final MeasurementMap map = getNewMap();
        setAppName(map, appName);
        setInAppMessageParameters(map);
        setLoginSubscriptionStatus(map);
        setUUID(map);
        if (!TextUtils.isEmpty(inAppMessageData.getMiscellany())) {
            String miscellany = inAppMessageData.getMiscellany()
                    .replace(" ", "_").toLowerCase();
            miscellany = "iam_button:" + miscellany;
            setMiscellany(map, miscellany);
        } else {
            setMiscellany(map, null);
        }
        trackEvent(map, event);
    }

    public static void setInAppMessageParameters(MeasurementMap map) {
        setPushNotificationId(map, currentInAppMessageData.getScheduleId());

        String campaignName = currentInAppMessageData.getMessageTracking() != null
                ? currentInAppMessageData.getMessageTracking().getCampaignName()
                : null;

        Integer campaignId = currentInAppMessageData.getAttributionInfo() != null
                ? currentInAppMessageData.getAttributionInfo().getCampaignId()
                : null;

        String locationString = AcquisitionEntranceTypeBuilder.build(
                "regular_in_app_message",
                campaignName,
                campaignId,
                false
        );

        if (campaignName != null || campaignId != null) {
            setPushHeadline(map, locationString);
            setMessageUseCase(map, locationString);
        }
        else {
            setPushHeadline(map, currentInAppMessageData.getHeadline());
            setMessageUseCase(map, currentInAppMessageData.getEventLabel());
        }

        setPushTitle(map, currentInAppMessageData.getTitle());
        setPushAction(map, "in_app_message");
        setNavigationBehavior(map, PATH_TO_VIEW_IN_APP_PROMPT);
    }

    private static boolean isBioPage(String pageName) {
        if (pageName == null) return false;
        return pageName.startsWith(PAGE_BIOPAGE_AUTHOR_PREFIX);
    }

    private static MeasurementMap getMyPostMap(MyPostSection section) {
        final MeasurementMap map = getNewMap();

        String sectionName = "";
        String appSection = "";
        switch (section) {
            case ALL:
                sectionName = PAGE_FRONT_MY_POST_ALL;
                break;
            case SAVED_STORIES:
                sectionName = PAGE_FRONT_MY_POST_SAVED_STORIES;
                appSection = APP_SECTION_MY_POST_SAVED_STORIES;
                break;
            case TOPICS:
                sectionName = PAGE_FRONT_MY_POST_TOPICS;
                appSection = APP_SECTION_MY_POST_TOPICS;
                break;
            case FOLLOWING:
                sectionName = PAGE_FRONT_MY_POST_FOLLOWING;
                appSection = APP_SECTION_MY_POST_FOLLOWING;
                break;
            case READING_HISTORY:
                sectionName = PAGE_FRONT_MY_POST_READING_HISTORY;
                appSection = APP_SECTION_MY_POST_READING_HISTORY;
                break;
            case PURCHASE:
                sectionName = PAGE_FRONT_MY_POST_PURCHASE;
                appSection = APP_SECTION_MY_POST_PURCHASE;
                break;
        }

        setNavigationBehavior(map,sectionName);
        setPageName(map, sectionName);
        setTabName(map, "my post");
        setContentType(map, PAGE_FRONT);
        setAppSection(map, appSection);

        return map;
    }

    private static MeasurementMap getFindMap() {
        final MeasurementMap map = getNewMap();

        String sectionName = PAGE_FRONT_FIND;

        setPageName(map, sectionName);
        setTabName(map, "find");
        setContentType(map, PAGE_FRONT);

        return map;
    }

    public static String getMyPostPageName(MyPostSection section) {
        MeasurementMap map = getMyPostMap(section);
        Object pageName = map.getEvar(Evars.PAGE_NAME.getVariable());
        if (pageName instanceof String) {
            return (String) pageName;
        } else {
            return "";
        }
    }

    public static String getMyPostPageName(String myPostSection) {
        switch (myPostSection) {
            case "ALL":
                return PAGE_FRONT_MY_POST_ALL;
            case "SAVED_STORIES":
                return PAGE_FRONT_MY_POST_SAVED_STORIES;
            case "TOPICS":
                return PAGE_FRONT_MY_POST_TOPICS;
            case "FOLLOWING":
                return PAGE_FRONT_MY_POST_FOLLOWING;
            case "READING_HISTORY":
                return PAGE_FRONT_MY_POST_READING_HISTORY;
        }
        return PAGE_FRONT_MY_POST_PREFIX;
    }

    public static void trackMyPostToolBarNavigation(MyPostSection section, Boolean isTopNavOriginated) {
        final MeasurementMap map = getMyPostMap(section);
        if (isTopNavOriginated) {
            setNavigationBehavior(map, Measurement.NAVIGATION_BEHAVIOR_TOP_NAV);
        }
        trackEvent(map, Events.EVENT_PAGE_VIEW);
    }

    public static void trackMyPostMenuOpenEvent(MyPostSection section, String contentUrl) {
        final MeasurementMap map = getMyPostMap(section);
        setContentUrl(map, contentUrl);
        trackEvent(map, Events.EVENT_UTILITY_MENU_OPEN);
    }

    public static void trackMyPostMenuAddArticleEvent(MyPostSection section, String contentUrl) {
        final MeasurementMap map = getMyPostMap(section);
        setContentUrl(map, contentUrl);
        trackEvent(map, Events.EVENT_SAVE_ARTICLE);
    }

    public static void trackMyPostMenuShareEvent(MyPostSection section, String contentUrl) {
        final MeasurementMap map = getMyPostMap(section);
        setContentUrl(map, contentUrl);
        trackEvent(map, Events.EVENT_SHARE);
    }

    public static void trackMyPostMenuRemoveArticleEvent(MyPostSection section, String contentUrl) {
        final MeasurementMap map = getMyPostMap(section);
        setContentUrl(map, contentUrl);
        trackEvent(map, Events.EVENT_REMOVE_SAVED_ARTICLE);
    }

    public static void trackMyPostMenuRemoveHistoryArticleEvent(MyPostSection section, String contentUrl) {
        final MeasurementMap map = getMyPostMap(section);
        setContentUrl(map, contentUrl);
        trackEvent(map, Events.EVENT_REMOVE_HISTORY_ARTICLE);
    }

    public static void setMyPostCarouselPosition(MeasurementMap map, int position) {
        map.setEvar(Evars.ARTICLE_POSITION.getVariable(), Integer.toString(position + 1));
    }

    public static void trackMyPostProfileInteraction() {
        final MeasurementMap map = getNewMap();
        setPageName(map, PAGE_FRONT_MY_POST_ALL);
        setUUID(map);
        setMiscellany(map, MISCELLANY_UNIFIED_SIGN_IN_IMPRESSION);
        trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void setInlinePushToggleFlag(MeasurementMap map, Boolean flag) {
        map.setEvar(Evars.INLINE_PUSH_TOGGLE_FLAG.getVariable(), flag);
    }

    /**
     * This is to uniquely identify a page view event.
     *
     * @param map   - Initial map with all other attributes set
     * @param jUcid - Unique identifier which is called as j_ucid
     */
    public static void setJUcid(MeasurementMap map, String jUcid) {
        map.setEvar(Evars.J_UCID.getVariable(), jUcid);
        Logger.d("JUCID ", "from article pageView: " + jUcid);
    }

    /**
     * This is to uniquely identify a page view event by time.
     *
     * @param map  - Initial map with all other attributes set
     * @param jTid - Unique identifier which is called as j_tid - This is generated when a page in article is selected (swiping in view pager)
     */
    public static void setJTid(MeasurementMap map, Long jTid) {
        map.setEvar(Evars.J_TID.getVariable(), jTid);
        Logger.d("JTID ", "from article pageView: " + jTid);
    }

    public static void setGenEventDimension(MeasurementMap map, String value) {
        map.setEvar(Evars.GEN_EVENT_DIMENSION.getVariable(), value);
    }

    /**
     * Fires a crossword event with the navigation behavior indicating that it was from the My Post CTA
     */
    public static void trackMyPostExploreCrosswordsClick() {
        final MeasurementMap map = getMyPostMap(MyPostSection.ALL); // The crosswords CTA is only in the All section
        setNavigationBehavior(map, PATH_TO_VIEW_MY_POST_CROSSWORD);
        trackEvent(map, Events.EVENT_CROSSWORD);
    }

    public static void trackAudioCarouselSeen(boolean backToFront) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        if (backToFront) {
            setNavigationBehavior(map, PATH_TO_VIEW_BACK_TO_FRONT);
        }
        trackEvent(map, Events.EVENT_AUDIO_CAROUSEL_SEEN);
    }

    /**
     * Fires event indicating that an immersion carousel was seen by the user
     */
    public static void trackImmersionCarouselSeen(boolean backToFront) {
        final MeasurementMap map = getDefaultMap();
        setLoginSubscriptionStatus(map);
        if (backToFront) {
            setNavigationBehavior(map, PATH_TO_VIEW_BACK_TO_FRONT);
        }
        trackEvent(map, Events.EVENT_IMMERSION_CAROUSEL_SEEN);
    }

    /**
     * Fires event indicating that an external carousel was seen by the user
     */
    public static void trackExternalCarouselSeen(boolean backToFront) {
        final MeasurementMap map = getDefaultMap();
        setLoginSubscriptionStatus(map);
        if (backToFront) {
            setNavigationBehavior(map, PATH_TO_VIEW_BACK_TO_FRONT);
        }
        trackEvent(map, Events.EVENT_EXTERNAL_CAROUSEL_SEEN);
    }

    /**
     * Fires event indicating that a seven live carousel was seen by the user
     */
    public static void trackSevenLiveCarouselSeen(boolean backToFront) {
        final MeasurementMap map = getDefaultMap();
        setLoginSubscriptionStatus(map);
        if (backToFront) {
            setNavigationBehavior(map, PATH_TO_VIEW_BACK_TO_FRONT);
        }
        trackEvent(map, Events.EVENT_SEVEN_LIVE_CAROUSEL_SEEN);
    }

    /**
     * Tracks an interaction event when the user swipes between items in an immersion carousel
     *
     * @param swipeDirection the direction that the user swiped - "immersion_carousel_forward" or "immersion_carousel_back"
     */
    public static void trackImmersionCarouselSwipeNavigation(String swipeDirection) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setNavigationBehavior(map, swipeDirection);
        trackEvents(map, Events.EVENT_INTERACTION.getKey());
    }

    public static void trackCommentsCarouselSwipeNavigation(String swipeDirection, int position) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setNavigationBehavior(map, NAVIGATION_BEHAVIOR_COMMENTS_SWIPE_CAROUSEL);
        setMiscellany(map, swipeDirection);
        setProgressThreshold(map, position + 1);
        trackEvents(map, Events.EVENT_INTERACTION.getKey());
    }

    public static void trackActionButtonUtilityMenuOpen(String appSection, String pageName) {
        if (pageName != null) {
            setPageName(defaultMap, pageName);
            setPreviousPageName(defaultMap);
        }
        trackActionButtonEvent(Events.EVENT_UTILITY_MENU_OPEN, appSection);
    }

    private static void trackActionButtonEvent(Events event, String appSection) {
        MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setGenEventDimension(map, GEN_DIMEN_ACTION_BUTTON);
        setAppSection(map, appSection);
        trackEvents(map, event.getKey());
    }

    /**
     * Method to add any targeting values to the given adManagerAdRequestBuilder.
     * Right now permutive is adding its values to the given builder.
     * This is getting called for all Banner Ads there in the app.
     *
     * @param adManagerAdRequestBuilder
     */
    public static void addCustomTargeting(AdManagerAdRequest.Builder adManagerAdRequestBuilder) {
        Logger.d(TAG, "PermutiveDebug, addCustomTargeting()," +
                " providerExists=" + (permutiveProvider != null) +
                ", targetingEnabled=" + isTargetingEnabled());
        if (!isTargetingEnabled()) return;
        if (!isPermutiveSdkInitialized()) return;
        permutiveProvider.addCustomTargeting(adManagerAdRequestBuilder);
    }

    /**
     * Callback method when OneTrust targeting consent changed
     *
     * @param targetingEnabled
     */
    public static void targetingConsentChanged(Boolean targetingEnabled) {
        Measurement.targetingEnabled = targetingEnabled;
        initializeProviders();
    }

    /**
     * Callback method when CCPS consent changed
     *
     * @param optedOut
     */
    public static void ccpaAdsConsentChanged(Boolean optedOut) {
        Measurement.ccpaAdsOptedOut = optedOut;
        initializeProviders();
    }

    private static boolean isTargetingEnabled() {
        return targetingEnabled && !ccpaAdsOptedOut;
    }

    private static String getLoginUUID() {
        final PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            return paywallService.getLoggedInUser().getUuid();
        }
        return null;
    }

    /**
     * Callback method when SignIn is completed
     */
    public static void onUserSignInComplete() {
        setIdentityToPermutiveSdk(getLoginUUID());
    }

    /**
     * Callback method when SignOut is completed
     */
    public static void onUserSignOutComplete() {
        setIdentityToPermutiveSdk(getLoginUUID());
    }

    /**
     * Callback method when paywall is initialized
     */
    public static void onPaywallInitialize() {
        setIdentityToPermutiveSdk(getLoginUUID());
    }

    public static void trackInlineTopicFollowClicked(Article2 article) {
        MeasurementMap map = getDefaultMap();
        setPageName(map, article.getOmniture().getPageName());
        setContentType(map, CONTENT_TYPE_ARTICLE);
        setArcId(map, article.getArcId());
        setAppSection(map, article.getSection());
//        setTabName(map, article.getOmniture().tab);
        setMiscellany(map, INLINE_TOPIC_FOLLOW);
        trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackArticleSummarySeenEvent(String pageName, String arcId, String navigationBehavior, String miscellanySuffix) {
        MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setArcId(map, arcId);
        setNavigationBehavior(map, navigationBehavior);
        setMiscellany(map, String.format("%s_%s", MISCELLANY_SUMMARY_SEEN, miscellanySuffix));
        trackEvent(map, Events.EVENT_ARTICLE_SUMMARY_SEEN);
    }

    public static void trackArticleSummaryFeedbackSeenEvent(String pageName, String arcId, String navigationBehavior, String miscellanySuffix) {
        MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setArcId(map, arcId);
        setNavigationBehavior(map, navigationBehavior);
        setMiscellany(map, String.format("%s_%s", MISCELLANY_FEEDBACK_SEEN, miscellanySuffix));
        trackEvent(map, Events.EVENT_ARTICLE_SUMMARY_FEEDBACK);
    }

    public static void trackArticleSummaryFeedbackSubmitEvent(String pageName, String arcId, String navigationBehavior, String miscellanySuffix) {
        MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setArcId(map, arcId);
        setNavigationBehavior(map, navigationBehavior);
        setMiscellany(map, String.format("%s_%s", MISCELLANY_SUBMIT_FEEDBACK, miscellanySuffix));
        trackEvent(map, Events.EVENT_ARTICLE_SUMMARY_FEEDBACK);
    }

    public static void setHabitTilesNavigationBehavior(String section, String subsection) {
        if (section != null) {
            habitTilesNavigationBehavior = "hp_tile_" + section;
            if (subsection != null) {
                habitTilesNavigationBehavior += "_" + subsection;
            }
        }
    }

    public static String getHabitTilesNavigationBehavior() {
        return habitTilesNavigationBehavior;
    }

    public static void clearHabitTilesNavigationBehavior() {
        habitTilesNavigationBehavior = null;
    }

    public static void trackPostAnswersImpression(String queryId, String query, String searchType, String navigationBehavior, boolean overviewSeen) {
        MeasurementMap map = getNewMap();
        setPageName(map, PAGE_SEARCH_RESULTS);
        setSearchKeywords(map, TextUtils.isEmpty(queryId) ? query : queryId);
        String miscellany = searchType;
        if (overviewSeen) {
            miscellany += AI_OVERVIEW_SEEN_YES;
        } else {
            miscellany += AI_OVERVIEW_SEEN_NO;
        }
        setMiscellany(map, miscellany);
        setNavigationBehavior(map, TextUtils.isEmpty(navigationBehavior) ? searchType : navigationBehavior);
        trackEvent(map, Events.EVENT_ONPAGE_IMPRESSION);
    }

    public static void trackPostAnswersShowMoreClick(String queryId, String query, String searchType) {
        MeasurementMap map = getNewMap();
        setPageName(map, PAGE_SEARCH_RESULTS);
        setSearchKeywords(map, TextUtils.isEmpty(queryId) ? query : queryId);
        setNavigationBehavior(map, searchType);
        setMiscellany(map, AI_OVERVIEW_EXPAND);
        trackEvent(map, Events.EVENT_INTERACTION);
    }

    public static void setPostAnswersCarouselInfo(int position, String query, String queryId) {
        postAnswersCarouselInfo = new PostAnswerCarouselTrackingHelper(position + 1, query, queryId);
    }

    public static void trackOnpageTap(String miscellany, String pageName, String arcId) {
        trackOnpageTap(miscellany, pageName, arcId, null);
    }

    public static void trackAudioInteraction(String avName, String navBehavior, String miscellany, String avTags) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setNavigationBehavior(map, navBehavior);
        setAvPlayerType(map, AUDIO_PERSO_PODCAST_PLAYER_TYPE);
        setMiscellany(map, miscellany);
        setAvType(map, PERSO_PODCAST);
        setAvTags(map, avTags);
        setAvName(map, "podcasts: " + avName);
        trackEvents(map, Events.EVENT_AUDIO_INTERACTION.getKey());
    }

    public static void trackAudioInteraction(String avName, String navBehavior, String miscellany) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setNavigationBehavior(map, navBehavior);
        setAvPlayerType(map, AUDIO_PERSO_PODCAST_PLAYER_TYPE);
        setMiscellany(map, miscellany);
        setAvType(map, PERSO_PODCAST);
        setAvName(map, "podcasts: " + avName);
        trackEvents(map, Events.EVENT_AUDIO_INTERACTION.getKey());
    }

    public static void trackAudioStart(String avName, String navBehavior, String miscellany, String id) {
        final MeasurementMap map = getNewMap();
        setLoginSubscriptionStatus(map);
        setNavigationBehavior(map, navBehavior);
        setAvPlayerType(map, AUDIO_PERSO_PODCAST_PLAYER_TYPE);
        setMiscellany(map, miscellany);
        setAvType(map, PERSO_PODCAST);
        setAvName(map, "podcasts: " + avName);
        setAvArcId(map, id);
        trackEvents(map, Events.EVENT_AUDIO_START.getKey());
    }

    public static void trackOnpageTap(String miscellany, String pageName, String arcId, @Nullable String genEventDimension) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, miscellany);
        setPageName(map, pageName);
        setArcId(map, arcId);
        setGenEventDimension(map, genEventDimension);
        Measurement.trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackMapImpression(
            String pageName,
            String arcId,
            String contentType,
            String contentSection,
            String contentSubsection,
            String authorId,
            String miscellany
    ) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setArcId(map, arcId);
        setContentType(map, contentType);
        setSiteSection(map, contentSection);
        setSubsection(map, contentSubsection);
        setAuthorId(map, authorId);
        setMiscellany(map, miscellany);
        trackEvent(map, Events.EVENT_ONPAGE_IMPRESSION);
    }

    public static void trackMapDismissal(
            String pageName,
            String arcId,
            String contentType,
            String contentSection,
            String contentSubsection,
            String authorId
    ) {
        final MeasurementMap map = getNewMap();
        setPageName(map, pageName);
        setArcId(map, arcId);
        setContentType(map, contentType);
        setSiteSection(map, contentSection);
        setSubsection(map, contentSubsection);
        setAuthorId(map, authorId);
        setMiscellany(map, MAP_RECIRC_DISMISS);
        trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void setNavigationBehaviorToSearchMap(String navigationBehavior) {
        if (!TextUtils.isEmpty(navigationBehavior)) {
            setNavigationBehavior(searchTrackingMap, navigationBehavior);
        }
    }

    public static void trackTalkSheetOpen() {
        MeasurementMap map = getNewMap();
        setMiscellany(map, VOICE_CLICK);
        setGenEventDimension(map, VOICE_OPEN);
        trackEvent(map, Events.EVENT_ANSWERBOT_VOICE);
    }

    public static void trackTalkFirstQuestionAsked() {
        MeasurementMap map = getNewMap();
        setMiscellany(map, SUBMIT_QUESTION);
        setGenEventDimension(map, VOICE_QUESTION);
        trackEvent(map, Events.EVENT_ANSWERBOT_VOICE);
    }

    public static void trackTalkResponded(String turnId) {
        MeasurementMap map = getNewMap();
        setMiscellany(map, ATP_RESPONSE);
        setGenEventDimension(map, VOICE_RESPONSE);
        setAvArcId(map, turnId);
        trackEvent(map, Events.EVENT_ANSWERBOT_VOICE);
    }

    public static void trackTalkFollowUpQuestionAsked(String turnId) {
        MeasurementMap map = getNewMap();
        setMiscellany(map, SUBMIT_FOLLOWUP);
        setGenEventDimension(map, VOICE_FOLLOWUP);
        setAvArcId(map, turnId);
        trackEvent(map, Events.EVENT_ANSWERBOT_VOICE);
    }

    public static void trackTalkSpeechRecError(String turnId, boolean errorLimitReached) {
        MeasurementMap map = getNewMap();
        setMiscellany(map, VOICE_ERROR);
        if (errorLimitReached) {
            setGenEventDimension(map, TRY_TEXT);
        } else {
            setGenEventDimension(map, SPEECH_REC_ERROR);
        }
        setAvArcId(map, turnId);
        trackEvent(map, Events.EVENT_ANSWERBOT_VOICE);
    }

    public static void trackTalkToggleCaptions(boolean enabled) {
        MeasurementMap map = getNewMap();
        setMiscellany(map, "caption-toggle");
        if (enabled) {
            setGenEventDimension(map, "caption-on");
        } else {
            setGenEventDimension(map, "caption-off");
        }
        trackEvent(map, Events.EVENT_ANSWERBOT_VOICE);
    }
    public static String getTestGroupString(Map<String, String> testGroupMap) {
        if (testGroupMap == null || testGroupMap.isEmpty()) {
            return "";
        }

        StringBuilder testGroup = new StringBuilder();
        int index = 0;
        int size = testGroupMap.size();

        for (HashMap.Entry<String, String> entry : testGroupMap.entrySet()) {
            testGroup.append(entry.getKey())
                    .append("|")
                    .append(entry.getValue());

            if (++index != size) {
                testGroup.append(";");
            }
        }

        return testGroup.toString();
    }

    public static void trackEngagement(EngagementTrace trace) {
        if (!trace.isValid()) {
            Logger.e(TAG, "Invalid engagement trace: id=" + trace.getId() + ", engagedTimeMillis=" + trace.getEngagedTimeMillis());
            return;
        }

        Events eventType = null;
        MeasurementMap map = getNewMap();
        int engagedTimeInSeconds = Math.round((float) trace.getEngagedTimeMillis() / 1000);
        setEngagedTime(map, engagedTimeInSeconds);

        if (trace instanceof SessionEngagementTrace) {
            eventType = Events.EVENT_SESSION_ENGAGEMENT_TIME;
            // Session is ending: emit this event with the current push id still attached (via
            // getNewMap above) and then clear it so future events do not continue to send it.
            PushIdTracker.INSTANCE.clear();
        } else if (trace instanceof PageEngagementTrace) {
            eventType = Events.EVENT_PAGE_ENGAGEMENT_TIME;
            PageEngagementTrace pageEngagement = (PageEngagementTrace) trace;
            setPageName(map, pageEngagement.getPageName());
            setTabName(map, pageEngagement.getTabName());
            setContentType(map, pageEngagement.getContentType());
            setJUcid(map, JUcidTracker.INSTANCE.getJUcid());
            setJTid(map, JTidTracker.getCurrentJTid());
        } else if (trace instanceof ArticlePageEngagementTrace) {
            eventType = Events.EVENT_PAGE_ENGAGEMENT_TIME;
            map.putAll(((ArticlePageEngagementTrace) trace).getEventsMap());
        }

        if (eventType == null) {
            Logger.e(TAG, "Event type not set");
            return;
        }

        trackEvent(map, eventType);
    }

    /**
     * Generic banner_displayed event for any Iterable embedded message placement.
     * @param placementType  The IamMessageType.type string (e.g. "banner", "article", "section", etc.)
     * @param contentUrl     Optional URL associated with the banner
     */
    public static void trackBannerDisplayedEvent(String placementType, String contentUrl, String kind, String campaignName, Integer campaignId) {
        MeasurementMap map = getNewMap();

        // build constructed of apps_{banner_placement}_{banner_campaign_name}
        String locationString = AcquisitionEntranceTypeBuilder.build(
                placementType,
                campaignName,
                campaignId,
                false
        );

        String locationKey = PAYWALL_KIND.equals(kind)
                ? Evars.ACQ_ENTRANCE_TYPE.getVariable()
                : Evars.SUB_START_LOCATION.getVariable();

        map.setEvar(locationKey, locationString);
        setMiscellany(map, locationString);
        if (contentUrl != null) {
            setContentUrl(map, contentUrl);
        }
        trackEvent(map, Events.EVENT_BANNER_DISPLAYED);
    }


    /**
     * Generic banner_displayed event for any Iterable embedded message placement.
     * @param placementType  The IamMessageType.type string (e.g. "banner", "article", "section", etc.)
     * @param contentUrl     Optional URL associated with the banner
     */
    public static void trackBannerClickEvent(String placementType, String contentUrl, String kind, String campaignName, Integer campaignId, String navigationBehavior) {
        MeasurementMap map = getNewMap();

        // build constructed of apps_{banner_placement}_{banner_campaign_name}
        String locationString = AcquisitionEntranceTypeBuilder.build(
                placementType,
                campaignName,
                campaignId,
                false
        );

        String locationKey = PAYWALL_KIND.equals(kind)
                ? Evars.ACQ_ENTRANCE_TYPE.getVariable()
                : Evars.SUB_START_LOCATION.getVariable();

        map.setEvar(locationKey, locationString);
        setMiscellany(map, locationString);
        if (contentUrl != null) {
            setContentUrl(map, contentUrl);
        }
        setNavigationBehavior(map, navigationBehavior);
        trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackWallDisplayedEvent(String placementType, String contentUrl, String kind, String campaignName, Integer campaignId, String navigationBehavior) {
        MeasurementMap map = getNewMap();

        // build constructed of apps_{banner_placement}_{banner_campaign_name}
        String locationString = AcquisitionEntranceTypeBuilder.build(
                placementType,
                campaignName,
                campaignId,
                false
        );

        String locationKey = PAYWALL_KIND.equals(kind)
                ? Evars.ACQ_ENTRANCE_TYPE.getVariable()
                : Evars.SUB_START_LOCATION.getVariable();

        map.setEvar(locationKey, locationString);
        if (contentUrl != null) {
            setContentUrl(map, contentUrl);
        }
        setNavigationBehavior(map, navigationBehavior);
        trackEvent(map, Events.EVENT_APPS_WALL);
    }

    public static void trackBannerClicked(String contentUrl) {
        MeasurementMap map = getNewMap();
        setMiscellany(map,ASK_THE_POST_BANNER);
        if (contentUrl != null) {
            setContentUrl(map, contentUrl);
        }
        setNavigationBehavior(map,ASK_THE_POST_BANNER);
        setAppSection(map, ASK_THE_POST);
        setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        trackEvent(map, Events.EVENT_ONPAGE_TAP);
    }

    public static void trackBannerDismissed(String contentUrl) {
        MeasurementMap map = getNewMap();
        setMiscellany(map,ASK_THE_POST_BANNER);
        setNavigationBehavior(map,ASK_THE_POST_BANNER);
        setAppSection(map, ASK_THE_POST);
        setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        if (contentUrl != null) {
            setContentUrl(map, contentUrl);
        }
        trackEvent(map, Events.EVENT_BANNER_DISMISSED);
    }

    public static void trackATPCloseProfileInteraction(String miscellany) {
        final MeasurementMap map = getNewMap();
        setMiscellany(map, miscellany);
        setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
        setAppSection(map, ASK_THE_POST);
        trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void setATPBannerPurchaseAttributes() {
        MeasurementMap map = Measurement.getDefaultMap();
        Measurement.setNavigationBehavior(map, ASK_THE_POST_BANNER);
        Measurement.setAppSection(map, ASK_THE_POST);
        setPaywallArticle(PAGE_FRONT_PREFIX + ASK_THE_POST, ASK_THE_POST_BANNER);
        Measurement.setPageName(map, PAGE_FRONT_PREFIX + ASK_THE_POST);
    }

    /**
     * Clears the shared default map and re-seeds baseline parameters.
     * Call this only when leaving a feature that writes feature-specific values into the default map,
     * so those values don’t leak into subsequent events.
     */
    public static void clearDefaultMap() {
        if (defaultMap == null) return;
        defaultMap.clear();
        setNavigationBehavior(defaultMap, PATH_TO_VIEW_APP_OPEN);
        setAppName(defaultMap, appName);
        setStandardParams(defaultMap);
    }

    public static void trackSignInPromptShown(boolean isSubActive, String campaignEntranceType, String pageName) {
        final MeasurementMap map = getNewMap();
        String destination;
        if (isSubActive) {
            destination = campaignEntranceType + "_anonymous_sub";
        } else {
            destination = campaignEntranceType + "_anonymous_no_sub";
        }
        pageName = getPageName(pageName);
        Measurement.setMiscellany(map, destination);
        Measurement.setPageName(map, pageName);
        Measurement.setAppSection(map, pageName);
        Measurement.setNavigationBehavior(map, destination);
        Measurement.trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    @NonNull
    private static String getPageName(String pageName) {
        if (pageName == null || pageName.equals("/.")) {
            pageName = PAGE_FRONT_TOP_STORIES;
        } else if (pageName.startsWith("/")) {
            pageName = PAGE_FRONT_PREFIX + pageName;
        }
        return pageName;
    }

    public static void trackSignInPromptDismissed(boolean isSubActive, String campaignEntranceType, String pageName) {
        final MeasurementMap map = getNewMap();
        String destination;
        if (isSubActive) {
            destination = campaignEntranceType + "_anonymous_sub_dismiss";
        } else {
            destination = campaignEntranceType + "_anonymous_no_sub_dismiss";
        }
        pageName = getPageName(pageName);
        Measurement.setMiscellany(map, destination);
        Measurement.setNavigationBehavior(map, destination);
        Measurement.setAppSection(map, pageName);
        Measurement.setPageName(map, pageName);
        Measurement.trackEvent(map, Events.EVENT_PROFILE_INTERACTION);
    }

    public static void setSignInPromptNavigationBehavior(boolean isSubActive, String pageName) {
        final MeasurementMap map = getDefaultMap();
        pageName = getPageName(pageName);
        Measurement.setPageName(map, pageName);
        Measurement.setAppSection(map, pageName);
        if (isSubActive) {
            setNavigationBehaviorInDefaultMap(SIGN_IN_HOMEPAGE + "_subs");
        } else {
            setNavigationBehaviorInDefaultMap(SIGN_IN_HOMEPAGE + "_nosubs");
        }
    }

    public static void trackWebViewEvent(String eventName, Map<String, Object> properties) {
        final MeasurementMap map = getNewMap();
        map.putAll(properties);
        Measurement.trackEvent(map, eventName, null, /* isRawEvent = */ true);
    }
}
