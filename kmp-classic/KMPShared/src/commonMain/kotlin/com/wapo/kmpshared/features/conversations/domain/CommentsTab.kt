package com.wapo.kmpshared.features.conversations.domain

enum class CommentsTab(
    val displayName: String,
    val queryString: String,
) {
    FEATURED("Featured", "RECOMMENDED"),
    TOP("Top", "RANK_DESC"),
    MY_COMMENTS("My Comments", "CREATED_AT_DESC"),
    ALL("All", "CREATED_AT_DESC"),
    OLDEST("Oldest", "CREATED_AT_ASC"),
}
