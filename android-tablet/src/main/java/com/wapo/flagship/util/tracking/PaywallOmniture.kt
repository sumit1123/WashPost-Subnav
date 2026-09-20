/*
 * Copyright (C) 2014 Washington Post Android Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wapo.flagship.util.tracking

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.wapo.android.commons.appsFlyer.AppsFlyerMeasurement
import com.wapo.android.commons.appsFlyer.AppsFlyerMeasurement.trackEvent
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.util.Utils.removeCurrencySignFromPrice
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.FlagshipApplication.Companion.getInstance
import com.wapo.flagship.features.deeplinks.AirshipAttributes.updateUserStatusAttribute
import com.wapo.flagship.features.onetrust.OneTrustHelper.initSdk
import com.wapo.flagship.features.support.ABTests
import com.wapo.flagship.sdk.iterable.models.DEFAULT_CAMPAIGN_ID
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.states.AcquisitionEntranceType
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.util.tracking.states.PaywallAnalyticsEntry
import com.wapo.flagship.util.tracking.states.PaywallAnalyticsEntryPrefix
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.paywall.PaywallOmniture
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel
import com.washingtonpost.android.paywall.models.PromoPurchaseType
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems
import com.washingtonpost.android.paywall.reminder.accounthold.AccountHoldFragment
import com.washingtonpost.android.paywall.reminder.state.DialogType
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import java.util.*

/**
 * Created by muppallav on 8/20/14.
 */
class PaywallOmniture :
    Measurement(),
    PaywallOmniture {
    /**
     * Store paywall / sign-in location to be passed to verifyDevice API call (for paywall)
     * and sign-in url as query param
     */
    private var genesisAcqEntranceType: String? = null // account_location in Genesis. Contains same data as acq_entrance_type for Firebase analytics.
    private var iterableAcqEntranceType: String? = null // account_location in Iterable. Contains same data as acq_entrance_type for Firebase analytics.
    private var genesisNavigationBehavior: String? = null // account_experience in Genesis. Contains same data as navigation_behavior/miscellany for Firebase analytics.
    private var arcId: String? = null
    private var isInAppMessageOriginated: Boolean = false
    private var subStartLocationType: String? = null // contains same data as sub_start_location for firebase analytics
    private var currentMessageTrackingKind: String = "paywall"

    private fun isPaywallKind(): Boolean = currentMessageTrackingKind == "paywall"

    private fun trackEvents(
        eventType: Events,
        isRegwall: Boolean = false,
    ) {
        setMeterCount(getDefaultMap())
        setLoginSubscriptionStatus(getDefaultMap())
        val newMap = getNewMap()
        if (isInAppMessageOriginated) {
            setInAppMessageParameters(newMap)
        }
        genesisNavigationBehavior =
            getDefaultMap().getEvar(Evars.NAVIGATION_BEHAVIOR.variable) as String? ?: NavigationBehavior.UNKNOWN.value
        if (isRegwall) {
            // analytics wants to register navigation behavior in miscellany if it's a regwall.
            setMiscellany(newMap, genesisNavigationBehavior)
            setPrevEntryPoint(genesisNavigationBehavior)
        }

        getDefaultMap().setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, null)
        newMap.setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, null)
        getDefaultMap().setEvar(Evars.ENTRANCE_TYPE.variable, null)
        newMap.setEvar(Evars.ENTRANCE_TYPE.variable, null)
        getDefaultMap().setEvar(Evars.SUB_START_LOCATION.variable, null)
        newMap.setEvar(Evars.SUB_START_LOCATION.variable, null)

        genesisAcqEntranceType?.let {
            if (eventType == Events.EVENT_APPS_WALL) {
                getDefaultMap().setEvar(Evars.ENTRANCE_TYPE.variable, genesisAcqEntranceType)
                newMap.setEvar(Evars.ENTRANCE_TYPE.variable, genesisAcqEntranceType)
            } else {
                getDefaultMap().setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, genesisAcqEntranceType)
                newMap.setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, genesisAcqEntranceType)
            }
        }
        //Set entrance type for Iterable if it is available. This will be used to track the acquisition entrance type in Iterable.
        val pageName = getDefaultMap().getEvar(Evars.PAGE_NAME.variable)?.toString()
        iterableAcqEntranceType?.let {
            newMap.setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, it)
            if (!it.contains(IamMessageType.BANNER.type)) {
                newMap.setEvar(Evars.MISCELLANY.variable, "sf_$pageName")
                newMap.setEvar(Evars.NAVIGATION_BEHAVIOR.variable, "sf_$pageName")
            }
        }
        newMap.putAll(getArticleContentMap())

        trackEvent(newMap, eventType)
    }

    /**
     * Will be called from Paywall2SheetFragment when it is shown in order to track paywall events.
     */
    override fun trackPaywallBlockOverlay(
        wallType: PaywallConstants.WallType?,
        wallName: String?,
        isOverlaidPaywall: Boolean?,
        isRegWallOriginated: Boolean?,
    ) {
        isInAppMessageOriginated = wallType == PaywallConstants.WallType.IAA_WALL
        // Set genesis account_location value when paywall is shown.
        val entranceType = getPaywallEntranceType(wallType, wallName)
        this.genesisAcqEntranceType = entranceType
        val map = getDefaultMap()
        if (FollowTrackingInfo.followTracking.tabName?.lowercase(Locale.US) == "following") {
            trackUserHitsPaywallFromFollowedArticle()
        } else {
            setPageName(map, getPaywallArticle())
            setArcId(map, getPaywallArcId())
            map.setEvar(Evars.PRODUCTS.variable, null)
            map.setEvar(Evars.PROMO_CODE.variable, null)
        }

        // Homepage pageview is overriding the navigation behavior to app_open before paywall is shown
        // current workaround is to override here
        if (wallType == PaywallConstants.WallType.REMINDER_PAYWALL){
            map.setEvar(
                Evars.NAVIGATION_BEHAVIOR.variable,
                NavigationBehavior.SUBSEQUENT_ACQUISITION_MESSAGE.value
            )
        }

        val siteSection = getPreviousMap()?.getEvar(Evars.SITE_SECTION.variable)?.toString()
        val subSection = getPreviousMap()?.getEvar(Evars.SUB_SECTION.variable)?.toString()

        setTetroAttributes()
        siteSection?.apply {
            setSiteSection(map, this)
        }
        subSection?.apply {
            setSubsection(map, this)
        }

        var events = Events.EVENT_APPS_WALL
        var appsflyerEvent = AppsFlyerMeasurement.PAYWALL_OVERLAY
        if (!isWallTypeRegwall(wallType)) {
            events = Events.EVENT_PAY_BLOCK_OVERLAY
        } else if (isWallTypeRegwall(wallType) && isOverlaidPaywall != true) {
            events = Events.EVENT_REGWALL_BLOCK_OVERLAY
            appsflyerEvent = AppsFlyerMeasurement.REGWALL_OVERLAY
        } else if (wallType == PaywallConstants.WallType.PAUSEWALL) {
            events = Events.EVENT_APPS_WALL
            appsflyerEvent = AppsFlyerMeasurement.PAUSEWALL_OVERLAY
        }
        if (wallType == PaywallConstants.WallType.ATP_SOFTWALL) {
            setPageName(getDefaultMap(), PAGE_FRONT_PREFIX + ASK_THE_POST)
        }
        trackEvents(events, isRegwall = false)
        trackEvent(FlagshipApplication.getInstance(), appsflyerEvent, null)
    }

    override fun trackPaywallBlockOverlay(
        wallType: PaywallConstants.WallType?,
        wallName: String?,
    ) {
        trackPaywallBlockOverlay(wallType, wallName, false, false)
    }

    override fun trackPaywallBlockOverlay(paywallType: PaywallConstants.WallType?) {
        trackPaywallBlockOverlay(paywallType, null)
    }

    private fun setTetroAttributes() {
        getArticleWeight()?.apply {
            setTetroAttributes(this)
        }
    }

    private fun getArticleWeight(): Float? = getPreviousMap()?.getEvar(Evars.TETRO_CONTENT_WEIGHT.variable)?.toString()?.toFloat()

    /**
     * Get Paywall acq_entrance_type value for given [PaywallType]
     */
    private fun getPaywallEntranceType(
        paywallType: PaywallConstants.WallType?,
        wallName: String? = null,
        isAcquisition: Boolean = true,
    ): String {
        if (!isAcquisition) {
            return AcquisitionEntranceType.NONE.value
        }
        val result = when (paywallType) {
            PaywallConstants.WallType.ONBOARDING_PAYWALL, PaywallConstants.WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL,
            PaywallConstants.WallType.ARTICLE_DEEP_LINK_PAYWALL, PaywallConstants.WallType.ARTICLE_LINKS_PAYWALL,
            PaywallConstants.WallType.WEBVIEW_PAYWALL, PaywallConstants.WallType.WEBVIEW_PRODUCT_PAGE,
            PaywallConstants.WallType.WIDGET_SMALL_PAYWALL, PaywallConstants.WallType.WIDGET_PAYWALL,
            PaywallConstants.WallType.REMINDER_PAYWALL, PaywallConstants.WallType.BOTTOM_CTA_PAYWALL,
            PaywallConstants.WallType.IAA_WALL, PaywallConstants.WallType.DEFAULT_DEEP_LINK_PAYWALL,
            PaywallConstants.WallType.ONELINK_WALL, PaywallConstants.WallType.METERED_PAYWALL,
            PaywallConstants.WallType.NAMED_PAYWALL, PaywallConstants.WallType.PAYWALL_TILE,
            PaywallConstants.WallType.ATP_SOFTWALL
                -> {
                "${PaywallAnalyticsEntryPrefix.PAYWALL.value}_${
                    wallName?.replace("-", "_") ?: PaywallAnalyticsEntry.MAIN.value
                }"
            }

            PaywallConstants.WallType.GIFT_INVALID_PAYWALL, PaywallConstants.WallType.GIFT_EXPIRED_PAYWALL,
            PaywallConstants.WallType.GIFT_SENDER_ACTION_BUTTONS, PaywallConstants.WallType.BOTTOM_CTA_GIFT_PAYWALL,
            PaywallConstants.WallType.GIFT_SENDER_NO_SUB,
                -> AcquisitionEntranceType.GIFT_PAYWALL.value

            PaywallConstants.WallType.SAVE_REGWALL -> AcquisitionEntranceType.SAVE_REGWALL.value
            PaywallConstants.WallType.SAVE_RECIPE_REGWALL -> AcquisitionEntranceType.SAVE_RECIPE_REGWALL.value
            PaywallConstants.WallType.REGWALL, PaywallConstants.WallType.REGWALL_TILE -> {
                "${PaywallAnalyticsEntryPrefix.REGWALL.value}_${
                    wallName?.replace("-", "_") ?: PaywallAnalyticsEntry.REGWALL.value
                }"
            }

            PaywallConstants.WallType.AUDIO_CAROUSEL_PAYWALL, PaywallConstants.WallType.AUDIO_ACTION_BUTTON_PAYWALL -> AcquisitionEntranceType.AUDIO_CAROUSEL_REGWALL.value
            PaywallConstants.WallType.PAUSEWALL -> AcquisitionEntranceType.PAUSE_WALL.value
            PaywallConstants.WallType.SOFTWALL ->
                "${PaywallAnalyticsEntryPrefix.REGWALL_SOFT.value}_${
                    wallName?.replace("-", "_") ?: PaywallAnalyticsEntry.REGWALL.value
                }"
            // Paywall Entrance type should NOT be unknown. If it is, ask analytics team for requirements.
            else -> AcquisitionEntranceType.UNKNOWN.value
        }
        return result
    }

    /**
     * Use this to get Sign-In intent with added [PaywallOrSignInEntryPoint] for Paywall2SheetFragment
     */
    override fun trackSignIn(
        paywallType: PaywallConstants.WallType?,
        wallName: String?,
        isAcquisition: Boolean,
        campaignEntranceType: String?
    ) {
        if (paywallType == PaywallConstants.WallType.REMINDER_PAYWALL){
            getDefaultMap().setEvar(
                Evars.NAVIGATION_BEHAVIOR.variable,
                NavigationBehavior.SUBSEQUENT_ACQUISITION_MESSAGE.value
            )
        }
        if (paywallType == PaywallConstants.WallType.ATP_SOFTWALL) {
            val map = getDefaultMap()
            map.setEvar(Evars.NAVIGATION_BEHAVIOR.variable, wallName)
            map.setEvar(Evars.PAGE_NAME.variable, PAGE_FRONT_PREFIX + ASK_THE_POST)
            map.setEvar(Evars.APP_SECTION.variable, ASK_THE_POST)
        }
        if (paywallType == PaywallConstants.WallType.ONBOARDING_PAYWALL) {
            val map = getDefaultMap()
            map.setEvar(Evars.NAVIGATION_BEHAVIOR.variable, NavigationBehavior.ONBOARDING.value)
        }
        val entranceType =
            getPaywallEntranceType(
                paywallType,
                wallName,
                isAcquisition = isAcquisition,
            )
        this.genesisAcqEntranceType = entranceType
        this.iterableAcqEntranceType = campaignEntranceType
        // TODO isRegwall should come from the caller.
        trackEvents(Events.EVENT_CLICK_SIGNIN_ATTEMPT, true)
    }

    /**
     * Use this to get Register intent with added [PaywallOrSignInEntryPoint] for Paywall2SheetFragment
     */
    override fun trackSignUp(
        paywallType: PaywallConstants.WallType,
        wallName: String?,
    ) {
        val entranceType = getPaywallEntranceType(paywallType, wallName)
        this.genesisAcqEntranceType = entranceType
        trackEvents(Events.EVENT_REGWALL_REGISTRATION, true)
    }

    override fun trackBuyWithStore(
        paywallType: PaywallConstants.WallType,
        storeType: String,
        campaignEntranceType: String?,
    ) {
        if (storeType.isBlank()) {
            return
        }
        iterableAcqEntranceType = campaignEntranceType
        isInAppMessageOriginated = paywallType == PaywallConstants.WallType.IAA_WALL
        setPageName(getDefaultMap(), getPaywallArticle())
        val subscriptionProductId = PaywallService.getBillingHelper().subscriptionProductId
        if (subscriptionProductId.isNullOrBlank()) return
        setTetroAttributes()
        getDefaultMap().setEvar(Evars.STORE_TYPE.variable, storeType)
        getDefaultMap().setEvar(Evars.PRODUCTS.variable, getProductName(subscriptionProductId))
        if (isPaywallKind()) {
            trackEvents(Events.EVENT_PAYWALL_CREATE_PAYMENT)
        } else {
            trackWallEvents(Events.EVENT_PRODUCT_SELECTED)
        }
    }

    override fun trackOpenExternalPurchase(contentUrl: String?) {
        val map = getNewMap()
        setLoginSubscriptionStatus(map)
        map.setEvar(Evars.CONTENT_URL.variable, contentUrl)
        map.setEvar(Evars.SUB_START_LOCATION.variable, "apps_wall_external_tile")
        trackEvents(map, Events.EVENT_WALL_OPEN.key)
    }

    override fun trackPurchaseComplete(
        storeType: String,
        attributionInfo: AttributionInfo?,
        campaignName: String?,
        offerType: String?,
    ) {
        val subscriptionProductId = PaywallService.getBillingHelper().subscriptionProductId
        val offerId = PaywallService.getBillingHelper().subscriptionOfferId
        val basePlanId = PaywallService.getBillingHelper().subscriptionBasePlanId
        val compositeId = if (!subscriptionProductId.isNullOrBlank() && !basePlanId.isNullOrBlank()) {
            "$subscriptionProductId:$basePlanId"
        } else {
            subscriptionProductId
        }
        if (compositeId.isBlank()) return
        val iapSubItems = PaywallService.getConnector().iapSubItems
        val iapSubItem = iapSubItems.getItem(compositeId)
        var subscriptionPrice =
            IAPSubItems.getFallBackPrice(
                compositeId,
                getInstance().applicationContext
            )
        setTetroAttributes()

        setPageName(getDefaultMap(), getPaywallArticle())
        iapSubItem?.apply {
            if (basePrice.isNullOrEmpty()) {
                PaywallService.getConnector().logHandledException(
                    Exception("Price for $productId returned as $basePrice"),
                )
            } else {
                subscriptionPrice = basePrice
            }
            getDefaultMap().setEvar(Evars.PRODUCTS.variable, getProductName(compositeId))
            Measurement.setPushNotificationId(
                getDefaultMap(),
                (if (attributionInfo?.campaignId == DEFAULT_CAMPAIGN_ID) attributionInfo.messageId else attributionInfo?.campaignId).toString()
            )
            if (isPaywallKind()) {
                trackEvents(Events.EVENT_PAY_PAYMENT_SUCCESS)
            } else {
                trackWallEvents(Events.EVENT_PRODUCT_PURCHASED)
            }
            val eventValue: MutableMap<String, Any> = HashMap()
            eventValue[AppsFlyerMeasurement.PAYWALL_PURCHASE_REVENUE] =
                removeCurrencySignFromPrice(
                    subscriptionPrice,
                )
            eventValue[AppsFlyerMeasurement.PAYWALL_PURCHASE_CURRENCY] = iapSubItem.currencyCode ?: ""
            eventValue[AppsFlyerMeasurement.PAYWALL_PURCHASE_CONTENT] = iapSubItem.title ?: ""
            eventValue[AppsFlyerMeasurement.PAYWALL_PURCHASE_CONTENT_ID] = iapSubItem.productId ?: ""
            trackEvent(
                FlagshipApplication.getInstance(),
                AppsFlyerMeasurement.PAYWALL_PURCHASE,
                eventValue,
            )
        }
        val firebaseAnalytics = getFirebaseAnalytics()
        if (firebaseAnalytics != null) {
            val bundle = Bundle()
            bundle.putString(
                FirebaseAnalytics.Param.PRICE,
                removeCurrencySignFromPrice(subscriptionPrice),
            )
            bundle.putString(
                FirebaseAnalytics.Param.CURRENCY,
                Currency.getInstance("USD").toString(),
            )
            bundle.putString(
                FirebaseAnalytics.Param.ITEM_NAME,
                PRODUCT_IDS_TO_PRODUCT_NAMES_MAP[compositeId],
            )
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
        }
        //subscriptionProductId
        //subscriptionPrice
        trackMessageEvent(
            eventType = PaywallSheet2ViewModel.EventType.PURCHASE_COMPLETE_EVENT,
            attributionInfo = attributionInfo,
            productId = compositeId,
            offerId = offerId,
            iapSubItem = iapSubItem,
            url = null,
            campaignName = campaignName,
            offerType = offerType,
        )
    }

    override fun setMeterValue(value: String) {}

    override fun trackAccountHoldEvent(
        context: Context?,
        accountHoldType: AccountHoldFragment.AccountHoldType?,
    ) {
        var accountType: String? = getAccountType(accountHoldType)
        getDefaultMap().setEvar(Evars.ACCOUNTHOLD_EVENT_LABEL.variable, accountType)
        trackEvents(Events.EVENT_ACCOUNTHOLD_NOTIFICATION_DISPLAY)
    }

    override fun trackAccountHoldPayment(accounthold: AccountHoldFragment.AccountHoldType?) {
        var accountType: String? = getAccountType(accounthold)
        getDefaultMap().setEvar(Evars.ACCOUNTHOLD_EVENT_LABEL.variable, accountType)
        trackEvents(Events.EVENT_ACCOUNTHOLD_NOTIFICATION_UPDATE_PAYMENT)
    }

    private fun getAccountType(accountHoldType: AccountHoldFragment.AccountHoldType?): String =
        when (accountHoldType) {
            AccountHoldFragment.AccountHoldType.GLOBAL_SCREEN_DISPLAY -> "global_account_hold_button"
            AccountHoldFragment.AccountHoldType.SETTINGS_DISPLAY -> "settings_account_hold"
            AccountHoldFragment.AccountHoldType.ARTICLE_SCREEEN -> "article_account_hold_button"
            AccountHoldFragment.AccountHoldType.SECTION_DISPLAY -> "direct_account_hold_message"
            else -> ""
        }

    override fun trackAccountHoldDismiss(accounthold: AccountHoldFragment.AccountHoldType?) {
        var accountType: String? = getAccountType(accounthold)
        getDefaultMap().setEvar(Evars.ACCOUNTHOLD_EVENT_LABEL.variable, accountType)
        trackEvents(Events.EVENT_ACCOUNTHOLD_CLOSE)
    }

    override fun trackSignInComplete(accountWasCreated: Boolean, isRegwall: Boolean) {
        setSignInMedium(getDefaultMap())
        val pageName = getDefaultMap().getEvar(Evars.PAGE_NAME.variable)?.toString()
        iterableAcqEntranceType?.let {
            getDefaultMap().setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, it)
            if (!it.contains("banner")) {
                getDefaultMap().setEvar(Evars.MISCELLANY.variable, "sf_$pageName")
                getDefaultMap().setEvar(Evars.NAVIGATION_BEHAVIOR.variable, "sf_$pageName")
            }
        }
        if (accountWasCreated) {
            trackEvents(Events.EVENT_REG_REGISTER_SUCCESS, true)
            // TODO Change AppsFlyerMeasurement.PAYWALL_LOGIN_SUCCESS to AppsFlyerMeasurement.PAYWALL_REGISTER_SUCCESS after iOS is ready
            trackEvent(getInstance(), AppsFlyerMeasurement.PAYWALL_REGISTER_SUCCESS, null)
        } else {
            trackEvents(Events.EVENT_REG_SIGN_IN_SUCCESS, true)
            trackEvent(getInstance(), AppsFlyerMeasurement.PAYWALL_LOGIN_SUCCESS, null)
        }
        initSdk(getInstance().applicationContext)
        PaywallService.getInstance().makeSaveIdentityPreferencesCallIfConditionsAreMet()
        updateUserStatusAttribute()
    }

    override fun trackPromoCodeRedeemedEvent(promoPurchaseType: PromoPurchaseType) {
        val measurementMap = getNewMap()
        measurementMap.setEvar(Evars.PROMO_PURCHASE_INFO.variable, promoPurchaseType.type)
        trackEvents(measurementMap, Events.PROMO_PURCHASE.key)
    }

    override fun trackWallProfileResume(contentUrl: String) {
        val map = getNewMap()
        setLoginSubscriptionStatus(map)
        map.setEvar(Evars.ENTRANCE_TYPE.variable, NavigationBehavior.PAUSEWALL.value)
        map.setEvar(Evars.CONTENT_URL.variable, contentUrl)
        trackEvents(map, Events.EVENT_WALL_OPEN.key)
    }

    override fun trackBannerProfileResume(
        navigationBehavior: String,
        contentUrl: String,
    ) {
        val map = getNewMap()
        setLoginSubscriptionStatus(map)
        if (navigationBehavior == NavigationBehavior.GLOBAL_SUBSCRIBE_BUTTON.value) {
            setPageName(map, PAGE_HOMEPAGE)
        } else if (navigationBehavior == NavigationBehavior.SETTINGS.value) {
            setPageName(map, PAGE_SETTINGS)
        }
        map.setEvar(Evars.NAVIGATION_BEHAVIOR.variable, navigationBehavior)
        map.setEvar(Evars.CONTENT_URL.variable, contentUrl)
        trackEvents(map, Events.EVENT_PROFILE_RESUME.key)
    }

    override fun getPricingTestVariant(): String? {
        val override = PrefUtils.getPricingVariantOverride(getInstance().applicationContext)
        return if (override != null) {
            override
        } else {
            val variantMap = PrefUtils.getABParametersMap(getInstance().applicationContext)

            // TODO can remove july2025PatternValue after 8/21/25
            val july2025PatternValue = variantMap[ABTests.PRICING]

            val iapPatternKey = variantMap.keys.firstOrNull { it.startsWith(ABTests.IAP_PATTERN) }
            val iapPatternValue = iapPatternKey?.let { variantMap[it] }

            iapPatternValue ?: july2025PatternValue
        }
    }

    override fun trackMessageEvent(
        eventType: PaywallSheet2ViewModel.EventType?,
        attributionInfo: AttributionInfo?,
        productId: String?,
        offerId: String?,
        iapSubItem: IAPSubItem?,
        url: String?
    ) {
        trackMessageEvent(
            eventType = eventType,
            attributionInfo = attributionInfo,
            productId = productId,
            offerId = offerId,
            iapSubItem = iapSubItem,
            url = url,
            campaignName = null,
            offerType = null,
        )
    }

    private fun trackMessageEvent(
        eventType: PaywallSheet2ViewModel.EventType?,
        attributionInfo: AttributionInfo?,
        productId: String?,
        offerId: String?,
        iapSubItem: IAPSubItem?,
        url: String?,
        campaignName: String?,
        offerType: String?,
    ) {
        attributionInfo ?: return
        val analyticsHandler = getInstance().iterableSdk.getAnalyticsHandler() ?: return
        when (eventType) {
            PaywallSheet2ViewModel.EventType.START_IMPRESSION_EVENT -> {
                analyticsHandler.startImpression(attributionInfo)
            }

            PaywallSheet2ViewModel.EventType.PAUSE_IMPRESSION_EVENT -> {
                analyticsHandler.pauseImpression(attributionInfo)
            }

            PaywallSheet2ViewModel.EventType.PURCHASE_EVENT -> {
                analyticsHandler.trackClick(
                    attributionInfo,
                    "${MESSAGE_EVENT_LINK_PREFIX}purchase"
                )
            }

            PaywallSheet2ViewModel.EventType.PRODUCT_PURCHASE_EVENT -> {
                analyticsHandler.trackClick(
                    attributionInfo,
                    "${MESSAGE_EVENT_LINK_PREFIX}purchase/$productId"
                )
            }

            PaywallSheet2ViewModel.EventType.OFFER_PURCHASE_EVENT -> {
                analyticsHandler.trackClick(
                    attributionInfo,
                    "${MESSAGE_EVENT_LINK_PREFIX}purchase/$productId/$offerId"
                )
            }

            PaywallSheet2ViewModel.EventType.URL_EVENT -> {
                analyticsHandler.trackClick(attributionInfo, url)
            }

            PaywallSheet2ViewModel.EventType.SIGN_IN_EVENT -> {
                analyticsHandler.trackClick(
                    attributionInfo, "${MESSAGE_EVENT_LINK_PREFIX}signIn"
                )
            }

            PaywallSheet2ViewModel.EventType.PURCHASE_COMPLETE_EVENT -> {
                analyticsHandler.trackPurchase(
                    attributionInfo = attributionInfo,
                    productId?.let { getProductName(it) },
                    iapSubItem?.title,
                    productId,
                    iapSubItem?.basePrice,
                    iapSubItem?.offers?.find { it.offerId == offerId }?.offerPrice,
                    iapSubItem?.currencyCode,
                    PaywallUtil.getBannerOfferText(productId, offerId),
                    campaignName,
                    offerType,
                )
            }

            PaywallSheet2ViewModel.EventType.DISMISS_EVENT -> {
                analyticsHandler.trackInAppDismissClickEvent(attributionInfo)
            }

            else -> {}
        }
    }

    override fun getGenesisLocation(): String = genesisAcqEntranceType ?: NavigationBehavior.UNKNOWN.value

    override fun getSubStartLocation(): String = subStartLocationType ?: ""

    override fun getGenesisExperience(): String = genesisNavigationBehavior ?: NavigationBehavior.UNKNOWN.value

    override fun getSignInEntranceType(dialogType: DialogType): String =
        when (dialogType) {
            DialogType.REMINDER -> NavigationBehavior.ACQUISITION_MESSAGE.value
            DialogType.ACQUISITION -> NavigationBehavior.SUBSEQUENT_ACQUISITION_MESSAGE.value
            else -> NavigationBehavior.UNKNOWN.value
        }

    override fun getTestGroup(): String = getABTestGroup()

    override fun getArcId(): String = arcId ?: ""

    fun setArcId(arcId: String?) {
        this.arcId = arcId
    }

    private fun isWallTypeRegwall(wallType: PaywallConstants.WallType?): Boolean {
        if (wallType == null) return false
        return wallType == PaywallConstants.WallType.REGWALL ||
            wallType == PaywallConstants.WallType.AUDIO_CAROUSEL_PAYWALL ||
            wallType == PaywallConstants.WallType.AUDIO_ACTION_BUTTON_PAYWALL ||
            wallType == PaywallConstants.WallType.SOFTWALL ||
            wallType == PaywallConstants.WallType.SAVE_REGWALL ||
            wallType == PaywallConstants.WallType.SAVE_RECIPE_REGWALL||
            wallType == PaywallConstants.WallType.REGWALL_TILE
    }

    private fun getProductName(productId: String): String? {
        var productName: String? = null
        if (!productId.isBlank()) {
            productName = PRODUCT_IDS_TO_PRODUCT_NAMES_MAP[productId]
            if (productName == null) {
                val adFreeSKUs = PaywallService.getInstance()?.adFreeSKUs
                if (adFreeSKUs?.contains(productId) == true) {
                    val baseSku = PaywallService.getInstance()?.resolveBaseProductId()
                    val baseName = baseSku?.let { PRODUCT_IDS_TO_PRODUCT_NAMES_MAP[it] }
                    productName = if (!baseName.isNullOrBlank()) "${baseName}-adfree" else "adfree"
                }
            }
        }
        return productName
    }

    private fun setEntranceType(
        map: MeasurementMap,
        entranceType: String,
    ) {
        map.setEvar(Evars.ENTRANCE_TYPE.variable, entranceType)
    }

    /**
     * Fires a wall/upgrade purchase event (product_selected or product_purchased) using
     * [subStartLocationType] as the sub_start_location property.
     *
     * Explicitly clears acq_entrance_type and entrance_type so the two location fields
     * are always mutually exclusive in any given event.
     */
    private fun trackWallEvents(eventType: Events) {
        setMeterCount(getDefaultMap())
        setLoginSubscriptionStatus(getDefaultMap())
        val newMap = getNewMap()
        if (isInAppMessageOriginated) {
            setInAppMessageParameters(newMap)
        }

        // Clear both acquisition fields; wall events only carry sub_start_location.
        getDefaultMap().setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, null)
        newMap.setEvar(Evars.ACQ_ENTRANCE_TYPE.variable, null)
        getDefaultMap().setEvar(Evars.ENTRANCE_TYPE.variable, null)
        newMap.setEvar(Evars.ENTRANCE_TYPE.variable, null)
        getDefaultMap().setEvar(Evars.SUB_START_LOCATION.variable, null)
        newMap.setEvar(Evars.SUB_START_LOCATION.variable, null)

        subStartLocationType?.let {
            getDefaultMap().setEvar(Evars.SUB_START_LOCATION.variable, it)
            newMap.setEvar(Evars.SUB_START_LOCATION.variable, it)
        }

        newMap.putAll(getArticleContentMap())
        trackEvent(newMap, eventType)
    }

    override fun setMessageTrackingKind(kind: String?) {
        currentMessageTrackingKind = kind ?: "paywall"
    }

    override fun setSubStartLocationType(location: String?) {
        subStartLocationType = location
    }

    override fun setGenesisAcqEntranceType(value: String) {
        genesisAcqEntranceType = value
    }

    companion object {
        val PRODUCT_IDS_TO_PRODUCT_NAMES_MAP =
            hashMapOf(
                "wp.classic.basic" to "basic-monthly",
                "wp.classic.basic.annual" to "basic-annual",
                "monthly_all_access" to "premium-monthly",
                "wp.classic.premium.annual" to "premium-annual",
                "wp.classic.flex:one-day-pass-1" to "flex-oneday-3.99",
                "wp.classic.flex:one-day-pass-2" to "flex-oneday-9.99",
                "wp.classic.flex:one-day-pass-3" to "flex-oneday-13.99",
            )
        const val EPAPER = "epaper"
        const val MESSAGE_EVENT_LINK_PREFIX = "washpost:/subs/"
    }
}
