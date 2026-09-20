/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.models

data class HeaderItem(
    override val type: String = "Header",
    val text: String
): HomepageItem(type)