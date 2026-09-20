package com.wapo.flagship.features.articles2.tracking

import com.wapo.android.commons.engagement.EngagementTrace
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.sections.utils.JTidTracker
import com.wapo.flagship.util.JUcidTracker
import com.wapo.flagship.util.tracking.Evars
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.MeasurementMap
import java.util.UUID

class ArticlePageEngagementTrace(
    id: String = UUID.randomUUID().toString(),
    startTimeMillis: Long? = null,
    endTimeMillis: Long? = null,
) : EngagementTrace(id, startTimeMillis, endTimeMillis) {

    var eventsMap = MeasurementMap()
    private set

    fun constructAnalyticsEvents(
        firebaseTrackingInfo: FirebaseTrackingInfo?,
        firebaseTrackingHelperData: FirebaseTrackingHelperData?=null
    ) {
        if (firebaseTrackingInfo == null) return

        firebaseTrackingInfo.contentWeight?.apply {
            Measurement.setTetroAttributes(this)
        }

        firebaseTrackingInfo.omnitureX?.toTrackingInfo()?.let { info ->
            eventsMap[Evars.ARC_ID.variable] = info.arcId
            eventsMap[Evars.CONTENT_TYPE.variable] = info.contentType
            eventsMap[Evars.CONTENT_SECTION.variable] = info.channel
            eventsMap[Evars.CONTENT_SUBSECTION.variable] = info.subSection
            eventsMap[Evars.PAGE_NAME.variable] = info.pageName
            eventsMap[Evars.AUTHOR_ID.variable] = info.authorId
        }
        eventsMap[Evars.PREV_PAGE.variable] = Measurement.getPreviousMap().getEvar(Evars.PREV_PAGE.variable)
        eventsMap[Evars.TAB_NAME.variable] = Measurement.getPreviousMap().getEvar(Evars.TAB_NAME.variable)
        eventsMap[Evars.J_UCID.variable] = JUcidTracker.jUcid
        eventsMap[Evars.J_TID.variable] = JTidTracker.currentJTid

        firebaseTrackingInfo.contentWeight?.let {
            eventsMap[Evars.TETRO_CONTENT_WEIGHT.variable] = it
        }
    }

    override fun isValid(): Boolean = super.isValid() && eventsMap.isNotEmpty()

    fun addNavigationBehavior(navigationBehavior: String) {
        eventsMap[Evars.NAVIGATION_BEHAVIOR.variable] = navigationBehavior
    }
}