package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.StyleRes
import com.washingtonpost.android.articles.R
import com.washingtonpost.android.recirculation.carousel.models.StyleEntity

object KickerStyleHelper {
    fun getKickerPillLiveTextStyle(
        context: Context,
        @StyleRes articleItemsStyle: Int =
            StylesHelper.getArticleItemStyle(
                context,
            ),
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                articleItemsStyle,
                R.styleable.ArticleItems,
            )

        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_kicker_pill_live_style,
                R.style.ArticleText_Kicker_Pill_Exclusive,
            ).also { typedArray.recycle() }
    }

    fun getKickerPillExclusiveTextStyle(
        context: Context,
        @StyleRes articleItemsStyle: Int =
            StylesHelper.getArticleItemStyle(
                context,
            ),
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                articleItemsStyle,
                R.styleable.ArticleItems,
            )

        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_kicker_pill_exclusive_style,
                R.style.ArticleText_Kicker_Pill_Live,
            ).also { typedArray.recycle() }
    }

    fun getTextKickerDefaultStyle(
        context: Context,
        @StyleRes articleItemsStyle: Int =
            StylesHelper.getArticleItemStyle(
                context,
            ),
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                articleItemsStyle,
                R.styleable.ArticleItems,
            )

        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_kicker_style,
                R.style.ArticleText_Kicker,
            ).also { typedArray.recycle() }
    }

    fun getTextKickerElevatedStyle(
        context: Context,
        @StyleRes articleItemsStyle: Int =
            StylesHelper.getArticleItemStyle(
                context,
            ),
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                articleItemsStyle,
                R.styleable.ArticleItems,
            )

        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_kicker_elevated_style,
                R.style.ArticleText_Elevated_Kicker,
            ).also { typedArray.recycle() }
    }

    fun getTextKickerBriefsStyle(
        context: Context,
        @StyleRes articleItemsStyle: Int =
            StylesHelper.getArticleItemStyle(
                context,
            ),
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                articleItemsStyle,
                R.styleable.ArticleItems,
            )

        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_kicker_style_briefs,
                R.style.ArticleText_Kicker_Briefs,
            ).also { typedArray.recycle() }
    }

    fun getDisplayTransparencyStyle(
        context: Context,
        @StyleRes articleItemsStyle: Int =
            StylesHelper.getArticleItemStyle(
                context,
            ),
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                articleItemsStyle,
                R.styleable.ArticleItems,
            )

        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_kicker_display_transparency_style,
                R.style.ArticleText_Kicker_DisplayTransparencyStyle,
            ).also { typedArray.recycle() }
    }

    fun getStyle(style: String): StyleEntity? =
        when (style) {
            "opinions" -> StyleEntity.OPINIONS
            else -> null
        }
}
