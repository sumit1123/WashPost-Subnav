// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.providers

import android.content.Context
import android.database.Cursor
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt

/**
 * Helper class to process login and subscription info from WapoSubscriptionProvider (rainbow)
 */
class WapoSubscriptionProviderHelper(
    val context: Context,
) {
    companion object {
        const val PROVIDER_URL =
            "content://com.washingtonpost.rainbow.providers.WapoSubscriptionProvider/userSubInfo"
        const val AMAZON_CLASSIC_PROVIDER_URL =
            "content://com.wapo.flagship.providers.AmazonClassicSubscriptionProvider/userSubInfo"
        const val ROW_TOKEN_RESPONSE_JSON = "tokenResponseJson"
        const val ROW_WAPO_LOGIN_ID = "wapoLoginId"
        const val ROW_WAPO_SECURE_LOGIN_ID = "wapoSecureLoginId"
        const val ROW_RECEIPT_ID = "receiptId"
        const val ROW_PRODUCT_ID = "sku"
        const val ROW_TRANSACTION_DATE = "transactionDate"
        const val ROW_IAP_TOKEN = "iapToken"
        const val ROW_HAS_RAINBOW_APP = "hasRainbowApp"

        // This essentially means the user has seen the last screen on the unification onboarding.
        const val ROW_HAS_MIGRATED = "hasMigrated"
        const val ROW_SHOULD_TRANSFER_USER_INFO = "shouldTransferUserInfo"
        const val ROW_HAS_MIGRATED_AMAZON_CLASSIC = "hasMigratedFromAmazonClassic"
        const val ROW_HAS_AMAZON_CLASSIC_APP = "hasAmazonClassicApp"
        const val ROW_AMAZON_USER_ID = "amazonUserId"
        const val ROW_ALERT_TOPICS_LIST = "alertTopicsList"
        const val TAG = "WapoSubscriptionProvider"
    }

    fun processCursor(cursor: Cursor?) {
        val paywallService = PaywallService.getInstance() ?: return
        cursor?.let {
            var tokenResponse: String? = null
            var receiptId: String? = null
            var sku: String? = null
            var transactionDate: Long? = null
            var iapToken: String? = null
            var shouldTransferUserInfo = false
            var hasRainbowApp = false
            var hasAmazonClassicApp = false
            if (!cursor.isClosed && cursor.moveToFirst()) {
                Logger.d("WapoSubProvider", "Begin reading provider")

                // extract token response from a login user if it exists
                val columnIndex = cursor.getColumnIndex(ROW_TOKEN_RESPONSE_JSON)
                if (columnIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedTokenResponse == null) {
                    val tokenResponseString = cursor.getString(columnIndex)
                    PaywallService.getInstance()?.let {
                        tokenResponse = tokenResponseString
                        PaywallPrefHelper.getInstance(it.context).prefMigratedTokenResponse =
                            tokenResponseString
                    }
                }

                // extract login id from a login user if it exists
                val loginIdIndex = cursor.getColumnIndex(ROW_WAPO_LOGIN_ID)
                if (loginIdIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedLoginId == null) {
                    val loginIdString = cursor.getString(loginIdIndex)
                    PaywallService.getInstance()?.let {
                        PaywallPrefHelper.getInstance(it.context).prefMigratedLoginId =
                            loginIdString
                    }
                }

                // extract secure login from a login user if it exists
                val secureLoginIdIndex = cursor.getColumnIndex(ROW_WAPO_SECURE_LOGIN_ID)
                if (secureLoginIdIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedSecureLoginId == null) {
                    val secureLoginIdString = cursor.getString(secureLoginIdIndex)
                    PaywallService.getInstance()?.let {
                        PaywallPrefHelper.getInstance(it.context).prefMigratedSecureLoginId =
                            secureLoginIdString
                    }
                }

                // extract receipt id from an iap user if it exists
                val receiptIdIndex = cursor.getColumnIndex(ROW_RECEIPT_ID)
                if (receiptIdIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedReceiptId == null) {
                    val receiptIdString = cursor.getString(receiptIdIndex)
                    PaywallService.getInstance()?.let {
                        receiptId = receiptIdString
                        PaywallPrefHelper.getInstance(it.context).prefMigratedReceiptId =
                            receiptIdString
                    }
                }

                // extract productId from an iap user if it exists
                val skuIndex = cursor.getColumnIndex(ROW_PRODUCT_ID)
                if (skuIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedSku == null) {
                    val skuString = cursor.getString(skuIndex)
                    PaywallService.getInstance()?.let {
                        sku = skuString
                        PaywallPrefHelper.getInstance(it.context).prefMigratedSku = skuString
                    }
                }

                // extract transaction date from an iap user if it exists
                val transactionDateIndex = cursor.getColumnIndex(ROW_TRANSACTION_DATE)
                if (transactionDateIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedTransactionDate == null) {
                    val transactionDateLong = cursor.getLong(transactionDateIndex)
                    PaywallService.getInstance()?.let {
                        transactionDate = transactionDateLong
                        PaywallPrefHelper.getInstance(it.context).prefMigratedTransactionDate =
                            transactionDateLong.toString()
                    }
                }

                // extract iap token from an iap user if it exists
                val iapTokenIndex = cursor.getColumnIndex(ROW_IAP_TOKEN)
                if (iapTokenIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedIapToken == null) {
                    val iapTokenString = cursor.getString(iapTokenIndex)
                    PaywallService.getInstance()?.let {
                        iapToken = iapTokenString
                        PaywallPrefHelper.getInstance(it.context).prefMigratedIapToken =
                            iapTokenString
                    }
                }

                val amazonUserIdIndex = cursor.getColumnIndex(ROW_AMAZON_USER_ID)
                if (amazonUserIdIndex != -1 && PaywallPrefHelper.getInstance(context).prefMigratedAmazonUserId == null) {
                    val amazonUserId = cursor.getString(amazonUserIdIndex)
                    PaywallService.getInstance()?.let {
                        PaywallPrefHelper.getInstance(it.context).prefMigratedAmazonUserId =
                            amazonUserId
                    }
                }

                val alertTopicsListIndex = cursor.getColumnIndex(ROW_ALERT_TOPICS_LIST)
                if (alertTopicsListIndex != -1 &&
                    !PrefUtils.getHasMigratedAlertsFromAmazonClassic(
                        context,
                    )
                ) {
                    val alertTopicsListString = cursor.getString(alertTopicsListIndex)
                    val alertTopicsList = alertTopicsListString.split(",")
                    val alertsSettings = FlagshipApplication.getInstance().alertsSettings
                    val allTopics = alertsSettings.getAlertsTopicsList()
                    allTopics.forEach {
                        alertsSettings.enableAlertsTopic(
                            it.topic.topicKey,
                            alertTopicsList.contains(it.topic.topicKey),
                        )
                    }
                    PrefUtils.setHasMigratedAlertsFromAmazonClassic(context, true)
                }

                val hasRainbowAppFlagIndex = cursor.getColumnIndex(ROW_HAS_RAINBOW_APP)
                if (hasRainbowAppFlagIndex != -1) {
                    when (cursor.getInt(hasRainbowAppFlagIndex)) {
                        1 -> {
                            PrefUtils.setHasRainbow(context, true)
                            hasRainbowApp = true
                        }
                        else -> PrefUtils.setHasRainbow(context, false)
                    }
                }
                val hasMigratedFromRainbowIndex = cursor.getColumnIndex(ROW_HAS_MIGRATED)
                if (hasMigratedFromRainbowIndex != -1) {
                    when (val hasMigratedFromRainbow = cursor.getInt(hasMigratedFromRainbowIndex)) {
                        1 -> PrefUtils.setHasMigratedFromRainbow(context, "true")
                        0 -> PrefUtils.setHasMigratedFromRainbow(context, "false")
                        else ->
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("WapoSubscriptionProviderHelper Error")
                                    setModule(LogModules.PAYWALL)
                                    setErrorMessage(
                                        "Invalid value was set for hasMigratedFromRainbow from content providers in rainbow",
                                    )
                                    set("has_migrated_from_rainbow", hasMigratedFromRainbow)
                                }.run {
                                    RemoteLog.e(context, build())
                                }
                    }
                }
                val shouldTransferUserInfoIndex =
                    cursor.getColumnIndex(
                        ROW_SHOULD_TRANSFER_USER_INFO,
                    )
                if (shouldTransferUserInfoIndex != -1) {
                    shouldTransferUserInfo = cursor.getInt(shouldTransferUserInfoIndex) == 1
                }

                val hasAmazonClassicAppIndex = cursor.getColumnIndex(ROW_HAS_AMAZON_CLASSIC_APP)
                if (hasAmazonClassicAppIndex != -1) {
                    when (cursor.getInt(hasAmazonClassicAppIndex)) {
                        1 -> {
                            PrefUtils.setHasAmazonClassic(context, true)
                            hasAmazonClassicApp = true
                        }
                        else -> PrefUtils.setHasAmazonClassic(context, false)
                    }
                }
                val hasMigratedFromAmazonClassicIndex =
                    cursor.getColumnIndex(
                        ROW_HAS_MIGRATED_AMAZON_CLASSIC,
                    )
                if (hasMigratedFromAmazonClassicIndex != -1) {
                    when (
                        val hasMigratedFromAmazonClassic =
                            cursor.getInt(
                                hasMigratedFromAmazonClassicIndex,
                            )
                    ) {
                        1 -> PrefUtils.setHasMigratedFromAmazonClassic(context, "true")
                        0 -> PrefUtils.setHasMigratedFromAmazonClassic(context, "false")
                        else ->
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("WapoSubscriptionProviderHelper Error")
                                    setModule(LogModules.PAYWALL)
                                    setErrorMessage(
                                        "Invalid value was set for hasMigratedFromAmazonClassic from content providers in amazon classic",
                                    )
                                    set(
                                        "has_migrated_from_amazon_classic",
                                        hasMigratedFromAmazonClassic,
                                    )
                                }.run {
                                    RemoteLog.e(context, build())
                                }
                    }
                }
            } else {
                EventLog
                    .Builder()
                    .apply {
                        setMessage("WapoSubscriptionProviderHelper Cursor Error")
                        setModule(LogModules.PAYWALL)
                        set("is_cursor_closed", cursor.isClosed)
                    }.run {
                        RemoteLog.e(context, build())
                    }
            }

            if (hasRainbowApp) {
                /**
                 * here we're not checking [PrefUtils.getHasMigratedFromRainbow] cause [shouldTransferUserInfo] covers it
                 */
                if (shouldTransferUserInfo && !PrefUtils.hasUserMigratedAccountFromRainbow(context)) {
                    tokenResponse?.let {
                        paywallService.migrateLoggedInUser(it)
                        PrefUtils.setUserMigratedAccountFromRainbow(context, true)
                    }
                }

                if (receiptId != null &&
                    sku != null &&
                    transactionDate != null &&
                    iapToken != null &&
                    PrefUtils
                        .getHasMigratedFromRainbow(
                            context,
                        ).equals("true") &&
                    !PrefUtils.hasUserMigratedIAPFromRainbow(context)
                ) {
                    val storeReceipt = StoreReceipt(receiptId, sku, transactionDate, null, null)
                    storeReceipt.token = iapToken
                    PaywallService.getInstance().wapoAccessServiceInstance.migrateRainbowSubscription(
                        storeReceipt,
                    )
                    PrefUtils.setUserMigratedIAPFromRainbow(context, true)
                }
            }

            if (hasAmazonClassicApp) {
                Logger.d(TAG, "hasAmazonClassic=true")
                /**
                 * here we're not checking [PrefUtils.getHasMigratedFromAmazonClassic] cause [shouldTransferUserInfo] covers it
                 */
                if (!PrefUtils.hasUserMigratedAccountFromAmazonClassic(context)) {
                    Logger.d(TAG, "migrating Amazon Classic user account")
                    tokenResponse?.let {
                        paywallService.migrateLoggedInUser(it)
                        PrefUtils.setUserMigratedAccountFromAmazonClassic(context, true)
                    }
                }

                if (receiptId != null &&
                    sku != null &&
                    transactionDate != null &&
                    !PrefUtils.hasUserMigratedIAPFromAmazonClassic(
                        context,
                    )
                ) {
                    Logger.d(TAG, "migrating Amazon Classic iap")
                    val storeReceipt = StoreReceipt(receiptId, sku, transactionDate, null, null)
                    PaywallService.getInstance().wapoAccessServiceInstance.migrateAmazonClassicSubscription(
                        storeReceipt,
                    )
                    PrefUtils.setUserMigratedIAPFromAmazonClassic(context, true)
                }
            }
        }
    }
}
