// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.sdk.iterable.handlers.BannerActionHandler
import com.wapo.flagship.sdk.iterable.models.BlockerMessage
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.washingtonpost.android.paywall.helper.PaywallSheetHelper
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import kotlinx.coroutines.launch

class IterablePlugin(
    private val activity: IterableActivity,
    private val iterableActivityViewModel: IterableActivityViewModel,
    paywallSheetHelper: PaywallSheetHelper? = null,
) : DefaultLifecycleObserver {

    private val appCompatActivity = activity as AppCompatActivity
    private val bannerActionHandler =
        BannerActionHandler(paywallSheetHelper, iterableActivityViewModel)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        observeEmbeddedMessagesState()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Updating messages map from Iterable SDK's local cache in onStart call of activities to update
        // local messages. IterableAppEmbeddedUpdateHandler's onMessagesUpdated is not getting
        // called in every case.
        iterableActivityViewModel.updateEmbeddedMessagesMap()
    }

    private fun observeEmbeddedMessagesState() {
        iterableActivityViewModel.viewModelScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            appCompatActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                launch {
                    iterableActivityViewModel.blockerMessagesState.collect { messages ->
                        Logger.d(TAG, "Iterable, observeEmbeddedMessagesState, state=$messages")
                        (activity as? IterableBlockerMessageActivity)?.onIterableBlockerMessageReceived(
                            messages
                        )
                    }
                }
            }
        }
    }

    fun executeBannerAction(
        banner: BannerPaywallMessage?,
        front: String?,
        onBannerClicked: () -> Unit = {},
        onShowArticleSoftwall: ((String) -> Unit)? = null,
        onShowStandardPaywall: ((wallName: String?, wallType: WallType, reason: Int) -> Unit)? = null,
    ) {
        bannerActionHandler.execute(
            banner = banner,
            context = appCompatActivity,
            front = front,
            onBannerClicked = onBannerClicked,
            onShowArticleSoftwall = onShowArticleSoftwall,
            onShowStandardPaywall = onShowStandardPaywall,
        )
    }

    fun dismissBanner(
        banner: BannerPaywallMessage?,
        sectionFront: String?,
    ) {
        bannerActionHandler.dismissBanner(banner, sectionFront)
    }

    fun handleBannerLifecycleEvent(event: BannerLifecycleEvent) {
        bannerActionHandler.handleLifecycleEvent(event)
    }

    interface IterableActivity

    interface IterableBlockerMessageActivity : IterableActivity {
        fun onIterableBlockerMessageReceived(messages: List<BlockerMessage>)
    }

    companion object {
        private const val TAG = "IterablePlugin"
    }
}