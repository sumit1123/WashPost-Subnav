/*
 *  Copyright (c) 2023 The Washington Post. All rights reserved.
 *
 */

package com.washingtonpost.android.paywall.newdata.response

import com.google.gson.annotations.SerializedName

data class SubLink(
    @SerializedName("status")
    val status: String?,
    @SerializedName("message")
    val message: String?
)
