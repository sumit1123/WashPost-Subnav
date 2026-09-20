// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.habittiles

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wapo.flagship.content.ContentManager
import com.washingtonpost.android.paywall.PaywallService
import rx.Observable
import rx.Subscription

class HabitTilesPlugin(
    private val contentManagerObs: Observable<ContentManager>,
    private val onHabitTilesFetchData: (shouldBypassCache: Boolean) -> Unit,
    private val onUserHistoryAddClickedToHabitTile: (link: String?) -> Unit,
) : DefaultLifecycleObserver {
    private var loggedInState: Boolean? = null
    private var cmObsSubscription: Subscription? = null
    private var cmApiCallsSubscription: Subscription? = null

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        cmObsSubscription?.unsubscribe()
        cmObsSubscription =
            contentManagerObs.subscribe {
                cmApiCallsSubscription?.unsubscribe()
                cmApiCallsSubscription =
                    it.canMakeApiCalls().subscribe { allowed ->
                        if (!allowed) return@subscribe
                        onHabitTilesFetchData(shouldBypassCache())
                    }
            }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        cmApiCallsSubscription?.unsubscribe()
        cmApiCallsSubscription = null
        cmObsSubscription?.unsubscribe()
        cmObsSubscription = null
    }

    private fun shouldBypassCache(): Boolean =
        PaywallService.getInstance()?.isWpUserLoggedIn?.let { loggedIn ->
            if (loggedIn != loggedInState) {
                loggedInState = loggedIn
                true
            } else {
                false
            }
        } ?: false
    
    fun addClickedToHabitTileViewedItem(link: String?) {
        onUserHistoryAddClickedToHabitTile(link)
    }
}
