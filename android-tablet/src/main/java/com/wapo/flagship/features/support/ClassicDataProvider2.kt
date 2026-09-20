package com.wapo.flagship.features.support

import android.content.Context
import com.wapo.android.commons.util.Utils
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ClassicDataProvider2(
    val context: Context,
) : DataProvider2() {
    private val ZENDESK_PREMIUM_AD_FREE_ANNOTATION = "(adfree)"

    override val versionName: String? =
        Utils.getAppVersionCode(context).toString() + "," +
            Utils.getAppVersionName(
                context,
            )

    override val appName: String? = context.resources.getString(R.string.app_name_full)

    override val paywallType: String? = pwType()

    override val paywallSource: String? = PaywallService.getInstance().paywallSource

    override val paywallExpiration: String? = expiry()

    override val paywallPartnerId: String? = PaywallService.getInstance().paywallPartnerId

    override val loginId: String? = PaywallService.getInstance().loginId

    override val isPaywallTurnedOn: Boolean = PaywallService.getInstance().isTurnedOn

    val paywallIsAdFreeSub: Boolean = PaywallService.getInstance().shouldEnableAdfreeExperience()

    private fun pwType(): String {
        var pwType = ""
        val subSource = PrefUtils.getPrefPaywallSubSource(context)
        val shortTitle = PrefUtils.getPrefPaywallSubShortTitle(context)
        if (subSource != null && shortTitle != null) {
            pwType = "$subSource. $shortTitle"
            if (paywallIsAdFreeSub) {
                pwType += " $ZENDESK_PREMIUM_AD_FREE_ANNOTATION"
            }
        }
        return pwType
    }

    private fun expiry(): String {
        var expiry = ""
        val accessExpiryDate = PaywallService.getInstance().wapoAccessServiceInstance.accessExpiryDate
        if (accessExpiryDate != null) {
            val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            expiry = df.format(accessExpiryDate)
        }
        return expiry
    }
}
