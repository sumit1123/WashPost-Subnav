package com.wapo.flagship.features.amazonunification.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.wapo.flagship.features.amazonunification.database.model.UserPreferenceEntry

/**
 * [UserPreferenceEntry]'s Dao
 */
@Dao
interface UserPreferenceEntryDao {
    @Query("SELECT * FROM UserPreferenceEntry")
    fun getEntries(): List<UserPreferenceEntry>
}
