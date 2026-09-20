package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import com.wapo.adsinf.models.AdsModel
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.InlineMessageItem
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.inlineoffer.ui.InlineOfferType
import com.wapo.flagship.features.inlineoffer.ui.showInlineOfferView
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.washingtonpost.android.databinding.InlineOfferToggleBinding


class InlineMessageArticleHolder(
    private val binding: InlineOfferToggleBinding,
    private val articleInlineMessage: ArticleInlineMessage?,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
    private val adsModel: AdsModel
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<InlineMessageItem>(binding.root) {

    private var impressionStarted = false

    override fun bind(item: InlineMessageItem, position: Int) {
        super.bind(item, position)
        binding.root.visibility = View.VISIBLE
        showInlineOfferView(
            binding.placeholder,
            articleInlineMessage?.isEligiblePromo == true,
            false,
            articleInlineMessage?.title,
            articleInlineMessage?.body,
            articleInlineMessage?.url,
            articleInlineMessage?.action,
            InlineOfferType.ARTICLE
        ) {
            articleInlineMessage?.let {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.InlineMessageBannerEvent(
                        BannerEvent.BannerClicked(
                            message = null,
                            iamMessageType = IamMessageType.ARTICLE.type
                        )
                    )
                )
            }
        }
        if (!impressionStarted) {
            impressionStarted = true
            articleInlineMessage?.attributionInfo?.let {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.InlineMessageBannerEvent(
                        BannerEvent.ImpressionEvent(BannerLifecycleEvent.StartImpression(it))
                    )
                )
            }
        }
    }

    override fun unbind() {
        if (impressionStarted) {
            impressionStarted = false
            articleInlineMessage?.attributionInfo?.let {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.InlineMessageBannerEvent(
                        BannerEvent.ImpressionEvent(BannerLifecycleEvent.EndImpression(it))
                    )
                )
            }
        }
    }
}
