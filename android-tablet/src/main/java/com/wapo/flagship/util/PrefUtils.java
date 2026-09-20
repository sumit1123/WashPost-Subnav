package com.wapo.flagship.util;

import static com.wapo.flagship.features.settings.preferences.DebugPanelPreference.FUSION_PROD;
import static com.wapo.flagship.features.settings.preferences.DebugPanelPreference.PROD;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import com.wapo.android.commons.util.Logger;

import androidx.annotation.Nullable;
import androidx.core.app.BuildConfig;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.preferencesapi.models.ContentPacksValueItem;
import com.wapo.flagship.features.preferencesapi.models.SnoozeInfo;
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.newdata.model.DeviceProfile;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems;
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt;
import com.washingtonpost.android.paywall.newdata.response.SubLink;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by muppallav on 10/19/15.
 */
public class PrefUtils {
    public static final String PREF_PRICE_FLAG = "pref.PRICE_FLAG";
    public static final String PREF_PAYWALL_SOURCE = "pref.PAYWALL_SOURCE";
    public static final String PREF_PAYWALL_SUB_SOURCE = "pref.PAYWALL_SUB_SOURCE";
    public static final String PREF_PAYWALL_SUB_SHORT_TITLE = "pref.PAYWALL_SUB_SHORT_TITLE";
    public static final String PREF_ARTICLE_NAV_SECTION_TRACK = "pref.PREF_ARTICLE_NAV_SECTION_TRACK";

    public static final String PREF_FUSION_API_DATA_SOURCE = "pref.PREF_FUSION_API_DATA_SOURCE";
    public static final String PREF_PAGEBUILDER_API_DATA_SOURCE = "pref.PREF_PAGEBUILDER_API_DATA_SOURCE";
    public static final String PREF_FEEDS_API_DATA_SOURCE = "pref.PREF_FEEDS_API_DATA_SOURCE";
    public static final String PREF_SITESERVICE_API_DATA_SOURCE = "pref.PREF_SITESERVICE_API_DATA_SOURCE";
    public static final String PREF_PAYWALL_HIDE = "pref.PREF_PAYWALL_HIDE";
    public static final String PREF_ONETRUST_STAGE = "pref.PREF_ONETRUST_STAGE";
    public static final String PREF_LEAKCANARY_DUMP_HEAP = "pref.PREF_LEAKCANARY_DUMP_HEAP";
    public static final String PREF_AGE_RESTRICTIONS = "pref.PREF_AGE_RESTRICTIONS";
    public static final String PREF_PIP_STATUS = "pref.PREF_PIP_STATUS";
    private static final String PREF_GDPR_CONSENT = "pref.PREF_GDPR_CONSENT";
    private static final String PREF_CLEAR_REGISTRATION_FLAG = "pref.PREF_CLEAR_REGISTRATION_FLAG";
    private static final String PREF_SAVED_ARTICLE_COUNT_SUMO = "pref.PREF_SAVED_ARTICLE_COUNT_SUMO";
    private static final String PREF_PAYWALL_SUB_ATTRIBUTES = "pref.PREF_PAYWALL_SUB_ATTRIBUTES";
    private static final String PREF_PAYWALL_IAP_SUB_ITEMS = "pref.PREF_PAYWALL_IAP_SUB_ITEMS";
    private static final String PREF_ONBAORDING_SCREEN_SHOWN = "pref.PREF_ONBAORDING_SCREEN_SHOWN";
    private static final String PREF_AB_BLOCKER = "pref.PREF_AB_BLOCKER";
    private static final String PREF_USER_SUBS_STATUS = "pref.PRER_USER_SUBS_STATUS";
    private static final String PREF_USER_PRODUCT_ID = "pref.PREF_USER_PRODUCT_ID";
    private static final String PREF_RAINBOW_SUBS_STATUS = "pref.PRER_RAINBOW_SUBS_STATUS";
    private static final String PREF_AMAZON_CLASSIC_SUBS_STATUS = "pref.PRER_AMAZON_CLASSIC_SUBS_STATUS";
    private static final String PREF_PAUSE_TIME = "pref.PREF_PAUSE_TIME";
    private static final String PREF_AUTO_RESUME_TIME = "pref.PREF_AUTO_RESUME_TIME";
    private static final String PREF_SHOULD_VERIFY_PLAY_STORE_RESULT = "pref.PREF_SHOULD_VERIFY_PLAY_STORE_RESULT";
    private static final String PREF_SHOULD_VERIFY_EXTERNAL_PURCHASE_RESULT = "pref.PREF_SHOULD_VERIFY_EXTERNAL_PURCHASE_RESULT";
    private static final String PREF_AB_PARAMETERS_MAP = "pref.PREF_AB_PARAMETERS_MAP";
    private static final String PREF_AMAZON_USER_ID = "pref.AMAZON_USER_ID";
    private static final String PREF_BLOCKER = "pref.BLOCKER";
    private static final String PREF_CONSENT_TOKEN = "pref.CONSENT_TOKEN";
    //This value is being set in rainbow's [WapoSubscriptionProviderHelper] and is retrieved in classic's [WapoSubscriptionProviderHelper]
    //This is set from rainbow on launch of rainbow app
    private static final String PREF_HAS_RAINBOW_APP = "pref.HAS_RAINBOW_APP";
    //This value is being set in rainbow's [WapoSubscriptionProviderHelper] and is retrieved in classic's [WapoSubscriptionProviderHelper]
    //This is set from rainbow when user completes the unification onboarding there.
    private static final String PREF_HAS_MIGRATED_FROM_RAINBOW = "pref.HAS_MIGRATED_FROM_RAINBOW";
    //This value is being set in unification onboarding screen when user clicks the CTA / chooses to sign in with a different account.
    private static final String PREF_USER_ACTED_ON_UNIFICATION_ONBOARDING = "pref.USER_ACTED_ON_UNIFICATION_ONBOARDING";
    //This value is being set when user's IAP has been loaded in classic app from rainbow via content providers.
    private static final String PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW = "pref.PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW";
    //This value is being set when user's account has been loaded in classic app from rainbow via content providers.
    private static final String PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW = "pref.PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW";
    //This is set from Amazon rainbow to identify a user who has migrated.
    private static final String PREF_IS_EXISTING_USER = "pref.is_existing_user";
    private static final String PREF_IS_LWA_MIGRATED = "pref.is_lwa_migrated";

    private static final String PREF_VERIFY_ON_EACH_LAUNCH = "pref.verify_on_each_launch";

    private static final String PREF_SHOW_ALERTS_ONBOARDING = "pref.show_alerts_onboarding";
    private static final String PREF_SHOW_CONTENT_PACKS_ONBOARDING = "pref.show_content_packs_onboarding";
    private static final String PREF_SHOW_AUDIO_ONBOARDING = "pref.show_audio_onboarding";

    private static final String PREF_SELECTED_CONTENT_PACKS = "pref.selected_content_packs";

    private static final String PREF_LAST_CONTENT_PACK_ITEMS_FETCH_TIME = "pref.last_content_pack_items_fetch_time";

    private static final String PREF_SUB_LINK_STATUS = "pref.sub_link_status";

    private static final String PREF_SUB_LINK_MESSAGE = "pref.sub_link_message";
    private static String PREF_HAS_AMAZON_CLASSIC = "pref.has_amazon_classic";
    private static String PREF_HAS_MIGRATED_FROM_AMAZON_CLASSIC = "pref.has_migrated_from_amazon_classic";
    private static String PREF_HAS_MIGRATED_ALERTS_FROM_AMAZON_CLASSIC = "pref.has_migrated_alerts_from_amazon_classic";
    //This value is being set in unification onboarding screen when user clicks the CTA / chooses to sign in with a different account.
    private static final String PREF_USER_ACTED_ON_AMAZON_UNIFICATION_ONBOARDING = "pref.USER_ACTED_ON_UNIFICATION_ONBOARDING";
    //This value is being set when user's IAP has been loaded in unified app from amazon classic via content providers.
    private static final String PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC = "pref.PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC";
    //This value is being set when user's account has been loaded in unified app from amazon classic via content providers.
    private static final String PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC = "pref.PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC";
    private static final String PREF_USER_ACTED_ON_DUPLICATE_SUBS_NOTICE = "pref.PREF_USER_ACTED_ON_DUPLICATE_SUBS_NOTICE";
    private static final String PREF_TOPIC_NOTIFICATIONS_INITIAL_SYNC = "pref.PREF_TOPIC_NOTIFICATIONS_INITIAL_SYNC";
    private static final String PREF_CHECK_PREF_LMT = "pref.PREF_CHECK_PREF_LMT";
    private static final String PREF_CONTENT_PACKS_CHECK_PREF_LMT = "pref.PREF_CONTENT_PACKS_CHECK_PREF_LMT";
    private static final String PREF_CONTEXT_PACKS_LAST_MODIFIED = "pref.PREF_CONTEXT_PACKS_LAST_MODIFIED";
    private static final String PREF_FIND_ONBOARDING_SEEN = "pref.PREF_FIND_ONBOARDING_SEEN";
    private static final String PREF_SEEN_FEATURE_ONBOARDING_IDS = "pref.PREF_SEEN_FEATURE_ONBOARDING_IDS";

    private static final String PREF_NEWSPRINT_ATTRIBUTES_CHECK_PREF_LMT = "pref.PREF_NEWSPRINT_ATTRIBUTES_CHECK_PREF_LMT";
    private static final String PREF_NEWSPRINT_STATE_CHECK_PREF_LMT = "pref.PREF_NEWSPRINT_STATE_CHECK_PREF_LMT";
    private static final String PREF_NEWSPRINT_HAS_VISITED_NEWSPRINT_SECTION = "pref.PREF_NEWSPRINT_HAS_VISITED_NEWSPRINT_SECTION";
    private static final String PREF_WALL_DISMISSAL_CHECK_PREF_LMT = "pref.PREF_WALL_DISMISSAL_CHECK_PREF_LMT";
    private static final String PREF_WALL_DISMISSAL = "pref.PREF_WALL_DISMISSAL";
    private static final String PREF_TOPIC_NOTIFICATIONS_CHECK_PREF_LMT = "pref.PREF_TOPIC_NOTIFICATIONS_CHECK_PREF_LMT";
    private static final String PREF_PRICING_VARIANT_OVERRIDE = "pref.PRICING_VARIANT_OVERRIDE";
    private static final String PREF_TALK_TO_THE_POST_FEEDBACK_LAST_SEEN = "pref.TALK_TO_THE_POST_FEEDBACK_LAST_SEEN";
    private static final String PREF_TALK_TO_THE_POST_FEEDBACK_SUBMITTED = "pref.TALK_TO_THE_POST_FEEDBACK_SUBMITTED";
    private static final String PREF_LAST_VISITED_BOTTOM_TAB = "pref.LAST_VISITED_BOTTOM_TAB";
    private static final String PREF_TALK_TO_THE_POST_PULSE_SHOWN = "pref.TALK_TO_THE_POST_PULSE_SHOWN";
    private static final String PREF_TALK_TO_THE_POST_VOICE_SELECTION = "pref.TALK_TO_THE_POST_VOICE_SELECTION";
    private static final String PREF_TALK_TO_THE_POST_VISIT_COUNT = "pref.TALK_TO_THE_POST_VISIT_COUNT";
    private static final String PREF_TALK_TO_THE_POST_CAPTIONS_ENABLED = "pref.TALK_TO_THE_POST_CAPTIONS_ENABLED";

    private static final String PREF_PROXY_API_HIT_COUNT = "pref.PROXY_API_HIT_COUNT";


    private static Set<String> toSet(Map<String, String> map) {
        Set<String> set = null;
        if (map != null && !map.isEmpty()) {
            set = new HashSet<>(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                set.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        return set;
    }

    public static int getProxyApiHitCount(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(PREF_PROXY_API_HIT_COUNT, 0);
    }

    public static boolean incrementProxyApiHitCount(Context context, int hitLimit) {
        synchronized (PrefUtils.class) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            int current = sharedPreferences.getInt(PREF_PROXY_API_HIT_COUNT, 0);
            if (hitLimit > 0 && current >= hitLimit) {
                return false;
            }
            sharedPreferences.edit()
                    .putInt(PREF_PROXY_API_HIT_COUNT, current + 1)
                    .apply();
            return true;
        }
    }

    private static Map<String, String> fromSet(Set<String> set) {
        Map<String, String> map = new HashMap<>();
        if (set != null && !set.isEmpty()) {
            for (String s : set) {
                String[] pair = s.split(":");
                if (pair.length == 2) {
                    map.put(pair[0], pair[1]);
                }
            }
        }
        return map;
    }

    /**
     * This is set when user has successfully migrated their account via content providers from rainbow app.
     * @param migratedAccount - true if migrated
     */
    public static void setUserMigratedAccountFromRainbow(Context context, boolean migratedAccount){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW, migratedAccount);
        editor.apply();
    }

    /**
     * @return true if migrated
     */
    public static boolean hasUserMigratedAccountFromRainbow(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_MIGRATED_ACCOUNT_FROM_RAINBOW, false);
    }

    /**
     * This is set when user has successfully migrated their IAP via content providers from rainbow app.
     * @param migratedIAP - true if migrated
     */
    public static void setUserMigratedIAPFromRainbow(Context context, boolean migratedIAP){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW, migratedIAP);
        editor.apply();
    }

    /**
     * @return true if migrated
     */
    public static boolean hasUserMigratedIAPFromRainbow(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_MIGRATED_IN_APP_SUB_FROM_RAINBOW, false);
    }


    /**
     * Set this value when user clicks the CTA/sign-in link on unification onboarding screen
     * @param userActed - true or false depending on the condition above.
     */
    public static void setUserActedOnUnificationOnboarding(Context context, boolean userActed) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_ACTED_ON_UNIFICATION_ONBOARDING, userActed);
        editor.apply();
    }

    /**
     * This value is mainly used to prevent showing on-boarding screen to the user if they have already
     * acted on it e.g. clicking the CTA/sign-in link.
     */
    public static boolean hasUserActedOnAmazonUnificationOnboardingScreen(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_ACTED_ON_AMAZON_UNIFICATION_ONBOARDING, false);
    }

    /**
     * Set this value when user clicks the CTA/sign-in link on unification onboarding screen
     * @param userActed - true or false depending on the condition above.
     */
    public static void setUserActedOnAmazonUnificationOnboarding(Context context, boolean userActed) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_ACTED_ON_AMAZON_UNIFICATION_ONBOARDING, userActed);
        editor.apply();
    }

    /**
     * This value is mainly used to prevent showing duplicate subscriptions screen (amazon unification only)
     * to the user if they have already acted on it.
     */
    public static boolean hasUserActedOnDuplicateSubscriptionsScreen(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_ACTED_ON_DUPLICATE_SUBS_NOTICE, false);
    }

    /**
     * Set this value when user clicks on manage subscription / continue in duplicate subscriptions screen (amazon unification only)
     * @param userActed - true or false depending on the condition above.
     */
    public static void setUserActedOnDuplicateSubscriptionsScreen(Context context, boolean userActed) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_ACTED_ON_DUPLICATE_SUBS_NOTICE, userActed);
        editor.apply();
    }

    /**
     * This value is mainly used to prevent showing on-boarding screen to the user if they have already
     * acted on it e.g. clicking the CTA/sign-in link.
     */
    public static boolean hasUserActedOnUnificationOnboardingScreen(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_ACTED_ON_UNIFICATION_ONBOARDING, false);
    }


    /**
     * Set this value to true from [WapoSubscriptionProviderHelper] if the the value read via content providers from
     * the rainbow app is 1. Otherwise set it to false.
     * @param hasRainbow - true or false depending on the condition above.
     */
    public static void setHasRainbow(Context context, boolean hasRainbow) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_HAS_RAINBOW_APP, hasRainbow);
        editor.apply();
    }

    /**
     * This value is mainly used in analytics events so that analytics team can query how many users have
     * rainbow and how many have migrated already.
     */
    public static boolean getHasRainbow(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_HAS_RAINBOW_APP, false);
    }

    /**
     * This value is set when user has successfully completed the unification off-boarding from rainbow and landed on classic app
     * (either from play store or already installed)
     * @param hasMigratedFromRainbow - This can be "true", "false". The default case is "na" means migration does not apply to these users.
     */
    public static void setHasMigratedFromRainbow(Context context, String hasMigratedFromRainbow) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_HAS_MIGRATED_FROM_RAINBOW, hasMigratedFromRainbow);
        editor.apply();
    }

    /**
     * This value is mainly used in analytics events so that analytics team can query how many users have
     * rainbow and how many have migrated already.
     * Please note that migrated only means that they came into the classic app from rainbow app. This does not mean that
     * their login creds. /in-app subscription is carried over to classic.
     */
    public static String getHasMigratedFromRainbow(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_HAS_MIGRATED_FROM_RAINBOW, "na");
    }

    public static Boolean getIsExistingUser(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_IS_EXISTING_USER, false);
    }

    /**
     * This is set when user has successfully migrated their account via content providers from amazon classic app.
     * @param migratedAccount - true if migrated
     */
    public static void setUserMigratedAccountFromAmazonClassic(Context context, boolean migratedAccount){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC, migratedAccount);
        editor.apply();
    }

    /**
     * @return true if migrated
     */
    public static boolean hasUserMigratedAccountFromAmazonClassic(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_MIGRATED_ACCOUNT_FROM_AMAZON_CLASSIC, false);
    }

    /**
     * This is set when user has successfully migrated their IAP via content providers from amazon classic app.
     * @param migratedIAP - true if migrated
     */
    public static void setUserMigratedIAPFromAmazonClassic(Context context, boolean migratedIAP){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC, migratedIAP);
        editor.apply();
    }

    /**
     * @return true if migrated
     */
    public static boolean hasUserMigratedIAPFromAmazonClassic(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_USER_MIGRATED_IN_APP_SUB_FROM_AMAZON_CLASSIC, false);
    }

    /**
     * Set this value to true from [WapoSubscriptionProviderHelper] if the the value read via content providers from
     * the amazon classic app is 1. Otherwise set it to false.
     * @param hasAmazonClassic - true or false depending on the condition above.
     */
    public static void setHasAmazonClassic(Context context, boolean hasAmazonClassic) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_HAS_AMAZON_CLASSIC, hasAmazonClassic);
        editor.apply();
    }

    /**
     * This value is mainly used in analytics events so that analytics team can query how many users have
     * amazon classic and how many have migrated already.
     */
    public static boolean getHasAmazonClassic(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_HAS_AMAZON_CLASSIC, false);
    }

    /**
     * This value is set when user has successfully completed the unification off-boarding from amazon classic and landed on unified app
     * (either from amazon app store or already installed)
     * @param hasMigratedFromAmazonClassic - This can be "true", "false". The default case is "na" means migration does not apply to these users.
     */
    public static void setHasMigratedFromAmazonClassic(Context context, String hasMigratedFromAmazonClassic) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_HAS_MIGRATED_FROM_AMAZON_CLASSIC, hasMigratedFromAmazonClassic);
        editor.apply();
    }

    /**
     * This value is mainly used in analytics events so that analytics team can query how many users have
     * rainbow and how many have migrated already.
     * Please note that migrated only means that they came into the unified app from amazon classic app. This does not mean that
     * their login creds. /in-app subscription is carried over to classic.
     */
    public static String getHasMigratedFromAmazonClassic(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_HAS_MIGRATED_FROM_AMAZON_CLASSIC, "na");
    }

    public static void setHasMigratedAlertsFromAmazonClassic(Context context, boolean hasMigratedAlertsFromAmazonClassic) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_HAS_MIGRATED_ALERTS_FROM_AMAZON_CLASSIC, hasMigratedAlertsFromAmazonClassic);
        editor.apply();
    }

    public static boolean getHasMigratedAlertsFromAmazonClassic(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_HAS_MIGRATED_ALERTS_FROM_AMAZON_CLASSIC, false);
    }

    private static final String PREF_HAS_RENAMED_RAINBOW_DATABASE = "pref.HAS_RENAMED_RAINBOW_DATABASE";
    private static final String PREF_HAS_MIGRATED_RAINBOW_SAVED_STORIES = "pref.HAS_MIGRATED_RAINBOW_SAVED_STORIES";
    private static final String PREF_IS_APP_LAUNCHED = "pref.IS_APP_EVER_LAUNCHED";
    private static final String PREF_APP_LAUNCHED_COUNT = "pref.launched_count";

    private static final String PREF_DEBUG_REMAINING_DAYS_FREE = "pref.PREF_DEBUG_REMAINING_DAYS_FREE";
    private static final String PREF_SIX_MONTHS_EXPIRY = "pref.PREF_SIX_MONTHS_EXPIRY";
    private static final String PREF_DEVICE_PROFILE = "pref.PREF_DEVICE_PROFILE";
    private static final String PREF_AMAZON_FT_LOGIN_PROMO_ARTICLE_CONSUMED = "pref.PREF_AMAZON_FT_LOGIN_PROMO_ARTICLE_CONSUMED";
    private static final String PREF_IS_LWA_SENT="pref.PREF_IS_LWA_SENT";
    private static final String PREF_SIX_MONTHS_SUBSCRIPTION_DETAILS = "pref.PREF_SIX_MONTHS_SUBSCRIPTION_DETAILS";
    private static final String PREF_PURCHASE_DATE = "pref.PURCHASE_DATE";
    private static final String PREF_ALL_RECEIPTS = "pref.PREF_ALL_RECEIPTS";
    private static final String PREF_SUB_ACCOUNT_ANALYTICS = "pref.PREF_SUB_ACCOUNT_ANALYTICS";
    private static final String MAP_LAST_DISPLAY_TIME = "pref.MAP_LAST_DISPLAY_TIME";
    private static final String ALLOW_MAP_ON_EVERY_ARTICLE = "pref.TRIGGER_MAP_ON_EVERY_ARTICLE";
    private static final String MAP_OVERRIDE_SNOOZE_TIME = "pref.MAP_OVERRIDE_SNOOZE_TIME";
    public static final String MAP_SNOOZE_COUNT = "pref.MAP_SNOOZE_COUNT";
    public static final String MAP_RESET_SNOOZE_COUNT = "pref.MAP_RESET_SNOOZE_COUNT";
    public static final String PREF_PUSH_TYPE = "pref.PREF_PUSH_TYPE";
    public static final String PREF_PUSH_TEST_GROUP = "pref.PREF_PUSH_TEST_GROUP";
    public static void setBlocker(Context context, String name) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_BLOCKER, name);
        editor.apply();
    }

    public static String getBlocker(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Unified app - Amazon and Playstore will have two different paywalls
        String defValue = Utils.isAmazonBuild() ? PaywallConstants.WALL_NAME_AMAZON_MAIN : PaywallConstants.WALL_NAME_MAIN;
        if (Utils.isAmazonBuild() && sharedPreferences.getString(PREF_BLOCKER, defValue).equals(PaywallConstants.WALL_NAME_MAIN)) {
            return PaywallConstants.WALL_NAME_AMAZON_MAIN;
        }
        return sharedPreferences.getString(PREF_BLOCKER, defValue);
    }

    public static void setPrefPriceFlag(Context context, String priceFlag) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PRICE_FLAG, priceFlag);
        editor.apply();
    }

    public static String getPrefPriceFlag(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PRICE_FLAG, null);
    }

    public static void setPrefPaywallSource(Context context, String source) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PAYWALL_SOURCE, source);
        editor.apply();
    }

    public static String getPrefPaywallSource(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PAYWALL_SOURCE, null);
    }

    public static void setPrefPaywallSubSource(Context context, String subSource) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PAYWALL_SUB_SOURCE, subSource);
        editor.apply();
    }

    public static String getPrefPaywallSubSource(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PAYWALL_SUB_SOURCE, null);
    }

    public static void setPrefPaywallSubShortTitle(Context context, String shortTitle) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PAYWALL_SUB_SHORT_TITLE, shortTitle);
        editor.apply();
    }

    public static String getPrefPaywallSubShortTitle(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PAYWALL_SUB_SHORT_TITLE, null);
    }

    public static void setPrefPaywallSubAttributes(Context context, @Nullable Map<String, String> subAttributes) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(PREF_PAYWALL_SUB_ATTRIBUTES, toSet(subAttributes));
        editor.apply();
    }

    @Nullable
    public static Set<String> getPrefPaywallSubAttributes(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getStringSet(PREF_PAYWALL_SUB_ATTRIBUTES, null);
    }

    public static void setPrefPaywallIAPSubItems(Context context, IAPSubItems iapSubItems) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            editor.putString(PREF_PAYWALL_IAP_SUB_ITEMS, new Gson().toJson(iapSubItems, IAPSubItems.class));
        } catch (Exception e) {
            Logger.e("PrefUtils", "Error writing iapSubItems to SharedPreferences:" + e.getMessage());
        }
        editor.apply();
    }

    public static IAPSubItems getPrefPaywallIAPSubItems(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String data = sharedPreferences.getString(PREF_PAYWALL_IAP_SUB_ITEMS, null);
        Type type = new TypeToken<IAPSubItems>(){}.getType();
        IAPSubItems iapSubItems;
        try {
            iapSubItems = new Gson().fromJson(data, type);
        } catch (Exception e) {
            setPrefPaywallIAPSubItems(context, null);
            EventLog.Builder builder = new EventLog.Builder();
            builder.setMessage("IAPSubItem Parsing Error")
                    .setModule(LogModules.PAYWALL)
                    .setErrorMessage(e.getMessage())
                    .set("cause", data);
            RemoteLog.e(context, builder.build());
            iapSubItems = null;
        }
        if (iapSubItems == null) {
            iapSubItems = new IAPSubItems();
        }
        return iapSubItems;
    }

    public static void setTrackedSectionDuringUserNav(Context context, String sectionName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_ARTICLE_NAV_SECTION_TRACK, sectionName);
        editor.apply();
    }

    public static String getTrackedSectionDuringUserNav(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_ARTICLE_NAV_SECTION_TRACK, null);
    }

    public static void setFusionAPIDataSource(Context context, String dataSource) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_FUSION_API_DATA_SOURCE, dataSource);
        editor.apply();
    }

    public static String getFusionAPIDataSource(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_FUSION_API_DATA_SOURCE, FUSION_PROD);
    }

    public static void setPageBuilderAPIDataSource(Context context, String dataSource) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PAGEBUILDER_API_DATA_SOURCE, dataSource);
        editor.apply();
    }

    public static String getPageBuilderAPIDataSource(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PAGEBUILDER_API_DATA_SOURCE, PROD);
    }

    public static String getFeedsAPIDataSource(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_FEEDS_API_DATA_SOURCE, PROD);
    }

    public static void setFeedsAPIDataSource(Context context, String dataSource) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_FEEDS_API_DATA_SOURCE, dataSource);
        editor.apply();
    }

    public static void setSiteServiceAPIDataSource(Context context, String dataSource) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_SITESERVICE_API_DATA_SOURCE, dataSource);
        editor.apply();
    }

    public static String getSiteServiceAPIDataSource(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_SITESERVICE_API_DATA_SOURCE, PROD);
    }

    public static void setPrefPaywallHide(Context context, Boolean hidePaywall) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_PAYWALL_HIDE, hidePaywall);
        editor.apply();
    }

    public static Boolean getPrefPaywallHide(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_PAYWALL_HIDE, false);
    }

    public static void setPrefOneTrustStage(Context context, Boolean useStage) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_ONETRUST_STAGE, useStage);
        editor.apply();
    }

    public static Boolean getPrefOneTrustStage(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_ONETRUST_STAGE, false);
    }


    public static boolean getPIPEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ((AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE)).checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), context.getPackageName())
                    == AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }

    public static void setPIPEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_PIP_STATUS, getPIPEnabled(context));
        editor.apply();
    }

    public static void setLastSavedArticleCountLogTime(Context context, long time) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_SAVED_ARTICLE_COUNT_SUMO, time);
        editor.apply();
    }

    public static long getLastSavedArticleCountLogTime(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_SAVED_ARTICLE_COUNT_SUMO, 0);
    }

    public static boolean isOnboardingScreenShown(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_ONBAORDING_SCREEN_SHOWN, false);
    }

    public static void setOnboardingScreenShown(Context context, boolean onboardingShown){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_ONBAORDING_SCREEN_SHOWN, onboardingShown);
        editor.apply();
    }

    /**
     * to know Renaming Rainbow Database is done or not done or not applicable.
     * @param context
     * @return "false" (default) - renaming is not done yet.
     *         "true" - renaming has been done.
     *         "na"   - not applicable. (No existing Rainbow Database files / Fresh Install case)
     */
    public static String hasRenamedRainbowDatabase(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_HAS_RENAMED_RAINBOW_DATABASE, "false");
    }

    /**
     * [{@link com.wapo.flagship.features.amazonunification.MigrationHelper}] sets value
     * @param context
     * @param value - "true" or "false" or "na"
     */
    public static void renamedRainbowDatabase(Context context, String value){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_HAS_RENAMED_RAINBOW_DATABASE, value);
        editor.apply();
    }

    /**
     * to know Rainbow SavedStories migration is done or not
     * @param context
     * @return false (default) - migration is not done yet, otherwise true.
     */
    public static boolean hasMigratedRainbowSavedStories(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_HAS_MIGRATED_RAINBOW_SAVED_STORIES, false);
    }

    public static void migratedRainbowSavedStories(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_HAS_MIGRATED_RAINBOW_SAVED_STORIES, true);
        editor.apply();
    }

    public static void saveABVariantBlocker(Context context, String type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_AB_BLOCKER, type);
        editor.apply();
    }


    public static void saveABParametersMap(Context context, Map<String, String> airshipMap) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(PREF_AB_PARAMETERS_MAP, toSet(airshipMap));
        editor.apply();
    }

    public static Map<String, String> getABParametersMap(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return fromSet(sharedPreferences.getStringSet(PREF_AB_PARAMETERS_MAP, new HashSet<>()));
    }

    public static String getUserSubscriptionStatus(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_USER_SUBS_STATUS, "");
    }

    public static void setUserSubscriptionStatus(Context context, String status) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_USER_SUBS_STATUS, status);
        editor.apply();
    }

    public static String getRainbowSubscriptionStatus(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_RAINBOW_SUBS_STATUS, "");
    }

    public static void setUserProductId(Context context, String sku) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_USER_PRODUCT_ID, sku);
        editor.apply();
    }

    public static String getUserProductId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_USER_PRODUCT_ID, "");
    }

    public static long getPauseTime(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_PAUSE_TIME, 0L);
    }

    public static void setPauseTime(Context context, long pauseTimeMillis) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_PAUSE_TIME, pauseTimeMillis);
        editor.apply();
    }

    public static long getAutoResumeTime(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_AUTO_RESUME_TIME, 0L);
    }

    public static void setAutoResumeTime(Context context, long autoResumeTimeMillis) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_AUTO_RESUME_TIME, autoResumeTimeMillis);
        editor.apply();
    }

    public static boolean getShouldVerifyPlayStoreResult(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_SHOULD_VERIFY_PLAY_STORE_RESULT, false);
    }

    public static void setShouldVerifyPlayStoreResult(Context context, boolean shouldVerify) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_SHOULD_VERIFY_PLAY_STORE_RESULT, shouldVerify);
        editor.apply();
    }

    public static boolean getShouldVerifyExternalPurchaseResult(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_SHOULD_VERIFY_EXTERNAL_PURCHASE_RESULT, false);
    }

    public static void setShouldVerifyExternalPurchaseResult(Context context, boolean shouldVerify) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_SHOULD_VERIFY_EXTERNAL_PURCHASE_RESULT, shouldVerify);
        editor.apply();
    }

    public static void setRainbowSubscriptionStatus(Context context, String status) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_RAINBOW_SUBS_STATUS, status);
        editor.apply();
    }

    public static String getAmazonClassicSubscriptionStatus(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_AMAZON_CLASSIC_SUBS_STATUS, "");
    }

    public static void setAmazonClassicSubscriptionStatus(Context context, String status) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_AMAZON_CLASSIC_SUBS_STATUS, status);
        editor.apply();
    }


    public static String getPrefAmazonUserId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_AMAZON_USER_ID, null);
    }

    public static void setPrefAmazonUserId(Context context, String status) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_AMAZON_USER_ID, status);
        editor.apply();
    }

    public static Integer getDebugRemainingDaysFree(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.contains(PREF_DEBUG_REMAINING_DAYS_FREE)) {
            return sharedPreferences.getInt(PREF_DEBUG_REMAINING_DAYS_FREE, 0);
        } else {
            return null;
        }
    }

    public static void setDebugRemainingDaysFree(Context context, Integer numDays) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (numDays != null) {
            editor.putInt(PREF_DEBUG_REMAINING_DAYS_FREE, numDays);
        } else {
            editor.remove(PREF_DEBUG_REMAINING_DAYS_FREE);
        }
        editor.apply();
    }

    public static void setPrefFreeTrialExpiry(Context context, long exp) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_SIX_MONTHS_EXPIRY, exp);
        editor.apply();
    }

    public static long getFreeTrialExpiryDate(Context context) {
        if (BuildConfig.DEBUG) {
            Integer remainingDays = getDebugRemainingDaysFree(context);
            if (remainingDays != null) {
                return System.currentTimeMillis() + (remainingDays * DateUtils.DAY_IN_MILLIS);
            }
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_SIX_MONTHS_EXPIRY, 0l);
    }

    public static void saveDeviceProfile(Context context, DeviceProfile profile) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_DEVICE_PROFILE, new Gson().toJson(profile));
        editor.apply();
    }

    public static DeviceProfile getDeviceProfile(Context context, Class<? extends DeviceProfile> deviceProfileType) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceProfileData = sharedPreferences.getString(PREF_DEVICE_PROFILE, null);
        if (deviceProfileData == null) return null;
        return new Gson().fromJson(deviceProfileData, deviceProfileType);
    }

    public static void setPrefDeviceProfileSent(Context context, boolean isSent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_IS_LWA_SENT, isSent);
        editor.apply();
    }

    public static boolean isDeviceProfileSent(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_IS_LWA_SENT, false);
    }

    public static void setPushData(Context context, String pushType, String testGroup, String url, String pushId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PUSH_TYPE, pushType);
        editor.putString(PREF_PUSH_TEST_GROUP, testGroup);
        editor.putString(pushId, url);
        editor.apply();
    }

    public static String getTestGroup(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PUSH_TEST_GROUP, "");
    }

    public static String getPushType(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PUSH_TYPE, "");
    }

    public static String getPushUrl(Context context, String pushId) {
        if (pushId == null || pushId.trim().isEmpty()) {
            return "";
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(pushId, "");
    }

    public static void saveFreeTrialSub(String freeTrialSubscriptionString) {
        SharedPreferences sharedPreferences = PaywallService.getInstance().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_SIX_MONTHS_SUBSCRIPTION_DETAILS, freeTrialSubscriptionString);
        editor.apply();
    }

    public static String getFreeTrialSub() {
        return PaywallService.getInstance().getSharedPreferences().getString(PREF_SIX_MONTHS_SUBSCRIPTION_DETAILS, null);
    }

    public static void setFreeTrialStartTime(Context context, long time) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_PURCHASE_DATE, time);
        editor.apply();
    }

    /**
     * Currently used only for Amazon to store all IAP receipts
     * @param context
     * @param receipts
     */
    public static void savePrefAllReceipts(Context context, List<StoreReceipt> receipts) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_ALL_RECEIPTS, new Gson().toJson(receipts));
        editor.apply();
    }

    /**
     * Currenlty used only for Amazon to get all IAP receipts
     * @param context
     * @return
     */
    public static List<StoreReceipt> getPrefAllReceipts(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String storeReceiptData = sharedPreferences.getString(PREF_ALL_RECEIPTS, null);
        if(storeReceiptData == null) return null;
        try {
            java.lang.reflect.Type type = new TypeToken<List<StoreReceipt>>(){}.getType();
            return new Gson().fromJson(storeReceiptData, type);
        } catch (Exception e) {
            //ignore exception, will get new receipt info next app launch
        }
        return null;
    }

    public static String getPrefConsentToken(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_CONSENT_TOKEN, "");
    }

    public static void setPrefConsentToken(Context context, String groups){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_CONSENT_TOKEN, groups);
        editor.apply();
    }

    public static void setIsLwaMigrated(Context context, Boolean type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_IS_LWA_MIGRATED, type);
        editor.apply();
    }

    public static boolean getIsLwaMigrated(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_IS_LWA_MIGRATED, false);
    }

    /**
     * DEBUG ONLY : This will be set in debug panel to allow verify to be called on every app launch
     * instead of every 24 hrs. This will allow for testing Grace Period and On Hold with Playstore IAP
     * license testing.
     */
    public static void setVerifyOnEachLaunch(Context context, Boolean type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_VERIFY_ON_EACH_LAUNCH, type);
        editor.apply();
    }

    /**
     * DEBUG ONLY : This will allow for testing Grace Period and On Hold with Playstore IAP
     * license testing.
     */
    public static boolean getVerifyOnEachLaunch(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_VERIFY_ON_EACH_LAUNCH, false);
    }

    @Nullable
    public static List<ContentPacksValueItem> getSelectedContentPacks(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sharedPreferences.getString(PREF_SELECTED_CONTENT_PACKS, "");
        if (!json.equals("")) {
            Gson gson = new Gson();
            try {
                Type type = new TypeToken<List<ContentPacksValueItem>>() {}.getType();
                return gson.fromJson(json, type);
            } catch(Exception e) {
                setSelectedContentPacks(context, Collections.emptyList());
                EventLog.Builder builder = new EventLog.Builder();
                builder.setMessage("ContentPacksValueItem Parsing Error")
                        .setModule(LogModules.PREFERENCES)
                        .setErrorMessage(e.getMessage())
                        .set("cause", json);
                RemoteLog.e(context, builder.build());
            }

        }
        return null;
    }

    public static void setSelectedContentPacks(Context context, List<ContentPacksValueItem> contentPacks) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(contentPacks);
        editor.putString(PREF_SELECTED_CONTENT_PACKS, json);
        editor.apply();
    }

    public static void setLastContentPackItemsFetch(Context context, long time) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_CONTENT_PACK_ITEMS_FETCH_TIME, time);
        editor.apply();
    }

    public static long getLastContentPackItemsFetch(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_LAST_CONTENT_PACK_ITEMS_FETCH_TIME, 0L);
    }

    public static void setShouldShowAlertsOnboarding(Context context, Boolean type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_SHOW_ALERTS_ONBOARDING, type);
        editor.apply();
    }

    public static boolean shouldShowAlertsOnboarding(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_SHOW_ALERTS_ONBOARDING, true);
    }

    public static void setShouldShowContentPacksOnboarding(Context context, Boolean type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_SHOW_CONTENT_PACKS_ONBOARDING, type);
        editor.apply();
    }

    public static boolean shouldShowContentPacksOnboarding(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_SHOW_CONTENT_PACKS_ONBOARDING, true);
    }

    public static void setShouldShowAudioOnboarding(Context context, Boolean type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_SHOW_AUDIO_ONBOARDING, type);
        editor.apply();
    }

    public static boolean shouldShowAudioOnboarding(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_SHOW_AUDIO_ONBOARDING, true);
    }

    public static void setSubscriptionLinkResult(Context context, @Nullable SubLink link) {
        if (link == null) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_SUB_LINK_STATUS, link.getStatus());
        editor.putString(PREF_SUB_LINK_MESSAGE, link.getMessage());
        editor.apply();
    }

    public static @Nullable String getSubscriptionLinkStatus(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_SUB_LINK_STATUS, null);
    }

    public static @Nullable String getSubscriptionLinkMessage(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_SUB_LINK_MESSAGE, null);
    }

    public static void setHasSyncedTopicNotifications(Context context, boolean hasSynced) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_TOPIC_NOTIFICATIONS_INITIAL_SYNC, hasSynced);
        editor.apply();
    }

    public static boolean getHasSyncedTopicNotifications(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_TOPIC_NOTIFICATIONS_INITIAL_SYNC, false);
    }

    public static Boolean getPrefLeakCanaryDumpHeap(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_LEAKCANARY_DUMP_HEAP, false);
    }

    public static void setLeakCanaryDumpHeap(Context context, Boolean enabled) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_LEAKCANARY_DUMP_HEAP, enabled);
        editor.apply();
    }

    @Nullable
    public static AgeRestrictionsFakeSetUp getAgeRestrictionPref(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String result = sharedPreferences.getString(PREF_AGE_RESTRICTIONS, null);
        Gson gson = new Gson();
        return gson.fromJson(result, AgeRestrictionsFakeSetUp.class);
    }

    public static void setAgeRestrictionPref(Context context, AgeRestrictionsFakeSetUp ageRestrictionsFakeSetUp) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        editor.putString(PREF_AGE_RESTRICTIONS, gson.toJson(ageRestrictionsFakeSetUp));
        editor.apply();
    }
    public static void setCheckPrefLmt(Context context, long lmt) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_CHECK_PREF_LMT, lmt);
        editor.apply();
    }

    public static long getCheckPrefLmt(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_CHECK_PREF_LMT, 0L);
    }

    public static void setContentPacksLmt(Context context, long lmt) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_CONTENT_PACKS_CHECK_PREF_LMT, lmt);
        editor.apply();
    }

    public static long getContentPacksLmt(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_CONTENT_PACKS_CHECK_PREF_LMT, 0L);
    }

    public static void setNewsprintAttributesLmt(Context context, long lmt) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_NEWSPRINT_ATTRIBUTES_CHECK_PREF_LMT, lmt);
        editor.apply();
    }

    public static long getNewsprintAttributesLmt(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_NEWSPRINT_ATTRIBUTES_CHECK_PREF_LMT, 0L);
    }

    public static void setNewsprintStateLmt(Context context, long lmt) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_NEWSPRINT_STATE_CHECK_PREF_LMT, lmt);
        editor.apply();
    }

    public static long getNewsprintStateLmt(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_NEWSPRINT_STATE_CHECK_PREF_LMT, 0L);
    }

    // Tracks whether user has visited the Newsprint section front
    public static void setNewsprintHasVisitedSection(Context context, boolean hasVisitedNewsprint) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_NEWSPRINT_HAS_VISITED_NEWSPRINT_SECTION, hasVisitedNewsprint);
        editor.apply();
    }

    // Tracks whether user has visited the Newsprint section front
    public static boolean getNewsprintHasVisitedSection(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_NEWSPRINT_HAS_VISITED_NEWSPRINT_SECTION, false);
    }

    @Nullable
    public static String getContentPacksLastModified(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_CONTEXT_PACKS_LAST_MODIFIED, null);
    }

    public static void setContentPacksLastModified(Context context, String date) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_CONTEXT_PACKS_LAST_MODIFIED, date);
        editor.apply();
    }

    public static List<String> getSeenFeatureOnboardingIds(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sharedPreferences.getString(PREF_SEEN_FEATURE_ONBOARDING_IDS, "");
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static void addSeenFeatureOnboardingId(Context context, String onboardingId) {
        if (onboardingId != null) {
            List<String> seenFeatureOnboardingIds = getSeenFeatureOnboardingIds(context);
            if (!seenFeatureOnboardingIds.contains(onboardingId)) {
                seenFeatureOnboardingIds.add(onboardingId);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Gson gson = new Gson();
                String json = gson.toJson(seenFeatureOnboardingIds);
                editor.putString(PREF_SEEN_FEATURE_ONBOARDING_IDS, json);
                editor.apply();
            }
        }
    }

    public static String getPrefSubAccountAnalytics(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_SUB_ACCOUNT_ANALYTICS, null);
    }

    public static void setPrefSubAccountAnalytics(Context context, String subAccountAnalytics) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_SUB_ACCOUNT_ANALYTICS, subAccountAnalytics);
        editor.apply();
    }

    public static void setMapLastDisplayTime(Context context, Long timeInSeconds) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(MAP_LAST_DISPLAY_TIME, timeInSeconds);
        editor.apply();
    }

    public static Long getMapLastDisplayTime(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(MAP_LAST_DISPLAY_TIME, 0L);
    }

    public static void setAllowMapOnEveryArticle(Context context, Boolean allowOnEveryArticle) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(ALLOW_MAP_ON_EVERY_ARTICLE, allowOnEveryArticle);
        editor.apply();
    }

    public static Boolean getAllowMapOnEveryArticle(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(ALLOW_MAP_ON_EVERY_ARTICLE, false);
    }

    public static void setOverrideMapSnoozeTime(Context context, Boolean overrideTo30Seconds) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MAP_OVERRIDE_SNOOZE_TIME, overrideTo30Seconds);
        editor.apply();
    }

    public static Boolean getOverrideMapSnoozeTime(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(MAP_OVERRIDE_SNOOZE_TIME, false);
    }

    public static void updateMapSnoozeCount(Context context) {
        Integer currentCount = getMapSnoozeCount(context);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(MAP_SNOOZE_COUNT, currentCount + 1);
        editor.apply();
    }

    public static void resetMapSnoozeCount(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(MAP_SNOOZE_COUNT, 0);
        editor.apply();
    }

    public static Integer getMapSnoozeCount(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(MAP_SNOOZE_COUNT, 0);
    }

    public static void setWallDismissalLmt(Context context, long lmt) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_WALL_DISMISSAL_CHECK_PREF_LMT, lmt);
        editor.apply();
    }

    public static long getWallDismissalLmt(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_WALL_DISMISSAL_CHECK_PREF_LMT, 0L);
    }

    public static void setTopicNotificationsLmt(Context context, long lmt) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_TOPIC_NOTIFICATIONS_CHECK_PREF_LMT, lmt);
        editor.apply();
    }

    public static long getTopicNotificationsLmt(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_TOPIC_NOTIFICATIONS_CHECK_PREF_LMT, 0L);
    }

    public static void setWallDismissal(Context context, Map<String, Map<String, SnoozeInfo>> wallDismissal) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(wallDismissal);
        editor.putString(PREF_WALL_DISMISSAL, json);
        editor.apply();
    }

    @Nullable
    public static Map<String, Map<String, SnoozeInfo>> getWallDismissal(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sharedPreferences.getString(PREF_WALL_DISMISSAL, "");

        if (!json.isEmpty()) {
            Gson gson = new Gson();
            try {
                Type type = new TypeToken<Map<String, Map<String, SnoozeInfo>>>() {}.getType();
                return gson.fromJson(json, type);
            } catch (Exception e) {
                Logger.e("PrefUtils", "Error parsing wall dismissal", e);
                setWallDismissal(context, null);
            }

        }
        return null;
    }

    public static void setPricingVariantOverride(Context context, @Nullable String variant) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_PRICING_VARIANT_OVERRIDE, variant);
        editor.apply();
    }

    public static @Nullable String getPricingVariantOverride(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PRICING_VARIANT_OVERRIDE, null);
    }

    public static void setTalkToThePostFeedbackLastSeen(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_TALK_TO_THE_POST_FEEDBACK_LAST_SEEN, System.currentTimeMillis());
        editor.apply();
    }

    public static Long getTalkToThePostFeedbackLastSeen(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(PREF_TALK_TO_THE_POST_FEEDBACK_LAST_SEEN, 0L);
    }

    public static void setTalkToThePostFeedbackSubmitted(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_TALK_TO_THE_POST_FEEDBACK_SUBMITTED, true);
        editor.apply();
    }

    public static Boolean getTalkToThePostFeedbackSubmitted(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_TALK_TO_THE_POST_FEEDBACK_SUBMITTED, false);
    }

    public static void setLastVisitedBottomTab(Context context, String tabName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_LAST_VISITED_BOTTOM_TAB, tabName);
        editor.apply();
    }

    public static @Nullable String getLastVisitedBottomTab(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_LAST_VISITED_BOTTOM_TAB, null);
    }

    public static void setTalkToThePostPulseShown(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_TALK_TO_THE_POST_PULSE_SHOWN, true);
        editor.apply();
    }

    public static Boolean getTalkToThePostPulseShown(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_TALK_TO_THE_POST_PULSE_SHOWN, false);
    }

    public static void setTalkToThePostVoiceSelection(Context context, String id) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_TALK_TO_THE_POST_VOICE_SELECTION, id);
        editor.apply();
    }

    public static @Nullable String getTalkToThePostVoiceSelection(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_TALK_TO_THE_POST_VOICE_SELECTION, null);
    }

    public static void incrementTalkToThePostVisitCount(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_TALK_TO_THE_POST_VISIT_COUNT, getTalkToThePostVisitCount(context) + 1);
        editor.apply();
    }

    public static int getTalkToThePostVisitCount(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(PREF_TALK_TO_THE_POST_VISIT_COUNT, 0);
    }

    public static void setTalkToThePostCaptionsEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_TALK_TO_THE_POST_CAPTIONS_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getTalkToThePostCaptionsEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_TALK_TO_THE_POST_CAPTIONS_ENABLED, false);
    }
}
