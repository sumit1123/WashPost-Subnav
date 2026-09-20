package com.wapo.flagship

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.external.foryouwidget.actions.ArticleClickAction.Companion.FOR_YOU_WIDGET_UPDATE
import com.wapo.flagship.features.alerts.AlertsActivity
import com.wapo.flagship.features.articles2.activities.ArticlesParcel.Companion.builder
import com.wapo.flagship.features.articles2.activities.PUSH_ORIGINATED
import com.wapo.flagship.features.articles2.activities.WIDGET_ORIGINATED
import com.wapo.flagship.features.articles2.repo.DEBUG_ARTICLES_RESOURCES
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.mypost.fragments.MyPost2Fragment
import com.wapo.flagship.features.print.PrintActivity
import com.wapo.flagship.features.settings.SettingsActivity
import com.wapo.flagship.features.settings.contactus.ContactUsActivity
import com.wapo.flagship.features.shared.NativePaywallActivity
import com.wapo.flagship.features.shared.activities.DefaultNativePaywallResultCallbacks
import com.wapo.flagship.features.shared.activities.SearchableArticlesActivity
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.wapomain.MainConstants.ACTION_ASK
import com.wapo.flagship.wapomain.MainConstants.ACTION_FIND
import com.wapo.flagship.wapomain.MainConstants.ACTION_GAMES
import com.wapo.flagship.wapomain.MainConstants.ACTION_LISTEN
import com.wapo.flagship.wapomain.MainConstants.ACTION_MY_POST
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_AUDIO_PLAYER
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_COMMENTS_DEEPLINK
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION_DEEPLINK
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SIGN_IN
import com.wapo.flagship.wapomain.MainConstants.ACTION_PRINT_EDITION
import com.wapo.flagship.wapomain.MainConstants.ACTION_PROMOCODE_OFFER
import com.wapo.flagship.wapomain.MainConstants.ACTION_TOP_STORIES
import com.wapo.flagship.wapomain.MainConstants.ACTION_WATCH
import com.wapo.flagship.wapomain.MainConstants.ATP_SHARE_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_AUDIO_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_AUDIO_SUBTYPE
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_COMMENT_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_COMMENT_STORY_URL
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_URL
import com.wapo.flagship.wapomain.MainConstants.SHORTCUT_ALERTS
import com.wapo.flagship.wapomain.MainConstants.SHORTCUT_MY_POST
import com.wapo.flagship.wapomain.MainConstants.SHORTCUT_POLITICS
import com.washingtonpost.android.paywall.billing.NativePaywallListenerActivity
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import com.washingtonpost.android.save.types.MyPostSection

class IntentHelper {
    val TAG = IntentHelper::class.java.simpleName
    private val menuSectionBundleAliases = mutableMapOf<List<String>, MenuSection>()

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun offer(
        intent: Intent,
        context: Context,
    ) {
        val data = intent.data?.toString()
        val urlParser = intent.getParcelableExtra<URLParser>(DeepLinksProcessor.ARG_URL_PARSER)
        Logger.d(TAG, "offer(), intent.data=$data, urlParser.isValid=${urlParser?.isValid()}")
        if (Utils.isExternalSpecialLink(data)) {
            val activity = findActivity(context)
            if (activity != null) {
                Utils.handleSpecialLink(activity, data)
            }
            return
        }
        urlParser ?: return
        if (!urlParser.isValid()) return
        if (handleDebugLaunch(urlParser.urlString, context)) {
            return
        }
        if (DeepLinksProcessor.handleConfigDestination(urlParser, intent, context)) {
            return
        }
        if (DeepLinksProcessor.isEnvironmentPath(urlParser)) {
            DeepLinksProcessor.handleDebugPanelPrefs(urlParser)
            return
        }
        if (DeepLinksProcessor.isNavigation(urlParser)) {
            val action =
                when {
                    DeepLinksProcessor.isTopStoriesNav(urlParser) -> ACTION_TOP_STORIES
                    DeepLinksProcessor.isListenNav(urlParser) -> ACTION_LISTEN
                    DeepLinksProcessor.isGamesNav(urlParser) -> ACTION_GAMES
                    DeepLinksProcessor.isMyPostNav(urlParser) -> {
                        setMyPostSectionForIntent(urlParser, intent)
                        ACTION_MY_POST
                    }
                    DeepLinksProcessor.isWatchNav(urlParser) -> ACTION_WATCH
                    DeepLinksProcessor.isAskNav(urlParser) -> {
                        val shareId = urlParser.getQueryParameter("share_id")
                        if (!shareId.isNullOrBlank()) {
                            intent.putExtra(ATP_SHARE_ID, shareId)
                        }
                        ACTION_ASK
                    }
                    DeepLinksProcessor.isFindUrl(urlParser) -> ACTION_FIND
                    DeepLinksProcessor.isPrintEditionNav(urlParser) -> ACTION_PRINT_EDITION
                    DeepLinksProcessor.isPromoCodeLink(urlParser) -> ACTION_PROMOCODE_OFFER
                    else -> null
                }
            if (action != null) {
                getMainActivityIntent(context).apply {
                    fillIn(intent, 0)
                    setAction(action)
                    setData(null)
                    context.startActivity(this)
                }
            }
        } else if (DeepLinksProcessor.isPrintEditionNav(urlParser)) {
            intent.setClass(context, PrintActivity::class.java)
            context.startActivity(intent)
        } else if (DeepLinksProcessor.isAlerts(urlParser)) {
            intent.setClass(context, AlertsActivity::class.java)
            context.startActivity(intent)
        } else if (DeepLinksProcessor.isSettings(urlParser)) {
            intent.setClass(context, SettingsActivity::class.java)
            context.startActivity(intent)
        } else if (DeepLinksProcessor.isContactUs(urlParser)) {
            intent.setClass(context, ContactUsActivity::class.java)
            context.startActivity(intent)
        } else if (DeepLinksProcessor.isSignin(urlParser)) {
            getMainActivityIntent(context).apply {
                fillIn(intent, 0)
                action = ACTION_OPEN_SIGN_IN
                setData(null)
                context.startActivity(this)
            }
        } else if (DeepLinksProcessor.isBlockerLink(urlParser)) {
            handleBlockerLink(intent, context, urlParser)
        } else if (DeepLinksProcessor.isDirectIapPurchaseLink(urlParser)) {
            handleDirectPurchaseLink(intent, context, urlParser)
        } else if (DeepLinksProcessor.isAddonPurchaseLink(urlParser)) {
            handleAddonPurchaseLink(intent, context, urlParser)
        } else if (DeepLinksProcessor.isSectionPath(urlParser)) {
            getMainActivityIntent(context).apply {
                fillIn(intent, 0)
                action = ACTION_OPEN_SECTION
                putExtra(EXTRAS_SECTION_URL, urlParser.urlString)
                setData(null)
                context.startActivity(this)
            }
        } else if (DeepLinksProcessor.isSectionUrl(urlParser)) {
            getMainActivityIntent(context).apply {
                fillIn(intent, 0)
                action = ACTION_OPEN_SECTION_DEEPLINK
                putExtra(
                    EXTRAS_SECTION_URL,
                    DeepLinksProcessor.getSectionUrl(urlParser),
                )
                setData(null)
                context.startActivity(this)
            }
        } else if (DeepLinksProcessor.isWapoHomepage(urlParser)) {
            getMainActivityIntent(context).apply {
                fillIn(intent, 0)
                action = ACTION_TOP_STORIES
                setData(null)
                context.startActivity(this)
            }
        } else if (DeepLinksProcessor.isNativeCommentDeepLink(urlParser)) {
            DeepLinksProcessor.getCommentDeepLinkData(urlParser)?.let { (storyUrl, commentId) ->
                getMainActivityIntent(context).apply {
                    fillIn(intent, 0)
                    action = ACTION_OPEN_COMMENTS_DEEPLINK
                    putExtra(EXTRAS_COMMENT_STORY_URL, storyUrl)
                    putExtra(EXTRAS_COMMENT_ID, commentId)
                    setData(null)
                    context.startActivity(this)
                }
            }
        } else if (DeepLinksProcessor.isAudioPlayerLink(urlParser)) {
            val audioData = DeepLinksProcessor.getAudioData(urlParser)
            if (audioData != null) {
                val (subtype, id) = audioData

                getMainActivityIntent(context).apply {
                    fillIn(intent, 0)
                    action = ACTION_OPEN_AUDIO_PLAYER
                    putExtra(EXTRAS_AUDIO_SUBTYPE, subtype)
                    putExtra(EXTRAS_AUDIO_ID, id)
                    setData(null)
                    context.startActivity(this)
                }
            }
        } else if (DeepLinksProcessor.isSocialRedirect(urlParser)) {
            DeepLinksProcessor.handleAuthRedirect(context, urlParser)
        } else if (DeepLinksProcessor.shouldDelegateToAppWebView(urlParser)) {
            Utils.startWeb(urlParser.urlString, context)
        } else if (!isDeepLinkToSection(intent) &&
            !isLaunchedFromRecent(intent) &&
            (isPushOriginated(intent) || isContentDeepLink(intent) || isWidgetOriginated(intent))
        ) {
            urlParser.urlString?.let {
                DeepLinksProcessor.giftToken(urlParser)?.let { giftToken ->
                    /*
                        At this point it is safe to assume that it is a gift article deeplink.
                        Hence we can start the gift recipient flow with Articles2Activity.
                     */
                    startGiftRecipientFlow(
                        intent.action ?: "ACTION_READ",
                        it,
                        giftToken,
                        context,
                    )
                    return
                }

                /*
                    Here we will check to see if the user has clicked a link from another
                    referrer app (ie Reddit).
                 */
                val referrer = urlParser.referrer
                if (!referrer.isNullOrEmpty() && !isPushOriginated(intent)) {
                    startReferrerDeeplink(
                        context,
                        intent.action ?: "ACTION_READ",
                        it,
                        referrer,
                    )
                    return
                }
            }
            val newIntent = Intent(context, SearchableArticlesActivity::class.java)
            newIntent.fillIn(intent, 0)
            newIntent.data = urlParser.uri
            context.startActivity(newIntent)
        }
    }

    /**
     * If the url is a My Post deeplink and has a section filter, set the [MY_POST_SECTION] for the intent
     */
    private fun setMyPostSectionForIntent(
        urlParser: URLParser,
        intent: Intent,
    ) {
        if (!DeepLinksProcessor.isMyPostNav(urlParser)) return
        when (DeepLinksProcessor.getMyPostFilter(urlParser)) {
            DeepLinksProcessor.MY_POST_SAVED_STORIES -> {
                intent.putExtra(
                    MyPost2Fragment.MY_POST_SECTION,
                    MyPostSection.SAVED_STORIES.toString(),
                )
            }
            // Add other My Post sections as needed
        }
    }

    private fun handleBlockerLink(
        intent: Intent,
        context: Context,
        urlParser: URLParser,
    ) {
        if (context is MainActivity && context.paywallSheetHelper.isPaywallSheetShowing()) {
            // dismiss if blocker is already showing in MainActivity to track type properly
            context.paywallSheetHelper.dismissPaywallSheetDialog(context.isFinishing)
        }
        // parse query params for blocker name and choice
        val paywallReason = getDefaultPaywallReason()
        val paywallType = getDefaultPaywallType(intent)
        val blockerName = DeepLinksProcessor.getBlockerName(urlParser)
        val choice = DeepLinksProcessor.getBlockerChoice(urlParser)

        startPaywallActivity(intent, context, paywallReason, paywallType, blockerName, choice)
    }

    private fun handleDirectPurchaseLink(
        intent: Intent,
        context: Context,
        urlParser: URLParser,
    ) {
        if (context is MainActivity && context.paywallSheetHelper.isPaywallSheetShowing()) {
            // dismiss if blocker is already showing in MainActivity
            context.paywallSheetHelper.dismissPaywallSheetDialog(context.isFinishing)
        }
        // Check if deeplink url has a valid product as the last path segment
        val purchaseProductIdAndOffer =
            DeepLinksProcessor.getDirectIapPurchaseProductIdAndOffer(
                urlParser,
            )

        val defaultPaywallReason = getDefaultPaywallReason()
        val defaultPaywallType = getDefaultPaywallType(intent)

        // Start Iap flow for productId if it is not null
        // else show the paywall
        purchaseProductIdAndOffer?.apply {
            startDirectIapPurchase(intent, this.first, this.second, defaultPaywallReason, defaultPaywallType, context)
        } ?: startPaywallActivity(intent, context, defaultPaywallReason, defaultPaywallType)
    }

    /**
     * Used to start purchase flow for a given productId from a deeplink
     * ie. Airship message with subs/purchase deeplink that provides productId
     */
    private fun startDirectIapPurchase(
        intent: Intent,
        productId: String,
        offerId: String?,
        fallbackPaywallReason: Int,
        fallbackPaywallType: PaywallConstants.WallType,
        context: Context,
    ) {
        NativePaywallListenerActivity.launch(
            context,
            productId,
            offerId,
            null,
            intent.extras,
            DefaultNativePaywallResultCallbacks(fallbackPaywallReason, fallbackPaywallType.ordinal),
        )
    }

    /**
     * Handles an add-on purchase deep link: /subs/addon/<addOnProductName>
     * Resolves the add-on product name to a product ID and starts the native
     * add-on purchase flow using the user's current base subscription.
     */
    private fun handleAddonPurchaseLink(intent: Intent, context: Context, urlParser: URLParser) {
        val addOnProductName = DeepLinksProcessor.getAddonProductName(urlParser) ?: return
        val addOnProductId = PaywallUtil.mapProductNameToProductId(addOnProductName) ?: return

        val service = PaywallService.getInstance() ?: return
        val baseProductId = service.inAppSubProductId

        if (!baseProductId.isNullOrEmpty()) {
            context.startActivity(
                NativePaywallListenerActivity.getPurchaseWithAddOnsIntent(
                    context,
                    baseProductId,
                    arrayListOf(addOnProductId),
                    intent.extras
                )
            )
        } else {
            // No base subscription — resolve base subscription id later
            context.startActivity(
                NativePaywallListenerActivity.getPurchaseAddOnsOnlyIntent(
                    context,
                    arrayListOf(addOnProductId),
                    intent.extras
                )
            )
        }
    }

    /**
     * Used to show paywall from a deeplink
     * ie. Airship message with subs/purchase deeplink
     */
    private fun startPaywallActivity(
        intent: Intent,
        context: Context,
        paywallReason: Int? = null,
        paywallType: PaywallConstants.WallType? = null,
        blockerName: String? = null,
        choice: Int? = null,
    ) {
        val nonNullPaywallReason: Int = paywallReason ?: getDefaultPaywallReason()
        val nonNullPaywallType: PaywallConstants.WallType = paywallType ?: run {
            val argSourceType = intent.getStringExtra(DeepLinksProcessor.ARG_SOURCE_TYPE)
            getDefaultPaywallType(argSourceType.orEmpty())
        }

        intent.setClass(context, NativePaywallActivity::class.java)
        // Reason and Type are required for paywall to be shown with NativePaywallActivity
        intent.putExtra(PaywallConstants.PAYWALL_REASON, nonNullPaywallReason)
        intent.putExtra(PaywallConstants.PAYWALL_TYPE_ORDINAL, nonNullPaywallType.ordinal)
        intent.putExtra(PaywallConstants.BLOCKER_NAME, blockerName)
        intent.putExtra(PaywallConstants.PAYWALL_CHOICE, choice)
        intent.data = null
        context.startActivity(intent)
    }

    private fun getDefaultPaywallReason() = PaywallConstants.METERED

    private fun getDefaultPaywallType(intent: Intent): PaywallConstants.WallType {
        val argSourceType = intent.getStringExtra(DeepLinksProcessor.ARG_SOURCE_TYPE)
        return getDefaultPaywallType(argSourceType.orEmpty())
    }

    private fun getDefaultPaywallType(argSourceType: String): PaywallConstants.WallType {
        return when (argSourceType) {
            DeepLinksProcessor.SourceType.IAA.name -> PaywallConstants.WallType.IAA_WALL
            DeepLinksProcessor.SourceType.ONE_LINK.name -> PaywallConstants.WallType.ONELINK_WALL
            DeepLinksProcessor.SourceType.WEBVIEW.name -> PaywallConstants.WallType.WEBVIEW_PRODUCT_PAGE
            else -> PaywallConstants.WallType.DEFAULT_DEEP_LINK_PAYWALL
        }
    }

    private fun handleDebugLaunch(
        url: String?,
        context: Context,
    ): Boolean {
        if (DEBUG_ARTICLES_RESOURCES.contains(url)) {
            val builder =
                builder()
                    .setArticleSingleUrl(url)
            val launch = builder.buildIntent(context)
            context.startActivity(launch)
            return true
        }
        return false
    }

    /**
     * Starts the gift recipient flow.
     * [action] intent action.
     * [url] gift article url (This is a resolved url)
     * [giftToken] gift token (This can be found in the url)
     * [context] context. WARNING: Do NOT use the application context here.
     */
    private fun startGiftRecipientFlow(
        action: String,
        url: String,
        giftToken: String,
        context: Context,
    ) {
        val builder =
            builder()
                .setArticleSingleUrl(url)
        val articlesUrls = arrayOf(url)
        val list = listOf(*articlesUrls)
        builder.setArticleUrls(list, list.indexOf(url))
        builder.setGiftToken(giftToken)
        builder.deepLinkOriginated(true)
        val launch = builder.buildIntent(context)
        launch.action = action
        context.startActivity(launch)
    }

    /**
     * Start Referrer Deeplink.
     * [action] intent action.
     * [url] deeplink url from referrer app
     * [referrer] the app referring to this App (ie. Reddit)
     * [medium] the type of app (ie. social)
     * [context] context. WARNING: Do NOT use the application context here.
     */
    private fun startReferrerDeeplink(
        context: Context,
        action: String,
        url: String,
        referrer: String,
        medium: String? = null,
    ) {
        val builder =
            builder()
                .setArticleSingleUrl(url)
        val articlesUrls = arrayOf(url)
        val list = listOf(*articlesUrls)
        builder.setArticleUrls(list, list.indexOf(url))
        builder.setReferrer(referrer)
        medium?.let {
            builder.setTetroUtm(it)
        }

        builder.deepLinkOriginated(true)
        val launch = builder.buildIntent(context)
        launch.action = action
        context.startActivity(launch)
    }

    private fun isDeepLinkToSection(intent: Intent): Boolean = intent.getBooleanExtra(TopBarFragment.EXTRAS_DEEPLINK_TO_SECTION, false)

    private fun isContentDeepLink(intent: Intent): Boolean = intent.data?.toString()?.isNotEmpty() ?: false

    fun isPushOriginated(intent: Intent): Boolean = intent.getBooleanExtra(PUSH_ORIGINATED, false)

    fun isWidgetOriginated(intent: Intent): Boolean = intent.getBooleanExtra(WIDGET_ORIGINATED, false)

    fun shouldRefreshForYouWidget(intent: Intent): Boolean = intent.getBooleanExtra(FOR_YOU_WIDGET_UPDATE, false)

    fun isShortcutOriginated(intent: Intent): Boolean =
        intent.action in
            listOf(
                SHORTCUT_MY_POST,
                SHORTCUT_ALERTS,
                SHORTCUT_POLITICS,
            )

    fun getCartaUrl(intent: Intent?): String? = intent?.data?.getQueryParameter("carta-url")

    fun isSectionURL(url: String?): Boolean {
        url ?: return false
        val urlParser = URLParser(url)
        return DeepLinksProcessor.isSectionPath(urlParser)
    }

    fun findMenuSection(
        name: String?,
        where: List<MenuSection>?,
    ): MenuSection? {
        if (name.isNullOrEmpty()) {
            return null
        }

        if (menuSectionBundleAliases.isEmpty()) {
            where
                ?.flatMap { listOf(it) + it.sectionInfo } // check the item and all its' subItems
                ?.forEach {
                    val aliases = mutableListOf<String>(it.bundleName, it.databaseId)
                    if (it.aliases != null) {
                        aliases.addAll(it.aliases)
                    }
                    menuSectionBundleAliases[aliases] = it
                }
        }

        menuSectionBundleAliases.forEach {
            if (it.key.contains(name)) {
                return it.value
            }
        }
        return null
    }

    /**
     * Standard app launch, i.e. not opened from deeplink, push, or widget
     */
    fun isStandardAppLaunch(intent: Intent): Boolean =
        intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            intent.data == null &&
            intent.extras == null

    fun shouldProcessIntent(intent: Intent?): Boolean {
        intent ?: return false
        val intentHelper = IntentHelper()
        return intent.data != null || intent.extras != null ||
                intentHelper.isWidgetOriginated(intent) ||
                intentHelper.isPushOriginated(intent) ||
                isShortcutOriginated(intent)
    }

    companion object {
        @JvmStatic
        fun isLaunchedFromRecent(intent: Intent): Boolean =
            (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY

        /**
         * Use this intent (for example in PendingIntents) when app links need to opened from external sources.
         */
        @JvmStatic
        fun getDeepLinkDelegatorActivityIntent(activityContext: Context): Intent =
            Intent(activityContext, DeepLinkSingleTaskActivity::class.java)

        /**
         * Use this intent for all deep links that MainActivity owns.
         * Sets package name in internal case so origin can be checked downstream.
         */
        @JvmStatic
        fun getMainActivityIntent(
            activityContext: Context,
            isExternalOrigin: Boolean = false,
        ): Intent =
            Intent(activityContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (!isExternalOrigin) {
                    setPackage(activityContext.packageName)
                }
            }
    }
}
