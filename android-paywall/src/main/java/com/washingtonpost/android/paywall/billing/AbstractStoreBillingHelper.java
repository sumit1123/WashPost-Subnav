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

package com.washingtonpost.android.paywall.billing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.PaywallReactive;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.helper.PaywallDbHelper;
import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.delegate.PaywallSubscriptionCursorDelegate;
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.util.PaywallUtil;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_RAINBOW_SUBSCRIPTION_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_ID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_PRODUCT_ID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_TABLE;

/**
 * Maxim Ignatyev
 * Date: 7/3/13
 */
public abstract class AbstractStoreBillingHelper implements StoreBillingHelper {

    private static final String TAG = AbstractStoreBillingHelper.class.getName();
    public static String PRODUCT_ID_SUBSCRIPTION = "monthly_all_access";
    private static Set<String> validProductIdList = null;
    protected Subscription currentSubscription = null;
    protected Subscription lastExpiredSubscription = null;
    protected Subscription migratedRainbowSubscription = null;
    protected Subscription migratedAmazonClassicSubscription = null;
    private TimeUnit units = TimeUnit.MONTHS;
    private int duration = 1;


    @Override
    public TimeUnit getTimeUnits() {
        return units;
    }

    @Override
    public void setTimeUnits(TimeUnit units) {
        this.units = units;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public boolean isSubscriptionActive() {
        return currentSubscription != null && isValidSubscriptionProductId(currentSubscription.getStoreProductId());
    }

    public boolean isMigratedRainbowSubscriptionActive() {
        return migratedRainbowSubscription != null && isValidSubscriptionProductId(migratedRainbowSubscription.getStoreProductId());
    }

    public boolean isMigratedAmazonClassicSubscriptionActive() {
        return migratedAmazonClassicSubscription != null && isValidSubscriptionProductId(migratedAmazonClassicSubscription.getStoreProductId());
    }

    public boolean isClassicOrRainbowSubscriptionActive() {
        boolean isValidClassicSubscription = currentSubscription != null && isValidSubscriptionProductId(currentSubscription.getStoreProductId());
        boolean isActiveClassicSubscription = isValidClassicSubscription && !PaywallService.getInstance().isSubscriptionPaused();

        boolean isValidRainbowSubscription = migratedRainbowSubscription != null && isValidSubscriptionProductId(migratedRainbowSubscription.getStoreProductId());
        boolean isActiveRainbowSubscription = isValidRainbowSubscription
                && (PaywallConstants.ACTIVE.equals(PaywallService.getConnector().getRainbowSubscriptionStatus())
                || PaywallConstants.SUSPENDED.equals(PaywallService.getConnector().getRainbowSubscriptionStatus()));

        boolean isValidAmazonClassicSubscription = migratedAmazonClassicSubscription != null && isValidSubscriptionProductId(migratedAmazonClassicSubscription.getStoreProductId());
        boolean isActiveAmazonClassicSubscription = isValidAmazonClassicSubscription
                && (PaywallConstants.ACTIVE.equals(PaywallService.getConnector().getAmazonClassicSubscriptionStatus())
                || PaywallConstants.SUSPENDED.equals(PaywallService.getConnector().getAmazonClassicSubscriptionStatus()));

        return isActiveClassicSubscription || isActiveRainbowSubscription || isActiveAmazonClassicSubscription;
    }

    public boolean isClassicOrRainbowSubscriptionVerified() {
        return currentSubscription != null && currentSubscription.isVerified() || migratedRainbowSubscription != null && migratedRainbowSubscription.isVerified()
                || migratedAmazonClassicSubscription != null && migratedAmazonClassicSubscription.isVerified();
    }

    public Subscription getClassicOrRainbowSubscription() {

        if (currentSubscription != null) {
            return currentSubscription;
        } else if (migratedRainbowSubscription != null) {
            return migratedRainbowSubscription;
        } else {
            return null;
        }
    }

    public Set<String> getValidSubscriptionProductIds() {
        if (validProductIdList == null) {
            validProductIdList = new HashSet<>(3);
            validProductIdList.add(PRODUCT_ID_SUBSCRIPTION);
            validProductIdList = Collections.unmodifiableSet(validProductIdList);
        }
        return validProductIdList;
    }

    @Override
    public void setValidSubscriptionProductIds(Set<String> productIds) {
        if (productIds != null) {
            validProductIdList = Collections.unmodifiableSet(productIds);
        }
    }

    @Override
    public boolean isValidSubscriptionProductId(String productId) {
        return getValidSubscriptionProductIds().contains(productId);
    }

    @Override
    public Subscription cachedSubscription() {
        return currentSubscription;
    }

    public void setCachedSubscription(Subscription subs) {
        currentSubscription = subs;
        if (subs != null && subs.getAddonSubscriptions() != null) {
            PaywallUtil.updateAdFreeStatusFromSubscriptions(subs.getAddonSubscriptions(), PaywallUtil.AdFreeSource.VERIFY);
        } else if (subs != null) {
            // No addon subscriptions — explicitly clear verify ad-free source
            // to prevent stale true state from a previous session
            PaywallReactive.updateAdFreeFromVerify(false);
        }
    }

    public Subscription getMigratedRainbowSubscription() {
        return migratedRainbowSubscription;
    }

    public Subscription getMigratedAmazonClassicSubscription() {
        return migratedAmazonClassicSubscription;
    }

    @Override
    public Subscription getLastActiveSubscription() {
        if (lastExpiredSubscription == null) {
            String token = PaywallService.getPaywallPrefHelper().getPrefLastSubToken();
            String productId = PaywallService.getPaywallPrefHelper().getPrefLastSubProductId();
            if (token != null && productId != null) {
                lastExpiredSubscription = new Subscription();
                lastExpiredSubscription.setStoreUID(token);
                lastExpiredSubscription.setStoreProductId(productId);
            }
        }

        if (lastExpiredSubscription != null) {
            lastExpiredSubscription.setExpirationDate(PaywallService.getPaywallPrefHelper().getPrefLastSubExpirationDate());
        }

        return lastExpiredSubscription;
    }

    public void setLastActiveSubscription(Subscription subs) {
        lastExpiredSubscription = subs;
    }

    public Date getAccessExpiryDate() {
        Date accessExpiryDate = null;

        if (isSubscriptionActive()) {
            if (currentSubscription.getExpirationDate() == 0) {
                //calculating next bill date by 1 month if verify receipt has not come back
                //Note: Confusing with 1 month default value when user has yearly subscriptions. commenting for now to not to show anything until get it from verify call.
                //accessExpiryDate = new Date(expirationDate(currentSubscription.getTransactionDate(), !currentSubscription.isUpgrade()));
            } else {
                accessExpiryDate = new Date(currentSubscription.getExpirationDate());
            }
            Logger.d(TAG, "getAccessExpiryDate - " + accessExpiryDate);
        } else if (migratedRainbowSubscription != null) {
            if (migratedRainbowSubscription.getExpirationDate() != 0) {
                accessExpiryDate = new Date(migratedRainbowSubscription.getExpirationDate());
            }
        } else if (getLastActiveSubscription() != null) {
            if (lastExpiredSubscription.getExpirationDate() != 0) {
                accessExpiryDate = new Date(lastExpiredSubscription.getExpirationDate());
            }
        }
        return accessExpiryDate;
    }

    private long expirationDate(long purchaseDate, boolean isNew) {
        Calendar calPurchase = Calendar.getInstance();
        calPurchase.setTimeInMillis(purchaseDate);

        int timeUnit = (units == TimeUnit.MONTHS) ? Calendar.MONTH : Calendar.MINUTE;
        if (isNew) {
            calPurchase.add(timeUnit, duration);
            return calPurchase.getTimeInMillis();
        }


        Calendar calNow = Calendar.getInstance();
        int currentMonth = calNow.get(Calendar.MONTH);
        int currentYear = calNow.get(Calendar.YEAR);


        calPurchase.set(Calendar.YEAR, currentYear);
        calPurchase.set(Calendar.MONTH, currentMonth);
        if (calNow.before(calPurchase)) {
            return calPurchase.getTimeInMillis();
        }
        calPurchase.add(timeUnit, duration);
        return calPurchase.getTimeInMillis();
    }

    public void cleanSubscription() {
        setCachedSubscription(null);
        cleanupSubscriptionInDB();
        PaywallService.getInstance().setVerifySubUUID(null);
    }


    @Override
    public void startAddAccountFlow(Activity activity, final StoreHelperAddAccountCallback callback) {
        AccountManager am = AccountManager.get(activity);
        am.addAccount(getStoreAccountType(), null, null, null, activity, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                Logger.d(TAG, bundleAccountManagerFuture.toString());// TODO Remove me
                try {
                    Bundle bundle = bundleAccountManagerFuture.getResult();
                    Object accountName = bundle.get(AccountManager.KEY_ACCOUNT_NAME);
                    callback.accountAddedWithResult(accountName != null);
                } catch (Exception e) {
                    callback.accountAddedWithResult(false);
                    Logger.e(TAG, "error creating account:", e);
                }
            }
        }, null);
    }

    @Override
    public boolean isStoreAccountActive(Context ctx) {
        AccountManager am = AccountManager.get(ctx);
        Account[] accs = am.getAccountsByType(getStoreAccountType());
        return accs != null && accs.length > 0;
    }

    public void readSubscriptionFromDB() {
        Logger.d(TAG, "Read subscription from DB");
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        PaywallSubscriptionCursorDelegate delegate =
                new PaywallSubscriptionCursorDelegate(db.rawQuery("SELECT * from " + PaywallDbHelper.PW_SUBSCRIPTION_TABLE, null));
        try {
            Subscription singleObject = delegate.getSingleObject();
            if (singleObject != null) setCachedSubscription(singleObject);
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Error reading subscription")
                    .setErrorMessage(e.getMessage()));
        } finally {
            delegate.close();
        }
    }

    public void readRainbowSubscriptionFromDB() {
        Logger.d(TAG, "Read rainbow subscription from DB");
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        PaywallSubscriptionCursorDelegate delegate =
                new PaywallSubscriptionCursorDelegate(db.rawQuery("SELECT * from " + PW_RAINBOW_SUBSCRIPTION_TABLE, null));
        try {
            Subscription singleObject = delegate.getSingleObject();
            if (singleObject != null) migratedRainbowSubscription = singleObject;
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Error reading subscription")
                    .setErrorMessage(e.getMessage()));
        } finally {
            delegate.close();
        }
    }

    public void readAmazonClassicSubscriptionFromDB() {
        Logger.d(TAG, "Read amazon classic subscription from DB");
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        PaywallSubscriptionCursorDelegate delegate =
                new PaywallSubscriptionCursorDelegate(db.rawQuery("SELECT * from " + PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, null));
        try {
            Subscription singleObject = delegate.getSingleObject();
            if (singleObject != null) migratedAmazonClassicSubscription = singleObject;
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Error reading subscription")
                    .setErrorMessage(e.getMessage()));
        } finally {
            delegate.close();
        }
    }


    public void setPromoCodePurchaseType(PromoPurchaseType promoPurchaseType) {
        if (currentSubscription != null) {
            currentSubscription.setPromoCodePurchaseType(promoPurchaseType);
            updateSubscriptionDetails(currentSubscription);
        } else {
            //Read the subscription again
            Logger.d(TAG, "Read subscription from DB");
            SQLiteDatabase db = PaywallService.getConnector().getDB();
            PaywallSubscriptionCursorDelegate delegate =
                    new PaywallSubscriptionCursorDelegate(db.rawQuery("SELECT * from " + PaywallDbHelper.PW_SUBSCRIPTION_TABLE, null));
            try {
                Subscription singleObject = delegate.getSingleObject();
                if (singleObject != null) {
                    singleObject.setPromoCodePurchaseType(promoPurchaseType);
                    updateSubscriptionDetails(singleObject);
                }
            } catch (Exception e) {
                PaywallService.getConnector().logE(new EventLog.Builder()
                        .setMessage("Error reading subscription")
                        .setErrorMessage(e.getMessage()));
            } finally {
                delegate.close();
            }
        }
    }

    private void cleanupSubscriptionInDB() {
        Logger.d(TAG, "Clean subscription from DB");
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        db.delete(PW_SUBSCRIPTION_TABLE, null, null);
    }

    private void cleanupRainbowSubscriptionInDB() {
        Logger.d(TAG, "Clean rainbow subscription from DB");
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        db.delete(PW_RAINBOW_SUBSCRIPTION_TABLE, null, null);
    }

    private void cleanupAmazonClassicSubscriptionInDB() {
        Logger.d(TAG, "Clean amazon classic subscription from DB");
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        db.delete(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, null, null);
    }

    @Override
    public void updateSubscriptionDetails(Subscription newSub) {
        cleanupSubscriptionInDB();
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = PaywallSubscriptionCursorDelegate.getContentValues(newSub);
        db.insert(PW_SUBSCRIPTION_TABLE, null, args);
        readSubscriptionFromDB();
    }

    public void updateRainbowSubscription(Subscription newSub) {
        cleanupRainbowSubscriptionInDB();
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = PaywallSubscriptionCursorDelegate.getContentValues(newSub);
        db.insert(PW_RAINBOW_SUBSCRIPTION_TABLE, null, args);
        readRainbowSubscriptionFromDB();
    }

    public void updateAmazonClassicSubscription(Subscription newSub) {
        cleanupAmazonClassicSubscriptionInDB();
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = PaywallSubscriptionCursorDelegate.getContentValues(newSub);
        db.insert(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, null, args);
        readAmazonClassicSubscriptionFromDB();
    }


    public void updateClassicOrRainbowSubscription(Subscription newSub) {
        if (currentSubscription != null) {
            updateSubscriptionDetails(newSub);
        } else if (migratedRainbowSubscription != null) {
            updateRainbowSubscription(newSub);
        } else if (migratedAmazonClassicSubscription != null) {
            updateAmazonClassicSubscription(newSub);
        }
    }

    @Override
    public void onResume(Context ctx) {
    }

    public void lastSubscriptionOnDevice(Subscription newSub) {
        PaywallService.getInstance().getPaywallPrefHelper().setPrefLastSubToken(newSub.getStoreUID());
        PaywallService.getInstance().getPaywallPrefHelper().setPrefLastSubProductId(newSub.getStoreProductId());
        setLastActiveSubscription(newSub);
    }

    public void clearSubscription(String productId) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        try {
            db.delete(PW_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_PRODUCT_ID + " = '" + PaywallSubscriptionCursorDelegate.encrypt(productId) + "'", null);
        } catch (GeneralSecurityException e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Encryption error")
                    .setErrorMessage(e.getMessage()));
            throw new RuntimeException("Encryption error");
        }
    }

    public abstract String getUserId();

    public boolean isSandboxMode() {
        return false;
    }

    public boolean canCallVerifyDeviceSubscription() {
        Subscription subs = cachedSubscription();
        return (subs != null && !subs.isVerified()) || (migratedRainbowSubscription != null && !migratedRainbowSubscription.isVerified()) || (migratedAmazonClassicSubscription != null && !migratedAmazonClassicSubscription.isVerified());
    }

    public Subscription createSubscription(StoreReceipt receipt) {
        Subscription subscription;

        subscription = new Subscription();
        subscription.setStoreProductId(receipt.productId);
        subscription.setTransactionDate(System.currentTimeMillis());
        subscription.setStartDate(receipt.transactionDate);
        subscription.setValidity(true);
        subscription.setReceiptNumber(receipt.receiptId);
        subscription.setReceiptInfo(receipt.receiptId);
        //currently only the playstore library requires the storeUID to be receipt.token
        subscription.setStoreUID(receipt.token == null ? receipt.receiptId : receipt.token);
        subscription.setProductSkuList(receipt.productSkuList);

        subscription.setSynced(isSubscriptionActive() && currentSubscription.isSynced());
        subscription.setVerified(isSubscriptionActive() && currentSubscription.isVerified());
        subscription.setUpgrade(isSubscriptionActive() && currentSubscription.isUpgrade());
        if (isSubscriptionActive()
                && currentSubscription.isVerified()) {
            subscription.setExpirationDate(currentSubscription.getExpirationDate());
        } else if (receipt.expirationDate != null && receipt.expirationDate != 0) {
            subscription.setExpirationDate(receipt.expirationDate);
        }

        if (currentSubscription != null) {
            subscription.setExistingSubType(currentSubscription.getExistingSubType());
        }

        return subscription;
    }


    public Subscription createMigratedRainbowSubscription(StoreReceipt receipt) {
        Subscription subscription = new Subscription();
        subscription.setStoreProductId(receipt.productId);
        subscription.setTransactionDate(System.currentTimeMillis());
        subscription.setStartDate(receipt.transactionDate);
        subscription.setValidity(true);
        subscription.setReceiptNumber(receipt.receiptId);
        subscription.setReceiptInfo(receipt.receiptId);
        //currently only the playstore library requires the storeUID to be receipt.token
        subscription.setStoreUID(receipt.token == null ? receipt.receiptId : receipt.token);
        subscription.setProductSkuList(receipt.productSkuList);
        //automatically subscription should be ACTIVE when migrated. verify will update it later if it changes
        subscription.setSubState(PaywallConstants.ACTIVE);
        return subscription;
    }

    public Subscription createMigratedAmazonClassicSubscription(StoreReceipt receipt) {
        Subscription subscription = new Subscription();
        subscription.setStoreProductId(receipt.productId);
        subscription.setTransactionDate(System.currentTimeMillis());
        subscription.setStartDate(receipt.transactionDate);
        subscription.setValidity(true);
        subscription.setReceiptNumber(receipt.receiptId);
        subscription.setReceiptInfo(receipt.receiptId);
        //currently only the playstore library requires the storeUID to be receipt.token
        subscription.setStoreUID(receipt.token == null ? receipt.receiptId : receipt.token);
        subscription.setProductSkuList(receipt.productSkuList);
        //automatically subscription should be ACTIVE when migrated. verify will update it later if it changes
        subscription.setSubState(PaywallConstants.ACTIVE);
        return subscription;
    }

    public void updateClassicSubscriptionId(String subscriptionId) {
        updateSubscriptionIdInTable(PW_SUBSCRIPTION_TABLE, subscriptionId);
        if (currentSubscription != null) {
            currentSubscription.setSubscriptionId(subscriptionId);
        }
    }

    public void updateRainbowSubscriptionId(String subscriptionId) {
        updateSubscriptionIdInTable(PW_RAINBOW_SUBSCRIPTION_TABLE, subscriptionId);
        if (migratedRainbowSubscription != null) {
            migratedRainbowSubscription.setSubscriptionId(subscriptionId);
        }
    }

    public void updateAmazonClassicSubscriptionId(String subscriptionId) {
        updateSubscriptionIdInTable(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, subscriptionId);
        if (migratedAmazonClassicSubscription != null) {
            migratedAmazonClassicSubscription.setSubscriptionId(subscriptionId);
        }
    }

    private void updateSubscriptionIdInTable(String tableName, String subscriptionId) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        ContentValues args = new ContentValues();
        try {
            String encryptedSubscriptionId = PaywallSubscriptionCursorDelegate.encrypt(subscriptionId);
            args.put(PW_SUBSCRIPTION_ID, encryptedSubscriptionId);
            db.update(tableName, args, null, null);
        } catch (GeneralSecurityException e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Encryption error updating subscriptionId in " + tableName)
                    .setErrorMessage(e.getMessage()));
        }
    }
}
