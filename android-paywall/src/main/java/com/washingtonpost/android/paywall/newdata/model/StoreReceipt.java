package com.washingtonpost.android.paywall.newdata.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class StoreReceipt {

    @SerializedName("receiptId") public final String receiptId;
    @SerializedName("productId") public final String productId;
    @SerializedName("transactionDate") public final Long transactionDate;
    @SerializedName("expirationDate") public final Long expirationDate;
    @SerializedName("token") public String token;
    @SerializedName("productSkuList") public final List<String> productSkuList;

    public StoreReceipt(String receiptId, String productId, Long transactionDate, Long expirationDate, List<String> productSkuList) {
        this.receiptId = receiptId;
        this.productId = productId;
        this.transactionDate = transactionDate;
        this.expirationDate = expirationDate;
        this.productSkuList = productSkuList;
    }

    public boolean isExpired() {
        long currentTime = (new Date()).getTime();
        return expirationDate!=null && currentTime > expirationDate;
    }
}
