package com.wapo.flagship.features.search2.ui.viewholder

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.RecipeItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.SearchRecipeItemBinding

class RecipeItemViewHolder(
    val binding: SearchRecipeItemBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<RecipeItem>(binding.root) {
    override fun bind(item: RecipeItem) {
        super.bind(item)

        binding.headline.text = item.headline

        setupImage(item)
        setupRating(item)
        setupSubtitle(item)

        binding.root.setOnClickListener {
            onItemClick(UserEvent.RecipeItemClick(item, bindingAdapterPosition))
        }
        binding.recipeBookmarkIcon.setOnClickListener {
            onItemClick(UserEvent.RecipeBookmarkClick(item, it as ImageView, false))
        }
        onItemClick(UserEvent.RecipeBookmarkClick(item, binding.recipeBookmarkIcon, true))
    }

    private fun setupSubtitle(item: RecipeItem) {
        val duration =
            item.duration.let {
                AudioViewUtils.getDurationText(it?.toLong(), binding.root.context)
            }
        val elements = listOfNotNull(duration, item.course).filter { it.isNotEmpty() }
        binding.subtitle.text = elements.joinToString(separator = " | ")
    }

    private fun setupRating(item: RecipeItem) {
        val rating = item.rating
        if (rating != null && rating > 0) {
            binding.rating.visibility = View.VISIBLE
            binding.ratingImage.visibility = View.VISIBLE
            binding.rating.text = rating.toString()
            setupReviews(item)
        } else {
            binding.rating.visibility = View.INVISIBLE
            binding.ratingImage.visibility = View.INVISIBLE
        }
    }

    private fun setupReviews(item: RecipeItem) {
        val reviews = item.reviews
        if (reviews != null && reviews > 0) {
            val text = binding.rating.text
            binding.rating.text = "$text ($reviews)"
        }
    }

    private fun setupImage(item: RecipeItem) {
        val requestOption = RequestOptions().centerInside()
        if (item.imageUrl.isNullOrBlank()) {
            binding.image.setImageResource(com.wapo.view.R.drawable.recipe_placeholder_item)
        } else {
            val glideUrl = GlideUrl(item.imageUrl)
            try {
                Glide
                    .with(binding.root.context)
                    .load(glideUrl)
                    .apply(requestOption)
                    .error(com.wapo.view.R.drawable.recipe_placeholder_item)
                    .into(binding.image)
            } catch (e: Exception) {
                Logger.e(TAG, "Error in Glide module. error_msg=${e.message}")
            }
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }

    companion object {
        val TAG = RecipeItemViewHolder::class.simpleName
    }
}
