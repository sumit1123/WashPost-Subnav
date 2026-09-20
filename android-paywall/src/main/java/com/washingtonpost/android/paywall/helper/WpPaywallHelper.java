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

package com.washingtonpost.android.paywall.helper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.BuildConfig;
import com.washingtonpost.android.paywall.features.ccpa.IdentityPreferencesRecord;
import com.washingtonpost.android.paywall.metering.MeteringPrefs;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.newdata.delegate.PaywallUserCursorDelegate;
import com.washingtonpost.android.paywall.newdata.model.WpUser;
import com.washingtonpost.android.paywall.newdata.response.LoggedInUser;
import com.washingtonpost.android.paywall.newdata.response.SubItem;
import com.washingtonpost.android.paywall.util.PaywallUtil;
import java.util.List;
import java.util.Map;

import static com.washingtonpost.android.paywall.PaywallService.getBillingHelper;


/**
 * Wash Post paywall helper
 *
 * @author Bkilari
 */
public class WpPaywallHelper {

    private static String TAG = WpPaywallHelper.class.getSimpleName();

    private static WpUser loggedInUser = null;

    public static void setPaywallUser(LoggedInUser user) {
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        SubItem baseSub = PaywallUtil.getBestAvailableBaseSubscription(
                user.getSubscriptions(),
                user.getSubscriptionID());
        WpUser wpUser = new WpUser();
        wpUser.setDisplayName(user.getDisplayName());
        wpUser.setFirstName(user.getFirstName());
        wpUser.setUserId(user.getEmail());
        wpUser.setUuid(user.getLoginId());
        wpUser.setSecureLoginID(user.getSecureLoginID());
        wpUser.setSignedInThrough(user.getLoginProvider());
        String product = baseSub != null && baseSub.getProduct() != null ? baseSub.getProduct() : user.getProduct();
        wpUser.setAccessLevel(product == null ? PaywallConstants.WP_PRODUCT_NO : product);
        String expDate = baseSub != null && baseSub.getExpirationDate() != null ? baseSub.getExpirationDate() : user.getExpirationDate();
        wpUser.setAccessExpiry(expDate == null ? "" : expDate);
        wpUser.setAccessPurchaseLocation(baseSub != null  && baseSub.getSource() != null ? baseSub.getSource() : user.getSource());
        wpUser.setCCExpired(PaywallConstants.WP_API_CCEXPIRED_TRUE.equals(user.getCcexpired()));
        wpUser.setSubStatus(baseSub != null && baseSub.getSubStatus() != null ? baseSub.getSubStatus() : user.getSubStatus());
        wpUser.setSubState(baseSub != null && baseSub.getSubState() != null ? baseSub.getSubState() : user.getSubState());
        wpUser.setPartnerId(null); //not getting partner info currently
        wpUser.setPartnerName(null);
        wpUser.setProfilePhotoUrl(user.getProfilePhotoUrl() == null ? "" : user.getProfilePhotoUrl());
        wpUser.setSubDuration(baseSub != null && baseSub.getSubDuration() != null ? baseSub.getSubDuration() : user.getSubDuration());
        Map<String, String> subAttributes = user.getSubAttributes();
        wpUser.setFreeTrialSubtype(parseFreeTrialSubtype(subAttributes));
        wpUser.setConsentToken(user.getConsentToken() == null ? "" : user.getConsentToken());
        wpUser.setSubSku(baseSub != null && baseSub.getSku() != null ? baseSub.getSku() : user.getProductId());
        wpUser.setIsProductRenewable(baseSub != null && baseSub.getIsProductRenewable() != null ? baseSub.getIsProductRenewable() : user.getIsProductRenewable());
        String subscriptionId = baseSub != null && baseSub.getSubscriptionId() != null ? baseSub.getSubscriptionId() : user.getSubscriptionID();
        wpUser.setSubscriptionId(subscriptionId == null ? "" : subscriptionId);
        wpUser.setSubscriptions(user.getSubscriptions());
        wpUser.setCToken(user.getCToken() == null ? "" : user.getCToken());
        wpUser.setFeatureJwt(user.getFeatureJwt());
        String subSource = baseSub != null && baseSub.getSubSource() != null ? baseSub.getSubSource() : user.getSubSource();
        String shortTitle = baseSub != null && baseSub.getShortTitle() != null ? baseSub.getShortTitle() : user.getShortTitle();
        if (subSource != null && shortTitle != null) {
            PaywallService.getConnector().setPaywallSubSource(subSource);
            PaywallService.getConnector().setPaywallSubShortTitle(shortTitle);
        }
        String source = baseSub != null && baseSub.getSource() != null ? baseSub.getSource() : user.getSource();
        if (source != null) {
            PaywallService.getConnector().setPaywallSource(source);
        }
        PaywallService.getConnector().setPaywallSubProduct(product);
        PaywallService.getConnector().setPaywallSubCurrentRateID(baseSub != null && baseSub.getCurrentRateId() != null ? baseSub.getCurrentRateId() : user.getCurrentRateID());
        PaywallService.getConnector().setPaywallSubscriberType(baseSub != null && baseSub.getSourceType() != null ? baseSub.getSourceType() : user.getSourceType());
        PaywallService.getConnector().setPaywallSubAttributes(subAttributes);
        PaywallService.getConnector().setPriceFlag(user.getPriceFlag());
        PaywallService.getConnector().setSubAcctMgmt(user.getSubAcctMgmt());
        PaywallService.getConnector().setSubAccountAnalytics(user.getSubAccountAnalytics());


        ContentValues args = PaywallUserCursorDelegate.getContentValues(wpUser);

        db.insert(PaywallDbHelper.PW_USER_TABLE, null, args);
    }

    public static WpUser getLoggedInUser() {

        if (loggedInUser != null) {
            return loggedInUser;
        }

        SQLiteDatabase db = PaywallService.getConnector().getDB();
        String[] whereVars = new String[]{"1"};
        String where = PaywallDbHelper.PW_LOGGED_IN + " = ?";
        PaywallUserCursorDelegate cursor = new PaywallUserCursorDelegate(
                db.rawQuery("SELECT * from " + PaywallDbHelper.PW_USER_TABLE
                        + " where " + where, whereVars)
        );
        try {
            loggedInUser = cursor.getSingleObject();
        } finally {
            cursor.close();
        }
        return loggedInUser;
    }

    public static void cleanUsers() {
        // Ideally just clear the current user but for now, clear all => only
        // one user at a time can be logged in
        loggedInUser = null;
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        db.delete(PaywallDbHelper.PW_USER_TABLE, null, null);
        if (!getBillingHelper().isClassicOrRainbowSubscriptionActive()) {
            PaywallService.getConnector().setPaywallSource(null);
            PaywallService.getConnector().setPaywallSubSource(null);
            PaywallService.getConnector().setPaywallSubShortTitle(null);
            PaywallService.getConnector().setPaywallSubProduct(null);
            PaywallService.getConnector().setPaywallSubscriberType(null);
            PaywallService.getConnector().setPaywallSubAttributes(null);
            PaywallService.getConnector().setPriceFlag(null);
            PaywallService.getConnector().setSubAcctMgmt(null);
            PaywallService.getConnector().setSubAccountAnalytics(null);
            MeteringPrefs.setFreeArticlesRemaining(0);
        }
    }

    public static void resetPaywallUserAccess(String uuid, String secureLoginId, String product,
                                              String expirationDate,
                                              String ccexpired,
                                              String source, String subSource, String shortTitle, String subStatus,
                                              String subscriberType, String subCurrentRateId, String subDuration,
                                              Map<String, String> subAttributes, String subAcctMgmt, String subAccountAnalytics, String priceFlag,
                                              String userId, String displayName, String firstName, String profilePhotoUrl, String loginProvider, String sku, Boolean isProductRenewable, List<SubItem> subscriptions, String cToken, String featureJwt) {
        WpUser user = getLoggedInUser();
        SQLiteDatabase db = PaywallService.getConnector().getDB();

        WpUser wpUser = new WpUser();
        if (user != null) {
            wpUser.setDisplayName( displayName == null ? user.getDisplayName() : displayName);
            wpUser.setFirstName( firstName == null ? user.getFirstName() : firstName);
            wpUser.setUserId(userId == null ? user.getUserId() : user.getUserId());
            wpUser.setSecureLoginID(secureLoginId == null ? user.getSecureLoginID() : secureLoginId);
            wpUser.setUuid(uuid == null ? user.getUuid() : uuid);
            wpUser.setSubSku(sku == null ? user.getSubSku() : sku);
            wpUser.setAccessLevel(product);
            wpUser.setAccessExpiry(expirationDate);
            wpUser.setAccessPurchaseLocation(source);
            wpUser.setSignedInThrough(user.getSignedInThrough());
            wpUser.setCCExpired(PaywallConstants.WP_API_CCEXPIRED_TRUE.equals(ccexpired));
            wpUser.setPartnerId(user.getPartnerId());
            wpUser.setPartnerName(user.getPartnerName());
            wpUser.setSubStatus(subStatus);
            wpUser.setProfilePhotoUrl(profilePhotoUrl == null ? user.getProfilePhotoUrl() : profilePhotoUrl);
            wpUser.setSubDuration(subDuration == null ? user.getSubDuration() : subDuration);
            wpUser.setIsProductRenewable(isProductRenewable == null ? user.getIsProductRenewable() : isProductRenewable);
            wpUser.setFreeTrialSubtype(parseFreeTrialSubtype(subAttributes));
            wpUser.setSubscriptions(subscriptions != null ? subscriptions : user.getSubscriptions());
            wpUser.setCToken(cToken == null ? user.getCToken() : cToken);
            wpUser.setFeatureJwt(featureJwt);
            PaywallService.getConnector().setPaywallSubAttributes(subAttributes);
            PaywallService.getConnector().setSubAcctMgmt(subAcctMgmt);
            PaywallService.getConnector().setSubAccountAnalytics(subAccountAnalytics);

            if (user.getSignedInThrough() == null) {
                String message = "loginProvider new = " + loginProvider;
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage(message));
            }
        }



        ContentValues args = PaywallUserCursorDelegate.getContentValues(wpUser);

        loggedInUser = null;
        String[] whereVars = new String[]{"1"};

        String where = PaywallDbHelper.PW_LOGGED_IN + " = ?";

        db.update(PaywallDbHelper.PW_USER_TABLE, args, where, whereVars);
        PaywallService.getConnector().setPriceFlag(priceFlag);
        PaywallService.getConnector().setPaywallSource(source);
        PaywallService.getConnector().setPaywallSubSource(subSource);
        PaywallService.getConnector().setPaywallSubShortTitle(shortTitle);
        PaywallService.getConnector().setPaywallSubProduct(product);
        PaywallService.getConnector().setPaywallSubscriberType(subscriberType);
        PaywallService.getConnector().setPaywallSubCurrentRateID(subCurrentRateId);
    }

    public static String parseFreeTrialSubtype(Map<String, String> subAttributes) {
        if (subAttributes != null) {
            if (subAttributes.containsKey(PaywallConstants.FREE_DAYS)) {
                return PaywallConstants.FREE_DAYS;
            } else if (subAttributes.containsKey(PaywallConstants.FREE_ARTICLES)) {
                return PaywallConstants.FREE_ARTICLES;
            } else if (subAttributes.containsKey(PaywallConstants.MOBILE_FREE_DAYS)) {
                return PaywallConstants.MOBILE_FREE_DAYS;
            }
            return null;
        }
        return null;
    }

    public static void setIdentityPreferences(final IdentityPreferencesRecord record) {
        if (record == null) {
            return;
        }
        Thread th = new Thread() {
            @Override
            public void run() {
                SQLiteDatabase db = PaywallService.getConnector().getDB();
                ContentValues contentValues = new ContentValues();
                if (record.getAdsOptOut() != null) {
                    contentValues.put(PaywallDbHelper.IP_ADS_OPT_OUT, record.getAdsOptOut());
                }
                if (record.getExplicitNotice() != null) {
                    contentValues.put(PaywallDbHelper.IP_EXPLICIT_NOTICE, record.getExplicitNotice());
                }
                if (record.getDataSynchronized() != null) {
                    contentValues.put(PaywallDbHelper.IP_DATA_SYNCHRONIZED, record.getDataSynchronized());
                }
                if (record.getServerResponse() != null) {
                    contentValues.put(PaywallDbHelper.IP_CCPA_SERVER_RESPONSE, record.getServerResponse());
                }
                if (record.getSwitchTimestamp() > 0) {
                    contentValues.put(PaywallDbHelper.IP_SWITCH_TIMESTAMP, record.getSwitchTimestamp());
                }
                if(record.getOtContentSynchronized() != null){
                    contentValues.put(PaywallDbHelper.IP_OT_CONTENT_SYNCHRONIZED, record.getOtContentSynchronized());
                }
                int numOfRowsUpdated = db.update(PaywallDbHelper.IDENTITY_PREFERENCES_TABLE, contentValues, null, null);
                if (numOfRowsUpdated == 0) {
                    long rowId = db.insert(PaywallDbHelper.IDENTITY_PREFERENCES_TABLE, null, contentValues);
                    if (BuildConfig.DEBUG) {
                        Logger.d(TAG, "Identity - setIdentityPreferences - Inserted record with id " + rowId + " Record: " + getIdentityPreferences());
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Logger.d(TAG, "Identity - setIdentityPreferences - Updated " + numOfRowsUpdated + " record(s)! - " + getIdentityPreferences());
                    }
                }
            }
        };
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();
        try {
            // Above thread is to make sure handling db operation on worked thread
            // and "join" here is to make sure current thread to wait until db thread finishes its work.
            th.join();
        } catch (InterruptedException e) {
        }
    }

    public static IdentityPreferencesRecord getIdentityPreferences() {
        IdentityPreferencesRecord record = null;
        SQLiteDatabase db = PaywallService.getConnector().getDB();
        Cursor cursor = db.query(PaywallDbHelper.IDENTITY_PREFERENCES_TABLE, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            record = new IdentityPreferencesRecord(
                    cursor.getString(cursor.getColumnIndex(PaywallDbHelper.IP_ADS_OPT_OUT)),
                    cursor.getString(cursor.getColumnIndex(PaywallDbHelper.IP_EXPLICIT_NOTICE)),
                    cursor.getString(cursor.getColumnIndex(PaywallDbHelper.IP_DATA_SYNCHRONIZED)),
                    cursor.getString(cursor.getColumnIndex(PaywallDbHelper.IP_CCPA_SERVER_RESPONSE)),
                    cursor.getLong(cursor.getColumnIndex(PaywallDbHelper.IP_SWITCH_TIMESTAMP)),
                    cursor.getString(cursor.getColumnIndex(PaywallDbHelper.IP_OT_CONTENT_SYNCHRONIZED))
            );
            cursor.close();
        }
        if (BuildConfig.DEBUG && record == null) {
            Logger.d(TAG, "Identity - getIdentityPreferences - no record found!");
        }
        return record;
    }
}