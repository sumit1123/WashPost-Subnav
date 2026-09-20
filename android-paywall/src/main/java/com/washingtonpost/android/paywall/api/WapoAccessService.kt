/*
 *  Copyright (c) 2018. The Washington Post. All rights reserved.
 */
package com.washingtonpost.android.paywall.api

import com.wapo.android.commons.util.Logger
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wapo.android.commons.logs.EventLog
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.helper.WpPaywallHelper
import com.washingtonpost.android.paywall.newdata.model.PaywallResult
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt
import com.washingtonpost.android.paywall.util.PaywallConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.openid.appauth.TokenResponse
import java.text.SimpleDateFormat
import java.util.*

/**
 * Paywall user access service
 *
 * @author Bkilari
 */
class WapoAccessService {
    var df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val coroutineScope = ProcessLifecycleOwner.get().lifecycleScope
    private val mutex = Mutex()

    val isPremiumUser: Boolean
        get() {
            val loggedInUser = WpPaywallHelper.getLoggedInUser() ?: return false
            return PaywallConstants.ACTIVE == loggedInUser.subStatus || PaywallConstants.SUSPENDED == loggedInUser.subStatus || isFreeDaysUser || isMobileFreeDaysUser
        }

    val isFreeDaysUser: Boolean
        get() {
            val loggedInUser = WpPaywallHelper.getLoggedInUser() ?: return false
            return PaywallConstants.FREE_TRIAL == loggedInUser.subStatus && PaywallConstants.FREE_DAYS == loggedInUser.freeTrialSubtype
        }

    val isMobileFreeDaysUser: Boolean
        get() {
            val loggedInUser = WpPaywallHelper.getLoggedInUser() ?: return false
            return PaywallConstants.FREE_TRIAL == loggedInUser.subStatus && PaywallConstants.MOBILE_FREE_DAYS == loggedInUser.freeTrialSubtype
        }

    val isFreeArticlesUser: Boolean
        get() {
            val loggedInUser = WpPaywallHelper.getLoggedInUser() ?: return false
            return PaywallConstants.FREE_TRIAL == loggedInUser.subStatus && PaywallConstants.FREE_ARTICLES == loggedInUser.freeTrialSubtype
        }

    val hasPremiumAccess: Boolean
        get() {
            return currentSubscriptionType() == PaywallConstants.WP_PREMIUM
        }

    private fun hasValidAccess(accessLevel: String?): Boolean {
        return accessLevel != null &&
                (accessLevel == PaywallConstants.WP_PREMIUM ||
                    accessLevel == PaywallConstants.WP_BASIC ||
                    accessLevel == PaywallConstants.WP_PRODUCT_ALL ||
                    accessLevel == PaywallConstants.WP_PRODUCT_WEB ||
                    accessLevel == PaywallConstants.WP_PRODUCT_NATIONAL ||
                    accessLevel == PaywallConstants.WP_MONTHLY_PASS ||
                    accessLevel == PaywallConstants.WP_PASS ||
                    accessLevel == PaywallConstants.WP_PAYG_WEEK)
    }

    fun currentSubscriptionType(): String {
        val loggedInUser =
            WpPaywallHelper.getLoggedInUser() ?: return PaywallConstants.WP_PRODUCT_NO
        val accessLevel = loggedInUser.accessLevel
        return accessLevel ?: PaywallConstants.WP_PRODUCT_NO
    }

    val isWpUserLoggedIn: Boolean
        get() {
            val loggedInUser = WpPaywallHelper.getLoggedInUser()
            return loggedInUser != null
        }

    private fun getDate(date: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(date)
        } catch (e: Exception) {
            null
        }
    }

    val accessExpiryDate: Date?
        get() {
            var accessExpiryDate: Date? = null
            val loggedInUser = WpPaywallHelper.getLoggedInUser()
            val accessLevel = loggedInUser?.accessLevel
            if (hasValidAccess(accessLevel)) {
                try {
                    df.timeZone = TimeZone.getDefault()
                    accessExpiryDate = df.parse(
                        loggedInUser
                            .accessExpiry
                    )
                } catch (e: Exception) {
                    Logger.d(TAG, e.message ?: "no error message")
                    accessExpiryDate = null
                }
            }
            return accessExpiryDate
        }

    @Synchronized
    fun verifyFreeTrialSubscription(): PaywallResult {
        return PaywallService.getInstance().apiServiceInstance.verifyFreeTrialSubscription()
    }

    suspend fun verifyDeviceSubscriptionIfRequired(force: Boolean, requestPromocode: Boolean, isPeriodicCheck: Boolean): PaywallResult? = withContext(
        Dispatchers.IO
    ) {
        mutex.withLock {
            Logger.d(TAG, "verifyDeviceSubscriptionIfRequired force $force")
            var result: PaywallResult? = null
            if (PaywallService.getBillingHelper().canCallVerifyDeviceSubscription() || force) {

                //verify both classic and rainbow subscription if they exist
                if (PaywallService.getBillingHelper().cachedSubscription() != null
                    || PaywallConstants.TERMINATED != PaywallService.getConnector().iapSubscriptionStatus
                    || (PaywallConstants.TERMINATED == PaywallService.getConnector().iapSubscriptionStatus && PaywallService.getPaywallPrefHelper().prefLastSubExpirationDate == 0L)
                ) {
                    Logger.d(TAG, "Calling verifyDevice force=$force")
                    result = PaywallService.getInstance().apiServiceInstance.verifyDeviceSubscription(requestPromocode, isPeriodicCheck)
                    if (result.isSuccess) {
                        val subs = PaywallService.getBillingHelper().cachedSubscription()
                        subs.isVerified = true
                        PaywallService.getBillingHelper().updateSubscriptionDetails(subs)
                        if (!isPeriodicCheck) {
                            refreshUserProfile()
                        }
                        PaywallService.getInstance().dispatchVerifySub(VerifyState.Verified)
                    }
                }
                if (PaywallService.getBillingHelper().migratedRainbowSubscription != null) {
                    Logger.d(TAG, "Calling verifyDevice (Rainbow sub) force=$force")
                    result = PaywallService.getInstance().apiServiceInstance.verifyRainbowSub()
                    if (result.isSuccess) {
                        val subs = PaywallService.getBillingHelper().migratedRainbowSubscription
                        subs.isVerified = true
                        PaywallService.getBillingHelper().updateRainbowSubscription(subs)
                        if (!isPeriodicCheck) {
                            refreshUserProfile()
                        }
                        PaywallService.getInstance().dispatchVerifySub(VerifyState.Verified)
                    }
                }
            }
            if (PaywallService.getBillingHelper().migratedAmazonClassicSubscription != null && !PaywallService.getBillingHelper().migratedAmazonClassicSubscription.isDeprecated) {
                Logger.d(TAG, "Calling verifyDevice (Amazon Classic sub) force=$force")
                result = PaywallService.getInstance().apiServiceInstance.verifyAmazonClassicSub()
                if (result.isSuccess) {
                    val subs = PaywallService.getBillingHelper().migratedAmazonClassicSubscription
                    subs.isVerified = true
                    PaywallService.getBillingHelper().updateAmazonClassicSubscription(subs)
                    if (!isPeriodicCheck) {
                        refreshUserProfile()
                    }
                    PaywallService.getInstance().dispatchVerifySub(VerifyState.Verified)
                }
            }
            result
        }
    }

    private fun refreshUserProfile() {
        val accessToken = AuthHelper.getInstance(PaywallService.getInstance().context).accessToken
        val clientId = PaywallService.getConnector().clientId
        if (accessToken != null && clientId != null) {
            PaywallService.getInstance().apiServiceInstance.getUserProfile(accessToken, clientId)
        }
    }

    fun verifyDeviceSubscription(force: Boolean, requestPromocode: Boolean, isPeriodicCheck: Boolean, onComplete: (PaywallResult?) -> Unit) {
        coroutineScope.launch {
            EventLog.Builder().setMessage("/verify device call starting").run {
                PaywallService.getConnector().logD(this)
            }
            val result = verifyDeviceSubscriptionIfRequired(force, requestPromocode, isPeriodicCheck)
            onComplete(result)
        }
    }


    fun migrateRainbowSubscription(storeReceipt: StoreReceipt) {
        if (PaywallService.getBillingHelper().migratedRainbowSubscription == null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    val sub = PaywallService.getBillingHelper()
                        .createMigratedRainbowSubscription(storeReceipt)
                    PaywallService.getBillingHelper().updateRainbowSubscription(sub)
                    //automatically subscription should be ACTIVE when migrated. verify will update it later if it changes
                    PaywallService.getConnector().rainbowSubscriptionStatus = PaywallConstants.ACTIVE
                    EventLog.Builder().setMessage("Rainbow IAP migration complete").run {
                        PaywallService.getConnector().logD(this)
                    }
                }
            }
        }
    }

    fun migrateAmazonClassicSubscription(storeReceipt: StoreReceipt) {
        if (PaywallService.getBillingHelper().migratedAmazonClassicSubscription == null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    val sub = PaywallService.getBillingHelper()
                        .createMigratedAmazonClassicSubscription(storeReceipt)
                    PaywallService.getBillingHelper().updateAmazonClassicSubscription(sub)
                    //automatically subscription should be ACTIVE when migrated. verify will update it later if it changes
                    PaywallService.getConnector().amazonClassicSubscriptionStatus = PaywallConstants.ACTIVE
                    EventLog.Builder().setMessage("Amazon Classic IAP migration complete").run {
                        PaywallService.getConnector().logD(this)
                    }
                }
            }
        }
    }

    fun authenticateWithOneLinkToken(oneLinkToken: String, onComplete: (TokenResponse?) -> Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val tokenResponse = PaywallService.getInstance().apiServiceInstance.authWithOneLinkToken(oneLinkToken, PaywallService.getConnector().clientId)
                onComplete(tokenResponse)
            }
        }
    }

    companion object {
        private const val TAG = "WapoAccessService"
    }
}

sealed class VerifyState{
    var isPeriodic: Boolean = false
    object NeedsVerification : VerifyState()
    object Verified : VerifyState()
}