package com.washingtonpost.android.paywall.billing

import android.content.Context
import com.washingtonpost.android.paywall.billing.AbstractBillingActivity.ResponseCode

interface NativePaywallResultCallbacks {
    fun handleActivityResponse(
        context: Context,
        responseCode: Int,
    ) {
        when (responseCode) {
            ResponseCode.RESULT_OK -> onSuccess(context, responseCode)
            ResponseCode.RESULT_CANCELED -> onCanceled(context, responseCode)
            ResponseCode.RESULT_ERROR -> onError(context, responseCode)
            ResponseCode.RESULT_INVALID_OFFER -> onError(context, responseCode)
        }
    }

    fun onSuccess(
        context: Context,
        responseCode: Int,
    )

    fun onError(
        context: Context,
        responseCode: Int,
    )

    fun onCanceled(
        context: Context,
        responseCode: Int,
    )
}