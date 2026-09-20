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

package com.washingtonpost.android.paywall.helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import com.wapo.android.commons.util.Logger;
import android.webkit.URLUtil;

import com.washingtonpost.android.paywall.metering.MeteringPrefs;
import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.delegate.PaywallArticleCursorDelegate;
import com.washingtonpost.android.paywall.newdata.model.ArticleStub;

import java.util.ArrayList;
import java.util.List;

public class PaywallDbHelper extends SQLiteOpenHelper {

    private static final String TAG = PaywallDbHelper.class.getSimpleName();
    public static final String CATEGORY_CONTENT_TYPE = "content_type";
    public static final String PW_SUBSCRIPTION_TABLE = "pw_subscription";
    public static final String PW_RAINBOW_SUBSCRIPTION_TABLE = "pw_rainbow_subscription";

    public static final String PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE = "pw_amazon_classic_subscription";
    public static final String PW_SUBSCRIPTION_STORE_UID = "pw_sub_store_uid";
    public static final String PW_SUBSCRIPTION_PRODUCT_ID = "pw_sub_store_sku";
    public static final String PW_SUBSCRIPTION_RECEIPT_INFO = "pw_sub_receipt_info";
    public static final String PW_SUBSCRIPTION_RECEIPT_NUMBER = "pw_sub_receipt_number";
    public static final String PW_SUBSCRIPTION_TRANSACTION_DATE = "pw_sub_transaction";
    public static final String PW_SUBSCRIPTION_EXPIRY = "pw_sub_expiry";
    public static final String PW_SUBSCRIPTION_VALIDITY = "pw_sub_validity";
    public static final String PW_SUBSCRIPTION_SYNCED = "pw_sub_synced";
    public static final String PW_SUBSCRIPTION_VERIFIED = "pw_sub_verified";
    public static final String PW_SUBSCRIPTION_UPGRADE = "pw_sub_upgrade";
    public static final String PW_SUBSCRIPTION_EXISTING_SUB = "pw_existing_sub";
    public static final String PW_SUBSCRIPTION_SUB_STATE = "pw_sub_state";
    public static final String PW_PROMO_PURCHASE_TYPE = "pw_promo_purchase_type";
    public static final String PW_SUBSCRIPTION_DEPRECATED = "pw_sub_deprecated";
    public static final String PW_SUBSCRIPTION_PRODUCT_SKU_LIST = "pw_sub_product_sku_list";
    public static final String PW_USER_TABLE = "pw_user";
    public static final String PW_DISPLAY_NAME = "pw_display_name";
    public static final String PW_FIRST_NAME = "pw_first_name";
    public static final String PW_USER_ID = "pw_id";
    public static final String PW_PASSWORD = "pw_password"; //TODO:remove
    public static final String PW_UUID = "pw_uuid";
    public static final String PW_SECURE_LOGIN_ID = "pw_secure_login_id";
    public static final String PW_LOGGED_IN = "pw_logged_in"; //TODO:remove
    public static final String PW_ACCESS_LEVEL = "pw_access_level";
    public static final String PW_ACCESS_EXPIRY = "pw_expiry";
    public static final String PW_ACCESS_PURCHASE_LOCATION = "pw_store";
    public static final String PW_USER_SIGNED_IN_THROUGH = "pw_fb_or_wp";
    public static final String PW_USER_SUB_STATUS="pw_sub_status";
    public static final String PW_USER_FREE_TRIAL_SUBTYPE="pw_free_trial_subtype";
    public static final String PW_USER_SUB_SKU="pw_sub_sku";
    public static final String PW_USER_CREDITCARD_EXPIRED = "pw_cc_expired";
    public static final String PW_PARTNER_ID="pw_partner_id";
    public static final String PW_PARTNER_NAME="pw_partner_name";
    public static final String PW_ARTICLE_TABLE = "pw_article";
    public static final String PW_ARTICLE_LINK = "pw_link";
    public static final String PW_ARTICLE_TITLE = "pw_title";
    public static final String PW_TETRO_SYNCED = "pw_tetro_synced";
    public static final String PW_USER_PHOTO_URL = "pw_user_photo_url";
    public static final String PW_USER_SUB_DURATION = "pw_user_sub_duration";
    public static final String PW_ARTICLE_TIME= "pw_article_time";
    //Calling the new group rule : 1
    public static final String PW_ARTICLE_RULE1_TABLE = "pw_article_group";
    public static final String PW_ARTICLE_GROUP_ID = "pw_groupid";
    public static final String PW_ARTICLE_SECTION = "pw_section";
    // Calling the new rolling meter rule :2
    public static final String PW_ARTICLE_RULE2_TABLE = "pw_article_rolling";
    private static final String DB_NAME = "wp.paywall";
    public static final String CREATE_TABLE = "CREATE TABLE ";
    public static final String SPACE = " ";
    public static final String OPENING_PARENTHESIS = "(";
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String TEXT_NOT_NULL = "TEXT NOT NULL";
    public static final String INTEGER = "INTEGER";
    public static final String INTEGER_DEFAULT_ZERO = "INTEGER DEFAULT 0";
    public static final String TEXT = "TEXT";
    public static final String SPACE_TEXT_COMMA_SPACE = " TEXT, ";
    public static final String DEFAULT = "DEFAULT";
    public static final String COMMA = ",";
    public static final String ALTER_TABLE = "ALTER TABLE ";
    public static final String ADD_COLUMN = "ADD COLUMN";
    public static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    // IdentityPreferences Table
    public static final String IDENTITY_PREFERENCES_TABLE = "identity_preferences";
    public static final String IP_ADS_OPT_OUT = "ip_ads_opt_out";
    public static final String IP_EXPLICIT_NOTICE = "ip_ads_explicit_notice";
    public static final String IP_DATA_SYNCHRONIZED = "ip_synchronized";
    public static final String IP_CCPA_SERVER_RESPONSE = "ip_ccpa_server_response";
    public static final String IP_SWITCH_TIMESTAMP = "ip_switch_timestamp";
    public static final String IP_OT_CONTENT_SYNCHRONIZED = "ip_ot_content_synchronized";
    public static final String PW_CONSENT_TOKEN = "pw_iab_jwt_token";
    public static final String PW_IS_PRODUCT_RENEWABLE = "pw_is_product_renewable";
    public static final String PW_SUBSCRIPTION_ID = "pw_subscription_id";
    public static final String PW_USER_SUBSCRIPTIONS = "pw_user_subscriptions";
    public static final String PW_FEATURE_JWT = "pw_feature_jwt";
    public static final String PW_ADDON_SUBSCRIPTIONS = "pw_addon_subscriptions";
    public static final String PW_CTOKEN = "pw_ctoken";

    private static final int DB_VERSION = 26;

    public PaywallDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    public static String getArticleCreation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_ARTICLE_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_ARTICLE_LINK).append(SPACE).append(TEXT_NOT_NULL).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_TITLE).append(SPACE).append(TEXT_NOT_NULL).append(COMMA).append(SPACE)
                .append(PW_TETRO_SYNCED).append(SPACE).append(INTEGER_DEFAULT_ZERO).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_TIME).append(SPACE).append(INTEGER_DEFAULT_ZERO)
                .append(CLOSING_PARENTHESIS)
                .toString();
    }


    public static String getUserCreation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_USER_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_USER_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SECURE_LOGIN_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PASSWORD).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_DISPLAY_NAME).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_FIRST_NAME).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_UUID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_LOGGED_IN).append(SPACE).append(INTEGER_DEFAULT_ZERO).append(COMMA).append(SPACE)
                .append(PW_ACCESS_LEVEL).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ACCESS_EXPIRY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SIGNED_IN_THROUGH).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_CREDITCARD_EXPIRED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ACCESS_PURCHASE_LOCATION).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PARTNER_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PARTNER_NAME).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SUB_STATUS).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_FREE_TRIAL_SUBTYPE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_PHOTO_URL).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SUB_DURATION).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_CONSENT_TOKEN).append(SPACE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_IS_PRODUCT_RENEWABLE).append(SPACE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SUB_SKU).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SUBSCRIPTIONS).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_FEATURE_JWT).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_CTOKEN).append(SPACE).append(TEXT)
                .append(CLOSING_PARENTHESIS)
                .toString();

    }

    public static String getSubscriptionCreation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_SUBSCRIPTION_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_SUBSCRIPTION_STORE_UID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_PRODUCT_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_RECEIPT_INFO).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_RECEIPT_NUMBER).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_TRANSACTION_DATE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_EXPIRY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_SYNCED).append(SPACE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_VERIFIED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_VALIDITY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_UPGRADE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_SUB_STATE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PROMO_PURCHASE_TYPE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_DEPRECATED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_PRODUCT_SKU_LIST).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_EXISTING_SUB).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_FEATURE_JWT).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ADDON_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(CLOSING_PARENTHESIS)
                .toString();

    }

    public static String getRainbowSubscriptionCreation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_RAINBOW_SUBSCRIPTION_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_SUBSCRIPTION_STORE_UID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_PRODUCT_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_RECEIPT_INFO).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_RECEIPT_NUMBER).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_TRANSACTION_DATE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_EXPIRY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_SYNCED).append(SPACE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_VERIFIED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_VALIDITY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_UPGRADE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_SUB_STATE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PROMO_PURCHASE_TYPE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_DEPRECATED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_PRODUCT_SKU_LIST).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_EXISTING_SUB).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_FEATURE_JWT).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ADDON_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(CLOSING_PARENTHESIS)
                .toString();

    }


    public static String getAmazonClassicSubscriptionCreation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_SUBSCRIPTION_STORE_UID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_PRODUCT_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_RECEIPT_INFO).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_RECEIPT_NUMBER).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_TRANSACTION_DATE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_EXPIRY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_SYNCED).append(SPACE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_VERIFIED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_VALIDITY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_UPGRADE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_SUB_STATE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PROMO_PURCHASE_TYPE).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_DEPRECATED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_PRODUCT_SKU_LIST).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_EXISTING_SUB).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_SUBSCRIPTION_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_FEATURE_JWT).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ADDON_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(CLOSING_PARENTHESIS)
                .toString();

    }
    public static String getArticleRule1Creation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_ARTICLE_RULE1_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_ARTICLE_LINK).append(SPACE).append(TEXT_NOT_NULL).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_GROUP_ID).append(SPACE).append(INTEGER).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_SECTION).append(SPACE).append(TEXT_NOT_NULL).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_TITLE).append(SPACE).append(TEXT_NOT_NULL).append(CLOSING_PARENTHESIS).toString();
    }

    public static String getArticleRule2Creation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(PW_ARTICLE_RULE2_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_ARTICLE_LINK).append(SPACE).append(TEXT_NOT_NULL).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_TITLE).append(SPACE).append(TEXT_NOT_NULL).append(CLOSING_PARENTHESIS)
                .toString();
    }

    public static String getIdentityPreferencesTableCreation() {
        return new StringBuilder()
                .append(CREATE_TABLE)
                .append(IDENTITY_PREFERENCES_TABLE).append(SPACE).append(OPENING_PARENTHESIS)
                .append(IP_ADS_OPT_OUT).append(SPACE_TEXT_COMMA_SPACE)
                .append(IP_EXPLICIT_NOTICE).append(SPACE_TEXT_COMMA_SPACE)
                .append(IP_DATA_SYNCHRONIZED).append(SPACE_TEXT_COMMA_SPACE)
                .append(IP_CCPA_SERVER_RESPONSE).append(SPACE_TEXT_COMMA_SPACE)
                .append(IP_SWITCH_TIMESTAMP).append(SPACE).append(INTEGER_DEFAULT_ZERO).append(COMMA)
                .append(IP_OT_CONTENT_SYNCHRONIZED).append(SPACE).append(TEXT)
                .append(CLOSING_PARENTHESIS)
                .toString();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PaywallDbHelper.getArticleCreation());
        db.execSQL(PaywallDbHelper.getArticleRule1Creation());
        db.execSQL(PaywallDbHelper.getArticleRule2Creation());
        db.execSQL(PaywallDbHelper.getUserCreation());
        db.execSQL(PaywallDbHelper.getSubscriptionCreation());
        db.execSQL(PaywallDbHelper.getIdentityPreferencesTableCreation());
        db.execSQL(PaywallDbHelper.getRainbowSubscriptionCreation());
        db.execSQL(PaywallDbHelper.getAmazonClassicSubscriptionCreation());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            //
            // versions 1 & 2 are dev. versions. It's ok to just drop the tables;
            db.execSQL(DROP_TABLE_IF_EXISTS + PW_SUBSCRIPTION_TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + PW_USER_TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + PW_ARTICLE_TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + PW_ARTICLE_RULE1_TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + PW_ARTICLE_RULE2_TABLE);
            onCreate(db);
        } else if(oldVersion == 3){
            db.execSQL(PaywallDbHelper.getArticleRule1Creation());
            db.execSQL(PaywallDbHelper.getArticleRule2Creation());
        } else if(oldVersion == 4){
            db.execSQL(PaywallDbHelper.getArticleRule2Creation());
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_SUBSCRIPTION_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_SUBSCRIPTION_UPGRADE).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_SUBSCRIPTION_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_SUBSCRIPTION_EXISTING_SUB).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }

        if (oldVersion < 9) {
            db.execSQL(PaywallDbHelper.getArticleRule2Creation().replace(
                    "CREATE TABLE",
                    "CREATE TABLE IF NOT EXISTS"
            ));

            db.execSQL(PaywallDbHelper.getArticleRule1Creation().replace(
                    "CREATE TABLE",
                    "CREATE TABLE IF NOT EXISTS"
            ));
        }
        if (oldVersion < 10) {
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_SUBSCRIPTION_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_SUBSCRIPTION_SUB_STATE).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }
        if (oldVersion < 11) {
            db.execSQL(new StringBuilder()
                    .append(CREATE_TABLE)
                    .append(IDENTITY_PREFERENCES_TABLE).append(SPACE).append(OPENING_PARENTHESIS)
                    .append(IP_ADS_OPT_OUT).append(SPACE_TEXT_COMMA_SPACE)
                    .append(IP_EXPLICIT_NOTICE).append(SPACE_TEXT_COMMA_SPACE)
                    .append(IP_DATA_SYNCHRONIZED).append(SPACE_TEXT_COMMA_SPACE)
                    .append(IP_CCPA_SERVER_RESPONSE).append(SPACE_TEXT_COMMA_SPACE)
                    .append(IP_SWITCH_TIMESTAMP).append(SPACE).append(INTEGER_DEFAULT_ZERO)
                    .append(CLOSING_PARENTHESIS)
                    .toString());
        }
        if (oldVersion < 12) {
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_SECURE_LOGIN_ID).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }
        if (oldVersion < 13) {
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_USER_PHOTO_URL).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_USER_SUB_DURATION).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }

        if(oldVersion < 14) {
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_ARTICLE_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_TETRO_SYNCED).append(SPACE).append(INTEGER_DEFAULT_ZERO).append(SPACE)
                    .toString());
        }

        if(oldVersion < 15){
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_ARTICLE_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_ARTICLE_TIME).append(SPACE).append(INTEGER_DEFAULT_ZERO).append(SPACE)
                    .toString());
        }

        if(oldVersion < 16){
            deleteInvalidUrl(db);
            MeteringPrefs.cleanUpMeteringPrefs();
        }

        if(oldVersion < 17){
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_SUBSCRIPTION_TABLE).append(SPACE)
                    .append(ADD_COLUMN).append(SPACE)
                    .append(PW_PROMO_PURCHASE_TYPE).append(SPACE)
                    .append(TEXT).append(SPACE)
                    .append(DEFAULT).append(SPACE)
                    .append(PromoPurchaseType.NONE.getType())
                    .toString());
        }
        if(oldVersion < 18){
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE).append(SPACE)
                    .append(ADD_COLUMN).append(SPACE)
                    .append(PW_CONSENT_TOKEN).append(SPACE)
                    .append(TEXT).append(SPACE)
                    .toString());
        }

        if(oldVersion < 19){
            if(!doesColumnExistInTable(db, IDENTITY_PREFERENCES_TABLE, IP_OT_CONTENT_SYNCHRONIZED)) {
                db.execSQL(new StringBuilder().
                        append(ALTER_TABLE).
                        append(IDENTITY_PREFERENCES_TABLE).append(SPACE).
                        append(ADD_COLUMN).append(SPACE).
                        append(IP_OT_CONTENT_SYNCHRONIZED).append(SPACE).append(TEXT).append(SPACE).
                        toString());
            }
        }

        if(oldVersion < 20) {
            db.execSQL(PaywallDbHelper.getRainbowSubscriptionCreation());
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_USER_SUB_SKU).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }

        if(oldVersion < 21) {
            if(!doesColumnExistInTable(db, PW_USER_TABLE, PW_USER_FREE_TRIAL_SUBTYPE)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_USER_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_USER_FREE_TRIAL_SUBTYPE).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
        }

        if (oldVersion < 22) {
            db.execSQL(PaywallDbHelper.getAmazonClassicSubscriptionCreation());
        }

        if (oldVersion < 23) {
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_IS_PRODUCT_RENEWABLE).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }

        if (oldVersion < 24){
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_FIRST_NAME).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }

        if (oldVersion < 25){
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_SUBSCRIPTION_ID).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }

        /*
         * Note: We use doesColumnExistInTable checks here because versions 20 and 22
         * create the rainbow and amazon_classic subscription tables (via getRainbowSubscriptionCreation()
         * and getAmazonClassicSubscriptionCreation()) with all columns already included.
         * Without these checks, users upgrading from versions 20-25 would hit a
         * "duplicate column name" crash. This pattern should be followed for any future
         * migrations until the upgrade logic is refactored.
         */
        if (oldVersion < 26){
            //Add PW_SUBSCRIPTION_PRODUCT_SKU_LIST
            if(!doesColumnExistInTable(db, PW_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_PRODUCT_SKU_LIST)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_SUBSCRIPTION_PRODUCT_SKU_LIST).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_RAINBOW_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_PRODUCT_SKU_LIST)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_RAINBOW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_SUBSCRIPTION_PRODUCT_SKU_LIST).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_PRODUCT_SKU_LIST)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_SUBSCRIPTION_PRODUCT_SKU_LIST).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }

            // Add PW_SUBSCRIPTION_ID
            if(!doesColumnExistInTable(db, PW_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_ID)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_SUBSCRIPTION_ID).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_RAINBOW_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_ID)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_RAINBOW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_SUBSCRIPTION_ID).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, PW_SUBSCRIPTION_ID)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_SUBSCRIPTION_ID).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }

            // Add PW_USER_SUBSCRIPTIONS
            if(!doesColumnExistInTable(db, PW_USER_TABLE, PW_USER_SUBSCRIPTIONS)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_USER_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_USER_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }

            // Add PW_FEATURE_JWT
            if(!doesColumnExistInTable(db, PW_SUBSCRIPTION_TABLE, PW_FEATURE_JWT)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_FEATURE_JWT).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_USER_TABLE, PW_FEATURE_JWT)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_USER_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_FEATURE_JWT).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_RAINBOW_SUBSCRIPTION_TABLE, PW_FEATURE_JWT)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_RAINBOW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_FEATURE_JWT).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, PW_FEATURE_JWT)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_FEATURE_JWT).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }

            if(!doesColumnExistInTable(db, PW_USER_TABLE, PW_CTOKEN)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_USER_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_CTOKEN).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }

            if(!doesColumnExistInTable(db, PW_SUBSCRIPTION_TABLE, PW_ADDON_SUBSCRIPTIONS)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_ADDON_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_RAINBOW_SUBSCRIPTION_TABLE, PW_ADDON_SUBSCRIPTIONS)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_RAINBOW_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_ADDON_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
            if(!doesColumnExistInTable(db, PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE, PW_ADDON_SUBSCRIPTIONS)) {
                db.execSQL(new StringBuilder()
                        .append(ALTER_TABLE)
                        .append(PW_AMAZON_CLASSIC_SUBSCRIPTION_TABLE)
                        .append(SPACE).append(ADD_COLUMN).append(SPACE)
                        .append(PW_ADDON_SUBSCRIPTIONS).append(SPACE).append(TEXT).append(SPACE)
                        .toString());
            }
        }

        fixCorruptDb(db);
    }

    private void deleteInvalidUrl(SQLiteDatabase db) {
        PaywallArticleCursorDelegate cursor = new PaywallArticleCursorDelegate(db.rawQuery("SELECT * from " + PW_ARTICLE_TABLE, null));
        List<ArticleStub> articleList;
        try {
            articleList = cursor.getObjectList();
            List<String> invalidUrlList = new ArrayList<>();
            for (ArticleStub stub: articleList) {
                if(stub.getUrl()!=null && !URLUtil.isValidUrl(stub.getUrl())) {
                    invalidUrlList.add(stub.getUrl());
                }
            }
            if(!invalidUrlList.isEmpty()) {
                for (String url: invalidUrlList) {
                    db.delete(PW_ARTICLE_TABLE, PaywallDbHelper.PW_ARTICLE_LINK + "=?", new String[]{url});
                }
            }
        } catch (Exception e) {
            Logger.d("InvalidUrl", "Failed to clean invalid url");
        } finally {
            cursor.close();
        }
    }

    private void fixCorruptDb(SQLiteDatabase db) {
        if (db == null) return;

        // fix PW_USER_TABLE
        Cursor pwUserTableCursor = db.rawQuery("SELECT * FROM " + PW_USER_TABLE, null);
        if (pwUserTableCursor == null) return;

        boolean foundUserSubStatusColumn = false;
        for (String name : pwUserTableCursor.getColumnNames()) {
            if (PW_USER_SUB_STATUS.equals(name)) {
                foundUserSubStatusColumn = true;
                break;
            }
        }
        if (!foundUserSubStatusColumn) {
            db.execSQL(new StringBuilder()
                    .append(ALTER_TABLE)
                    .append(PW_USER_TABLE)
                    .append(SPACE).append(ADD_COLUMN).append(SPACE)
                    .append(PW_USER_SUB_STATUS).append(SPACE).append(TEXT).append(SPACE)
                    .toString());
        }
        // fix PW_USER_TABLE

    }

    private static boolean doesColumnExistInTable(SQLiteDatabase db, String table, String columnToCheck) {
        Cursor cursor = null;
        try {
            // Query a row.
            cursor = db.rawQuery("SELECT * FROM " + table + " LIMIT 0", null);

            // getColumnIndex()  will return the index of the column
            // in the table if it exists, otherwise it will return -1
            if (cursor.getColumnIndex(columnToCheck) != -1) {
                // the column exists
                return true;
            } else {
                // the column does not exist
                return false;
            }

        } catch (SQLiteException Exp) {
            // Something went wrong with SQLite.
            // If the table exists and your query was good,
            // the problem is likely that the column doesn't exist in the table.
            // I don't think we will hit this case.
            return false;
        } finally {
            //close the cursor
            if (cursor != null) cursor.close();
        }
    }
}