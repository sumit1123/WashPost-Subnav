package com.washingtonpost.android.paywall.newdata.response;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class SubItem {

    // ── Strings (nullable) ───────────────────────────────────────────────
    @SerializedName("subscriptionID") @Nullable private String subscriptionId;
    @SerializedName("product") @Nullable private String product;
    @SerializedName("sku") @Nullable private String sku;
    @SerializedName("title") @Nullable private String title;
    @SerializedName("shortTitle") @Nullable private String shortTitle;
    @SerializedName("subStatus") @Nullable private String subStatus;
    @SerializedName("subState") @Nullable private String subState;
    @SerializedName("source") @Nullable private String source;
    @SerializedName("subSource") @Nullable private String subSource;
    @SerializedName("sourceType") @Nullable private String sourceType;
    @SerializedName("deviceStore") @Nullable private String deviceStore;
    @SerializedName("expirationDate") @Nullable private String expirationDate;
    @SerializedName("rateDuration") @Nullable private String rateDuration;
    @SerializedName("currentRateId") @Nullable private String currentRateId;
    @SerializedName("subDuration") @Nullable private String subDuration;
    @SerializedName("productCategoryId") @Nullable private String productCategoryId;

    // ── Booleans (wrapper, nullable) ───────────────────────────────────
    @SerializedName("isActive") @Nullable private Boolean isActive;
    @SerializedName("isProductRenewable") @Nullable private Boolean isProductRenewable;
    @SerializedName("promoCodeRedeemed") @Nullable private Boolean promoCodeRedeemed;

    // ── Getters (all annotated as @Nullable where appropriate) ────────
    @Nullable public String getSubscriptionId() { return subscriptionId; }
    @Nullable public String getProduct() { return product; }
    @Nullable public String getSku() { return sku; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getShortTitle() { return shortTitle; }
    @Nullable public String getSubStatus() { return subStatus; }
    @Nullable public String getSubState() { return subState; }
    @Nullable public String getSource() { return source; }
    @Nullable public String getSubSource() { return subSource; }
    @Nullable public String getSourceType() { return sourceType; }
    @Nullable public String getDeviceStore() { return deviceStore; }
    @Nullable public String getExpirationDate() { return expirationDate; }
    @Nullable public String getRateDuration() { return rateDuration; }
    @Nullable public String getCurrentRateId() { return currentRateId; }
    @Nullable public String getSubDuration() { return subDuration; }
    @Nullable public String getProductCategoryId() { return productCategoryId; }

    // Boolean getters – you can keep the “isX” style, but return the wrapper
    @Nullable public Boolean getIsActive() { return isActive; }
    @Nullable public Boolean isActive() { return isActive; }
    @Nullable public Boolean getIsProductRenewable() { return isProductRenewable; }
    @Nullable public Boolean isProductRenewable() { return isProductRenewable; }
    @Nullable public Boolean getPromoCodeRedeemed() { return promoCodeRedeemed; }
    @Nullable public Boolean isPromoCodeRedeemed() { return promoCodeRedeemed; }

    // ── Setters (unchanged, but accept nullable values) ───────────────
    public void setSubscriptionId(@Nullable String subscriptionId) { this.subscriptionId = subscriptionId; }
    public void setProduct(@Nullable String product) { this.product = product; }
    public void setSku(@Nullable String sku) { this.sku = sku; }
    public void setTitle(@Nullable String title) { this.title = title; }
    public void setShortTitle(@Nullable String shortTitle) { this.shortTitle = shortTitle; }
    public void setSubStatus(@Nullable String subStatus) { this.subStatus = subStatus; }
    public void setSubState(@Nullable String subState) { this.subState = subState; }
    public void setSource(@Nullable String source) { this.source = source; }
    public void setSubSource(@Nullable String subSource) { this.subSource = subSource; }
    public void setSourceType(@Nullable String sourceType) { this.sourceType = sourceType; }
    public void setDeviceStore(@Nullable String deviceStore) { this.deviceStore = deviceStore; }
    public void setExpirationDate(@Nullable String expirationDate) { this.expirationDate = expirationDate; }
    public void setRateDuration(@Nullable String rateDuration) { this.rateDuration = rateDuration; }
    public void setCurrentRateId(@Nullable String currentRateId) { this.currentRateId = currentRateId; }
    public void setSubDuration(@Nullable String subDuration) { this.subDuration = subDuration; }
    public void setProductCategoryId(@Nullable String productCategoryId) { this.productCategoryId = productCategoryId; }

    public void setIsActive(@Nullable Boolean isActive) { this.isActive = isActive; }
    public void setIsProductRenewable(@Nullable Boolean isProductRenewable) { this.isProductRenewable = isProductRenewable; }
    public void setProductRenewable(@Nullable Boolean isProductRenewable) { this.isProductRenewable = isProductRenewable; }
    public void setPromoCodeRedeemed(@Nullable Boolean promoCodeRedeemed) { this.promoCodeRedeemed = promoCodeRedeemed; }
}