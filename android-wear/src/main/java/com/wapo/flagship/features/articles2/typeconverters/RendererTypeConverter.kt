/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.wapo.flagship.features.articles2.models.Renderer

/**
 * [TypeConverter] for [Renderer] to be able to serialize and deserialize it
 */
class RendererTypeConverter {

    @TypeConverter
    fun toJson(renderer: Renderer?): String? {
        if (renderer == null)
            return null
        return renderer.value
    }

    @TypeConverter
    fun fromJson(value: String?): Renderer? {
        if (value == null)
            return null
        return Renderer.values().find { it.value == value }
    }

}