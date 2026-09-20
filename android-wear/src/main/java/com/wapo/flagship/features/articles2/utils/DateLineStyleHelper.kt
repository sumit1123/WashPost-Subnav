/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.R

object DateLineStyleHelper {

    fun getDateLineStyle(context: Context): Int {
        val typedArray: TypedArray = context.theme.obtainStyledAttributes(
            StylesHelper.getArticleItemStyle(context),
            R.styleable.ArticleItems,
        )
        return typedArray.getResourceId(
            R.styleable.ArticleItems_article_dateline_style,
            R.style.ArticleText_Dateline
        ).also { typedArray.recycle() }
    }

}