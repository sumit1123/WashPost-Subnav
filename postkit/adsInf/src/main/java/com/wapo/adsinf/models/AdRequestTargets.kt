package com.wapo.adsinf.models

import com.wapo.adsinf.AdManager
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog

class AdRequestTargets {
    private val adProvider get() = AdManager.Companion.getInstance().adProvider
    private val _customTargetsMap: MutableMap<String, List<String>> = mutableMapOf()
    val customTargetsMap: Map<String, List<String>> = _customTargetsMap
    private val _simpleCustomTargetsMap: MutableMap<String, String> = mutableMapOf()
    val simpleCustomTargetsMap: Map<String, String> = _simpleCustomTargetsMap
    var contentUrl: String? = null
        private set

    /**
     * Sets custom params value from "Test Options - Ads" for Ad Ops testing (debug & prod).
     * Sets here as long as "Test options - Ads" options are enabled and value is not empty in settings.
     */
    fun addAdOpsTestValues(isTestAdsEnabled: Boolean? = null, testAdsValue: String? = null) {
        val isTestAdsEnabled = isTestAdsEnabled ?: adProvider.isTestAdsEnabled
        val testAdsValue = testAdsValue ?: adProvider.testAdsValue
        if (isTestAdsEnabled && testAdsValue.isNotBlank()) {
            _customTargetsMap.put(AD_OPS_TEST_KEY, testAdsValue.trim().split(","))
        }
    }

    /**
     * Sets app version name to the custom params
     */
    fun addAppVersion(appVersion: String? = null) {
        _customTargetsMap.put(APP_VERSION_KEY, listOf(appVersion ?: adProvider.appVersionName))
    }

    fun addSubscriptionStatus(status: String? = null) {
        _customTargetsMap.put(SUB_STATUS_KEY, listOf(status ?: adProvider.adSubscriptionStatus))
    }

    fun addHourOfDay() {
        _customTargetsMap.put(HOUR_OF_DAY_KEY, listOf(adProvider.hourOfDay.toString()))
    }

    fun addPushTopic(topic: String?) {
        if (!topic.isNullOrBlank()) {
            // adops temporarily wants to use both keys even though they have the same value
            val values = listOf("apps_alert_$topic")
            _customTargetsMap.put(ITID_KEY, values)
            _customTargetsMap.put(ITID_TEMP_KEY, values)
        }
    }

    /**
     * j_ucid and j_tid are added to ads request to uniquely attribute a page view to user
     * and the time at which user read the article.
     */
    fun addAnalyticsTags(
        jTid: Long?,
        jUcid: String? = null,
        logEventExtras: (EventLog.Builder) -> EventLog.Builder = { it },
    ) {
        if (jTid != null && jTid != 0L) _customTargetsMap.put(J_TID_KEY, listOf(jTid.toString()))
        _customTargetsMap.put(J_UCID_KEY, listOf(jUcid ?: adProvider.jUcid))

        when {
            jUcid == null ->
                EventLog
                    .Builder()
                    .apply {
                        setMessage("$J_UCID_KEY is null in an ad req")
                        setModule(LogModules.ADS)
                        logEventExtras(this)
                    }.run {
                        RemoteLog.w(adProvider.applicationContext, build())
                    }

            jTid == null || jTid == 0L ->
                EventLog
                    .Builder()
                    .apply {
                        setMessage("$J_TID_KEY is null in an ad req")
                        setModule(LogModules.ADS)
                        logEventExtras(this)
                    }.run {
                        RemoteLog.w(adProvider.applicationContext, build())
                    }

            else -> {
                //  Logger.d("JUCID ", "j_ucid from article ad req: ${mutableListOf(jUcid)}")
                //  Logger.d("JTID ", "j_tid from article ad req: ${mutableListOf("$jTid").toString()}")
            }
        }
    }

    fun addSlotSizeParameters(adSlotType: AdSlotType) {
        val value = when (adSlotType) {
            AdSlotType.TALL -> "tall"
            else -> "short"
        }
        _customTargetsMap.put(AD_SIZE_KEY, listOf(value))
    }

    fun addBrandSuitabilityKey(value: String) {
        _customTargetsMap.put(BRAND_SUITABILITY_KEY, listOf(value))
    }

    fun addSection(section: String) {
        _customTargetsMap.put(SECTION_KEY, listOf(section))
    }

    fun addSectionAdTargetingValues(
        primarySectionId: String,
        id: String? = null,
        adCall: Any? = null,
        permutive: Any? = null,
        contentUrl: String? = null
    ) {
        val targetingValues = adProvider.getSectionsAdTargetingValues(
            primarySectionId,
            id,
            adCall,
            permutive,
            contentUrl
        )
        _customTargetsMap.putAll(targetingValues)
    }

    fun addAllSectionAdTargetingValues(map: Map<String, List<String>>) {
        _customTargetsMap.putAll(map.filter { it.key.isNotEmpty() && it.value.isNotEmpty() })
    }

    fun addAllArticlesAdTargetingValues(map: Map<String, List<String>>) {
        _customTargetsMap.putAll(map.filter { it.key.isNotEmpty() && it.value.isNotEmpty() })
    }

    fun addArticleTags(tags: List<String>) {
        _customTargetsMap.put(ARTICLE_TAGS_KEY, tags)
    }

    fun addPageId(arcId: String?) {
        if (!arcId.isNullOrBlank()) {
            _customTargetsMap.put(PAGE_ID_KEY, listOf(arcId))
        }
    }

    fun addAdPosition(pos: String) {
        _simpleCustomTargetsMap.put(POSITION_KEY, pos)
    }

    fun addNimbusEnabled(enabled: Boolean) {
        _simpleCustomTargetsMap.put(NIMBUS_ENABLED_KEY, if (enabled) "1" else "0")
    }

    fun setRenderCount(count: Int) {
        _simpleCustomTargetsMap.put(RENDER_COUNT, count.toString())
    }

    fun getRenderCount(): Int {
        return simpleCustomTargetsMap[RENDER_COUNT]?.toIntOrNull() ?: 1
    }

    fun setContentUrl(contentUrl: String) {
        this.contentUrl = contentUrl
    }

    fun putAll(map: Map<String, List<String>>) {
        _customTargetsMap.putAll(map)
    }

    fun copy(): AdRequestTargets {
        val copy = AdRequestTargets()
        copy._customTargetsMap.putAll(
            customTargetsMap.mapValues { (_, value) -> value.toList() }
        )
        copy._simpleCustomTargetsMap.putAll(simpleCustomTargetsMap)
        copy.contentUrl = contentUrl
        return copy
    }

    override fun toString(): String {
        return super.toString() + "(customTargetsMap=$customTargetsMap, simpleCustomTargetsMap=$simpleCustomTargetsMap, contentUrl=$contentUrl)"
    }

    companion object {
        private const val AD_OPS_TEST_KEY = "kw"
        private const val APP_VERSION_KEY = "av"
        private const val SUB_STATUS_KEY = "sub="
        private const val ITID_KEY = "itid"
        private const val ITID_TEMP_KEY = "itid_temp"
        private const val J_TID_KEY = "j_tid"
        private const val J_UCID_KEY = "j_ucid"
        private const val AD_SIZE_KEY = "adsize"
        private const val BRAND_SUITABILITY_KEY = "bs"
        private const val ARTICLE_TAGS_KEY = "wp_tag"
        private const val POSITION_KEY = "pos"
        private const val SECTION_KEY = "section"
        private const val NIMBUS_ENABLED_KEY = "nd"
        private const val HOUR_OF_DAY_KEY = "hour"
        private const val PAGE_ID_KEY = "pageid"
        private const val RENDER_COUNT = "zeus_rendercount"

        fun getDefault() = AdRequestTargets().apply {
            addAdOpsTestValues()
            addAppVersion()
            addSubscriptionStatus()
            addHourOfDay()
            setRenderCount(1)
        }
    }
}