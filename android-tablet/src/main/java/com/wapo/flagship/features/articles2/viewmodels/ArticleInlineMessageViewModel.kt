package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ArticleInlineMessageViewModel @Inject constructor() : ViewModel() {
    private val _articleInlineMessage: MutableLiveData<ArticleInlineMessage?> = MutableLiveData()
    val articleInlineMessage: LiveData<ArticleInlineMessage?> = _articleInlineMessage

    private val _messageUpdateEvent: MutableLiveData<Any> = MutableLiveData()
    val messageUpdateEvent: LiveData<Any> = _messageUpdateEvent


    private val _messageImpressionEvent: MutableLiveData<BannerLifecycleEvent> = MutableLiveData()
    val messageImpressionEvent: LiveData<BannerLifecycleEvent> = _messageImpressionEvent

    fun setArticleInlineMessage(message: ArticleInlineMessage?) {
        _articleInlineMessage.value = message
    }

    fun dispatchMessageUpdateEvent() {
        _messageUpdateEvent.value = Any()
    }
}
