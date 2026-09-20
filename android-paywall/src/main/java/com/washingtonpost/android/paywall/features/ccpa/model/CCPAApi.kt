package com.washingtonpost.android.paywall.features.ccpa

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.washingtonpost.android.paywall.BuildConfig
import java.util.*

const val VERSION = "1"
const val EXPLICIT_NOTICE = "Y"
const val LSPA = "Y"

data class SaveIdentityPreferencesRequest(val caller: String?,
                                          val requestId: String?,
                                          val privacySetting: PrivacySetting?) {
    constructor(privacySetting: PrivacySetting) : this("washpost", UUID.randomUUID().toString(), privacySetting)
}

data class PrivacySetting(val ccpa: CCPA?, val oneTrustConsent:String?)

data class CCPA(val optOut: String?,
                val explicitNotice: String?,
                val iabSpec: String?,
                val lspa: String?) {
    constructor(optOut: String?, explicitNotice: String?) : this(optOut, explicitNotice, VERSION, LSPA)
}

data class SaveIdentityPreferencesResponse(
    @SerializedName("status")
    val status: String?,
    @SerializedName("errorState")
    val errorState: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("messages")
    val messages: Array<Any>?,
    @SerializedName("userState")
    val userState: UserState?,
    @SerializedName("responseJson")
    var responseJson: String?
) {
    override fun toString(): String {
        return if (BuildConfig.DEBUG) {
            Gson().toJson(this)
        } else {
            super.toString()
        }
    }
}

data class UserState(val geoState: String?,
                     val privacySetting: PrivacySetting?)