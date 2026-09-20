package com.wapo.flagship.providers

import android.content.SearchRecentSuggestionsProvider
import com.washingtonpost.android.BuildConfig

class SearchSuggestionProvider : SearchRecentSuggestionsProvider() {
    init {
        val authority = "${BuildConfig.APPLICATION_ID}.providers.searchSuggestions.unified"
        setupSuggestions(authority, DATABASE_MODE_QUERIES)
    }
    companion object {
        const val AUTHORITY = "com.wapo.flagship.providers.searchSuggestions.unified"
        const val MODE: Int = DATABASE_MODE_QUERIES
        const val URI = "content://$AUTHORITY/"
    }
}
