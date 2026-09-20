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

package com.washingtonpost.android.paywall.newdata.response;

import com.google.gson.annotations.SerializedName;
import com.washingtonpost.android.paywall.features.ccpa.PrivacySetting;

import java.util.List;
import java.util.Map;

/**
 * Response
 *
 * @author Bkilari
 */
public class LoggedInUser {

    @SerializedName("subdata")
    String subData;
    @SerializedName("iddata")
    String idData;
    @SerializedName("product")
    String product;
    @SerializedName("expirationDate")
    String expirationDate;
    @SerializedName("status")
    String status;
    @SerializedName("source")
    String source;
    @SerializedName("ccexpired")
    String ccexpired = "false";
    @SerializedName("subSource")
    String subSource;
    @SerializedName("shortTitle")
    String shortTitle;
    @SerializedName("subStatus")
    String subStatus;
    @SerializedName("subState")
    String subState;
    @SerializedName("subDuration")
    String subDuration;
    @SerializedName("currentRateID")
    String currentRateID;
    // this is the subscriber_type
    @SerializedName("sourceType")
    String sourceType;
    @SerializedName("error")
    String error;
    @SerializedName("error_description")
    String errorDescription;
    @SerializedName("subAttributes")
    Map<String, String> subAttributes;
    @SerializedName("subAcctMgmt")
    String subAcctMgmt;
    @SerializedName("subAccountAnalytics")
    String subAccountAnalytics;
    @SerializedName("subAcctInfo")
    SubAcctInfo subAcctInfo;
    @SerializedName("loginId")
    String loginId;
    @SerializedName("secureLoginID")
    String secureLoginID;
    @SerializedName("displayName")
    String displayName;
    @SerializedName("email")
    String email;
    @SerializedName("loginProvider")
    String loginProvider;
    @SerializedName("profile_pic_url")
    String profilePhotoUrl;
    @SerializedName("privacySetting")
    PrivacySetting privacySetting;
    @SerializedName("iab_jwt_token")
    String consentToken;
    @SerializedName("sku")
    String sku;
    @SerializedName("isProductRenewable")
    Boolean isProductRenewable;
    @SerializedName("featureJwt")
    String featureJwt;
    @SerializedName("subscriptions")
    List<SubItem> subscriptions;

    @SerializedName("firstName")
    String firstName;
    @SerializedName("subscriptionID")
    String subscriptionID;

    @SerializedName("ctoken")
    String cToken;

    public String getFirstName(){ return firstName; }
    public String getConsentToken() {
        return consentToken;
    }

    public String getCcexpired() {
        return ccexpired;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public String getSubSource() {
        return subSource;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public String getSubStatus() {
        return subStatus;
    }

    public void setSubStatus(String subStatus) {
        this.subStatus = subStatus;
    }

    public String getSubState() {
        return subState;
    }

    public void setSubState(String subState) {
        this.subState = subState;
    }

    public String getSubDuration() {
        return subDuration;
    }

    public String getCurrentRateID() {
        return currentRateID;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSubData() {
        return subData;
    }

    public String getIdData() {
        return idData;
    }

    public Map<String, String> getSubAttributes() {
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

    public String getLoginId() {
        return loginId;
    }

    public String getSecureLoginID() {
        return secureLoginID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public PrivacySetting getPrivacySetting() {
        return privacySetting;
    }

    @Override
    public String toString() {
        return "LoggedInUser: " + product + " " + expirationDate;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getProfilePhotoUrl() {return profilePhotoUrl;}

    public String getProductId() { return sku; }

    public Boolean getIsProductRenewable() { return isProductRenewable != null ? isProductRenewable : true; }

    public String getFeatureJwt() {
        return featureJwt;
    }

    public List<SubItem> getSubscriptions() {
        return subscriptions;
    }

    public String getSubscriptionID() {
        return subscriptionID;
    }

    public String getCToken(){
        return cToken;
    }
}
