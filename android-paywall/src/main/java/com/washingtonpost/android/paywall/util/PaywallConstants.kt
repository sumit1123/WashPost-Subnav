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
package com.washingtonpost.android.paywall.util

import android.os.Bundle
import java.util.Arrays

/**
 * Paywall constants
 *
 * @author Bkilari
 */
object PaywallConstants {
    const val PREFS_NAME = "pw_prefs_name"

    // TODO: Move into db if necessary
    const val PW_METER_CYCLE_DAYS = "paywallMeterCycleDays"
    const val PW_MAX_ARTICLE_COUNT = "paywallMaxArticleCount"
    const val PW_FACEBOOK_DISPLAY_NAME = "facebookDisplayName"
    const val PW_CURRENT_ARTICLE_COUNT = "paywallCurrentArticleCountFloat"
    const val PW_CURRENT_ARTICLE_COUNT_OLD = "paywallCurrentArticleCount"
    const val PW_ARTICLE_WEIGHTS = "tetroArticleWeights"
    const val PW_ARTICLE_WEIGHTS_FETCH_TIME = "tetroArticleWeightsFetchTime"
    const val PW_VIEWED_WARNING_POINT = "paywallViewedWarningPoint"
    const val PW_SHOW = "showPaywall"
    const val PW_IS_CCEXPIRED_SHOWN = "showCCExpired"
    const val PW_RESET_MONTH = "paywallResetMonth"
    const val PW_TURNED_ON = "paywallTurnedOn"
    const val TETRO_TURNED_ON = "tetroTurnedOn"
    const val PW_WARNING_POINTS = "paywallWarningPoints"
    const val PW_LWA_ID = "paywallLwaId"
    const val PW_UUID_PREF = "uuid"
    const val PW_SUBSCRIPTION_ID = "subscriptionID"
    const val PW_PREF_FREE_TRIAL_SUB_VERIFIED = "pref.PW_PREF_FREE_TRIAL_SUB_VERIFIED"
    const val PW_FREE_ARTICLE_COUNT = "paywallFreeArticlesRemainingCount"
    const val PW_PRICE_PLACEHOLDER = "price"
    const val PW_UNIT_PLACEHOLDER = "unit"
    const val PW_TEXT_PLACEHOLDER = "text"
    const val PW_PERIOD_PLACEHOLDER = "period"
    const val PW_FREE_PERIOD_PLACEHOLDER = "freePeriod"
    const val OFFER_PRICE_FREE = "Free"
    const val PW_PAUSED_SKU_PLACEHOLDER = "pausedSku"
    const val PW_REGULAR_PRICE_PLACEHOLDER = "regularPrice"
    const val PW_REGULAR_UNIT_PLACEHOLDER = "regularUnit"
    const val PW_REGULAR_PERIOD_PLACEHOLDER = "regularPeriod"
    const val PW_REGULAR_TEXT_PLACEHOLDER = "regularText"
    const val PW_INTRO_PRICE_PLACEHOLDER = "introPrice"
    const val PW_INTRO_UNIT_PLACEHOLDER = "introUnit"
    const val PW_INTRO_PERIOD_PLACEHOLDER = "introPeriod"
    const val PW_INTRO_TEXT_PLACEHOLDER = "introText"
    const val PW_OFFER_PRICE_PLACEHOLDER = "offerPrice"
    const val PW_OFFER_UNIT_PLACEHOLDER = "offerUnit"
    const val PW_OFFER_PERIOD_PLACEHOLDER = "offerPeriod"
    const val PW_OFFER_TEXT_PLACEHOLDER = "offerText"
    const val PW_PRODUCT_NAME_PLACEHOLDER = "productName"
    const val PW_RENEWAL_DATE_PLACEHOLDER = "renewalDate"


    @JvmField
    val ignoreCategoryList = Arrays.asList(
        "Weather", "Traffic", "Video"
    ) // TODO:from config, to change
    const val WP_API_LINK = "link/account"
    const val WP_API_VERIFY = "verify"
    const val WP_API_VERIFY_DEVICE = "verify/device"
    const val WP_API_AMAZON_RAINBOW_VERIFY_DEVICE = "verify/device-amazon-rbw"
    const val WP_API_UUID = "uuid"
    const val WP_API_STATUS_OK = "OK"
    const val WP_API_STATUS_CLAIMED = "CLAIMED"
    const val WP_API_STATUS_EXPIRED = "EXPIRED"
    const val WP_API_CCEXPIRED_TRUE = "true"
    const val WP_API_CCEXPIRED_FALSE = "false"
    const val WP_PRODUCT_ALL = "ALLACCESS"
    const val WP_PRODUCT_WEB = "WEBONLY"
    const val WP_PRODUCT_NO = "NOACCESS"
    const val WP_PRODUCT_NATIONAL = "NATIONAL"
    const val WP_PREMIUM = "PREMIUM"
    const val WP_BASIC = "BASIC"
    const val WP_MONTHLY_PASS = "MONTHLY_PASS"
    const val WP_PASS = "PASS"
    const val WP_PAYG_WEEK = "PAYG_WEEK"

    // messages
    const val PW_CONFIRM_TITLE = "title"
    const val PW_CONFIRM_DESCRIPTION = "description"
    const val PW_FACEBOOK_SIGNED = "facebook"
    const val PW_WASHPOST_SIGNED = "washpost"
    const val PW_UUID = "uuid"
    const val DIGITAL_PREMIUM = "National Digital + D.C."
    const val DIGITAL_NATIONAL = "National Digital"
    const val DIGITAL_WEBSITE = "Digital"
    const val DIGITAL_SOURCE = "DIGITAL"
    const val PROMPT_TYPE = "promptType"
    const val FB_ACCESS_TOKEN = "accessToken"
    const val FB_STATE_CODE = "stateCode"
    const val ANDROID = "android"
    const val SOURCE_DEVICE = "device"
    const val PLAYSTORE_SUBS_SOURCE = "play_store_iap"

    @JvmField
    var WP_API_URL = ""
    var RAINBOW_DEBUG_API_URL = "https://subscribe.digitalink.com/rbw-web-views/"
    var RAINBOW_API_URL = "https://subscribe.washingtonpost.com/rbw-web-views/"
    var WEBVIEW_SIGN_IN_URL = RAINBOW_API_URL + "?_=%d#signin-menu"
    var WEBVIEW_WELCOME_URL = RAINBOW_API_URL + "?_=%d#initial-paywall"
    var WEBVIEW_UPGRADE_SUBSCRIPTION_URL = RAINBOW_API_URL + "?_=%d#upgrade-subscription"
    var WEBVIEW_STORE_ERROR_URL = RAINBOW_API_URL + "?_=%d#subscription-error"
    var WEBVIEW_PRINT_VERIFICATION_URL = RAINBOW_API_URL + "?_=%d#verify-subscription"
    var WEBVIEW_UPDATE_PAYMENT_URL = RAINBOW_API_URL + "?_=%d#upgrade-subscription"
    var WEBVIEW_THANK_YOU_URL = RAINBOW_API_URL + "?_=%d#thankyou"
    var WEBVIEW_FB_LOGIN_SUCCESS_URL =
        RAINBOW_API_URL + "fb-redirect-landing.html?#state=%s&access_token=%s&expires_in=%d"
    var WEBVIEW_OFFER_URL = ""
    const val OS_VERSION = "os_version"
    const val HARDWARE_TYPE = "hardware_name"
    const val APP_VERSION = "app_version"
    const val APP_NAME = "appName"
    const val DEVICE_ID = "deviceId"
    const val TETRO_TIMEOUT = 2500
    const val STANDARD_LOGIN = "StandardLoginMode"
    const val DIGITAL_VERIFICATION = "DigitalVerificationMode"
    const val PRINT_VERIFICATION = "PrintVerificationMode"
    const val STORE_SUB_LINK = "AppStoreSubscriptionLinkingMode"
    const val PURCHASE_SUBSCRIPTION = "PURCHASE_SUBSCRIPTION"
    const val UPGRADE_SUBSCRIPTION = "UPGRADE_SUBSCRIPTION"
    const val GOOGLE_STORE = "google"
    const val AMAZON_STORE = "amazon"
    const val GOOGLE_ENV = "playstore"
    const val SAMSUNG_STORE = "samsung"
    const val AMAZON_ENV = "amazon"
    const val NEW_STORIES_AVAILABLE_RECEIVER =
        "com.washingtonpost.rainbow.receivers.NewStoriesAvailableReceiver"
    const val SUSPENDED = "S"
    const val ACTIVE = "A"
    const val TERMINATED = "T"
    const val FREE_TRIAL = "F"
    const val PAUSED = "P"
    const val EMPTY = ""
    const val FREE_DAYS = "FA_T"
    const val MOBILE_FREE_DAYS = "MFA_T"
    const val FREE_ARTICLES = "FA_X"
    const val SUBDATA_OK = "OK"
    const val SUBDATA_FAILED = "FAILED"
    const val AUTH_REDIRECT_SCHEME = "com.washingtonpost.rainbow://authcallback"
    const val PAUSE_SCHEDULED = "PauseScheduled"
    const val AUTO_RESUME_TIME = "auto_resume_time"
    const val SUB_STATE_CANCELLED = "Canceled"

    @JvmField
    var IS_FTC_VISIBLE = false

    @JvmField
    var AUTH_PROFILE_API = "https://idstg.digitalink.com/identity/oauth/profile"

    @JvmField
    var AUTH_REVOKE_API = "https://idstg.digitalink.com/identity/oauth/revoke"

    @JvmField
    var AUTH_MIGRATE_API = "https://idstg.digitalink.com/identity/oauth/migrate"

    @JvmField
    var TETRO_API = "https://www.washingtonpost.com/tetro/metering/"

    @JvmField
    var METERING_PROXY_BASE_URL = "https://subs-stage.washingtonpost.com/nativeservice/proxy/metering/apps/"

    @JvmField
    var USE_METERING_PROXY = true

    @JvmField
    var AUTH_ONE_LINK_TOKEN_API = "https://subs-stage.washingtonpost.com/oauth/one-link/token/"

    const val ACCESS_TOKEN_PARAM = "access_token"
    const val CLIENT_ID_PARAM = "client_id"
    const val CLIENT_ID_HEADER_PARAM = "clientId"
    const val TOKEN_PARAM = "token"
    const val CLIENT_SECRET_PARAM = "client_secret"
    const val TOKEN_TYPE_HINT = "token_type_hint"
    const val REFRESH_TOKEN_PARAM = "refresh_token"
    const val LOGIN_ID_PARAM = "wapo_login_id"
    const val SECURE_LOGIN_ID_PARAM = "wapo_secure_login_id"
    const val PROVIDER_NAME_PARAM = "provider_name"
    const val WAPO_PROVIDER = "Washington Post"
    const val GRANT_TYPE_PARAM = "grant_type"
    const val ONE_LINK_TOKEN_PARAM = "oneLinkToken"
    const val LEGACY_LOGIN = "legacy_login"
    const val AUTHORIZATION_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer"
    const val PW_GDPR_ANONYMIZED = "gdpr_anonymized"
    const val PW_CSR_DELETED = "csr_hard_deleted"
    const val PW_IOS_DELETED = "apple_account_deletion_requested"
    const val COOKIE_HEADER = "Cookie"
    const val SEC_WAPO_LOGIN_ID_PREFIX = "sec_wapo_login_id="
    const val APPS_REGWALL_PREFIX = "apps_regwall"
    const val AUTH_EXTRA_FAILED = "failed"

    //constants for new paywall rules
    const val PW_PREF_GROUP_MAX_PREFIX = "groupMax"
    const val PW_PREF_GROUP_CURRENT_COUNT_PREFIX = "groupCurrent"
    const val PW_SHOW_RULE1 = "showPaywallRuleOne"
    const val PW_PREF_MAX_ROLLING_DAYS = "rollingMaxDays"
    const val PW_PREF_ROLLING_MAX_ARTICLES = "rollingMaxArticles"
    const val PW_PREF_ROLLING_CURRENT_ARTICLE_COUNT = "rollingCurArtCount"
    const val PW_PREF_CURRENT_ARTICLE_METER_REASON = "currentArtMeterReason"
    const val PW_SHOW_RULE2 = "showPaywallRuleTwo"
    const val PW_ROLLING_COUNTS_FILE = "rollfile"


    const val SUB_GRACE_PERIOD = "GracePeriod"
    const val SUB_ON_HOLD = "OnHold"
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS_OF_SERVICE = "terms_of_service"

    const val METERED = 0
    const val SUB_ONLY_CONTENT = 1
    const val SUB_ONLY_FEATURE = 2
    const val ONBOARDING = 3
    const val GLOBAL_SUBSCRIBE_BUTTON = 4
    const val FREE_NOT_METERED = 5
    const val WEBVIEW_HANDLED = 6
    const val SECTION_INLINE_OFFER = 7
    const val ARTICLE_INLINE_OFFER = 8

    const val METERED_PAYWALL_RESULT_ID = 1
    const val SETTINGS_PAYWALL_RESULT_ID = 2
    const val ONBOARDING_PAYWALL_RESULT_ID = 3
    const val GLOBAL_SUBSCRIBE_BUTTON_RESULT_ID = 4
    const val REMINDER_PAYWALL_RESULT_ID = 5
    const val ACCOUNT_HOLD_REMINDER = 6
    const val REGWALL_REGISTER = 7
    const val SUBSCRIPTION_PAUSED = 8

    const val TETRO_DEFAULT_METER_LIMIT = 2

    const val BLOCKER_NAME = "blockerName"
    const val PAYWALL_CHOICE = "choice"
    const val PAYWALL_CHOICE_DEFAULT_VALUE = -1
    const val MONTHLY = 0
    const val ANNUAL = 1

    const val H1 = "h1"
    const val H2 = "h2"
    const val H3 = "h3"
    const val H4 = "h4"
    const val H5 = "h5"

    // Config Version Enum
    enum class BlockerConfigVersion(val version: Int) {
        // Original Tiny Tiles. hard-coded Create account tile
        V2(2),

        // Configurable Tiny Tiles. Configurable Create account tile (hero tile)
        V3(3),

        // iOS has bumped up
        V4(4),

        // Addition of flex native (one day pass)
        V5(5),

        // Added `items` array support in place of `products` array for iap, external and registration types.
        V6(6)
    }
    @JvmField
    val SUPPORTED_CONFIG_VERSION = BlockerConfigVersion.V6.version

    const val PAYWALL_REASON: String = "paywall_reason"
    const val PAYWALL_TYPE_ORDINAL: String = "paywall_type_ordinal"
    const val REQUEST_CODE: String = "request_code"

    const val CTA_BANNER = "banner"
    const val CTA_WALL = "wall"
    const val CTA_ACQUISITION_REMINDER = "acquisition_reminder"
    const val CTA_BOTTOM_CTA = "bottom_cta"

    const val ENTRY_POINT_UNKNOWN = "unknown"

    const val WALL_NAME_MAIN = "main"
    const val WALL_NAME_AMAZON_MAIN = "amazon_main"
    const val WALL_NAME_PAUSED = "paused"
    const val WALL_NAME_AUDIO_CAROUSEL = "audio_carousel"
    const val WALL_NAME_AUDIO_ACTION_BUTTON = "audio_article__action_button"
    const val WALL_NAME_REDDIT = "reddit"
    const val WALL_NAME_UNIVERSAL_REGWALL = "regwall"
    const val WALL_NAME_SAVE_REGWALL = "save_content"
    const val WALL_NAME_SAVE_RECIPE_REGWALL = "save_recipe"
    const val WALL_NAME_AD_FREE_LEGAL = "ad-free-legal"

    val regwalls = listOf(
        WALL_NAME_REDDIT,
        WALL_NAME_AUDIO_CAROUSEL,
        WALL_NAME_AUDIO_ACTION_BUTTON,
        WALL_NAME_UNIVERSAL_REGWALL,
        WALL_NAME_SAVE_REGWALL,
        WALL_NAME_SAVE_RECIPE_REGWALL
    )

    const val TWENTY_FOUR_HOURS_IN_MILLISECONDS = 86400000
    const val FOUR_HOURS_IN_MILLISECONDS = 14400000
    const val ONE_MINUTE_IN_MILLISECONDS = 60000
    const val VERIFY_EXPIRATION_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val PROFILE_EXPIRATION_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    val RAINBOW_SKU_LIST: List<String> = listOf(
        "m6-r",
        "M1-R",
        "m1_r",
        "basic_monthly_1m1d",
        "monthly_all_access",
        "basic_monthly_6m0d",
        "basic_test_30free",
        "wp.unified.basic",
        "wp.unified.basic.annual",
        "wp.unified.premium.annual",
        "wp.unified.premium"
    )

    val AMAZON_CLASSIC_SKU_LIST: List<String> = listOf(
        "wp.classic.basic",
        "monthly_all_access",
        "wp.classic.basic.annual",
        "wp.classic.premium.annual"
    )

    @JvmStatic
    fun getBundle(paywallReason: Int, paywallTypeOrdinal: Int): Bundle {
        return Bundle().apply {
            if (paywallReason != -1) {
                this.putInt(PAYWALL_REASON, paywallReason)
            }
            if (paywallTypeOrdinal != -1) {
                this.putInt(PAYWALL_TYPE_ORDINAL, paywallTypeOrdinal)
            }
        }
    }

    @JvmStatic
    fun getWallReason(type: WallType): Int {
        return when (type) {
            WallType.SUB_ONLY_CONTENT_PAYWALL -> SUB_ONLY_CONTENT
            WallType.ONBOARDING_PAYWALL -> ONBOARDING
            WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL -> GLOBAL_SUBSCRIBE_BUTTON
            WallType.WEBVIEW_PAYWALL, WallType.WEBVIEW_PRODUCT_PAGE -> WEBVIEW_HANDLED
            WallType.REMINDER_PAYWALL -> GLOBAL_SUBSCRIBE_BUTTON
            WallType.SETTINGS_PAYWALL -> SUB_ONLY_FEATURE
            WallType.ARTICLE_LINKS_PAYWALL -> SUB_ONLY_FEATURE
            WallType.PAUSEWALL -> SUBSCRIPTION_PAUSED
            else -> METERED
        }
    }

    enum class ManageSubUrlItids(val value: String) {
        APP_SETTINGS("app_settings"),
        APP_PAUSE_BANNER("app_pause_banner"),
        APPS_WALL_PAUSE("apps_wall_pause")
    }

    enum class SubscriptionType {
        WASHPOST, STORE, PARTNER
    }

    enum class SubscriptionSource {
        CLASSIC_IAP, MIGRATED_RAINBOW, WAPO_PROFILE, NO_SUB
    }

    /**
     * State of IAP Subscription.
     * - ACTIVE -> Amazon App Store / Playstore sub is active
     * - TERMINATED -> Amazon App Store / Playstore has expired subs and no active sub
     * - SUSPENDED -> Playstore Sub is in Grace Period
     * - PAUSED -> Playstore Sub is Paused
     * - NO_SUB -> Amazon App Store / Playstore has no history of receipts
     * - UNKNOWN -> There was a failure to get IAP State from Amazon App Store / Playstore
     */
    enum class IapSubStatus {
        ACTIVE, TERMINATED, SUSPENDED, PAUSED, NO_SUB, UNKNOWN
    }

    enum class WallCategory {
        PAYWALL, REGWALL, PAUSEWALL, SOFTWALL
    }

    enum class WallType {
        SUB_ONLY_CONTENT_PAYWALL, METERED_PAYWALL, SETTINGS_PAYWALL,
        ONBOARDING_PAYWALL, GLOBAL_SUBSCRIBE_BUTTON_PAYWALL, ARTICLE_LINKS_PAYWALL,
        ARTICLE_DEEP_LINK_PAYWALL, WEBVIEW_PAYWALL, REMINDER_PAYWALL, WIDGET_SMALL_PAYWALL,
        WIDGET_PAYWALL, BOTTOM_CTA_PAYWALL, IAA_WALL,
        GIFT_EXPIRED_PAYWALL, GIFT_INVALID_PAYWALL, BOTTOM_CTA_GIFT_PAYWALL, GIFT_SENDER_NO_SUB,
        GIFT_SENDER_ACTION_BUTTONS, DEFAULT_DEEP_LINK_PAYWALL, NAMED_PAYWALL, AUDIO_CAROUSEL_PAYWALL,
        REGWALL, SAVE_REGWALL, SAVE_RECIPE_REGWALL,
        WEBVIEW_PRODUCT_PAGE, ONELINK_WALL, AUDIO_ACTION_BUTTON_PAYWALL, PAUSEWALL, CCPA_PAYWALL, SOFTWALL,
        SECTION_INLINE_OFFER_PAYWALL, ARTICLE_INLINE_OFFER_PAYWALL,REGWALL_TILE,PAYWALL_TILE, ATP_SOFTWALL
    }

    //Ad Subscription Constants
    const val AD_SUBSCRIPTION_ACTIVE = "yes"
    const val AD_SUBSCRIPTION_REGISTERED = "reg"
    const val AD_SUBSCRIPTION_NONE = "no"
    const val AD_SUBSCRIPTION_TERMINATED = "terminated"
}
