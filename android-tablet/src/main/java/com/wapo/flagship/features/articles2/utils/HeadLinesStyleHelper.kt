package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.articles.R

object HeadLinesStyleHelper {
    fun getHeadLineStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_headline_style,
                R.style.ArticleText_Headline,
            ).also { typedArray.recycle() }
    }

    fun getTextHeadLineStyleText(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_headline_style_text,
                R.style.ArticleText_Headline_StyleText,
            ).also { typedArray.recycle() }
    }

    fun getTextHeadLineStyle1(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_headline_h1_style,
                R.style.ArticleText_Headline_H1,
            ).also { typedArray.recycle() }
    }

    fun getTextHeadLineStyle2(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_headline_h2_style,
                R.style.ArticleText_Headline_H2,
            ).also { typedArray.recycle() }
    }

    fun getTextHeadLinePrefixStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_headline_prefix_style,
                R.style.ArticleText_Headline_Prefix,
            ).also { typedArray.recycle() }
    }

    fun getArticleLiveHeadLineStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_headline_live,
                R.style.ArticleText_LiveHeadline,
            ).also { typedArray.recycle() }
    }
}
