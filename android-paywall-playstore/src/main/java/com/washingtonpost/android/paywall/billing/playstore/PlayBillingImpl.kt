package com.washingtonpost.android.paywall.billing.playstore

import android.app.Activity
import android.content.Context
import com.wapo.android.commons.util.Logger
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogKeys
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.models.PromoPurchaseType
import com.washingtonpost.android.paywall.util.EncryptUtil
import kotlinx.coroutines.*

class PlayBillingImpl(val context: Context, val playLicenseKey: String) :
        PurchasesUpdatedListener {

    enum class SetupStatus {
        SETUP_NOT_STARTED,
        IS_SETTING_UP,
        SETUP_DONE
    }

    private var mDebugLog = false
    private var mDebugTag = "PlayBillingImpl"
    private var mSignatureBase64: String? = null
    private var mPurchaseFinished: ((IabResult, Purchase?, PromoPurchaseType?) -> Unit)? = null
    private var playBillingScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    val isSettingUp
        get() = setupStatus == SetupStatus.IS_SETTING_UP

    var adFreeProductIds = emptySet<String>()

    /**
     * Holds current status of Play Billing Services connection
     *  - Exception thrown if status goes from SETUP_DONE -> IS_SETTING_UP, SETUP_NOT_STARTED -> SETUP_DONE
     */
    var setupStatus: SetupStatus = SetupStatus.SETUP_NOT_STARTED
        set(value) {
            var updateValue = true

            when (value) {
                SetupStatus.IS_SETTING_UP -> {
                    if (setupStatus == SetupStatus.SETUP_DONE) {
                        PaywallService.getConnector().logHandledException(IllegalStateException("Billing Status cannot go from SETUP_DONE -> $value"))
                        updateValue = false
                    }
                }
                SetupStatus.SETUP_DONE -> {
                    if (setupStatus == SetupStatus.SETUP_NOT_STARTED) {
                        PaywallService.getConnector().logHandledException(IllegalStateException("Billing Status cannot go from SETUP_NOT_STARTED -> $value"))
                        updateValue = false
                    }
                }
                else -> { }
            }

            if(updateValue) {
                field = value
            }
        }

    /**
     * Setup for Billing Client
     */
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .setListener(this)
            .build()

    /**
     * Check to see if Feature is supported
     */
    private fun isFeatureSupported(type: String) = billingClient.isFeatureSupported(type).responseCode == BillingClient.BillingResponseCode.OK

    /**
     * Start connection to billing client service
     * @param callback - handle outcome of service connection
     */
    fun startClientConnection(callback: (IabResult) -> Unit?) {

        setupStatus = SetupStatus.IS_SETTING_UP
        // Connection to IAB service
        logDebug("Starting in-app billing setup.")
        try {
            mSignatureBase64 = EncryptUtil.decrypt(playLicenseKey)
        } catch (e: Exception) {
            Logger.e(mDebugTag, "License Key Failed")
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                logDebug("Billing service connected.")
                val resultMessage: String

                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        processBillingClientConfig()
                        resultMessage = if (isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS))
                            "Subscriptions AVAILABLE"
                        else
                            "Subscriptions NOT AVAILABLE"

                        setupStatus = SetupStatus.SETUP_DONE
                    }
                    else -> {
                        resultMessage = billingResult.debugMessage ?: "Billing Setup Failed"
                    }
                }

                callback(IabResult(billingResult.responseCode
                        ?: BillingClient.BillingResponseCode.ERROR, resultMessage))
            }

            override fun onBillingServiceDisconnected() {
                setupStatus = SetupStatus.SETUP_NOT_STARTED
                // Logic from ServiceConnection.onServiceDisconnected should be moved here.
            }
        })
    }

    /**
     * Attempt to call connect to Billing Client if service has been disconnected
     * @param billingResult Result code used to check if Service was disconnected
     */
    private fun attemptToReconnectService(billingResult: BillingResult?) {
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
            startClientConnection {
                logDebug("Attempt to Restart Connection resulted in ${it.response} : ${it.message}")
            }
        }
    }

    /**
     * Implementation of PurchaseUpdatedListener. Called on App load to check if user has existing purchase and
     * when user makes purchase while in the app
     * @param billingResult - Result code for whether purchase response was successful
     * @param purchaseList - list of purchases attributed to the user
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            attemptToReconnectService(billingResult)
            val iabResult = IabResult(billingResult.responseCode, billingResult.debugMessage)
            mPurchaseFinished?.invoke(iabResult, null, null)
            logError(EventLog.Builder().setErrorMessage("onPurchaseUpdated Failed because of ${billingResult.responseCode} : ${billingResult.debugMessage}"))
            return
        }

        if (purchaseList == null || purchaseList?.count() == 0) {
            val iabResult = IabResult(BillingClient.BillingResponseCode.ERROR, "No Purchases Found")
            mPurchaseFinished?.invoke(iabResult, null, null)
            logError(EventLog.Builder().setErrorMessage("No Purchases Found"))
            return
        }

        purchaseList.forEach {
            handlePurchase(it)
        }
    }

    /**
     * Based on new Google guidelines on https://developer.android.com/google/play/billing/migrate#acknowledge
     * User must acknowledge all purchases
     * @param purchase - Purchase that needs to be acknowledged
     */
    suspend fun acknowledgePurchase(purchase: Purchase, acknowledgmentResult: (successful: Boolean) -> Unit) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)

        val ackPurchaseResult = withContext(Dispatchers.IO) {
            billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
        }

        when (ackPurchaseResult.responseCode) {
            BillingResponseCode.OK -> {
                acknowledgmentResult(true)
                logDebug("Purchase Acknowledged Successfully")
            }
            else -> {
                acknowledgmentResult(false)
                logError(EventLog.Builder().setErrorMessage("Purchase Acknowledgement Error ${ackPurchaseResult.responseCode} : ${ackPurchaseResult.debugMessage}"))
            }
        }

    }

    /**
     * Handle each purchase ->
     * 1. Acknowledge purchase that are not acknowledged.
     * 2. Provide entitlements to user if they own a specific purchase
     * 3. Handle purchases that are pending or in an unspecified state
     * @param purchase - purchase being handled
     */
    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {

                // If purchase has not be acknowledge, make sure to acknowledge it.
                if (!purchase.isAcknowledged) {
                    playBillingScope.launch {
                        acknowledgePurchase(purchase){ success ->
                            if(success){
                                //it is unknown whether or not a promotion has been applied on this in-app purchase.
                                purchaseFinished(purchase, PromoPurchaseType.UNDEFINED_IN_APP)
                            }else{
                                purchaseFinished(purchase)
                            }

                        }
                    }
                }else {
                    // Verify purchase signature
                    purchaseFinished(purchase)
                }
            }
            Purchase.PurchaseState.PENDING -> {
                mPurchaseFinished?.invoke(IabResult(BillingClient.BillingResponseCode.ERROR, "Purchase is still Pending"), purchase, null)
            }
            Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                mPurchaseFinished?.invoke(IabResult(BillingClient.BillingResponseCode.ERROR, "Purchase is in unspecified state"), purchase, null)
            }
        }
    }

    private fun purchaseFinished(purchase: Purchase, promoPurchaseType: PromoPurchaseType? = null) {
        if (!Security.verifyPurchase(mSignatureBase64, purchase.originalJson, purchase.signature)) {
            logError(EventLog.Builder().setErrorMessage("Purchase signature verification FAILED for productId ${purchase.products.firstOrNull()}"))
            val result = IabResult(
                BillingClient.BillingResponseCode.ERROR,
                "Signature verification failed for productId ${purchase.products.firstOrNull()}"
            )
            mPurchaseFinished?.invoke(result, purchase, promoPurchaseType)
        } else {

            // Handle successful purchase
            mPurchaseFinished?.invoke(
                IabResult(
                    BillingClient.BillingResponseCode.OK,
                    "Purchase Successful"
                ), purchase, promoPurchaseType
            )
        }
    }

    /**
     * Close billing client connection safely
     */
    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
            playBillingScope.cancel()
        }
    }

    /**
     * Get current App offerings
     */
    fun queryProductDetailsAsync(productIdList: List<String>, type: String, listener: ProductDetailsResponseListener) {
        val productList = productIdList.map { productId ->
            Product.newBuilder().setProductId(productId).setProductType(type).build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        billingClient.queryProductDetailsAsync(params.build(), listener)
    }

    /**
     * Get current user purchases. This function has been updted to be the suspend
     * function since Google play billing library 4.0.0 introduced the [queryPurchasesAsync] and deprecated the [BillingClient.queryPurchases]
     */
    suspend fun queryPurchases(productType: String): PurchasesResult {
        return withContext(Dispatchers.IO) { billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(productType).build()) }
    }

    /**
     * Selects the base plan offer from the list of offers for a product. The base plan offer is identified by having a null offerId.
     */
    private fun selectBasePlanOffer(details: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        // Base plan is the one with offerId == null
        return details.subscriptionOfferDetails
            ?.firstOrNull { it.offerId == null }
    }

    /**
     * Finds the active purchase token for a given productId.
     * This is used to identify the existing subscription purchase when launching a purchase flow with add-ons for an existing base subscription.
     */
    private suspend fun findActivePurchaseTokenForProduct(productId: String): String? {
        val result = queryPurchases(BillingClient.ProductType.SUBS)
        if (result.billingResult.responseCode != BillingResponseCode.OK) return null

        return result.purchasesList
            .firstOrNull { purchase ->
                purchase.products.contains(productId)
            }?.purchaseToken
    }

    /**
     * Adds add-ons to an EXISTING base subscription purchase.
     *
     * Per Play Billing docs for "Subscription with add-ons" modifications:
     * - MUST set oldPurchaseToken via SubscriptionUpdateParams
     * - MUST include base item first in productDetailsParamsList
     */
    fun launchPurchaseFlowWithAddOnsForExistingBase(
        activity: Activity,
        baseProductId: String,
        addOnProductIds: List<String>,
        purchaseFinished: ((IabResult, Purchase?, PromoPurchaseType?) -> Unit)?
    ) {
        if (baseProductId.isBlank() || addOnProductIds.isEmpty()) {
            purchaseFinished?.invoke(
                IabResult(BillingResponseCode.DEVELOPER_ERROR, "Missing base product or add-ons"),
                null,
                null
            )
            return
        }

        mPurchaseFinished = purchaseFinished

        playBillingScope.launch {
            val oldPurchaseToken = findActivePurchaseTokenForProduct(baseProductId)
            if (oldPurchaseToken.isNullOrBlank()) {
                purchaseFinished?.invoke(
                    IabResult(
                        BillingResponseCode.ITEM_UNAVAILABLE,
                        "Base subscription not found on device for productId=$baseProductId"
                    ),
                    null,
                    null
                )
                return@launch
            }

            val allProductIds = listOf(baseProductId) + addOnProductIds
            val productList = allProductIds.map { productId ->
                Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

            billingClient.queryProductDetailsAsync(params.build()) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode != BillingResponseCode.OK) {
                    purchaseFinished?.invoke(
                        IabResult(billingResult.responseCode, billingResult.debugMessage),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                val productDetailsList = queryProductDetailsResult.productDetailsList ?: emptyList()
                val detailsById = productDetailsList.associateBy { it.productId }
                val baseDetails = detailsById[baseProductId]
                if (baseDetails == null) {
                    purchaseFinished?.invoke(
                        IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "Base product not found: $baseProductId"),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                val addOnDetails = addOnProductIds.mapNotNull { detailsById[it] }
                if (addOnDetails.size != addOnProductIds.size) {
                    purchaseFinished?.invoke(
                        IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "One or more add-ons not found"),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                // Base MUST be first in the list
                val orderedDetails = listOf(baseDetails) + addOnDetails

                val productDetailsParamsList = orderedDetails.map { details ->
                    val basePlanOffer = selectBasePlanOffer(details)
                    if (basePlanOffer == null) {
                        purchaseFinished?.invoke(
                            IabResult(
                                BillingResponseCode.ITEM_UNAVAILABLE,
                                "No base plan offer (offerId=null) for productId=${details.productId}"
                            ),
                            null,
                            null
                        )
                        return@queryProductDetailsAsync
                    }

                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(basePlanOffer.offerToken)
                        .build()
                }

                val updateParams =
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(oldPurchaseToken)
                        .build()

                val flowParams =
                    BillingFlowParams.newBuilder()
                        .setSubscriptionUpdateParams(updateParams)
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    /**
     * Finds the active subscription purchase that contains the given productId.
     * This is used to identify the existing subscription purchase when launching a purchase flow to remove an add-on from an existing base subscription.
     */
    private suspend fun findActiveSubscriptionPurchaseContaining(productId: String): Purchase? {
        val result = queryPurchases(BillingClient.ProductType.SUBS)
        if (result.billingResult.responseCode != BillingResponseCode.OK) return null

        return result.purchasesList.firstOrNull { purchase ->
            purchase.products.contains(productId)
        }
    }

    /**
     * Removes an add-on from an EXISTING subscription purchase.
     *
     * Per Play Billing docs for "Subscription with add-ons" modifications:
     * - MUST set oldPurchaseToken via SubscriptionUpdateParams
     * - MUST include base item first in productDetailsParamsList
     *
     * Note: Play Store does not have a direct flow to remove an add-on, but we can achieve this by launching a purchase flow with the items we want to keep (base + any remaining add-ons).
     * The user will see a confirmation screen showing the removed add-on and can confirm the change.
     */
    fun launchRemoveAddOnFromExistingSubscription(
        activity: Activity,
        baseProductId: String,
        addOnProductIdToRemove: String,
        purchaseFinished: ((IabResult, Purchase?, PromoPurchaseType?) -> Unit)?
    ) {
        mPurchaseFinished = purchaseFinished

        playBillingScope.launch {
            // Find the current active subscription purchase
            val currentPurchase = findActiveSubscriptionPurchaseContaining(baseProductId)
            if (currentPurchase == null) {
                purchaseFinished?.invoke(
                    IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "No active base purchase found for $baseProductId"),
                    null,
                    null
                )
                return@launch
            }

            val oldPurchaseToken = currentPurchase.purchaseToken
            val activeItems = currentPurchase.products

            // Compute kept items (remove the add-on)
            val keptItems = activeItems.filterNot { it == addOnProductIdToRemove }

            // If user tries to remove last remaining item (or base), block
            if (!keptItems.contains(baseProductId)) {
                purchaseFinished?.invoke(
                    IabResult(BillingResponseCode.DEVELOPER_ERROR, "Cannot remove base item via add-on removal flow"),
                    null,
                    null
                )
                return@launch
            }

            // If add-on wasn't actually present, treat as no-op / invalid
            if (keptItems.size == activeItems.size) {
                purchaseFinished?.invoke(
                    IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "Add-on not owned: $addOnProductIdToRemove"),
                    null,
                    null
                )
                return@launch
            }

            // Fetch ProductDetails for items we are keeping
            val productList = keptItems.map { productId ->
                Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

            billingClient.queryProductDetailsAsync(params.build()) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode != BillingResponseCode.OK) {
                    purchaseFinished?.invoke(
                        IabResult(billingResult.responseCode, billingResult.debugMessage),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                val productDetailsList = queryProductDetailsResult.productDetailsList ?: emptyList()
                val detailsById = productDetailsList.associateBy { it.productId }


                val keptDetails = keptItems.mapNotNull { detailsById[it] }
                if (keptDetails.size != keptItems.size) {
                    purchaseFinished?.invoke(
                        IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "Missing ProductDetails for one or more kept items"),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                val baseDetails = detailsById[baseProductId]
                if (baseDetails == null) {
                    purchaseFinished?.invoke(
                        IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "Missing base ProductDetails for $baseProductId"),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                val orderedDetails =
                    listOf(baseDetails) + keptDetails.filterNot { it.productId == baseProductId }

                val productDetailsParamsList = orderedDetails.map { details ->
                    val offer = selectBasePlanOffer(details)
                    if (offer == null) {
                        purchaseFinished?.invoke(
                            IabResult(BillingResponseCode.ITEM_UNAVAILABLE, "No base plan offer (offerId=null) for productId=${details.productId}"),
                            null,
                            null
                        )
                        return@queryProductDetailsAsync
                    }

                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        .build()
                }

                // modify existing purchase
                val updateParams =
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(oldPurchaseToken)
                        .build()

                val flowParams =
                    BillingFlowParams.newBuilder()
                        .setSubscriptionUpdateParams(updateParams)
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    /**
     * Updates an existing subscription from [currentProductId] to [targetProductId]
     * Includes any active add-ons (ad-free) in the upgrade/downgrade flow
     *
     * @param activity
     * @param currentProductId Product ID of the current subscription base plan
     * @param targetProductId Product ID of the target subscription base plan
     * @param selectedOfferId Specific offer token to be purchased
     * @param replacementMode The replacement mode for the subscription update (e.g. CHARGE_FULL_PRICE, DEFERRED)
     * @param purchaseFinished
     */
    fun launchSubscriptionUpdateFlow(
        activity: Activity,
        currentProductId: String,
        targetProductId: String,
        selectedOfferId: String?,
        replacementMode: Int = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE,
        purchaseFinished: ((IabResult, Purchase?, PromoPurchaseType?) -> Unit)?
    ) {
        if (currentProductId.isBlank() || targetProductId.isBlank()) {
            purchaseFinished?.invoke(
                IabResult(BillingResponseCode.ERROR, "Missing current or target product IDs"),
                null, null
            )
            return
        }

        mPurchaseFinished = purchaseFinished
        playBillingScope.launch {
            val activePurchase = findActiveSubscriptionPurchaseContaining(currentProductId)
            if (activePurchase == null) {
                purchaseFinished?.invoke(
                    IabResult(
                        BillingResponseCode.ITEM_UNAVAILABLE,
                        "Current subscription not found on device for productId=$currentProductId"
                    ),
                    null, null
                )
                return@launch
            }

            // retrieve purchase token for current subscription - needed to launch subscription update flow
            val oldPurchaseToken = activePurchase.purchaseToken
            // retrieve all active add on products
            val activeAddOnSkus = activePurchase.products.filter { it != currentProductId }

            val targetSkusList = (listOf(targetProductId) + activeAddOnSkus)
            val productList = targetSkusList.map { productId ->
                Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            // query product details to retrieve offer tokens - needed to launch subscription update flow
            val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
            billingClient.queryProductDetailsAsync(params.build()) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode != BillingResponseCode.OK) {
                    purchaseFinished?.invoke(
                        IabResult(billingResult.responseCode, billingResult.debugMessage),
                        null,
                        null
                    )
                    return@queryProductDetailsAsync
                }

                val productDetailsList = queryProductDetailsResult.productDetailsList ?: emptyList()
                val detailsById = productDetailsList.associateBy { it.productId }

                // retrieve details for target base plan
                val targetProductDetails = detailsById[targetProductId]
                val iapSubItem: IAPPlayStoreSubItem? =
                    targetProductDetails?.subscriptionOfferDetails?.let {
                        IAPPlayStoreSubItem(
                            targetProductDetails.productId,
                            targetProductDetails.title,
                            it
                        )
                    }
                val offerToken = iapSubItem?.getOfferToken(selectedOfferId)

                if (targetProductDetails == null || offerToken == null) {
                    purchaseFinished?.invoke(
                        IabResult(
                            BillingResponseCode.ITEM_UNAVAILABLE,
                            "No target product details or offer token found"
                        ),
                        null, null
                    )
                    return@queryProductDetailsAsync
                }

                // creates product list for any subscription modifications
                val productDetailsParamsList = mutableListOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(targetProductDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                if (activeAddOnSkus.isNotEmpty()) {
                    // ensure updates only happen when active add-ons are compatible with new base plan
                    handleActiveAddOns(
                        activeAddOnSkus,
                        detailsById
                    )?.let { addOnParams ->
                        productDetailsParamsList.addAll(addOnParams)
                        launchUpdateBillingFlow(
                            activity,
                            oldPurchaseToken,
                            productDetailsParamsList,
                            replacementMode
                        )
                    } ?: run {
                        purchaseFinished?.invoke(
                            IabResult(
                                BillingResponseCode.ITEM_UNAVAILABLE,
                                "Unable to carry over your current add-ons to the new plan. Please try again later."
                            ),
                            null, null
                        )
                    }
                } else {
                    // if there are no active add-ons, proceed with update flow with just the base plans
                    launchUpdateBillingFlow(
                        activity,
                        oldPurchaseToken,
                        productDetailsParamsList,
                        replacementMode
                    )
                }
            }
        }
    }

    fun launchUpdateBillingFlow(
        activity: Activity,
        oldPurchaseToken: String,
        productDetailsParamsList: List<BillingFlowParams.ProductDetailsParams>,
        replacementMode: Int
    ) {
        // identifies what the current subscription that is being replaced is and how the user is charged
        val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldPurchaseToken(oldPurchaseToken)
            .setSubscriptionReplacementMode(replacementMode)
            .build()

        // contains the details for the new subscription plan (base plan + add-ons) the user is updating to
        val flowParams = BillingFlowParams.newBuilder()
            .setSubscriptionUpdateParams(updateParams)
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun handleActiveAddOns(
        activeAddOnSkus: List<String>,
        detailsById: Map<String, ProductDetails>
    ): List<BillingFlowParams.ProductDetailsParams>? {
        val addOnParams = mutableListOf<BillingFlowParams.ProductDetailsParams>()

        for (addOnSku in activeAddOnSkus) {
            val addOnDetails = detailsById[addOnSku] ?: run {
                logError(EventLog.Builder().setErrorMessage(
                    "Update Subscription flow: ProductDetails not found for add-on $addOnSku — aborting upgrade"
                ))
                return null
            }

            val addOnOffer = selectBasePlanOffer(addOnDetails)
            if (addOnOffer != null) {
                addOnParams.add(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(addOnDetails)
                        .setOfferToken(addOnOffer.offerToken)
                        .build()
                )
            } else {
                logError(
                    EventLog.Builder().setErrorMessage(
                        "Upgrade flow: No base-plan offer token found for add-on $addOnSku — aborting upgrade"
                    )
                )
                return null
            }
        }

        return addOnParams
    }

    /**
     * Start a Purchase Flow for a given productId
     * @param activity - activity from which the billing flow is launched
     * @param productId - current productId being purchased
     * @param selectedOfferId - specific offer token to be purchased. if null, it will use the highest weighted offer in config
     * @param type - type of product. Required to see if type is supported on the device
     * @param purchaseFinished - call back to handle the result of the purchase.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String, basePlanId: String?, selectedOfferId: String?, type: String, purchaseFinished: ((IabResult, Purchase?, PromoPurchaseType?) -> Unit)?) {
        val featureSupportResponse = billingClient.isFeatureSupported(type).responseCode
        if (featureSupportResponse != BillingClient.BillingResponseCode.OK) {
            purchaseFinished?.invoke(IabResult(featureSupportResponse, "Feature $type is not supported because of $featureSupportResponse"), null, null)
            return
        }

        val productList = mutableListOf<Product>()
        productList.add(Product.newBuilder().setProductId(productId).setProductType(BillingClient.ProductType.SUBS).build())
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        mPurchaseFinished = purchaseFinished

        billingClient.queryProductDetailsAsync(params.build())
        { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                logError(EventLog.Builder().setErrorMessage("Purchase Flow : Failed to Query $productId with response ${billingResult.responseCode}"))
                purchaseFinished?.invoke(IabResult(billingResult.responseCode, billingResult.debugMessage), null, null)
                return@queryProductDetailsAsync
            }

            val productDetailsList = queryProductDetailsResult.productDetailsList ?: emptyList()
            val productDetails = productDetailsList.find { it.productId == productId }

            if (productDetails == null) {
                logError(EventLog.Builder().setErrorMessage("Purchase Flow : $productId does not exist in productDetailsList"))
                purchaseFinished?.invoke(IabResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, billingResult.debugMessage.ifEmpty { "Item unavailable" }), null, null)
                return@queryProductDetailsAsync
            }

            val iapSubItem: IAPPlayStoreSubItem? = productDetails.subscriptionOfferDetails?.let {
                IAPPlayStoreSubItem(productDetails.productId, productDetails.title, it)
            }

            // match by base plan id if base plan exists (for flex products), otherwise use offer id
            val offerToken = if (basePlanId != null) {
                val matchingOffers = productDetails.subscriptionOfferDetails?.filter {
                    it.basePlanId == basePlanId
                }
                matchingOffers?.firstOrNull()?.offerToken
            } else {
                iapSubItem?.getOfferToken(selectedOfferId)
            }

            if (offerToken == null && productDetails.productType == BillingClient.ProductType.SUBS) {
                logError(EventLog.Builder().setErrorMessage("Purchase Flow : null offerToken for $productId"))
                purchaseFinished?.invoke(IabResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, billingResult.debugMessage.ifEmpty { "Item unavailable" }), null, null)
                return@queryProductDetailsAsync
            }

            try {
                val productDetailsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                productDetailsBuilder.setProductDetails(productDetails)
                if (offerToken != null) productDetailsBuilder.setOfferToken(offerToken)

                val productDetailsParamsList = listOf(productDetailsBuilder.build())

                val purchaseParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, purchaseParams)
            } catch (e: Throwable) {
                purchaseFinished?.invoke(IabResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, e.message), null, null)
            }
        }
    }

    fun logDebug(msg: String?) {
        if (mDebugLog) msg?.let { Logger.d(mDebugTag, it) }
        //PaywallService.getConnector().logW(mDebugTag,mDebugTag +": " +msg);
    }

    fun logError(eventLogBuilder: EventLog.Builder?) {
        Logger.e(mDebugTag, "In-app billing error: ${eventLogBuilder?.get(LogKeys.ERROR_MESSAGE.keyName)}")
        eventLogBuilder?.let {
            it.setMessage("In-app billing error")
            PaywallService.getConnector().logW(it)
        }
    }

    fun queryPurchaseHistory(productType: String, callBack: PurchasesResponseListener) {
        val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()
        billingClient.queryPurchasesAsync(params, callBack)
    }

    fun processBillingClientConfig() {
        // Use the default GetBillingConfigParams.
        val configParams = GetBillingConfigParams.newBuilder().build()
        billingClient.getBillingConfigAsync(configParams) { billingResult, billingConfig ->
            if (billingResult.responseCode == BillingResponseCode.OK)
                // Store the `countryCode` for determining pricing codes.
                PaywallService.getConnector().billingCountryCode = billingConfig?.countryCode
        }
    }
}