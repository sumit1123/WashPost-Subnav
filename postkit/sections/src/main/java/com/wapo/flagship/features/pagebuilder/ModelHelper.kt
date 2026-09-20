@file:JvmName("ModelHelper")

package com.wapo.flagship.features.pagebuilder

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import androidx.annotation.DimenRes
import com.wapo.android.commons.util.UiUtils
import com.wapo.flagship.features.grid.ComponentSize
import com.wapo.flagship.features.grid.model.*
import com.wapo.flagship.features.sections.utils.UIUtils
import com.washingtonpost.android.sections.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

internal fun getHeadlineSize(context: Context, headline: Headline, isGrid: Boolean): Int {

    if (isGrid) {
        val baseSize = when(headline.size) {
            Size.TINY -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_tiny)
            Size.XSMALL -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_xsmall)
            Size.SMALL -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_small)
            Size.MEDIUM -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_medium)
            Size.STANDARD -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_standard)
            Size.LARGE -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_large)
            Size.XLARGE -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_xlarge)
            Size.HUGE -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_huge)
            Size.MASSIVE -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_massive)
            Size.COLOSSAL -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_colossal)
            Size.JUMBO -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_jumbo)
            Size.GARGANTUAN -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_gargantuan)
            Size.COLOSSAL_ALL_CAPS -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_colossal_all_caps)
            Size.JUMBO_ALL_CAPS -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_jumbo_all_caps)
            Size.GARGANTUAN_ALL_CAPS -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_headline_gargantuan_all_caps)
        }
        return if (headline.style == Style.CONVERSATIONS) Math.round(baseSize * 0.9f) else baseSize
    }

    val baseSize = when (headline.fontStyle) {
        FontStyle.HIGHLIGHT_STYLE -> context.resources.getDimensionPixelSize(R.dimen.page_headline_highlight_size_base)
        FontStyle.NORMAL_STYLE -> context.resources.getDimensionPixelSize(R.dimen.page_headline_normal_size_base)
        FontStyle.THIN_STYLE -> context.resources.getDimensionPixelSize(R.dimen.page_headline_thin_size_base)
        else -> context.resources.getDimensionPixelSize(R.dimen.page_headline_normal_size_base) // fallback
    }
    return Math.round(baseSize * getFontSizeMultiplier(context, headline.size, R.array.text_size_multiplier))
}

internal fun getBlurbSize(context: Context, blurbInfo: BlurbInfo?, isGrid: Boolean): Int {

    if (isGrid) {
        return context.resources.getDimensionPixelSize(R.dimen.grid_homepagestory_blurb_size)
    }

    val baseSize = when (blurbInfo?.fontStyle) {
        BlurbFontStyle.NORMAL_STYLE -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_blurb_size)
        BlurbFontStyle.LIKE_ARTICLE_BODY -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_blurb_size_like_article_body)
        else -> context.resources.getDimensionPixelSize(R.dimen.homepagestory_blurb_size) // fallback
    }
    return Math.round(baseSize * getFontSizeMultiplier(context, blurbInfo?.size, R.array.blurb_size_multiplier))
}

internal fun getRelatedLinkSize(context: Context, relatedLinksInfo: RelatedLinksInfo?, isGrid: Boolean): Int {

    if (isGrid) {
        return context.resources.getDimensionPixelSize(R.dimen.grid_homepagestory_related_links_size)
    }

    val baseSize = context.resources.getDimensionPixelSize(R.dimen.homepagestory_related_links_size)
    return Math.round(baseSize * getFontSizeMultiplier(context, relatedLinksInfo?.size, R.array.related_links_size_multiplier))
}

internal fun getFontSizeMultiplier(context: Context, size: Size?, resId: Int): Float {
    val multipliers = context.resources.getIntArray(resId)
    val idx = Math.max(0, Math.min(size?.ordinal
            ?: PagebuilderSize.NORMAL.ordinal, multipliers.size - 1))
    return if (multipliers.isEmpty()) 1f else multipliers[idx] / 10000f
}

internal fun isEmpty(signature: Signature?): Boolean {
    var date: Date? = null

    if (signature?.timestamp != null) {
        for (dateFormat in dateFormats) {
            try {
                val parsed = dateFormat.parse(signature.timestamp)
                if (parsed != null) {
                    date = parsed
                    break
                }
            } catch (t: Throwable) {
                // ignore and continue
            }
        }
    }

    val isFutureTime = date?.after(Calendar.getInstance().time) ?: false

    return signature == null || TextUtils.isEmpty(signature.byLine) &&
            TextUtils.isEmpty(signature.section) &&
            (TextUtils.isEmpty(signature.timestamp) || isFutureTime)
}

@get:DimenRes
internal val Alignment?.gravity: Int
    get() {
        return when (this) {
            Alignment.CENTER -> Gravity.CENTER
            Alignment.LEFT -> Gravity.LEFT
            Alignment.RIGHT -> Gravity.RIGHT
            Alignment.INHERIT -> Gravity.NO_GRAVITY
            else -> Gravity.LEFT
        }
    }

internal fun HomepageStory.isMediaThumbnail(artWidth: ArtWidth?) : Boolean {

    return (artWidth != ArtWidth.FULL_WIDTH && resolvedColumnSpan < 5) || artWidth == ArtWidth.TINY || artWidth == ArtWidth.MINI
}

// Number of grid columns media should take up within a cell based on the following specification
// https://docs.google.com/spreadsheets/d/1siK6kQyZM6QCw6mWKCE-qj5nZjaen3DQ5plkihGJ2ZA/
private val MEDIA_SIZES_XSMALL = mapOf(20 to 5, 19 to 5, 18 to 5, 17 to 5, 16 to 3, 15 to 3, 14 to 3, 13 to 3, 12 to 3, 11 to 3, 10 to 2, 9 to 2, 8 to 2, 7 to 2, 6 to 2, 5 to 2)
private val MEDIA_SIZES_SMALL = mapOf(20 to 6, 19 to 6, 18 to 6, 17 to 6, 16 to 4, 15 to 4, 14 to 4, 13 to 4, 12 to 4, 11 to 4, 10 to 3, 9 to 3, 8 to 3, 7 to 3, 6 to 2, 5 to 2)
private val MEDIA_SIZES_MEDIUM = mapOf(20 to 7, 19 to 7, 18 to 7, 17 to 7, 16 to 5, 15 to 5, 14 to 5, 13 to 5, 12 to 5, 11 to 5, 10 to 4, 9 to 4, 8 to 3, 7 to 3, 6 to 3, 5 to 2)
private val MEDIA_SIZES_LARGE = mapOf(20 to 10, 19 to 10, 18 to 10, 17 to 10, 16 to 8, 15 to 8, 14 to 8, 13 to 8, 12 to 6, 11 to 6, 10 to 5, 9 to 5, 8 to 4, 7 to 3, 6 to 3, 5 to 2)
private val MEDIA_SIZES_XLARGE = mapOf(20 to 12, 19 to 12, 18 to 12, 17 to 12, 16 to 10, 15 to 10, 14 to 10, 13 to 10, 12 to 8, 11 to 8, 10 to 5, 9 to 5, 8 to 4, 7 to 3, 6 to 3, 5 to 2)
private val MEDIA_SIZES_XXLARGE= mapOf(20 to 16, 19 to 16, 18 to 16, 17 to 16, 16 to 12, 15 to 11, 14 to 11, 13 to 11, 12 to 8, 11 to 8, 10 to 6, 9 to 6, 8 to 4, 7 to 3, 6 to 3, 5 to 2)

/*
    Return image width units per story column span according to https://docs.google.com/spreadsheets/d/1siK6kQyZM6QCw6mWKCE-qj5nZjaen3DQ5plkihGJ2ZA/edit#gid=302453858
 */
internal fun HomepageStory.getMediaWidth(context: Context, artWidth: ArtWidth, horSpacing: Int = 0) : Int {

    if (artWidth == ArtWidth.FULL_WIDTH) {
        val aspectRatio = media?.aspectRatio?.takeIf { it > 0 } ?: 1f
        return when {
            aspectRatio < 1 -> {
                //constraint tall videos to max 6 units wide
                getColumnsWidth(min(resolvedColumnSpan, 6), horSpacing, context)
            }
            aspectRatio == 1f -> {
                //constraint square videos to max 8 units wide
                getColumnsWidth(min(resolvedColumnSpan, 8), horSpacing, context)
            }
            else -> getColumnsWidth(resolvedColumnSpan, horSpacing, context)
        }
    }

    val imageUnits: Int = when (artWidth) {
        ArtWidth.XSMALL ->
            MEDIA_SIZES_XSMALL.getValue(resolvedColumnSpan)
        ArtWidth.SMALL ->
            MEDIA_SIZES_SMALL.getValue(resolvedColumnSpan)
        ArtWidth.MEDIUM ->
            MEDIA_SIZES_MEDIUM.getValue(resolvedColumnSpan)
        ArtWidth.LARGE ->
            MEDIA_SIZES_LARGE.getValue(resolvedColumnSpan)
        ArtWidth.XLARGE ->
            MEDIA_SIZES_XLARGE.getValue(resolvedColumnSpan)
        ArtWidth.XXLARGE ->
            MEDIA_SIZES_XXLARGE.getValue(resolvedColumnSpan)
        else -> resolvedColumnSpan
    }

    return getColumnsWidth(imageUnits, horSpacing, context)

}

internal fun getColumnsWidth(units: Int, horSpacing: Int = 0, context: Context) : Int {
    return if (units == 1) {
        val screenWidth = UIUtils.displayMetrics(context).widthPixels
        val singleColumnMargin = context.resources.getDimensionPixelSize(R.dimen.grid_single_column_margin)
        val cardShadowMargin = context.resources.getDimensionPixelSize(R.dimen.card_horizontal_margin)
        screenWidth - singleColumnMargin * 2 - cardShadowMargin * 2 - horSpacing
    } else {
        val columnWidth = context.resources.getDimensionPixelSize(R.dimen.grid_column_width)
        val gutterWidth = context.resources.getDimensionPixelSize(R.dimen.grid_gutter_width)
        units * (columnWidth + gutterWidth) - columnWidth - horSpacing
    }
}

private val dateFormats = mutableListOf<SimpleDateFormat>().apply {
    add(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US))
    add(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US))
    for (dateFormat in this) {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }
}

fun List<ComponentSize>.getSize(context: Context): ComponentSize? {
    val screenSize = UiUtils.screenSizeInDp(context)
    val screenWidth = screenSize.x
    val screenHeight = screenSize.y
    var nearestSize: ComponentSize? = null
    var minDiff = Int.MAX_VALUE
    forEach { size ->
        val diff = Math.abs(screenWidth - (size.width ?: 0)) + Math.abs(screenHeight - (size.height ?:0))
        if (diff < minDiff) {
            nearestSize = size
            minDiff = diff
        }
    }
    return nearestSize
}

fun String?.parseSectionFrontDate() : Date? {
    this ?: return null
    for (dateFormat in dateFormats) {
        try {
            val parsed = dateFormat.parse(this)
            if (parsed != null) {
                return parsed
            }
        } catch (t: Throwable) {
            // ignore and continue
        }
    }
    return null
}