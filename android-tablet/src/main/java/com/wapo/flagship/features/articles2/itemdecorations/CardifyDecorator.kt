package com.wapo.flagship.features.articles2.itemdecorations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.util.containsValue
import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.SubNav
import com.wapo.flagship.features.articles2.models.deserialized.Ad
import com.wapo.flagship.features.articles2.models.deserialized.Card
import com.wapo.flagship.features.articles2.models.deserialized.ExpandCollapseCard
import com.wapo.flagship.features.articles2.models.deserialized.Types
import com.washingtonpost.android.R

class CardifyDecorator(
    context: Context,
    private val marginItemDecoration: MarginItemDecoration?,
    private val adViews: SparseArray<View>,
) : RecyclerView.ItemDecoration() {
    private val resources = context.resources
    private val firstDrawable =
        ContextCompat.getDrawable(
            context,
            R.drawable.article_cardification_first_card_bg,
        )
    private val drawable = ContextCompat.getDrawable(context, R.drawable.article_cardification_bg)
    private val lastDrawable =
        ContextCompat.getDrawable(
            context,
            R.drawable.article_cardification_last_card_bg,
        )
    private val noCardDrawable =
        ContextCompat.getDrawable(
            context,
            R.drawable.article_cardification_no_card_bg,
        )
    private val borderStrokeWidth =
        resources.getDimensionPixelSize(
            com.washingtonpost.android.articles.R.dimen.card_article_stroke_width,
        )
    private val cardBottomPadding = resources.getDimensionPixelSize(com.washingtonpost.android.articles.R.dimen.articles_x_large_margin)
    private val cardTopPadding = resources.getDimensionPixelSize(com.washingtonpost.android.articles.R.dimen.articles_large_margin)
    private val cards = mutableMapOf<String, MutableList<View>>()
    private val itemCardTypeMap = mutableMapOf<Int, Card>()

    fun processItemsCards(items: List<Item>?) {
        var currentCardId: String? = null
        var isFirstCard = true
        var lastKnownCardId: String? = null
        itemCardTypeMap.clear()
        items?.forEach { item ->
            if (item.group != null && item.group != lastKnownCardId) {
                lastKnownCardId = item.group
            }
            if (item.type == Types.RECIRC.type || item.type == Types.TAGLINE.type) {
                item.group = lastKnownCardId
            }
        }
        if (lastKnownCardId == null) return
        items?.forEachIndexed { index, item ->
            if (item.group == null) {
                currentCardId = item.group
                itemCardTypeMap[index] = Card.NO_CARD
            } else if (item is SubNav) {
                currentCardId = null
            } else if (currentCardId == null) {
                currentCardId = item.group
                itemCardTypeMap[index] = if (!isFirstCard) Card.CARD_START else Card.FIRST_CARD_START
                isFirstCard = false
            } else if (item.group == currentCardId) {
                itemCardTypeMap[index] =
                    if (index + 1 == items.size) {
                        Card.LAST_CARD_END
                    } else if (index + 1 < items.size && items[index + 1].group != currentCardId) {
                        Card.CARD_END
                    } else {
                        Card.CARD_MIDDLE
                    }
            } else {
                currentCardId = item.group
                itemCardTypeMap[index] = Card.CARD_START
            }
        }
    }

    override fun onDraw(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.onDraw(c, parent, state)
        if (itemCardTypeMap.isEmpty()) return
        cards.clear()
        val items = (parent.adapter as? Articles2ItemsRecyclerViewAdapter)?.currentList
        if (items.isNullOrEmpty()) return

        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)
            val adapterPosition = parent.getChildAdapterPosition(view)
            if (adViews.containsValue(view) || itemCardTypeMap[adapterPosition] == Card.NO_CARD) {
                continue
            }
            if (adapterPosition >= 0 && adapterPosition < items.size) {
                val item = items[adapterPosition]
                val cardId = item?.group
                if (cardId != null) {
                    val cardViews = cards[cardId] ?: mutableListOf()
                    cardViews.add(view)
                    cards[cardId] = cardViews
                } else {
                    cards[adapterPosition.toString()] = mutableListOf(view)
                }
            } else {
                adViews.forEach { key, value ->
                    if (view == value) {
                        cards[key.toString()] = mutableListOf(view)
                    }
                }
        }

            cards.forEach { entry ->
                val views = entry.value

                if (views.isNotEmpty()) {
                    val top = views.minBy { it.top }.top - cardTopPadding
                    val bottom = views.maxBy { it.bottom }.bottom + cardBottomPadding
                    val left = parent.left + borderStrokeWidth
                    val right = parent.right - borderStrokeWidth
                    val firstChildAdapterPosition = parent.getChildAdapterPosition(views[0])
                    val lastChildAdapterPosition =
                        parent.getChildAdapterPosition(
                            views[views.size - 1],
                        )
                    val drawable =
                        if (itemCardTypeMap[firstChildAdapterPosition] == Card.FIRST_CARD_START) {
                            firstDrawable
                        } else if (itemCardTypeMap[lastChildAdapterPosition] == Card.LAST_CARD_END) {
                            lastDrawable
                        } else if (itemCardTypeMap[firstChildAdapterPosition] == Card.NO_CARD || firstChildAdapterPosition == -1) {
                            noCardDrawable
                        } else {
                            drawable
                        }
                    drawable?.setBounds(left, top, right, bottom)
                    drawable?.draw(c)
                }
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        if (itemCardTypeMap.isEmpty()) return
        val insets = Rect()
        marginItemDecoration?.getItemOffsets(insets, view, parent, state)
        val currentList = (parent.adapter as? Articles2ItemsRecyclerViewAdapter)?.currentList
        val childAdapterPosition = parent.getChildAdapterPosition(view)
        val cardType = itemCardTypeMap[childAdapterPosition]
        if (cardType == Card.NO_CARD && adViews.containsValue(view)) {
            return
        }
        if (childAdapterPosition >= 0 && childAdapterPosition < itemCardTypeMap.size) {
            val cardType = itemCardTypeMap[childAdapterPosition]
            val isPrevAd = currentList != null && childAdapterPosition - 1 >= 0 && currentList[childAdapterPosition - 1] is Ad
            val isNextAd = currentList != null && childAdapterPosition + 1 < currentList.size && currentList[childAdapterPosition + 1] is Ad

            when (cardType) {
                Card.CARD_START -> {
                    if (isPrevAd) {
                        outRect.top = 0
                    } else {
                        outRect.top = -insets.top + cardTopPadding
                    }
                }
                Card.CARD_END -> {
                    if (isNextAd) {
                        outRect.bottom = 0
                    } else {
                        outRect.bottom = -insets.bottom + cardBottomPadding
                    }
                }
                Card.CARD_MIDDLE -> {
                    if (currentList != null && childAdapterPosition in 0 until currentList.size) {
                        val item = currentList[childAdapterPosition]
                        if (item is ExpandCollapseCard && item.state?.isTruncated() == true) {
                            outRect.top = 0
                            outRect.bottom = -insets.bottom
                        }
                    }
                }
                Card.NO_CARD -> {
                    outRect.top = -insets.top + cardTopPadding
                    outRect.bottom = -insets.bottom + cardBottomPadding
                }
                else -> {
                    outRect.top = 0
                    outRect.bottom = 0
                }
            }
        }
        adViews.forEach { _, value ->
            if (view == value) {
                outRect.top = -insets.top + cardTopPadding
                outRect.bottom = -insets.bottom + cardBottomPadding
            }
        }
    }
}
