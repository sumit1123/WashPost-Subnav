package com.wapo.android.commons.engagement

object EngagementTrackerHelper {

    private val pageTracker = mutableMapOf<String, EngagementTrackerHelperData>()

    fun trackPage(key: String, pageEngagementTrace: PageEngagementTrace, currentOwner: String) {
        if (!pageTracker.containsKey(key)) {
            pageEngagementTrace.startTrace()
            pageTracker[key] = EngagementTrackerHelperData(pageEngagementTrace, mutableListOf(currentOwner))
        } else {
            pageTracker[key]?.let { newTrack ->
                newTrack.ownerList.add(currentOwner)
                pageTracker[key] = newTrack
            }
        }
    }

    fun stopPageTrack(key: String, currentOwner: String, force: Boolean = false): PageEngagementTrace? {
        return pageTracker[key]?.let { data ->
            if (data.ownerList.last() == currentOwner || force) {
                pageTracker.remove(key)
                data.pageEngagementTrace.stopTrace()
                data.pageEngagementTrace
            } else null
        }
    }

    fun clear() {
        pageTracker.forEach { (_, value) ->
            value.pageEngagementTrace.stopTrace()
        }
        pageTracker.clear()
    }
}

data class EngagementTrackerHelperData(
    val pageEngagementTrace: PageEngagementTrace,
    val ownerList: MutableList<String> = mutableListOf()
)
