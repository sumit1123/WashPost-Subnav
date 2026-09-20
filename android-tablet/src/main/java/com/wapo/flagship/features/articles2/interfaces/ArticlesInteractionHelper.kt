package com.wapo.flagship.features.articles2.interfaces

import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent

/**
 * This click helper is used to propagate various click events from individual view holders to the containing fragment (or a class that implements this interface)
 */
interface ArticlesInteractionHelper {
    /**
     * This is invoked from within the articles screen. E.g. when user taps on "View comments" button or on author names from by-line.
     */
    fun onEventFired(event: ArticleInteractionEvent)
}
