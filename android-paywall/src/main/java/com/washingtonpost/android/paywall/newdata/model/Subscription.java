package com.washingtonpost.android.paywall.newdata.model;

import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.response.SubItem;

import java.util.List;

/**
 * Paywall subscription
 * 
 * @author Bkilari
 * 
 */
public class Subscription {

    private String storeUID;
    private String storeSKU;
    private String deviceID;
    private String storeEnv;
    private boolean provisional;
    private String receiptInfo;
    private String receiptNumber;
    private long transactionDate;
    private long startDate;
    private long expirationDate;
    private String storeType;
    private boolean validity;
    private boolean isSynced;
    private boolean isVerified;
    private String subState;
    private String existingSubType;
    private boolean upgrade;
    private PromoPurchaseType promoPurchaseType;
    private List<String> productSkuList;
    private String subscriptionId;
    private String featureJwt;
    private List<SubItem> addonSubscriptions;

    private boolean isDeprecated;

    public PromoPurchaseType getPromoCodePurchaseType(){
        return promoPurchaseType;
    }

    public void setPromoCodePurchaseType(PromoPurchaseType promoPurchaseType){
        this.promoPurchaseType = promoPurchaseType;
    }

    public boolean isVerified() {
		return isVerified;
	}

	public void setVerified(boolean isVerified) {
		this.isVerified = isVerified;
	}

	public boolean isSynced() {
		return isSynced;
	}

	public void setSynced(boolean isSynced) {
		this.isSynced = isSynced;
	}

	public boolean getValidity() {
        return validity;
    }

    public void setValidity(boolean validity) {
        this.validity = validity;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getStoreUID() {
        return storeUID;
    }

    public void setStoreUID(String storeUID) {
        this.storeUID = storeUID;
    }

    public String getStoreProductId() {
        return storeSKU;
    }

    public void setStoreProductId(String storeSKU) {
        this.storeSKU = storeSKU;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getStoreEnv() {
        return storeEnv;
    }

    public void setStoreEnv(String storeEnv) {
        this.storeEnv = storeEnv;
    }

    public boolean isProvisional() {
        return provisional;
    }

    public void setProvisional(boolean provisional) {
        this.provisional = provisional;
    }

    public String getReceiptInfo() {
        return receiptInfo;
    }

    public void setReceiptInfo(String receiptInfo) {
        this.receiptInfo = receiptInfo;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public long getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(long transactionDate) {
        this.transactionDate = transactionDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getSubState() {
        return subState;
    }

    public void setSubState(String subState) {
        this.subState = subState;
    }

    public String getExistingSubType() {
        return existingSubType;
    }

    public void setExistingSubType(String existingSubType) {
        this.existingSubType = existingSubType;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    public List<String> getProductSkuList() {
        return productSkuList;
    }

    public void setProductSkuList(List<String> productSkuList) {
        this.productSkuList = productSkuList;
    }


    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    //this  is used to deprecate a subscription (or archive because it's no longer in use),
    //specifically migrated amazon classic subscriptions whose skus will be migrated by amazon.
    //once we receive the migrated subscription from amazon app store, isDeprecated is set to true
    //and this subscription is no longer used or verified.
    public void setDeprecated(boolean deprecated) {
        this.isDeprecated = deprecated;
    }

    public boolean isDeprecated()  {
        return isDeprecated;
    }

    public String getFeatureJwt() {
        return featureJwt;
    }

    public void setFeatureJwt(String featureJwt) {
        this.featureJwt = featureJwt;
    }

    public List<SubItem> getAddonSubscriptions() {
        return addonSubscriptions;
    }

    public void setAddonSubscriptions(List<SubItem> addonSubscriptions) {
        this.addonSubscriptions = addonSubscriptions;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "storeUID='" + storeUID + '\'' +
                ", storeProductId='" + storeSKU + '\'' +
                ", deviceID='" + deviceID + '\'' +
                ", storeEnv='" + storeEnv + '\'' +
                ", provisional=" + provisional +
                ", receiptInfo='" + receiptInfo + '\'' +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", transactionDate=" + transactionDate +
                ", startDate=" + startDate +
                ", expirationDate=" + expirationDate +
                ", storeType='" + storeType + '\'' +
                ", validity=" + validity +
                ", isSynced=" + isSynced +
                ", isVerified=" + isVerified +
                ", existingSubType='" + existingSubType + '\'' +
                ", upgrade=" + upgrade +
                ", productSkuList=" + productSkuList +
                ", subscriptionId='" + subscriptionId + '\'' +
                '}';
    }
}
