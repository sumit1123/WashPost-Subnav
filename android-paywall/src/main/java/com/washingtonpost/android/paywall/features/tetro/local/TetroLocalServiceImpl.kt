/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.features.tetro.local

import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.features.tetro.TetroManager
import com.washingtonpost.android.paywall.features.tetro.TetroResponse
import com.washingtonpost.android.paywall.features.tetro.remote.ArticleBody
import com.washingtonpost.android.paywall.features.tetro.remote.ArticleObj
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper
import com.washingtonpost.android.paywall.helper.PaywallDbHelper
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.metering.MeteringPrefs
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.util.PaywallConstants
import okhttp3.Headers

class TetroLocalServiceImpl : TetroLocalService {
    private var weightedArticles: HashMap<String, Float> = hashMapOf()

    /**
     * Update Meter Limit and Weight Articles map [updateWeightedArticles] in shared preferences.
     * Update articles in db to have [PaywallDbHelper.PW_TETRO_SYNCED] set to true
     */
    override fun updateMeterData(response: TetroResponse?) {
        response?.apply {
            val meterCount = minOf(this.data.meterCount, this.data.meterLimit).toFloat()
            MeteringPrefs.setCurrentArticleCount(
                PaywallConstants.PW_CURRENT_ARTICLE_COUNT,
                meterCount
            )
            MeteringPrefs.setCurrentArticleMeterReason(PaywallConstants.PW_PREF_CURRENT_ARTICLE_METER_REASON,
                this.data.meterState
            )
            PaywallService.getInstance().currentTetroAction = this.action
            PaywallService.getInstance().currentTetroActionCodes = this.data.actionCodes.joinToString(",")
            MeteringPrefs.setMeterCycleDays(data.meterCycleDays)
            MeteringPrefs.setMaxArticleLimit(this.data.meterLimit)
            MeteringPrefs.setFreeArticlesRemaining(this.data.freeTrialConsumptionCount)
            updateWeightedArticles(this.data.weightedArticles)
            PaywallService.getConnector()
                .trackTetroEvent(this.data.meterCount.toFloat(), this.data.meterState)
        }

        PaywallCounterHelper.updateArticlesSynced(PaywallDbHelper.PW_ARTICLE_TABLE)
    }

    /**
     * Store 'wp_pwapi_ar' cookie which holds the metering state for user. This will be passed back in
     * the POST request under a `cookie` header.
     * Parse for 'wapo_actmgmt' cookie and update subAcctMgmt if present and not empty.
     * Same cookie, different name depending if origin is Tetro (former) or Profile (latter).
     */
    override fun storeCookies(headers: Headers) {
        val cookies = headers.values("Set-Cookie")

        val stateCookie = cookies.firstOrNull { it.startsWith("wp_pwapi_ar") }
        Logger.d("Tetro", "State Cookie : $stateCookie")
        stateCookie?.apply {
            PaywallPrefHelper.setPrefTetroStateCookie(this)
        }

        val subAcctMgtCookie =
            cookies.firstOrNull { it.startsWith("wapo_actmgmt") && !it.startsWith("wapo_actmgmt=;") }
        Logger.d("Tetro", "SubAcctMgmt : $subAcctMgtCookie")
        subAcctMgtCookie?.apply {
            PaywallService.getConnector().subAcctMgmt = this
        }

        val geoCookie =
            cookies.firstOrNull { it.startsWith("wp_geo") && !it.startsWith("wp_geo=;") }
        Logger.d("Tetro", "Geo : $geoCookie")
        geoCookie?.apply {
            PaywallPrefHelper.setPrefGeoCookie(this)
        }

        val rctCookie =
            cookies.firstOrNull { it.startsWith("wp_ak_v_mab") && !it.startsWith("wp_ak_v_mab=;") }
        Logger.d("Tetro", "Rct : $rctCookie")
        rctCookie?.apply {
            PaywallPrefHelper.setPrefTetroRctCookie(this)
        }
    }

    /**
     * Get list of articles that are buffered and have not been sent to Tetro.
     * This will be used for when articles metered articles are stored offline.
     */
    override fun getBufferedReadList(): List<ArticleStub>? {
        return PaywallCounterHelper.getArticleListNotSynced(PaywallDbHelper.PW_ARTICLE_TABLE)
    }

    /**
     * Return the tetro state cookie
     */
    override fun getStateCookie(): String? {
        return PaywallPrefHelper.getPrefTetroStateCookie()
    }

    /**
     * Take first 10 weighted articles map from Tetro POST response and transform it to a map with
     * article url as KEY and weight int as VALUE
     */
    private fun updateWeightedArticles(weightedArticlesResponse: HashMap<String, Map<String, String>>) {
        weightedArticles.clear()
        if (!weightedArticlesResponse.isNullOrEmpty()) {
            weightedArticlesResponse.entries.take(10).forEach {
                val weight = it.value.getOrElse("0") { "1.0" }.toFloatOrNull() ?: 1.0f
                val key = DEFAULT_BODY.replace("/?", it.key)
                weightedArticles[key] = weight
            }
        }

        if (!weightedArticles.isNullOrEmpty()) {
            MeteringPrefs.setArticleWeights(PaywallConstants.PW_ARTICLE_WEIGHTS, weightedArticles)
            MeteringPrefs.setArticleWeightsLastFetched(System.currentTimeMillis())
        }
    }

    companion object {
        private const val DEFAULT_BODY = "https://www.washingtonpost.com/?"
        private val Tag = TetroManager::class.simpleName
        private val defaultBody = ArticleBody(listOf(ArticleObj(DEFAULT_BODY, null)))
        private const val GIFT_TOKEN_KEY = "pwapi_token"
    }

}