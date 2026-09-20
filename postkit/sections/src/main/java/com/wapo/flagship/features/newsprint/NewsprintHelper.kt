package com.wapo.flagship.features.newsprint

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.wapo.android.commons.extensions.toUri
import com.wapo.flagship.features.grid.model.Item

object NewsprintHelper {

    const val NEWSPRINT_URL = "https://www.washingtonpost.com/newsprint?wv=nonav"
    const val NEWSPRINT_TOP_CARD_ITID = "sf_newsprint"

    /**
     * Assigns a specific pair of colors to each Newsprint card segment.
     * These will be used later to render the background gradient.
     * Does not apply to non-cardified versions of Newsprint.
     */
    fun handleNewsprintCard(item: Item, isFirst: Boolean) {
        if (item.isNewsprint) {
            item.cardIndex?.let { cardIndex ->
                item.gradientColors = getGradientColors(cardIndex, isFirst)
            }
        }
    }

    /**
     * Match either of the two 2024 Newsprint SiteService sections: "Fancy" or "Plain."
     */
    fun isNewsprintSection(sectionDisplayName: String): Boolean {
        return sectionDisplayName.lowercase().contains("newsprint")
    }

    private fun getGradientColors(cardIndex: Int, isFirst: Boolean): Pair<Int, Int> {
        val offset = if (isFirst) 0 else 1
        val startColorIndex = ((cardIndex * 2) + offset) % gradientColors.size
        val endColorIndex = (startColorIndex + 1) % gradientColors.size

        return Pair(
            gradientColors[startColorIndex],
            gradientColors[endColorIndex]
        )
    }

    /**
     * Applies the color gradient as a layer on the existing background to preserve the original shape (i.e. rounded corners).
     */
    fun applyNewsprintGradient(view: View, item: Item) {
        findMaterialCardView(view)?.let {
            it.background.alpha = 0 // remove color from preexisting background while preserving its shape
            val layerDrawable = LayerDrawable(arrayOf(it.background, getGradientDrawable(item)))
            it.background = layerDrawable
        }
    }

    private fun getGradientDrawable(item: Item): GradientDrawable {
        val gradientDrawable = GradientDrawable()
        item.gradientColors?.let {
            gradientDrawable.colors = intArrayOf(
                it.first,
                it.second
            )
        }
        return gradientDrawable
    }

    /**
     * Assigns a specific color to each Newsprint divider based on which card it's on.
     */
    fun getDividerPaint(paint: Paint, item: Item): Paint {
        item.cardIndex?.let { cardIndex ->
            paint.apply {
                this.color = getDividerColor(cardIndex)
            }
        }
        return paint
    }

    private fun getDividerColor(cardIndex: Int): Int {
        val index = cardIndex % dividerColors.size
        return dividerColors[index]
    }

    /**
     * Have to search both up and down in the tree to find the MaterialCardView for different ViewHolders.
     */
    private fun findMaterialCardView(view: View): MaterialCardView? {
        (view.parent as? MaterialCardView)?.let {
            return it
        }
        view.findViewWithTag<MaterialCardView>("material_card_view")?.let {
            return it
        }
        return null
    }

    /**
     * Uses 4 WPDS colors with 50% alpha and 4 colors which are 40% of the way between each WPDS color.
     * This way we can apply gradients to card segments without a shared background across the card.
     */
    private val gradientColors = listOf(
        Color.parseColor("#80166DFC"),
        Color.parseColor("#80615AE4"), // 40% of the way to next
        Color.parseColor("#80D138BF"),
        Color.parseColor("#80DF5050"), // 40% of the way to next
        Color.parseColor("#80F3750E"),
        Color.parseColor("#80F3A15A"), // 40% of the way to next
        Color.parseColor("#80F3E4CD"),
        Color.parseColor("#809B8C9B") // 40% of the way to first
    )

    private val dividerColors = listOf(
        Color.parseColor("#166DFC"),
        Color.parseColor("#D138BF"),
        Color.parseColor("#F3750E"),
        Color.parseColor("#F3E4CD")
    )

    fun getNewsprintUrlWithItId(itId: String): String {
        val uriBuilder = NEWSPRINT_URL.toUri().buildUpon()
        return uriBuilder.appendQueryParameter("itid", itId).build().toString()
    }
}