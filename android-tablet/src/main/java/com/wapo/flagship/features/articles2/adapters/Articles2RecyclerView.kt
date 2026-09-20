package com.wapo.flagship.features.articles2.adapters

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import com.wapo.view.selection.SelectableLayoutManager
import com.wapo.view.selection.SelectableRecyclerView

/**
 * A wrapper class for selectable recycler view with [SelectableLayoutManager]
 * This is required so that we can use the selection callbacks when text is selected in selection mode.
 */
class Articles2RecyclerView(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : SelectableRecyclerView(
        context,
        attrs,
        defStyleAttr,
    ) {
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?) : this(context, null)

    init {
        layoutManager = SelectableLayoutManager(context)
    }

    override fun requestFocus(
        direction: Int,
        previouslyFocusedRect: Rect?,
    ): Boolean = false
}
