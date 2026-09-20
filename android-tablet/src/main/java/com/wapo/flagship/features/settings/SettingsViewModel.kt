package com.wapo.flagship.features.settings

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Utils
import com.wapo.android.commons.util.monthDayYearFormat
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils.isProductFlavorPlayStore
import com.wapo.flagship.features.articles2.utils.appendQueryParams
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.features.support.ABTests
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowBottomSheetViewModel.Companion.CARTA_PROFILE_LOCATION
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowBottomSheetViewModel.Companion.CARTA_PROFILE_METHOD
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.Measurement.SIGN_IN_OR_OUT_FROM_SETTINGS
import com.wapo.flagship.util.tracking.Measurement.getDefaultMap
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.WpPaywallHelper
import com.washingtonpost.android.paywall.metering.MeteringPrefs
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class SignInState {
    object SigningIn : SignInState()

    class SignedIn(
        val name: String?,
        val email: String?,
        val subscription: String?,
        val photoUrl: String?,
    ) : SignInState()

    object ConfirmSignOut : SignInState()

    class SignedOut(
        val subscription: String?,
    ) : SignInState()
}

sealed class SubscriptionState {
    object NotSubscribed : SubscriptionState()

    object Subscribed : SubscriptionState()

    object FreeDays : SubscriptionState()

    class FreeArticles(
        val remainingCount: Int,
    ) : SubscriptionState()

    object GracePeriod : SubscriptionState()

    object OnHold : SubscriptionState()

    object Terminated : SubscriptionState()

    class Paused(
        val autoResumeTime: Long,
    ) : SubscriptionState()

    class ScheduledPause(
        val pauseTime: Long,
    ) : SubscriptionState()

    object MobileFreeTrial : SubscriptionState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val newslettersRepository: NewslettersRepository,
) : ViewModel() {
    private val TAG = SettingsViewModel::class.java.simpleName
    val signInState = MutableLiveData<SignInState>()
    val subscriptionState = MutableLiveData<SubscriptionState>()
    private val subSource: String?
        get() {
            return if (!PaywallService.getConnector().paywallSubSource.isNullOrEmpty()) {
                PaywallService.getConnector().paywallSubSource
            } else {
                PaywallService.getInstance().loggedInUser?.accessPurchaseLocation
            }
        }

    private val source: String?
        get() = PaywallService.getInstance().loggedInUser?.accessPurchaseLocation

    private val config: Config
        get() = ConfigManager.getInstance().config

    /**
     * LiveEvent for to maintain view clicks count to show/hide "Test Options" preference.
     */
    private val _testOptionsToggleCount = LiveEvent<Int>()
    val testOptionsToggleCount: LiveEvent<Int> = _testOptionsToggleCount

    private val _wpWeeklyPlacement = MutableLiveData<BannerPaywallMessage?>()
    val wpWeeklyPlacement: LiveData<BannerPaywallMessage?> = _wpWeeklyPlacement

    init {
        updateStates()
        viewModelScope.launch {
            PaywallReactive.isAdFree.collect {
                updateSubscriptionState()
            }
        }
        viewModelScope.launch {
            PaywallReactive.isAdFreeRenewable.collect {
                updateSubscriptionState()
            }
        }
    }

    fun updateStates() {
        updateSubscriptionState()
        updateSignInState()
    }

    private fun updateSignInState() {
        if (!PaywallService.initialized()) {
            return
        }
        var subscription: String? = PaywallService.getConnector().prefPaywallSubShortTitle
        val state =
            if (PaywallService.getInstance().isWpUserLoggedIn) {
                val loggedInUser = PaywallService.getInstance().loggedInUser
                val displayNameList = loggedInUser.displayName?.split("|")
                val name = displayNameList?.firstOrNull()
                val email = loggedInUser.userId
                val photoUrl = loggedInUser.profilePhotoUrl
                SignInState.SignedIn(name, email, subscription, photoUrl)
            } else {
                SignInState.SignedOut(subscription)
            }
        signInState.value = state
    }

    private fun updateSubscriptionState() {
        val state =
            when {
                !PaywallService.initialized() -> SubscriptionState.NotSubscribed
                PaywallService.getInstance().isSubActive -> SubscriptionState.Subscribed
                PaywallService.getInstance().isFreeDaysUser -> SubscriptionState.FreeDays
                PaywallService.getInstance().isFreeArticlesUser ->
                    SubscriptionState.FreeArticles(
                        MeteringPrefs.getFreeArticlesRemaining(),
                    )
                PaywallService.getInstance().isMobileFreeDaysUser -> SubscriptionState.MobileFreeTrial
                PaywallService.getInstance().isPremiumUser -> SubscriptionState.Subscribed
                PaywallService.getInstance().isSubInGracePeriod -> SubscriptionState.GracePeriod
                PaywallService.getInstance().isSubOnHold -> SubscriptionState.OnHold
                PaywallService.getInstance().isSubscriptionPaused ->
                    SubscriptionState.Paused(
                        PaywallService.getConnector().autoResumeTime,
                    )
                PaywallService.getInstance().isSubscriptionPauseScheduled ->
                    SubscriptionState.ScheduledPause(
                        PaywallService.getConnector().pauseTime,
                    )
                PaywallService.getInstance().isSubscriptionTerminated -> SubscriptionState.Terminated
                else -> SubscriptionState.NotSubscribed
            }
        subscriptionState.value = state
    }

    fun startSignInProcess() {
        signInState.value = SignInState.SigningIn
    }

    fun startSignOutProcess() {
        if (signInState.value is SignInState.SignedIn) {
            signInState.value = SignInState.ConfirmSignOut
        } else if (signInState.value == SignInState.ConfirmSignOut) {
            EventLog
                .Builder()
                .apply {
                    setMessage("Settings startSignOutProcess")
                    setModule(LogModules.SETTINGS)
                }.run {
                    RemoteLog.p(application, build())
                }
            FlagshipApplication.getInstance().savedArticleManager.onLogout()
            FlagshipApplication.getInstance().followManager.onLogout()
            PaywallService.getInstance().logOutCurrentUser()
            Measurement.setSignInMedium(getDefaultMap())
            Measurement.setUUIDInDefaultMap("")
            FlagshipApplication.getInstance().updateUserTrackingForChartbeat()
            val subscription = PaywallService.getConnector().prefPaywallSubShortTitle
            updateSubscriptionState()
            signInState.value = SignInState.SignedOut(subscription)
            trackSignOutComplete()
            OneTrustHelper.initSdk(FlagshipApplication.getInstance().applicationContext)
            PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
            PreferencesSyncCoordinator.synchronize(FlagshipApplication.getInstance().applicationContext)
        }
    }

    // This function removes the test group for DAILY_READ from the stored A/B test parameters when users signs out.
    fun removeTestGroupDailyRead(activity: FragmentActivity) {
        val map = PrefUtils.getABParametersMap(activity)
        map.remove(ABTests.DAILY_READ)
        PrefUtils.saveABParametersMap(activity, map)
    }

    /**
     * Check if user is signed in through (WPAA)
     */
    fun isUserSignedIn(): Boolean = PaywallService.getInstance() != null && PaywallService.getInstance().isWpUserLoggedIn

    /**
     * Check if user has any type of subscription (IAP, WaPo, etc.)
     */
    fun isUserSubscribed(): Boolean = PaywallService.getInstance() != null && PaywallService.getInstance().isPremiumUser

    /**
     * Check if user has an IAP (Could be Playstore or Amazon)
     */
    private fun isAppStoreSub(): Boolean = PaywallService.getInstance() != null && PaywallService.getInstance().isSubActive

    /**
     * Check if user has a Playstore IAP (Could be Classic or Rainbow)
     */
    fun isPlaystoreSub(): Boolean = isAppStoreSub() && isProductFlavorPlayStore()

    /**
     * Check if user has an Classic IAP (Could be Playstore or Amazon)
     */
    private fun isClassicAppStoreSub(): Boolean =
        PaywallService.getBillingHelper() != null && PaywallService.getBillingHelper().isSubscriptionActive

    /**
     * Check if IAP is for this (Classic) Playstore App
     */
    fun isClassicAppPlaystoreSub(): Boolean = isClassicAppStoreSub() && isProductFlavorPlayStore()

    /**
     * Check if sub is from WaPo Select (Rainbow) App. Conditions for this are as follows:
     * - If Migrated Rainbow sub exists. (Pulled from Content Provider) (User does not need to be logged in)
     * - If Logged in user has a productId that is in Rainbow Sku list.
     */
    fun isSelectAppPlaystoreSub(): Boolean {
        val sku = PaywallService.getInstance()?.loggedInUser?.subSku
        val isPlaystoreSub =
            PaywallService
                .getConnector()
                ?.paywallSubSource
                ?.lowercase(Locale.US)
                ?.equals("play store") ?: false
        return PaywallService.getBillingHelper().isMigratedRainbowSubscriptionActive ||
            isRainbowSku(sku) &&
            isPlaystoreSub
    }

    /**
     * Check if sub is from Amazon Classic App. Conditions for this are as follows:
     * - If Migrated Amazon Classic sub exists. (Pulled from Content Provider) (User does not need to be logged in)
     * - If Logged in user has a productId that is in Amazon Classic Sku list.
     */
    fun isAmazonClassicSub(): Boolean {
        val sku = PaywallService.getInstance()?.loggedInUser?.subSku
        val isAmazonSub =
            PaywallService
                .getConnector()
                ?.paywallSubSource
                ?.lowercase(Locale.US)
                ?.equals("amazon store") ?: false
        return PaywallService.getBillingHelper().isMigratedAmazonClassicSubscriptionActive ||
            isAmazonClassicSku(sku) &&
            isAmazonSub
    }

    fun isFreeTrialSub(): Boolean {
        val subscriptionState = subscriptionState.value
        return subscriptionState is SubscriptionState.MobileFreeTrial ||
            subscriptionState is SubscriptionState.FreeArticles ||
            subscriptionState is SubscriptionState.FreeDays
    }

    /**
     * Check if IAP is for this Amazon App
     */
    fun isAmazonSub(): Boolean =
        isAppStoreSub() &&
            com.wapo.flagship.Utils
                .isProductFlavorAmazon()

    fun isAmazonSubOnNonAmazonDevice(): Boolean = isAmazonSub() && !Utils.isAmazonDevice

    fun getEditEmailPasswordUrl(): String = config.paywallConfig.editEmailPasswordUrl

    fun getEditNamePhotoUrl(): String = config.paywallConfig.editNamePhotoUrl

    fun getManageSubUrl(): String {
        // PaywallService.getInstance() or WpPaywallHelper.getLoggedInUser() may be null in some states.
        // Use safe calls to avoid NPEs (see FATAL exception stack trace where getLoggedInUser() was null).
        val manageSubUrl = config.paywallConfig.manageSubUrl
        // If user has ad-free, don't append sub ID so they can see all subscriptions
        if (PaywallReactive.isAdFree.value) {
            return manageSubUrl
        }
        val subscriptionId = PaywallService.getInstance()?.subscriptionID
            ?: WpPaywallHelper.getLoggedInUser()?.subscriptionId
        return if (!subscriptionId.isNullOrEmpty()) {
            appendQueryParams(manageSubUrl, mapOf("id" to subscriptionId))
        } else {
            manageSubUrl
        }
    }

    fun getSubBenefitsUrl(): String = config.paywallConfig.subBenefitsUrl

    fun getAboutMeUrl(): String = config.paywallConfig.aboutMeUrl

    /**
     * Get appropriate sub source text to be displayed on account settings ("via ...")
     */
    fun getSubSourceText(): String {
        if (!PaywallService.initialized()) {
            return ""
        }

        val hasSubSource = !subSource.isNullOrEmpty()
        val appSource =
            when {
                isAmazonClassicSub() -> "Amazon Store (Migrated)"
                isAmazonSource() -> "Amazon Store"
                isClassicAppPlaystoreSub() -> "Play Store"
                isSelectAppPlaystoreSub() -> "Select App on Play Store"
                hasSubSource -> subSource
                isDirectSource() -> "The Post"
                else -> {
                    PaywallService
                        .getConnector()
                        .logHandledException(Exception("$TAG - Failed to find App Source"))
                    ""
                }
            }

        return if (appSource.isNullOrEmpty()) "" else "via $appSource"
    }

    /**
     * Check if sub source for logged in user is "Appstore"
     */
    private fun isAmazonSource(): Boolean =
        isAmazonSub() ||
            subSource?.lowercase(Locale.getDefault()) == "appstore"

    /**
     * Check both source and  subSource for logged in user. Basically any type of sub that may be linked
     * to the account should come through.
     */
    fun isDirectSource(): Boolean =
        source?.lowercase(Locale.getDefault()) == "associate" ||
            source?.lowercase(Locale.getDefault()) == "digitaldsi" ||
            source?.lowercase(Locale.getDefault()) == "dsi" ||
            source?.lowercase(Locale.getDefault()) == "invalid" ||
            source?.lowercase(Locale.getDefault()) == "digital" ||
            source?.lowercase(Locale.getDefault()) == "device" ||
            source?.lowercase(Locale.getDefault()) == "partner" ||
            subSource?.lowercase(Locale.getDefault()) == "digital" ||
            subSource?.lowercase(Locale.getDefault()) == "home delivery" ||
            subSource?.lowercase(Locale.getDefault()) == "free" ||
            subSource?.lowercase(Locale.getDefault()) == "shared" ||
            subSource?.lowercase(Locale.getDefault()) == "partner" ||
            subSource?.lowercase(Locale.getDefault()) == "device" ||
            subSource?.lowercase(Locale.getDefault()) == "lagoon" ||
            subSource?.lowercase(Locale.getDefault()) == "app store" ||
            subSource?.lowercase(Locale.getDefault()) == "play store" ||
            subSource?.lowercase(Locale.getDefault()) == "appstore" ||
            subSource?.lowercase(Locale.getDefault()) == "amazon" ||
            subSource?.lowercase(Locale.getDefault()) == "samsung"

    /**
     * Check if the current subscription is a non-renewable product (e.g., day pass).
     */
    fun isNonRenewableSubscription(): Boolean {
        val sku = PaywallService.getBillingHelper()?.cachedSubscription()?.storeProductId
            ?: PaywallService.getInstance()?.loggedInUser?.subSku
        return PaywallUtil.isNonRenewableProduct(sku)
    }

    fun getExpiredSubscription(): String? {
        val dateText = getExpireRenewalDateFormatted()
        val subString = PaywallService.getConnector().prefPaywallSubShortTitle
       return when {
            !dateText.isNullOrEmpty() -> "$subString Expired $dateText"
           else -> null
        }

    }

    fun getFreeDaysExpiry(): String {
        val dateText = getExpireRenewalDateFormatted()
        return when {
            dateText.isNullOrEmpty() -> ""
            isPassedExpiredDate() -> "Expired $dateText"
            else -> "Free access ends $dateText"
        }
    }

    fun getFreeArticlesExpiry(remainingCount: Int): String {
        val dateText = getExpireRenewalDateFormatted()
        return when {
            dateText.isNullOrEmpty() -> ""
            isPassedExpiredDate() -> "Expired $dateText"
            else -> "Read $remainingCount free articles by $dateText"
        }
    }

    fun getMobileFreeTrialExpiry(): String {
        val dateText = getMobileFreeTrialFormatted()
        return when {
            dateText.isNullOrEmpty() -> ""
            isPassedExpiredDate() -> "Expired $dateText"
            else -> "Free access ends $dateText"
        }
    }

    fun getMobileFreeTrialFormatted(): String? {
        return if (PaywallService.getInstance().isMobileFreeDaysUser) {
            val mobileFreeTrialEndDate =
                PaywallService.getInstance().getAccessExpiryDate(
                    PaywallConstants.SubscriptionType.WASHPOST,
                )
                    ?: return null
            val formatter = SimpleDateFormat("MMMM d", Locale.US)
            return formatter.format(mobileFreeTrialEndDate)
        } else {
            null
        }
    }

    fun getRenewalDate(): String {
        val loggedInUser = PaywallService.getInstance().loggedInUser
        return if (getExpireRenewalDateFormatted().isNullOrEmpty()) {
            ""
        } else if (loggedInUser != null && !loggedInUser.isProductRenewable) {
            "Your access is valid through ${getExpireRenewalDateFormatted()}"
        } else {
            "Next payment ${getExpireRenewalDateFormatted()}"
        }
    }

    fun getPaymentErrorDate(): String? =
        if (getExpireRenewalDateFormatted().isNullOrEmpty()) null else "Payment error ${getExpireRenewalDateFormatted()}"

    /**
     * Get formatted expiration / renewal date as MM/dd/yy
     */
    fun getExpireRenewalDateFormatted(): String? {
        val date = getExpireRenewalDate()
        return if (date == null) "" else monthDayYearFormat(date)
    }

    /**
     * Get Sub renewal date or free trial expiration date.
     */
    private fun getExpireRenewalDate(): Date? {
        if (!PaywallService.initialized()) {
            return null
        }
        return PaywallService
            .getInstance()
            .getAccessExpiryDate(
                if (isAppStoreSub()) PaywallConstants.SubscriptionType.STORE else PaywallConstants.SubscriptionType.WASHPOST,
            )
    }

    /**
     * Check if Free trial is passed expired date.
     */
    private fun isPassedExpiredDate(): Boolean {
        val date = getExpireRenewalDate() ?: return false
        return Calendar.getInstance().time.after(date)
    }

    fun trackSignOutStarted() {
        Measurement.trackSignOutAttempt(SIGN_IN_OR_OUT_FROM_SETTINGS)
    }

    fun trackSignOutComplete() {
        Measurement.trackSignOutComplete(SIGN_IN_OR_OUT_FROM_SETTINGS)
    }

    /**
     * If the user has free articles we want to sync with Tetro
     * to determine the amount of free articles the user has left
     */
    fun syncWithTetro() {
        PaywallService.getInstance()?.apply {
            if (isFreeArticlesUser) {
                tetroManager?.sync(
                    abTestVariants =
                        Measurement.getABTestingVariants(
                            FlagshipApplication.getInstance().applicationContext,
                        ),
                )
            }
        }
    }

    /**
     * Method to handle App Version text Click event from [com.wapo.flagship.features.settings.preferences.AppVersionPreference]
     */
    fun handleAppVersionClick() {
        testOptionsToggleCount.value = (testOptionsToggleCount.value ?: 0) + 1
    }

    private fun isRainbowSku(sku: String?): Boolean = PaywallConstants.RAINBOW_SKU_LIST.contains(sku)

    private fun isAmazonClassicSku(sku: String?): Boolean = PaywallConstants.AMAZON_CLASSIC_SKU_LIST.contains(sku)

    fun makeTopicPrefApiCall(activity: FragmentActivity?) {
        PrefUtils.setHasSyncedTopicNotifications(activity, false)
        TopicNotificationsRepo.getInstance().syncTopicsWithPreferencesApi()
    }

    fun getAdFreeCancelBaseProductId(): String? {
        return PaywallService.getBillingHelper().cachedSubscription()?.storeProductId
    }

    fun getSubscribedAdFreeSkuForCancel(): String? {
        val paywallService = PaywallService.getInstance()
        val subscription = PaywallService.getBillingHelper().cachedSubscription() ?: return null
        val allAdFreeSkus = paywallService.adFreeSKUs
        return subscription.productSkuList?.intersect(allAdFreeSkus)?.firstOrNull()
    }

    fun shouldCancelAdFreeViaWeb(): Boolean {
        val subscriptions = PaywallService.getInstance().loggedInUser?.subscriptions ?: return false
        return PaywallUtil.hasAdFreeFromDigital(subscriptions)
    }

    fun getAdFreeManageSubUrl(): String {
        val subscriptions = PaywallService.getInstance().loggedInUser?.subscriptions
        val adFreeSubId = subscriptions?.firstOrNull { subItem ->
            PaywallReactive.isAdFreeProduct(subItem)
        }?.subscriptionId
        val manageSubUrl = config.paywallConfig.manageSubUrl
        return if (!adFreeSubId.isNullOrEmpty()) {
            appendQueryParams(manageSubUrl, mapOf("id" to adFreeSubId))
        } else {
            getManageSubUrl()
        }
    }

    fun isSubscriptionClaimed(): Boolean =
        PaywallService.getConnector()?.subscriptionLinkStatus?.equals(
            PaywallConstants.WP_API_STATUS_CLAIMED,
        ) ?: false

    fun enrollNewsletter(list: List<NewslettersKey>) {
        viewModelScope.launch {
            newslettersRepository.enrollNewsletter(
                list,
                CARTA_PROFILE_METHOD,
                CARTA_PROFILE_LOCATION,
                "",
            )
        }
    }

    fun getWpWeeklyPlacement(message: BannerPaywallMessage?) {
        _wpWeeklyPlacement.postValue(message)
    }

    fun resetWpWeeklyPlacement() {
        _wpWeeklyPlacement.postValue(null)
    }
}
