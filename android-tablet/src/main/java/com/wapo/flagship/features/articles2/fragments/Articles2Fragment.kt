package com.wapo.flagship.features.articles2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.wapo.android.commons.engagement.EngagementTracker
import com.wapo.flagship.common.fixPagingGesture
import com.wapo.flagship.features.articles2.adapters.ArticlesPagerAdapter
import com.wapo.flagship.features.articles2.navigation_models.ArticlePage
import com.wapo.flagship.features.articles2.navigation_models.ArticlesActivity2Destinations
import com.wapo.flagship.features.articles2.tracking.ArticlePageEngagementTrace
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.viewmodels.ArticleTimeStamp
import com.wapo.flagship.features.articles2.viewmodels.Articles2DestinationViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.wapo.flagship.features.articles2.viewmodels.PageViewTimeTrackerViewModel
import com.wapo.flagship.util.tracking.Measurement.PATH_TO_VIEW_BACK_TO_FRONT
import com.wapo.flagship.util.tracking.Measurement.PATH_TO_VIEW_SWIPE
import com.washingtonpost.android.databinding.FragmentArticlesNewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

/**
 * A fragment that displays articles in a view pager.
 */
@AndroidEntryPoint
class Articles2Fragment : Fragment() {
    private var _binding: FragmentArticlesNewBinding? = null
    private val binding get() = _binding!!

    private var articlePageEngagementTrace: ArticlePageEngagementTrace? = null

    private val destinationViewModel: Articles2DestinationViewModel by activityViewModels()
    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel by activityViewModels()
    private val pageViewTimeTrackerViewModel: PageViewTimeTrackerViewModel by activityViewModels()
    private lateinit var articlesPagerAdapter: ArticlesPagerAdapter

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            articlePageEngagementTrace?.addNavigationBehavior(PATH_TO_VIEW_BACK_TO_FRONT)
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentArticlesNewBinding.inflate(inflater, container, false)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        startArticlesEngagementTracker()

        // call clearFocus() on the root View when Articles2Activity is resumed. This is needed when
        // user signs in from the Articles page and a bottom sheet is shown, dismissing a bottom sheet
        // leaves the article view in a state showing a gray transparent overlay.
        binding.root.clearFocus()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        destinationViewModel.destinations.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {
                    is ArticlesActivity2Destinations.StartUp -> {
                        articlesPagerAdapter =
                            ArticlesPagerAdapter(
                                this,
                                it.articlesMetaData.toMutableList(),
                                it.pushTopic,
                                it.shouldPlayAudioArticle,
                                it.audioArticleToPlayPosition
                            )
                        articlesPagerAdapter.setViewPager(binding.articlesPager)
                        binding.articlesPager.adapter = articlesPagerAdapter
                        registerPageChangeListener(it)
                        binding.articlesPager.offscreenPageLimit = 1
                        adjustViewPager(false)
                        if (savedInstanceState == null) {
                            binding.articlesPager.setCurrentItem(it.positionOfSelected, false)
                        } else {
                            binding.articlesPager.setCurrentItem(
                                articlesPagerCollaborationViewModel.currentPage.value?.position ?: it.positionOfSelected,
                                false,
                            )
                        }
                    }
                    ArticlesActivity2Destinations.Finish -> {
                    /*
                        Stubbed - This action will be taken care from the activity side.
                     */
                    }
                }
            },
        )

        articlesPagerCollaborationViewModel.pageExpandEvent.observe(viewLifecycleOwner) {
            adjustViewPager(it)
        }

        binding.articlesPager.fixPagingGesture()

        observePageRemoved()

        articlesPagerCollaborationViewModel.pagerScrollToggleEvent.observe(viewLifecycleOwner) { shouldAllowScroll ->
            binding.articlesPager.isUserInputEnabled = shouldAllowScroll
        }
    }

    private fun startArticlesEngagementTracker() {
        articlePageEngagementTrace = ArticlePageEngagementTrace()
        articlePageEngagementTrace?.startTrace()
    }

    private fun stopArticlesEngagementTracker(url: String?, navigationBehavior: String = "") {
        articlePageEngagementTrace?.let {
            val analytics = articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[url]
            if (navigationBehavior.isNotEmpty()) it.addNavigationBehavior(navigationBehavior)
            it.constructAnalyticsEvents(analytics)
            it.stopTrace()
            EngagementTracker.getInstance().trackEngagement(it)
        }
    }

    override fun onStop() {
        super.onStop()
        val url = articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id?.let {
            metaId -> getUrlWithoutParameters(metaId)
        }
        stopArticlesEngagementTracker(url)
    }

    /**
     * Ads ability to show preview and expand the card when scrolled up
     * [expand] adjusts the view pager to show preview if false. Removes all margins and adjustments to show full-screeen zoomed view.
     */
    private fun adjustViewPager(expand: Boolean) {
        binding.articlesPager.clipToPadding = false
        val nextItemVisiblePx =
            if (expand) {
                requireContext().resources.getDimension(com.washingtonpost.android.articles.R.dimen.articles_no_margin)
            } else {
                requireContext().resources.getDimension(com.washingtonpost.android.articles.R.dimen.viewpager_next_article_visible)
            }
        val currentItemHorizontalMarginPx =
            if (expand) {
                requireContext().resources.getDimension(com.washingtonpost.android.articles.R.dimen.articles_no_margin)
            } else {
                requireContext().resources.getDimension(
                    com.washingtonpost.android.articles.R.dimen.viewpager_current_item_horizontal_margin,
                )
            }
        val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
        val pageTransformer =
            ViewPager2.PageTransformer { page: View, position: Float ->
                page.translationX = -pageTranslationX * position
                // Next line scales the item's height. You can remove it if you don't want this effect
//            page.scaleY = 1 - (0.25f * kotlin.math.abs(position))
                // If you want a fading effect uncomment the next line:
                // page.alpha = 0.25f + (1 - abs(position))
            }
        binding.articlesPager.setPageTransformer(pageTransformer)
    }

    /**
     * This function registers the page change listener for [viewPager]. As pages are changed we need to make some changes at the top level
     * i.e. in Activity or a container fragment. E.g. Changing toolbar icons.
     */
    private fun registerPageChangeListener(it: ArticlesActivity2Destinations.StartUp) {
        binding.articlesPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // As currentArticlesMetaData is a dynamic list, we need to make sure that the list is not empty before accessing it.
                    if (articlesPagerAdapter.currentArticlesMetaData.isEmpty()) return
                    val selectedArticle = articlesPagerAdapter.currentArticlesMetaData[position]
                    val currentUrl = getUrlWithoutParameters(selectedArticle.id)
                    val prevUrl =
                        articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id?.let { metaId ->
                            getUrlWithoutParameters(metaId)
                        }
                    // We need to make sure that the current page url (page swiped from) is not same as the one that this page is requesting for.
                    // This is because sometimes we can get callback more than once to this function when there's accidental swiping action from user.
                    // Also initialize articleTimeStamp when it is not yet initialized. This case is needed when the system recreates the activity.
                    if (currentUrl != prevUrl || !pageViewTimeTrackerViewModel.initialized) {
                        pageViewTimeTrackerViewModel.articleTimeStamp =
                            ArticleTimeStamp(
                                currentUrl,
                                Date().time,
                            )
                        pageViewTimeTrackerViewModel.initialized = true

                        if (prevUrl != null) {
                            stopArticlesEngagementTracker(prevUrl, PATH_TO_VIEW_SWIPE)
                        }

                        // start timer for the article user navigated to
                        startArticlesEngagementTracker()
                    }
                    articlesPagerCollaborationViewModel.selectPage(
                        ArticlePage(selectedArticle, position),
                    )
                }
            },
        )
    }

    /**
     * This function observes the pageRemoved event from the [ArticlesPagerCollaborationViewModel]. When an article is removed from the list,
     * it updates the articles list and notifies the adapter of the change.
     *
     */
    private fun observePageRemoved() {
        articlesPagerCollaborationViewModel.pageRemoved.observe(
            viewLifecycleOwner
        ) {
           articlesPagerAdapter.removeArticleAt(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /**
         * To avoid mem. leaks
         */
        _binding?.articlesPager?.adapter = null
        _binding = null
    }
}
