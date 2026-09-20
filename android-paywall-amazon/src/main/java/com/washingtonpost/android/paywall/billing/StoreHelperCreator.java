package com.washingtonpost.android.paywall.billing;

import com.washingtonpost.android.paywall.billing.amazon.AmazonBillingHelper;

/**
 * Created by Artur Glyzin a.glyzin@eastbanctech.ru
 */
public class StoreHelperCreator {

    public static AbstractStoreBillingHelper create(){
        return AmazonBillingHelper.getInstance();
    }
}