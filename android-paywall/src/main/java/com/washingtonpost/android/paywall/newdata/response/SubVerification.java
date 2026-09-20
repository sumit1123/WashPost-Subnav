package com.washingtonpost.android.paywall.newdata.response;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response
 *
 * @author Bkilari
 */
public class SubVerification {

    @SerializedName("expirationDate")
    String expirationDate;
    @SerializedName("subStatus")
    String subStatus;
    @SerializedName("original_transaction_id")
    String original_transaction_id;
    @SerializedName("status")
    String status;
    @SerializedName("latest_receipt_purchase_date")
    String latest_receipt_purchase_date;
    @SerializedName("purchase_date")
    String purchase_date;
    @SerializedName("transaction_id")
    String transaction_id;
    @SerializedName("latest_receipt_transaction_id")
    String latest_receipt_transaction_id;
    @SerializedName("sourceInfo")
    String sourceInfo;
    @SerializedName("uuid")
    String uuid;
    @SerializedName("existingSubType")
    String existingSubType;
    @SerializedName("upgrade")
    boolean upgrade;
    @SerializedName("subSource")
    String subSource;
    @SerializedName("subState")
    String subState;
    @SerializedName("shortTitle")
    String shortTitle;
    @SerializedName("current_rate_id")
    String currentRateID;
    @SerializedName("subscription_id")
    String subscriptionID;
    @SerializedName("subscriberType")
    String subscriberType;
    @SerializedName("source")
    String source;
    @SerializedName("rateDuration")
    String rateDuration;
    @SerializedName("product")
    String product;
    @SerializedName("subAttributes")
    Map<String, String> subAttributes;
    @SerializedName("subAcctMgmt")
    String subAcctMgmt;
    @SerializedName("subAccountAnalytics")
    String subAccountAnalytics;
    @SerializedName("subAcctInfo")
    SubAcctInfo subAcctInfo;
    @SerializedName("promoCode")
    String promoCode;
    @SerializedName("promoName")
    String promoName;
    @SerializedName("startDate")
    String startDate;
    @SerializedName("endDate")
    String endDate;
    @SerializedName("sku")
    String sku;
    @SerializedName("promoTermType")
    String promoTermType;
    @SerializedName("promoTerm")
    String promoTerm;
    @SerializedName("promoCodeRedeemed")
    boolean promoCodeRedeemed;
    @SerializedName("promoCodeAssigned")
    boolean promoCodeAssigned;
    @SerializedName("promoDuration")
    int promoDuration;
    @SerializedName("messages")
    List<ErrorMessage> messages = new ArrayList<ErrorMessage>();
    @SerializedName("link")
    SubLink link;
    @SerializedName("subscriptions")
    List<SubItem> subscriptions;
    @SerializedName("featureJwt")
    String featureJwt;

    public List<SubItem> getSubscriptions() {
        return subscriptions;
    }

    public String getFeatureJwt() {
        return featureJwt;
    }

    public List<ErrorMessage> getMessages() {
        return messages;
    }

    public String getStatus() {
        return status;
    }

    public String getUuid() {
        return uuid;
    }

    public String getExistingSubType() {
        return existingSubType;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    public String getSubSource() {
        return subSource;
    }

    public String getSubState() {
        return subState;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public String getCurrentRateID() {
        return currentRateID;
    }

    public String getSubscriptionID() {
        return subscriptionID;
    }

    public String getSubscriberType() {
        return subscriberType;
    }

    public String getSource() {
        return source;
    }

    public String getRateDuration() {
        return rateDuration;
    }

    public String getProduct() {
        return product;
    }

    public Map<String, String> getAttributes() {
        return subAttributes;
    }

    public String getSubAcctMgmt() {
        return subAcctMgmt;
    }

    public String getSubAccountAnalytics() {
        return subAccountAnalytics;
    }

    public SubAcctInfo getSubAcctInfo() {
        return subAcctInfo;
    }

    public String getPriceFlag() {
        return subAcctInfo != null ? subAcctInfo.getPriceFlag() : null;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getSubStatus() {
        return subStatus;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public String getPromoName() {
        return promoName;
    }

    public String getPromoStartDate() {
        return startDate;
    }

    public String getPromoEndDate() {
        return endDate;
    }

    public String getPromoCodeProductId() {
        return sku;
    }

    public String getPromoTermType() {
        return promoTermType;
    }

    public String getPromoTerm() {
        return promoTerm;
    }

    public int getPromoDuration() {
        return promoDuration;
    }

    public boolean isPromoCodeRedeemed(){
        return promoCodeRedeemed;
    }

    public SubLink getLink() { return link; }

    public String getOldReceiptId() {
        return original_transaction_id;
    }

    public String getSku() { return sku; }
}
