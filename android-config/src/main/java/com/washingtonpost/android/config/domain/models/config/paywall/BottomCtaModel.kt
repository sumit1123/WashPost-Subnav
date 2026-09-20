package com.washingtonpost.android.config.domain.models.config.paywall

import com.wapo.android.commons.util.Utils

data class BottomCtaModel(
    val productId: String,
    val enabled: Boolean,
    val newSubMessage: String,
    val terminatedSubMessage: String,
) {
    companion object {
        val backupModel = BottomCtaModel(
            productId = if (Utils.isAmazonBuild()) {
                "wp.unified.basic"
            } else {
                "wp.classic.basic"
            },
            enabled = true,
            newSubMessage = "Unlimited access to all our journalism",
            terminatedSubMessage = "Unlimited access to all our journalism",
        )
    }
}