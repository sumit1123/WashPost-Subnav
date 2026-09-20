package com.wapo.flagship.features.audio.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.audio.models.UserClickEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Shared [ViewModel] for maintaining information about the currently selected article.
 */
@HiltViewModel
class AudioFeatureArticleStateViewModel @Inject constructor() : ViewModel() {

    private val _currentArticleUrl = LiveEvent<String>()
    val currentArticleUrl: LiveData<String> = _currentArticleUrl
    private val _userClickEvent = LiveEvent<UserClickEvent>()
    val userClickEvent: LiveData<UserClickEvent> = _userClickEvent

    /**
     * Updates the currently selected article URL.
     */
    fun selectCurrentArticleUrl(url: String) {
        _currentArticleUrl.value = url
    }

    fun getCurrentArticleUrl(): String? = _currentArticleUrl.value

    fun articleTitleClick(articleData: ArticleData) {
        _userClickEvent.value = UserClickEvent.ClickArticle(articleData)
    }
}

/**
 * Class to send article data as part of the event when user clicks on a Title
 * in an Expanded player.
 */
data class ArticleData(
    val contentUrl:String?,
    val section:String?,
)