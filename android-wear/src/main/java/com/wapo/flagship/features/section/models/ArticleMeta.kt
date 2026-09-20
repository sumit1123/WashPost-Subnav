/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.models

import android.os.Parcel
import android.os.Parcelable

data class ArticleMeta(
    val headline: String,
    val contentUrl: String,
    val blurb: String?,
    val timestamp: String? = null,
    val byLine: String? = null,
    val imageUrl: String? = null,
    val lastModified: Long = 0L,
    val bypassCache: Boolean = false
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readInt() != 0
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(headline)
        parcel.writeString(contentUrl)
        parcel.writeString(blurb)
        parcel.writeString(timestamp)
        parcel.writeString(byLine)
        parcel.writeString(imageUrl)
        parcel.writeLong(lastModified)
        parcel.writeInt(if (bypassCache) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ArticleMeta> {
        override fun createFromParcel(parcel: Parcel): ArticleMeta {
            return ArticleMeta(parcel)
        }

        override fun newArray(size: Int): Array<ArticleMeta?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArticleMeta) return false
        return contentUrl == other.contentUrl
    }

    override fun hashCode(): Int {
        return contentUrl.hashCode()
    }

}