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

package com.washingtonpost.android.paywall.metering;

import android.content.Intent;
import android.content.SharedPreferences;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper;
import com.washingtonpost.android.paywall.helper.PaywallDbHelper;
import com.washingtonpost.android.paywall.newdata.model.ArticleStub;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.util.Map;
import java.util.Set;

/**
 * Paywall metering service
 *
 * @author Bkilari
 */
public class MeteringService {

    public final static String TAG = MeteringService.class.getSimpleName();
    public long tetroWeightedArticleTTL = 86400000;
    public int tetroSyncFrequency = 3;
    public boolean skipTTLcheck = false;

    public boolean isAtLimit(String category, ArticleStub article) {

        if (isPaywallTurnedOffOrIsValidUser()) {
            PaywallService.getConnector().logD(new EventLog.Builder().setMessage("PaywallService Paywall Turned Off or a premium user"));
            // free
            return false;
        }

        if (isTetroTurnedOn()) {
            checkArticleMeterDays();
            boolean isAtTetroLimit = isAtTetroLimit(article);
            if (!isAtTetroLimit) {
                handleFreeArticleCount(article);
                PaywallService.getInstance().getTetroManager().updateReadArticleList(article);
            }
            return isAtTetroLimit;
        } else {
            return isAtStandardLimit(category, article);
        }
    }

    public boolean isAtTetroLimit(ArticleStub article) {
        try {
            // check if this is a free metered article or if user is on free trial
            if (isArticleStoredAsFreeMetered(article)
                    || userHasFreeArticlesRemaining()
            ) {
                return false;
            }

            // check if this article weight pushes meter over limit and update meter count.
            boolean isWeightLimitReached = processWeightedLimit(article);

            // if limit is reach based on current measurement, show paywall
            if (isWeightLimitReached) {
                return true;
            }
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Paywall service failed")
                    .setErrorMessage(e.getMessage()));
        }
        return false;
    }

    private boolean isArticleStoredAsFreeMetered(ArticleStub article) {
        ArticleStub storedArticle = article == null || article.getUrl() == null ? null : PaywallCounterHelper
                .getArticleByUrl(article.getUrl(), PaywallDbHelper.PW_ARTICLE_TABLE);

        return storedArticle != null;
    }

    private boolean userHasFreeArticlesRemaining() {
        int freeArticlesRemaining = MeteringPrefs.getFreeArticlesRemaining();
        if (freeArticlesRemaining > 0) {
            return true;
        }
        return false;
    }

    /**
     * Handle offline flow for free articles from Reddit promo.
     * Decrement local value of free articles remaining while we cannot connect to Tetro.
     * - Decrement only if article is not stored as Free (viewed already).
     * - Gifted or unmetered articles are handled at a higher level and don't need to be handled here.
     */
    private void handleFreeArticleCount(ArticleStub article) {
        if (!PaywallService.getConnector().isOnline() && !isArticleStoredAsFreeMetered(article)) {
            int freeArticlesRemaining = MeteringPrefs.getFreeArticlesRemaining();
            if (freeArticlesRemaining > 0) {
                freeArticlesRemaining--;
                MeteringPrefs.setFreeArticlesRemaining(freeArticlesRemaining);
            }
        }
    }

    public boolean processWeightedLimit(ArticleStub article) {
        Map<String, Float> articleWeights = PaywallService.getInstance().getTetroManager().getWeightedArticles(skipTTLcheck);
        String key = article != null && article.getUrl() != null ? article.getUrl() : "";
        float weight = articleWeights.containsKey(key) && articleWeights.get(key) != null ? articleWeights.get(key) : 1;
        int maxCount = MeteringPrefs.getMaxArticleLimit();
        float currentCount = MeteringPrefs.getCurrentArticleCount();
        Logger.d("Count", "ArticleCount:" + currentCount);
        float newCount = currentCount +  weight;

        StringBuilder weightsString = new StringBuilder();
        weightsString.append("-----Article Weights-------\n");
        for (Map.Entry<String, Float> entry : articleWeights.entrySet()) {
            weightsString.append(entry.getValue() + " : " + entry.getKey() + "\n");
        }

        Logger.d("Tetro Logic", weightsString.toString());
        Logger.d("Tetro Logic", "article: " + article.getUrl() +
                "\nWeight: " + weight +
                "\nCurrent Total: " + currentCount +
                "\nNew Total: " + newCount +
                "\nLimit: " + maxCount);

        if (newCount > maxCount) {
            return true;
        }

        MeteringPrefs.setCurrentArticleCount(PaywallConstants.PW_CURRENT_ARTICLE_COUNT, newCount);
        return false;
    }

    public boolean hasBeenPaywalled() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        return settings.getBoolean(PaywallConstants.PW_SHOW,
                false) || settings.getBoolean(PaywallConstants.PW_SHOW_RULE2, false) || settings.getBoolean(PaywallConstants.PW_SHOW_RULE1, false);
    }

    public boolean isAtStandardLimit(String category, ArticleStub article) {
        String meterValue = "1";
        try {
            SharedPreferences settings = PaywallService.getSharedPreferences();
            SharedPreferences.Editor editor = settings.edit();
            boolean showPaywall = settings.getBoolean(PaywallConstants.PW_SHOW,
                    false);

            boolean allowed = false;

            String articleUrl = article == null || article.getUrl() == null ? null : article.getUrl();


            ArticleStub storedArticle = articleUrl == null ? null : PaywallCounterHelper
                    .getArticleByUrl(articleUrl, PaywallDbHelper.PW_ARTICLE_TABLE);

            // if article in ignore list or is already stored
            if (PaywallConstants.ignoreCategoryList.contains(category) || storedArticle != null) {
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("PaywallService ignorelist or storedArticle"));
                return false;
            }
            Set<String> allowedSec = PaywallService.getConnector().getAllowedPwSections();
            if (allowedSec != null && allowedSec.size() > 0 && allowedSec.contains(category)) {
                allowed = true;
                meterValue = PaywallService.getOmniture().OVERLAY;
                Logger.d(TAG, "This section is allowed to be viewed");
            }

            // else, store article
            int paywallMaxArticleCount = MeteringPrefs.getMaxArticleLimit();
            float paywallCurrentArticleCount = MeteringPrefs.getCurrentArticleCount();
            meterValue = "" + paywallCurrentArticleCount;
            if (showPaywall && paywallCurrentArticleCount >= paywallMaxArticleCount && !allowed) {
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("PaywallService CurrentArticleCount > MaxAllowed && not an allowed section"));
                return true;
            }

            if (paywallCurrentArticleCount < paywallMaxArticleCount) {
                if (article != null) {
                    // if limit already reached
                    PaywallService.getConnector().logD(new EventLog.Builder().setMessage("PaywallService inserting Article " + paywallCurrentArticleCount));

                    PaywallCounterHelper.insertArticle(article, PaywallDbHelper.PW_ARTICLE_TABLE);
                    paywallCurrentArticleCount++;
                }
            } else {
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("PaywallService paywallCurrentArticleCount " + paywallCurrentArticleCount));
                showPaywall = true;
            }

            if (showPaywall && paywallCurrentArticleCount >= paywallMaxArticleCount) {
                editor.putBoolean(PaywallConstants.PW_SHOW, true);
                editor.putFloat(PaywallConstants.PW_CURRENT_ARTICLE_COUNT,
                        paywallCurrentArticleCount);
                editor.apply();
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("PaywallService showPaywall"));
                if (!allowed) {
                    meterValue = PaywallService.getOmniture().OVERLAY;
                    return true;
                }
            }
            editor.putFloat(PaywallConstants.PW_CURRENT_ARTICLE_COUNT,
                    paywallCurrentArticleCount);
            editor.commit();

        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Paywall service failed")
                    .setErrorMessage(e.getMessage()));
        } finally {
            PaywallService.getOmniture().setMeterValue(meterValue);
        }
        return false;
    }

    public void initialize(ServiceConfigStub serviceConfig) {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        int limit = serviceConfig.getLimit();
        boolean tetroOn = serviceConfig.getTetroTurnedOn();
        boolean turnedOn = serviceConfig.getPwTurnedOn();
        tetroWeightedArticleTTL = serviceConfig.getTetroWeightArticleTTL();
        tetroSyncFrequency = serviceConfig.getTetroSyncFrequency();

        editor.putBoolean(PaywallConstants.TETRO_TURNED_ON, tetroOn);
        editor.putBoolean(PaywallConstants.PW_TURNED_ON, turnedOn);
        //If tetro is turned off, default to Meter Limit from remote config
        if (!tetroOn) {
            editor.putInt(PaywallConstants.PW_MAX_ARTICLE_COUNT, limit);
        }
        Logger.d("MeterDays", ""+MeteringPrefs.getMeterCycleDays());
        editor.apply();
    }

    /*** Checking metering days, remove article from DB if more than meter cycle days ***/
    private void checkArticleMeterDays() {
        long meterCycleDays = MeteringPrefs.getMeterCycleDays();
        long articleFrequency = meterCycleDays * (24 * 60 * 60 * 1000);
        PaywallCounterHelper.removeArticleByFrequency(articleFrequency, PaywallDbHelper.PW_ARTICLE_TABLE);
    }

    private void launchNewStoriesIntent() {
        Intent intent = new Intent();
        intent.setAction(PaywallConstants.NEW_STORIES_AVAILABLE_RECEIVER);
        PaywallService.getInstance().getContext().sendBroadcast(intent);
    }

    protected boolean isPaywallTurnedOffOrIsValidUser() {
        // TODO: Check this before any other paywall service
        SharedPreferences settings = PaywallService.getSharedPreferences();
        boolean turnedOn = settings.getBoolean(PaywallConstants.PW_TURNED_ON,
                false);

        // is signed in and does not have valid sub
        boolean isValidUser = PaywallService.getInstance().isPremiumUser();
        return !turnedOn || isValidUser;

    }

    public boolean isTetroTurnedOn() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        return settings.getBoolean(PaywallConstants.TETRO_TURNED_ON, false);
    }

    public static int getMaxArticleLimit() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        int paywallMaxArticleCount = settings.getInt(
                PaywallConstants.PW_MAX_ARTICLE_COUNT, 20);
        return paywallMaxArticleCount;
    }

    public static float getCurrentArticleCount() {
        MeteringPrefs.getCurrentArticleCount();
        SharedPreferences settings = PaywallService.getSharedPreferences();
        float paywallCurrentArticleCount = settings.getFloat(
                PaywallConstants.PW_CURRENT_ARTICLE_COUNT, 0);
        return paywallCurrentArticleCount;
    }
}
