package com.wapo.flagship.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wapo.flagship.features.articles2.dao.Article2Dao
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.typeconverters.*

/**
 * App database component that builds the room database that can be injected anywhere in the app (Since the module is scoped to [@CoreScope])
 */
@Database(entities = [Article2::class], version = 16)
@TypeConverters(
    ItemListTypeConverter::class,
    OmnitureXTypeConverter::class,
    TaxonomyTypeConverter::class,
    EditorPickListTypeConverter::class,
    RendererTypeConverter::class,
    TableOfContentsTypeConverter::class,
    AudioTypeConverter::class,
    TargetingTypeConverter::class,
    SummaryTypeConverter::class,
    ATPQuestionTypeConverter::class,
    FtsCarouselTypeConverter::class,
    AutoRecircCarouselTypeConverter::class,
    DisclaimerInfoTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articlesDao(): Article2Dao
}
