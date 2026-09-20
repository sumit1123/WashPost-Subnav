/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.di.app.modules.features.articles2

import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependencyEntryPoint {
    fun getArticlesSaveRepo(): ArticlesSaveRepo
}
