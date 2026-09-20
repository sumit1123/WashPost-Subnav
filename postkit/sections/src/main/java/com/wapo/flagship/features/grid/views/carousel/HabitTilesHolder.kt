/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.GONE
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewGroup.VISIBLE
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.HabitTiles
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.flagship.features.sections.utils.UIUtils
import com.wapo.view.habittiles.Tile
import com.wapo.view.habittiles.TilesCta
import com.wapo.view.habittiles.TilesGrid
import com.washingtonpost.android.sections.R
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme

class HabitTilesHolder(
    val view: View,
    private val cardified: Boolean,
    private val onHabitTileClicked: ((Tile?) -> Unit)? = null,
    private val userHistoryViewModel: UserHistoryViewModel?,
    private val sectionsHabitTilesRequestId: String?,
    private val sectionsHabitTilesTestGroup: String?
) : GridViewHolder(view.rootView) {

    private var previousTiles: List<Tile> = emptyList()
    private val res = itemView.context.resources
    private val card = itemView.findViewById<ViewGroup?>(R.id.card)
    private val container = itemView.findViewById<ViewGroup>(R.id.ll_container)
    private var compoundLabelView = itemView.findViewById<CompoundLabelView>(R.id.compoundLabel)
    private val composeFormWrapperView =
        itemView.findViewById<ComposeView>(R.id.compose_form_wrapper)
    private val loggedByRequest = mutableMapOf<String, MutableSet<String>>()
    private var lastRequestId: String? = null

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val habitTiles = gridAdapter.items[position] as? HabitTiles ?: return
        val phoneBreakpoint = gridAdapter.grid?.cards?.extraSmall != null && gridAdapter.isScreenXSmall
        val tiles = gridAdapter.environment.getHabitTiles()
        val areThereEnoughTilesToRender = tiles.size >= 2
        setContainer(areThereEnoughTilesToRender, phoneBreakpoint)
        setLabel(habitTiles.label, areThereEnoughTilesToRender)
        setTiles(
            tiles,
            habitTiles.cta,
            areThereEnoughTilesToRender,
            phoneBreakpoint,
            gridAdapter
        )

        val requestId = sectionsHabitTilesRequestId
        if (requestId != lastRequestId) {
            loggedByRequest.clear()
            lastRequestId = requestId
        }

        previousTiles = tiles
    }

    private fun setContainer(areThereEnoughTilesToRender: Boolean, phoneBreakpoint: Boolean) {
        card?.apply {
            val verticalMargin = if (areThereEnoughTilesToRender) UIUtils.dpToPx(8f, res) else 0
            val layoutParams = layoutParams as? MarginLayoutParams
            layoutParams?.setMargins(0, verticalMargin, 0, verticalMargin)
        }
        container.apply {
            if (areThereEnoughTilesToRender) {
                if (phoneBreakpoint) {
                    setPadding(
                        res.getDimensionPixelSize(R.dimen.grid_cell_card_homepagestory_horizontal_padding),
                        res.getDimensionPixelSize(R.dimen.grid_cell_card_homepagestory_vertical_padding),
                        res.getDimensionPixelSize(R.dimen.grid_cell_card_homepagestory_horizontal_padding),
                        res.getDimensionPixelSize(R.dimen.grid_cell_card_homepagestory_vertical_padding)
                    )
                } else {
                    setPadding(0, 0, 0, 0)
                }
                setBackgroundColor(ContextCompat.getColor(context, com.wapo.view.R.color.tile_card_background))
                visibility = VISIBLE
            } else {
                setPadding(0, 0, 0, 0)
                visibility = GONE
            }
        }
    }

    private fun setLabel(
        label: CompoundLabel?,
        areThereEnoughTilesToRender: Boolean
    ) {
        compoundLabelView?.apply {
            if (label != null && areThereEnoughTilesToRender) {
                visibility = View.VISIBLE
                setLabel(label, false)
                setCardify(cardified)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun setTiles(
        tiles: List<Tile>,
        cta: TilesCta?,
        areThereEnoughTilesToRender: Boolean,
        phoneBreakpoint: Boolean,
        gridAdapter: GridAdapter
    ) {
        val updatedList = hasUpdated(tiles)
        displayTiles(
            tiles,
            cta,
            areThereEnoughTilesToRender,
            phoneBreakpoint,
            gridAdapter,
            updatedList
        )
        previousTiles = tiles
    }

    private fun hasUpdated(tiles: List<Tile>): List<Int> {
        val list = mutableListOf<Int>()
        if (previousTiles.isEmpty()) return list
        val indices = if (previousTiles.size >= tiles.size) previousTiles.indices else tiles.indices
        for (i in indices) {
            if (i < previousTiles.size && i < tiles.size) {
                if (previousTiles[i].copy(score = tiles[i].score) != tiles[i].copy(score = tiles[i].score))
                    list.add(i)
            } else {
                list.add(i)
            }
        }
        return list
    }

    private fun displayTiles(
        tiles: List<Tile>,
        cta: TilesCta?,
        areThereEnoughTilesToRender: Boolean,
        phoneBreakpoint: Boolean,
        gridAdapter: GridAdapter,
        updateIndices: List<Int> = emptyList()
    ) {
        composeFormWrapperView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent {
                AndroidClassicTheme {
                    Surface(
                        color = Color.Unspecified
                    ) {
                        Column {
                            TilesGrid(tiles,
                                areThereEnoughTilesToRender,
                                cardified,
                                phoneBreakpoint,
                                updateIndices,
                                { onHabitTileClicked?.invoke(it) })
                            TilesCta(
                                cta = cta,
                                areThereEnoughTilesToRender = areThereEnoughTilesToRender
                            ) { gridAdapter.environment.openLink(it) }
                        }
                    }
                }
            }
        }
    }

    private fun logHabitTiles() {
        previousTiles.forEach { tile ->
            val key = tile.tileLink
            if (shouldLogTile(sectionsHabitTilesRequestId.toString(), key.toString())) {
                userHistoryViewModel?.addHabitTileViewedItem(
                    tile.tileLink,
                    sectionsHabitTilesRequestId,
                    tile.position,
                    tile.tileCategory,
                    tile.tileLabel,
                    tile.tileCategoryDetail,
                    sectionsHabitTilesTestGroup
                )
            }
        }
    }

    private fun shouldLogTile(requestId: String, tileKey: String): Boolean {
        val set = loggedByRequest.getOrPut(requestId) { mutableSetOf() }
        return set.add(tileKey)
    }

    init {
        itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                logHabitTiles()
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })
    }
}

