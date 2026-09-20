/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized

/**
 * Recirculation module data type with section name.
 */
enum class RecirculationType(var sectionName: String) {
    MOST_READ("Most read"),
    FOR_YOU("More for you")
}