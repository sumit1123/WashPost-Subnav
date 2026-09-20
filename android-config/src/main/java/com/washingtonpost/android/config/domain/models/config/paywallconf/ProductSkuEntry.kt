package com.washingtonpost.android.config.domain.models.config.paywallconf

import com.wapo.android.commons.util.Utils

data class ProductSkuEntry(
    val playstore: String? = null,
    val amazon: String? = null,
    val basePlanId: String? = null,
    val type: String? = null,
    val duration: Long? = null,
) {
    val isNonRenewable: Boolean
        get() = type == NON_RENEWABLE

    val isOneDayPass: Boolean
        get() = playstore?.equals(
            PLAYSTORE_FLEX_SKU,
            ignoreCase = true
        ) == true && basePlanId?.contains("one-day") == true

    val resolvedSku: String?
        get() = if (Utils.isAmazonBuild()) amazon else playstore

    /** The composite key used to look up this product in IAPSubItems ("sku:basePlanId" or just "sku" format)
     *  e.g. wp.classic.flex:one-day-pass-1; wp.classic.basic */
    val compositeKey: String?
        get() {
            val sku = resolvedSku ?: return null
            return if (basePlanId != null) "$sku:$basePlanId" else sku
        }

    companion object {
        const val NON_RENEWABLE = "non-renewable"
        const val PLAYSTORE_FLEX_SKU = "wp.classic.flex"
    }
}
