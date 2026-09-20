package com.wapo.flagship.features.articles2.paywall

import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.map.models.MapWallType
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.features.tetro.Prompt
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.util.PaywallConstants

/**
 * This class consists of events that will fire the paywall (now or delayed) from articles activity.
 */
sealed class WallUiEvent {
    /**
     * This is to show the paywall immediately for users that have no sub.
     * This will most likely be triggered by some type of user action (e.g. bookmark/audio click)
     */
    class ShowPaywall(
        val type: PaywallConstants.WallType,
    ) : WallUiEvent()

    /**
     * This is to show the paywall based on a Tetro response that includes an action code,
     * which we are mapping to a name in the config.
     */
    class ShowPaywallByName(
        val wallName: String,
    ) : WallUiEvent()

    /**
     * This is to show the Regwall based on Tetro Response
     */
    class ShowRegwall(
        val wallName: String,
        val wallType: PaywallConstants.WallType,
    ) : WallUiEvent()

    /**
     * This is to show the Softwall based on Tetro Response
     */
    class ShowSoftwall(
        val wallName: String,
        val wallType: PaywallConstants.WallType,
    ) : WallUiEvent()

    /**
     * Show gift wall
     */
    class ShowGiftWall(
        val giftState: GiftState,
    ) : WallUiEvent()

    /**
     * Show MAP wall
     */
    class ShowMapWall(
        val mapWallType: MapWallType,
        val mapWallPrompt: Prompt,
    ) : WallUiEvent()

    /**
     * This will show a delayed paywall on an article when user has hit the meter limit and
     * the article does not have content_restriction_code = free.
     */
    class StartDelayedWall(
        // The type of wall shown. Also tied to entry point
        val type: PaywallConstants.WallType,
        // Article details that need to be passed to tetro
        val articleStub: ArticleStub,
        // Article related analytics data
        val trackingInfo: OmnitureX?,
        // If referrer should be passed to Tetro
        val passReferrer: Boolean = true,
    ) : WallUiEvent()

    object StopDelayedWall : WallUiEvent()

    object DismissWall : WallUiEvent()
}
