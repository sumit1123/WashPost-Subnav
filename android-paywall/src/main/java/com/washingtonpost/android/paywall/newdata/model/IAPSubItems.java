/**
 * Copyright (c) 2019 Washington Post. All rights reserved.
 */

package com.washingtonpost.android.paywall.newdata.model;


import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wapo.android.commons.util.Logger;
import com.wapo.android.commons.util.Utils;
import com.washingtonpost.android.paywall.R;
import com.washingtonpost.android.paywall.util.PaywallUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * @author Jayesh Elamgodil 08/12/2019
 */
public class IAPSubItems {

    private final Map<String, IAPSubItem> iapSubItemsMap = new HashMap<>();

    public void insertItem(IAPSubItem subItem) {
        String subPeriod = subItem.getSubscriptionPeriod();
        if (subPeriod == null) {
            if (subItem.getProductId() != null && subItem.getProductId().contains("annual")) {
                subItem.setSubscriptionPeriod("P1Y");
            } else if (subItem.getBasePlanId() != null && subItem.getBasePlanId().contains("day") && subItem.getProductId() != null && subItem.getProductId().contains("flex")) {
                subItem.setSubscriptionPeriod("P1D");
            } else {
                subItem.setSubscriptionPeriod("P1M");
            }
        }

        if (checkIfAmazonProductHasPrice(subItem.getProductId())) {
            return;
        }

        // flex products will use a composite key (sku:basePlanId) and renewing subscriptions will use the sku as the key
        String key;
        if (subItem.getProductId() != null) {
            if (subItem.getBasePlanId() != null && PaywallUtil.INSTANCE.isNonRenewableProduct(subItem.getProductId())) {
                key = (subItem.getProductId() + ":" + subItem.getBasePlanId()).toLowerCase();
            } else {
                key = subItem.getProductId().toLowerCase();
            }
            iapSubItemsMap.put(key, subItem);
        }
    }

    /**
     * If amazon product is already added and has price, do not overwrite it.
     * Note: This can happen when parent productId and child productId have same name (eg. wp.unified.basic and WP.UNIFIED.BASIC)
     * @param productId
     * @return
     */
    private boolean checkIfAmazonProductHasPrice(String productId) {
        return Utils.INSTANCE.isAmazonBuild() &&
                iapSubItemsMap.containsKey(productId.toLowerCase()) &&
                Objects.requireNonNull(
                        iapSubItemsMap.get(productId.toLowerCase())
                ).getBasePrice() != null;
    }

    @Nullable
    public IAPSubItem getItem(@NonNull String productId) {
        return iapSubItemsMap.get(productId.toLowerCase());
    }

    // TODO: Remove dependency on this from CA Settlement and Omniture and delete function
    public static String getFallBackPrice(String productId, Context context) {
        int id = R.string.core_monthly_price;

        switch (productId) {
            case "wp.classic.basic":
            case "wp.unified.basic":
                id = R.string.core_monthly_price;
                break;
            case "wp.classic.basic.annual":
            case "wp.unified.basic.annual":
                id = R.string.core_yearly_price;
                break;
            case "monthly_all_access":
            case "wp.unified.premium":
                id = R.string.premium_monthly_Price;
                break;
            case "wp.classic.premium.annual":
            case "wp.unified.premium.annual":
                id = R.string.premium_yearly_Price;
                break;
            case "m1-r":
                id = R.string.amazon_monthly_price;
                break;
            case "wp.classic.flex:one-day-pass-1":
                id = R.string.flex_oneday_1_price;
                break;
            case "wp.classic.flex:one-day-pass-2":
                id = R.string.flex_oneday_2_price;
                break;
            case "wp.classic.flex:one-day-pass-3":
                id = R.string.flex_oneday_3_price;
                break;
            default:
                break;
        }
        return context.getString(id);
    }

    public String getIapPricingInfo() {
        String value = "";
        ArrayList<IAPSubItem> itemList = new ArrayList<>(iapSubItemsMap.values());
        for (IAPSubItem item : itemList) {
            String price = item.getBasePrice();
            String introPrice = TextUtils.isEmpty(item.getOfferPrice()) ? "0" : item.getOfferPrice();
            if(!TextUtils.isEmpty(price)) {
                value += item.getProductId() + "|" + introPrice + "|" + price;
            }
            if(item != itemList.get(itemList.size()-1)) {
                value += ";";
            }
        }
        return value;
    }
}
