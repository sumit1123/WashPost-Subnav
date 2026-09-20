package com.wapo.flagship.features.subscribebanner.utils

import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.wapo.android.commons.extensions.toSpannableBuilder
import com.wapo.android.commons.util.setStyleSpan
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.grid.model.GlobalBannerMessage
import com.wapo.flagship.features.grid.model.GlobalBannerState
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.washingtonpost.android.paywall.models.PromoAction
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.sections.databinding.GlobalBannerBinding

class GlobalBannerViewStateHelper(
    val binding: GlobalBannerBinding,
    val context: Context,
    val shouldSuppressAds: Boolean,
    val onImpressionEvent: ((BannerLifecycleEvent) -> Unit)?
) {
    /**
     * Setup the Subscribe Banner with
     * - Subscribe Primary Cta Button
     */
    @OptIn(UnstableApi::class)
    fun showNoSub(
        noSub: GlobalBannerState.NoSub,
        onClick: ((BannerEvent) -> Unit)?,
    ) {
        val primaryText =
            noSub.offer?.offerTitle ?: context.resources.getString(R.string.no_sub_primary_text)
        val secondaryText = noSub.offer?.offerSubtitle
            ?: context.resources.getString(R.string.no_sub_secondary_text)

        // Set the background color for the subscribe banner
        binding.mainBanner.setVisible(true)
        binding.mainBanner.setBackgroundResource(R.color.subscribe_banner_bg)
        binding.mainButton.background =
            ContextCompat.getDrawable(context, R.drawable.button_rounded_blue)

        // Set the primary bold text
        val primarySpan = SpannableString(primaryText)
        primarySpan.setStyleSpan(primaryText, primaryText, R.style.SubscribeBannerMainText, context)
        binding.primaryText.text = primarySpan

        // Set the secondary text
        val fullSecondaryText = secondaryText
        val secondarySpan = SpannableString(fullSecondaryText)
        secondarySpan.setStyleSpan(
            fullSecondaryText,
            fullSecondaryText,
            R.style.SubscribeBannerSecondaryText,
            context
        )

        binding.secondaryText.text = secondarySpan
        binding.secondaryText.movementMethod = LinkMovementMethod()

        if (!noSub.isSignedIn) {
            //use GlobalBannerConfig or default
            setSubscribeOrResubscribeButtonText(noSub)

            binding.mainButton.setOnClickListener {
                noSub.offer?.let {
                    onClick?.invoke(
                        BannerEvent.GlobalBannerClicked(it)
                    )
                }
            }
        } else if (noSub.offer?.productId == null) {
            setSubscribeOrResubscribeButtonText(noSub)
            binding.mainButton.setOnClickListener {
                noSub.offer?.let {
                    onClick?.invoke(
                        BannerEvent.GlobalBannerClicked(it)
                    )
                }

            }
        } else {
            setSubscribeOrResubscribeButtonText(noSub)
            binding.mainButton.setOnClickListener {
                noSub.offer.let {
                    onClick?.invoke(
                        BannerEvent.GlobalBannerClicked(it)
                    )
                }

            }
        }
        showOrHideCloseButton(noSub.offer, onImpressionEvent)
    }

    private fun setSubscribeOrResubscribeButtonText(noSub: GlobalBannerState.NoSub) {
        with(binding.mainButton) {
            if (noSub.offer?.offerDetail?.isNotEmpty() == true) {
                text = noSub.offer.offerDetail.toSpannableBuilder()
                setTextAppearance(context, R.style.SubscribeBannerButtonText)
            } else if (noSub.offer?.ctaText?.isNotEmpty() == true) {
                text = noSub.offer.ctaText
                setTextAppearance(context, R.style.SubscribeBannerButtonText)
            } else if (noSub.isTerminated) {
                text = context.resources.getString(R.string.resubscribe)
                setTextAppearance(context, R.style.SubscribeBannerButtonTextBold)
            } else {
                text = context.resources.getString(R.string.subscribe)
                setTextAppearance(context, R.style.SubscribeBannerButtonTextBold)
            }
        }
    }

    /**
     * Hides the banner for when user has an Active Sub
     * Or Sub State is Unknown
     */
    fun hideBanner() {
        binding.mainBanner.setVisible(false)
    }

    /**
     * Show promotional messages for subscribers. Basically messages that have an URL to open.
     * No checks for url messages for now, since we don't have any good logic to filter these out.
     *
     * Banner visibility logic:
     * - If passedIterableGuardrails is true: also check that legacy promotionalOfferConditions are met
     * - If passedIterableGuardrails is false: hide banner (guardrails exist and failed)
     * - If passedIterableGuardrails is null: fall back to legacy promotionalOfferConditions
     */
    fun showSub(
        sub: GlobalBannerState.Sub,
        onClick: ((BannerEvent) -> Unit)?,
        passedIterableGuardrails: Boolean? = null
    ) {
        val offer = sub.offer
        val isAdFreeOffer = offer?.isAdFreeProduct == true

        val promotionalOfferConditions =
            !(isAdFreeOffer && shouldSuppressAds) &&
                    (offer?.offerUrl?.isNotEmpty() == true || isAdFreeOffer)

        val shouldShow = when (passedIterableGuardrails) {
            true -> promotionalOfferConditions // ensure legacy conditions are still met, even though guardrails pass
            false -> false // guardrails exist and failed
            null -> promotionalOfferConditions // no guardrails, use legacy logic
        }

        if (offer?.offerTitle?.isNotEmpty() == true && shouldShow) {
            val primaryText = offer.offerTitle
            val secondaryText = offer.offerSubtitle ?: ""

            // Set the background color for the subscribe banner
            binding.mainBanner.setVisible(true)
            binding.mainBanner.setBackgroundResource(R.color.subscribe_banner_bg)
            binding.mainButton.background =
                ContextCompat.getDrawable(context, R.drawable.button_rounded_blue)

            // Set the primary bold text
            val primarySpan = SpannableString(primaryText)
            primarySpan.setStyleSpan(
                primaryText,
                primaryText,
                R.style.SubscribeBannerMainText,
                context
            )
            binding.primaryText.text = primarySpan

            // Set the secondary text
            if (secondaryText.isNotEmpty()) {
                binding.secondaryText.visibility = View.VISIBLE
                val fullSecondaryText = secondaryText
                val secondarySpan = SpannableString(fullSecondaryText)
                secondarySpan.setStyleSpan(
                    fullSecondaryText,
                    fullSecondaryText,
                    R.style.SubscribeBannerSecondaryText,
                    context
                )

                binding.secondaryText.text = secondarySpan
                binding.secondaryText.movementMethod = LinkMovementMethod()
            } else {
                binding.secondaryText.visibility = View.GONE
            }

            with(binding.mainButton) {
                if (offer.ctaText?.isNotEmpty() == true) {
                    text = offer.ctaText
                    setTextAppearance(context, R.style.SubscribeBannerButtonText)
                    setOnClickListener {
                        // Use the promoAction from iterable, otherwise create a new PromoAction.Open with the offerUrl and action
                        val promoAction = offer.promoAction ?: PromoAction.Open(
                            url = offer.offerUrl ?: "",
                            action = offer.action
                        )
                        offer.let {
                            onClick?.invoke(
                                BannerEvent.GlobalBannerClicked(it)
                            )
                        }
                    }
                } else {
                    visibility = View.GONE
                }
            }
            showOrHideCloseButton(offer, onImpressionEvent)
        } else {
            hideBanner()
        }
    }

    private fun showOrHideCloseButton(
        offer: GlobalBannerMessage?,
        onImpressionEvent: ((BannerLifecycleEvent) -> Unit)?
    ) {
        binding.closeButton.run {
            if (offer?.dismissible == true) {
                visibility = View.VISIBLE
                setOnClickListener {
                    offer.attributionInfo?.let {
                        onImpressionEvent?.invoke(BannerLifecycleEvent.Dismiss(offer.attributionInfo))
                    }
                }
            } else {
                visibility = View.GONE
            }
        }
    }
}
