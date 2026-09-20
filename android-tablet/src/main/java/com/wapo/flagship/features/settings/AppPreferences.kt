package com.wapo.flagship.features.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.util.ConnectivityMonitor
import com.wapo.flagship.util.ConnectivityMonitor.ConnectivityState
import com.wapo.text.GlobalFont.DEFAULT_FONT_SIZE
import com.washingtonpost.android.R

object AppPreferences {
    private val application: Application = FlagshipApplication.getInstance()

    private val sharedPrefs: SharedPreferences =
        PreferenceManager
            .getDefaultSharedPreferences(FlagshipApplication.getInstance())

    // SettingsFragment Preferences
    val PREF_SETTINGS_UPGRADE_BANNER = application.getString(R.string.pref_upgrade_banner)
    val PREF_SIGN_IN = application.getString(R.string.pref_settings_sign_in)
    val PREF_SIGN_OUT = application.getString(R.string.pref_settings_sign_out)
    val PREF_SUB_LINK_ERROR = application.getString(R.string.pref_sub_link_error)
    val PREF_SUB_PAUSE_BANNER = application.getString(R.string.pref_sub_pause_banner)
    val PREF_MOBILE_FREE_TRIAL_BANNER =
        application.getString(
            R.string.pref_mobile_free_trial_banner,
        )
    val PREF_ACCOUNT_SUB_PRIMARY = application.getString(R.string.pref_primary_profile_sub)
    val PREF_ACCOUNT_SUB_SECONDARY = application.getString(R.string.pref_secondary_profile_sub)
    val PREF_SUB_BENEFITS = application.getString(R.string.pref_subscription_benefits)
    val PREF_THEME = application.getString(R.string.pref_settings_theme_key)
    val PREF_TEXT_SIZE = application.getString(R.string.pref_settings_text_size_key)
    val PREF_DEFAULT_FONT_SIZE =
        application.getString(
            R.string.pref_settings_match_default_font_size_key,
        )
    val PREF_OPEN_LINKS = application.getString(R.string.pref_settings_open_links_key)
    val PREF_PRINT_TUTORIAL = application.getString(R.string.pref_settings_print_tutorial_key)
    val PREF_ALERTS = application.getString(R.string.pref_settings_alerts_key)
    val PREF_NEWSLETTERS_AND_EMAIL_ALERTS =
        application.getString(
            R.string.pref_settings_newsletters_and_email_alerts_key,
        )
    val PREF_ABOUT_ME = application.getString(R.string.pref_settings_about_me_key)
    val PREF_CUSTOM_NAV = application.getString(R.string.pref_settings_custom_nav_key)
    val PREF_AUDIO = application.getString(R.string.pref_settings_audio_key)
    val PREF_STORAGE = application.getString(R.string.pref_settings_storage_key)
    val PREF_TEST_OPTIONS = application.getString(R.string.pref_settings_test_options_key)
    val PREF_DEVELOPER_MODE = application.getString(R.string.pref_developer_mode)
    val PREF_OPEN_DEBUG_PANEL = application.getString(R.string.pref_open_debug_panel)
    val PREF_DEBUG_PANEL = application.getString(R.string.pref_debug_panel)
    val PREF_DEBUG_PANEL_CATEGORY = application.getString(R.string.pref_debug_panel_category)
    val PREF_HELP = application.getString(R.string.pref_settings_help_key)
    val PREF_CONTACT_US = application.getString(R.string.pref_settings_contact_us_key)
    val PREF_PRIVACY = application.getString(R.string.pref_settings_privacy_key)
    val PREF_FEEDBACK = application.getString(R.string.pref_settings_feedback_key)
    val PREF_TERMS = application.getString(R.string.pref_settings_terms_key)
    val PREF_FOOTER = application.getString(R.string.pref_settings_footer_key)

    // Account Preferences
    val PREF_EDIT_EMAIL = application.getString(R.string.pref_edit_email)
    val PREF_EDIT_NAME = application.getString(R.string.pref_edit_name)
    val PREF_SUB_TYPE = application.getString(R.string.pref_sub_type)
    val PREF_AD_FREE_ADD_ON = application.getString(R.string.pref_ad_free_add_on)
    val PREF_MANAGE_SUB = application.getString(R.string.pref_manage_sub)
    val PREF_APP_STORE_TERMS = application.getString(R.string.pref_app_store_terms)
    val PREF_SUB_CATEGORY = application.getString(R.string.pref_sub_category)
    val PREF_SUB_TERMS_CATEGORY = application.getString(R.string.pref_sub_terms_category)
    val PREF_SUB_PAYMENT_ERROR = application.getString(R.string.pref_sub_payment_error)
    val PREF_DEVELOPER_CATEGORY = application.getString(R.string.pref_developer_category)
    val PREF_USER_CATEGORY = application.getString(R.string.pref_user_category)
    val PREF_SUBSCRIPTION_CATEGORY = application.getString(R.string.pref_subscription_category)
    val PREF_ADD_ON_CATEGORY = application.getString(R.string.pref_add_on_category)
    val PREF_NETWORKING_CATEGORY = application.getString(R.string.pref_networking_category)

    // Alerts Preferences - Reading preferences data from configs. no-op.
    val PREF_ALERTS_CATEGORY = application.getString(R.string.pref_alerts_category_key)

    // Storage Preferences
    val PREF_UPDATES_ON_WIFI = application.getString(R.string.pref_storage_updates_on_wifi_key)
    val PREF_LOW_DATA_MODE = application.getString(R.string.pref_storage_low_data_mode)
    val PREF_LOW_DATA_MODE_FROM_SETTINGS =
        application.getString(
            R.string.pref_storage_low_data_mode_from_settings,
        )
    val PREF_BACKGROUND_SYNC = application.getString(R.string.pref_storage_background_sync_key)
    val PREF_DOWNLOAD_PRINT_DAILY =
        application.getString(
            R.string.pref_storage_download_print_daily_key,
        )
    val PREF_CUSTOMIZE_PRINT_SECTIONS =
        application.getString(
            R.string.pref_storage_customize_print_sections_key,
        )
    val PREF_DELETE_PRINT = application.getString(R.string.pref_storage_delete_print_key)
    val PREF_AUTOPLAY_VIDEOS = application.getString(R.string.pref_storage_autoplay_videos_key)
    val PREF_BETA_WEBVIEW_OVERRIDE = application.getString(R.string.beta_webview_override)

    // Storage - Print Preferences

    // Test Options
    val PREF_TEST_ADS = application.getString(R.string.pref_test_ads_key)
    val PREF_TEST_ADS_VALUE = application.getString(R.string.pref_test_ads_value_key)

    // Privacy Preferences
    val PREF_AD_INFO = application.getString(R.string.pref_privacy_ad_info_key)
    val PREF_NOTICE_OF_COLLECTION =
        application.getString(
            R.string.pref_privacy_notice_of_collection_key,
        )
    val PREF_DO_NOT_SELL_INFO = application.getString(R.string.pref_privacy_do_not_sell_info_key)
    val PREF_PRIVACY_POLICY = application.getString(R.string.pref_privacy_privacy_policy_key)
    val PREF_CA_SETTLEMENT_SHOWN = application.getString(R.string.pref_ca_settlement_shown)
    val PREF_ONETRUST = application.getString(R.string.pref_privacy_onetrust_preference_center_key)
    val PREF_WP_WEEKLY = application.getString(R.string.pref_settings_wp_weekly)

    // Debug
    const val PREF_DEBUG_USE_RDS_TEST = "prefDebugRDSTest"
    const val PREF_SHOW_TEST_OPTIONS = "prefShowTestOptions"

    // Beta
    val PREF_BETA = application.getString(R.string.pref_settings_beta)
    val PREF_BETA_AGGREGATOR = application.getString(R.string.pref_settings_beta_aggregator)
    val PREF_BETA_ASK_THE_POST_AI_BOT =
        application.getString(
            R.string.pref_settings_beta_ask_the_post_ai,
        )
    val PREF_BETA_CLIMATE_BOT = application.getString(R.string.pref_settings_beta_climate_bot)
    val PREF_BETA_POST_LLM = application.getString(R.string.pref_settings_beta_post_llm)

    fun getTextSizeAsPerDefaultFontCheck(): Float {
        if (isDefaultFontSizeEnabled()) {
            return DEFAULT_FONT_SIZE * 1f
        }
        return getTextSize()
    }

    fun setTextSizeAsPerDefaultFontCheck(textSize: Float) {
        if (isDefaultFontSizeEnabled()) {
            return
        }
        setTextSize(textSize)
    }

    fun getTextSize(): Float = sharedPrefs.getFloat(PREF_TEXT_SIZE, DEFAULT_FONT_SIZE * 1f)

    fun setTextSize(textSize: Float) {
        sharedPrefs.edit().putFloat(PREF_TEXT_SIZE, textSize).apply()
    }

    fun isDeveloperModeEnabled(): Boolean = sharedPrefs.getBoolean("developer_mode_enabled", false)

    fun setDeveloperModeEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("developer_mode_enabled", enabled).apply()
    }

    fun isBetaWebviewOverrideEnabled(): Boolean = sharedPrefs.getBoolean(PREF_BETA_WEBVIEW_OVERRIDE, false)

    fun setBetaWebviewOverrideEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_BETA_WEBVIEW_OVERRIDE, enabled).apply()
    }

    fun isDefaultFontSizeEnabled(): Boolean = sharedPrefs.getBoolean(PREF_DEFAULT_FONT_SIZE, true)

    fun setDefaultFontSizeEnabled(enabled: Boolean) = sharedPrefs.edit().putBoolean(PREF_DEFAULT_FONT_SIZE, enabled).apply()

    fun isOpenWebInAppEnabled(): Boolean = sharedPrefs.getBoolean(PREF_OPEN_LINKS, true)

    fun isPrintTutorialEnabled(): Boolean = sharedPrefs.getBoolean(PREF_PRINT_TUTORIAL, true)

    fun setPrintTutorialEnabled(showPrintTutorial: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_PRINT_TUTORIAL, showPrintTutorial).apply()
    }

    fun isUpdatesOnWifiOnlyEnabled(): Boolean = sharedPrefs.getBoolean(PREF_UPDATES_ON_WIFI, true)

    fun isLowDataModeEnabled(): Boolean = sharedPrefs.getBoolean(PREF_LOW_DATA_MODE, true)

    fun setIsLowDataModeEnabled(value: Boolean) {
        if (!value) {
            setIsLowDataModeEnabledFromSettings(false)
        }
        sharedPrefs.edit().putBoolean(PREF_LOW_DATA_MODE, value).apply()
    }

    fun isLowDataModeEnabledFromSettings(): Boolean {
        val fromSettings = sharedPrefs.getBoolean(PREF_LOW_DATA_MODE_FROM_SETTINGS, true)
        if (fromSettings) {
            setIsLowDataModeEnabledFromSettings(false)
        }
        return fromSettings
    }

    fun setIsLowDataModeEnabledFromSettings(value: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_LOW_DATA_MODE_FROM_SETTINGS, value).apply()
    }

    fun canSyncOverCellular(): Boolean = !isUpdatesOnWifiOnlyEnabled()

    fun isDownloadDailyPaperOn(): Boolean = sharedPrefs.getBoolean(PREF_DOWNLOAD_PRINT_DAILY, false)

    fun setDownloadDailyPaperOn(dailyDownloadOn: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_DOWNLOAD_PRINT_DAILY, dailyDownloadOn).apply()
    }

    /**
     * @return true if the user preference is true AND the [ConnectivityState] is not [ConnectivityState.METERED_AND_RESTRICTED].
     */
    fun isAutoplayVideosOn(): Boolean =
        sharedPrefs.getBoolean(PREF_AUTOPLAY_VIDEOS, true) &&
            !ConnectivityMonitor.getInstance(application.applicationContext).hasDeviceLevelDataRestriction()

    fun getBackgroundSync(): String {
        val defaultValue = application.getString(R.string.pref_background_sync_default)
        return sharedPrefs.getString(PREF_BACKGROUND_SYNC, defaultValue) ?: defaultValue
    }

    fun getBackgroundSyncAsInt(): Int = getBackgroundSync().toInt()

    fun isUseDebugArticleTemplateEnabled(): Boolean = sharedPrefs.getBoolean(PREF_DEBUG_USE_RDS_TEST, false)

    fun isCASettlementDialogShown(): Boolean = sharedPrefs.getBoolean(PREF_CA_SETTLEMENT_SHOWN, false)

    fun setCASettlementDialogShown() {
        sharedPrefs.edit().putBoolean(PREF_CA_SETTLEMENT_SHOWN, true).apply()
    }

    fun canShowTestOptions(): Boolean = sharedPrefs.getBoolean(PREF_SHOW_TEST_OPTIONS, false)

    fun showTestOptions(show: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_SHOW_TEST_OPTIONS, show).apply()
    }

    fun isTestAdsEnabled(): Boolean = canShowTestOptions() && sharedPrefs.getBoolean(PREF_TEST_ADS, false)

    fun getTestAdsValue(): String = sharedPrefs.getString(PREF_TEST_ADS_VALUE, "") ?: ""
}
