/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.models

enum class MenuAction(val position: Int) {
    ACTION_TOP_STORIES(0),
    ACTION_ALERTS(1),
    ACTION_EXTRA_SECTION(2),
    ACTION_SETTINGS(3);

    companion object {
        fun fromInt(position: Int) = values().first { it.position == position }
    }
}