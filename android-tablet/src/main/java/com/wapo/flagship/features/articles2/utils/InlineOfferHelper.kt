package com.wapo.flagship.features.articles2.utils

import com.wapo.flagship.features.articles2.models.InlineMessageItem
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

class InlineOfferHelper {

    fun getInlineOfferItem(articleBannerMessage: BannerPaywallMessage? = null): InlineMessageItem? {
        return articleBannerMessage?.let { message ->
            val articleMetaData = ArticleInlineMessage(
                attributionInfo = message.attributionInfo,
                title = message.title,
                body = message.body,
                action = message.action,
                url = message.url,
                isEligiblePromo = null
            )
            InlineMessageItem(sectionInlineMessage = null, articleInlineMessage = articleMetaData)
        }
    }
}