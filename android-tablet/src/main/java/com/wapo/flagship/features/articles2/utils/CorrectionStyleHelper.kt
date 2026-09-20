package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.articles.R

object CorrectionStyleHelper {
    fun getCorrectionTitleStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_correction_title_style,
                R.style.ArticleText_Correction_Title,
            ).also { typedArray.recycle() }
    }

    fun getCorrectionBodyStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_correction_body_style,
                R.style.ArticleText_Correction_Body,
            ).also { typedArray.recycle() }
    }
}
