package com.wapo.flagship.features.newsletter.repo.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey

@Entity(
    indices = [
        Index(value = ["last_modified"]),
    ],
)
data class NewslettersEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,    // derived: id ?: list
    @ColumnInfo(name = "id") val id: String?,
    @ColumnInfo(name = "list") val list: String?,
    @ColumnInfo(name = "last_modified") val lmt: Long,
    @ColumnInfo(name = "isEnrolled", defaultValue = "1") var isEnrolled: Boolean? = true,
    @ColumnInfo(name = "isSynced", defaultValue = "0") var isSynced: Int? = 0,
) {
    @Ignore
    constructor(
        key: NewslettersKey,
        lmt: Long,
        isEnrolled: Boolean? = true,
        isSynced: Int? = 0,
    ) : this(
        key = when (key) {
            is NewslettersKey.Id -> key.value
            is NewslettersKey.List -> key.value
        },
        id = (key as? NewslettersKey.Id)?.value,
        list = (key as? NewslettersKey.List)?.value,
        lmt = lmt,
        isEnrolled = isEnrolled,
        isSynced = isSynced
    )

    val newslettersKey: NewslettersKey
        get() = when {
            id != null -> NewslettersKey.Id(id)
            list != null -> NewslettersKey.List(list)
            else -> throw IllegalStateException("Either id or list must exist")
        }
}
