package com.washingtonpost.android.paywall.bottomsheet.ui.component.product

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.size
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component
import com.washingtonpost.android.paywall.bottomsheet.ui.component.SeparatorView
import com.washingtonpost.android.paywall.bottomsheet.ui.component.old.ProductCheckTextView
import com.washingtonpost.android.paywall.databinding.ProductTextBinding
import com.washingtonpost.android.paywall.helper.componentTextToString

/**
 * This is the view for the Product Feature List component.
 * Holds list of features for this product.
 */
class ProductTextView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val binding = ProductTextBinding.inflate(LayoutInflater.from(context), this, true)

    fun setTextList(textList: List<String>) {
        if (binding.featureList.childCount > 0) {
            binding.featureList.removeAllViews()
        }
        textList.forEach {
            context.apply {
                val checkTextView = ProductCheckTextView(this)
                checkTextView.isInclusive = true
                checkTextView.setText(it)
                binding.featureList.addView(checkTextView)
            }
        }
    }

    /**
     * Inserts a text divider at the specified index and styles it for this use case.
     */
    fun insertSeparator(separator: Component) {
        context.apply {
            val separatorView = SeparatorView(this)
            separatorView.setText(separator.text.componentTextToString())
            separatorView.styleForOffer()
            separator.index?.let { separatorIndex ->
                if (separatorIndex >= 0 && separatorIndex < binding.featureList.size) {
                    binding.featureList.addView(separatorView, separatorIndex)
                }
            }
        }
    }
}