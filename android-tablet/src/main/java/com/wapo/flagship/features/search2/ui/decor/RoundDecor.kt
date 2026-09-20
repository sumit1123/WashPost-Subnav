package com.wapo.flagship.features.search2.ui.decor

import android.graphics.Canvas
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.decorator.Decorator
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter

class RoundDecor(
    private val cornerRadius: Float,
    private val roundPolitic: RoundPolitic = RoundPolitic.Every(RoundMode.ALL),
) : Decorator.ViewHolderDecor {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun draw(
        canvas: Canvas,
        view: View,
        recyclerView: RecyclerView,
        state: RecyclerView.State,
    ) {
        val viewHolder = recyclerView.getChildViewHolder(view)
        val nextViewHolder =
            recyclerView.findViewHolderForAdapterPosition(
                viewHolder.bindingAdapterPosition + 1,
            )
        val previousChildViewHolder =
            recyclerView.findViewHolderForAdapterPosition(
                viewHolder.bindingAdapterPosition - 1,
            )

        if (cornerRadius.compareTo(0f) != 0) {
            val roundMode = getRoundMode(previousChildViewHolder, viewHolder, nextViewHolder)
            val outlineProvider = view.outlineProvider
            if (outlineProvider is RoundOutlineProvider) {
                outlineProvider.roundMode = roundMode
                view.invalidateOutline()
            } else {
                view.outlineProvider = RoundOutlineProvider(cornerRadius, roundMode)
                view.clipToOutline = true
            }
        }
    }

    private fun getRoundMode(
        previousChildViewHolder: RecyclerView.ViewHolder?,
        currentViewHolder: RecyclerView.ViewHolder?,
        nextChildViewHolder: RecyclerView.ViewHolder?,
    ): RoundMode {
        val previousHolderItemType = previousChildViewHolder?.itemViewType ?: Companion.UNDEFINE_VIEW_HOLDER
        val currentHolderItemType = currentViewHolder?.itemViewType ?: Companion.UNDEFINE_VIEW_HOLDER
        val nextHolderItemType = nextChildViewHolder?.itemViewType ?: Companion.UNDEFINE_VIEW_HOLDER

        if (roundPolitic is RoundPolitic.Every) return roundPolitic.roundMode

        return when {
            previousHolderItemType == Search2Adapter.SearchItemType.SPACER.id &&
                nextHolderItemType == Search2Adapter.SearchItemType.SPACER.id -> RoundMode.ALL
            previousHolderItemType == Search2Adapter.SearchItemType.SPACER.id -> RoundMode.TOP
            nextHolderItemType == Search2Adapter.SearchItemType.SPACER.id -> RoundMode.BOTTOM
            else -> RoundMode.NONE
//            previousHolderItemType != currentHolderItemType && currentHolderItemType != nextHolderItemType -> RoundMode.ALL
//            previousHolderItemType != currentHolderItemType && currentHolderItemType == nextHolderItemType -> RoundMode.TOP
//            previousHolderItemType == currentHolderItemType && currentHolderItemType != nextHolderItemType -> RoundMode.BOTTOM
//            else -> RoundMode.NONE
        }
    }

    companion object {
        private const val UNDEFINE_VIEW_HOLDER: Int = -1
    }
}

sealed class RoundPolitic {
    class Every(
        val roundMode: RoundMode,
    ) : RoundPolitic()

    class Group : RoundPolitic()
}
