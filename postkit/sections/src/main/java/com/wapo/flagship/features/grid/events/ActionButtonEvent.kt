package com.wapo.flagship.features.grid.events

import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.json.TrackingInfo

sealed class ActionButtonEvent {

    class Listen(val audioMediaConfig: AudioMediaConfig) : ActionButtonEvent()

    class Share(val headline: String?, val byline: String?, val url: String?) : ActionButtonEvent()

    class Menu(val ellipsisActionItem: EllipsisActionItem) : ActionButtonEvent()

    class Save(val ellipsisActionItem: EllipsisActionItem) : ActionButtonEvent()

    class Remove(val ellipsisActionItem: EllipsisActionItem) : ActionButtonEvent()

    class Comments(val url: String, val storyId: String, val storyTitle: String?, val trackingInfo: TrackingInfo? = null): ActionButtonEvent()
}