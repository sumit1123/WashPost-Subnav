// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VerticalVideosRecyclerView(
    context: Context,
    attrs: AttributeSet?,
) : RecyclerView(
        context,
        attrs,
    ) {
    init {
        layoutManager = LinearLayoutManager(context)
    }
}
