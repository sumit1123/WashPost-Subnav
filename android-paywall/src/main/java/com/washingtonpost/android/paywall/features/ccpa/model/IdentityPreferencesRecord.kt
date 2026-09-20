@file:JvmName("IdentityPreferences")

package com.washingtonpost.android.paywall.features.ccpa;

import com.google.gson.Gson
import com.washingtonpost.android.paywall.BuildConfig

const val FLAG_YES = "Y"
const val FLAG_NO = "N"

data class IdentityPreferencesRecord(var adsOptOut: String?,
                                     var explicitNotice: String?,
                                     var dataSynchronized: String?,
                                     var serverResponse: String?,
                                     var switchTimestamp: Long?,
                                     var otContentSynchronized: String?) {
    override fun toString(): String {
        return if (BuildConfig.DEBUG) {
            Gson().toJson(this)
        } else {
            super.toString()
        }
    }
}
