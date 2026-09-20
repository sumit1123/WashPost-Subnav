/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import com.wapo.android.commons.util.LogUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.color
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Callback
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.models.UserEvent
import com.wapo.flagship.utils.PicassoImageLoader
import com.wapo.flagship.utils.SanitizedHtmlTextFormatter
import com.wapo.flagship.views.ExpandableTextView
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentHeadlineBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HeadlineFragment : Fragment() {

    private var _binding: FragmentHeadlineBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val articlesPagerCollaborationViewModel:
            ArticlesPagerCollaborationViewModel by activityViewModels()
    private val articles2ViewModel: Articles2ViewModel by viewModels()

    // views
    private lateinit var scrollView: NestedScrollView
    private lateinit var articleImage: ImageView
    private lateinit var articleHeadline: ExpandableTextView
    private lateinit var articleByline: TextView
    private lateinit var articleText: TextView
    private lateinit var listen: Button

    private lateinit var article2: Article2

    private var loadArticle2Job: Job? = null

    private var articleHomepageImageLoaded = false

    private val mMainThreadHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeadlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // init views
        articleImage = binding.ivArticleImage
        articleByline = binding.tvByLine
        articleText = binding.tvText
        listen = binding.btnListenToArticle

        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            getParcelable<ArticleMeta>(ARG_OBJECT)?.let { articleMeta ->
                // load article2
                articles2ViewModel.getArticle2(articleMeta)

                // attempt to load article homepage image,
                // which is not always available
                PicassoImageLoader.loadImage(
                    requireActivity(),
                    articleMeta.imageUrl,
                    articleImage,
                    object : Callback.EmptyCallback() {
                        override fun onSuccess() {
                            articleHomepageImageLoaded = true
                        }
                    }
                )

                listen.setOnClickListener { listenToArticle() }
            }
        }

        // init views
        scrollView = binding.scrollView
        articleHeadline = binding.articleHeadline

        // hide dots and expand headline when scrolling down
        // show dots and collapse headline when the screen is back to top
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val scrollToTop = scrollY == 0
            articlesPagerCollaborationViewModel.setDotVisibility(scrollToTop)
            if (scrollToTop) {
                articleHeadline.collapse()
            } else {
                articleHeadline.expand()
            }
        }


        binding.btnNext.setOnClickListener {
            articlesPagerCollaborationViewModel.sendUserEvent(UserEvent.Next)
        }
    }

    override fun onStart() {
        super.onStart()

        subscribeToStateUpdate()
    }

    override fun onResume() {
        super.onResume()

        scrollView.smoothScrollTo(0, 0)
        articlesPagerCollaborationViewModel.setDotVisibility(true)
    }

    override fun onStop() {
        super.onStop()

        unsubscribeToStateUpdate()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    private fun subscribeToStateUpdate() {
        loadArticle2Job = viewLifecycleOwner.lifecycleScope.launch {
            articles2ViewModel.article2State
                .map { it.articleContentState }
                .collect { articleContentState ->
                    when (articleContentState) {
                        is ArticleContentState.Loading -> {
                            LogUtil.d(TAG, "Loading article...")
                        }
                        is ArticleContentState.Success -> {
                            LogUtil.d(TAG, "Article loaded!")
                            article2 = articleContentState.article
                            populateArticleData()
                        }
                        is ArticleContentState.Failure -> {
                            LogUtil.d(TAG, "Error loading article!")
                        }
                        is ArticleContentState.Unsupported -> {
                            articleContentState.article415
                            LogUtil.d(TAG, "Article unsupported!")
                        }
                        is ArticleContentState.UiTimedOut -> {
                            LogUtil.d(TAG, "Article timed out!")
                        }
                    }
                }
        }
    }

    private fun unsubscribeToStateUpdate() {
        loadArticle2Job?.cancel()
    }

    private fun populateArticleData() {
        val title = article2.items?.firstOrNull { it is Title } as? Title
        val byLine = article2.items?.firstOrNull { it is ByLine } as? ByLine
        val sanitizedHtml = article2.items
            ?.firstOrNull { it is SanitizedHtml } as? SanitizedHtml

        if (title?.content == null) {
            articleHeadline.visibility = View.GONE
        } else if (title.prefix != null) {
            articleHeadline.text = SpannableStringBuilder()
                .color(resources.getColor(R.color.gold, null)) { append(title.prefix) }
                .append(" ")
                .append(title.content)
        } else {
            articleHeadline.text = title.content
        }

        if (byLine?.content != null) {
            articleByline.text = byLine.content
        } else {
            articleByline.visibility = View.GONE
        }

        if (sanitizedHtml?.content != null) {
            articleText.text = "${SanitizedHtmlTextFormatter.format(sanitizedHtml)}.."
        } else {
            articleText.visibility = View.GONE
        }

        // if article homepage image fails to load, attempt to load article
        // social image 1s after Article2 data arrives successfully
        mMainThreadHandler.postDelayed({
            if (!articleHomepageImageLoaded) {
                PicassoImageLoader.loadImage(requireActivity(), article2.socialImage, articleImage)
            }
        }, 1000)
    }

    private fun listenToArticle() {
        if (this::article2.isInitialized) {
        }
    }

    companion object {
        private const val TAG = "HeadlineFragment"
        const val ARG_OBJECT = "ARG_OBJECT"
    }

}