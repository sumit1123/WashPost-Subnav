/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

import android.view.View
import androidx.core.content.ContextCompat
import com.washingtonpost.android.R

object RippleHelper {

    fun addRippleEffectToView(v: View) {
        val rippleDrawable = ContextCompat.getDrawable(v.context, R.drawable.wear_ripple_effect)
        // set rippleDrawable as foreground drawable.
        v.foreground = rippleDrawable
    }

}