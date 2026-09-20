package com.washingtonpost.android.paywall.metering;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.util.HashMap;


/**
 * Created by kilarib on 3/27/17.
 */

public class MeteringPrefs {

    public static void cleanUpMeteringPrefs(){
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallCurrentArticleCount = settings.getInt(
                PaywallConstants.PW_CURRENT_ARTICLE_COUNT_OLD, -1);
        if(paywallCurrentArticleCount!=-1) {
            setCurrentArticleCount(PaywallConstants.PW_CURRENT_ARTICLE_COUNT, (float)paywallCurrentArticleCount);
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(PaywallConstants.PW_CURRENT_ARTICLE_COUNT);
            editor.apply();
        }
    }

    public static void setMeterCycleDays(int cycleDays) {
        SharedPreferences.Editor editor = PaywallService.getSharedPreferences().edit();
        editor.putInt(PaywallConstants.PW_METER_CYCLE_DAYS, cycleDays);
        editor.apply();
    }
    public static int getMeterCycleDays() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallMaxArticleCount = settings.getInt(
                PaywallConstants.PW_METER_CYCLE_DAYS, 45);
        return paywallMaxArticleCount;
    }

    public static void setMaxArticleLimit(int limit) {
        SharedPreferences.Editor editor = PaywallService.getSharedPreferences().edit();
        editor.putInt(PaywallConstants.PW_MAX_ARTICLE_COUNT, limit);
        editor.apply();
    }

    public static int getMaxArticleLimit() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallMaxArticleCount = settings.getInt(
                PaywallConstants.PW_MAX_ARTICLE_COUNT, PaywallConstants.TETRO_DEFAULT_METER_LIMIT);
        return paywallMaxArticleCount;
    }

    public static int getMaxArticleLimitForRule2() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallMaxArticleCount = settings.getInt(
                PaywallConstants.PW_PREF_ROLLING_MAX_ARTICLES, 3);
        return paywallMaxArticleCount;
    }

    public static void setFreeArticlesRemaining(int freeCount) {
        SharedPreferences.Editor editor = PaywallService.getSharedPreferences().edit();
        editor.putInt(PaywallConstants.PW_FREE_ARTICLE_COUNT, freeCount);
        editor.apply();
    }

    public static int getFreeArticlesRemaining() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int freeArticlesRemaining = settings.getInt(
                PaywallConstants.PW_FREE_ARTICLE_COUNT, 0);
        return freeArticlesRemaining;
    }

    public static void setCurrentArticleCount(String pref, float count){
        SharedPreferences settings = PaywallService.getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(pref, count);
        editor.commit();
    }

    public static void setCurrentArticleMeterReason(String pref, int count){
        SharedPreferences settings = PaywallService.getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(pref, count);
        editor.commit();
    }

    public static float getCurrentArticleCount() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        float paywallCurrentArticleCount = settings.getFloat(
                PaywallConstants.PW_CURRENT_ARTICLE_COUNT, 0f);
        return paywallCurrentArticleCount;
    }

    public static int getCurrentArticleCountForRule2() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallCurrentArticleCount = settings.getInt(
                PaywallConstants.PW_PREF_ROLLING_CURRENT_ARTICLE_COUNT, 0);
        return paywallCurrentArticleCount;
    }

    public static int getMaxArticleLimitForRule1(int groupId) {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallMaxArticleCount = settings.getInt(PaywallConstants.PW_PREF_GROUP_MAX_PREFIX + groupId, 3);
        return paywallMaxArticleCount;
    }

    public static int getCurrentArticleCountForRule1(int groupId) {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallCurrentArticleCount = settings.getInt(PaywallConstants.PW_PREF_GROUP_CURRENT_COUNT_PREFIX + groupId, 0);
        return paywallCurrentArticleCount;
    }

    public static int getCurrentArticleMeterReason() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int articleMeterState = settings.getInt(PaywallConstants.PW_PREF_CURRENT_ARTICLE_METER_REASON, 0);
        return articleMeterState;
    }

    public static boolean shouldShowPaywallFor(String key) {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        boolean showPaywall = settings.getBoolean(key,
                false);
        return showPaywall;
    }

    public static void setArticleWeights(String key , HashMap<String,Float> obj) {
        SharedPreferences.Editor editor = PaywallService.getSharedPreferences().edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(key,json);
        editor.apply();
    }

    public static HashMap<String,Float> getArticleWeights(String key) throws JsonSyntaxException {
        Gson gson = new Gson();
        String json = PaywallService.getSharedPreferences().getString(key,"");
        if(json.isEmpty()) {
            return new HashMap<>();
        }
        java.lang.reflect.Type type = new TypeToken<HashMap<String,Float>>(){}.getType();
        HashMap<String,Float> obj = gson.fromJson(json, type);
        return obj;
    }

    public static void setArticleWeightsLastFetched(long fetchTime){
        SharedPreferences settings = PaywallService.getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PaywallConstants.PW_ARTICLE_WEIGHTS_FETCH_TIME, fetchTime);
        editor.commit();
    }

    public static long getArticleWeightsLastFetched() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        long fetchTime = settings.getLong(PaywallConstants.PW_ARTICLE_WEIGHTS_FETCH_TIME, 0);
        return fetchTime;
    }

}
