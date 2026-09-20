package com.washingtonpost.android.follow.misc

import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.database.model.FollowEntity

object FollowTrackingInfo {
    @JvmField
    var followTracking: FollowTracking = FollowTracking()

    @JvmField
    var authors: List<FollowEntity>? = null
}