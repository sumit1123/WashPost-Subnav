package com.wapo.flagship.features.articles2.adinjector

import com.wapo.android.commons.util.Logger
import android.util.SparseArray
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import com.wapo.adsinf.utils.AdsUtil.DEFAULT_AD_SIZE
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles.AdViewInfo
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.ListItem
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.AdConfig
import com.washingtonpost.android.config.domain.models.config.AdPositionConfig

class ClassicAdInjector2 : AdInjector2 {
    private val adsConfig get() = ConfigManager.getInstance().config.adsConfig

    override fun getAdPositions(articleModel: Article2?): SparseArray<AdViewInfo> = SparseArray(1)

    override fun getAdPositions(
        articleModel: Article2?,
        articleItems: List<Any?>?,
    ): SparseArray<AdViewInfo> {
        val adPositions = SparseArray<AdViewInfo>(1)
        try {
            val adConfig = adsConfig.nonSubscriberAdConfig
            if (!articleItems.isNullOrEmpty()) {
                var minParagraphsBetweenAds: Int
                var minCharactersBetweenAds: Int
                var minCharactersBeforeAd: Int
                var minCharactersAfterAd: Int
                var maxNumberOfAds: Int
                val showBottomAd: Boolean

                var numberOfAds = 0
                var numberOfParagraphsSinceLastAd = 0
                var numberOfCharactersSinceLastAd = 0
                var numberOfCharactersBeforeAd = 0
                var isPrevItemTextParagraph = false

                for (index in articleItems.indices) {
                    // set required values from config, or use defaults
                    minParagraphsBetweenAds =
                        getIntValue(
                            AdPositionConfig.MIN_PARAGRAPHS_BETWEEN_ADS,
                            numberOfAds,
                            adConfig,
                            DEFAULT_MIN_PARAGRAPHS_BETWEEN_ADS,
                        )
                    minCharactersBetweenAds =
                        getIntValue(
                            AdPositionConfig.MIN_CHARACTERS_BETWEEN_ADS,
                            numberOfAds,
                            adConfig,
                            DEFAULT_MIN_CHARACTERS_BETWEEN_ADS,
                        )
                    minCharactersBeforeAd =
                        getIntValue(
                            AdPositionConfig.MIN_CHARACTERS_BEFORE_AD,
                            numberOfAds,
                            adConfig,
                            DEFAULT_MIN_CHARACTERS_BEFORE_AD,
                        )
                    minCharactersAfterAd =
                        getIntValue(
                            AdPositionConfig.MIN_CHARACTERS_AFTER_AD,
                            numberOfAds,
                            adConfig,
                            DEFAULT_MIN_CHARACTERS_AFTER_AD,
                        )
                    maxNumberOfAds =
                        getIntValue(
                            AdPositionConfig.MAX_NUMBER_OF_ADS,
                            numberOfAds,
                            adConfig,
                            DEFAULT_MAX_NUMBER_OF_ADS,
                        )
                    val currentItem = articleItems[index]
                    if (numberOfAds >= maxNumberOfAds) break
                    // check if the current item is text
                    if (currentItem != null && (currentItem::class == SanitizedHtml::class || currentItem::class == ListItem::class)) {
                        if (isPrevItemTextParagraph &&
                            numberOfParagraphsSinceLastAd >= minParagraphsBetweenAds &&
                            numberOfCharactersSinceLastAd >= minCharactersBetweenAds &&
                            numberOfCharactersBeforeAd >= minCharactersBeforeAd
                        ) {
                            val numberOfCharactersAfterAd: Int =
                                countCharactersAfter(
                                    articleItems,
                                    index,
                                    minCharactersAfterAd,
                                )
                            if (numberOfCharactersAfterAd > minCharactersAfterAd) {
                                // since we insert in order one-by-one, we must also add the total so far
                                if (isFirstAdSlot(numberOfAds)) {
                                    /** Remote config may override the first ad slot's dimensions
                                     *  Default width and height for the first ad slot are 300x600
                                     *  Config overrides to 300x250, matching other ads
                                     */
                                    val firstAdSlotConfig =
                                        adsConfig
                                            .firstAdSlot
                                            .defaultConfig
                                    val paths: ArrayList<String> =
                                        firstAdSlotConfig.get(
                                            AdPositionConfig.SECTION_PATH,
                                        ) as? ArrayList<String>
                                            ?: ArrayList()
                                    var width = DEFAULT_AD_WIDTH
                                    var height = DEFAULT_TALL_AD_HEIGHT
                                    val shouldOverrideSize =
                                        firstAdSlotConfig.get(
                                            AdPositionConfig.OVERRIDE_FIRST_SLOT,
                                        ) == true
                                    val isSectionPathAvailable =
                                        isSectionTargetAvailable(
                                            paths,
                                            articleModel,
                                        ) as Boolean
                                    if (shouldOverrideSize && isSectionPathAvailable) {
                                        (firstAdSlotConfig.get(AdPositionConfig.AD_WIDTH) as? Number)?.toInt()?.let {
                                            width = it
                                        }
                                        (firstAdSlotConfig.get(AdPositionConfig.AD_HEIGHT) as? Number)?.toInt()?.let {
                                            height = it
                                        }
                                    }
                                    adPositions.put(
                                        index + adPositions.size(),
                                        AdViewInfoImpl(
                                            getAdKey(articleModel),
                                            articleModel?.contenturl,
                                            articleModel?.title,
                                            width,
                                            height,
                                            getAdSlotType(width, height),
                                            "",
                                            articleModel?.contentType
                                        ),
                                    )
                                } else {
                                    adPositions.put(
                                        index + adPositions.size(),
                                        AdViewInfoImpl(
                                            getAdKey(articleModel),
                                            articleModel?.contenturl,
                                            articleModel?.title,
                                            DEFAULT_AD_WIDTH,
                                            DEFAULT_SHORT_AD_HEIGHT,
                                            AdSlotType.SHORT,
                                            "",
                                            articleModel?.contentType
                                        ),
                                    )
                                }
                                numberOfAds++
                                numberOfParagraphsSinceLastAd = 0
                                numberOfCharactersSinceLastAd = 0
                                numberOfCharactersBeforeAd = 0
                            }
                        }
                        numberOfParagraphsSinceLastAd++
                        numberOfCharactersSinceLastAd += numberOfCharacters(currentItem)
                        numberOfCharactersBeforeAd += numberOfCharacters(currentItem)
                        isPrevItemTextParagraph = true
                    } else {
                        numberOfCharactersBeforeAd = 0
                        isPrevItemTextParagraph = false
                    }
                }
                val bottomAd = adsConfig.overrides
                showBottomAd = bottomAd.defaultConfig.get(AdPositionConfig.SHOW_BOTTOM_AD) as? Boolean
                    ?: false
                val paths: ArrayList<String> =
                    bottomAd.defaultConfig.get(
                        AdPositionConfig.SECTION_PATH,
                    ) as? ArrayList<String>
                        ?: ArrayList()
                if (showBottomAd) {
                    for (path in paths) {
                        if (articleModel?.commercialnode?.contains(path) == true) {
                            adPositions.put(
                                articleItems.size + adPositions.size(),
                                AdViewInfoImpl(
                                    getAdKey(articleModel),
                                    articleModel.contenturl,
                                    articleModel.title,
                                    DEFAULT_AD_WIDTH,
                                    DEFAULT_SHORT_AD_HEIGHT,
                                    AdSlotType.SHORT,
                                    "",
                                    articleModel.contentType
                                ),
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Ad injector error", e)
        }
        return adPositions
    }

    private fun isFirstAdSlot(numberOfAds: Int): Boolean = numberOfAds == 0

    private fun getAdSlotType(
        width: Int,
        height: Int,
    ): AdSlotType {
        if (width != DEFAULT_AD_WIDTH) {
            return AdSlotType.OTHER
        }
        return when (height) {
            DEFAULT_TALL_AD_HEIGHT -> AdSlotType.TALL
            DEFAULT_SHORT_AD_HEIGHT -> AdSlotType.SHORT
            else -> AdSlotType.OTHER
        }
    }

    private fun isSectionTargetAvailable(
        paths: ArrayList<String>,
        articleModel: Article2?,
    ): Boolean? = paths?.contains(articleModel?.commercialnode)

    private fun getAdKey(articleModel: Article2?): String? =
        ((articleModel?.commercialnode ?: articleModel?.adKey) ?: articleModel?.adkey)?.trim(
            '/',
        )

    override fun shouldSuppressAds(): Boolean = FlagshipApplication.getInstance().shouldSuppressAds()

    private fun countCharactersAfter(
        items: List<Any?>,
        index: Int,
        until: Int,
    ): Int {
        var total = 0
        for (i in (index + 1) until items.size) {
            total += numberOfCharacters(items[i])
            if (total > until) break
        }
        return total
    }

    private fun getIntValue(
        key: AdPositionConfig,
        adPosition: Int,
        adConfig: AdConfig,
        defaultValue: Int,
    ): Int =
        (adConfig.positionConfig.get(adPosition)?.get(key) as? Number)?.toInt()
            ?: (adConfig.defaultConfig.get(key) as? Number)?.toInt()
            ?: defaultValue

    private fun numberOfCharacters(item: Any?): Int =
        when (item) {
            is SanitizedHtml -> {
                val contentText =
                    when {
                        "text/plain" == item.mime -> item.content
                        (item as? SanitizedHtml)?.content == null -> ""
                        else -> item.content?.let { HtmlCompat.fromHtml(it, FROM_HTML_MODE_LEGACY) }
                    }
                contentText?.length ?: 0
            }
            is ListItem -> {
                var sum = 0
                item.content?.forEach { sum += it.length }
                sum
            }
            else -> 0
        }

    companion object {
        private val TAG: String = ClassicAdInjector2::class.java.simpleName
        private const val DEFAULT_MIN_PARAGRAPHS_BETWEEN_ADS = 4
        private const val DEFAULT_MIN_CHARACTERS_BETWEEN_ADS = 1500
        private const val DEFAULT_MIN_CHARACTERS_BEFORE_AD = 150
        private const val DEFAULT_MIN_CHARACTERS_AFTER_AD = 450
        private const val DEFAULT_MAX_NUMBER_OF_ADS = 1000
        private val DEFAULT_AD_WIDTH = DEFAULT_AD_SIZE.w
        private val DEFAULT_SHORT_AD_HEIGHT = DEFAULT_AD_SIZE.h
        private val DEFAULT_TALL_AD_HEIGHT = AdDimension.Tall.h
    }
}
