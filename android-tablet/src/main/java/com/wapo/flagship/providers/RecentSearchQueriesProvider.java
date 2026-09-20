package com.wapo.flagship.providers;

import com.washingtonpost.android.R;

public class RecentSearchQueriesProvider extends SearchRecentSuggestionsProviderImproved {
    public static final String Authority = RecentSearchQueriesProvider.class.getPackage().getName() + ".searchSuggestions.unified";
    public static final int Mode = DATABASE_MODE_QUERIES;

    public RecentSearchQueriesProvider() {
        setupSuggestions(Authority, Mode, R.drawable.ic_search_history);
    }
}
