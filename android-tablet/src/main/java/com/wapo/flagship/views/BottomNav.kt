package com.wapo.flagship.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNav
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : BottomNavigationView(context, attrs, defStyle) {
        override fun getMaxItemCount(): Int = 6
    }
