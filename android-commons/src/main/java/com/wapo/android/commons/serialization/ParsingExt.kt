package com.wapo.android.commons.serialization

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJsonToList(json: String) =
    fromJson<T>(json, object : TypeToken<T>() {}.type)