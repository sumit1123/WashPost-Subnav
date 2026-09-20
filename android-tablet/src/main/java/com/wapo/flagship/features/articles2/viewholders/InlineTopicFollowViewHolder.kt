package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.text.SpannableString
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.wapo.android.commons.util.setStyleSpan
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.FollowState
import com.wapo.flagship.features.articles2.models.InlineTopicFollowItem
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.InlineTopicFollowBinding

class InlineTopicFollowViewHolder(
    private val binding: InlineTopicFollowBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<InlineTopicFollowItem>(
        binding.root,
        null,
    ) {
    val context: Context = binding.root.context

    override fun onBindItem(
        item: InlineTopicFollowItem,
        position: Int,
    ) {
        setText(item.followState, item.topicDisplayName ?: "")
        setFollowState(item.followState)
        binding.followButton.setOnClickListener {
            articlesInteractionHelper.onEventFired(
                ArticleInteractionEvent.TopicFollowButtonClicked(item),
            )
        }
    }

    override fun setPlaceHolderData(item: InlineTopicFollowItem): PlaceHolderData? = null

    override fun onLowDataModeEnable(item: InlineTopicFollowItem) {
        binding.icon.isVisible = false
    }

    override fun onLowDataModeDisable(item: InlineTopicFollowItem) {
        binding.icon.isVisible = true
        setIcon(item)
    }

    private fun setIcon(item: InlineTopicFollowItem) {
        var iconVisible = false
        item.iconUrl?.let {
            val glideUrl = GlideUrl(it)
            Glide.with(binding.root.context).load(glideUrl).into(binding.icon)
            iconVisible = true
        }
        binding.icon.setVisible(iconVisible)
    }

    private fun setText(
        state: FollowState,
        text: String,
    ) {
        val boldText =
            when (state) {
                FollowState.NOT_FOLLOWED -> "Follow"
                else -> "You're following"
            }
        val fullText = "$boldText $text"
        val spanString = SpannableString(fullText)
        spanString.setStyleSpan(
            fullText,
            boldText,
            R.style.inline_follow_bold_text,
            binding.root.context,
        )
        binding.mainText.text = spanString
    }

    private fun setFollowState(state: FollowState) {
        val iconId =
            when (state) {
                FollowState.NOT_FOLLOWED -> com.wpds.wpds.R.drawable.add
                FollowState.FOLLOWED -> com.wapo.flagship.features.aixp.R.drawable.check
            }
        binding.followButton.text = if (state == FollowState.NOT_FOLLOWED) "Follow" else "Following"
        binding.followButton.setIconResource(iconId)
    }
}
