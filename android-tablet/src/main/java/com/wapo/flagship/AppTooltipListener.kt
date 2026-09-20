package com.wapo.flagship

import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.view.tooltip.TooltipListener

class AppTooltipListener : TooltipListener {
    override fun onTooltipDisplayed() {
        AirshipAnalytics.startTracking(AirshipAnalytics.Screen.TOOLTIP_SCREEN.name)
    }

    override fun onTooltipFinished() {
        AirshipAnalytics.stopTracking(AirshipAnalytics.Screen.TOOLTIP_SCREEN.name)
    }
}
