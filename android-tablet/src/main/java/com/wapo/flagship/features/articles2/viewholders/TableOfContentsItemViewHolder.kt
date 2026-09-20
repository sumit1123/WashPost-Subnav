package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.articles2.models.LiveEntry
import com.wapo.text.GlobalFontAdjustmentSpan
import com.washingtonpost.android.databinding.ItemSelectedTableOfContentsBinding
import com.washingtonpost.android.databinding.ItemTableOfContentsBinding

abstract class ItemViewHolder(
    open val itemView: View,
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(liveEntry: LiveEntry?)

    protected fun setupDateView(
        datetime: TextView,
        displayDate: Long?,
    ) {
        if (displayDate == null || displayDate == 0L) {
            datetime.visibility = View.GONE
        } else {
            datetime.visibility = View.VISIBLE
            datetime.text =
                GlobalFontAdjustmentSpan.applyGlobalFontSpan(
                    DateViewHolder.formatTimestamp(
                        displayDate,
                    ),
                )
        }
    }
}

class TableOfContentsItemViewHolder(
    val binding: ItemTableOfContentsBinding,
    val onClick: (String?) -> Unit,
) : ItemViewHolder(binding.root) {
    override fun bind(liveEntry: LiveEntry?) {
        liveEntry?.let {
            binding.apply {
                setupDateView(datetime, it.displayDate)
                headline.text = GlobalFontAdjustmentSpan.applyGlobalFontSpan(it.title)
                root.setOnClickListener { _ -> onClick(it.anchor) }
            }
        }
    }
}

class TableOfContentsItemSelectedViewHolder(
    val binding: ItemSelectedTableOfContentsBinding,
    val onClick: (String?) -> Unit,
) : ItemViewHolder(binding.root) {
    override fun bind(liveEntry: LiveEntry?) {
        liveEntry?.let {
            binding.apply {
                setupDateView(datetime, it.displayDate)
                headline.text = GlobalFontAdjustmentSpan.applyGlobalFontSpan(it.title)
                root.setOnClickListener { _ -> onClick(it.anchor) }
            }
        }
    }
}
