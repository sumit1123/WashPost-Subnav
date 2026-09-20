package com.wapo.flagship.di.app.modules.features.readinghistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingHistoryViewModel
@Inject constructor(
    private val readingHistoryRepo: ReadingHistoryRepo
) : ViewModel() {

    fun saveArticle(articleModel: ReadingHistoryModel, metadataModel: MetadataModel) {
        articleModel.contentUrl = replaceHttp(articleModel.contentUrl)
        metadataModel.contentURL = replaceHttp(metadataModel.contentURL)

        // prevents articles that are not WaPo articles from being added to Reading History
        if (isWapoUrl(articleModel.contentUrl) && !privacyPersonalWapoCanonicalUrls.any { articleModel.contentUrl.contains(it) }) {
            viewModelScope.launch {
                readingHistoryRepo.addArticle(articleModel, metadataModel)
            }
        }
    }


    /**
     * This supporting method was added because at this time, print edition URLs are not https.
     * The collections API automatically converts URLs from http to https. This change prevents
     * UI bugs such as the save icon flashing or not staying highlighted.
     */
    private fun replaceHttp(url: String): String {
        return url.replace("http://", "https://")
    }

    private fun isWapoUrl(url: String): Boolean {
        return url.contains("www.washingtonpost.com")
    }

    private val privacyPersonalWapoCanonicalUrls: Set<String> = setOf(
        "/my-post/account/about-me",
        "/newsletters/",
        "/my-post/my-benefits",
        "/my-post/account/subscription"

    )
}