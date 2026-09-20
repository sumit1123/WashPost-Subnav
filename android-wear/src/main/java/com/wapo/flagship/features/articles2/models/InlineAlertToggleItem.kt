/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models

/**
 * Data class used by InlineAlertToggleViewHolder
 * @param type the item type
 * @param topicDisplayName the value for the topic that is displayed in the UI i.e. "Health and Science"
 * @param topicKey the key that corresponds to the topic in the alert settings. Used to turn on the appropriate alert when the toggle changes
 */
data class InlineAlertToggleItem(
    override val type: String? = "InlineAlertToggleType",
    val topicDisplayName: String?,
    val topicKey: String?
) : Item(type)
