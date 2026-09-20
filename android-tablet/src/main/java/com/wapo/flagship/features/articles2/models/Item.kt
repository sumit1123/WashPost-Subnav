package com.wapo.flagship.features.articles2.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder.PlaceHolderState
import com.wapo.flagship.features.articles2.utils.BreakPoints
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner

@JsonClass(generateAdapter = true)
open class Item(
    @Json(name = "type")
    open val type: String? = null,
    @Json(name = "hideVersion")
    open val hideVersion: Int? = null,
    @Json(name = "showVersion")
    open val showVersion: Int? = null,
    @Json(name = "arcId")
    open val arcId: String? = null,
    // this field must be "var" because Moshi thinks this is the way...
    // see more:
    // https://github.com/square/moshi/issues/700
    // https://github.com/square/moshi/issues/577
    @Json(name = "group")
    open var group: String? = null,
    @Json(name = "breakpoints")
    var breakpoints: List<String?>? = null,
) {
    var state: ItemState? = null
    var layoutSpec: BreakPoints.LayoutSpec? = null
    var invisible: Boolean = false
    var isLowDataModeEnable: Boolean = false
    var isItemAlreadyShowed: Boolean = false

    @Transient
    var lowDataModeLive: LiveData<LowDataBanner>? = null

    @Transient
    var placeHolderState: MutableLiveData<PlaceHolderState>? = null

    @Transient
    var pageName: String? = null

    override fun equals(other: Any?): Boolean =
        if (other as? Item == null) {
            false
        } else {
            other === this
        }

    override fun hashCode(): Int {
        var result = this.hashCode() + state.hashCode() + layoutSpec.hashCode()
        result *= 31
        return result
    }
}
