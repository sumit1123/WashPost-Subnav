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

package com.washingtonpost.android.paywall.newdata.delegate;


import android.content.ContentValues;
import android.database.Cursor;
import com.wapo.android.commons.util.Base64DecoderException;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.helper.PaywallDbHelper;
import com.washingtonpost.android.paywall.newdata.model.WpUser;
import com.washingtonpost.android.paywall.newdata.response.SubItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.List;

public class PaywallUserCursorDelegate extends CursorDelegate<WpUser> {
    static final String TAG = PaywallArticleCursorDelegate.class.getSimpleName();

    public PaywallUserCursorDelegate(Cursor cursor) {
        super(cursor);
    }

    @Override
    public WpUser getObject() {
        try {
            WpUser user = new WpUser();
            user.setDisplayName(getStringDecrypt(PaywallDbHelper.PW_DISPLAY_NAME));
            user.setFirstName(getStringDecrypt(PaywallDbHelper.PW_FIRST_NAME));
            user.setUserId(getStringDecrypt(PaywallDbHelper.PW_USER_ID));
            user.setUuid(getStringDecrypt(PaywallDbHelper.PW_UUID));
            user.setSecureLoginID(getStringDecrypt(PaywallDbHelper.PW_SECURE_LOGIN_ID));
            user.setAccessLevel(getStringDecrypt(PaywallDbHelper.PW_ACCESS_LEVEL));
            user.setAccessExpiry(getStringDecrypt(PaywallDbHelper.PW_ACCESS_EXPIRY));
            user.setAccessPurchaseLocation(getStringDecrypt(PaywallDbHelper.PW_ACCESS_PURCHASE_LOCATION));
            user.setSignedInThrough(getStringDecrypt(PaywallDbHelper.PW_USER_SIGNED_IN_THROUGH));
            user.setCCExpired(PaywallConstants.WP_API_CCEXPIRED_TRUE.equals(getStringDecrypt(PaywallDbHelper.PW_USER_CREDITCARD_EXPIRED)));
            user.setPartnerId(getStringDecrypt(PaywallDbHelper.PW_PARTNER_ID));
            user.setPartnerName(getStringDecrypt(PaywallDbHelper.PW_PARTNER_NAME));
            user.setProfilePhotoUrl(getStringDecrypt(PaywallDbHelper.PW_USER_PHOTO_URL));
            user.setSubDuration(getStringDecrypt(PaywallDbHelper.PW_USER_SUB_DURATION));
            user.setConsentToken(getStringDecrypt(PaywallDbHelper.PW_CONSENT_TOKEN));
            user.setSubSku(getStringDecrypt(PaywallDbHelper.PW_USER_SUB_SKU));
            user.setFreeTrialSubtype(getStringDecrypt(PaywallDbHelper.PW_USER_FREE_TRIAL_SUBTYPE));
            user.setIsProductRenewable(Boolean.parseBoolean(getStringDecrypt(PaywallDbHelper.PW_IS_PRODUCT_RENEWABLE)));
            user.setSubscriptionId(getStringDecrypt(PaywallDbHelper.PW_SUBSCRIPTION_ID));
            user.setFeatureJwt(getStringDecrypt(PaywallDbHelper.PW_FEATURE_JWT));
            String subscriptionsJson = getStringDecrypt(PaywallDbHelper.PW_USER_SUBSCRIPTIONS);
            if (subscriptionsJson != null && !subscriptionsJson.isEmpty()) {
                try {
                    Type listType = new TypeToken<List<SubItem>>() {}.getType();
                    List<SubItem> subscriptions = new Gson().fromJson(subscriptionsJson, listType);
                    user.setSubscriptions(subscriptions);
                } catch (Exception e) {
                    // If JSON parsing fails, leave subscriptions null
                }
            }
            //classic never encrypted subStatus, but rainbow did. this is to handle both cases
            try {
                user.setSubStatus(getStringDecrypt(PaywallDbHelper.PW_USER_SUB_STATUS));
            } catch (Base64DecoderException | GeneralSecurityException e) {
                user.setSubStatus(getString(PaywallDbHelper.PW_USER_SUB_STATUS));
            }
            user.setCToken(getStringDecrypt(PaywallDbHelper.PW_CTOKEN));
            return user;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Decoding error");
        } catch (Base64DecoderException e) {
            throw new RuntimeException("Decoding error");
        }
    }

    @Override
    public List<WpUser> getObjectList() {
        return null;
    }

    public static ContentValues getContentValues(WpUser user) {
        try {
            ContentValues args = new ContentValues();
            args.put(PaywallDbHelper.PW_DISPLAY_NAME, encrypt(user.getDisplayName()));
            args.put(PaywallDbHelper.PW_FIRST_NAME, encrypt(user.getFirstName()));
            args.put(PaywallDbHelper.PW_USER_ID, encrypt(user.getUserId()));
            args.put(PaywallDbHelper.PW_UUID, encrypt(user.getUuid()));
            args.put(PaywallDbHelper.PW_SECURE_LOGIN_ID, encrypt(user.getSecureLoginID()));
            args.put(PaywallDbHelper.PW_LOGGED_IN, 1);
            args.put(PaywallDbHelper.PW_ACCESS_LEVEL, encrypt(user.getAccessLevel()));
            args.put(PaywallDbHelper.PW_ACCESS_EXPIRY, encrypt(user.getAccessExpiry()));
            args.put(PaywallDbHelper.PW_ACCESS_PURCHASE_LOCATION, encrypt(user.getAccessPurchaseLocation()));
            args.put(PaywallDbHelper.PW_USER_SIGNED_IN_THROUGH, encrypt(user.getSignedInThrough()));
            args.put(PaywallDbHelper.PW_USER_SUB_STATUS, encrypt(user.getSubStatus()));
            args.put(PaywallDbHelper.PW_USER_FREE_TRIAL_SUBTYPE, encrypt(user.getFreeTrialSubtype()));
            args.put(PaywallDbHelper.PW_USER_PHOTO_URL, encrypt(user.getProfilePhotoUrl()));
            args.put(PaywallDbHelper.PW_USER_SUB_DURATION, encrypt(user.getSubDuration()));
            args.put(PaywallDbHelper.PW_USER_SUB_SKU, encrypt(user.getSubSku()));
            args.put(
                    PaywallDbHelper.PW_USER_CREDITCARD_EXPIRED,
                    encrypt(user.isCCExpired() ?
                            PaywallConstants.WP_API_CCEXPIRED_TRUE :
                            PaywallConstants.WP_API_CCEXPIRED_FALSE
                    )
            );
            args.put(PaywallDbHelper.PW_CONSENT_TOKEN, encrypt(user.getConsentToken()));
            args.put(PaywallDbHelper.PW_PARTNER_ID,encrypt(user.getPartnerId()));
            args.put(PaywallDbHelper.PW_PARTNER_NAME,encrypt(user.getPartnerName()));
            args.put(PaywallDbHelper.PW_IS_PRODUCT_RENEWABLE,encrypt(user.getIsProductRenewable().toString()));
            args.put(PaywallDbHelper.PW_SUBSCRIPTION_ID,encrypt(user.getSubscriptionId()));
            args.put(PaywallDbHelper.PW_FEATURE_JWT,encrypt(user.getFeatureJwt()));
            if (user.getSubscriptions() != null && !user.getSubscriptions().isEmpty()) {
                String subscriptionsJson = new Gson().toJson(user.getSubscriptions());
                args.put(PaywallDbHelper.PW_USER_SUBSCRIPTIONS, encrypt(subscriptionsJson));
            } else {
                args.put(PaywallDbHelper.PW_USER_SUBSCRIPTIONS, encrypt(""));
            }
            args.put(PaywallDbHelper.PW_CTOKEN,encrypt(user.getCToken()));
            return args;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encoding exception");
        }
    }


    public WpUser getSingleObject() {
        if (cursor.moveToFirst()) {
            return getObject();
        }
        return null;
    }

    public void close() {
        cursor.close();
    }
}