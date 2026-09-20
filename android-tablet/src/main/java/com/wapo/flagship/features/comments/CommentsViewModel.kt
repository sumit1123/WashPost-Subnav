package com.wapo.flagship.features.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.truncatedString
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.comments.model.CommentsServiceRequest
import com.wapo.flagship.features.comments.model.SourceAnnotation
import com.wapo.flagship.features.comments.repo.CommentsRepository
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

const val TAG = "CommentsViewModel"

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentsRepository: CommentsRepository
) : ViewModel() {

    private val _commentCount: MutableLiveData<String> = MutableLiveData()
    val commentCount: LiveData<String> = _commentCount

    private val _sourceAnnotations: MutableLiveData<List<SourceAnnotation?>> = MutableLiveData()
    val sourceAnnotations: LiveData<List<SourceAnnotation?>> = _sourceAnnotations

    @Volatile
    private var requestedArticleUrl: String? = null

    fun fetchCommentsData(url: String, isPushOriginated: Boolean) {
        if (isPushOriginated) return
        requestedArticleUrl = url
        val body = CommentsServiceRequest(
            articleUrl = url,
            comments = true,
            sourceAnnotations = true
        )
        val endpoint = ConfigManager.getInstance().config.commentsConfig.url
        viewModelScope.launch(Dispatchers.IO) {
            endpoint.let {
                val context = FlagshipApplication.getInstance().applicationContext
                val response = commentsRepository.getCommentsData(body, endpoint)
                when (response) {
                    is APIResult.Failure -> {
                        EventLog.Builder().apply {
                            setMessage("$TAG#fetchCommentsData failed")
                            setModule(LogModules.COMMENT_COUNT)
                            setErrorMessage(response.getMessage())
                            setContentUrl(url)
                        }.run {
                            RemoteLog.e(context, build())
                        }
                    }

                    is APIResult.NetworkError -> Unit

                    is APIResult.Success -> {
                        if (requestedArticleUrl == url) {
                            response.data?.comments?.let {
                                _commentCount.postValue(it.count?.truncatedString() ?: "0")
                                it.sourceAnnotations?.let { sourceAnnotations ->
                                    _sourceAnnotations.postValue(sourceAnnotations)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
