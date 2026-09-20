package com.wapo.flagship.features.articles2.viewholders

import android.text.SpannableString
import android.text.Spanned
import android.view.View
import com.bumptech.glide.Glide
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.instagram.Instagram
import com.wapo.text.GlobalFontAdjustmentSpan
import com.washingtonpost.android.databinding.ItemInstagramBinding

class InstagramViewHolder(
    private val binding: ItemInstagramBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Instagram>(
        binding.root,
    ) {
    override fun bind(
        item: Instagram,
        position: Int,
    ) {
        var playOverLay = binding.videoOverlay
        val captionView = binding.articleMediaCaption
        val userName = binding.username
        val photoSlot = binding.photoSlot

        playOverLay.visibility = View.GONE

        val thumbnailUrl = item?.imageURL
        if (thumbnailUrl != null) {
            Glide.with(FlagshipApplication.getInstance().applicationContext).load(thumbnailUrl).centerCrop().into(
                binding.articleMediaImage,
            )
        } else {
            photoSlot.visibility = View.GONE
        }
        val authorName = item?.instagramContent?.authorName
        if (authorName != null) {
            val spannableUserName = SpannableString.valueOf(authorName)
            spannableUserName.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                spannableUserName.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            userName.text = spannableUserName
            userName.visibility = View.VISIBLE
        } else {
            userName.visibility = View.GONE
        }
        val caption = item?.instagramContent?.title
        if (caption != null) {
            captionView.text = caption
            captionView.visibility = View.VISIBLE
        } else {
            captionView.visibility = View.GONE
        }
    }
}
