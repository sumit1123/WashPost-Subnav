/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.data.repository

import android.os.Build
import android.text.Html
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.flagship.features.articles2.models.deserialized.Deck
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.models.deserialized.Title
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.tts.domain.ProvideExternalTtsRepo
import com.wapo.flagship.features.tts.model.ExternalTts
import com.wapo.flagship.network.retrofit.network.APIResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProvideExternalTtsRepoImpl @Inject constructor(
    @CoroutineScopeCommonsModule.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val articles2Service: Articles2Service
) : ProvideExternalTtsRepo {

    override suspend fun getExternalTts(source: String): ExternalTts {
        return withContext(ioDispatcher) {
            val articleContentResponse = articles2Service.getArticleContent(
                url = source,
                timeoutMs = 10000
            )

            when (articleContentResponse) {
                is APIResult.Failure -> {
                    throw Exception(articleContentResponse.getMessage())
                }
                is APIResult.NetworkError -> {
                    throw Exception(articleContentResponse.getMessage())
                }
                is APIResult.Success -> {
                    if (articleContentResponse.data != null) {
                        val articleContent = articleContentResponse.data
                        val items = mutableListOf<String>()

                        articleContent.items?.forEach { item ->
                            val itemContent = when (item) {
                                is Title -> item.content
                                is Deck -> item.content
                                is SanitizedHtml -> item.content?.let { htmlToPlainText(it) }
                                else -> null
                            }
                            itemContent?.let {
                                items.add(it)
                            }
                        }
                        ExternalTts(items, articleContentResponse.data.title)
                    } else {
                        throw Exception("Article content response is empty")
                    }
                }
            }
        }
    }

    private fun htmlToPlainText(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }
    }
}
