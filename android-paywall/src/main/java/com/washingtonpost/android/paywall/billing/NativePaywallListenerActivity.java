package com.washingtonpost.android.paywall.billing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.domain.repository.LoadRenderMetrics;
import com.wapo.android.domain.repository.LoadRenderMetricsEvent;
import com.wapo.android.remotelog.logger.EventTimerLogRepoImpl;
import com.wapo.android.remotelog.logger.LoadRenderMetricsImpl;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.R;
import com.washingtonpost.android.paywall.features.ftc.ComposeDialogFragment;
import com.washingtonpost.android.paywall.newdata.model.IAPOfferItem;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem;
import com.washingtonpost.android.paywall.reminder.ReminderScreenSharedPreferenceStorage;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.util.PaywallUtil;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import kotlin.Unit;

/**
 * Created by elamgodilj on 8/30/17.
 */

public class NativePaywallListenerActivity extends AbstractBillingActivity {

    public final static String ARG_SOURCE_TYPE = "SOURCE_TYPE";
    public final static String ARG_CAMPAIGN_ENTRANCE_TYPE = "CAMPAIGN_ENTRANCE_TYPE";
    public final static String PRODUCT_ID_TO_PURCHASE = "product_id_to_purchase";
    public final static String BASE_PLAN_ID_TO_PURCHASE = "base_plan_id_to_purchase";
    public final static String OFFER_ID_TO_PURCHASE = "offer_id_to_purchase";
    public final static String BASE_PRODUCT_ID_TO_PURCHASE = "base_product_id_to_purchase";
    public final static String ADD_ON_PRODUCT_IDS_TO_PURCHASE = "add_on_product_ids_to_purchase";
    public final static String ACTION_TYPE = "action_type";
    public final static String ACTION_REMOVE_ADDON = "remove_addon";
    public final static String ACTION_UPGRADE = "upgrade_subscription";
    public final static String ACTION_DOWNGRADE = "downgrade_subscription";
    public final static String ADD_ON_PRODUCT_ID_TO_REMOVE = "add_on_product_id_to_remove";
    public final static String CURRENT_PRODUCT_ID = "current_product_id";
    public final static String PAYWALL_KIND = "paywall";
    public final static String MESSAGE_TRACKING_KIND = "message_tracking_kind";
    public final static String MESSAGE_TRACKING_CAMPAIGN_NAME = "message_tracking_campaign_name";
    public final static String MESSAGE_TRACKING_OFFER_TYPE = "message_tracking_offer_type";
    public final static String SUBSCRIPTION_UPGRADE_LOCATION = "subscription_upgrade_location";
    protected static final String TAG = NativePaywallListenerActivity.class.getSimpleName();
    private boolean purchaseCompleted = false;
    private String productId;
    private String basePlanId;
    private String offerId;
    private boolean isIAMOriginated;
    private String baseProductId;
    private ArrayList<String> addOnProductIds;
    private String addOnProductIdToRemove;
    private String actionType;
    private String currentProductId;
    private String messageTrackingKind;
    private String subscriptionUpgradeLocation;
    private LoadRenderMetrics loadRenderMetrics;

    private int resultCode;
    private ProgressDialog progressDialog;

    public enum SourceType {
        IAA,
        UNKNOWN
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadRenderMetrics = new LoadRenderMetricsImpl(new EventTimerLogRepoImpl(this));
        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.PAYWALL_RENDER_EVENT);
        Intent intent = getIntent();
        if (intent != null) {
            actionType = intent.getStringExtra(ACTION_TYPE);
            addOnProductIdToRemove = intent.getStringExtra(ADD_ON_PRODUCT_ID_TO_REMOVE);
            baseProductId = intent.getStringExtra(BASE_PRODUCT_ID_TO_PURCHASE);
            addOnProductIds = intent.getStringArrayListExtra(ADD_ON_PRODUCT_IDS_TO_PURCHASE);
            productId = intent.getStringExtra(PRODUCT_ID_TO_PURCHASE);
            basePlanId = intent.getStringExtra(BASE_PLAN_ID_TO_PURCHASE);
            offerId = intent.getStringExtra(OFFER_ID_TO_PURCHASE);
            currentProductId = intent.getStringExtra(CURRENT_PRODUCT_ID);
            isIAMOriginated = Objects.equals(intent.getStringExtra(ARG_SOURCE_TYPE), SourceType.IAA.name());
            messageTrackingKind = intent.getStringExtra(MESSAGE_TRACKING_KIND);
            subscriptionUpgradeLocation = intent.getStringExtra(SUBSCRIPTION_UPGRADE_LOCATION);

            // Decompose composite keys (e.g. "wp.classic.flex:one-day-pass-1") into
            // separate productId and basePlanId when basePlanId was not explicitly provided.
            // Some callers (deeplinks, WebView JS interface) pass the composite key from
            // mapProductNameToProductId() as the productId, which causes Play Store to
            // return "Item unavailable" since the actual SKU is only the part before ":".
            if (productId != null && TextUtils.isEmpty(basePlanId) && productId.contains(":")) {
                String[] parts = productId.split(":", 2);
                productId = parts[0];
                basePlanId = TextUtils.isEmpty(parts[1]) ? null : parts[1];
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void showErrorFragment(int responseCode, String errMsg) {
        runOnUiThread(() -> {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_error_toast, (ViewGroup) findViewById(R.id.toast_error_root));
            TextView toastTxtView = (TextView) layout.findViewById(R.id.pw_error_toast);
            toastTxtView.setText(errMsg);

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
            setActivityResult(responseCode);
            finish();
        });
    }

    @Override
    public void onSkipNowSelectedOnPaywall() {
        setActivityResult(ResponseCode.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void initBillingFinished() {
        if (loadRenderMetrics != null) {
            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.PAYWALL_RENDER_EVENT);
        }
        purchaseSubscription(PaywallService.getConnector().getStoreType());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (purchaseCompleted) {
            setActivityResult(ResponseCode.RESULT_OK);
            finish();
        }
    }

    public static Intent getPurchaseWithAddOnsIntent(
            Context context,
            String baseProductId,
            java.util.ArrayList<String> addOnProductIds,
            Bundle bundle
    ) {
        Intent intent = new Intent(context, NativePaywallListenerActivity.class);
        intent.putExtra(BASE_PRODUCT_ID_TO_PURCHASE, baseProductId);
        intent.putStringArrayListExtra(ADD_ON_PRODUCT_IDS_TO_PURCHASE, addOnProductIds);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return intent;
    }

    public static Intent getRemoveAddOnIntent(
            Context context,
            String baseProductId,
            String addOnProductIdToRemove,
            Bundle bundle
    ) {
        Intent intent = new Intent(context, NativePaywallListenerActivity.class);
        intent.putExtra(ACTION_TYPE, ACTION_REMOVE_ADDON);
        intent.putExtra(BASE_PRODUCT_ID_TO_PURCHASE, baseProductId);
        intent.putExtra(ADD_ON_PRODUCT_ID_TO_REMOVE, addOnProductIdToRemove);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return intent;
    }

    public static Intent getSubscriptionUpdateIntent(
            Context context,
            String currentProductId,
            String targetProductId,
            Bundle bundle
    ) {
        Intent intent = new Intent(context, NativePaywallListenerActivity.class);
        if (isUpgrade(currentProductId, targetProductId)) {
            intent.putExtra(ACTION_TYPE, ACTION_UPGRADE);
        } else {
            intent.putExtra(ACTION_TYPE, ACTION_DOWNGRADE);
        }
        intent.putExtra(CURRENT_PRODUCT_ID, currentProductId);
        intent.putExtra(PRODUCT_ID_TO_PURCHASE, targetProductId);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return intent;
    }

    public static boolean isUpgrade(String currentProductId, String targetProductId) {
        // determines upgrades as a basic subscription -> premium subscription (no crossgrades)
        boolean isCurrentPremium = PaywallUtil.isPremiumProduct(currentProductId);
        boolean isTargetPremium = PaywallUtil.isPremiumProduct(targetProductId);

        return !isCurrentPremium && isTargetPremium;
    }

    public static boolean isDowngrade(String currentProductId, String targetProductId) {
        // determines downgrades as a premium subscription -> basic subscription (no crossgrades)
        boolean isCurrentPremium = PaywallUtil.isPremiumProduct(currentProductId);
        boolean isTargetPremium = PaywallUtil.isPremiumProduct(targetProductId);

        return isCurrentPremium && !isTargetPremium;
    }

    public static Intent getPurchaseAddOnsOnlyIntent(
            Context context,
            ArrayList<String> addOnProductIds,
            Bundle bundle
    ) {
        Intent intent = new Intent(context, NativePaywallListenerActivity.class);
        intent.putStringArrayListExtra(ADD_ON_PRODUCT_IDS_TO_PURCHASE, addOnProductIds);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return intent;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (loadRenderMetrics != null) {
            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.PAYWALL_RENDER_EVENT);
            loadRenderMetrics = null;
        }
    }

    @Override
    public void onPurchaseComplete() {
        purchaseCompleted = true;
        applyMessageTrackingStateToOmniture();
        // Note: Commenting out the consent storage logic for now, will be replaced with actual implementation later
//        FTCConsentStorage.INSTANCE.updateStatus(
//                this,
//                ConsentStatus.COMPLETED,
//                new Continuation<Unit>() {
//                    @Override
//                    public void resumeWith(@NonNull Object o) {
//                        // Handle completion if needed
//                        ConsentData storedConsent = FTCConsentStorage.INSTANCE.getStoredConsent(NativePaywallListenerActivity.this);
//                        LogUtil.d("FTCConsent", String.format("Status updated to COMPLETED - terms: %s, timestamp: %d, status: %s",
//                                storedConsent != null ? storedConsent.getTermsText() : "null",
//                                storedConsent != null ? storedConsent.getTimestamp() : -1,
//                                storedConsent != null ? storedConsent.getStatus() : "null"
//                        ));
//                    }
//
//                    @NonNull
//                    @Override
//                    public CoroutineContext getContext() {
//                        return EmptyCoroutineContext.INSTANCE;
//                    }
//                }
//        );
        ReminderScreenSharedPreferenceStorage.Companion.getInstance(getApplicationContext()).clearStorage();
        super.onPurchaseComplete();
    }

    public void purchaseSubscription(String msg) {

        if (ACTION_REMOVE_ADDON.equals(actionType)
                && !TextUtils.isEmpty(baseProductId)
                && !TextUtils.isEmpty(addOnProductIdToRemove)) {
            startRemoveAddOnFlow(baseProductId, addOnProductIdToRemove);
            return;
        }

        PaywallConstants.WallType wallType = isIAMOriginated ?
                PaywallConstants.WallType.IAA_WALL :
                PaywallConstants.WallType.DEFAULT_DEEP_LINK_PAYWALL;

        if ((ACTION_UPGRADE.equals(actionType) || ACTION_DOWNGRADE.equals(actionType))
                && !TextUtils.isEmpty(currentProductId)
                && !TextUtils.isEmpty(productId)) {
            applyMessageTrackingStateToOmniture();
            PaywallService.getBillingHelper().setSubscriptionProductId(productId);
            PaywallService.getOmniture().trackBuyWithStore(
                    wallType,
                    PaywallService.getConnector().getStoreType(),
                    null
            );
            startUpdateSubscriptionFlow(currentProductId, productId);
            return;
        }

        if (!TextUtils.isEmpty(baseProductId) && addOnProductIds != null && !addOnProductIds.isEmpty()) {
            // product_selected for add-on purchase (also bypasses startPurchase).
            applyMessageTrackingStateToOmniture();
            PaywallService.getBillingHelper().setSubscriptionProductId(addOnProductIds.get(0));
            PaywallService.getBillingHelper().setSubscriptionBasePlanId(null);
            PaywallService.getBillingHelper().setSubscriptionOfferId(null);
            PaywallService.getOmniture().trackBuyWithStore(
                    wallType,
                    PaywallService.getConnector().getStoreType(),
                    null
            );
            startPurchaseFlowWithAddOns(baseProductId, addOnProductIds);
            return;
        }

        // If no base product was provided but add-ons were requested,
        // resolve the base product from the user's current subscription.
        if (TextUtils.isEmpty(baseProductId) && addOnProductIds != null && !addOnProductIds.isEmpty()) {
            String resolvedBaseProductId = PaywallService.getInstance().resolveBaseProductId();
            if (!TextUtils.isEmpty(resolvedBaseProductId)) {
                applyMessageTrackingStateToOmniture();
                PaywallService.getBillingHelper().setSubscriptionProductId(addOnProductIds.get(0));
                PaywallService.getBillingHelper().setSubscriptionBasePlanId(null);
                PaywallService.getBillingHelper().setSubscriptionOfferId(null);
                PaywallService.getOmniture().trackBuyWithStore(
                        wallType,
                        PaywallService.getConnector().getStoreType(),
                        null
                );
                startPurchaseFlowWithAddOns(resolvedBaseProductId, addOnProductIds);
                return;
            }
        }

        if (!TextUtils.isEmpty(productId)) {
            PaywallService.getBillingHelper().setSubscriptionProductId(productId);
            PaywallService.getBillingHelper().setSubscriptionBasePlanId(basePlanId);
            PaywallService.getBillingHelper().setSubscriptionOfferId(offerId);
        }

        IAPSubItem iapSubItem = null;
        String text = null;

        if (PaywallService.getConnector() != null && productId != null) {
            // if basePlanId is present, look for composite key, otherwise look for productId key
            String compositeKey = !TextUtils.isEmpty(basePlanId) ? (productId + ":" + basePlanId) : productId;
            iapSubItem = PaywallService.getConnector().getIAPSubItems().getItem(compositeKey);
        }
        if (iapSubItem!=null) {
            text = getDialogText(iapSubItem);
        }
        if (PaywallConstants.IS_FTC_VISIBLE) {
            ComposeDialogFragment dialogFragment = ComposeDialogFragment.Companion.newInstance(
                    text,
                    () -> {
                        startPurchase(msg);
                        return Unit.INSTANCE;
                    },
                    () -> {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                        return Unit.INSTANCE;
                    }
            );
            dialogFragment.show(getSupportFragmentManager(), "purchase_dialog");
        } else {
            startPurchase(msg);
        }
    }


    @Override
    void verifyDeviceSub() {
        purchaseCompleted = true;
        // Fire verify in the background. PaywallService.verifyDeviceSubscription
        // (vs. WapoAccessService directly) ensures prefVerifyDeviceSubLastTime is
        // updated and PaywallReactive.notifyVerifyComplete fires — the latter is
        // consumed by MainActivity to re-run showNextDialog and surface the CA
        // settlement dialog.
        PaywallService.getInstance().verifyDeviceSubscription(false, false, false);
        PaywallService.getConnector().onSubscriptionItemChanged(true);
        setActivityResult(ResponseCode.RESULT_OK);
        finish();
    }

    private void startPurchase(String msg) {
        PaywallService.getConnector().logD(new EventLog.Builder()
                .setMessage("appBridgeAndroid called: arg: " + msg));
        PaywallConstants.WallType wallType = isIAMOriginated ?
                PaywallConstants.WallType.IAA_WALL :
                PaywallConstants.WallType.DEFAULT_DEEP_LINK_PAYWALL;
        applyMessageTrackingStateToOmniture();
        String campaignEntranceType = getIntent().getStringExtra(ARG_CAMPAIGN_ENTRANCE_TYPE);
        PaywallService.getOmniture().trackBuyWithStore(wallType, msg, campaignEntranceType);

        if (billingInitResult != null) {
            if (billingInitResult.isSuccessfull()) {
                startPurchaseFlow();
            } else if (billingInitResult.isAccountMissing()) {
                PaywallService.getConnector().logD(new EventLog.Builder()
                        .setMessage("Starting Add Account Flow"));
                startAddAccountFlow();
            } else {
                PaywallService.getConnector().logD(new EventLog.Builder()
                        .setMessage("Starting Purchase Flow"));
                startPurchaseFlow();
            }
        }
    }


    public void setActivityResult(int code) {
        setResult(code, getIntent());
        resultCode = code;
    }

    private void applyMessageTrackingStateToOmniture() {
        if (messageTrackingKind == null && subscriptionUpgradeLocation == null) {
            return;
        }
        PaywallService.getOmniture().setMessageTrackingKind(messageTrackingKind);
        if (PAYWALL_KIND.equals(messageTrackingKind)) {
            // for iterable messageTracking kind: paywall, the dynamic constructed location overrides genesisAcqEntranceType so that
            // acq_entrance_type = apps_<placement>_<campaign> instead of the wall-type default.
            if (!TextUtils.isEmpty(subscriptionUpgradeLocation)) {
                PaywallService.getOmniture().setGenesisAcqEntranceType(subscriptionUpgradeLocation);
            }
            PaywallService.getOmniture().setSubStartLocationType(null);
        } else {
            // for iterable messageTracking kind: wall, location becomes sub_start_location for analytics and
            // subscription_upgrade_location for verify.
            PaywallService.getOmniture().setSubStartLocationType(subscriptionUpgradeLocation);
        }
    }


    @Override
    public void finish() {
        Intent intent = getIntent();
        long requestCode = intent.getLongExtra(PaywallConstants.REQUEST_CODE, -1);
        NativePaywallResultCallbacks callback = NativePaywallListenerResultCallbackManager.INSTANCE.getCallback(requestCode);
        if (callback != null) {
            callback.handleActivityResponse(this, resultCode);
            NativePaywallListenerResultCallbackManager.INSTANCE.unregisterCallback(requestCode);
        }
        super.finish();
    }

    @Override
    void showProgressBar() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            progressDialog.dismiss();
        }
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @Override
    void hideProgressBar() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public static Intent getPurchaseIntent(Context context, String productId, String offerId, Bundle bundle, String campaignEntranceType) {
        return getPurchaseIntent(context, productId, null, offerId, bundle, SourceType.UNKNOWN, campaignEntranceType);
    }

    public static Intent getPurchaseIntent(Context context, String productId, String basePlanId, String offerId, Bundle bundle, String campaignEntranceType) {
        return getPurchaseIntent(context, productId, basePlanId, offerId, bundle, SourceType.UNKNOWN, campaignEntranceType);
    }

    public static Intent getPurchaseIntent(Context context, String productId, String basePlanId, String offerId, Bundle bundle, SourceType sourceType, String campaignEntranceType) {
        Intent intent = new Intent(context, NativePaywallListenerActivity.class);
        intent.putExtra(NativePaywallListenerActivity.PRODUCT_ID_TO_PURCHASE, productId);
        if (basePlanId != null) {
            intent.putExtra(NativePaywallListenerActivity.BASE_PLAN_ID_TO_PURCHASE, basePlanId);
        }
        intent.putExtra(NativePaywallListenerActivity.OFFER_ID_TO_PURCHASE, offerId);
        intent.putExtra(NativePaywallListenerActivity.ARG_SOURCE_TYPE, sourceType.name());
        intent.putExtra(NativePaywallListenerActivity.ARG_CAMPAIGN_ENTRANCE_TYPE, campaignEntranceType);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return intent;
    }

    private String getDialogText(IAPSubItem iapSubItem) {
        IAPOfferItem eligibleOffer = PaywallUtil.INSTANCE.getEligibleOffer(iapSubItem, null);
        String offerPrice = iapSubItem.getOfferPrice();
        String offerPeriod = iapSubItem.getOfferPeriod();
        Integer offerCycle = iapSubItem.getOfferPriceCycles();
        String basePrice = iapSubItem.getBasePrice();
        String basePlanId = iapSubItem.getBasePlanId();
        if (eligibleOffer != null) {
            offerPrice = eligibleOffer.getOfferPrice();
            offerPeriod = eligibleOffer.getOfferPeriod();
            offerCycle = eligibleOffer.getOfferPriceCycles();
        }
        String cancelText = "<a href=\"https://helpcenter.washingtonpost.com/hc/en-us/articles/360000177571-How-to-cancel-your-digital-only-subscription\">cancel</a>";
        if (offerPrice != null && offerPeriod != null && basePlanId != null) {
            String formattedOfferPeriodWithoutOne = PaywallUtil.INSTANCE.getFormattedPeriod(offerPeriod, false);
            String formattedOfferPeriodWithOne = PaywallUtil.INSTANCE.getFormattedPeriod(offerPeriod, true);
            String formattedBasePlanId = PaywallUtil.INSTANCE.getFormattedPeriod(basePlanId.toUpperCase(Locale.ROOT), false);
            String formattedOfferCycles = PaywallUtil.INSTANCE.getFormattedCycles(offerPeriod, offerCycle);
            if (offerPrice.equals(PaywallConstants.OFFER_PRICE_FREE)) {
                return getString(
                        R.string.paywall_free_trial,
                        formattedOfferPeriodWithOne,
                        basePrice,
                        formattedBasePlanId,
                        cancelText
                );
            } else {
                if (formattedOfferCycles != null && formattedOfferCycles.equals("1 year")) {
                    return getString(
                            R.string.paywall_offer_one_year,
                            offerPrice,
                            formattedOfferPeriodWithoutOne,
                            basePrice,
                            formattedBasePlanId,
                            cancelText
                    );
                }
                return getString(
                        R.string.paywall_offer_multi_cycle,
                        offerPrice,
                        formattedOfferPeriodWithoutOne,
                        formattedOfferCycles,
                        basePrice,
                        formattedBasePlanId,
                        cancelText
                );
            }
        } else if (offerPrice == null && basePlanId != null) {
            return getString(
                    R.string.paywall_base_plan_only,
                    basePrice,
                    PaywallUtil.INSTANCE.getFormattedPeriod(basePlanId, false),
                    cancelText
            );
        } else if (basePlanId == null) {
            String period = "P1M";
            if (productId.contains("annual")) {
                period = "P1Y";
            } else if (productId.contains("flex")) {
                // TODO: base plan shouldn't ever be null for flex products but added in case
                period = "P1D";
            }
            if (offerPrice != null && offerPeriod != null) {
                if (offerPrice.equals(PaywallConstants.OFFER_PRICE_FREE)) {
                    return getString(
                            R.string.paywall_free_trial,
                            PaywallUtil.INSTANCE.getFormattedPeriod(offerPeriod, true),
                            basePrice,
                            PaywallUtil.INSTANCE.getFormattedPeriod(period, false),
                            cancelText
                    );
                } else {
                    if (offerPeriod.toUpperCase(Locale.ROOT).equals("P1Y")) {
                        return getString(
                                R.string.paywall_offer_one_year,
                                offerPrice,
                                PaywallUtil.INSTANCE.getFormattedPeriod(offerPeriod, false),
                                basePrice,
                                PaywallUtil.INSTANCE.getFormattedPeriod(period, false),
                                cancelText
                        );
                    } else {
                        return getString(
                                R.string.paywall_offer_multi_cycle,
                                iapSubItem.getOfferPrice(),
                                PaywallUtil.INSTANCE.getFormattedPeriod(offerPeriod, false),
                                PaywallUtil.INSTANCE.getFormattedCycles(offerPeriod, offerCycle),
                                basePrice,
                                PaywallUtil.INSTANCE.getFormattedPeriod(period, false),
                                cancelText
                        );
                    }
                }
            } else {
                return getString(
                        R.string.paywall_base_plan_only,
                        basePrice,
                        PaywallUtil.INSTANCE.getFormattedPeriod(period, false),
                        cancelText
                );
            }
        }
        return "";
    }

    public static void launch(Context context, String productId, String offerId, String campaignId,
                              Bundle bundle, NativePaywallResultCallbacks onResult) {
        Intent intent = NativePaywallListenerActivity.getPurchaseIntent(context, productId, offerId, bundle, campaignId);
        if (onResult != null) {
            long requestCode = System.currentTimeMillis();
            intent.putExtra(PaywallConstants.REQUEST_CODE, requestCode);
            NativePaywallListenerResultCallbackManager.INSTANCE.registerCallback(requestCode, onResult);
        }
        context.startActivity(intent);
    }
}
