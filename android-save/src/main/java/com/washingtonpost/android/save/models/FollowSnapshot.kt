package com.washingtonpost.android.save.models

import com.washingtonpost.android.follow.database.model.AuthorEntity

/**
 * Represents a combination of following authors and their articles
 */
class FollowSnapshot(val authors: List<AuthorEntity>?, val articles: List<MyPostArticleItem>?)