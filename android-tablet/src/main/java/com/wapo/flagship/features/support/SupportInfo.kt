package com.wapo.flagship.features.support

import android.os.Build
import android.text.TextUtils
import com.google.gson.Gson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

class SupportInfo(
    deviceConnectivity: String?,
    deviceId: String?,
    deviceName: String?,
    dataProvider: ClassicDataProvider2,
) {
    var app: SupportInfoApp? =
        SupportInfoApp().apply {
            version = dataProvider.versionName
            name = dataProvider.appName
        }
    var user: User? = null
    var device: Device = Device()

    override fun toString(): String {
        val sb = StringBuilder()
        if (app != null) {
            sb.append(Gson().toJson(app))
        }
        return sb.toString()
    }

    init {
        val userLoginId = dataProvider.loginId
        if (!TextUtils.isEmpty(userLoginId)) {
            user =
                User().apply {
                    loginId = userLoginId
                    if (dataProvider.isPaywallTurnedOn) {
                        subscription =
                            Subscription().apply {
                                source = dataProvider.paywallSource
                                type = dataProvider.paywallType
                                partner = dataProvider.paywallPartnerId // currently always null
                                expiration = dataProvider.paywallExpiration
                                if (TextUtils.isEmpty(source) &&
                                    TextUtils.isEmpty(type) &&
                                    TextUtils.isEmpty(partner) &&
                                    TextUtils.isEmpty(expiration)
                                ) {
                                    subscription = null
                                }
                            }
                    }
                }
        }
        device.connectivity = deviceConnectivity
        device.id = deviceId
        device.model = deviceName
        device.operatingSystemVersion = Build.VERSION.RELEASE
    }
}

@JsonClass(generateAdapter = true)
data class SupportInfoApp(
    @Json(name = "version")
    var version: String? = null,
    @Json(name = "name")
    var name: String? = null,
)

@JsonClass(generateAdapter = true)
data class Subscription(
    @Json(name = "source")
    var source: String? = null,
    @Json(name = "type")
    var type: String? = null,
    @Json(name = "partner")
    var partner: String? = null,
    @Json(name = "expiration")
    var expiration: String? = null,
)

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "login_id")
    var loginId: String? = null,
    @Json(name = "subscription")
    var subscription: Subscription? = null,
)

@JsonClass(generateAdapter = true)
data class Device(
    @Json(name = "connectivity")
    var connectivity: String? = null,
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "model")
    var model: String? = null,
    @Json(name = "operating_system_version")
    var operatingSystemVersion: String? = null,
)
