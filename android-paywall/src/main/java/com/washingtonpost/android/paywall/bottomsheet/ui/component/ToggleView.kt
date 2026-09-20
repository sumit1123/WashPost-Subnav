package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.graphics.Paint
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.wapo.android.commons.util.setStyleSpan
import com.washingtonpost.android.config.domain.models.config.paywallconf.SplitType
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.bottomsheet.GroupType
import com.washingtonpost.android.paywall.databinding.PaywallToggleBinding


/**
 * Toggle Choice component used to determine which products to show. Currently Toggle can be
 * - Monthly/Yearly
 * - Core/Premium
 */
class ToggleView : FrameLayout, ViewTreeObserver.OnGlobalLayoutListener {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallToggleBinding.inflate(LayoutInflater.from(context), this, true)
    var groupType:GroupType? = null

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun init(type: SplitType?, default:Int?, onButtonCheckedListener: MaterialButtonToggleGroup.OnButtonCheckedListener) {
        if(type == SplitType.INTERVALS) {
            groupType = if(default == 1) GroupType.Yearly else GroupType.Monthly
            groupType?.apply {
                initInterval(this, onButtonCheckedListener)
            }
        } else {
            groupType = if(default == 1) GroupType.Premium else GroupType.Core
            groupType?.apply {
                initProduct(this,onButtonCheckedListener)
            }
        }
    }

    fun initInterval(
        groupType: GroupType = GroupType.Monthly,
        onButtonCheckedListener: MaterialButtonToggleGroup.OnButtonCheckedListener
    ) {
        val monthlyText = resources.getString(R.string.monthly)
        val yearlyText = resources.getString(R.string.yearly)
        val bestValueText = resources.getString(R.string.best_value)
        val fullYearlyText = "$yearlyText | $bestValueText"
        val monthlySpan = SpannableString(monthlyText)
        val yearlySpan = SpannableString(fullYearlyText)
        context?.apply {
            monthlySpan.setStyleSpan(
                monthlyText,
                monthlyText,
                R.style.period_toggle_text_bold,
                this
            )
            yearlySpan.setStyleSpan(
                fullYearlyText,
                yearlyText,
                R.style.period_toggle_text_bold,
                this
            )
            yearlySpan.setStyleSpan(fullYearlyText, " | ", R.style.period_toggle_seporater, this)
            yearlySpan.setStyleSpan(
                fullYearlyText,
                bestValueText,
                R.style.period_toggle_text_italics,
                this
            )
            binding.optionA.text = monthlySpan
            binding.optionB.text = yearlySpan
        }
        binding.periodToggle.clearOnButtonCheckedListeners()
        binding.periodToggle.addOnButtonCheckedListener(onButtonCheckedListener)
        groupType.apply {
            val checkedId = when (this) {
                GroupType.Yearly -> R.id.option_b
                else -> R.id.option_a
            }
            binding.periodToggle.check(checkedId)
        }
        /*
            In order to re-compute the text fitness criterion of option B text for interval splits.
         */
        binding.optionB.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    fun initProduct(
        groupType: GroupType = GroupType.Core,
        onButtonCheckedListener: MaterialButtonToggleGroup.OnButtonCheckedListener
    ) {
        val coreText = resources.getString(R.string.core_toggle)
        val premiumText = resources.getString(R.string.premium_toggle)

        val coreSpan = SpannableString(coreText)
        val premiumSpan = SpannableString(premiumText)
        context?.apply {
            coreSpan.setStyleSpan(
                coreText,
                coreText,
                R.style.period_toggle_text_bold,
                this
            )
            premiumSpan.setStyleSpan(
                premiumText,
                premiumText,
                R.style.period_toggle_text_bold,
                this
            )

            binding.optionA.text = coreSpan
            binding.optionB.text = premiumSpan
        }
        binding.periodToggle.clearOnButtonCheckedListeners()
        binding.periodToggle.addOnButtonCheckedListener(onButtonCheckedListener)
        groupType.apply {
            val checkedId = when (this) {
                GroupType.Premium -> R.id.option_b
                else -> R.id.option_a
            }
            binding.periodToggle.check(checkedId)
        }
    }

    /**
     * WARNING: This function is exclusively for the interval type toggles [SplitType.INTERVALS]
     * Any changes made in this function that are not strictly tied to [SplitType.INTERVALS] type toggles (including the call-site location)
     * may cause side-effects.
     */
    override fun onGlobalLayout() {
        val yearlyText = resources.getString(R.string.yearly)
        val bestValueText = resources.getString(R.string.best_value)
        var fullYearlyText = "$yearlyText | $bestValueText"
        val measurePaint = Paint(binding.optionB.paint)
        val requiredWidth = measurePaint.measureText(fullYearlyText)
        val availableWidth = binding.optionB.width
        if(requiredWidth >= availableWidth){
            fullYearlyText = "$yearlyText \n $bestValueText"
            val yearlySpan = SpannableString(fullYearlyText)
            context?.apply {
                yearlySpan.setStyleSpan(
                    fullYearlyText,
                    yearlyText,
                    R.style.period_toggle_text_bold,
                    this
                )
                yearlySpan.setStyleSpan(fullYearlyText, " \n ", R.style.period_toggle_seporater, this)
                yearlySpan.setStyleSpan(
                    fullYearlyText,
                    bestValueText,
                    R.style.period_toggle_text_italics,
                    this
                )
                binding.optionB.text = yearlySpan
            }
        }
        binding.periodToggle.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}