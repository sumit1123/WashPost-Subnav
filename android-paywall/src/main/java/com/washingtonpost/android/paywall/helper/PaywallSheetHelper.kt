package com.washingtonpost.android.paywall.helper

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import com.wapo.android.commons.iterable.AttributionInfo
import com.washingtonpost.android.paywall.PaywallOmniture
import com.washingtonpost.android.paywall.bottomsheet.ui.PaywallSheet2Fragment
import com.washingtonpost.android.paywall.util.PaywallConstants

class PaywallSheetHelper(private val fragActivity: FragmentActivity, private val paywallAnalytics: PaywallOmniture) {

    private val paywallSheetFragment: PaywallSheet2Fragment? get() = fragActivity.supportFragmentManager.findFragmentByTag(paywallTag) as? PaywallSheet2Fragment
    private val paywallTag = "paywall_sheet"

    @UiThread
    fun showWall(
        wallName: String? = null,
        wallType: PaywallConstants.WallType? = null,
        defaultIntervalOverride: Int? = null,
        wallReason: Int,
        preventAutoDismiss: Boolean = false,
        attributionInfo: AttributionInfo? = null,
        onShow: () -> Unit = {}
    ) {
        val fragment = paywallSheetFragment
        if (fragment != null && fragment.isAdded && fragment.isVisible) {
            return
        }

        PaywallSheet2Fragment().show(
            fragActivity.supportFragmentManager,
            paywallTag,
            paywallAnalytics,
            wallType,
            wallReason,
            wallName,
            wallName,
            defaultIntervalOverride,
            isRegWallOriginated = wallReason == PaywallConstants.WallType.REGWALL.ordinal,
            preventAutoDismiss = preventAutoDismiss,
            attributionInfo = attributionInfo
        )
        onShow()
    }
    @UiThread
    fun showWall(wallName: String? = null, wallType: PaywallConstants.WallType? = null, wallReason: Int, onShow: () -> Unit = {}) {
        showWall(wallName, wallType, null, wallReason, false, null, onShow)
    }

    fun savePaywallFragmentState(@NonNull outState: Bundle) {
        paywallSheetFragment?.apply {
            if (isAdded) {
                fragActivity.supportFragmentManager.putFragment(outState, paywallTag, this)
            }
        }
    }

    fun doIfVisible(isActivityFinishing: Boolean, action: () -> Unit = {}) {
        paywallSheetFragment?.apply {
            if (isVisible && !isActivityFinishing) {
                action()
            }
        }
    }

    fun isPaywallSheetShowing() = paywallSheetFragment?.isVisible == true

    fun isRegwallShowing() = paywallSheetFragment?.isRegwall() == true

    @UiThread
    fun dismissPaywallSheetDialog(isActivityFinishing: Boolean) {
        paywallSheetFragment?.apply {
            if (isVisible && !isActivityFinishing) {
                dismissWall()
            }
        }
    }

    companion object {
        var scrollThreshold = 500
        val isUnderScrollThresholds: (x: Int) -> Boolean = { x -> x < scrollThreshold }

        fun getResultId(paywallType: PaywallConstants.WallType?): Int {
            return when (paywallType) {
                PaywallConstants.WallType.SETTINGS_PAYWALL -> PaywallConstants.SETTINGS_PAYWALL_RESULT_ID
                PaywallConstants.WallType.ONBOARDING_PAYWALL -> PaywallConstants.ONBOARDING_PAYWALL_RESULT_ID
                PaywallConstants.WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL -> PaywallConstants.GLOBAL_SUBSCRIBE_BUTTON
                PaywallConstants.WallType.REGWALL -> PaywallConstants.REGWALL_REGISTER
                else -> PaywallConstants.METERED_PAYWALL_RESULT_ID
            }
        }

    }
}