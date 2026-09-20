/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.states

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Article415

sealed class ArticleContentState {

    object Loading : ArticleContentState()

    object Failure : ArticleContentState()

    class Success(val article: Article2, val source: Source) : ArticleContentState()

    class Unsupported(val article: Article2? = null, val article415: Article415? = null) :
        ArticleContentState()

    object UiTimedOut: ArticleContentState()

    enum class Source { NETWORK, CACHE }

}