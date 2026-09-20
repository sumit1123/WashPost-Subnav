package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import com.wapo.adsinf.databinding.AdLayoutBinding
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.adsinf.models.AdsModel
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.common.getArticlesAdTargetingValues
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.Ad
import com.wapo.flagship.features.articles2.utils.BreakPoints

const val DEFAULT_AD_POSITION = "incontent_1"

class AdViewHolder(
    private val binding: AdLayoutBinding,
    private val articleModel: Article2?,
    private val pushTopic: String? = "",
    private val jTid: Long?,
    private val adsModel: AdsModel
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Ad>(binding.root) {
    private val context get() = binding.root.context

    override fun bind(
        item: Ad,
        position: Int,
    ) {
        if (adsModel is AdsModel.Disabled) {
            AdsUtil.setAdLayoutVisibility(adLayout = binding.root, isVisible = false)
            return
        }
        super.bind(item, position)
        AdsUtil.setAdLayoutVisibility(adLayout = binding.root, isVisible = true)
        val adSlotType = getAdSlotType(item.size)
        val adConfig = AdConfig(
            adUnitId = AdsUtil.getAdUnitId(
                context = context,
                contentType = articleModel?.contentType ?: "article",
                adKey = getAdKey(item.adPath).orEmpty(),
                adType = getAdPosition(item),
            ),
            adRequestTargets = getAdRequestTargets(item),
            networkExtras = AdsUtil.getDefaultNetworkExtras(),
            adDimensions = mutableListOf<AdDimension>().apply {
                add(AdsUtil.findAdDimensionFromAdSlotType(adSlotType))
                add(AdDimension.Fluid)
                addAll(AdsUtil.findAdDimensionsFromDeviceWidth())
            },
            adSlotType = adSlotType,
            section = "articles",
        )
        binding.adView.loadAd(adConfig)
    }

    fun getItemView(): View = binding.root

    private fun getAdRequestTargets(item: Ad): AdRequestTargets =
        AdRequestTargets.getDefault().apply {
            addPushTopic(pushTopic)
            addAnalyticsTags(
                jTid = jTid,
                logEventExtras = { it.setContentUrl(articleModel?.contenturl) }
            )
            addSlotSizeParameters(getAdSlotType(item.size))
            addAllArticlesAdTargetingValues(
                getArticlesAdTargetingValues(
                    articleModel,
                    item.primarySectionId,
                    null,
                    articleModel?.contenturl
                )
            )
            addArticleTags(articleModel?.tags?.split(",").orEmpty())
            addPageId(articleModel?.arcId)
            addAdPosition(getAdPosition(item))
            setContentUrl(
                getAdContentUrl(
                    articleModel?.contenturl.orEmpty(),
                    articleModel?.title
                )
            )
        }

    private fun getAdKey(adPath: String?): String? = adPath?.trim('/')

    private fun getAdSlotType(size: String?): AdSlotType {
        // item.size will be "tall" or "medium"
        return when (size) {
            AdSlotType.TALL.value -> AdSlotType.TALL
            else -> AdSlotType.SHORT
        }
    }

    private fun getAdPosition(item: Ad): String {
        return when (item.layoutSpec?.layout) {
            BreakPoints.Layout.SMALL -> item.position?.small ?: DEFAULT_AD_POSITION
            BreakPoints.Layout.LARGE -> item.position?.large ?: DEFAULT_AD_POSITION
            else -> DEFAULT_AD_POSITION
        }
    }

    private fun getAdContentUrl(contentUrl: String, title: String?): String {
        if (contentUrl.isNotEmpty()) {
            return contentUrl
        } else {
            RemoteLog.w(
                context,
                EventLog.Builder()
                    .setMessage("ContentUrl is missing in article ads")
                    .setModule(LogModules.ADS)
                    .set("title", title).build()
            )
        }

        val domain = "https://www.washingtonpost.com"
        return domain
    }

    override fun unbind() {
        binding.adView.release()
    }
}
