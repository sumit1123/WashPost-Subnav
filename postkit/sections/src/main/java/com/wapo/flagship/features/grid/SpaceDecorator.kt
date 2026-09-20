package com.wapo.flagship.features.grid

import android.graphics.Rect
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.grid.model.Ad
import com.wapo.flagship.features.grid.model.CardSegmentType
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.GlobalBanner
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.HabitTiles
import com.wapo.flagship.features.grid.model.LabelItem
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.wapo.flagship.features.grid.model.SectionTopper
import com.wapo.flagship.features.grid.model.Separator
import com.wapo.flagship.features.grid.model.SeparatorSize
import com.wapo.flagship.features.sections.utils.UIUtils

/**
 * Inserts vertical space between items, tables, chains.
 */
class SpaceDecorator(
    private val gutterHeight: Int,
    private val cardGutterHeight: Int,
    private val labelGutterHeight: Int,
    private val separatorLargeHeight: Int,
    private val separatorSmallHeight: Int,
    private val separatorExtraSmallHeight: Int,
    private val chainGutterHeight: Int
) : RecyclerView.ItemDecoration() {
    var grid: Grid? = null

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)
        val lp = view.layoutParams as SpannableGridLayoutManager.LayoutParams
        val cardified = isPageCardified(parent)
        val topMargin = 0
        val bottomMargin = if (position + 1 == parent.adapter?.itemCount) {
            // more space for final item so it's not hidden beneath Bottom Nav bar
            UIUtils.dpToPx(56f, view.resources)
        } else if (isFullOrBottomCard(lp) || isHabitTiles(lp) && view.isVisible) {
            UIUtils.dpToPx(8f, view.resources)
        } else {
            0
        }
        val topPadding = when {
            position == 0 ->  if (cardified) 0 else {
                val padding = if (itemsHaveAnIntrinsicTopPadding(lp)) 0f else 24f
                UIUtils.dpToPx(padding, view.resources)
            }
            isAdItem(lp) ->  0
            isSeparator(lp) -> getSeparatorSize(lp.item as Separator, cardified) / 2
            isLabel(lp) -> if (isCardified(lp)) 0 else labelGutterHeight / 2
            else -> 0
        }
        val bottomPadding = when {
            isTopOrMiddleCard(lp) -> 0
            isSeparator(lp) -> getSeparatorSize(lp.item as Separator, cardified) / 2
            isLabel(lp) -> if ((lp.item as LabelItem).compoundLabel.type == CompoundLabel.Type.Cta || (lp.item as LabelItem).compoundLabel.type == CompoundLabel.Type.Button || (lp.item as LabelItem).compoundLabel.type == CompoundLabel.Type.Kicker) 0 else labelGutterHeight / 2
            isAdItem(lp) -> UIUtils.dpToPx(8f, view.resources)
            isLastItemInFloatingChain(lp) -> 0
            isFullOrBottomCard(lp) -> 0
            isHabitTiles(lp) -> 0
            else -> gutterHeight
        }
        outRect.set(0, topPadding + topMargin , 0, bottomPadding + bottomMargin)
    }

    private fun isPageCardified(parent: RecyclerView) : Boolean {
        val wpGridView = parent as WPGridView
        return wpGridView.getScreenSizeLayout() == ScreenSizeLayout.XSMALL && grid?.cards?.extraSmall != null
    }
    private fun isCardified(lp: SpannableGridLayoutManager.LayoutParams) = lp.item?.cardSegmentType == CardSegmentType.TOP_CARD || lp.item?.cardSegmentType == CardSegmentType.MIDDLE_CARD || lp.item?.cardSegmentType == CardSegmentType.BOTTOM_CARD || lp.item?.cardSegmentType == CardSegmentType.FULL_CARD

    private fun isTopOrMiddleCard(lp: SpannableGridLayoutManager.LayoutParams) = lp.item?.cardSegmentType == CardSegmentType.TOP_CARD || lp.item?.cardSegmentType == CardSegmentType.MIDDLE_CARD

    private fun isFullOrBottomCard(lp: SpannableGridLayoutManager.LayoutParams) = lp.item?.cardSegmentType == CardSegmentType.BOTTOM_CARD || lp.item?.cardSegmentType == CardSegmentType.FULL_CARD

    private fun isTopOrFullCard(lp: SpannableGridLayoutManager.LayoutParams) =  lp.item?.cardSegmentType == CardSegmentType.TOP_CARD || lp.item?.cardSegmentType == CardSegmentType.FULL_CARD

    private fun isHabitTiles(lp: SpannableGridLayoutManager.LayoutParams) = lp.item is HabitTiles

    private fun isLabel(lp: SpannableGridLayoutManager.LayoutParams) = lp.item is LabelItem
    private fun isAdItem(lp: SpannableGridLayoutManager.LayoutParams) = lp.item is Ad

    private fun isSeparator(lp: SpannableGridLayoutManager.LayoutParams) = lp.item is Separator

    private fun isGlobalBanner(lp: SpannableGridLayoutManager.LayoutParams) = lp.item is GlobalBanner
    private fun isSectionTopper(lp: SpannableGridLayoutManager.LayoutParams) = lp.item is SectionTopper

    private fun getSeparatorSize(separator: Separator, isPageCardified: Boolean) : Int {
        if (isPageCardified) {
            return 0
        }
        return when (separator.size) {
            SeparatorSize.XSMALL -> separatorExtraSmallHeight
            SeparatorSize.SMALL -> separatorSmallHeight
            SeparatorSize.LARGE -> separatorLargeHeight
        }
    }

    private fun isFirstRow(lp: SpannableGridLayoutManager.LayoutParams): Boolean {
        val grid = this.grid ?: return false
        return lp.table?.resolvedRow == 0 &&
                lp.item?.resolvedRow == 0 &&
                grid.regions.firstOrNull()?.items?.indexOf(lp.chain) == 0
    }

    //checks if item is the last item in a chain that is in between other chains (i.e. not the last chain)
    private fun isLastItemInFloatingChain(lp: SpannableGridLayoutManager.LayoutParams): Boolean {
        if (this.grid == null || lp.chain == null || lp.table == null || lp.item == null) return false

        val chains = grid!!.regions.flatMap { it.items }

        if (chains.size - 1 == lp.chainIndex) return false

        for (tableItem in lp.chain!!.items) {
            if (tableItem.resolvedRow > lp.table!!.resolvedRow) {
                return false
            }
        }

        for (item in lp.table!!.items) {
            if (item.resolvedRow > lp.item!!.resolvedRow) {
                return false
            }
        }

        return true
    }

    private fun itemsHaveAnIntrinsicTopPadding(lp: SpannableGridLayoutManager.LayoutParams): Boolean {
        return isSectionTopper(lp) || isGlobalBanner(lp)
    }
}