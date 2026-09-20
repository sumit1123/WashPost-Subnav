package com.wapo.flagship.external.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity
class AppWidget(
    @PrimaryKey val appWidgetId: String,
    @ColumnInfo(name = "section_name") val sectionName: String,
    @ColumnInfo(name = "bundle_name") val bundleName: String,
    @ColumnInfo(name = "widget_type") val widgetType: WidgetType,
)

enum class WidgetType {
    WIDGET,
    TABLET_WIDGET,
    FOR_YOU_WIDGET,
}

class AppWidgetTypeConverters {
    @TypeConverter
    fun toWidgetType(value: Int) = enumValues<WidgetType>()[value]

    @TypeConverter
    fun fromWidgetType(value: WidgetType) = value.ordinal
}
