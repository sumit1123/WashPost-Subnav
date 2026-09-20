package com.washingtonpost.android.paywall.newdata.delegate;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.util.Base64DecoderException;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.newdata.response.SubItem;

import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.*;

import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

/**
 * Maxim Ignatyev
 * Date: 5/16/13
 */
public class PaywallSubscriptionCursorDelegate extends CursorDelegate<Subscription> {

    public PaywallSubscriptionCursorDelegate(Cursor cursor) {
        super(cursor);
    }

    @Override
    public Subscription getObject() {
        try {
            Subscription subs = new Subscription();
            subs.setExpirationDate(getLongDecrypt(PW_SUBSCRIPTION_EXPIRY));
            subs.setReceiptInfo(getStringDecrypt(PW_SUBSCRIPTION_RECEIPT_INFO));
            subs.setReceiptNumber(getStringDecrypt(PW_SUBSCRIPTION_RECEIPT_NUMBER));

            Long transactionDate = getLongDecrypt(PW_SUBSCRIPTION_TRANSACTION_DATE);
            subs.setStartDate(transactionDate);
            subs.setTransactionDate(transactionDate);

            subs.setStoreProductId(getStringDecrypt(PW_SUBSCRIPTION_PRODUCT_ID));
            subs.setStoreUID(getStringDecrypt(PW_SUBSCRIPTION_STORE_UID));
            subs.setValidity("true".equals(getStringDecrypt(PW_SUBSCRIPTION_VALIDITY)));
            subs.setSynced("true".equals(getStringDecrypt(PW_SUBSCRIPTION_SYNCED)));
            subs.setVerified("true".equals(getStringDecrypt(PW_SUBSCRIPTION_VERIFIED)));
            subs.setSubState(getStringDecrypt(PW_SUBSCRIPTION_SUB_STATE));
            subs.setExistingSubType(getStringDecrypt(PW_SUBSCRIPTION_EXISTING_SUB));
            subs.setSubscriptionId(getStringDecrypt(PW_SUBSCRIPTION_ID));
            subs.setUpgrade("true".equals(getStringDecrypt(PW_SUBSCRIPTION_UPGRADE)));
            subs.setDeprecated("true".equals(getStringDecrypt(PW_SUBSCRIPTION_DEPRECATED)));
            String productSkuListString = getStringDecrypt(PW_SUBSCRIPTION_PRODUCT_SKU_LIST);
            if (!TextUtils.isEmpty(productSkuListString)) {
                subs.setProductSkuList(Arrays.asList(productSkuListString.split(",")));
            }
            subs.setFeatureJwt(getStringDecrypt(PW_FEATURE_JWT));
            String addonJson = getStringDecrypt(PW_ADDON_SUBSCRIPTIONS);
            if (addonJson != null && !addonJson.isEmpty()) {
                try {
                    Type listType = new TypeToken<List<SubItem>>() {}.getType();
                    List<SubItem> addons = new Gson().fromJson(addonJson, listType);
                    subs.setAddonSubscriptions(addons);
                } catch (Exception ignored) {
                }
            }
            String promoPurchaseType = getStringDecrypt(PW_PROMO_PURCHASE_TYPE);
            switch (promoPurchaseType){
                case "promo_out_of_app": subs.setPromoCodePurchaseType(PromoPurchaseType.PROMO_OUT_OF_APP);
                    break;
                case "promo_in_app": subs.setPromoCodePurchaseType(PromoPurchaseType.PROMO_IN_APP);
                    break;
                case "undefined_out_of_app": subs.setPromoCodePurchaseType(PromoPurchaseType.UNDEFINED_OUT_OF_APP);
                    break;
                case "undefined_in_app": subs.setPromoCodePurchaseType(PromoPurchaseType.UNDEFINED_IN_APP);
                    break;
                default: subs.setPromoCodePurchaseType(PromoPurchaseType.NONE);
            }
            return subs;
        } catch (GeneralSecurityException e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Security error")
                    .setErrorMessage(e.getMessage()));
            throw new RuntimeException("Decrypt error");
        } catch (Base64DecoderException e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Decode error")
                    .setErrorMessage(e.getMessage()));
            throw new RuntimeException("Decrypt error");
        }
    }

    @Override
    public List<Subscription> getObjectList() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static ContentValues getContentValues(Subscription subscription) {
        try {
            ContentValues args = new ContentValues();
            args.put(PW_SUBSCRIPTION_EXPIRY, encrypt(subscription.getExpirationDate()));
            args.put(PW_SUBSCRIPTION_RECEIPT_INFO, encrypt(subscription.getReceiptInfo()));
            args.put(PW_SUBSCRIPTION_RECEIPT_NUMBER, encrypt(subscription.getReceiptNumber()));
            args.put(PW_SUBSCRIPTION_PRODUCT_ID, encrypt(subscription.getStoreProductId()));
            args.put(PW_SUBSCRIPTION_STORE_UID, encrypt(subscription.getStoreUID()));
            args.put(PW_SUBSCRIPTION_TRANSACTION_DATE, encrypt(subscription.getTransactionDate()));
            args.put(PW_SUBSCRIPTION_VALIDITY, encrypt(subscription.getValidity() ? "true" : "false"));
            args.put(PW_SUBSCRIPTION_SYNCED, encrypt(subscription.isSynced() ? "true" : "false"));
            args.put(PW_SUBSCRIPTION_VERIFIED, encrypt(subscription.isVerified() ? "true" : "false"));
            args.put(PW_SUBSCRIPTION_SUB_STATE, encrypt(subscription.getSubState()));
            if (subscription.getProductSkuList() != null && !subscription.getProductSkuList().isEmpty()) {
                args.put(PW_SUBSCRIPTION_PRODUCT_SKU_LIST, encrypt(TextUtils.join(",", subscription.getProductSkuList())));
            } else {
                args.put(PW_SUBSCRIPTION_PRODUCT_SKU_LIST, encrypt(""));
            }
            String promoCodePurchaseType;
            if(subscription.getPromoCodePurchaseType() == null){
                promoCodePurchaseType = PromoPurchaseType.NONE.getType();
            }else{
                promoCodePurchaseType = subscription.getPromoCodePurchaseType().getType();
            }
            args.put(PW_PROMO_PURCHASE_TYPE, encrypt(promoCodePurchaseType));
            args.put(PW_SUBSCRIPTION_EXISTING_SUB,encrypt(subscription.getExistingSubType()));
            args.put(PW_SUBSCRIPTION_ID, encrypt(subscription.getSubscriptionId()));
            args.put(PW_SUBSCRIPTION_UPGRADE, encrypt(subscription.isUpgrade()?"true":"false"));
            args.put(PW_SUBSCRIPTION_DEPRECATED, encrypt(subscription.isDeprecated()?"true":"false"));
            args.put(PW_FEATURE_JWT, encrypt(subscription.getFeatureJwt()));
            if (subscription.getAddonSubscriptions() != null && !subscription.getAddonSubscriptions().isEmpty()) {
                String addonJson = new Gson().toJson(subscription.getAddonSubscriptions());
                args.put(PW_ADDON_SUBSCRIPTIONS, encrypt(addonJson));
            } else {
                args.put(PW_ADDON_SUBSCRIPTIONS, encrypt(""));
            }
            return args;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encrypt error");
        }
    }



    public Subscription getSingleObject() {
        if(cursor.moveToFirst()) {
            return getObject();
        }
        return null;
    }

    public void close(){
        cursor.close();
    }

}
