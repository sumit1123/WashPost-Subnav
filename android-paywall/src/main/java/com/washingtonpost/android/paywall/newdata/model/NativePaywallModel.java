package com.washingtonpost.android.paywall.newdata.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kattim on 8/23/17.
 */

public class NativePaywallModel {

    public final String message;
    public final String heading;
    public final String offer1Label;
    public final String offer1Price;
    public final String offer1Summary;
    public final String offer1ButtonText;
    public final String offer1ButtonColor;
    public final String offer1SKU;
    public final String offer2Label;
    public final String offer2Price;
    public final String offer2Summary;
    public final String offer2ButtonText;
    public final String offer2ButtonColor;
    public final String offer2SKU;
    public final String signInText;
    public final String finePrint;

    public NativePaywallModel(String message, String heading, String offer1Label, String offer1Price,
                              String offer1Summary, String offer1ButtonText, String offer1ButtonColor,
                              String offer1SKU, String offer2Label, String offer2Price, String offer2Summary,
                              String offer2ButtonText, String offer2ButtonColor, String offer2SKU,
                              String signInText, String finePrint) {
        this.message = message;
        this.heading = heading;
        this.offer1Label = offer1Label;
        this.offer1Price = offer1Price;
        this.offer1Summary = offer1Summary;
        this.offer1ButtonText = offer1ButtonText;
        this.offer1ButtonColor = offer1ButtonColor;
        this.offer1SKU = offer1SKU;
        this.offer2Label = offer2Label;
        this.offer2Price = offer2Price;
        this.offer2Summary = offer2Summary;
        this.offer2ButtonText = offer2ButtonText;
        this.offer2ButtonColor = offer2ButtonColor;
        this.offer2SKU = offer2SKU;
        this.signInText = signInText;
        this.finePrint = finePrint;
    }

    public static NativePaywallModel fromJSONObject(JSONObject config) throws JSONException {
        String message = config.has("message")? config.getString("message"): null ;
        String heading = config.has("heading")? config.getString("heading"): null ;
        String offer1Label = config.has("offer1Label") ? config.getString("offer1Label") : null;
        String offer1Price = config.has("offer1Price") ? config.getString("offer1Price") : null;
        String offer1Summary = config.has("offer1Summary") ? config.getString("offer1Summary") : null;
        String offer1ButtonText = config.has("offer1ButtonText") ? config.getString("offer1ButtonText") : null;
        String offer1ButtonColor = config.has("offer1ButtonColor") ? config.getString("offer1ButtonColor") : null;
        String offer1SKU = config.has("offer1SKU") ? config.getString("offer1SKU") : "wp.classic.basic";
        String offer2Label = config.has("offer2Label") ? config.getString("offer2Label") : null;
        String offer2Price = config.has("offer2Price") ? config.getString("offer2Price") : null;
        String offer2Summary = config.has("offer2Summary") ? config.getString("offer2Summary") : null;
        String offer2ButtonText = config.has("offer2ButtonText") ? config.getString("offer2ButtonText") : null;
        String offer2ButtonColor = config.has("offer2ButtonColor") ? config.getString("offer2ButtonColor") : null;
        String offer2SKU = config.has("offer2SKU") ? config.getString("offer2SKU") : "monthly_all_access";
        String signInText = config.has("signInText") ? config.getString("signInText") : null;
        String finePrint = config.has("finePrint") ? config.getString("finePrint") : null;

        return new NativePaywallModel(message, heading, offer1Label, offer1Price, offer1Summary,
                offer1ButtonText, offer1ButtonColor, offer1SKU, offer2Label, offer2Price, offer2Summary,
                offer2ButtonText,offer2ButtonColor, offer2SKU, signInText, finePrint);
    }
}
