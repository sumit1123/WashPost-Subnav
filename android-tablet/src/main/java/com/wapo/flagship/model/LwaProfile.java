package com.wapo.flagship.model;

import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.util.PrefUtils;
import com.washingtonpost.android.paywall.newdata.model.DeviceProfile;
import java.util.HashMap;
import java.util.Map;

public class LwaProfile implements DeviceProfile {

    public final String lwaId;
    public final String email;
    public final String userName;
    public final String postalCode;

    public LwaProfile(String lwaId, String email, String userName, String postalCode) {
        this.lwaId = lwaId;
        this.email = email;
        this.userName = userName;
        this.postalCode = postalCode;
    }

    @Override
    public String toString() {
        return "LwaProfile{" +
                "lwaId='" + lwaId + '\'' +
                ", email='" + email + '\'' +
                ", userName='" + userName + '\'' +
                ", postalCode='" + postalCode + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return lwaId;
    }

    @Override
    public Map<String, String> profileToMap() {

        if ("cancel".equalsIgnoreCase(lwaId)) {
            return null;
        }

        Map<String, String> paramsMap = new HashMap<>();

        paramsMap.put("lwaId", lwaId);
        paramsMap.put("email", email);
        paramsMap.put("userName", userName);
        paramsMap.put("postalCode", postalCode);

        PrefUtils.setPrefDeviceProfileSent(FlagshipApplication.getInstance(), true);

        return paramsMap;
    }
}

