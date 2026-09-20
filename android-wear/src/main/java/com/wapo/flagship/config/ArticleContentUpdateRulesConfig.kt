/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.config

import com.google.gson.annotations.SerializedName

/**
 * [timeout] indicates the max time spent on the network request before
 * showing the webview version of the article in the card.
 */
data class ArticleContentUpdateRulesConfig(
    @SerializedName("timeout")
    val timeout: Long = 5000L
)
