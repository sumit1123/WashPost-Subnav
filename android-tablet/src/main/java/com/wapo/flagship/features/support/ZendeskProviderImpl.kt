package com.wapo.flagship.features.support

import android.content.ContentResolver
import com.squareup.moshi.Moshi
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.zendesk.ZendeskProvider
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.TicketForm
import com.washingtonpost.android.config.domain.models.config.ZendeskConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.volley.RequestQueue
import java.io.File

class ZendeskProviderImpl : ZendeskProvider {
    override val requestQueue: RequestQueue = FlagshipApplication.getInstance().requestQueue

    override val metadataCustomField: String = buildMetadata()

    override val config: ZendeskConfig
        get() = ConfigManager.getInstance().config.zendeskConfig
            .let {
                if (isBeta) {
                    it.copy(
                        url = "https://twpinternal.zendesk.com",
                        ticketForms =
                            listOf(
                                TicketForm(id = 24883654196379, name = "Android Beta Feedback"),
                            ),
                    )
                } else {
                    it
                }
            }

    override val cacheDir: File = File(FlagshipApplication.getInstance().cacheDir, "wp_zendesk")

    override val contentResolver: ContentResolver = FlagshipApplication.getInstance().contentResolver

    override val email: String = PaywallService.getInstance().loggedInUser?.userId ?: ""

    override val name: String = PaywallService.getInstance().loggedInUser?.displayName ?: ""

    override val isBeta: Boolean = BuildConfig.BUILD_TYPE.contains("beta")

    private fun buildMetadata(): String {
        val context = FlagshipApplication.getInstance()

        val deviceConnectivity = Measurement.detectConnectionType(context)
        val deviceId = DeviceUtils.getUniqueDeviceId(context)
        val deviceName = DeviceUtils.getDeviceName()
        val dataProvider = ClassicDataProvider2(context)

        val supportInfo = SupportInfo(deviceConnectivity, deviceId, deviceName, dataProvider)
        val zendeskMetadata = ZendeskMetadata(supportInfo.app, supportInfo.device, supportInfo.user)

        val moshi: Moshi = Moshi.Builder().build()
        return moshi.adapter(ZendeskMetadata::class.java).toJson(zendeskMetadata)
    }
}
