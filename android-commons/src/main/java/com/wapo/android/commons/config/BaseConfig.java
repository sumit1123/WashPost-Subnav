package com.wapo.android.commons.config;

import android.content.Context;

import com.google.gson.Gson;

import org.json.JSONException;


public abstract class BaseConfig {

    protected int configVersion = 1;

    public static BaseConfig configFromJsonString(Context context, String configJsonStr, Class<? extends BaseConfig> clsName) throws JSONException {
        Gson gson = new Gson();
        BaseConfig config = (BaseConfig) gson.fromJson(configJsonStr, clsName);
        return config;
    }
}
