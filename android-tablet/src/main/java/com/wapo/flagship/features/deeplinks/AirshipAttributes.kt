package com.wapo.flagship.features.deeplinks

import com.wapo.android.commons.util.Logger
import com.urbanairship.UAirship
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.flagship.AppContext
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.support.ABTests
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washpost.airship.AirshipPrivacyManager.Feature
import com.washpost.airship.AirshipProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object AirshipAttributes {
    val TAG = this.javaClass.simpleName
    private const val STATUS_KEY = "user_status"
    private const val STATUS_SIGNED_IN = "|signedIn"
    private const val STATUS_SUBSCRIBER = "|subscriber"
    private const val STATUS_PREMIUM = "|premium"
    private const val STATUS_BASIC = "|basic"
    private const val STATUS_UNLINKED = "|unlinked"
    private const val STATUS_IN_GRACE_PERIOD = "|inGracePeriod"
    private const val STATUS_ON_HOLD = "|onHold"
    private const val STATUS_TERMINATED = "|terminated"
    private const val STATUS_SOURCE_WAPO_PROFILE = "|srcWapo"
    private const val STATUS_SOURCE_MIGRATED_RAINBOW = "|srcRainbow"
    private const val STATUS_SOURCE_IAP_CLASSIC = "|srcIAP"
    private const val STATUS_AUTO_RENEWAL_OFF = "|autoRenewalOff"
    private const val STATUS_FEATURES_KEY = "features"
    private const val SUB_STATUS_PAUSED = "|paused"
    private const val SUB_STATE_PAUSE_SCHEDULED = "|pauseScheduled"
    private const val IDENTITY_UUID = "identity_uuid"
    private const val SUPPORT_ID = "support_id"
    private const val SUB_EXPIRY = "subscription_expiry"

    // Attribute to track status when upgrading from "Play Store Rainbow" to the "Unified App".
    private const val STATUS_RAINBOW_MIGRATED_KEY = "rainbow_migrated"

    private val initialWait: AtomicBoolean = AtomicBoolean(false)
    var job: Job? = null

    fun getUserStatusAttributeValue(): String {
        val userStatus = StringBuilder()

        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return userStatus.toString()
        }

        PaywallService.getInstance().run {
            if (isWpUserLoggedIn) {
                userStatus.append(STATUS_SIGNED_IN)
            }

            if (wapoAccessServiceInstance.hasPremiumAccess){
                userStatus.append(STATUS_PREMIUM)
            }

            if (isPremiumUser) {
                userStatus.append(STATUS_SUBSCRIBER)

                // Add source of active subscriptions. Users may possible have 2
                // User has active IAP
                if (PaywallService.getBillingHelper()?.isSubscriptionActive == true) {
                    userStatus.append(STATUS_SOURCE_IAP_CLASSIC)
                }
                // User has migrated Rainbow IAP
                if (PaywallService.getBillingHelper()?.isMigratedRainbowSubscriptionActive == true) {
                    userStatus.append(STATUS_SOURCE_MIGRATED_RAINBOW)
                }
                // User has wapo IAP
                if (PaywallService.getInstance()?.wapoAccessServiceInstance?.isPremiumUser == true) {
                    userStatus.append(STATUS_SOURCE_WAPO_PROFILE)
                }
                if (PaywallService.getInstance().isSubscriptionPauseScheduled) {
                    userStatus.append(SUB_STATE_PAUSE_SCHEDULED)
                }
            }

            if (isSubActive && !isPremiumUser){
                userStatus.append(STATUS_BASIC)
            }

            if (!wapoAccessServiceInstance.isPremiumUser) {
                if (isSubActive) {
                    if (!isWpUserLoggedIn && verifySubUUID.isNullOrEmpty()) {
                        userStatus.append(STATUS_UNLINKED)
                    }
                    // if (isSubInGracePeriod) {
                    //    userStatus.append(STATUS_IN_GRACE_PERIOD)
                    // }
                    if (PaywallConstants.SUSPENDED == subStatus) {
                        userStatus.append(STATUS_AUTO_RENEWAL_OFF)
                    }
                } else if (PaywallService.getInstance().isSubscriptionPaused) {
                    userStatus.append(SUB_STATUS_PAUSED)
                } else {
                    // if (isSubOnHold) {
                    //    userStatus.append(STATUS_ON_HOLD)
                    // }
                    if (isSubscriptionTerminated) {
                        userStatus.append(STATUS_TERMINATED)
                        if (isSubSource(PaywallConstants.SubscriptionSource.CLASSIC_IAP)) {
                            userStatus.append(STATUS_SOURCE_IAP_CLASSIC)
                        }
                        if (isSubSource(PaywallConstants.SubscriptionSource.MIGRATED_RAINBOW)) {
                            userStatus.append(STATUS_SOURCE_MIGRATED_RAINBOW)
                        }
                        if (isSubSource(PaywallConstants.SubscriptionSource.WAPO_PROFILE)) {
                            userStatus.append(STATUS_SOURCE_WAPO_PROFILE)
                        }
                    }
                }
            }

            PrefUtils.getABParametersMap(context)?.forEach { (parameter, value) ->
                if (parameter.startsWith(ABTests.AIRSHIP_PATTERN)) {
                    val abTestId = parameter.substring(ABTests.AIRSHIP_PATTERN.length)
                    if (abTestId.isNotBlank() && value.isNotBlank()) {
                        userStatus.append("|$abTestId:$value")
                    }
                }
            }

            if (subscriptionSource != null) {
                userStatus.append("|$subscriptionSource")
            }
        }

        Logger.d(TAG, "userStatus=$userStatus")
        return userStatus.toString()
    }

    fun updateUserStatusAttribute() {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }

        val userStatus = getUserStatusAttributeValue()

        if (!initialWait.get()) {
            Logger.d(
                TAG,
                "InAppMessage, updateUserStatusAttribute(), $STATUS_KEY=$userStatus, skip update as initial wait is not finished.",
            )
            scheduleUserStatusAttributeUpdateAfter()
            return
        }

        Logger.d(TAG, "InAppMessage, updateUserStatusAttribute(), $STATUS_KEY=$userStatus")

        UAirship
            .shared()
            .channel
            .editAttributes()
            .apply {
                if (userStatus.isEmpty()) {
                    removeAttribute(STATUS_KEY)
                } else {
                    setAttribute(STATUS_KEY, userStatus)
                }
            }.apply()
    }

    fun resetInitialWait() {
        Logger.d(TAG, "InAppMessage, resetInitialWait()")
        initialWait.set(false)
        job = null
    }

    /**
     * Schedule updateUserStatusAttribute call with an initial delay to wait for paywall and Sign In
     * components to be ready with correct user status.
     */
    private fun scheduleUserStatusAttributeUpdateAfter(timeMillis: Long = 1500) {
        if (job == null) {
            job =
                GlobalScope.launch(Dispatchers.Default) {
                    delay(timeMillis)
                    withContext(Dispatchers.Main) {
                        initialWait.set(true)
                        updateUserStatusAttribute()
                    }
                }
        }
    }

    fun getFeaturesAttributeValue(): String {
        val userStatus = StringBuilder()

        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            userStatus.append("notReady")
            return userStatus.toString()
        }

        Feature.values().forEach { feature ->
            if (AirshipProvider.privacyManager.isEnabled(feature)) {
                userStatus.append("|${feature.name.lowercase(Locale.US)}")
            }
        }

        return userStatus.toString()
    }

    /**
     * Method to send Airship Features status as an attribute value to Airship with [STATUS_FEATURES_KEY] key.
     * Enabled for debug versions for now.
     */
    fun updateFeaturesAttribute() {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }

        val featuresStatus = getFeaturesAttributeValue()

        Logger.d(
            TAG,
            "InAppMessage, updateFeaturesAttribute(), $STATUS_FEATURES_KEY=$featuresStatus",
        )

        if (BuildConfig.DEBUG) {
            UAirship
                .shared()
                .channel
                .editAttributes()
                .apply {
                    if (featuresStatus.isEmpty()) {
                        removeAttribute(STATUS_FEATURES_KEY)
                    } else {
                        setAttribute(STATUS_FEATURES_KEY, featuresStatus)
                    }
                }.apply()
        }
    }

    /**
     * Method to set [STATUS_RAINBOW_MIGRATED_KEY] attribute to "true" value.
     * Do not overwrite with any subsequent negative values.
     * This method should be called for now when:
     * 1. [Utils.isProductFlavorPlayStore()] is true, means Play Store build
     * 2. [PrefUtils.getHasMigratedFromRainbow(context)] is true, means the app is upgraded from Play Store.
     * Calling from two places:
     * 1. Splash
     * 2. Once processed the common db cursor in BaseActivity.
     */
    fun updateRainbowMigratedAttribute() {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }

        Logger.d(TAG, "InAppMessage, updateRainbowMigratedAttribute()")

        UAirship
            .shared()
            .channel
            .editAttributes()
            .apply {
                setAttribute(STATUS_RAINBOW_MIGRATED_KEY, "true")
            }.apply()
    }

    /**
     * Updates the identity UUID attribute in Airship's contact attributes.
     * Functionality:
     * - First, checks if Airship (UAirship) is initialized and running. If not, exits early.
     * - Verifies if OneTrust functionality is enabled before proceeding.
     * - Retrieves the `loginId` (UUID) from `PaywallService`.
     * - Updates the `IDENTITY_UUID` attribute in Airship’s contact attributes.
     *
     * This ensures that the user's identity UUID is correctly associated with Airship’s contact management,
     * allowing for personalized notifications
     */

    fun updateIdentityUUIDAttribute() {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }
        val airshipChannel = UAirship.shared().channel.editAttributes()
        val paywallService = PaywallService.getInstance() ?: run {
            return
        }

        if (paywallService.isWpUserLoggedIn && OneTrustHelper.isFunctionalityEnabled() && OneTrustHelper.isTargetingEnabled()) {
            paywallService.loginId?.let {
                airshipChannel.setAttribute(IDENTITY_UUID, it)
            } ?: Logger.e("Airship", "Login ID is null, not setting attribute")
            AirshipProvider.getUserId()?.let {
                airshipChannel.setAttribute(SUPPORT_ID,it)
                Logger.e("Support_id", "Support_id is $it")
            } ?: Logger.e("Support_id", "Support_id is null, not setting attribute")
        } else {
            AirshipProvider.getUserId()?.let {
                airshipChannel.setAttribute(SUPPORT_ID, it)
                Logger.e("Support_id", "Support_id is $it")
                airshipChannel.removeAttribute(IDENTITY_UUID)
            }
        }
        airshipChannel.apply()
    }

    // Identifies the Airship contact using the login ID if logged in, otherwise falls back to the support ID.
    fun updateAirshipContactIdentify(context: BaseActivity) {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }
        val airshipContact = UAirship.shared().contact
        val paywallService = PaywallService.getInstance()

        if (paywallService != null && paywallService.isWpUserLoggedIn && OneTrustHelper.isFunctionalityEnabled() && OneTrustHelper.isTargetingEnabled()) {
            paywallService.loginId?.let {
                airshipContact.identify(it)
            } ?: Logger.e("Airship", "Login ID is null, not setting attribute")
        } else {
            val deviceId = DeviceUtils.getUniqueDeviceId(context)
            airshipContact.identify(deviceId)
        }
    }

    fun getIdentityUUIDAttribute(): String? {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return ""
        }
        return PaywallService.getInstance().loginId
    }

    fun updateUserSubExpirationDate() {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }
        val date: Date? = if (PaywallService.getInstance().isWpUserLoggedIn) {
            PaywallService.getInstance().getAccessExpiryDate(
                PaywallConstants.SubscriptionType.WASHPOST,
            )
        } else {
            PaywallService.getInstance().getAccessExpiryDate(
                PaywallConstants.SubscriptionType.STORE,
            )
        }

        date?.let {
            UAirship
                .shared()
                .channel
                .editAttributes()
                .apply {
                    setAttribute(SUB_EXPIRY, it)
                }.apply()
        }
    }

    // Clears specific contact-level Airship attributes once, if they haven't already been cleared
    fun clearAirshipAttributesContactLevel() {
        if (!(UAirship.isTakingOff() || UAirship.isFlying())) {
            return
        }
        if(!AppContext.getAirshipAttributeClearFlag()){
            val editor = UAirship.shared().contact.editAttributes()
            editor.removeAttribute(STATUS_FEATURES_KEY)
            editor.removeAttribute(SUPPORT_ID)
            editor.removeAttribute(SUB_EXPIRY)
            editor.removeAttribute(STATUS_KEY)
            editor.removeAttribute(IDENTITY_UUID)
            editor.apply()
            AppContext.setAirshipAttributeClearFlag(true);
        }
    }
}
