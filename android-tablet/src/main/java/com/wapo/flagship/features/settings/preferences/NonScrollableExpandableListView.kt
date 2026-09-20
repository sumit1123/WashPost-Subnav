package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.ExpandableListView

/**
 * A custom ExpandableListView that properly calculates its height when placed
 * inside a scrollable parent (like RecyclerView in PreferenceFragment).
 *
 * This solves the issue where ExpandableListView with wrap_content height
 * only shows one item when nested inside another scrollable view.
 */
class NonScrollableExpandableListView : ExpandableListView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Make the height as large as possible to show all items
        val expandedHeightSpec = MeasureSpec.makeMeasureSpec(
            Int.MAX_VALUE shr 2,
            MeasureSpec.AT_MOST
        )
        super.onMeasure(widthMeasureSpec, expandedHeightSpec)
        layoutParams.height = measuredHeight
    }
}

