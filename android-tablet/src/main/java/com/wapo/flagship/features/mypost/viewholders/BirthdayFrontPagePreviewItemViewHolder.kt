package com.wapo.flagship.features.mypost.viewholders

import com.washingtonpost.android.save.databinding.MyPostBirthdayFrontPageBinding
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.viewholders.ItemViewHolder

class BirthdayFrontPagePreviewItemViewHolder(
    val binding: MyPostBirthdayFrontPageBinding,
    private val onBirthdayFrontPageClick: () -> Unit,
) : ItemViewHolder(binding.root) {
    override fun bind(previewItem: PreviewItem) {
        binding.buttonViewMore.setOnClickListener {
            onBirthdayFrontPageClick()
        }
    }
}
