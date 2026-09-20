/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.viewholders

import android.view.View
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.MyPostDetailHeaderBinding
import com.washingtonpost.android.save.models.DetailItem
import com.washingtonpost.android.save.types.MyPostSection

class SectionDetailHeaderViewHolder(val binding: MyPostDetailHeaderBinding) :
    DetailsItemViewHolder(binding.root) {

    override fun bind(detailItem: DetailItem) {
        binding.tvLabel.visibility = View.VISIBLE
        binding.tvDescription.visibility = View.VISIBLE

        val res = binding.root.resources

        when (detailItem.myPostSection) {
            MyPostSection.SAVED_STORIES -> {
                binding.tvLabel.text = res.getString(R.string.my_post_saved_stories_label)
                binding.tvDescription.text = res.getString(R.string.my_post_saved_stories_desc)
            }
            MyPostSection.READING_HISTORY -> {
                binding.tvLabel.text = res.getString(R.string.my_post_reading_history_label)
                binding.tvDescription.text = res.getString(R.string.my_post_reading_history_desc)
            }
            else -> {
                binding.tvLabel.visibility = View.GONE
                binding.tvDescription.visibility = View.GONE
            }
        }
    }
}