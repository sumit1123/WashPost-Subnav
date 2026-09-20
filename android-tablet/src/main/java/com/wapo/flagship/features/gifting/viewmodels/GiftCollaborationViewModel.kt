package com.wapo.flagship.features.gifting.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.gifting.events.GiftCollabEvent
import com.wapo.flagship.features.gifting.tracking.GiftTrackingDetails
import com.wapo.flagship.util.coroutines.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * This vm is specific to gift sender flow.
 */
@HiltViewModel
class GiftCollaborationViewModel
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        var giftArticleUrl: String? = null
        var isActionButton: Boolean = false
        var appSection: String? = null

        var trackingInfo: OmnitureX? = null

        private val _giftCollabEvent: LiveEvent<GiftCollabEvent> = LiveEvent()
        val giftCollabEvent: LiveData<GiftCollabEvent> = _giftCollabEvent

        /**
         * This is used to propagate bookmark button tap events happened on specific articles.
         */
        private val _giftTrackingEvent: LiveEvent<Pair<String, GiftTrackingDetails>> = LiveEvent()
        val giftTrackingEvent: LiveData<Pair<String, GiftTrackingDetails>> = _giftTrackingEvent

        /**
         * Dispatches event to proceed with bookmark save or delete functionality.
         */
        fun dispatchGiftTrackingEvent(giftTrackingDetails: GiftTrackingDetails) {
            val url = giftArticleUrl
            url?.let {
                _giftTrackingEvent.value = Pair(it, giftTrackingDetails)
            }
        }

        fun dispatchCollabEvent(event: GiftCollabEvent) {
            _giftCollabEvent.value = event
        }
    }
