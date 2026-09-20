/*
 *  Copyright (c) 2019. The Washington Post. All rights reserved.
 */

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;
import com.wapo.android.commons.iterable.AttributionInfo;
import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product;
import com.washingtonpost.android.paywall.PaywallConnector;
import com.washingtonpost.android.paywall.PaywallOmniture;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.api.WPPaywallApiService;
import com.washingtonpost.android.paywall.billing.AbstractStoreBillingHelper;
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel;
import com.washingtonpost.android.paywall.features.casettlement.CaSettlementValues;
import com.washingtonpost.android.paywall.models.BannerPaywallMessage;
import com.washingtonpost.android.paywall.models.BlockerPaywallMessage;
import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.model.DeviceProfile;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems;
import com.washingtonpost.android.paywall.newdata.model.PaywallResult;
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.newdata.response.SubLink;
import com.washingtonpost.android.paywall.newdata.response.SubVerification;
import com.washingtonpost.android.paywall.reminder.accounthold.AccountHoldFragment;
import com.washingtonpost.android.paywall.reminder.state.DialogType;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class PaywallAPITest {

    private WPPaywallApiService apiService;
    private AbstractStoreBillingHelper storeBillingHelper;
    private final static String paywallConfig = "{\n" +
            "        \"on\": true,\n" +
            "      \"limit\": 6,\n" +
            "      \"maxRollingDays\": 5,\n" +
            "      \"maxRollingArticleLimit\": 1000,\n" +
            "      \"groupLimits\": [\n" +
            "        {\n" +
            "          \"groupId\": 0,\n" +
            "          \"sectionIds\": [\n" +
            "            \"world\",\n" +
            "            \"local\",\n" +
            "            \"politics\",\n" +
            "            \"business\",\n" +
            "            \"national\",\n" +
            "            \"powerpost\",\n" +
            "            \"classic-apps\",\n" +
            "            \"sports\",\n" +
            "            \"video\",\n" +
            "            \"opinions\",\n" +
            "            \"technology\",\n" +
            "            \"lifestyle\",\n" +
            "            \"capital_business\",\n" +
            "            \"courts-law\",\n" +
            "            \"federal_government\",\n" +
            "            \"polling\",\n" +
            "            \"whitehouse\",\n" +
            "            \"GovBeat\",\n" +
            "            \"mdpolitics\",\n" +
            "            \"dcpolitics\",\n" +
            "            \"vapolitics\",\n" +
            "            \"forums\",\n" +
            "            \"toles\",\n" +
            "            \"telnaes\",\n" +
            "            \"post-view\",\n" +
            "            \"books\",\n" +
            "            \"celebrities\",\n" +
            "            \"gog\",\n" +
            "            \"comics\",\n" +
            "            \"movies\",\n" +
            "            \"museums\",\n" +
            "            \"music\",\n" +
            "            \"kidspost\",\n" +
            "            \"magazine\",\n" +
            "            \"wellness\",\n" +
            "            \"weddings\",\n" +
            "            \"travel\",\n" +
            "            \"style\",\n" +
            "            \"home\",\n" +
            "            \"express\",\n" +
            "            \"food\",\n" +
            "            \"hax\",\n" +
            "            \"advice\",\n" +
            "            \"restaurants\",\n" +
            "            \"bars-clubs\",\n" +
            "            \"events\",\n" +
            "            \"theater-dance\",\n" +
            "            \"investigations\",\n" +
            "            \"the-switch\",\n" +
            "            \"innovation\",\n" +
            "            \"on-it\",\n" +
            "            \"get-there\",\n" +
            "            \"wonkblog\",\n" +
            "            \"industries\",\n" +
            "            \"markets\",\n" +
            "            \"capitalbusiness\",\n" +
            "            \"digger\",\n" +
            "            \"on-leadership\",\n" +
            "            \"on-small-business\",\n" +
            "            \"africa\",\n" +
            "            \"the_americas\",\n" +
            "            \"asia_pacific\",\n" +
            "            \"europe\",\n" +
            "            \"middle_east\",\n" +
            "            \"national-security\",\n" +
            "            \"foreign-bureaus\",\n" +
            "            \"energy-environment\",\n" +
            "            \"health-science\",\n" +
            "            \"national_security\",\n" +
            "            \"investigations\",\n" +
            "            \"on-innovations\",\n" +
            "            \"on-giving\",\n" +
            "            \"corrections\",\n" +
            "            \"gary-williams\",\n" +
            "            \"highschools\",\n" +
            "            \"colleges\",\n" +
            "            \"dcunited\",\n" +
            "            \"nationals\",\n" +
            "            \"wizards\",\n" +
            "            \"capitals\",\n" +
            "            \"redskins\",\n" +
            "            \"dc-news\",\n" +
            "            \"maryland-news\",\n" +
            "            \"virginia-news\",\n" +
            "            \"public-safety\",\n" +
            "            \"education\",\n" +
            "            \"immigration\",\n" +
            "            \"obituaries\",\n" +
            "            \"trafficandcommuting\",\n" +
            "            \"weather\",\n" +
            "            \"dc-politics\",\n" +
            "            \"blogsandcolumns\",\n" +
            "            \"md-politics\",\n" +
            "            \"loudoun-fauqier\",\n" +
            "            \"prince-william\",\n" +
            "            \"arlington\",\n" +
            "            \"alexandria\",\n" +
            "            \"howard\",\n" +
            "            \"anne-arrundel\",\n" +
            "            \"montgomery\",\n" +
            "            \"fairfax\",\n" +
            "            \"prince-georges\",\n" +
            "            \"southern-maryland\",\n" +
            "            \"on-faith-local\",\n" +
            "            \"virginia-politics\",\n" +
            "            \"va-politics\",\n" +
            "            \"social-issues\",\n" +
            "            \"letters\",\n" +
            "            \"localopinions\",\n" +
            "            \"personalities\",\n" +
            "            \"outlook\",\n" +
            "            \"five-myths\",\n" +
            "            \"courts_law\",\n" +
            "            \"the12\",\n" +
            "            \"watergate\",\n" +
            "            \"realestate\"\n" +
            "          ],\n" +
            "          \"limit\": 3\n" +
            "        }\n" +
            "      ],\n" +
            "        \"freeThresholdSec\" : 3,\n" +
            "        \"omitPwSectionsList\": [\n" +
            "            \"Top-Stories\"\n" +
            "        ],\n" +
            "        \"validSKUs\": [\n" +
            "            \"monthly_all_access\",\n" +
            "            \"MONTHLY_ALL_ACCESS\",\n" +
            "            \"wp.classic.basic\"\n" +
            "        ],\n" +
            "        \"SKU\": \"monthly_all_access\",\n" +
            "        \"playLicense\": \"fnX1xuQrzDgqC59Wpr2MFO8lZMIZ7Z5O01wwQwUGH88WhjPgyBHGXkpKJZi9jzCKMD4UGVsN4EXSZAKwa1ljzdBf2K8HVfwb7A0YNICyy9elhoIuOhgsz/5EVcjz2nxqMSBcHxbDR/VMPJFgiviSW0jDHvJvNBHfj6KvrMc0+lfbuGRx2N5Qz++xO8cGwETZ5qT5xzzox9dHd5RC7fP1tSlA6YSaUD90hoi/KPyiefW2J22zaTkSGEcW7GqPMCIdiVWMD2rgXsUVhPDwxwkyQDQTC3l5m8igQSEqJIf+mHWN7Ihp4+f4KGupTeJsE5//MlI20QLdUDWyAEqe240VUskW2AOKVrvIUmEaPysY+cgg7eondR74aLCClH3Uqwpv9xBJUPJci/yQH+iTw7fNJ44TdU+tilo2r69aIJaQGEyG1IDhroGDM7cHLFOw6bbTtB0HXh7q9gjIef8EA6GPNbY4X1VyqXnKyilK9U1z5DdN1eXRZRDJALOb0AUzpjnXrLuJ9uLijMjoXLS+RNsW9Q==\",\n" +
            "        \"paywallBaseUrl\": \"https://subs-stage.washingtonpost.com/%s/rest/\",\n" +
            "        \"oAuthConfigStub\" : {\n" +
            "          \"clientId\": \"SMACDFC25F524844E0530100007FADB4\",\n" +
            "          \"clientSecret\": \"stgmaCd09fc25f34844e0530100007fadb4\",\n" +
            "          \"authorizationUrl\": \"https://www.washingtonpost.com/subscribe/stage/signin/?case=noa\",\n" +
            "          \"authorizationScope\": \"scope1\",\n" +
            "          \"authorizationState\": \"state1\",\n" +
            "          \"tokenUrl\": \"https://idstg.washingtonpost.com/identity/oauth/v1/token\",\n" +
            "          \"profileUrl\": \"https://idstg.washingtonpost.com/identity/oauth/v1/profile\",\n" +
            "          \"revokeUrl\": \"https://idstg.washingtonpost.com/identity/oauth/v1/revoke\",\n" +
            "          \"migrateUrl\": \"https://idstg.washingtonpost.com/identity/oauth/v1/migrate\",\n" +
            "          \"appType\": \"classic\"\n" +
            "        },\n" +
            "        \"nativePaywallModelUnauthenticated\" : {\n" +
            "            \"message\" : \"Get access to every story in our apps and on the web when you subscribe.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our\\nTerms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelAuthenticatedNoSubscription\" : {\n" +
            "            \"message\" : \"It looks like there’s no subscription associated with\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Sign in with a different account\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelAuthenticatedExpired\" : {\n" +
            "            \"message\" : \"Reactivate your subscription to access unlimited content.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Subscribe\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Subscribe\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Sign in with a different account\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelSubscriberOnlyContent\" : {\n" +
            "            \"message\" : \"This content is available for subscribers only.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelUnauthenticatedSubscriberOnlyContent\" : {\n" +
            "            \"message\" : \"This content is available for subscribers only.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelAuthenticatedSubscriberOnlyContent\" : {\n" +
            "            \"message\" : \"This content is available for subscribers only. It looks like there’s no subscription associated with\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelUnauthenticatedActiveSubscriptionSubscriberOnlyFeature\" : {\n" +
            "            \"message\" : \"Thank you for being a subscriber. Sign in or create a Washington Post account to save stories to your reading list. Your reading list is available on all devices, even offline.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelUnauthenticatedNoSubscriptionSubscriberOnlyFeature\" : {\n" +
            "            \"message\" : \"This feature is available for subscribers only. \",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelAuthenticatedNoSubscriptionSubscriberOnlyFeature\" : {\n" +
            "            \"message\" : \"This feature is available for subscribers only, and there’s no subscription associated with\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelAuthenticatedExpiredSubscriberOnlyFeature\" : {\n" +
            "            \"message\" : \"This feature is available for subscribers only. Reactivate your subscription to save this story.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Subscribe\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Subscribe\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Sign in with a different account\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our Terms of Service and Privacy Policy.\"\n" +
            "        },\n" +
            "        \"nativePaywallModelOnboarding\" : {\n" +
            "            \"message\" : \"Get access to every story in our apps and on the web when you subscribe.\",\n" +
            "            \"heading\" : \"We're glad you're enjoying the Washington Post.\",\n" +
            "            \"offer1Label\" : \"CORE\",\n" +
            "            \"offer1Price\" : \"$9.99/month\",\n" +
            "            \"offer1Summary\" : \"Unlimited web and app access.\",\n" +
            "            \"offer1ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer1ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer1SKU\" : \"wp.classic.basic\",\n" +
            "            \"offer2Label\" : \"PREMIUM\",\n" +
            "            \"offer2Price\" : \"$14.99/month\",\n" +
            "            \"offer2Summary\" : \"Basic Digital plus a bonus subscription, unlimited ebook downloads, and free 30-day passes to share with your friends.\",\n" +
            "            \"offer2ButtonText\" : \"Try 1 month free\",\n" +
            "            \"offer2ButtonColor\" : \"#1955a5\",\n" +
            "            \"offer2SKU\" : \"monthly_all_access\",\n" +
            "            \"signInText\" : \"Subscriber sign in\",\n" +
            "            \"finePrint\" : \"By subscribing to The Washington Post, you agree to our\\nTerms of Service and Privacy Policy.\"\n" +
            "        }\n" +
            "    }";

    @Before
    public void setUp() throws IOException {
        Context testContext = InstrumentationRegistry.getTargetContext();
        JsonAdapter<ServiceConfigStub> adapter = new Moshi.Builder()
                .add(new KotlinJsonAdapterFactory())
                .build().adapter(ServiceConfigStub.class);
        ServiceConfigStub configStub = adapter.fromJson(paywallConfig);
        storeBillingHelper = new AbstractStoreBillingHelper() {
            @Override
            public void initAndCheckSubscription(Context ctx, ServiceConfigStub serviceConfig) {

            }

            @Override
            public void saveSubscriptionProducts(@Nullable StoreHelperInitCallback initCallback) {

            }

            @Override
            public void init(Context ctx, StoreHelperInitCallback callback) {

            }

            @Override
            public boolean isInitialized() {
                return false;
            }

            @Override
            public boolean isInitializing() {
                return false;
            }

            @Override
            public void cleanup() {

            }

            @Override
            public void startPurchaseFlow(Activity parent, StoreHelperPurchaseCallback callback) {

            }

            @Override
            public void startPurchaseFlowWithAddOns(Activity parent, String baseProductId, List<String> addOnProductIds, StoreHelperPurchaseCallback callback) {

            }

            @Override
            public void removeAddOnFromSubscription(Activity parent, String baseProductId, String addOnProductId, StoreHelperPurchaseCallback callback) {

            }

            @Override
            public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
                return false;
            }

            @Override
            public void setSubscriptionProductId(String productId) {

            }

            @Override
            public String getSubscriptionProductId() {
                return null;
            }

            @Override
            public void setSubscriptionOfferId(String offerId) {

            }

            @Override
            public String getSubscriptionOfferId() {
                return null;
            }

            @Override
            public void setSubscriptionBasePlanId(String basePlanId) {

            }

            @Override
            public String getSubscriptionBasePlanId() {
                return null;
            }

            @Override
            public Date getAccessExpiryDate() {
                return null;
            }

            @Override
            public String getUserId() {
                return null;
            }

            @Override
            public String getStoreAccountType() {
                return null;
            }
        };

        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                PaywallService.initialize(
                        testContext,
                        configStub,
                        "salt",
                        new PaywallConnector(testContext) {
                            @Override
                            public void breadcrumb(String breadcrumb) {

                            }

                            @Override
                            public void setCaSettlementValues(CaSettlementValues caSettlementValues) {

                            }

                            @Override
                            public CaSettlementValues getCaSettlementValues() {
                                return null;
                            }

                            @Override
                            public void logHandledException(Exception e) {

                            }

                            @Override
                            public boolean isOnline() {
                                return false;
                            }

                            @Override
                            public String billingEncryptedKey() {
                                return null;
                            }

                            @Override
                            public void logE(EventLog.Builder eventLogBuilder) {

                            }

                            @Override
                            public void logW(EventLog.Builder eventLogBuilder) {

                            }

                            @Override
                            public void logD(EventLog.Builder eventLogBuilder) {

                            }

                            @Override
                            public String getStoreType() {
                                return null;
                            }

                            @Override
                            public String getStoreEnv() {
                                return null;
                            }

                            @Override
                            public String getDeviceId() {
                                return null;
                            }

                            @Override
                            public void paywallClosed() {

                            }

                            @Override
                            public String getPrevEntryPoint() {
                                return null;
                            }

                            @Override
                            public String getAppName() {
                                return null;
                            }

                            @Override
                            public String getAppVersion() {
                                return null;
                            }

                            @Override
                            public String getUserAgent() {
                                return null;
                            }

                            @Override
                            public String getIpAddress() {
                                return null;
                            }

                            @Override
                            public Set<String> getAllowedPwSections() {
                                return null;
                            }

                            @Override
                            public void saveTestSubProductId(String productId, Set<String> validProductIdSet) {

                            }

                            @Override
                            public void updateSixMonthsExpiry(long expDate) {

                            }

                            @Override
                            public void updateTemporaryAccess(boolean shouldGiveAccess) {

                            }

                            @Override
                            public void saveAllReceipts(List<StoreReceipt> receipts) {

                            }

                            @Override
                            public boolean hasDuplicateSubscription() {
                                return false;
                            }

                            @Override
                            public void saveFreeTrialSub(Subscription freeTrialSub) {

                            }

                            @Override
                            public Subscription getFreeTrialSub() {
                                return null;
                            }

                            @Override
                            public void saveFreeTrialStartTime(long time) {

                            }

                            @Override
                            public void saveIAPSubItems(IAPSubItems iapSubItems) {

                            }

                            @NonNull
                            @Override
                            public IAPSubItems getIAPSubItems() {
                                return new IAPSubItems();
                            }

                            @Override
                            public void startSearchActivity(Context ctx) {

                            }

                            @Override
                            public void onSubscriptionStatusChanged(boolean subscribed) {

                            }

                            @Override
                            public void resetLoginAfterIapFlag() {

                            }

                            @Override
                            public void setPrefDeviceProfileSent(boolean isSent) {

                            }

                            @Override
                            public boolean isDeviceProfileSent() {
                                return false;
                            }

                            @Override
                            public void setPrefAmazonUserId(String userId) {

                            }

                            @Override
                            public String getPrefAmazonUserId() {
                                return null;
                            }

                            @Override
                            public void setPriceFlag(String priceFlag) {

                            }

                            @Override
                            public void setPaywallSource(String source) {

                            }

                            @Override
                            public void setPaywallSubSource(String subSource) {

                            }

                            @Override
                            public void setPaywallSubShortTitle(String shortTitle) {

                            }

                            @Override
                            public void setPaywallSubProduct(String product) {

                            }

                            @Override
                            public int getSamsungIAPMode() {
                                return 0;
                            }

                            @Override
                            public boolean isAppFrontOrVisible() {
                                return false;
                            }

                            @Override
                            public boolean expiredFreeTrialAndNotSubscribed() {
                                return false;
                            }

                            @Override
                            public void setPaywallSubCurrentRateID(String currentRateID) {

                            }

                            @Override
                            public String getPaywallSubCurrentRateID() {
                                return null;
                            }

                            @Override
                            public void setPaywallSubscriberType(String subscriberType) {

                            }

                            @Override
                            public String getPaywallSubscriberType() {
                                return null;
                            }

                            @Override
                            public void setPaywallSubAttributes(@Nullable Map<String, String> subAttributes) {

                            }

                            @Nullable
                            @Override
                            public Set<String> getPaywallSubAttributes() {
                                return null;
                            }

                            @Override
                            public void setSubAcctMgmt(String trackingInfo) {

                            }

                            @Override
                            public String getSubAcctMgmt() {
                                return null;
                            }

                            @Override
                            public void setSubAccountAnalytics(String subAccountAnalytics) {

                            }

                            @Override
                            public String getSubAccountAnalytics() {
                                return null;
                            }

                            @Override
                            public long getFreeTrialExpiryDate() {
                                return 0;
                            }

                            @Override
                            public boolean hasUserSubscribedEver() {
                                return false;
                            }

                            @Override
                            public DeviceProfile getDeviceProfile() {
                                return null;
                            }

                            @Override
                            public String getPaywallSubSource() {
                                return null;
                            }

                            @Override
                            public String getPrefPaywallSubShortTitle() {
                                return null;
                            }

                            @Override
                            public void startOnboardingSubscriber(Bundle extras) {

                            }

                            @Override
                            public void syncAlertTopicsWithPreferencesApi() {
                            }

                            @Override
                            public void onLogoutComplete() {

                            }

                            @Override
                            public String getClientId() {
                                return null;
                            }

                            @Override
                            public String getClientSecret() {
                                return null;
                            }

                            @Override
                            public String getJwtSecret() {
                                return "";
                            }

                            public String getIapSubscriptionStatus() {
                                return null;
                            }

                            @Override
                            public void setIapSubscriptionStatus(PaywallConstants.IapSubStatus subscription) {

                            }

                            @Override
                            public void setIapSubscriptionStatus(String subStatus) {

                            }

                            @Override
                            public String getRainbowSubscriptionStatus() {
                                return null;
                            }

                            @Override
                            public void setAmazonClassicSubscriptionStatus(String amazonClassicSubscriptionStatus) {

                            }

                            @Override
                            public String getAmazonClassicSubscriptionStatus() {
                                return null;
                            }

                            public long getPauseTime() {
                                return 0;
                            }

                            @Override
                            public void setPauseTime(long pauseTimeMillis) {

                            }

                            @Override
                            public long getAutoResumeTime() {
                                return 0;
                            }

                            @Override
                            public void setAutoResumeTime(long autoResumeTimeMillis) {

                            }

                            @Override
                            public boolean getShouldVerifyPlayStoreResult() {
                                return false;
                            }

                            @Override
                            public void setShouldVerifyPlayStoreResult(boolean shouldVerify) {

                            }

                            @Override
                            public boolean getShouldVerifyExternalPurchaseResult() {
                                return false;
                            }

                            @Override
                            public void setShouldVerifyExternalPurchaseResult(boolean shouldVerify) {

                            }

                            @Override
                            public void setRainbowSubscriptionStatus(String subStatus) {

                            }

                            @Override
                            public void showPolicy(String type, Context context) {

                            }

                            @Override
                            public void showContactUs(Context context) {

                            }

                            @Override
                            public Intent getPlaystoreIntent(Context context) {
                                return null;
                            }

                            @Override
                            public String getPlayStoreUrl(Context context) {
                                return null;
                            }

                            @Override
                            public void openPlaystore(Context context) {

                            }

                            @Override
                            public void openSiteSubManagement(Context context, Boolean resume, String itid) {

                            }

                            @Override
                            public String getSiteSubManagementUrl(Boolean resume, String itid) {
                                return null;
                            }

                            @Override
                            public void trackTetroEvent(float meterCount, int meterReason) {

                            }

                            @Override
                            public void trackBackFromWall() {

                            }

                            @Override
                            public void clearOneTrustData(Context context) {

                            }

                            @Override
                            public boolean isTablet() {
                                return false;
                            }

                            @Override
                            public String getBlocker() {
                                return null;
                            }

                            @Override
                            public void updateAirshipUserStatus() {

                            }

                            @Override
                            public long amazonFreeTrialDaysRemaining() {
                                return 0;
                            }

                            @Override
                            public String getOneTrustConsentToken() {
                                return null;
                            }

                            @Override
                            public void setOneTrustConsentToken(String consent) {

                            }

                            @Override
                            public boolean canVerifyOnEveryLaunch() {
                                return false;
                            }

                            @Override
                            public void trackOnboardingSeen(String miscellany) {

                            }

                            @Override
                            public void trackOneLinkSignIn() {

                            }

                            @Override
                            public void setSubscriptionLinkResult(SubLink link) {

                            }

                            @Override
                            public String getSubscriptionLinkStatus() {
                                return null;
                            }

                            @Override
                            public String getSubscriptionLinkMessage() {
                                return null;
                            }

                            @Override
                            public void showSignInScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType, boolean isAcquisition, String campaignEntranceType) {

                            }

                            @Override
                            public void showSignUpScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType) {

                            }

                            @Override
                            public void onPaywallInitialize() {

                            }

                            @Override
                            public String getAdSubscriptionStatus() {
                                return "";
                            }

                            @Override
                            public BlockerPaywallMessage getBlockerPaywallMessage() {
                                return null;
                            }

                            @Override
                            public BlockerPaywallMessage getBlockerPaywallMessage(String wallName) {
                                return null;
                            }

                            @Override
                            public void setBlockerPaywallMessages(List<BlockerPaywallMessage> messages) {

                            }

                            @Override
                            public String getBillingCountryCode() {
                                return "";
                            }

                            @Override
                            public void setBillingCountryCode(String countryCode) {

                            }

                            @Override
                            public void openCancelSubscriptionPage(String url, Context context) {

                            }

                            @Override
                            public void openWeb(String url, Context context) {

                            }

                            @Override
                            public Blocker getBlockerFromMessages(String wallName) {
                                return null;
                            }

                            @Override
                            public int getSupportedConfigVersion() {
                                return 0;
                            }

                            @Override
                            public int getSupportedBlockerVersion(Blocker blocker, String category) {
                                return 0;
                            }

                            @Override
                            public List<Product> getProducts(Blocker blocker, String category) {
                                return Collections.emptyList();
                            }

                            @Override
                            public List<Component> getComponents(Blocker blocker, String category) {
                                return Collections.emptyList();
                            }

                            @Override
                            public void onCCPAAdsTrackingUpdated() {

                            }

                            @Override
                            public void openUrl(String url) {

                            }
                        },
                        new PaywallOmniture() {
                            @Override
                            public void trackPaywallBlockOverlay(PaywallConstants.WallType paywallType, String wallName) {

                            }

                            @Override
                            public void trackPaywallBlockOverlay(PaywallConstants.WallType paywallType, String wallName, Boolean isOverlaidPaywall, Boolean isRegWallOriginated) {

                            }

                            @Override
                            public void trackBuyWithStore(PaywallConstants.WallType paywallType, String storeType, String campaignEntranceType) {

                            }

                            @Override
                            public void trackOpenExternalPurchase(String contentUrl) {

                            }

                            @Override
                            public void trackPurchaseComplete(String storeType, AttributionInfo attributionInfo,
                                                              @Nullable String campaignName, @Nullable String offerType) {

                            }

                            @Override
                            public void trackPaywallBlockOverlay(PaywallConstants.WallType paywallType) {

                            }

                            @Override
                            public void trackSignIn(PaywallConstants.WallType paywallType, String wallName, boolean isAcquisition, String campaignEntranceType) {

                            }

                            @Override
                            public void trackSignUp(PaywallConstants.WallType paywallType, String wallName) {

                            }

                            @Override
                            public void setMeterValue(String value) {

                            }

                            @Override
                            public void trackAccountHoldEvent(Context context, AccountHoldFragment.AccountHoldType accounthold) {

                            }

                            @Override
                            public void trackAccountHoldPayment(AccountHoldFragment.AccountHoldType accounthold) {

                            }

                            @Override
                            public void trackAccountHoldDismiss(AccountHoldFragment.AccountHoldType accountholdType) {

                            }

                            @Override
                            public void trackSignInComplete(boolean accountWasCreated, boolean isRegwallClick) {

                            }

                            @Override
                            public String getGenesisLocation() {
                                return null;
                            }

                            @Override
                            public String getGenesisExperience() {
                                return null;
                            }

                            @Override
                            public String getSignInEntranceType(DialogType type) {
                                return null;
                            }

                            @Override
                            public String getTestGroup() {
                                return null;
                            }

                            @Override
                            public String getArcId() {
                                return null;
                            }

                            @Override
                            public void trackPromoCodeRedeemedEvent(PromoPurchaseType promoPurchaseType) {

                            }

                            @Override
                            public void trackWallProfileResume(String contentUrl) {

                            }

                            @Override
                            public void trackBannerProfileResume(String navigationBehavior, String contentUrl) {

                            }

                            @Override
                            public String getPricingTestVariant() {
                                return "";
                            }

                            @Override
                            public void trackMessageEvent(PaywallSheet2ViewModel.EventType eventType, AttributionInfo attributionInfo, @Nullable String productId, @Nullable String offerId, @Nullable IAPSubItem iapSubItem, @Nullable String url) {

                            }
                        },
                        storeBillingHelper
                )
        );
        apiService = new WPPaywallApiService();
    }

    @Test
    public void testVerifyResponse() {

        //test successful response
        String subResponse = "{\"product\":\"BASIC\",\"original_transaction_id\":\"eghdcpcghcdbfmjcnmbfmplc.AO-J1OxJbfiHOSBWRRqSrjiXf1Nz-4oWFErBkESW7VpuInG9KTfB5i-eMEVcbNR6FS3SyqEWeh876kNHA_cjhLr1MLS1y1_zJS9zeWvzR0HVQGiFdMPLS8M\",\"subSource\":\"Play Store\",\"subscriberType\":\"PAID\",\"shortTitle\":\"Basic Digital\",\"source\":\"DEVICE\",\"subscription_id\":2702092,\"current_rate_id\":505,\"sourceInfo\":\"google\",\"purchase_date\":\"2019-07-05 08:26:51\",\"rateDuration\":28,\"status\":\"OK\",\"expirationDate\":\"2019-07-05 08:33:43\",\"subAcctMgmt\":\"28,BASIC,505,DEVICE,SUBSCRIBER,p,DEVICE,\"}";
        Gson gson = new Gson();
        SubVerification deviceSubVerification = gson.fromJson(subResponse, SubVerification.class);
        StoreReceipt storeReceipt = new StoreReceipt("receiptid", "productId", (long) 2323, (long) 2332, null);
        Subscription subscription = storeBillingHelper.createSubscription(storeReceipt);
        PaywallResult.State state = apiService.getVerifyDeviceState(deviceSubVerification, subscription);
        assertEquals(PaywallResult.State.SUCCESS, state);

        //test failed response
        String failedSubResponse = "{\"messages\":[{\"head\":\"\",\"body\":\"Unknown error\"}],\"status\":\"FAILED\"}\n";
        SubVerification deviceSubVerificationFailed = gson.fromJson(failedSubResponse, SubVerification.class);
        StoreReceipt storeReceipt2 = new StoreReceipt("receiptid", "productId", (long) 2323, (long) 2332, null);
        Subscription subscription2 = storeBillingHelper.createSubscription(storeReceipt2);
        PaywallResult.State failedState = apiService.getVerifyDeviceState(deviceSubVerificationFailed, subscription2);
        assertEquals(failedState, PaywallResult.State.FAIL);
    }


}
