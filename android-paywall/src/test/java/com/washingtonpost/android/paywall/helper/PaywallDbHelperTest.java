package com.washingtonpost.android.paywall.helper;

import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.ADD_COLUMN;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.ALTER_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.CLOSING_PARENTHESIS;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.COMMA;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.INTEGER_DEFAULT_ZERO;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.OPENING_PARENTHESIS;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ACCESS_EXPIRY;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ACCESS_LEVEL;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ACCESS_PURCHASE_LOCATION;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ARTICLE_LINK;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ARTICLE_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_ARTICLE_TITLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.CREATE_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_DISPLAY_NAME;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_LOGGED_IN;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_PARTNER_ID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_PARTNER_NAME;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_PASSWORD;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_EXISTING_SUB;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_EXPIRY;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_RECEIPT_INFO;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_RECEIPT_NUMBER;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_PRODUCT_ID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_STORE_UID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_SYNCED;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_TRANSACTION_DATE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_UPGRADE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_VALIDITY;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_SUBSCRIPTION_VERIFIED;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_USER_CREDITCARD_EXPIRED;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_USER_ID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_USER_SIGNED_IN_THROUGH;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_USER_SUB_STATUS;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_USER_TABLE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.PW_UUID;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.SPACE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.SPACE_TEXT_COMMA_SPACE;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.TEXT;
import static com.washingtonpost.android.paywall.helper.PaywallDbHelper.TEXT_NOT_NULL;

/**
 * Created by elamgodilj on 1/30/17.
 */
public class PaywallDbHelperTest {


    @org.junit.Test
    public void testReplaceStringWithStringBuilder() {

        getArticleCreationTableStringTest();

        getUserCreationTableStringTest();

        getSubscriptionCreationTableStringTest();

        onUpgradeTableStringTest();

    }

    private void getArticleCreationTableStringTest() {
        String string = "CREATE TABLE " + PW_ARTICLE_TABLE + " (" + PW_ARTICLE_LINK + " TEXT NOT NULL, " + PW_ARTICLE_TITLE + " TEXT NOT NULL)";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CREATE_TABLE)
                .append(PW_ARTICLE_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_ARTICLE_LINK).append(SPACE).append(TEXT_NOT_NULL).append(COMMA).append(SPACE)
                .append(PW_ARTICLE_TITLE).append(SPACE).append(TEXT_NOT_NULL)
                .append(CLOSING_PARENTHESIS);

        System.out.println(string);
        System.out.println(stringBuilder.toString());

        assert (string.equals(stringBuilder.toString()));
    }

    private void getUserCreationTableStringTest() {

        String string = "CREATE TABLE " + PW_USER_TABLE + " (" + PW_USER_ID + " TEXT, " + PW_PASSWORD + " TEXT, "
                + PW_DISPLAY_NAME + " TEXT, " + PW_UUID + " TEXT, " + PW_LOGGED_IN + " INTEGER DEFAULT 0, "
                + PW_ACCESS_LEVEL + " TEXT, " + PW_ACCESS_EXPIRY + " TEXT, "
                + PW_USER_SIGNED_IN_THROUGH + " TEXT, " + PW_USER_CREDITCARD_EXPIRED + " TEXT, " +
                PW_ACCESS_PURCHASE_LOCATION + " TEXT, " + PW_PARTNER_ID + " TEXT, " +PW_PARTNER_NAME +" TEXT, "+ PW_USER_SUB_STATUS +" TEXT)";


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CREATE_TABLE)
                .append(PW_USER_TABLE)
                .append(SPACE).append(OPENING_PARENTHESIS)
                .append(PW_USER_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PASSWORD).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_DISPLAY_NAME).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_UUID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_LOGGED_IN).append(SPACE).append(INTEGER_DEFAULT_ZERO).append(COMMA).append(SPACE)
                .append(PW_ACCESS_LEVEL).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ACCESS_EXPIRY).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SIGNED_IN_THROUGH).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_CREDITCARD_EXPIRED).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_ACCESS_PURCHASE_LOCATION).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PARTNER_ID).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_PARTNER_NAME).append(SPACE_TEXT_COMMA_SPACE)
                .append(PW_USER_SUB_STATUS).append(SPACE).append(TEXT)
                .append(CLOSING_PARENTHESIS);

        System.out.println(string);
        System.out.println(stringBuilder.toString());


        assert(string.equals(stringBuilder.toString()));
    }

    private void getSubscriptionCreationTableStringTest() {
        String string = "CREATE TABLE " + PW_SUBSCRIPTION_TABLE + " (" +
                PW_SUBSCRIPTION_STORE_UID + " TEXT, " +
                PW_SUBSCRIPTION_PRODUCT_ID + " TEXT, " +
                PW_SUBSCRIPTION_RECEIPT_INFO + " TEXT, " +
                PW_SUBSCRIPTION_RECEIPT_NUMBER + " TEXT, " +
                PW_SUBSCRIPTION_TRANSACTION_DATE + " TEXT, " +
                PW_SUBSCRIPTION_EXPIRY + " TEXT, " +
                PW_SUBSCRIPTION_SYNCED + "  TEXT, " +
                PW_SUBSCRIPTION_VERIFIED + " TEXT, " +
                PW_SUBSCRIPTION_VALIDITY + " TEXT, " +
                PW_SUBSCRIPTION_UPGRADE + " TEXT, " +
                PW_SUBSCRIPTION_EXISTING_SUB + " TEXT)";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CREATE_TABLE)
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
                .append(PW_SUBSCRIPTION_EXISTING_SUB).append(SPACE).append(TEXT).append(CLOSING_PARENTHESIS);

        System.out.println(string);
        System.out.println(stringBuilder.toString());

        assert string.equals(stringBuilder.toString());

    }

    private void onUpgradeTableStringTest() {
        String stringA = "ALTER TABLE " +PW_SUBSCRIPTION_TABLE +" ADD COLUMN " + PW_SUBSCRIPTION_UPGRADE +" TEXT ";
        StringBuilder stringBuilderA = new StringBuilder();
        stringBuilderA.append(ALTER_TABLE).append(PW_SUBSCRIPTION_TABLE).append(SPACE).append(ADD_COLUMN).append(SPACE).append(PW_SUBSCRIPTION_UPGRADE).append(SPACE).append(TEXT).append(SPACE);

        System.out.println(stringA);
        System.out.println(stringBuilderA.toString());
        assert stringA.equals(stringBuilderA.toString());

        String stringB = "ALTER TABLE " +PW_SUBSCRIPTION_TABLE +" ADD COLUMN " + PW_SUBSCRIPTION_EXISTING_SUB + " TEXT ";
        StringBuilder stringBuilderB = new StringBuilder();
        stringBuilderB.append(ALTER_TABLE).append(PW_SUBSCRIPTION_TABLE).append(SPACE).append(ADD_COLUMN).append(SPACE).append(PW_SUBSCRIPTION_EXISTING_SUB).append(SPACE).append(TEXT).append(SPACE);

        System.out.println(stringB);
        System.out.println(stringBuilderB.toString());
        assert stringB.equals(stringBuilderB.toString());


        String stringC = "ALTER TABLE " +PW_USER_TABLE +" ADD COLUMN " + PW_USER_SUB_STATUS +" TEXT ";
        StringBuilder stringBuilderC = new StringBuilder();
        stringBuilderC.append(ALTER_TABLE).append(PW_USER_TABLE).append(SPACE).append(ADD_COLUMN).append(SPACE).append(PW_USER_SUB_STATUS).append(SPACE).append(TEXT).append(SPACE);

        System.out.println(stringC);
        System.out.println(stringBuilderC.toString());
        assert stringC.equals(stringBuilderC.toString());
    }

}