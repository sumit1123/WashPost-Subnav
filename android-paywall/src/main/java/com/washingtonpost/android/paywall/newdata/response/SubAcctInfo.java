package com.washingtonpost.android.paywall.newdata.response;

import com.google.gson.annotations.SerializedName;

public class SubAcctInfo {

    @SerializedName("priceFlag")
    String priceFlag;
    @SerializedName("duration")
    String duration;
    @SerializedName("startDate")
    String startDate;

    public String getPriceFlag() {
        return priceFlag;
    }

    public String getDuration() {
        return duration;
    }

    public String getStartDate() {
        return startDate;
    }

}
