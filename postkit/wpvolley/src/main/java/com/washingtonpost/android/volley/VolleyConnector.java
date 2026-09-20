/* Copyright (c) 2023 The Washington Post. All rights reserved. */
package com.washingtonpost.android.volley;

import com.wapo.android.commons.logs.EventLog;

public interface VolleyConnector {
    void logD(EventLog.Builder eventLogBuilder);

    void logE(EventLog.Builder eventLogBuilder);
}
