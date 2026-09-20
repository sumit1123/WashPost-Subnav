package com.washingtonpost.android.follow.ui

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.wapo.android.commons.util.ViewUtil.requireComponentActivity
import com.wapo.text.WpTextFormatter
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.database.model.FollowEntity
import com.washingtonpost.android.follow.databinding.AuthorArticleViewBinding
import com.washingtonpost.android.follow.helper.AuthorProvider
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper
import com.washingtonpost.android.paywall.PaywallService

class AuthorArticleView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val followViewModel by ViewModelHelper.getViewModel(context.requireComponentActivity(), FollowViewModel::class)
    private var observer: Observer<FollowEntity?>? = null
    private var authorItem: AuthorItem? = null
    private val binding = AuthorArticleViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(author: AuthorItem, isOpinion: Boolean) {
        if (author.name != null && author.bio != null) {
            binding.image.apply {
                followViewModel.followManager.followProvider.getAuthorImageRequestUrl(author.image)?.apply {
                    setImageUrl(this, followViewModel.followManager.followProvider.animatedImageLoader)
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
                    followViewModel.followManager.followProvider.onAuthorNameClicked(author)
                    followViewModel.followManager.followProvider.startAuthorPageActivity(context, author)
                }
            }
            @ColorInt
            val authorColor = when {
                isOpinion -> ContextCompat.getColor(context, R.color.author_article_name_opinion)
                author.id == null -> ContextCompat.getColor(context, R.color.author_article_name)
                else -> ContextCompat.getColor(context, R.color.author_article_name_clickable)
            }
            binding.authorName.setTextColor(authorColor)
            val description = author.bio
            WpTextFormatter.applyLineSpacing(binding.authorBio, R.style.author_article_bio)
            binding.authorBio.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(description)
            }
            binding.authorBio.movementMethod = LinkMovementMethod.getInstance()
            removeObserver()
            if (author.id != null && PaywallService.getInstance().loggedInUser != null) {
                binding.authorFollowButton.visibility = View.VISIBLE
                observer = Observer<FollowEntity?> { followEntity ->
                    val isFollowing = followEntity != null && followEntity?.isFollowing as Boolean
                    binding.authorFollowButton.isEnabled = true
                    binding.authorFollowButton.text = if (isFollowing) {
                        resources.getString(R.string.author_button_following)
                    } else {
                        resources.getString(R.string.author_button_follow)
                    }
                    VectorDrawableCompat.create(resources, R.drawable.ic_following_btn_check, context?.theme)?.apply {
                        binding.authorFollowButton.setCompoundDrawablesWithIntrinsicBounds(if (isFollowing) this else null, null, null, null)
                    }
                    binding.authorFollowButton.isSelected = isFollowing
                    binding.authorFollowButton.setOnClickListener {
                        binding.authorFollowButton.isEnabled = false
                        followViewModel.setFollowing(!isFollowing, author) { reachedMaxFollow ->
                            if (reachedMaxFollow) {
                                followViewModel.followManager.followProvider.onMaxFollowReached(context, author)
                                binding.authorFollowButton.isEnabled = true
                            } else {
                                if (!isFollowing) {
                                    followViewModel.followManager.followProvider.onAuthorFollowed((context as AuthorProvider).getRootView(), author.id)
                                }
                                FollowTrackingInfo.followTracking.miscellany = TRACKING_BIO_PAGE
                                val trackingEvent = if (isFollowing) TrackingEvent.ON_UNFOLLOWED else TrackingEvent.ON_FOLLOWED
                                followViewModel.followManager.followProvider.onTrackingEvent(trackingEvent)
                            }
                        }
                    }
                }.apply {
                    followViewModel.isFollowing(author).observe(context.requireComponentActivity(), this)
                }
            } else {
                binding.authorFollowButton.visibility = View.GONE
            }
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    fun unbind() {
        removeObserver()
    }

    private fun removeObserver() {
        authorItem?.let { authorItem ->
            observer?.let { observer ->
                followViewModel.isFollowing(authorItem).removeObserver(observer)
            }
        }
    }

    companion object {
        const val TRACKING_BIO_PAGE = "authorarticle"
    }
}