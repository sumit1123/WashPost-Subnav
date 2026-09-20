@file:JvmName("CCPAUtils")

package com.washingtonpost.android.paywall.features.ccpa

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.annotation.NonNull
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.WpPaywallHelper

private val prefCCPAOptOut = "pref.PREF_CCPA_OPT_OUT"

fun isCCPAOptedOut(): Boolean {
    val record = WpPaywallHelper.getIdentityPreferences()
    if (record != null) {
        return FLAG_YES == record.adsOptOut
    }
    return true
}

fun getCCPABundle(): Bundle? {
    val rdpValue = if (isCCPAOptedOut()) 1 else 0
    if (rdpValue != 1) {
        return null
    }
    return Bundle().apply {
        putInt("rdp", rdpValue)
    }
}

fun appendCCPAQueryParameterToUrl(url: String?): String? {
    val rdpValue = if (isCCPAOptedOut()) 1 else 0
    if (url.isNullOrEmpty() || rdpValue != 1) {
        return url
    }
    return Uri.parse(url).buildUpon().appendQueryParameter("rdp", rdpValue.toString()).build().toString()
}

// Note: Just storing opt out in shared prefs for toggling third party libraries even before PaywallService is initialized.
// Use isCCPAOptedOut() for all other places once PaywallService is initialized and db is up.
fun setHasUserOptedOutCCPAAdsTracking(@NonNull context: Context, @NonNull optOut: Boolean) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val editor = sharedPreferences.edit()
    editor.putBoolean(prefCCPAOptOut, optOut)
    editor.apply()
    PaywallService.getConnector().onCCPAAdsTrackingUpdated()
}

fun hasUserOptedOutCCPAAdsTracking(@NonNull context: Context): Boolean {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    return sharedPreferences.getBoolean(prefCCPAOptOut, false)
}

fun setCCPAGadRdpFlagInSharedPrefs(activity: Activity) {
    val gadRdpKey = "gad_rdp"
    if (activity != null && !activity.isFinishing) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        if (isCCPAOptedOut()) {
            editor.putInt(gadRdpKey, 1)
        } else {
            if (sharedPref.contains(gadRdpKey)) {
                editor.remove(gadRdpKey)
            }
        }
        editor.commit()
    }
}

fun getCCPAAdsPrivacyString(context: Context): String {
    val optOutFlag = if (hasUserOptedOutCCPAAdsTracking(context)) "Y" else "N"
    return "$VERSION$EXPLICIT_NOTICE$optOutFlag$LSPA"
}