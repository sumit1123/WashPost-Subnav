/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.models

data class MenuItem(
    override val type: String = "Menu",
    val sectionName: String,
    val iconResId: Int
): HomepageItem(type)
