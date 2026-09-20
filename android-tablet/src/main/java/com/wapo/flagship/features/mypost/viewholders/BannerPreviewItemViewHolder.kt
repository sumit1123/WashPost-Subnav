// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.mypost.viewholders

import android.view.ViewGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.flagship.external.toDp
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.washingtonpost.android.save.databinding.MyPostBannerBinding
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.viewholders.ItemViewHolder
import com.wpds.theme.AndroidClassicTheme
import com.wpds.components.ChevronView

class BannerPreviewItemViewHolder(
    val binding: MyPostBannerBinding,
    val onBannerEvent: (BannerEvent) -> Unit,
) : ItemViewHolder(binding.root) {

    var attributionInfo: AttributionInfo? = null

    override fun bind(previewItem: PreviewItem) {
        val context = binding.root.context
        val bannerPreviewItem = previewItem as PreviewItem.BannerPreviewItem
        attributionInfo = bannerPreviewItem.attributionInfo
        val displayMetrics = context.resources.displayMetrics
        val screenWidth =
            displayMetrics.widthPixels.toDp(
                displayMetrics.density,
            )

        /**
         * If minimizeOnPhone is true, show a chevron instead of a button on small screens.
         */
        val showButton =
            !(bannerPreviewItem.minimizeOnNarrowScreen == true && screenWidth <= LARGE_WIDTH_BREAKPOINT)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent {
                AndroidClassicTheme {
                    ChevronView(
                        modifier = Modifier,
                        headlineText = bannerPreviewItem.title,
                        subtitle = bannerPreviewItem.subtitle,
                        iconUrl = bannerPreviewItem.imageUrl,
                        darkIconUrl = bannerPreviewItem.darkImageUrl,
                        showButton = showButton,
                        buttonText = bannerPreviewItem.buttonText
                    ) {
                        onBannerEvent.invoke(
                            BannerEvent.BannerClicked(
                                message = null,
                                iamMessageType = IamMessageType.MY_POST_BANNER.type
                            )
                        )
                    }
                }
            }
        }
        adjustTopMargin()
    }

    override fun unbind() {
        attributionInfo?.let {
            onBannerEvent.invoke(BannerEvent.ImpressionEvent(BannerLifecycleEvent.StartImpression(it)))
        }
    }


    /**
     * Margins are handled differently for Preview and Detail items.
     */
    private fun adjustTopMargin() {
        val params: ViewGroup.MarginLayoutParams =
            binding.myPost2Banner.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = 0
        binding.myPost2Banner.layoutParams = params
    }

    companion object {
        private const val LARGE_WIDTH_BREAKPOINT = 600
    }
}
