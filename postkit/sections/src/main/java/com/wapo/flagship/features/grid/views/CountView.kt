/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.wapo.flagship.features.grid.model.Count
import com.wapo.flagship.features.grid.model.Size
import com.washingtonpost.android.sections.R

class CountView : FrameLayout {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(
        context, attrs, defStyleAttr
    )

    private var underlinedTextView: UnderlinedTextView? = null

    fun init() {
        underlinedTextView = findViewById(R.id.countText)
    }

    fun setCount(count: Count?) {
        if (count == null || count.count.isNullOrEmpty()) {
            visibility = View.GONE
            return
        } else {
            visibility = View.VISIBLE
        }
        underlinedTextView?.apply {
            text = count.count
            setUnderlined(count.size == Size.LARGE)
        }
    }
}