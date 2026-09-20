/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.viewholders

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import androidx.annotation.StringRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.MyPostEmptyStateBinding
import com.washingtonpost.android.save.models.DetailItem
import com.washingtonpost.android.save.models.EmptyState

class EmptyDetailItemViewHolder(
    val binding: MyPostEmptyStateBinding,
    private val onSignInClick: () -> Unit,
    private val onViewArchiveClick: () -> Unit,
    private val onUpdateConsentSettingsClick: () -> Unit,
) :
    DetailsItemViewHolder(binding.root) {

    override fun bind(detailItem: DetailItem) {
        val item = detailItem as? DetailItem.Empty
        when (val state = item?.emptyState) {
            EmptyState.SAVED_STORIES_SIGN_IN,
            EmptyState.SAVED_STORIES_SIGNED_IN -> {
                handleReadingListEmptyStates(state)
            }
            EmptyState.FOLLOWING_SIGN_IN,
            EmptyState.FOLLOWING_SIGNED_IN -> {
                handleFollowingEmptyStates(state)
            }
            EmptyState.READING_HISTORY_SIGN_IN,
            EmptyState.READING_HISTORY_CONSENT_MISSING,
            EmptyState.READING_HISTORY_SIGNED_IN -> {
                handleReadingHistoryEmptyStates(state)
            }
            else -> {
                // no op
            }
        }
    }

    private fun handleReadingListEmptyStates(state: EmptyState) {
        // set section's label and description
        binding.tvLabel.text = getText(R.string.my_post_saved_stories_label)
        binding.tvDescription.text = getText(R.string.my_post_saved_stories_desc)
        // set image
        binding.ivImage.setImageResource(R.drawable.ic_saved_stories)
        when (state) {
            EmptyState.SAVED_STORIES_SIGN_IN -> {
                // set message title and description
                binding.tvMsgTitle.text = getText(R.string.my_post_es_saved_stories_sign_in_title)
                binding.tvMsgDesc.text =
                    getSavedStoriesDesc(getText(R.string.my_post_es_saved_stories_sign_in_desc))
                // set button text
                binding.buttonOpen.apply {
                    visibility = View.VISIBLE
                    text = getText(R.string.my_post_es_saved_stories_sign_in_button)
                    setOnClickListener { onSignInClick.invoke() }
                }
            }
            EmptyState.SAVED_STORIES_SIGNED_IN -> {
                // set message title and description
                binding.tvMsgTitle.text = getText(R.string.my_post_es_saved_stories_signed_in_title)
                binding.tvMsgDesc.text =
                    getSavedStoriesDesc(getText(R.string.my_post_es_saved_stories_signed_in_desc))
                // set button text
                binding.buttonOpen.apply {
                    visibility = View.VISIBLE
                    text = getText(R.string.my_post_es_saved_stories_signed_in_button)
                    setOnClickListener { onViewArchiveClick.invoke() }
                }
            }
            else -> {
                // no op
            }
        }
    }

    private fun handleFollowingEmptyStates(state: EmptyState) {
        // set section's label and description
        binding.tvLabel.text = getText(R.string.my_post_following_label)
        binding.tvDescription.text = getText(R.string.my_post_following_desc)
        // set image
        binding.ivImage.setImageResource(R.drawable.ic_following)
        when (state) {
            EmptyState.FOLLOWING_SIGN_IN -> {
                // set message title and description
                binding.tvMsgTitle.text = getText(R.string.my_post_es_following_sign_in_title)
                binding.tvMsgDesc.text = getText(R.string.my_post_es_following_sign_in_desc)
                // set button text
                binding.buttonOpen.apply {
                    visibility = View.VISIBLE
                    text = getText(R.string.my_post_es_following_sign_in_button)
                    setOnClickListener { onSignInClick.invoke() }
                }
            }
            EmptyState.FOLLOWING_SIGNED_IN -> {
                // set message title and description
                binding.tvMsgTitle.text = getText(R.string.my_post_es_following_signed_in_title)
                binding.tvMsgDesc.text = getText(R.string.my_post_es_following_signed_in_desc)
                // set button text
                binding.buttonOpen.apply {
                    binding.buttonOpen.visibility = View.GONE
                }
            }
            else -> {
                // no op
            }
        }
    }

    private fun handleReadingHistoryEmptyStates(state: EmptyState) {
        // set section's label and description
        binding.tvLabel.text = getText(R.string.my_post_reading_history_label)
        binding.tvDescription.text = getText(R.string.my_post_reading_history_desc)
        // set image
        binding.ivImage.setImageResource(R.drawable.ic_reading_history)
        when (state) {
            EmptyState.READING_HISTORY_SIGN_IN -> {
                // set message title and description
                binding.tvMsgTitle.text = getText(R.string.my_post_es_reading_history_sign_in_title)
                binding.tvMsgDesc.text = getText(R.string.my_post_es_reading_history_sign_in_desc)
                // set button text
                binding.buttonOpen.apply {
                    visibility = View.VISIBLE
                    text = getText(R.string.my_post_es_reading_history_sign_in_button)
                    setOnClickListener { onSignInClick.invoke() }
                }
            }
            EmptyState.READING_HISTORY_CONSENT_MISSING -> {
                // set message title and description
                binding.tvMsgTitle.text = getText(R.string.my_post_es_reading_history_consent_missing_title)
                binding.tvMsgDesc.text = getText(R.string.my_post_es_reading_history_consent_missing_desc)
                // set button text
                binding.buttonOpen.apply {
                    visibility = View.VISIBLE
                    text = getText(R.string.my_post_es_reading_history_consent_missing_button)
                    setOnClickListener { onUpdateConsentSettingsClick.invoke() }
                }
            }
            EmptyState.READING_HISTORY_SIGNED_IN -> {
                // set message title and description
                binding.tvMsgTitle.text =
                    getText(R.string.my_post_es_reading_history_signed_in_title)
                binding.tvMsgDesc.text = getText(R.string.my_post_es_reading_history_signed_in_desc)
                // set button text
                binding.buttonOpen.apply {
                    binding.buttonOpen.visibility = View.GONE
                }
            }
            else -> {
                // no op
            }
        }
    }

    private fun getText(@StringRes id: Int): String {
        return binding.root.resources.getString(id)
    }

    private fun getSavedStoriesDesc(desc: String): Spannable {
        return SpannableString(desc).also {
            VectorDrawableCompat.create(
                binding.root.context.resources,
                R.drawable.ic_article_save_16dp,
                binding.root.context.theme
            )?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                val imageSpan = ImageSpan(this, ImageSpan.ALIGN_CENTER)
                val imageText = "[inactive icon]"
                val start = desc.indexOf(imageText)
                if (start > -1) {
                    val end = start + imageText.length
                    it.setSpan(
                        imageSpan,
                        start,
                        end,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }
}