package com.wapo.flagship.features.articles2.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.common.expandShortUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShortenedUrlResolverViewModel
    @Inject
    constructor() : ViewModel() {
        /**
         * States for identifying Gift Tokens and processing them.
         */
        fun deeplinkResolvedUrl(
            shortUrl: String,
            context: Context,
            onUrlResolved: (String) -> Unit,
        ) {
            viewModelScope.launch {
                val fullUrl =
                    withContext(Dispatchers.IO) {
                        expandShortUrl(shortUrl, context)
                    }
                fullUrl?.let {
                    onUrlResolved(it)
                }
            }
        }
    }
