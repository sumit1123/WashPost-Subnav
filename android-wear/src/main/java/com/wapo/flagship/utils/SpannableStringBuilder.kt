/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import androidx.core.text.inSpans
import com.wapo.view.CustomTypefaceSpan

inline fun SpannableStringBuilder.typeface(
    customTypefaceSpan: CustomTypefaceSpan,
    builderAction: SpannableStringBuilder.() -> Unit
): SpannableStringBuilder = inSpans(customTypefaceSpan, builderAction = builderAction)

inline fun SpannableStringBuilder.typefaceAndScale(
    customTypefaceSpan: CustomTypefaceSpan,
    proportion: Float,
    builderAction: SpannableStringBuilder.() -> Unit
): SpannableStringBuilder =
    inSpans(customTypefaceSpan, RelativeSizeSpan(proportion), builderAction = builderAction)