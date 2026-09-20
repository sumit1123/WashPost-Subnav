/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.use_cases

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.models.Status
import com.wapo.flagship.querypolicies.Query
import kotlinx.coroutines.flow.Flow

class GetArticles2(private val articles2Repository: Articles2Repository) {
    operator fun invoke(query: Query<Article2>): Flow<Status<out Article2>> =
        articles2Repository.getArticle2(query)
}