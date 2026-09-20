// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.handlers

import android.net.Uri
import com.iterable.iterableapi.IterableActionContext
import com.iterable.iterableapi.IterableActionSource
import com.iterable.iterableapi.IterableUrlHandler
import com.wapo.android.commons.iterable.toBundle
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.TrafficSource

class IterableAppUrlHandler(
    private val deepLinksProcessor: DeepLinksProcessor,
    private val inAppHandler: IterableAppInAppHandler
) : IterableUrlHandler {

    override fun handleIterableURL(uri: Uri, actionContext: IterableActionContext): Boolean {
        Logger.d(TAG, "Iterable, UrlHandler, $uri")
        val attributionInfo = inAppHandler.getAttributionInfo()
        if (actionContext.source == IterableActionSource.IN_APP) {
            inAppHandler.onMessageButtonClicked(uri.toString())
        }
        if (actionContext.source == IterableActionSource.PUSH) {
            // IterableTrampolineActivity activity is not opening App's MainActivity from
            // Push actions. So delegating links from the pushes to the app to get them processed
            // by the MainActivity.
            deepLinksProcessor.getCurrentActivity()?.let {
                val intent = IntentHelper.getDeepLinkDelegatorActivityIntent(it).apply {
                    data = Uri.parse(uri.toString())
                }
                it.startActivity(intent)
            }
        } else if (actionContext.source == IterableActionSource.APP_LINK) {
            // delegate to [DeepLinksProcessor] instead of letting Iterable handle it and return true.
            Measurement.mark(TrafficSource.fromDeepLinkUri(uri))
            Measurement.trackDeepLinkOpen(uri)
            deepLinksProcessor.processAsync(
                urlParser = URLParser(uri.toString()),
                bundle = attributionInfo.toBundle(),
                sourceType = DeepLinksProcessor.SourceType.ITERABLE,
            )
        } else {
            // delegate to [DeepLinksProcessor] instead of letting Iterable handle it and return true.
            deepLinksProcessor.processAsync(
                urlParser = URLParser(uri.toString()),
                bundle = attributionInfo.toBundle(),
                sourceType = DeepLinksProcessor.SourceType.ITERABLE,
            )
        }
        return true
    }

    companion object {
        private const val TAG = "IterableAppUrlHandler"
    }
}
