/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.viewholders

import com.washingtonpost.android.save.databinding.MyPostDetailReadingListFooterBinding
import com.washingtonpost.android.save.models.DetailItem

class SectionDetailReadingListFooterViewHolder(
    val binding: MyPostDetailReadingListFooterBinding,
    private val onViewArchiveClick: () -> Unit
) :
    DetailsItemViewHolder(binding.root) {

    override fun bind(detailItem: DetailItem) {
        binding.buttonFooter.setOnClickListener { onViewArchiveClick.invoke() }
    }
}