package com.wapo.view.tooltip

import android.view.View
import android.view.ViewGroup

/**
 * This data class holds all the required properties to build a tooltip.
 */
data class TooltipProperties(
        /**
         * Text shown on the tooltip.
         */
        val text: CharSequence = "",
        /**
         * The view on which the tooltip os anchored.
         */
        val anchorView: View?,
        /**
         * Parent view in which this tool tip will be shown
         */
        val parentView: ViewGroup? = null,
        /**
         * Tooltip related data that such as priority, gravity, string resources etc.
         */
        val tooltipData: TooltipData
)