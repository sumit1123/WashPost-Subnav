package com.wapo.flagship.features.search2.ui.decor

import android.graphics.Color
import androidx.annotation.ColorInt
import com.wapo.flagship.features.search2.ui.decor.Rules.MIDDLE

/**
 * @param color color of divider
 * @param height height of divider
 * @param paddingStart padding at start of divider
 * @param paddingEnd padding at end of divider
 * @param rule rule for draw item divider
 */
class Gap(
    @ColorInt val color: Int = Color.TRANSPARENT,
    val height: Int = 0,
    val paddingStart: Int = 0,
    val paddingEnd: Int = 0,
    @DividerRule val rule: Int = MIDDLE
)

const val UNDEFINE_VIEW_HOLDER: Int = -1
