package com.wapo.view.tooltip

import android.view.Gravity
import androidx.annotation.StringRes
import com.wapo.view.R


/**
 * This class is used as a data holder for all the tooltips.
 * [priorityData] is the data associated with priorities and screen types
 * [sharedPrefKey] is the shared pref. key used to store shown tooltips
 */
class TooltipData(
        /**
         * Priority related data for the tooltip which decides which tooltip to show first.
         */
        open val priorityData: TooltipPriority,
        /**
         * pref. key for storing when tooltip is already shown.
         */
        open val sharedPrefKey: String,
        /**
         * String resource id for tooltip text.
         */
        @StringRes open val tooltipTextResourceId: Int = 0,
        /**
         * String resource id for bold portion of tooltip text.
         */
        @StringRes open val boldPortionResourceId: Int = 0,
        /**
         * <div class="pt">Set where the tool tip will appear in relation to the <tt>anchorView</tt>.
         * Use <tt>[Gravity.TOP]</tt> and <tt>[Gravity.BOTTOM]</tt>.
         * Default is <tt>[Gravity.BOTTOM]</tt>.</div>
         */
        open val gravity: Int = Gravity.BOTTOM,
        open val verticalMargin: Int = 0,
        /**
         * <div class="pt">Set's the shape of the gap in the overlay that will appear over the
         * <tt>anchorView</tt>.
         * Use <tt>[TooltipPopup.HIGHLIGHT_SHAPE_OVAL]</tt> and
         * <tt>[TooltipPopup.HIGHLIGHT_SHAPE_RECTANGLE]</tt>.
         * Default is <tt>[TooltipPopup.HIGHLIGHT_SHAPE_OVAL]</tt>.</div>
         */
        open val highlightShape: Int = TooltipPopup.HIGHLIGHT_SHAPE_OVAL,
        /**
         * duration for which the tooltip will be shown if auto-dismissed
         */
        open val duration: Long = 10000,
        /**
         * Whether or not the tooltip is automatically dismissed after duration above is passed.
         */
        open val autoDismissible: Boolean = false,
        /**
         * Whether or not the tooltip will show an arrow.
         */
        open val hideArrow: Boolean = false

)