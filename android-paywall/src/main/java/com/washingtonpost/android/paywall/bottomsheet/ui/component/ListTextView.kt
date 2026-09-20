package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component
import com.washingtonpost.android.paywall.databinding.ListTextBinding
import com.washingtonpost.android.paywall.helper.componentTextToString

/**
 * This is the view for the CheckBoxList component.
 */
class ListTextView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val binding = ListTextBinding.inflate(LayoutInflater.from(context), this, true)

    fun createList(items: List<Any>, checkBoxList: Component) {
        if (binding.featureList.childCount > 0) {
            binding.featureList.removeAllViews()
        }

        val textSpacer = checkBoxList.textSpacer?.space ?: 4
        val textSeparatorSpacer = checkBoxList.textSeparatorSpacer?.space ?: 8

        items.forEachIndexed { index, _ ->
            val currentItem = items[index]
            val nextItem = if (index < items.size - 1) { items[index + 1] } else { null }

            if (currentItem is String && nextItem is Component) {
                // List item that precedes a separator
                insertText(currentItem)
                insertSpacing(textSeparatorSpacer)
            } else if (currentItem is String) {
                // List item that precedes either another list item or nothing
                insertText(currentItem)
                insertSpacing(textSpacer)
            } else if (currentItem is Component) {
                // Separator
                insertSeparator(currentItem)
                insertSpacing(textSeparatorSpacer)
            }
        }
    }

    private fun insertText(text: String) {
        val checkTextView = ListCheckTextView(context)
        checkTextView.isInclusive = true
        checkTextView.setText(text)
        binding.featureList.addView(checkTextView)
    }

    /**
     * Inserts a text divider at the specified index and styles it for this use case.
     */
    private fun insertSeparator(separator: Component) {
        context.apply {
            val separatorView = SeparatorView(this)
            separatorView.setText(separator.text.componentTextToString())
            separatorView.styleForList()
            binding.featureList.addView(separatorView)
        }
    }

    private fun insertSpacing(size: Int) {
        val spacer = View(context)
        spacer.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size)
        binding.featureList.addView(spacer)
    }
}