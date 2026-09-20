package com.wapo.flagship.features.amazonunification.database.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class to migrate Rainbow's UserPreferenceEntry (Saved Stories) table from
 * [com.wapo.flagship.features.amazonunification.database.RainbowAppDatabase] Database to Classic
 */
@Entity
class UserPreferenceEntry(
    @PrimaryKey
    @ColumnInfo(name = "fullUrl")
    val fullUrl: String,
    @ColumnInfo(name = "hash", defaultValue = "0") @NonNull val hash: Int = 0,
    @ColumnInfo(name = "localFilePath") val localFilePath: String?,
    @ColumnInfo(name = "binary", typeAffinity = ColumnInfo.BLOB) val binary: ByteArray?,
    @ColumnInfo(name = "type") val type: Int?,
    @ColumnInfo(name = "savedType") val savedType: Int?,
    @ColumnInfo(name = "popularity", defaultValue = "0") val popularity: Int? = 0,
    @ColumnInfo(name = "last_touched") val lastTouched: Long?,
)
