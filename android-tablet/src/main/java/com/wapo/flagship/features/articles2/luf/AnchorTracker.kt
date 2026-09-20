package com.wapo.flagship.features.articles2.luf

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.Anchor
import com.wapo.flagship.util.tracking.Measurement

class AnchorTracker(
    items: MutableList<Item>?,
    val onAnchorChanged: (Anchor?) -> Unit,
) : RecyclerView.OnScrollListener() {
    private val anchors: List<IndexedValue<Anchor>>
    private var lastReportedAnchor: Anchor? = null
    private var highestIndex = -1

    init {
        val anchors = mutableListOf<IndexedValue<Anchor>>()
        items?.forEachIndexed { index, item ->
            if (item is Anchor) {
                anchors.add(IndexedValue(index, item))
            }
        }
        this.anchors = anchors
    }

    override fun onScrolled(
        recyclerView: RecyclerView,
        dx: Int,
        dy: Int,
    ) {
        super.onScrolled(recyclerView, dx, dy)
        val lm = recyclerView.layoutManager ?: return
        if (lm !is LinearLayoutManager) {
            return
        }
        if (anchors.isEmpty()) return

        var currentAnchor: Anchor? = null
        var currentAnchorIndex = -1
        val currentItem = lm.findFirstCompletelyVisibleItemPosition()
        anchors.forEachIndexed { index, anchor ->
            if (currentItem >= anchor.index) {
                currentAnchor = anchor.value
                currentAnchorIndex = index
            }
        }
        reportCurrentAnchor(currentAnchor, currentAnchorIndex)
    }

    private fun reportCurrentAnchor(
        anchor: Anchor?,
        index: Int,
    ) {
        if (lastReportedAnchor != anchor) {
            if ((index == 0 || index == 4) && index > highestIndex) {
                Measurement.trackLiveUpdateScroll((index + 1).toString())
            }
            highestIndex =
                when {
                    index > highestIndex -> index
                    else -> highestIndex
                }
            lastReportedAnchor = anchor
            onAnchorChanged(anchor)
        }
    }
}
