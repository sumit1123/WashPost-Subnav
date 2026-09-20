package com.washingtonpost.android.paywall.bottomsheet.ui.component.old

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.washingtonpost.android.paywall.databinding.PaywallTopLabelBinding

class TopLabelView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallTopLabelBinding.inflate(LayoutInflater.from(context), this, true)

    fun setLabel(mainLabel: String?, secondaryLabel: String = "") {
        binding.topLabelGroup.visibility = if (mainLabel == null) View.GONE else View.VISIBLE
        mainLabel?.apply {
            binding.mainLabel.text = this
        }

        binding.secondaryLabel.visibility = if (secondaryLabel.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.separator.visibility = binding.secondaryLabel.visibility
        secondaryLabel?.apply {
            binding.secondaryLabel.text = this
        }
    }

    fun setTitle(title: String) {
        binding.mainTitle.text = title
    }
}