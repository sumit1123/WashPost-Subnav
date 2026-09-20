package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.navigation_models.ArticlesActivity2Destinations
import com.wapo.flagship.model.ArticleMeta
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * This is a destination view model that is responsible for dispatching events that trigger navigation across different screens within the Articles activity.
 */
@HiltViewModel
class Articles2DestinationViewModel
    @Inject
    constructor() : ViewModel() {
        private val _destination: LiveEvent<ArticlesActivity2Destinations> =
            LiveEvent()
        val destinations: LiveEvent<ArticlesActivity2Destinations> = _destination

        /**
         * Initializes the articles workflow by assigning a relevant [articleUrl] to fetch an article (either from remote or local source) based on it.
         */
        fun startUp(
            articlesList: List<ArticleMeta>,
            indexOfSelected: Int,
            pushTopic: String? = "",
            shouldPlayAudioArticle: Boolean = false,
            shouldPlayArticleForPosition: Int? = null
        ) = _destination.postValue(
            ArticlesActivity2Destinations.StartUp(articlesList, indexOfSelected, pushTopic, shouldPlayAudioArticle, shouldPlayArticleForPosition),
        )
    }
