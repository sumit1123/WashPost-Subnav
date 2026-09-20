package com.washingtonpost.android.paywall.billing;

import com.washingtonpost.android.paywall.billing.playstore.PlayStoreBillingHelper;

/**
 * Created by Artur Glyzin a.glyzin@eastbanctech.ru
 */
public class StoreHelperCreator {

    static PlayStoreBillingHelper playStoreBillingHelper;

    public static AbstractStoreBillingHelper create() {
        if (playStoreBillingHelper == null) {
            playStoreBillingHelper = new PlayStoreBillingHelper();
        }
        return playStoreBillingHelper;
    }
}
