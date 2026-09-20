package com.washingtonpost.android.paywall.bottomsheet.ui.component.old

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.wapo.android.commons.util.animateScale
import com.washingtonpost.android.config.domain.models.config.paywall.Product
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.BillingOptionBoxBinding

class BillingOptionView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    var product: Product? = null
    private val binding = BillingOptionBoxBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * [isActive] will call [updateState] when it's value is updated
     */
    var isActive: Boolean = true
        set(value) {
            updateState(value)
        }

    /**
     * set [billing_period_label] - Usually MONTHLY, YEARLY
     */
    fun setPeriod(periodLabel: String) {
        binding.billingPeriodLabel.text = periodLabel
    }

    /**
     * set [primary_offer_text] bold text
     */
    fun setPrimaryOffer(primaryOfferText: String) {
        binding.primaryOfferText.text = primaryOfferText
    }

    /**
     * set [secondary_offer_text] which may be hidden in certain cases like Terminated Subs
     */
    fun setSecondaryOffer(secondaryOfferText: String?) {
        binding.secondaryOfferText.visibility = if (secondaryOfferText == null) View.GONE else View.VISIBLE
        secondaryOfferText?.apply {
            binding.secondaryOfferText.text = this
        }
    }

    fun setSavingsPill(savings: String) {
        binding.savingsPill.visibility = if (savings.isEmpty()) View.GONE else View.VISIBLE
        binding.savingsPill.text = savings
        binding.savingsPill.apply {
            if(visibility == View.VISIBLE) {
                binding.savingsPill.animateScale(0f, 1f, 300, 500, OvershootInterpolator()).start()
            }
        }
    }

    /**
     * Update style of this [BillingOptionView] based on if it is selected / unselected
     */
    private fun updateState(active: Boolean) {
        if (active) {
            binding.container.background = ContextCompat.getDrawable(context, R.drawable.billing_selector_box_selected)
            binding.checkCircleSelected.animateScale(0f, 1f, 200, 0, OvershootInterpolator()).start()
        } else {
            binding.container.background = ContextCompat.getDrawable(context, R.drawable.billing_selector_box_normal)
            binding.checkCircleSelected.animateScale(binding.checkCircleSelected.scaleX, 0f, 200, 0, AccelerateInterpolator()).start()
        }
    }
}