/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

import android.os.Parcel
import android.os.Parcelable

data class Label(
        private val text: String?,
        val alignment: Alignment?,
        val textColor: String?,
        val backgroundColor: String?,
        val horizontalPadding: String?,
        val style: LabelStyle?,
        val url: String?,
        /* secondary label */
        val secondaryStyle: LabelStyle?,
        val secondaryText: String?,
        private val hasArrow: Boolean?
) : Parcelable {

    private var cachedText: String? = null

    constructor(primaryText: String?, secondaryText: String?, url: String?, alignment: Alignment?)
            : this(primaryText, alignment, null, null, null, LabelStyle.NORMAL_SMALL, url, LabelStyle.LIGHT_SMALL, secondaryText, false)

    /**
     * Remove this getter when Web API is ready
     */
    fun getText(): String? {
        if (cachedText == null) {
            cachedText = if (secondaryText != null) text?.replace(" | $secondaryText", "") else text
        }
        return cachedText
    }

    fun isArrowVisible(): Boolean = if (hasArrow == null) false else hasArrow

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.apply {
            writeString(text)
            writeInt(alignment?.ordinal ?: -1)
            writeString(textColor)
            writeString(backgroundColor)
            writeString(horizontalPadding)
            writeInt(style?.ordinal ?: -1)
            writeString(url)
            writeInt(secondaryStyle?.ordinal ?: -1)
            writeString(secondaryText)
            writeInt(if (hasArrow == null) {
                -1
            } else {
                if (hasArrow) 1 else 0
            })
        }
    }

    override fun describeContents() = 1

    companion object CREATOR : Parcelable.Creator<Label> {
        override fun createFromParcel(source: Parcel) = source.run {
            Label(
                    readString(),
                    readInt().run {
                        if (this >= 0 && this < Alignment.values().size) Alignment.values()[this] else null
                    },
                    readString(),
                    readString(),
                    readString(),
                    readInt().run {
                        if (this >= 0 && this < LabelStyle.values().size) LabelStyle.values()[this] else null
                    },
                    readString(),
                    readInt().run {
                        if (this >= 0 && this < LabelStyle.values().size) LabelStyle.values()[this] else null
                    },
                    readString(),
                    readInt().run {
                        if (this == -1) {
                            null
                        } else {
                            if (this == 1) true else false
                        }
                    }
            )
        }

        override fun newArray(size: Int) = arrayOfNulls<Label?>(size)
    }
}

enum class LabelStyle {
    LABEL_BTN,
    LABEL_BAR,
    NORMAL,
    KICKER,
    HIGHLIGHT,
    LIGHT,
    LIGHT_SMALL,
    NORMAL_SMALL
}