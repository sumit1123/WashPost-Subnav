package com.washingtonpost.android.paywall.bottomsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

/**
 * Use this [MaterialButtonToggleGroup] extended class if you want the button text to be 2 lines.
 */
class MultilineMaterialButtonToggleGroup : MaterialButtonToggleGroup {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun addView(child: View?) {
        super.addView(child)
        if (child is MaterialButton)
            child.maxLines = 2
    }
}