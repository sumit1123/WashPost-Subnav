package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PageViewTimeTrackerViewModel
    @Inject
    constructor() : ViewModel() {
        var initialized: Boolean = false
        var articleTimeStamp: ArticleTimeStamp = ArticleTimeStamp()
    }

data class ArticleTimeStamp(
    var articleUrl: String? = null,
    var timeStamp: Long? = null,
)
