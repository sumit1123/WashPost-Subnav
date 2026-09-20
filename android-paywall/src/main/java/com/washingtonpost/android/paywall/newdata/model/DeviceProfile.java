package com.washingtonpost.android.paywall.newdata.model;

import java.util.Map;

public interface DeviceProfile {
    String getId();
    Map<String, String> profileToMap();
}
