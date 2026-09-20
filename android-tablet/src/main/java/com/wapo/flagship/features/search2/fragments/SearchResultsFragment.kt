package com.wapo.flagship.features.search2.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.aixp.ui.FeedbackFragment
import com.wapo.flagship.features.aixp.ui.FeedbackStatusFragment
import com.wapo.flagship.features.aixp.viewmodels.FeedbackCollaborationViewModel
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.model.ExpandableItem
import com.wapo.flagship.features.search2.model.PostAnswerContainerItem
import com.wapo.flagship.features.search2.model.SearchItem
import com.wapo.flagship.features.search2.model.SectionItem
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.wapo.flagship.features.search2.viewmodel.FilterViewModel
import com.wapo.flagship.features.search2.viewmodel.SearchMode
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment to display search results or error UI
 */
@AndroidEntryPoint
class SearchResultsFragment : SearchBaseFragment() {
    private val filterViewModel: FilterViewModel by activityViewModels()

    private val feedbackCollaborationViewModel: FeedbackCollaborationViewModel by activityViewModels()

    private val askThePostViewModel: AskThePostViewModel by activityViewModels()

    /**
     * cached sections list for expand collapse view
     */
    private var sectionFullListCached = listOf<SectionItem>()

    /**
     * cached articles list for expand collapse view of sections
     */
    private var articleFullListCached = listOf<SearchItem>()

    private var searchResultAdapter: Search2Adapter? = null

    @Inject lateinit var adService: AdService

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initSearchRV(handleAskThePostEvents = {
            handleAskThePostEvent(it)
        })
        observerUiState()
        observePostAnswersUIState()
        loadMoreSections()
        observePostAnswersFeedback()
        observeFeedbackSubmittedEvent()
        observeAdsMode()
    }

    /**
     * Initialize RV for search results with appropriate decorators, and layout manager
     */
    private fun initSearchRV(handleAskThePostEvents: (AskThePostEvent) -> Unit) {
        binding?.rvSearch?.apply {
            // Clear existing decorations and listeners to prevent stacking on re-init
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            clearOnScrollListeners()

            layoutManager = LinearLayoutManager(context)
            searchResultAdapter =
                Search2Adapter(
                    shouldSuppressAds = adService.currentAdsMode == AdsModel.Disabled,
                    askThePostViewModel = askThePostViewModel,
                    fragmentManager = childFragmentManager,
                    onItemClick = { searchViewModel.itemClicked(it) },
                    askThePostUIEvent = {
                        handleAskThePostEvents(it)
                    }
                )
            adapter = searchResultAdapter
            this.recycledViewPool.setMaxRecycledViews(Search2Adapter.SearchItemType.AD.id, 0)
            addItemDecoration(decorator)
            initPagination(this, this.layoutManager as? LinearLayoutManager)
        }

        /**
         * Removes keyboard when user scrolls on search
         */
        binding?.rvSearch?.setOnTouchListener { _, _ ->
            val inputMethodManager =
                context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
            false
        }
    }

    private fun observeAdsMode() {
        viewLifecycleOwner.lifecycleScope.launch {
            adService.adsMode.collect {
                // Re-initialize the RV to update ad suppression status
                initSearchRV(handleAskThePostEvents = {
                    handleAskThePostEvent(it)
                })
            }
        }
    }

    private fun handleAskThePostEvent(event: AskThePostEvent) {
        when (event) {
            is AskThePostEvent.ShowSourceSheetWith -> {
                askThePostViewModel.showSourceSheetWith(event.carouselItems)
            }

            else ->
                Logger.d("SearchResultsFragment", "No handling state - $event")

        }
    }

    /**
     * Initialize pagination based on scroll position of rv
     */
    private fun initPagination(
        rv: RecyclerView,
        layoutManager: LinearLayoutManager?,
    ) {
        rv.addOnScrollListener(
            object : OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    super.onScrolled(recyclerView, dx, dy)

                    val visibleItemCount = layoutManager?.childCount ?: 0
                    val totalItemCount = layoutManager?.itemCount ?: 0
                    val firstVisiblePosition = layoutManager?.findFirstVisibleItemPosition() ?: -1

                    if (searchViewModel.searchUiState.value != SearchUiState.NextPageLoading) {
                        if (visibleItemCount + firstVisiblePosition >= totalItemCount) {
                            searchViewModel.loadMoreSearches()
                        }
                    }
                }
            },
        )
    }

    /**
     * Observe various UI States and updated UI accordingly
     */
    private fun observerUiState() {
        if (searchViewModel.searchUiState.value == SearchUiState.Failure) {
            searchViewStateHelper.showNoMatches()
        }
        searchViewModel.searchUiState.observe(viewLifecycleOwner) {
            when (it) {
                SearchUiState.Loading -> searchViewStateHelper.showLoading()
                is SearchUiState.Success -> {
                    loadSearches(it.sections, it.articles)
                    sectionFullListCached = it.sections
                    articleFullListCached = it.articles
                }

                is SearchUiState.NextPageLoaded -> {
                    loadSearches(sectionFullListCached, it.articles)
                    articleFullListCached = it.articles
                }

                is SearchUiState.NextPageLoading -> {
                    loadSearches(sectionFullListCached, articleFullListCached, true)
                }

                else -> {} // no-op
            }
        }

        filterViewModel.filterEvent.observe(viewLifecycleOwner) {
            when (it) {
                is FilterEvent.Close -> {
                    Measurement.trackResultsFilterClosed(searchViewModel.searchQuery)
                }

                else -> {}
            }
        }
    }

    private fun observePostAnswersUIState() {
        lifecycleScope.launch {
            searchViewModel.postAnswersUIState.collect { state ->
                val postAnswersContainer =
                    searchViewModel.currentItems?.firstOrNull { item ->
                        item is PostAnswerContainerItem
                    } as? PostAnswerContainerItem

                postAnswersContainer?.let { container ->
                    container.state = state
                    searchViewModel.currentItems?.indexOf(postAnswersContainer)?.let { pos ->
                        binding?.rvSearch?.adapter?.notifyItemChanged(pos)
                    }
                }
            }
        }
    }

    private fun observePostAnswersFeedback() {
        searchViewModel.initiatePostAnswersFeedbackEvent.observe(viewLifecycleOwner) { data ->
            FeedbackFragment(
                FeedbackFragment.FeedbackType.POST_ANSWERS,
                data.first,
                data.second,
                data.third,
            ).show(
                requireActivity().supportFragmentManager,
                "feedback_fragment",
            )
        }
    }

    private fun observeFeedbackSubmittedEvent() {
        feedbackCollaborationViewModel.feedbackSubmittedEvent.observe(viewLifecycleOwner) {
            FeedbackStatusFragment(FeedbackFragment.FeedbackType.POST_ANSWERS).show(
                requireActivity().supportFragmentManager,
                "feedback_status_fragment",
            )
            PrefUtils.setTalkToThePostFeedbackSubmitted(context)
        }
    }

    override fun onDestroyView() {
        binding?.rvSearch?.let {
            searchResultAdapter?.cleanup()
        }
        super.onDestroyView()
    }

    /**
     * Load search results for sections and articles
     */
    private fun loadSearches(
        sections: List<SectionItem>,
        articles: List<SearchItem>,
        showLoader: Boolean = false,
    ) {
        val loadMore: Boolean = searchViewModel.loadMore.value ?: false
        val expandableItem =
            when {
                sections.size <= 3 -> null
                loadMore -> ExpandableItem(getString(R.string.search_see_less), true)
                else -> ExpandableItem(getString(R.string.search_see_more), false)
            }

        val sectionsModList =
            if (expandableItem != null && !loadMore) sections.subList(0, 3) else sections

        val shouldShowPostAnswers =
            ConfigManager.getInstance().config.search2Config.postAnswersEnabled &&
                    searchViewModel.searchMode.value == SearchMode.Regular

        searchViewModel.currentItems =
            searchViewStateHelper.showSearchResult(
                sectionsModList,
                articles,
                expandableItem,
                showLoader,
                shouldShowPostAnswers,
                searchViewModel.postAnswersUIState.value,
            )

        (binding?.rvSearch?.adapter as? Search2Adapter)?.apply {
            submitList(searchViewModel.currentItems)
        }
    }

    /**
     * handle expand / collapse of sections list
     */
    private fun loadMoreSections() {
        searchViewModel.loadMore.observe(viewLifecycleOwner) {
            if (sectionFullListCached.isNotEmpty()) {
                loadSearches(
                    sections = sectionFullListCached,
                    articles = articleFullListCached,
                )
            }
        }
    }

    companion object {
        @JvmField
        val FRAGMENT_TAG = SearchResultsFragment::class.java.name + ".fragmentTag"
    }
}
