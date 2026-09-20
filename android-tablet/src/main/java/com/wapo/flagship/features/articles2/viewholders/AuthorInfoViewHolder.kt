package com.wapo.flagship.features.articles2.viewholders

import android.content.Intent
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.AuthorInfo
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.mypost.MyPostAuthorPageActivity
import com.wapo.text.WpTextFormatter
import com.washingtonpost.android.databinding.ItemAuthorInfoBinding
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.activity.AuthorPageActivity
import com.washingtonpost.android.follow.database.model.FollowEntity
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.follow.viewmodel.FollowViewModel

class AuthorInfoViewHolder(
    private val binding: ItemAuthorInfoBinding,
    private val followViewModel: FollowViewModel,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<AuthorInfo>(
        binding.root,
        null,
    ) {
    private var followEntityObserver: Observer<FollowEntity?>? = null

    override fun onBindItem(
        author: AuthorInfo,
        position: Int,
    ) {
        val isOpinion = false
        if (author.name != null && author.bio != null) {
            binding.image.apply {
                followViewModel.followManager.followProvider
                    .getAuthorImageRequestUrl(author.image)
                    ?.apply {
                        setImageUrl(
                            this,
                            followViewModel.followManager.followProvider.animatedImageLoader,
                        )
                    }
                setPlaceholder(R.drawable.author_placeholder)
                setErrorDrawable(R.drawable.author_placeholder)
            }
            WpTextFormatter.applyLineSpacing(binding.authorName, R.style.author_article_name)
            binding.authorName.text = author.name
            if (author.id == null) {
                binding.authorName.setOnClickListener(null)
            } else {
                binding.authorName.setOnClickListener {
                    val authorItem =
                        AuthorItem(
                            author.id,
                            author.name,
                            author.bio,
                            null,
                            author.image,
                            null,
                            0,
                        )
                    followViewModel.followManager.followProvider.onAuthorNameClicked(authorItem)
                    val intent =
                        Intent(binding.root.context, MyPostAuthorPageActivity::class.java).apply {
                            putExtra(AuthorPageActivity.PARAM_AUTHOR, authorItem)
                        }
                    binding.root.context?.startActivity(intent)
                }
            }
            @ColorInt
            val authorColor =
                when {
                    isOpinion ->
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.author_article_name_opinion,
                        )

                    author.id == null ->
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.author_article_name,
                        )

                    else ->
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.author_article_name_clickable,
                        )
                }
            binding.authorName.setTextColor(authorColor)
            val description = author.bio
            WpTextFormatter.applyLineSpacing(binding.authorBio, R.style.author_article_bio)
            binding.authorBio.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(description)
                }
            binding.authorBio.movementMethod = LinkMovementMethod.getInstance()
            if (author.showDivider == false) {
                binding.authorListDivider.visibility = View.GONE
            } else {
                binding.authorListDivider.visibility = View.VISIBLE
            }
            binding.root.visibility = View.VISIBLE
        } else {
            binding.root.visibility = View.GONE
        }
    }

    override fun setPlaceHolderData(item: AuthorInfo): PlaceHolderData? = null

    override fun onLowDataModeEnable(item: AuthorInfo) {
        binding.image.visibility = View.GONE
    }

    override fun onLowDataModeDisable(item: AuthorInfo) {
        binding.image.visibility = View.VISIBLE
    }
}
