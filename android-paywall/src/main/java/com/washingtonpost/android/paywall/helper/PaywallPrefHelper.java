package com.washingtonpost.android.paywall.helper;

/**
 * Created by elamgodilj on 6/27/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/**
 * Use this file for all new paywall preferences
 *
 */
//TODO move all old paywall preferences to this file
public class PaywallPrefHelper {

    private static PaywallPrefHelper instance;
    private static SharedPreferences paywallPreferences;
    private static final String PREF_FORCE_VERIFY_DEVICE_TRACKING = "com.washingtonpost.rainbow.pref_force_verify_device_tracking";
    private static final String PREF_HAS_MIGRATED_TO_OAUTH = "com.washingtonpost.rainbow.pref_has_migrated_to_oauth";
    private static final String PREF_IS_USER_MISSING_REFRESH_TOKEN = "isUserMissingRefreshToken";
    private static final String PREF_HAS_MIGRATED_NULL_TOKEN = "com.washingtonpost.rainbow.pref_has_migrated_null_token";
    private static final String PREF_VERIFY_USER_LAST_TIME = "verifyUserSubLastTime";
    private static final String PREF_VERIFY_DEVICE_SUB_LAST_TIME = "verifyDeviceSubLastTime";
    private static final String PREF_LAST_SUB_TOKEN = "lastSubToken";
    private static final String PREF_LAST_SUB_SKU = "lastSubSku";
    private static final String PREF_LAST_SUB_EXP = "lastSubExp";
    private static final String PREF_LAST_SUB_STATE = "lastSubState";
    private static final String PREF_TETRO_STATE_COOKIE = "cookie";
    private static final String PREF_TETRO_GEO_COOKIE = "geo_cookie";
    private static final String PREF_TETRO_RCT_COOKIE = "rct_cookie";
    private static final String PREF_PROMO_CODE = "promo_code";
    private static final String PREF_MIGRATED_TOKEN_RESPONSE = "migrated_token_response"; //rainbow to classic migrated token response
    private static final String PREF_MIGRATED_LOGIN_ID = "migrated_login_id"; //rainbow to classic migrated login id
    private static final String PREF_MIGRATED_SECURE_LOGIN_ID = "migrated_secure_login_id"; //rainbow to classic migrated secure login id
    private static final String PREF_MIGRATED_RECEIPT_ID = "migrated_receipt_id"; //rainbow to classic migrated receipt id
    private static final String PREF_MIGRATED_SKU = "migrated_sku"; //rainbow to classic migrated productId
    private static final String PREF_MIGRATED_TRANSACTION_DATE = "migrated_transaction_date"; //rainbow to classic migrated transaction date
    private static final String PREF_MIGRATED_IAP_TOKEN = "migrated_iap_token"; //rainbow to classic migrated iap token
    private String PREF_MIGRATED_AMAZON_USER_ID = "migrated_amazon_user_id"; //amazon classic to unified amazon user id
    private String PREF_MIGRATED_AMAZON_USER_HAS_DUPLICATE_SUB = "migrated_amazon_user_has_duplicate_sub"; //identifies whether we have detected user has duplicate sub after amazon unification
    private String PREF_MIGRATED_AMAZON_USER_APPSTORE_MIGRATION_COMPLETE = "migrated_amazon_user_appstore_migration_complete"; //identifies when after amazon unification, we confirmed that user's IAP has been migrated in the app store.
    private String PREF_ARTICLES_META_LIST = "articles_meta_list";
    private String PREF_CURRENT_ARTICLE_URL = "current_article_url";
    private String PREF_PURCHASE_HISTORY = "purchase_history";
    private String PREF_PREVIOUS_SCREEN = "previous_screen";
    private static final String PREF_BYPASS_PAYWALL = "bypass_paywall";

    private static final String PREF_LOGIN_ID = "pref.PREF_LOGIN_ID";

    public static PaywallPrefHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PaywallPrefHelper();
            paywallPreferences = context.getSharedPreferences(PaywallConstants.PREFS_NAME, Context.MODE_PRIVATE);
        }
        return instance;
    }

    public boolean wasForceDeviceVerifyForTrackingInvoked() {
        return paywallPreferences.getBoolean(PREF_FORCE_VERIFY_DEVICE_TRACKING, false);
    }

    public void setForceDeviceVerifyForTrackingInvoked(boolean isInvoked) {
        paywallPreferences.edit().putBoolean(PREF_FORCE_VERIFY_DEVICE_TRACKING, isInvoked).apply();
    }

    public boolean hasMigratedToOAuth() {
        return paywallPreferences.getBoolean(PREF_HAS_MIGRATED_TO_OAUTH, false);
    }

    public void setHasMigratedToOAuth() {
        paywallPreferences.edit().putBoolean(PREF_HAS_MIGRATED_TO_OAUTH, true).apply();
    }

    public void setPrefIsUserMissingRefreshToken(boolean isMissing) {
        paywallPreferences.edit().putBoolean(PREF_IS_USER_MISSING_REFRESH_TOKEN, isMissing).apply();
    }

    public boolean isUserMissingRefreshToken() {
        return paywallPreferences.getBoolean(PREF_IS_USER_MISSING_REFRESH_TOKEN, false);
    }

    public void setHasMigratedNullToken() {
        paywallPreferences.edit().putBoolean(PREF_HAS_MIGRATED_NULL_TOKEN, true).apply();
    }

    public boolean hasMigratedNullToken() {
        return paywallPreferences.getBoolean(PREF_HAS_MIGRATED_NULL_TOKEN, false);
    }

    public long getLastVerifyUserTime() {
        return paywallPreferences.getLong(PREF_VERIFY_USER_LAST_TIME, 0);
    }

    public void setLoginId(Context context, String loginId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_LOGIN_ID, loginId);
        editor.apply();
    }

    public void clearLoginIdFromPrefs(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_LOGIN_ID);
        editor.apply();
    }

    public String getLoginId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_LOGIN_ID, "");
    }

    public void setPrefVerifyUserLastTime(long verifyUserLastTime) {
        paywallPreferences.edit().putLong(PREF_VERIFY_USER_LAST_TIME, verifyUserLastTime).apply();
    }

    public long getLastVerifyDeviceSubTime() {
        return paywallPreferences.getLong(PREF_VERIFY_DEVICE_SUB_LAST_TIME, 0);
    }

    public void setPrefVerifyDeviceSubLastTime(long verifyUserLastTime) {
        paywallPreferences.edit().putLong(PREF_VERIFY_DEVICE_SUB_LAST_TIME, verifyUserLastTime).apply();
    }

    public String getPrefLastSubToken() {
        return paywallPreferences.getString(PREF_LAST_SUB_TOKEN, null);
    }

    public void setPrefLastSubToken(String lastSubToken) {
        paywallPreferences.edit().putString(PREF_LAST_SUB_TOKEN, lastSubToken).apply();
    }

    public String getPrefLastSubProductId() {
        return paywallPreferences.getString(PREF_LAST_SUB_SKU, null);
    }

    public void setPrefLastSubProductId(String lastSubToken) {
        paywallPreferences.edit().putString(PREF_LAST_SUB_SKU, lastSubToken).apply();
    }

    public long getPrefLastSubExpirationDate() {
        return paywallPreferences.getLong(PREF_LAST_SUB_EXP, 0);
    }

    public void setPrefLastSubExpirationDate(long expirationDate) {
        paywallPreferences.edit().putLong(PREF_LAST_SUB_EXP, expirationDate).apply();
    }

    public void setPrefLastSubState(String lastSubState) {
        paywallPreferences.edit().putString(PREF_LAST_SUB_STATE, lastSubState).apply();
    }

    public String getPrefLastSubState() {
        return paywallPreferences.getString(PREF_LAST_SUB_STATE, null);
    }


    public static void setPrefTetroStateCookie(String cookie) {
        paywallPreferences.edit().putString(PREF_TETRO_STATE_COOKIE, cookie).apply();
    }

    public static String getPrefTetroStateCookie() {
        return paywallPreferences.getString(PREF_TETRO_STATE_COOKIE, null);
    }

    public static void setPrefGeoCookie(String cookie) {
        paywallPreferences.edit().putString(PREF_TETRO_GEO_COOKIE, cookie).apply();
    }

    public static String getPrefGeoCookie() {
        return paywallPreferences.getString(PREF_TETRO_GEO_COOKIE, null);
    }

    public static void setPrefTetroRctCookie(String cookie) {
        paywallPreferences.edit().putString(PREF_TETRO_RCT_COOKIE, cookie).apply();
    }

    public static String getPrefTetroRctCookie() {
        return paywallPreferences.getString(PREF_TETRO_RCT_COOKIE, null);
    }

    public static void setPromoCode(String promoCodeSerialized){
        paywallPreferences.edit().putString(PREF_PROMO_CODE, promoCodeSerialized).apply();
    }

    @Nullable
    public static String getPromoCode(){
        return paywallPreferences.getString(PREF_PROMO_CODE, null);
    }

    public void setPrefMigratedTokenResponse(String migratedTokenResponse) {
        paywallPreferences.edit().putString(PREF_MIGRATED_TOKEN_RESPONSE, migratedTokenResponse).apply();
    }

    public String getPrefMigratedTokenResponse() {
        return paywallPreferences.getString(PREF_MIGRATED_TOKEN_RESPONSE, null);
    }

    public void setPrefMigratedLoginId(String migratedLoginId){
        paywallPreferences.edit().putString(PREF_MIGRATED_LOGIN_ID, migratedLoginId).apply();
    }

    @Nullable
    public String getPrefMigratedLoginId(){
        return paywallPreferences.getString(PREF_MIGRATED_LOGIN_ID, null);
    }

    public void setPrefMigratedSecureLoginId(String migratedSecureLoginId){
        paywallPreferences.edit().putString(PREF_MIGRATED_SECURE_LOGIN_ID, migratedSecureLoginId).apply();
    }

    @Nullable
    public String getPrefMigratedSecureLoginId(){
        return paywallPreferences.getString(PREF_MIGRATED_SECURE_LOGIN_ID, null);
    }

    public void setPrefMigratedReceiptId(String migratedReceiptId) {
        paywallPreferences.edit().putString(PREF_MIGRATED_RECEIPT_ID, migratedReceiptId).apply();
    }

    public String getPrefMigratedReceiptId() {
        return paywallPreferences.getString(PREF_MIGRATED_RECEIPT_ID, null);
    }

    public void setPrefMigratedSku(String migratedSku) {
        paywallPreferences.edit().putString(PREF_MIGRATED_SKU, migratedSku).apply();
    }

    public String getPrefMigratedSku() {
        return paywallPreferences.getString(PREF_MIGRATED_SKU, null);
    }

    public void setPrefMigratedTransactionDate(String migratedTransactionDate) {
        paywallPreferences.edit().putString(PREF_MIGRATED_TRANSACTION_DATE, migratedTransactionDate).apply();
    }

    public String getPrefMigratedTransactionDate() {
        return paywallPreferences.getString(PREF_MIGRATED_TRANSACTION_DATE, null);
    }

    public void setPrefMigratedIapToken(String migratedIapToken) {
        paywallPreferences.edit().putString(PREF_MIGRATED_IAP_TOKEN, migratedIapToken).apply();
    }

    public String getPrefMigratedIapToken() {
        return paywallPreferences.getString(PREF_MIGRATED_IAP_TOKEN, null);
    }

    public void setPrefMigratedAmazonUserId(String migratedAmazonUserId) {
        paywallPreferences.edit().putString(PREF_MIGRATED_AMAZON_USER_ID, migratedAmazonUserId).apply();
    }

    public String getPrefMigratedAmazonUserId() {
        return paywallPreferences.getString(PREF_MIGRATED_AMAZON_USER_ID, null);
    }

    public void setPrefMigratedAmazonUserHasDuplicateSub(boolean migratedAmazonUserHasDuplicateSub) {
        paywallPreferences.edit().putBoolean(PREF_MIGRATED_AMAZON_USER_HAS_DUPLICATE_SUB, migratedAmazonUserHasDuplicateSub).apply();
    }

    public boolean getPrefMigratedAmazonUserHasDuplicateSub() {
        return paywallPreferences.getBoolean(PREF_MIGRATED_AMAZON_USER_HAS_DUPLICATE_SUB, false);
    }

    public void setPrefMigratedAmazonUserAppstoreMigrationComplete(boolean migratedAmazonUserAppstoreMigrationComplete) {
        paywallPreferences.edit().putBoolean(PREF_MIGRATED_AMAZON_USER_APPSTORE_MIGRATION_COMPLETE, migratedAmazonUserAppstoreMigrationComplete).apply();
    }

    public boolean getPrefMigratedAmazonUserAppstoreMigrationComplete() {
        return paywallPreferences.getBoolean(PREF_MIGRATED_AMAZON_USER_APPSTORE_MIGRATION_COMPLETE, false);
    }

    public void setCurrentArticleUrl(String articleUrl) {
        paywallPreferences.edit().putString(PREF_CURRENT_ARTICLE_URL, articleUrl).apply();
    }

    @Nullable
    public String getCurrentArticleUrl() {
        return paywallPreferences.getString(PREF_CURRENT_ARTICLE_URL, null);
    }

    public void setArticlesMetaList(String articlesMetaList) {
        paywallPreferences.edit().putString(PREF_ARTICLES_META_LIST, articlesMetaList).apply();
    }

    @Nullable
    public String getArticlesMetaList() {
        return paywallPreferences.getString(PREF_ARTICLES_META_LIST, null);
    }


    public static boolean getPrefBypassPaywall() {
        return paywallPreferences.getBoolean(PREF_BYPASS_PAYWALL, false);
    }

    public static void setBypassPaywall(boolean enabled) {
        paywallPreferences.edit().putBoolean(PREF_BYPASS_PAYWALL, enabled).apply();
    }

    /**
     * Writes the list of product ids that the user has previously purchased to preferences
     * @param productIds
     */
    public void setPurchaseHistory(List<String> productIds) {
        Gson gson = new Gson();
        String json = gson.toJson(productIds);
        paywallPreferences.edit().putString(PREF_PURCHASE_HISTORY, json).apply();
    }

    /**
     * @return The product ids that the user has previously purchased
     */
    public List<String> getPurchaseHistory() {
        String json = paywallPreferences.getString(PREF_PURCHASE_HISTORY, "");
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void setPreviousScreen(String articlesMetaList) {
        paywallPreferences.edit().putString(PREF_PREVIOUS_SCREEN, articlesMetaList).apply();
    }

    @Nullable
    public String getPreviousScreen() {
        return paywallPreferences.getString(PREF_PREVIOUS_SCREEN, null);
    }
}
