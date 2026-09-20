/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wpds.utils

import android.content.Context

class IconUtils(private val context: Context) {
    fun getDrawableId(iconName: String?): Int? {
        iconName ?: return null
        val normalizedIconName = iconName.replace("-", "_")
        val id = context.resources.getIdentifier(normalizedIconName, "drawable", context.packageName)
        return if (id == 0) { null } else { id }
    }
}
