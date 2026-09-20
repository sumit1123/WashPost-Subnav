package com.washingtonpost.android.volley.toolbox;


import com.washingtonpost.android.volley.VolleyError;

public interface GlobalImageListener {
    void onResponse(Object data, String requestUrl, boolean isImmediate);
    void onErrorResponse(String requestUrl, VolleyError error);
}
