package com.washingtonpost.foryou

import com.washingtonpost.foryou.data.RecommendationsItem

interface ForYouActivity {

    fun onCommentClicked(recommendationsItem: RecommendationsItem)

    fun onItemClicked(
        position: Int,
        recommendationsItem: RecommendationsItem,
        articleMetas: List<RecommendationsItem>,
        defaultForYouLaunched: Boolean? = false
    )

    fun playForYouAudio(
        recommendationsItem: RecommendationsItem,
        isActionAudio: Boolean,
        isAudioCarousel: Boolean,
        feed: String?,
        isFlexAudio: Boolean,
        isActionButton: Boolean,
        onLoadingChange: (Boolean) -> Unit
    )

    fun onLoadMore(fetchingRecs: Boolean) {

    }

    fun onHabitTileClicked(destinationUrl: String)

    fun onSummaryIconClicked(recommendationsItem: RecommendationsItem)

    fun toggleNavBars(scrollY: Float, hasSectionTitle: Boolean)
}