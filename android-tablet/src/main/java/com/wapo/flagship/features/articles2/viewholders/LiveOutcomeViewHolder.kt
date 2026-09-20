package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.bumptech.glide.Glide
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.LiveOutcome
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemLiveOutcomeBinding

class LiveOutcomeViewHolder(
    private val binding: ItemLiveOutcomeBinding,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<LiveOutcome>(binding.root) {
    override fun bind(
        item: LiveOutcome,
        position: Int,
    ) {
        binding.headline.text = item.headline?.content
        binding.subHeadline.text = item.subHeadline?.content
        adjustHeadlinePosition()
        if (!item.image?.imageURL.isNullOrEmpty()) {
            binding.image.visibility = View.VISIBLE
            Glide
                .with(binding.root.context)
                .load(item.image?.imageURL)
                .into(binding.image)
        } else {
            binding.image.visibility = View.GONE
        }
        binding.subHeadline.visibility = if (binding.subHeadline.text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun adjustHeadlinePosition() {
        binding.headline.post {
            val constraintSet =
                ConstraintSet().apply {
                    clone(binding.constraintLayout)

                    if (binding.subHeadline.text.isNullOrBlank()) {
                        connect(
                            R.id.headline,
                            ConstraintSet.TOP,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.TOP,
                        )
                        connect(
                            R.id.headline,
                            ConstraintSet.BOTTOM,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.BOTTOM,
                        )
                    } else {
                        connect(
                            R.id.headline,
                            ConstraintSet.TOP,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.TOP,
                        )
                        clear(R.id.headline, ConstraintSet.BOTTOM)
                    }
                }
            constraintSet.applyTo(binding.constraintLayout)
        }
    }
}
