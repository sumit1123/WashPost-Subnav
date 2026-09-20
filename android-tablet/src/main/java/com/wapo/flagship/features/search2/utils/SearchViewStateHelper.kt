package com.wapo.flagship.features.search2.utils

import android.content.Context
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.search2.model.*
import com.wapo.flagship.features.search2.state.PostAnswersUIState
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.AdSectionConfig
import com.washingtonpost.android.databinding.FragmentSearch2Binding

class SearchViewStateHelper(
    val binding: FragmentSearch2Binding?,
    val context: Context,
) {
    /**
     * Show loading indicator as the page is being loaded
     * used by both [SearchLandingFragment] and [SearchResultsFragment]
     */
    fun showLoading() {
        binding?.let {
            binding.rvSearch.setVisible(false)
            binding.loading.setVisible(true)
        }
    }

    /**
     * No Search results found
     * used by [SearchResultsFragment]
     */
    fun showNoMatches() {
        binding?.let {
            binding.rvSearch.setVisible(false)
            binding.loading.setVisible(false)
        }
    }

    /**
     * Show section and article search results
     * used by [SearchResultsFragment]
     */
    fun showSearchResult(
        sections: List<SectionItem>,
        articles: List<SearchItem>,
        expandable: ExpandableItem? = null,
        showLoader: Boolean = false,
        shouldShowPostAnswers: Boolean,
        postAnswersUIState: PostAnswersUIState,
        adSectionConfig: AdSectionConfig? = ConfigManager.getInstance().config.adsConfig.search
    ): List<SearchItem> {
        binding?.rvSearch?.setVisible(true)
        binding?.loading?.setVisible(false)

        val list = mutableListOf<SearchItem>()

        if (shouldShowPostAnswers) {
            list.add(PostAnswerContainerItem(postAnswersUIState))
        }

        if (sections.isNotEmpty()) {
            list.add(SpacerItem())
            list.add(HeaderItem(SECTION_HEADER))
            list.addAll(sections)
            if (sections.size > 2) {
                expandable
                    ?.label
                    ?.let { ExpandableItem(it, expandable.expanded) }
                    ?.let { list.add(it) }
            }
        }
        list.add(SpacerItem())
        if (articles.isNotEmpty()) {
            if (isAdsInSearchEnabled(adSectionConfig, articles)) {
                addAdsToSearchItemList(list, articles, adSectionConfig)
            } else {
                list.addAll(articles)
                list.add(SpacerItem())
            }
        }

        if (showLoader) {
            list.add(LoaderItem())
            list.add(SpacerItem())
        }

        return list
    }

    private fun isAdsInSearchEnabled(
        adSectionConfig: AdSectionConfig?,
        articles: List<SearchItem>
    ) =
        adSectionConfig?.enabled != null
                && (adSectionConfig.enabled ?: false)
                && articles.size > (adSectionConfig.minimumResults ?: 0)
                && (adSectionConfig.adItemInterval ?: 0) > 0
                && !FlagshipApplication.getInstance().shouldSuppressAds()

    /**
     * @param list - This function modifies the Search Item list by injecting Ads between article search results.
     * @param articles - list of articles that is used to determine the Ad placement based on its size and constructing the SearchItem List
     * @param adSectionConfig - The config object that contains the config values
     * This function adds an Ad Item taking into account 3 variables - articles size, firstItemDelay, and adItemInterval.
     * Add an AdItem in the list after [firstItemDelay] articles. The subsequent AdItems are after [adItemInterval] articles.
     */
    private fun addAdsToSearchItemList(
        list: MutableList<SearchItem>,
        articles: List<SearchItem>,
        adSectionConfig: AdSectionConfig?
    ) {
        val firstItemDelay = adSectionConfig?.firstItemDelay ?: 0
        val adIntervals = adSectionConfig?.adItemInterval ?: 0

        if (adIntervals == 0) {
            return
        }

        var adPositionCounter = 1
        var currentIndex = 0
        val remainingSize = articles.size - firstItemDelay
        val endIndex = if (remainingSize % adIntervals == 0) {
            articles.size
        } else {
            (remainingSize - (remainingSize % adIntervals)) + firstItemDelay
        }

        while (currentIndex < endIndex) {
            if (currentIndex == 0 && firstItemDelay > 0) {
                for (i in 0 until firstItemDelay) {
                    list.add(articles[i])
                }
                currentIndex = firstItemDelay
            } else {
                for (i in currentIndex until currentIndex+adIntervals) {
                    list.add(articles[i])
                }
                currentIndex += adIntervals
            }

            val adPositionPath = if (UIUtil.isPhone(context)) {
                "incontent-mob_" + adPositionCounter++
            } else {
                "incontent_" + adPositionCounter++
            }

            list.add(AdItem(commercialNode = adSectionConfig?.commercialNode ?: "search", adPosition = adPositionPath))
        }

        while (currentIndex < articles.size) {
            list.add(articles[currentIndex])
            currentIndex++
        }

        list.add(SpacerItem())
    }

    fun showElectionStateList(states: List<ElectionItem>) {
        binding?.rvSearch?.setVisible(true)
        binding?.loading?.setVisible(false)
        val list = mutableListOf<SearchItem>()
        list.add(SpacerItem())
        list.addAll(states)
        (binding?.rvSearch?.adapter as? Search2Adapter)?.apply {
            submitList(list)
        }
    }

    /**
     * Show the list of recent searches or default "No Search History"
     * used by [SearchLandingFragment]
     */
    fun showRecentSearches(
        recents: List<SearchQueryItem>,
        top: List<SearchQueryItem>,
    ) {
        binding?.rvSearch?.setVisible(true)
        binding?.loading?.setVisible(false)

        val list = mutableListOf<SearchItem>()
        list.add(AskQuestionsItem("Ask"))
        list.add(SpacerItem())
        if (recents.isNotEmpty()) {
            list.add(HeaderItem(RECENTS_HEADER, groupType = SearchQueryItem::class))
            list.addAll(recents)
        } else {
            list.add(HeaderItem(RECENTS_HEADER, false))
            list.add(NoResult("No Result History"))
        }
        list.add(SpacerItem())
        if (top.isNotEmpty()) {
            list.add(HeaderItem(TOP_HEADER))
            list.addAll(top)
        }

        (binding?.rvSearch?.adapter as? Search2Adapter)?.apply {
            submitList(list)
        }
    }

    companion object {
        private const val SECTION_HEADER = "SECTIONS"
        private const val RECENTS_HEADER = "RECENTS"
        private const val TOP_HEADER = "TOP SEARCHES"
    }
}
