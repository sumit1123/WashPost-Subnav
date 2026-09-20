/*
 *
 *  *  Copyright (c) 2018. The Washington Post. All rights reserved.
 *
 */

package com.wapo.flagship;

import static com.wapo.flagship.wapomain.MainConstants.PREF_FOR_YOU_TAB_NOTIFICATION;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.commons.util.DeviceUtils;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.features.onetrust.OneTrustHelper;
import com.wapo.flagship.features.settings.AppPreferences;
import com.wapo.flagship.push.PushListener;
import com.wapo.flagship.push.PushPreferencesHelper;
import com.wapo.flagship.util.ReachabilityUtil;
import com.wapo.text.BuildConfig;
import com.wapo.text.GlobalFont;
import com.washingtonpost.android.paywall.PaywallService;

import java.util.HashSet;
import java.util.Set;

public class AppContext {

    private final static String TAG = AppContext.class.getSimpleName();
    private static  AppContext instance;

    private static final String IsFirstRun = "IsFirstRun";
    private static final String IsFirstArticleLoad = "IsFirstArticleLoad";
    public static final String NeedCountRunsForRateMessageIndex = "NeedCountRunsForRateMessageIndex";
    public static final String NeedShowRateMessage = "NeedShowRateMessage";
    public static final String LastRateMessageDisplayedTime = "LastRateMessageDisplayedTime";
    public static final String GENERAL_PREFERENCES = "GeneralPreferences";
    private static final String FontSize = "FontSize";
    private static final String MetaDbVersion = "MetaDbVersion";
    private static final String TEST_SKU_NAME="testSku";
    private static final String TEST_VALID_SKU_SET="testValidSkuSet";
    private static final String RegistrationId="RegistrationId";
    private static final String PREF_IS_FIRST_ALERTS_LAUNCH = "IsFirstAlertsLaunch";
    private static final String PREF_ALERTS_LAUNCH_COUNT = "AlertsLaunchCount";
    private static final String PREF_DO_NOT_ASK_ABOUT_NIGHT_MODE_ENABLED = "ShowNightModeDialogAgain";
    private static final String PREF_SHOULD_SHOW_NIGHT_MODE_ON_START = "IsArticleRecreateForNightMode";
    private static final String PREF_SHOULD_SHOW_WEAR_MESSAGE ="ShouldShowWearMessage";
    private static final String PREF_FILE_NAME_AUTO_SUBSCRIBED_TOPICS = "PREF_FILE_NAME_AUTO_SUBSCRIBE_TOPICS";
    public static final boolean DEBUG = false;
    public static final String PREF_IS_APP_UPGRADE = "pref.IsAppUpgrade";
    private static final String PREF_APP_RESUME_COUNT = "AppResumeCount";
    private static final String PREF_APP_RESUME_DAY_COUNT = "AppResumeDayCount";
    private static final String PREF_APP_LAST_APP_RESUME_MILLIS = "LastAppResumeMillis";
    public static final String NighModeDialogActive = "nighModeDialogActive";
    private static final String PREF_AIRSHIP_ATTRIBUTE_FLAG = "AirshipAttributeFlag";
    public static final String PREF_ONE_TIME_TOPIC_SYNC = "pref.isOneTimePrefTopicSync";
    private static final String CheckAgeRestriction = "CheckAgeRestriction";
    private static final String PREF_UPDATE_AGE_RESTRICTION_DIALOG_SHOWN = "pref.UPDATE_AGE_RESTRICTION_DIALOG_SHOWN";
    private static final String DIRTY_PREFERENCES = "pref.DirtyPreferences";
    private Context _context;

    private AppContext(Context ctx) {
        _context = ctx;
    }

    private static AppContext getInstance() {
        return instance;
    }

    public static void init(Context ctx) {
        if (instance == null) {
            instance = new AppContext(ctx);
        }
        float fontSizeAdjustment = getFontSizeAsPerLegacyPreferenceCheck();
        GlobalFont.INSTANCE.setFontSizeAdjustment(fontSizeAdjustment);
        com.wpds.theme.ArticleFontScale.INSTANCE.setAdjustment(fontSizeAdjustment);
    }

    public static boolean isFirstRun() {
        return getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE).getBoolean(IsFirstRun, true);
    }

    public static void setFirstRun(boolean isFirstRun) {
        SharedPreferences generalPrefs = getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putBoolean(IsFirstRun, isFirstRun);
        prefEditor.apply();
    }

    public static void setFirstArticleLoad(boolean isFirstArticleLoad) {
        SharedPreferences generalPrefs = getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putBoolean(IsFirstArticleLoad, isFirstArticleLoad);
        prefEditor.apply();
    }

    public static String getAirshipNamedUserId(Context context) {
        PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            return paywallService.getLoginId();
        }
        return DeviceUtils.getUniqueDeviceId(context);
    }

    public static String getIterableUserId(Context context) {
        PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()
                && OneTrustHelper.INSTANCE.isFunctionalityEnabled() && OneTrustHelper.INSTANCE.isTargetingEnabled()) {
            return paywallService.getLoginId();
        }
        return DeviceUtils.getUniqueDeviceId(context);
    }

    public static void setIsAppUpgrade(Context context) {
        final String PREF_CURRENT_VERSION_CODE = "pref.CurrentVersionCode";
        SharedPreferences preference = context.getSharedPreferences(AppContext.GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        int oldVersionCode = preference.getInt(PREF_CURRENT_VERSION_CODE, -1);
        SharedPreferences.Editor prefsEditor = preference.edit();

        //not a fresh install and actual version code is greater than stored version code
        if(oldVersionCode != -1 && com.wapo.android.commons.util.Utils.getAppVersionCode(context) > oldVersionCode) {
            prefsEditor.putBoolean(PREF_IS_APP_UPGRADE, true);
            prefsEditor.apply();
        }
    }

    public static boolean isAppUpgrade() {
        return getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE).getBoolean(PREF_IS_APP_UPGRADE, false);
    }

    public static boolean isPrefApiOneTimeSync() {
        return getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE).getBoolean(PREF_ONE_TIME_TOPIC_SYNC, false);
    }

    public static void setPrefApiOneTimeSync(Context context, boolean oneTime) {
        SharedPreferences preference = context.getSharedPreferences(AppContext.GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = preference.edit();
        prefsEditor.putBoolean(PREF_ONE_TIME_TOPIC_SYNC, oneTime);
        prefsEditor.apply();
    }

    public static int getMetaDbVersion() {
        return getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE).getInt(MetaDbVersion, 0);
    }

    public static void setMetaDbVersion(int version) {
        SharedPreferences generalPrefs = getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putInt(MetaDbVersion, version);
        prefEditor.apply();
    }

    public static float getFontSizeAsPerLegacyPreferenceCheck() {
        // Check if there is a font size value that is already in preferences.
        // If it is (that means this is an upgrade case), then set it to the new PREF_TEXT_SIZE preference and disable PREF_DEFAULT_FONT_SIZE preference.
        // Code can be cleaned once after having good adoption, and caller can use AppPreferences.INSTANCE.getTextSizeAsPerDefaultFontCheck() to get the size directly.
        SharedPreferences sp = getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        int previousFontSize = sp.getInt(FontSize, Integer.MIN_VALUE);
        if (previousFontSize != Integer.MIN_VALUE) {
            // Upgrade case
            sp.edit().putInt(FontSize, Integer.MIN_VALUE).apply();
            AppPreferences.INSTANCE.setDefaultFontSizeEnabled(false);
            AppPreferences.INSTANCE.setTextSize(previousFontSize);
        }
        return AppPreferences.INSTANCE.getTextSizeAsPerDefaultFontCheck();
    }

    public static boolean isAllowingBackgroundSync(Context context) {
        int backgroundSyncValue = AppPreferences.INSTANCE.getBackgroundSyncAsInt();
        boolean isNeverBackgroundSync = backgroundSyncValue == -1;
        if (isNeverBackgroundSync) {
            return false;
        }

        boolean isOnWiFi = ReachabilityUtil.isOnWiFi(context);
        if (isOnWiFi) {
            return true;
        }

        return AppPreferences.INSTANCE.canSyncOverCellular();
    }

    public static void warningLog(String log) {
        RemoteLog.w(getInstance()._context, new EventLog.Builder()
                .setMessage(log)
                .setModule(LogModules.APP).build());
    }

    public static void saveRegistrationId(String registrationId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString(RegistrationId, registrationId);
        editor.apply();
    }

    public static String getRegistrationId(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        String registrationId= sharedPreferences.getString(RegistrationId, "");
        if(registrationId.isEmpty()){
            Logger.i("FCM", "Registration not found");
            return "";
        }
        return registrationId;
    }

    public static boolean isTopicEnabled(String topicKey) {
        if (getInstance() == null || getInstance()._context == null) {
            return true;
        }

        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(topicKey, false);
    }

    public static void changeTopicEnabled(String topicName, boolean enabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putBoolean(topicName, enabled);
        editor.apply();
    }

    public static synchronized void markPreferenceDirty(String name) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        Set<String> dirty = new HashSet<String>(getDirtyPreferences());
        dirty.add(name);
        sp.edit()
                .putStringSet(DIRTY_PREFERENCES, dirty)
                .apply();
    }

    public static synchronized void clearPreferenceDirty(String preference) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        Set<String> dirty = new HashSet<String>(getDirtyPreferences());
        dirty.remove(preference);
        sp.edit()
                .putStringSet(DIRTY_PREFERENCES, dirty)
                .apply();
    }

    public static Set<String> getDirtyPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        return sp.getStringSet(DIRTY_PREFERENCES, new HashSet<String>());
    }

    public static boolean isNotificationActive(int notificationId) {
        if (getInstance() == null || getInstance()._context == null) {
            return false;
        }

        return getInstance()._context.getSharedPreferences(PushListener.PREFS_NAME, 0).getBoolean(Integer.toString(notificationId), false);
    }

    public static void updateActiveNotification(int notificationId, boolean isActive) {
        if (getInstance() == null || getInstance()._context == null) {
            return;
        }
        SharedPreferences sp = getInstance()._context.getSharedPreferences(PushListener.PREFS_NAME, 0);
        SharedPreferences.Editor editor = sp.edit();

        if (isActive) {
            editor.putBoolean(Integer.toString(notificationId), true);
        } else {
            editor.remove(Integer.toString(notificationId));
        }
        editor.apply();
    }

    public static boolean isFirstAlertsLaunch() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(PREF_IS_FIRST_ALERTS_LAUNCH, true);
    }

    public static int getAlertsLaunchCount(){
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getInt(PREF_ALERTS_LAUNCH_COUNT, 0);
    }

    public static void setFirstAlertsLaunch(boolean isFirstAlertsLaunch) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putBoolean(PREF_IS_FIRST_ALERTS_LAUNCH, isFirstAlertsLaunch);
        editor.apply();
    }

    public static void setAlertsLaunchCount(int alertsLaunchCount){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putInt(PREF_ALERTS_LAUNCH_COUNT, alertsLaunchCount);
        editor.apply();
    }


    public static void saveTestSubSku(String sku, Set<String> validSku){
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putString(TEST_SKU_NAME, sku);
        prefEditor.putStringSet(TEST_VALID_SKU_SET, validSku);
        prefEditor.apply();
    }

    public static String getTestSubSku(){
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        return generalPrefs.getString(TEST_SKU_NAME,null);
    }

    public static Set<String> getTestValidSkuSet(){
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        Set<String> validSkuSet = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
                null :
                generalPrefs.getStringSet(TEST_VALID_SKU_SET,null);
        if(validSkuSet==null || validSkuSet.isEmpty()){
            String sku=generalPrefs.getString(TEST_SKU_NAME,null);
            validSkuSet=new HashSet<String>();
            if(sku!=null)validSkuSet.add(sku);
        }
        return validSkuSet;
    }

    public static boolean showNightModeSnackbar() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(NighModeDialogActive, true);
    }

    public static void setShowNightModeSnackbar(boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putBoolean(NighModeDialogActive, value);
        editor.apply();
    }

    public static boolean shouldShowNightModeSnackbarOnStart() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(PREF_SHOULD_SHOW_NIGHT_MODE_ON_START, false);
    }

    public static void setShowNightModeSnackbarOnStart(boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putBoolean(PREF_SHOULD_SHOW_NIGHT_MODE_ON_START, value);
        editor.apply();
    }

    public static boolean isDoNotAskAboutNightModeEnabled() {
        return false;
    }

    public static int getCountRuns() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getInt(NeedCountRunsForRateMessageIndex, 0);
    }
    public static void setCountRuns(int countRuns) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(NeedCountRunsForRateMessageIndex, countRuns);
        editor.apply();
    }
    public static boolean needShowRateMessage(){
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(NeedShowRateMessage, true);
    }

    public static void setShowRateMessage(boolean showRateMessage){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putBoolean(NeedShowRateMessage, showRateMessage);
        editor.apply();
    }

    public static long getLastRateMessageDisplayedTime() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getLong(LastRateMessageDisplayedTime, 0);
    }

    public static void setLastRateMessageDisplayedTime(long lastRateMessageDisplayedTime){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putLong(LastRateMessageDisplayedTime, lastRateMessageDisplayedTime);
        editor.apply();
    }

    public static boolean shouldShowWearMessage(){
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(PREF_SHOULD_SHOW_WEAR_MESSAGE, true);
    }

    public static void setPrefShouldShowWearMessage(boolean shouldShowWearMessage){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getInstance()._context).edit();
        editor.putBoolean(PREF_SHOULD_SHOW_WEAR_MESSAGE, shouldShowWearMessage);
        editor.apply();
    }

    /**
     * Method to increment app resume count.
     * It will be reset when it reaches to Long.MAX_VALUE + 1 or Long.MIN_VALUE
     * Note: Provided here to be used by an app logic. App logic should not reset/change this value outside of this method.
     */
    public static void incrementAppResumeCount() {
        long count = getAppResumeCount() + 1;
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putLong(PREF_APP_RESUME_COUNT, (count < 0) ? 1 : count);
        prefEditor.apply();
    }

    public static Long getAppResumeCount() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getLong(PREF_APP_RESUME_COUNT, 0);
    }

    /**
     * Method to increment days count when user visits the app on any unique day.
     * It will be reset when it reaches to Int.MAX_VALUE + 1 or Int.MIN_VALUE
     * Note: Provided here to be used by an app logic. App logic should not reset/change this value outside of this method.
     */
    public static void incrementAppResumeDayCount() {
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();

        long lastAppResumeTimeMillis = generalPrefs.getLong(PREF_APP_LAST_APP_RESUME_MILLIS, -1);
        boolean freshDay = Utils.isTodayAFreshDayFromMillis(lastAppResumeTimeMillis);
        if (freshDay) {
            int count = getAppResumeDayCount() + 1;
            prefEditor.putInt(PREF_APP_RESUME_DAY_COUNT, (count < 0) ? 1 : count);
        }
        prefEditor.putLong(PREF_APP_LAST_APP_RESUME_MILLIS, System.currentTimeMillis());
        prefEditor.apply();
        if (BuildConfig.DEBUG) {
            Logger.d(TAG, "AppResumeCount: " + getAppResumeCount() + ", AppResumeDayCount: " + getAppResumeDayCount());
        }
    }

    public static int getAppResumeDayCount() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getInt(PREF_APP_RESUME_DAY_COUNT, -1);
    }

    public static void handleFirstRun() {
        PushPreferencesHelper.INSTANCE.onAppRun(getInstance()._context, AppContext.isFirstRun());
    }

    /**
     * @return true if notification badge should be shown in "For you" tab in top sectioned tabs in My post
     * false otherwise.
     */
    public static boolean shouldShowForYouTabNotification(){
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(PREF_FOR_YOU_TAB_NOTIFICATION, false);
    }

    /**
     * Retrieves the value of the Airship attribute clear flag from SharedPreferences.
     * @return true if the flag is set, false otherwise (default is false).
     */
    public static Boolean getAirshipAttributeClearFlag() {
        return PreferenceManager.getDefaultSharedPreferences(getInstance()._context).getBoolean(PREF_AIRSHIP_ATTRIBUTE_FLAG, false);
    }

    /**
     * Sets the value of the Airship attribute clear flag in SharedPreferences.
     * Used to track whether Airship attributes need to be cleared or note.
     */
    public static void setAirshipAttributeClearFlag(Boolean flag) {
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putBoolean(PREF_AIRSHIP_ATTRIBUTE_FLAG, flag);
        prefEditor.apply();
    }

    /**
     * Sets whether or not to show the notification badge in the "For you" tab in My post
     * @param show - whether or not to show it the next time.
     */
    public static void setShowForYouTabNotification(Boolean show){
        SharedPreferences generalPrefs = PreferenceManager.getDefaultSharedPreferences(getInstance()._context);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putBoolean(PREF_FOR_YOU_TAB_NOTIFICATION, show);
        prefEditor.apply();
    }
    public static boolean updateAgeRestrictionDialogShown() {
        return getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE).getBoolean(PREF_UPDATE_AGE_RESTRICTION_DIALOG_SHOWN, false);
    }

    public static void setPrefUpdateAgeRestrictionDialogShown() {
        SharedPreferences generalPrefs = getInstance()._context.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = generalPrefs.edit();
        prefEditor.putBoolean(PREF_UPDATE_AGE_RESTRICTION_DIALOG_SHOWN, true);
        prefEditor.apply();
    }
}
