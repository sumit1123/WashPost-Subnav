package com.washingtonpost.android.paywall.newdata.response;

import com.google.gson.annotations.SerializedName;

public class RevokeResponse {

    @SerializedName("reqId")
    private String reqId;
    @SerializedName("error")
    private String error;
    @SerializedName("error_description")
    private String errorDescription;

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
