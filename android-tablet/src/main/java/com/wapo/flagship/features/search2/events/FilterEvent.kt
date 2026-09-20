package com.wapo.flagship.features.search2.events

import com.wapo.flagship.features.search2.model.FilterCheckItem
import com.wapo.flagship.features.search2.model.FilterHeaderItem
import com.wapo.flagship.features.search2.model.FilterRadioItem

sealed class FilterEvent {
    class Radio(
        val item: FilterRadioItem,
    ) : FilterEvent()

    class Check(
        val item: FilterCheckItem,
    ) : FilterEvent()

    class ExpandCollapse(
        val item: FilterHeaderItem,
    ) : FilterEvent()

    object Reset : FilterEvent()

    object Close : FilterEvent()

    object Apply : FilterEvent()
}
