package com.wapo.flagship.features.articles2.navigation_models

import android.os.Parcelable
import com.wapo.flagship.model.ArticleMeta
import kotlinx.parcelize.Parcelize

/**
 * This represents the selected Article page in the view pager 2
 */
@Parcelize
data class ArticlePage(
    val articleMeta: ArticleMeta,
    val position: Int,
) : Parcelable
