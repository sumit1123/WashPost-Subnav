package com.washingtonpost.android.paywall.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub;
import com.washingtonpost.android.paywall.newdata.model.Subscription;

import java.util.Set;

/**
 * Maxim Ignatyev
 * Date: 5/16/13
 */
public interface StoreBillingHelper {

    enum TimeUnit {
        MONTHS, MINUTES
    }

    void setTimeUnits(TimeUnit units);
    TimeUnit getTimeUnits();

    void setDuration(int duration);
    int getDuration();

    /**
     * Asynchronously inits billing(binds to service, checks inventory for subscription)
     * Asynchronously updates database record(updates in case of existing subscription and removes record in case if subscription is missing in inventory)
     * Asynchronously updates cache(static field in cache with subscription data), if subscription is missing sets cached data to null
     * @param ctx
     * @param serviceConfig
     */
    void initAndCheckSubscription(Context ctx, ServiceConfigStub serviceConfig);

    void saveSubscriptionProducts(@Nullable StoreHelperInitCallback initCallback);

    /**
     * Asynchronously inits billing, doesn't make inventory checks, invokes callback methods on a main thread
     * @param ctx
     * @param callback
     */
    void init(final Context ctx, StoreHelperInitCallback callback);

    /**
     * checks if subscription is stored in the database and checks it's expiration date, doesn't check against store inventory
     * @return
     */
    boolean isSubscriptionActive();

    /**
     * get's cached subscription data without querying the db
     * @return
     */
    Subscription cachedSubscription();

    void setCachedSubscription(Subscription subs);

    Subscription getLastActiveSubscription();

    /**
     * checks if store helper is initialized(binded to the billing service)
     * @return
     */
    boolean isInitialized();

    /**
     * checks if async initialization is in progress
     * @return
     */
    boolean isInitializing();

    /**
     * releases service bindings, probably releases some other resources
     */
    void cleanup();

    /**
     * Starts intent for native billing activity, invokes callback methods on result
     * @param parent
     * @param callback
     */

    void startPurchaseFlow(Activity parent, final StoreHelperPurchaseCallback callback);

    void startPurchaseFlowWithAddOns(
            Activity parent,
            String baseProductId,
            java.util.List<String> addOnProductIds,
            StoreBillingHelper.StoreHelperPurchaseCallback callback
    );

    void removeAddOnFromSubscription(
            Activity parent,
            String baseProductId,
            String addOnProductId,
            StoreBillingHelper.StoreHelperPurchaseCallback callback
    );

    /**
     * Updates an existing subscription from [currentProductId] to [targetProductId]
     * @param parent
     * @param currentProductId Product ID of the current subscription
     * @param targetProductId Product ID of the target subscription
     * @param callback
     */
    void startSubscriptionUpdateFlow(
            Activity parent,
            String currentProductId,
            String targetProductId,
            StoreHelperPurchaseCallback callback
    );

    /**
     * Handler of native billing activity result, used in activity which works with billing, invoked in onResult method
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    boolean handleActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * sets monthly subscriptions productId, will be provided from a config
     * @param productId
     */
    void setSubscriptionProductId(String productId);

    /**
     * gets monthly subscriptions productId, will be provided from a config
     *
     */
    String getSubscriptionProductId();

    void setSubscriptionOfferId(String offerId);

    String getSubscriptionOfferId();

    void setSubscriptionBasePlanId(String basePlanId);

    String getSubscriptionBasePlanId();

    /**
     * sets all valid subscription productId values
     *
     */
    void setValidSubscriptionProductIds(Set<String> productIds);

    boolean isValidSubscriptionProductId(String productId);

    interface StoreHelperInitCallback {
        void initializedWithResult(InitResult result);
    }

    interface StoreHelperPurchaseCallback {
        void purchaseFinishedWithResult(PurchaseResult result);
    }

    interface StoreHelperAddAccountCallback {
        void accountAddedWithResult(boolean result);
    }

    enum PurchaseResultStatus {
        RESULT_OK,
        RESULT_CANCELED,
        RESULT_INVALID_OFFER,
        RESULT_ERROR,
        /* Amazon has a generic FAILED result that could mean a legitimate failure, but could also
           mean the user cancelled the transaction */
        RESULT_AMAZON_GENERIC_FAILURE
    }

    class PurchaseResult {
        PurchaseResultStatus status;
        String message;

        public PurchaseResult(PurchaseResultStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public PurchaseResultStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "PurchaseResult{" +
                    "status=" + status +
                    ", message='" + message + '\'' +
                    '}';
        }
    }


    class InitResult{
        String message;
        boolean successfull;
        boolean accountMissing;

        public InitResult(boolean successful, String message, boolean accountMissing) {
            this.successfull = successful;
            this.message = message;
            this.accountMissing = accountMissing;
        }

        public boolean isAccountMissing() {
            return accountMissing;
        }

        public boolean isSuccessfull() {
            return successfull;
        }

        public String getMessage() {
            return message;
        }
    }

    boolean isStoreAccountActive(Context ctx);

    void startAddAccountFlow(Activity activity, StoreHelperAddAccountCallback callback);

    void updateSubscriptionDetails(Subscription updatedSub);

    void onResume(Context ctx);

    String getUserId();

    String getStoreAccountType();
}
