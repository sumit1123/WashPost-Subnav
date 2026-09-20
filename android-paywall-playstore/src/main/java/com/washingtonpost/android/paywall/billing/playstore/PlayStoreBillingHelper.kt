package com.washingtonpost.android.paywall.billing.playstore

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.api.VerifyState
import com.washingtonpost.android.paywall.billing.AbstractStoreBillingHelper
import com.washingtonpost.android.paywall.billing.NativePaywallListenerActivity
import com.washingtonpost.android.paywall.billing.StoreBillingHelper.InitResult
import com.washingtonpost.android.paywall.billing.StoreBillingHelper.PurchaseResult
import com.washingtonpost.android.paywall.billing.StoreBillingHelper.PurchaseResultStatus
import com.washingtonpost.android.paywall.billing.StoreBillingHelper.StoreHelperInitCallback
import com.washingtonpost.android.paywall.billing.StoreBillingHelper.StoreHelperPurchaseCallback
import com.washingtonpost.android.paywall.models.PromoPurchaseType
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt
import com.washingtonpost.android.paywall.newdata.model.Subscription
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.PLAYSTORE_SUBS_SOURCE
import kotlinx.coroutines.CoroutineScope
import com.washingtonpost.android.paywall.util.PaywallUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PlayStoreBillingHelper : AbstractStoreBillingHelper() {

    private var playBilling: PlayBillingImpl? = null
    private var subscriptionProductId: String = "monthly_all_access"
    private var selectedSubscriptionOfferId: String? = null
    private var adFreeSKUs: Set<String> = emptySet()
    private var selectedSubscriptionBasePlanId: String? = null
    private var productQueryComplete = false
    private var purchaseHistoryComplete = false

    /**
     * Called on launch of Application Start to start connection to Play Billing service.
     * Gets valid productIds from config, assigns default productId, reads subscriptions from DB, and
     * updates subscriptions status based on play store.
     */
    override fun initAndCheckSubscription(ctx: Context?, serviceConfig: ServiceConfigStub?) {
        Logger.d(TAG, "initAndCheckSubscription called on ${ctx?.javaClass?.name}")
        validSubscriptionProductIds = serviceConfig!!.validProductIdList
        subscriptionProductId = serviceConfig.productId
        adFreeSKUs = serviceConfig.adFreeSKUs

        // Load persisted ad-free status from WpUser subscriptions in DB
        val cachedUser = PaywallService.getInstance()?.loggedInUser
        if (cachedUser?.subscriptions != null) {
            PaywallUtil.updateAdFreeStatusFromSubscriptions(cachedUser.subscriptions, PaywallUtil.AdFreeSource.PROFILE)
        }

        PaywallService.getConnector()
            .breadcrumb("initAndCheckSubscription from + " + ctx?.javaClass?.name)
        readSubscriptionFromDB()
        if (playBilling == null) {
            ctx?.apply {
                Logger.i(TAG, "initAndCheckSubscription started")
                playBilling =
                    PlayBillingImpl(this, PaywallService.getConnector().billingEncryptedKey())
                playBilling?.adFreeProductIds = adFreeSKUs
                playBilling?.startClientConnection {
                    Logger.i(TAG, "initAndCheckSubscription ${it.message}")
                    initUpdateSubscriptionStatus(null)
                }
            }
        }
    }

    /**
     * Called whenever AbstractBillingActivity is created and initializes Play Billing service connection if
     * it has disconnected or has not been started. Once connected, it will update subscription status or if connection fails
     * it will send error message.
     */
    override fun init(ctx: Context?, callback: StoreHelperInitCallback?) {
        if (isInitialized) {
            Logger.i(TAG, "init is not initialized ${playBilling?.setupStatus}")
            callback?.initializedWithResult(InitResult(true, "initialized", false))
            return
        }

        ctx?.apply {
            if (playBilling == null) {
                playBilling =
                    PlayBillingImpl(this, PaywallService.getConnector().billingEncryptedKey())
                playBilling?.adFreeProductIds = adFreeSKUs
            }

            playBilling?.startClientConnection {
                Logger.i(TAG, "init playbilling connection ${it.message}")
                if (it.isFailure) {
                    Logger.i(TAG, "isStoreAccountActive ${it.message}")
                    val isAccountMissing = !isStoreAccountActive(this)
                    callback?.initializedWithResult(
                        InitResult(
                            it.isSuccess,
                            it.message,
                            isAccountMissing
                        )
                    )
                } else {
                    initUpdateSubscriptionStatus(callback)
                }
            }

        }
    }

    /**
     * Returns true if play billing service is connected
     */
    override fun isInitialized(): Boolean {
        return playBilling != null && playBilling?.setupStatus == PlayBillingImpl.SetupStatus.SETUP_DONE
    }

    /**
     * Returns true if play billing service connection has started
     */
    override fun isInitializing(): Boolean {
        return playBilling?.setupStatus == PlayBillingImpl.SetupStatus.IS_SETTING_UP
    }

    override fun removeAddOnFromSubscription(
        parent: Activity,
        baseProductId: String,
        addOnProductId: String,
        callback: StoreHelperPurchaseCallback?
    ) {
        if (baseProductId.isBlank() || addOnProductId.isBlank()) {
            callback?.purchaseFinishedWithResult(
                PurchaseResult(PurchaseResultStatus.RESULT_ERROR, "Missing base product or add-on")
            )
            return
        }

        playBilling?.launchRemoveAddOnFromExistingSubscription(
            activity = parent,
            baseProductId = baseProductId,
            addOnProductIdToRemove = addOnProductId
        ) { result, info, _ ->
            PaywallService.getConnector().logD(EventLog.Builder().setMessage("removeAddOn: response=$result"))

            if (!result.isSuccess) {
                val status = when (result.response) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResultStatus.RESULT_CANCELED
                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> PurchaseResultStatus.RESULT_INVALID_OFFER
                    else -> PurchaseResultStatus.RESULT_ERROR
                }
                callback?.purchaseFinishedWithResult(PurchaseResult(status, result.message))
                return@launchRemoveAddOnFromExistingSubscription
            }

            // Update cached subscription with the NEW purchase token from Play Store.
            // After a subscription modification, Play issues a new token; the old one
            // becomes invalid. Without this, verify will fail with "expired sub".
            if (info != null) {
                val productId = info.products.firstOrNull()
                val storeReceipt = StoreReceipt(
                    info.orderId,
                    productId,
                    info.purchaseTime,
                    null,
                    info.products
                ).apply {
                    token = info.purchaseToken
                }
                val subscription = createSubscription(storeReceipt)
                // Mark as unverified so the backend is notified of the product change
                subscription.isVerified = false
                updateSubscriptionDetails(subscription)
                lastSubscriptionOnDevice(subscription)

            } else {
                Logger.d(TAG, "removeAddOn: info is null, subscription not updated")
            }

            // Only flip ad-free renewable when the removed add-on itself is ad-free.
            if (adFreeSKUs.contains(addOnProductId)) {
                PaywallReactive.updateAdFreeRenewableStatus(false)
            }

            callback?.purchaseFinishedWithResult(
                PurchaseResult(PurchaseResultStatus.RESULT_OK, result.message)
            )
        }
    }

    /**
     * This function starts a purchase flow for add-ons with play billing service.
     * It first checks if the base product and add-on product ids are valid, then it tries to launch the purchase flow for modifying existing base subscription.
     * If the base subscription is not found on the device, it returns an error message through the callback.
     * Otherwise, it handles the purchase result in the callback.
     */
    override fun startPurchaseFlowWithAddOns(
        parent: Activity,
        baseProductId: String,
        addOnProductIds: List<String>,
        callback: StoreHelperPurchaseCallback?
    ) {
        if (baseProductId.isBlank() || addOnProductIds.isEmpty()) {
            callback?.purchaseFinishedWithResult(
                PurchaseResult(PurchaseResultStatus.RESULT_ERROR, "Missing base product or add-ons")
            )
            return
        }

        // First try the "modify existing base subscription" flow (this is required when base already owned)
        playBilling?.launchPurchaseFlowWithAddOnsForExistingBase(
            activity = parent,
            baseProductId = baseProductId,
            addOnProductIds = addOnProductIds
        ) { result, info, _ ->
            PaywallService.getConnector().logD(EventLog.Builder().setMessage("onIabPurchase(addons-modify): response=$result"))

            // If base isn't found on device, return error message
            if (!result.isSuccess &&
                result.response == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE &&
                result.message?.contains("Base subscription not found", ignoreCase = true) == true
            ) {
                callback?.purchaseFinishedWithResult(
                    PurchaseResult(PurchaseResultStatus.RESULT_ERROR, "You need to have an active subscription to purchase")
                )
            } else {
                handleAddOnPurchaseCallback(result, info, callback)
            }
        }
    }

    /**
     * Handles the purchase result for add-on purchase flow.
     * It checks if the purchase was successful, and if so, it checks if any of the purchased SKUs are ad-free SKUs.
     * If an ad-free SKU was purchased, it updates the ad-free status in PaywallReactive.
     * It then creates a StoreReceipt and Subscription object for the purchase, updates the subscription details in the database, and sets the IAP subscription status to active.
     * Finally, it calls the callback with the purchase result.
     */
    private fun handleAddOnPurchaseCallback(
        result: IabResult,
        info: Purchase?,
        callback: StoreHelperPurchaseCallback?
    ) {
        if (!result.isSuccess) {
            val status = when (result.response) {
                BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResultStatus.RESULT_CANCELED
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResultStatus.RESULT_INVALID_OFFER
                else -> PurchaseResultStatus.RESULT_ERROR
            }
            callback?.purchaseFinishedWithResult(PurchaseResult(status, result.message))
            return
        }

        // Check if the ad-free SKU was just purchased
        val purchasedSkus = info?.products ?: emptyList()

        if (adFreeSKUs.any { purchasedSkus.contains(it) }) {
            PaywallReactive.updateAdFreeFromBilling(true)
            PaywallReactive.updateAdFreeRenewableStatus(true)
        }

        val productId = info?.products?.firstOrNull()
        val storeReceipt = StoreReceipt(
            info?.orderId,
            productId,
            info?.purchaseTime,
            null,
            info?.products
        ).apply {
            token = info?.purchaseToken
        }

        val subscription = createSubscription(storeReceipt)
        // Mark as unverified so the backend is notified of the product change
        subscription.isVerified = false
        updateSubscriptionDetails(subscription)
        lastSubscriptionOnDevice(subscription)

        PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.ACTIVE)

        callback?.purchaseFinishedWithResult(
            PurchaseResult(PurchaseResultStatus.RESULT_OK, result.message)
        )
    }

    override fun startSubscriptionUpdateFlow(
        parent: Activity,
        currentProductId: String,
        targetProductId: String,
        callback: StoreHelperPurchaseCallback?
    ) {
        if (currentProductId.isBlank() || targetProductId.isBlank()) {
            callback?.purchaseFinishedWithResult(
                PurchaseResult(PurchaseResultStatus.RESULT_ERROR, "Missing current or target product IDs")
            )
            return
        }

        val replacementMode = if (NativePaywallListenerActivity.isUpgrade(currentProductId, targetProductId)) {
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
        } else {
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
        }

        playBilling?.launchSubscriptionUpdateFlow(
            activity = parent,
            currentProductId = currentProductId,
            targetProductId = targetProductId,
            selectedOfferId = selectedSubscriptionOfferId,
            replacementMode = replacementMode
        ) { result, info, _ ->
            PaywallService.getConnector()
                .logD(EventLog.Builder().setMessage("upgradeSubscription: response=$result"))

            if (!result.isSuccess) {
                val status = when (result.response) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResultStatus.RESULT_CANCELED
                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResultStatus.RESULT_INVALID_OFFER
                    else -> PurchaseResultStatus.RESULT_ERROR
                }
                callback?.purchaseFinishedWithResult(PurchaseResult(status, result.message))
                return@launchSubscriptionUpdateFlow
            }

            val purchasedSkus = info?.products ?: emptyList()

            // ensure ad free is still active if it was part of the upgrade bundle
            if (adFreeSKUs.any { purchasedSkus.contains(it) }) {
                PaywallReactive.updateAdFreeFromBilling(true)
                PaywallReactive.updateAdFreeRenewableStatus(true)
            }

            // update cached subscription with the new purchase token issued by Play after the upgrade
            val productId = info?.products?.firstOrNull()
            val storeReceipt = StoreReceipt(
                info?.orderId,
                productId,
                info?.purchaseTime,
                null,
                info?.products
            ).apply {
                token = info?.purchaseToken
            }
            val subscription = createSubscription(storeReceipt)
            subscription.isVerified = false
            updateSubscriptionDetails(subscription)
            lastSubscriptionOnDevice(subscription)

            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.ACTIVE)
            callback?.purchaseFinishedWithResult(
                PurchaseResult(PurchaseResultStatus.RESULT_OK, result.message)
            )
        }
    }

    /**
     * This function starts a purchase flow with play billing service. Usually triggered by clicking on buy from paywall
     * screen. Once the flow is complete (purchase completed, user canceled, or flow errored), the callback will be executed.
     */
    override fun startPurchaseFlow(parent: Activity, callback: StoreHelperPurchaseCallback?) {
        playBilling?.launchPurchaseFlow(
            parent,
            subscriptionProductId,
            selectedSubscriptionBasePlanId,
            selectedSubscriptionOfferId,
            BillingClient.FeatureType.SUBSCRIPTIONS,

        ) { result, info, promoPurchaseType ->
            PaywallService.getConnector().logD(EventLog.Builder().setMessage("onIabPurchase: response=$result"))
            if (!result.isSuccess) {
                val status = when(result.response) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResultStatus.RESULT_CANCELED
                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResultStatus.RESULT_INVALID_OFFER
                    else -> PurchaseResultStatus.RESULT_ERROR
                }
                callback!!.purchaseFinishedWithResult(
                    PurchaseResult(status, result.message)
                )
                return@launchPurchaseFlow
            }

            //Check if the ad-free SKU was just purchased
            val purchasedSkus = info?.products ?: emptyList()

            if (adFreeSKUs.any { purchasedSkus.contains(it) }) {
                PaywallReactive.updateAdFreeFromBilling(true)
                PaywallReactive.updateAdFreeRenewableStatus(true)
            }

            val productId = info?.products?.firstOrNull()
            val expirationDate: Long? = if (PaywallUtil.isNonRenewableProduct(productId)) {
                info?.purchaseTime?.let { purchaseTime ->
                    calculateNonRenewableExpirationDate(productId, purchaseTime)
                }
            } else {
                null
            }
            val storeReceipt = StoreReceipt(
                info?.orderId,
                productId,
                info?.purchaseTime,
                expirationDate,
                info?.products
            )
            storeReceipt.token = info?.purchaseToken
            val subscription = createSubscription(storeReceipt).also {
                it.promoCodePurchaseType = promoPurchaseType
            }
            updateSubscriptionDetails(subscription)
            lastSubscriptionOnDevice(subscription)

            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.ACTIVE)

            callback?.purchaseFinishedWithResult(
                PurchaseResult(
                    PurchaseResultStatus.RESULT_OK,
                    result.message
                )
            )
        }
    }

    override fun getUserId(): String? {
        return null
    }

    override fun getStoreAccountType(): String {
        return "com.google"
    }

    override fun setSubscriptionProductId(productId: String?) {
        productId?.apply {
            subscriptionProductId = this
        }
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        // Note : For Playstore Subscriptions, this has bee replaced with PlayBillingImpl.onPurchasesUpdated
        // which will be triggered when BillingClient.launchBillingFlow(...) completes.
        return false
    }

    override fun getSubscriptionProductId(): String {
        return subscriptionProductId
    }

    override fun setSubscriptionOfferId(offerId: String?) {
        selectedSubscriptionOfferId = offerId
    }

    override fun getSubscriptionOfferId(): String? {
        return selectedSubscriptionOfferId
    }

    override fun setSubscriptionBasePlanId(basePlanId: String?) {
        selectedSubscriptionBasePlanId = basePlanId
    }

    override fun getSubscriptionBasePlanId(): String? {
        return selectedSubscriptionBasePlanId
    }

    override fun cleanup() {
        if (playBilling?.setupStatus == PlayBillingImpl.SetupStatus.SETUP_DONE) {
            playBilling?.destroy()
        }

        playBilling = null
    }

    /**
     * During onResume of App, check if a Non-Sub or Terminated Sub user acquired a Sub and if a sub purchased add-ons. Used to check out of app purchases.
     * Also, if users sub is in Grace Period, see if user has fixed payment.
     * If user is went to the Play Store, is paused, or has a scheduled pause, make a verify call.
     */
    override fun onResume(ctx: Context) {
        if (isInitialized) {
            updateSubscriptionStatus(null)
        } else if(playBilling == null) {
            ctx.apply {
                Logger.i(TAG, "initAndCheckSubscription started")
                playBilling = PlayBillingImpl(this, PaywallService.getConnector().billingEncryptedKey())
                playBilling?.startClientConnection {
                    Logger.i(TAG, "initAndCheckSubscription ${it.message}")
                    updateSubscriptionStatus(null)
                }
            }
        }
    }

    /**
     * Acquire store subscriptions and check valid user purchases.
     */
    private fun initUpdateSubscriptionStatus(initCallback: StoreHelperInitCallback?) {
        // Check if service is connected.
        if (!isInitialized) {
            Logger.d(TAG, "Is not Initialized")
            cleanup()
            return
        }

        saveSubscriptionProducts(initCallback)
        updateSubscriptionStatus(initCallback)
    }

    /**
     * Store products as [IAPSubItems]
     */
    override fun saveSubscriptionProducts(initCallback: StoreHelperInitCallback?) {
        productQueryComplete = false
        purchaseHistoryComplete = false
        PaywallService.getConnector().breadcrumb("updateStoreSubscriptionStatusAsync")
        // Get valid productId list from config
        val productIdList: List<String> =
            ArrayList(PaywallService.getBillingHelper().validSubscriptionProductIds)

        // Query productId details for each productId in productIdList and create IAPSubItems model for each.
        // IAPSubItems will be saved in user preferences.
        playBilling?.queryProductDetailsAsync(
            productIdList,
            BillingClient.ProductType.SUBS,
            ProductDetailsResponseListener { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    EventLog.Builder().apply {
                        setMessage("Error fetching purchased items")
                        setErrorMessage("${billingResult.responseCode}, ${billingResult.debugMessage}")
                    }.run {
                        PaywallService.getConnector().logW(this)
                    }
                    return@ProductDetailsResponseListener
                }

                val productDetailsList = queryProductDetailsResult.productDetailsList
                val iapSubItems = IAPSubItems()
                productDetailsList.forEach { productDetails ->
                    if (PaywallUtil.isNonRenewableProduct(productDetails.productId)) {
                        productDetails.subscriptionOfferDetails?.forEach { offerDetails ->
                            iapSubItems.insertItem(
                                IAPPlayStoreSubItem(
                                    productDetails.productId,
                                    productDetails.title,
                                    listOf(offerDetails)
                                )
                            )
                        }
                    } else {
                        productDetails.subscriptionOfferDetails?.let { subOfferDetails ->
                            iapSubItems.insertItem(
                                IAPPlayStoreSubItem(
                                    productDetails.productId,
                                    productDetails.title,
                                    subOfferDetails
                                )
                            )
                        }
                    }
                }

                PaywallService.getConnector().saveIAPSubItems(iapSubItems)

                productQueryComplete = true
                cleanup(initCallback)
                Logger.i(TAG, "updateStoreSubscriptionStatus ${billingResult.debugMessage}")
            }
        )
    }

    /**
     * Acquire store subscriptions and check valid user purchases.
     */
    private fun updateSubscriptionStatus(initCallback: StoreHelperInitCallback?) {
        /*
            queryPurchases is now a suspend function (starting Google play billing update 4.0.0), we need to call it from a coroutine.
         */
        GlobalScope.launch(Dispatchers.Main) {

            val purchaseResults = playBilling?.queryPurchases(BillingClient.ProductType.SUBS)

            /*
                We do have couple of UI operations that are carried in this block of code. To avoid performaing any such operations
                outside of the main thread we need to force operation on main.
             */
            var currentTime: Long = -1

            Logger.i(TAG, "updateStoreSubscriptionStatus purchaseResult: ${purchaseResults.toString()}")

            purchaseResults?.purchasesList?.let { list ->
                val observedSkus = list.flatMap { it.products }.toSet()
                val hasAdFree = observedSkus.any { adFreeSKUs.contains(it) }

                PaywallReactive.updateAdFreeFromBilling(hasAdFree)

                val cachedUser = PaywallService.getInstance()?.loggedInUser
                if (cachedUser?.subscriptions != null) {
                    PaywallUtil.updateAdFreeStatusFromSubscriptions(cachedUser.subscriptions, PaywallUtil.AdFreeSource.PROFILE)
                }
            }

            // Handle active user subscription purchases
            purchaseResults?.apply {
                Logger.i(TAG, "updateStoreSubscriptionStatus purchaseResults Response: ${this.billingResult.debugMessage}")
                if (this.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    var purchase: Purchase? = null
                    PaywallService.getBillingHelper().validSubscriptionProductIds.forEach { validProductId ->
                        // Find purchase for valid productId
                        val productIdPurchase =
                            this.purchasesList.firstOrNull { it.products.firstOrNull() == validProductId }

                        // If purchase exists for valid productId create a StoreReceipt object
                        productIdPurchase?.apply {
                            val productId = this.products.firstOrNull()

                            val expirationDate: Long? = if (PaywallUtil.isNonRenewableProduct(productId)) {
                                calculateNonRenewableExpirationDate(productId, this.purchaseTime)
                            } else {
                                null
                            }

                            val storeReceipt = StoreReceipt(
                                this.orderId,
                                productId,
                                this.purchaseTime,
                                expirationDate,
                                this.products
                            )
                            storeReceipt.token = this.purchaseToken
                            val sub = createSubscription(storeReceipt)
                            Logger.d(
                                TAG,
                                "Time Now : $currentTime | expiration Time : ${sub.expirationDate}"
                            )
                            if (sub.expirationDate > currentTime) {
                                currentTime = sub.expirationDate
                                purchase = this
                                updatePlayStoreIAPStatus()
                            }
                            Logger.d(
                                "PlayBilling",
                                "id : ${this.orderId} | token : ${this.purchaseToken}"
                            )
                        }
                    }

                    // Handle active subscription (queryPurchases will not return expired, or canceled purchases)
                    if (PaywallService.getConnector().iapSubscriptionStatus == PaywallConstants.PAUSED) {
                        Logger.d(TAG, "user has a paused subscription")
                    } else if (purchase == null) {
                        // If there is NO active subscription (and there is no paused subscription), remove subscription from DB and set cached subscription to null.
                        Logger.d(TAG, "user does not have a subscription")
                        checkSubscriptionHistory(initCallback)
                        cleanSubscriptionInDb()
                        setCachedSubscription(null)
                    } else {
                        // If there is an active subscription, create a StoreReceipt object and store it in DB and cache it.
                        // This is handled in updateSubscriptionDetails
                        Logger.d(TAG, "user has a subscription")
                        val subscription =
                            PaywallService.getBillingHelper().cachedSubscription()
                        purchase?.apply {
                            if (subscription == null || subscription.receiptNumber != purchase?.orderId) {
                                val productId = this.products.firstOrNull()

                                val expirationDate: Long? = if (PaywallUtil.isNonRenewableProduct(productId)) {
                                    calculateNonRenewableExpirationDate(productId, this.purchaseTime)
                                } else {
                                    null
                                }

                                val storeReceipt = StoreReceipt(
                                    this.orderId,
                                    productId,
                                    this.purchaseTime,
                                    expirationDate,
                                    this.products
                                )
                                storeReceipt.token = this.purchaseToken
                                Logger.i(TAG, "Sub - Token : ${this.purchaseToken}")
                                val sub = createSubscription(storeReceipt)
                                updateSubscriptionInDb(sub)
                                lastSubscriptionOnDevice(sub)
                            }
                            if (subscription != null) {
                                // Update productSkuList if Play Store reports a different product list
                                // (e.g. after an expired add-on is removed at period end)
                                val playStoreProducts = this.products
                                if (subscription.productSkuList != playStoreProducts) {
                                    Logger.d(TAG, "productSkuList changed: ${subscription.productSkuList} -> $playStoreProducts")
                                    subscription.productSkuList = playStoreProducts
                                    updateSubscriptionDetails(subscription)
                                }
                                lastSubscriptionOnDevice(subscription)
                                Logger.i(TAG, "Sub - Token : ${subscription.storeUID}")
                            }
                            //Set subscription status (isAutoRenewing = true -> ACTIVE | is a flex product = true -> ACTIVE | isAutoRenewing = false -> CANCELED / GRACE_PERIOD)
                            PaywallService.getConnector().setIapSubscriptionStatus(
                                if (this.isAutoRenewing) PaywallConstants.IapSubStatus.ACTIVE
                                else if (PaywallUtil.isNonRenewableProduct(this.products.firstOrNull()))
                                    PaywallConstants.IapSubStatus.ACTIVE
                                else
                                    PaywallConstants.IapSubStatus.SUSPENDED)
                            if(!isAcknowledged) {
                                playBilling?.acknowledgePurchase(this){
                                    if (it) {
                                        PaywallService.getOmniture().trackPurchaseComplete(PaywallService.getConnector().storeType, null, null, null)
                                        setPromoCodePurchaseType(PromoPurchaseType.UNDEFINED_OUT_OF_APP)
                                        PaywallService.getInstance().dispatchVerifySub(VerifyState.NeedsVerification)
                                    }
                                }
                            }
                        }
                        purchaseHistoryComplete = true
                        cleanup(initCallback)
                    }

                    if (initCallback != null) {
                        initCallback.initializedWithResult(
                            InitResult(
                                true,
                                "subscriptions read",
                                false
                            )
                        )
                    } else {
                        Logger.i(TAG, "updateStoreSubscriptionStatus cleanup called on OK")
                    }
                } else {
                    if (initCallback != null) {
                        initCallback.initializedWithResult(
                            InitResult(
                                false,
                                "Error : Reading Subscriptions ${this.billingResult.debugMessage}",
                                false
                            )
                        )
                    } else {
                        Logger.i(TAG, "updateStoreSubscriptionStatus cleanup called on Error")
                        cleanup()
                    }
                    PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.UNKNOWN)
                }
            }
        }
    }

    private suspend fun updateSubscriptionInDb(newSub: Subscription?) {
        withContext(Dispatchers.IO) {
            super.updateSubscriptionDetails(newSub)
        }
    }

    private suspend fun cleanSubscriptionInDb() {
        withContext(Dispatchers.IO) {
            super.cleanSubscription()
        }
    }

    private fun checkSubscriptionHistory(initCallback: StoreHelperInitCallback?) {
        Logger.i(TAG, "#####PKinitAndCheckSubscription helper")

        if (PaywallConstants.TERMINATED == PaywallService.getConnector().iapSubscriptionStatus
            && PaywallService.getPaywallPrefHelper().prefLastSubExpirationDate != 0L
        ) {
            updatePlayStoreIAPStatus()
            playBilling?.queryPurchaseHistory(BillingClient.ProductType.SUBS) { billingResult, purchasesList ->
                val productIds = mutableListOf<String>()
                purchasesList?.forEach { purchase ->
                    purchase.products.forEach { product ->
                        productIds.add(product)
                    }
                }
                PaywallService.getPaywallPrefHelper().setPurchaseHistory(productIds)
            }
            purchaseHistoryComplete = true
            cleanup(initCallback)
            return
        }

        playBilling?.queryPurchaseHistory(
            BillingClient.ProductType.SUBS
        ) { billingResult, purchasesList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Logger.i(TAG, "#####PKinitAndCheckSubscription response")
                    if (!purchasesList.isNullOrEmpty()) {
                        if (purchasesList[0].products.isNotEmpty()) {
                            val token = purchasesList[0].purchaseToken
                            val productId = purchasesList[0].products[0]
                            PaywallService.getPaywallPrefHelper().prefLastSubToken = token
                            PaywallService.getPaywallPrefHelper().prefLastSubProductId = productId
                            //verify last active sub in case it is in Paused / On Hold state
                            PaywallService.getInstance()
                                .dispatchVerifySub(VerifyState.NeedsVerification)
                            Logger.d(TAG, "Dispatch to verify last active receipt")
                        }
                        updatePlayStoreIAPStatus()
                        purchaseHistoryComplete = true
                        cleanup(initCallback)
                    } else {
                        // if a user used to have a subscription -> set their status as TERMINATED
                        // otherwise, they never had a sub -> set status to NO_SUB
                        if (PaywallService.getConnector().iapSubscriptionStatus.equals(PaywallConstants.ACTIVE) ||
                            PaywallService.getConnector().iapSubscriptionStatus.equals(PaywallConstants.PAUSED) ||
                            PaywallService.getConnector().iapSubscriptionStatus.equals(PaywallConstants.SUSPENDED)) {
                            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.TERMINATED)
                        } else {
                            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.NO_SUB)
                        }
                    }
                }
            }
        }
    }

    /*
        We add the play_store_iap flag in the user status for airship if one of the following conditions meet:
        1. If user has current active subscription with Google play.
        2. If user ever had a subscription with Google play and now is terminated.
        Note that for case #2, the user may still have active subscription from elsewhere e.g. premium site subscription.
        This flag will be added irrespective of their current subscription status.
     */
    private fun updatePlayStoreIAPStatus() {
        PaywallService.getInstance().subscriptionSource = PLAYSTORE_SUBS_SOURCE
        PaywallService.getConnector().updateAirshipUserStatus()
    }

    private fun cleanup(initCallback: StoreHelperInitCallback?) {
        if (initCallback == null && productQueryComplete && purchaseHistoryComplete) {
            cleanup()
        }
    }

    companion object {
        private val TAG = PlayStoreBillingHelper::class.java.name
    }

    /**
     * Calculates the expiration date for a non-renewable product using the subscription period
     * from the IAPSubItem (sourced from Play Billing's billingPeriod, e.g. "P1D").
     */
    private fun calculateNonRenewableExpirationDate(productId: String?, purchaseTime: Long): Long? {
        if (productId == null || !PaywallUtil.isNonRenewableProduct(productId)) return null

        // Look up the composite key (sku:basePlanId) first, then fall back to productId
        val compositeKey = if (selectedSubscriptionBasePlanId != null) {
            "$productId:$selectedSubscriptionBasePlanId"
        } else {
            productId
        }
        val iapSubItem = PaywallService.getConnector().getIAPSubItems()?.getItem(compositeKey)
        val periodMillis = PaywallUtil.convertISOtoMs(iapSubItem?.subscriptionPeriod ?: "")

        return purchaseTime + (periodMillis ?: 0L)
    }
}
