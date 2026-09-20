/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wapo.flagship.features.articles2.dao.Article2Dao
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.typeconverters.*

@Database(entities = [Article2::class], version = 1)
@TypeConverters(
    ItemListTypeConverter::class,
    OmnitureXTypeConverter::class,
    TaxonomyTypeConverter::class,
    EditorPickListTypeConverter::class,
    RendererTypeConverter::class,
    TableOfContentsTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articlesDao(): Article2Dao
}