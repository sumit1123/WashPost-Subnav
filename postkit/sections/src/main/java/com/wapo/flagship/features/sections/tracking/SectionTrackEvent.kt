package com.wapo.flagship.features.sections.tracking

import com.wapo.flagship.features.grid.Tracking

sealed class SectionTrackEvent {

    data class PageView(val displayName: String?, val tracking: Tracking?) : SectionTrackEvent()

    data class OnpageTap(val tracking: Tracking?) : SectionTrackEvent()

    data class AudioInteraction(val avName: String?, val touchpoint: String?, val miscellany: String?, val itemType: String?) : SectionTrackEvent()

    data class AudioStart(val avName: String?, val touchpoint: String?, val miscellany: String?, val itemType: String?, val id: String?) : SectionTrackEvent()

}