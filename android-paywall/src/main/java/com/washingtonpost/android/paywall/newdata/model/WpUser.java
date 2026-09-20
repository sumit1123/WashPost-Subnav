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

package com.washingtonpost.android.paywall.newdata.model;

import androidx.annotation.Nullable;
import com.washingtonpost.android.paywall.newdata.response.SubItem;
import java.util.List;

/**
 * Paywall user
 *
 * @author Bkilari
 *
 */
public class WpUser {

    private String displayName;
    private String firstName;
    private String userId;
    private String uuid;
    private String secureLoginID;
    private String accessLevel;
    private String accessExpiry;
    private String accessPurchaseLocation;
    private String signedInThrough;
    private boolean isCCExpired = false;
    private String partnerId;
    private String partnerName;
    private String subStatus;
    private String subState;
    private String freeTrialSubtype;
    private String profilePhotoUrl;
    private String subDuration;
    private String subSku;
    private String consentToken;
    private Boolean isProductRenewable;
    private String subscriptionId;
    private String featureJwt;
    private List<SubItem> subscriptions;
    private String cToken;

    @Nullable
    public String getConsentToken() {
        return consentToken;
    }

    public void setConsentToken(String consentToken) {
        this.consentToken = consentToken;
    }

    public boolean isCCExpired() {
        return isCCExpired;
    }

    public void setCCExpired(boolean isCCExpired) {
        this.isCCExpired = isCCExpired;
    }

    public String getSignedInThrough() {
        return signedInThrough;
    }

    public void setSignedInThrough(String signedInThrough) {
        this.signedInThrough = signedInThrough;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSecureLoginID() {
        return this.secureLoginID;
    }

    public void setSecureLoginID(String secureLoginID) {
        this.secureLoginID = secureLoginID;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getAccessExpiry() {
        return accessExpiry;
    }

    public void setAccessExpiry(String accessExpiry) {
        this.accessExpiry = accessExpiry;
    }

    public String getAccessPurchaseLocation() {
        return accessPurchaseLocation;
    }

    public void setAccessPurchaseLocation(String accessPurchaseLocation) {
        this.accessPurchaseLocation = accessPurchaseLocation;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
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

    public String getFreeTrialSubtype() {
        return freeTrialSubtype;
    }

    public void setFreeTrialSubtype(String freeTrialSubtype) {
        this.freeTrialSubtype = freeTrialSubtype;
    }

    public String getSubSku() { return subSku; }

    public void setSubSku(String sku) { this.subSku = sku; }

    public String getProfilePhotoUrl() {return profilePhotoUrl;}

    public void setProfilePhotoUrl(String url) {this.profilePhotoUrl = url;}

    public String getSubDuration() { return subDuration; }

    public void setSubDuration(String duration) { this.subDuration = duration; }

    public Boolean getIsProductRenewable() {
        return isProductRenewable != null ? isProductRenewable : true;
    }

    public void setIsProductRenewable(Boolean isProductRenewable) {
        this.isProductRenewable = isProductRenewable;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getFeatureJwt() {
        return featureJwt;
    }

    public void setFeatureJwt(String featureJwt) {
        this.featureJwt = featureJwt;
    }

    public List<SubItem> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubItem> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Nullable
    public String getCToken() {
        return cToken;
    }

    public void setCToken(String cToken) {
        this.cToken = cToken;
    }
}