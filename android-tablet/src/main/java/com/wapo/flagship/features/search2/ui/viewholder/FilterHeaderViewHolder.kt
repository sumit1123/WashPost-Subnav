package com.wapo.flagship.features.search2.ui.viewholder

import android.animation.Animator
import com.wapo.android.commons.util.animateRotate
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.model.FilterHeaderItem
import com.wapo.flagship.features.search2.ui.adapter.FilterAdapter
import com.washingtonpost.android.databinding.FilterHeaderBinding

class FilterHeaderViewHolder(
    val binding: FilterHeaderBinding,
    val onCollapseChanged: (FilterEvent) -> Unit,
) : FilterAdapter.FilterViewHolder<FilterHeaderItem>(binding.root) {
    private var isCollapsed: Boolean = false
    private var collapseAnimator: Animator? = null

    override fun bind(item: FilterHeaderItem) {
        super.bind(item)
        binding.label.text = item.label
        binding.expandableImage.setVisible(item.isCollapsed != null)
        item.isCollapsed?.let {
            isCollapsed = it
            binding.root.setOnClickListener {
                isCollapsed = !isCollapsed
                item.isCollapsed = isCollapsed
                onCollapseChanged(FilterEvent.ExpandCollapse(item))
                val toVal = if (isCollapsed) 0f else 180f
                val fromVal = if (isCollapsed) 180f else 0f
                collapseAnimator =
                    binding.expandableImage.animateRotate(
                        fromVal,
                        toVal,
                        setPivot = false,
                    )
                collapseAnimator?.start()
            }
        }
    }
}
