package com.wapo.flagship.features.mypost.viewholders

import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.save.databinding.MyPostPreviewFooterBinding
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.viewholders.ItemViewHolder

class FooterPreviewItemViewHolder(
    val binding: MyPostPreviewFooterBinding,
    private val onOpenSectionClick: (String) -> Unit,
) : ItemViewHolder(binding.root) {
    override fun bind(sectionPreviewItem: PreviewItem) {
        binding.buttonOpen.setOnClickListener {
            onOpenSectionClick.invoke("/games")
            Measurement.trackMyPostExploreCrosswordsClick()
            // Set the navigation behavior for the ensuing page view event
            Measurement.setNavigationBehavior(
                Measurement.getDefaultMap(),
                Measurement.PATH_TO_VIEW_MY_POST_CROSSWORD,
            )
        }
    }
}
