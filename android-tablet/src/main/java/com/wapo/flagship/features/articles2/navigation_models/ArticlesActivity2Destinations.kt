package com.wapo.flagship.features.articles2.navigation_models

import com.wapo.flagship.model.ArticleMeta

/**
 * This is a destination view model that is used to collaborate in between the fragments and the hosting activity in terms of where to navigate when specific criteria is met
 * or the action is performed.
 */
sealed class ArticlesActivity2Destinations {
    /**
     * This destination is more or less considered as an initial destination (or rather a state)
     * (Usually destination means a specific screen/fragment in single-activity-multiple-fragments architecture but in this case destination term is used to define what state should
     * the fragment be in for lack of a better terminology. Note that [ArticleScreenState]
     * represents the state inferred by the status of the network call from
     * [FetchArticlesStatus]
     */
    class StartUp(
        val articlesMetaData: List<ArticleMeta>,
        val positionOfSelected: Int,
        val pushTopic: String? = "",
        val shouldPlayAudioArticle: Boolean = false,
        val audioArticleToPlayPosition: Int? = null
    ) : ArticlesActivity2Destinations()

    /**
     * This finishes the entire workflow (activity) [Articles2Activity]
     * In this specific implementation, users will be navigated back to section front (or any other last active activity on the back stack)
     */
    object Finish : ArticlesActivity2Destinations()
}
