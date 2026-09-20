/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.repo

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.models.Status
import com.wapo.flagship.querypolicies.Query
import kotlinx.coroutines.flow.Flow

interface Articles2Repository {

    fun getArticle2(query: Query<Article2>): Flow<Status<out Article2>>

}