package com.washingtonpost.android.follow.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.wapo.android.commons.util.applySystemBarPadding
import com.wapo.flagship.features.nightmode.NightModeController
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.databinding.AuthorPageActivityBinding
import com.washingtonpost.android.follow.fragment.ArticleListFragment
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.follow.viewmodel.ArticleListViewModel
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper

open class AuthorPageActivity : AppCompatActivity() {
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)
    private val articleListViewModel by ViewModelHelper.getViewModel(this, ArticleListViewModel::class)
    private lateinit var binding: AuthorPageActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        (applicationContext as? NightModeController)?.handleNightMode(this)
        binding = AuthorPageActivityBinding.inflate(layoutInflater)
        binding.root.applySystemBarPadding()
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        val author = intent?.extras?.getSerializable(PARAM_AUTHOR) as? AuthorItem
        if (author == null) {
            finish()
        } else {
            initFollow(author)
        }
    }

    private fun initFollow(author: AuthorItem) {
        title = ""
        binding.authorName.text = author.name
        if (followViewModel.isLoggedIn) {
            followViewModel.isFollowing(author).observe(this, Observer { followEntity ->
                val isFollowing = followEntity != null
                binding.authorFollowButton.isEnabled = true

                if (isFollowing) {
                    binding.authorFollowButton.text = resources.getString(R.string.author_button_following)
                    VectorDrawableCompat.create(resources, R.drawable.ic_active_follow, theme)?.apply {
                        binding.authorFollowButton.setCompoundDrawablesWithIntrinsicBounds(null, null, this, null)
                    }
                } else {
                    binding.authorFollowButton.text = resources.getString(R.string.author_button_follow)
                    VectorDrawableCompat.create(resources, R.drawable.ic_inactive_follow, theme)?.apply {
                        binding.authorFollowButton.setCompoundDrawablesWithIntrinsicBounds(null, null, this, null)
                    }
                }

                binding.authorFollowButton.isSelected = isFollowing
                binding.authorFollowButton.setOnClickListener {
                    binding.authorFollowButton.isEnabled = false
                    followViewModel.setFollowing(!isFollowing, author) { reachedMaxFollow ->
                        if (reachedMaxFollow) {
                            followViewModel.followManager.followProvider.onMaxFollowReached(this, author)
                            binding.authorFollowButton.isEnabled = true
                        } else {
                            if (!isFollowing) {
                                followViewModel.followManager.followProvider.onAuthorFollowed(binding.authorPage, author.id)
                            }
                            FollowTrackingInfo.followTracking.miscellany = TRACKING_BIO_PAGE
                            val trackingEvent = if (isFollowing) TrackingEvent.ON_UNFOLLOWED else TrackingEvent.ON_FOLLOWED
                            followViewModel.followManager.followProvider.onTrackingEvent(trackingEvent)
                        }
                    }

                }
            })
        } else {
            binding.authorFollowButton.visibility = View.GONE
        }
        articleListViewModel.showArticles(author.id)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.article_list, ArticleListFragment.newInstance(false, ArticleListFragment.Companion.Source.AUTHOR_PAGE))
                .commit()
    }

    companion object {
        const val PARAM_AUTHOR = "AuthorPageActivity.PARAM_AUTHOR"
        const val TRACKING_BIO_PAGE = "biopage"
    }
}