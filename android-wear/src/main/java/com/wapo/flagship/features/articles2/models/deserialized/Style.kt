/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized

enum class Style(private val value: String) {
    BRIEFS("briefs"),
    DEFAULT("");

    companion object {
        fun getValue(input: String?): Style {
            for (b in values()) {
                if (b.value.equals(input, ignoreCase = true)) {
                    return b
                }
            }
            return DEFAULT
        }
    }
}