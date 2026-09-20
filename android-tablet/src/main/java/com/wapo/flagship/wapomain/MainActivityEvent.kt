/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.wapomain

import android.os.Bundle
import com.wapo.flagship.features.articles2.activities.ArticlesParcel

sealed class MainActivityEvent {

    data class OnGetArticles(
        val articlesUrls: List<String>,
        val bundle: Bundle?,
        val widgetType: String?,
        val articleContentUrl: String?,
        val sectionDisplayName: String,
    ) : MainActivityEvent()
}
