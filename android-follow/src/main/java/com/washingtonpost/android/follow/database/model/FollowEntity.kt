package com.washingtonpost.android.follow.database.model

import androidx.room.*

/***
 * As part of implementing follow services introduced 3 new column
 * @isFollowing : Is TRUE when the User follows an author and turns to FALSE when unfollows
 * @isSynced : Is the status Flag that we set it to 1 when the sync is successful with the following services
 * @isAuthorMetaDataAvailable : This flag is to check if the author meta data available and update if its not in the Author Entity
 */

@Entity(indices = [
    Index(value = ["last_modified"])])
data class FollowEntity(
    @PrimaryKey @ColumnInfo(name = "author_id") val authorId: String,
    @ColumnInfo(name = "last_modified") val lmt: Long,
    @ColumnInfo(name = "isFollowing", defaultValue = "1") val isFollowing: Boolean? = true,
    @ColumnInfo(name = "isSynced", defaultValue = "0") val isSynced : Int? =0,
    @ColumnInfo(name = "isAuthorMetaDataAvailable", defaultValue = "1") var isAuthorMetaDataAvailable: Int?=1)