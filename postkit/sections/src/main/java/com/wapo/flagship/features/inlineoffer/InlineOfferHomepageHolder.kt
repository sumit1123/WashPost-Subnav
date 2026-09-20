package com.wapo.flagship.features.inlineoffer

import android.view.View
import androidx.compose.ui.platform.ComposeView
import com.wapo.flagship.features.inlineoffer.ui.InlineOfferType
import com.wapo.flagship.features.inlineoffer.ui.showInlineOfferView
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import com.wapo.flagship.features.grid.model.InlineOffer
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent


class InlineOfferHomepageHolder(
    private val composeView: ComposeView,
    private val isSingleColumn: Boolean,
    private val offer: SectionInlineMessage?,
    private val shouldSuppressAds: Boolean,
    private val onClick: (SectionInlineMessage?) -> Unit,
    val onImpressionEvent: ((BannerLifecycleEvent) -> Unit)?
) : GridViewHolder(composeView) {

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        composeView.visibility = View.VISIBLE
        (gridAdapter.items[position] as? InlineOffer)?.let { _ ->
            showInlineOfferView(
                composeView,
                offer?.isEligiblePromo == true,
                isSingleColumn,
                title = offer?.title,
                body = offer?.body,
                url = offer?.url,
                action = offer?.action,
                InlineOfferType.HOMEPAGE,
                onClick = {
                    onClick.invoke(offer)
                },
            )
        }
        offer?.attributionInfo?.let {
            onImpressionEvent?.invoke(BannerLifecycleEvent.StartImpression(it))
        }
    }

    override fun unbind() {
        offer?.attributionInfo?.let {
            onImpressionEvent?.invoke(BannerLifecycleEvent.EndImpression(it))
        }
    }
}