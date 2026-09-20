package com.wapo.adsinf.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.wapo.adsinf.AdManager
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import com.wapo.adsinf.models.NetworkExtras
import com.wapo.adsinf.publisher.DefaultPublisher
import com.wapo.adsinf.publisher.IPublisher
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.UiUtils
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType
import java.lang.Math.random

object AdsUtil {
    val DEFAULT_AD_SIZE = AdDimension.Medium
    const val MIN_HEIGHT_TO_RESERVE_IN_AD_CONTAINER_TABLET = 250
    val MIN_HEIGHT_TO_RESERVE_IN_AD_CONTAINER
        get() = when {
            UiUtils.isPhone(adProvider.applicationContext) -> 375
            else -> MIN_HEIGHT_TO_RESERVE_IN_AD_CONTAINER_TABLET
        }

    private val TAG = AdsUtil::class.java.simpleName
    private val adProvider get() = AdManager.getInstance().adProvider
    private val bannersConfig get() = ConfigManager.getInstance().config.adsConfig.bannersConfig

    @JvmStatic
    fun findAdDimensionsFromDeviceWidth(): List<AdDimension> {
        val result = mutableListOf<AdDimension>()
        val deviceWidth = AppContextUtils.getDeviceWidthInDp()
        if (deviceWidth >= 620) result.add(AdDimension.Banner620x250)
        if (deviceWidth >= 728) result.add(AdDimension.Banner728x90)
        if (deviceWidth >= 970) result.add(AdDimension.Banner970x250)
        return result
    }

    @JvmStatic
    fun findAdDimensionFromAdSlotType(slotType: AdSlotType?): AdDimension {
        return when (slotType) {
            AdSlotType.TALL -> AdDimension.Tall
            else -> AdDimension.Medium
        }
    }

    @JvmStatic
    fun getPrimarySectionIdValues(primarySectionId: String?): List<Pair<String, String>>? {
        primarySectionId ?: return null
        val sections = mutableListOf<Pair<String, String>>()
        var stringBuilder = ""
        primarySectionId.split("/").filter { it.isNotEmpty() }.forEachIndexed { index, str ->
            if (index == 0) {
                stringBuilder = str
                sections.add(Pair("section", stringBuilder))
            } else {
                stringBuilder += "/$str"
                sections.add(Pair("subsection$index", stringBuilder))
            }
        }
        Logger.d(TAG, "Primary Section ID: $sections")
        return sections
    }

    fun getAdUnitId(
        context: Context,
        contentType: String,
        adType: String,
        adKey: String,
        adKeyPrepend: String = "",
        publisher: IPublisher = DefaultPublisher(context),
    ): String {
        if (publisher.adUnitId.isNotEmpty()) return publisher.adUnitId
        val keyPrepend = adKeyPrepend.ifEmpty {
            if (UiUtils.isPhone(context)) publisher.adKeyRootPathForMob else publisher.adKeyRootPathForTab
        }

        return String.format("/%s/%s/%s", keyPrepend, contentType, adType)
    }

    fun getDefaultNetworkExtras(): NetworkExtras? {
        val ccpaBundle = adProvider.getCCPABundle()
        return ccpaBundle?.let { NetworkExtras(it) }
    }

    fun isAdLoaderAvailable(loaderType: AdLoaderType): Boolean {
        return when (loaderType) {
            AdLoaderType.GOOGLE -> true
            AdLoaderType.NIMBUS -> bannersConfig.nimbus.enabled && AdManager.isNimbusInitialized
        }
    }

    fun isAdBidSourceAvailable(bidSource: AdBidSource): Boolean {
        return when (bidSource) {
            AdBidSource.GAM -> true
            AdBidSource.APS -> !adProvider.isEURegion && AdManager.isA9Initialized
            AdBidSource.ADMOB -> true
            AdBidSource.NIMBUS -> bannersConfig.nimbus.enabled && AdManager.isNimbusInitialized
        }
    }

    fun setAdLayoutVisibility(adLayout: View, isVisible: Boolean) {
        adLayout.isVisible = isVisible
        (adLayout as? ViewGroup)?.forEach { it.isVisible = isVisible }
    }

    fun getAdLoadConfig(): List<AdLoaderConfig> {
        if (isAdLoaderAvailable(AdLoaderType.NIMBUS)) {
            val nimbusConfig = bannersConfig.nimbus
            val nimbusLoadConfig = nimbusConfig.loadConfig
            if (nimbusLoadConfig.isNotEmpty()) {
                if (adProvider.isDebugBuild && nimbusConfig.testPercent > 0) {
                    val useNimbus = random() * 100 <= nimbusConfig.testPercent
                    if (useNimbus) return nimbusLoadConfig
                } else {
                    return nimbusLoadConfig
                }
            }
        }
        return bannersConfig.defaultLoadConfig
    }
}