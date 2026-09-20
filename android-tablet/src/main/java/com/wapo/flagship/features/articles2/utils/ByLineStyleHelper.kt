package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.articles.R

object ByLineStyleHelper {
    fun getByLineStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_byline_style,
                R.style.ArticleText_Byline,
            ).also { typedArray.recycle() }
    }

    fun getElevatedByLineStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_elevated_byline_style,
                R.style.ArticleText_Elevated_Byline,
            ).also { typedArray.recycle() }
    }

    fun getByLineLiveUpdateStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_byline_live_update_style,
                R.style.ArticleText_Byline_LiveUpdate,
            ).also { typedArray.recycle() }
    }

    fun getByLineSubtextLiveUpdateStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_byline_subtext_live_update_style,
                R.style.ArticleText_Byline_LiveUpdate_Subtext,
            ).also { typedArray.recycle() }
    }
}
