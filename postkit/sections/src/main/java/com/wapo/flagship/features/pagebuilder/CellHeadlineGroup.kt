package com.wapo.flagship.features.pagebuilder

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.wapo.flagship.features.grid.model.Headline
import com.wapo.flagship.features.grid.model.HeadlineIcon
import com.wapo.view.FlowableTextView
import com.wapo.view.FlowableView
import com.washingtonpost.android.sections.R

/**
 * Layout that combines [CellHeadlineView] and a deck view ([FlowableTextView]) and also implements
 * [FlowableView] interface. [FlowableView.setFlowObstruction] and other [TextView] calls will be forwarded to both internal
 * headline and deck views
 */
class CellHeadlineGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), FlowableView {

    val headlineView: CellHeadlineView
    val deckView: FlowableTextView

    init {
        orientation = VERTICAL

        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_homepage_headline_group, this, true)

        headlineView = findViewById(R.id.headline)
        deckView = findViewById(R.id.deck)

        if (!ViewCompat.hasAccessibilityDelegate(this)) {
            ViewCompat.setAccessibilityDelegate(
                this,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        nodeInfo: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, nodeInfo)
                        val itemInfo = AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                            0,
                            1,
                            0,
                            1,
                            true
                        )
                        nodeInfo.setCollectionItemInfo(itemInfo)
                    }
                })
        }
        isFocusable = true
    }

    fun setHeadline(
        headline: Headline?,
        prefixIcon: Drawable?,
        postfixIcon: Drawable?,
        grid: Boolean
    ) {
        headlineView.setHeadline(headline, prefixIcon, postfixIcon, grid)
    }

    fun setHeadline(
        headline: Headline?,
        prefixIcon: HeadlineIcon?,
        postfixIcon: HeadlineIcon?,
        grid: Boolean
    ) {
        headlineView.setHeadline(headline, prefixIcon, postfixIcon, grid)
    }

    fun setDeck(deck: CharSequence?) {
        deckView.text = deck
        if (deck.isNullOrBlank()) {
            deckView.visibility = View.GONE
        } else {
            deckView.visibility = View.VISIBLE
            deckView.setTextAppearance(R.style.homepagestory_deck_style)
        }
    }

    override fun setFlowObstruction(widthAdjustment: Int, heightAdjustment: Int, floatType: Int) {
        headlineView.setFlowObstruction(widthAdjustment, heightAdjustment, floatType)
        deckView.setFlowObstruction(widthAdjustment, heightAdjustment, floatType)
    }

    fun setTextGravity(gravity: Int) {
        headlineView.setTextGravity(gravity)
        deckView.setTextGravity(gravity)
    }

    fun getText() : CharSequence {
        return headlineView.text
    }

    fun hasTextBelowBox(): Boolean {
        return headlineView.hasTextBelowBox() || deckView.hasTextBelowBox()
    }
}