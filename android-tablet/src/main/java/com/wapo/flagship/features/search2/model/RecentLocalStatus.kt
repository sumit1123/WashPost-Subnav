package com.wapo.flagship.features.search2.model

sealed class RecentLocalStatus {
    object AddSuccess : RecentLocalStatus()

    object RemoveSuccess : RecentLocalStatus()

    object RemoveAllSuccess : RecentLocalStatus()

    class FetchNextSuccess(
        val recents: List<SearchQuery>,
    ) : RecentLocalStatus()

    class Failure(
        val message: String,
    ) : RecentLocalStatus()
}
