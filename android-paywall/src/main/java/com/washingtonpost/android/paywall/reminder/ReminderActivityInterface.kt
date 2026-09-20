package com.washingtonpost.android.paywall.reminder

import com.washingtonpost.android.paywall.util.PaywallConstants

interface ReminderActivityInterface {
    fun showPaywallFromReminder(type: PaywallConstants.WallType)
    fun startPurchaseFlow(productSku: String, offer: String?, type: PaywallConstants.WallType)
}