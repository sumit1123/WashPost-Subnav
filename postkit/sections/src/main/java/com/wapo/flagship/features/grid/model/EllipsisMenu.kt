package com.wapo.flagship.features.grid.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class EllipsisMenu : Parcelable {
    object Carousel : EllipsisMenu()
    object ActionButton : EllipsisMenu()
    object EllipsisButton : EllipsisMenu()
    object AudioPlayerEllipsisButton : EllipsisMenu()
    object CurrentPlayingViewEllipsisButton : EllipsisMenu()
    object RemoveActionButton : EllipsisMenu()
    object VideoActionButton : EllipsisMenu()
    object ForYouEllipsisButton : EllipsisMenu()
}
