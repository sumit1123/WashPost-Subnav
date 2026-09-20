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

package com.washingtonpost.android.paywall;

import android.content.Context;

import androidx.annotation.Nullable;

import com.wapo.android.commons.iterable.AttributionInfo;
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel;
import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem;
import com.washingtonpost.android.paywall.reminder.accounthold.AccountHoldFragment;
import com.washingtonpost.android.paywall.reminder.state.DialogType;
import com.washingtonpost.android.paywall.util.PaywallConstants;

/**
 * Created by muppallav on 8/18/14.
 */
public interface PaywallOmniture {

    String OVERLAY = "overlay";
    String SUBSCRIBED = "subscribed";

    void trackPaywallBlockOverlay(PaywallConstants.WallType paywallType);

    void trackPaywallBlockOverlay(PaywallConstants.WallType paywallType, String wallName);

    void trackPaywallBlockOverlay(PaywallConstants.WallType paywallType, String wallName, Boolean isOverlaidPaywall, Boolean isRegWallOriginated);

    void trackBuyWithStore(PaywallConstants.WallType paywallType, String storeType, String campaignEntranceType);

    void setMessageTrackingKind(String kind);

    void setSubStartLocationType(String location);

    void setGenesisAcqEntranceType(String value);

    void trackOpenExternalPurchase(String contentUrl);

    void trackPurchaseComplete(String storeType, AttributionInfo attributionInfo,
                               @Nullable String campaignName, @Nullable String offerType);

    void trackSignIn(PaywallConstants.WallType paywallType, String wallName, boolean isAcquisition, String campaignEntranceType);

    void trackSignUp(PaywallConstants.WallType paywallType, String wallName);

    void setMeterValue(String value);

    void trackAccountHoldEvent(Context context, AccountHoldFragment.AccountHoldType accounthold);

    void trackAccountHoldPayment(AccountHoldFragment.AccountHoldType accounthold);

    void trackAccountHoldDismiss(AccountHoldFragment.AccountHoldType accountholdType);

    void trackSignInComplete(boolean accountWasCreated, boolean isRegwallClick);

    String getGenesisLocation();

    String getSubStartLocation();

    String getGenesisExperience();

    String getSignInEntranceType(DialogType type);

    String getTestGroup();

    String getArcId();

    void trackPromoCodeRedeemedEvent(PromoPurchaseType promoPurchaseType);

    void trackWallProfileResume(String contentUrl);

    void trackBannerProfileResume(String navigationBehavior, String contentUrl);

    String getPricingTestVariant();

    void trackMessageEvent(PaywallSheet2ViewModel.EventType eventType, AttributionInfo attributionInfo,
                           @Nullable String productId, @Nullable String offerId,
                           @Nullable IAPSubItem iapSubItem, @Nullable String url);
}
