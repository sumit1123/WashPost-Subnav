package com.wapo.flagship.features.deeplinks

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.config.DefaultConfigManager
import com.wapo.flagship.config.findSectionByPathAliases
import com.wapo.flagship.features.articles2.activities.PUSH_ORIGINATED
import com.wapo.flagship.features.search2.ui.Search2Activity
import com.wapo.flagship.push.PushListener
import com.wapo.flagship.push.PushMetadata
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.OverrideGroup
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.util.PaywallUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

object DeepLinksProcessor {
    private val TAG = this.javaClass.simpleName
    private val TOP_STORIES_NAV_URL_PATTERN = "(\\/tab\\/home\\/?|\\/)"
    private val LISTEN_NAV_URL_PATTERN = "/tab/listen"
    private val ALERTS_NAV_URL_PATTERN = "/tab/alerts" // TODO: rename
    private val GAMES_NAV_URL_PATTERN = "(\\/tab\\/games\\/?|\\/tab\\/play\\/?|\\/games\\/?|\\/play\\/?)"
    private val MY_POST_NAV_URL_PATTERN = "/tab/mypost"
    private val ASK_NAV_URL_PATTERN = "(\\/tab\\/ask\\/?|\\/ask-the-post-ai\\/|\\/ask-the-post-ai$)"
    private val FIND_URL_PATTERN = "(\\/tab\\/find|\\/tab\\/find\\/)"
    private val WATCH_NAV_URL_PATTERN = "(\\/classic-apps\\/watch|\\/watch\\/|\\/classic-apps\\/watch\\/|\\/watch|\\/tab\\/watch|\\/tab\\/watch\\/)"
    private val FILTER_QUERY = "filter"
    const val MY_POST_SAVED_STORIES = "saved"

    // Add other My Post sections as needed
    private val PRINT_EDITION_NAV_URL_PATTERN = "(\\/tab\\/print\\/?|\\/todays_paper\\/updates\\/)"
    private val BLOCKER_URL_PATTERN = "/subs/blocker"
    private val LEGACY_BLOCKER_URL_PATTERN = "/subs/purchase"
    const val DIRECT_IAP_PURCHASE_PATTERN = "/subs/purchase/[^/]+"
    const val DIRECT_IAP_PURCHASE_OFFER_PATTERN = "/subs/purchase/[^/]+/[^/]+"
    private const val ADDON_PURCHASE_PATTERN = "/subs/addon/[^/]+"
    private val PROMOCODE_URL_PATTERN = "/subs/promocode"
    private val SETTINGS_URL_PATTERN = "/settings(\\/main|\\/alerts|\\/newsletters)"
    val SIGNIN_URL_PATTERN = "/subs/signin"
    private val CONTACT_US_PATTERN = "(\\/settings\\/contactus|\\/hc\\/en-us\\/)"
    private val SECTION_URL_PATTERN = "/section\\?url=(.*)"
    private val SECTION_PATH_PATTERN = "/section(/.*)"
    private val ENVIRONMENT_PATH_PATTERN = "/environment"
    private val AUDIO_PATH_PATTERN =
        "^/audio/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_-]+)$"
    private val WP_DOMAIN = "washingtonpost.com"
    private val WP_DOMAIN_WWW = "www.washingtonpost.com"
    private val WP_STORE_DOMAIN = "store.washingtonpost.com"
    private val WP_SHORT_LINK = "wapo.st"
    private val SOCIAL_REDIRECT = "social-redirect"
    val OUTPUT_TYPE = "outputType"
    val COMMENT = "comment"
    val NO_NAV = "no_nav"
    private val COMMENTS_NAV_URL_PATTERN = "/comments"
    private val COMMENT_ID_QUERY_PARAM = "commentID"
    private val CROSSWORD = "crossword"
    private val PRIVACY_POLICY = "/privacy-policy/"
    private val AD_CHOICES = "/ad-choices/"
    private val NEWSLETTER = "/newsletter"
    private val TERMS_OF_SERVICE = "/terms-of-service/"
    private val ZENDESK_URL = "helpcenter.washingtonpost.com"
    private val EXTRA_ACCOUNT_URL = "extra-accounts"
    private val MY_POST_URL = "my-post"
    private val GIFT_TOKEN_PARAM = "pwapi_token"
    private val TETRO_UTM_PARAM = "utm_medium"
    private val REFERRER_PARAM = "utm_source"
    private val ALLOWED_BLOCKER_QUERY_PARAMS = listOf("name", "choice")

    private val CHROME_BROWSER = "com.android.chrome"
    private val AMAZON_BROWSER = "com.amazon.cloud9"
    private val SAMSUNG_BROWSER = "com.sec.android.app.sbrowser"

    const val PAYWALL_LINK = "washpost:///subs/blocker"
    const val WASHPOST_SCHEMA = "washpost://"

    const val ARG_SOURCE_TYPE = "SOURCE_TYPE"
    const val ARG_URL_PARSER = "URL_PARSER"
    const val ASK_THE_POST_SHARE_URL = "https://www.washingtonpost.com/ask-the-post-ai/"

    private val DESTINATIONS =
        mapOf(
            "recipe_search" to Search2Activity::class.java,
            "election_search" to Search2Activity::class.java,
        )

    private val config get() = ConfigManager.getInstance().config
    private val scope = MainScope()

    /**
     * OneLink is AppsFlyer's Unified Deep Linking feature.
     * Mobile users click on external OneLink links and are sent to specific pages in the app,
     * or first to app installation if app is not installed.
     */
    enum class SourceType {
        IAA,
        ONE_LINK,
        WEBVIEW,
        HABIT_TILES,
        ITERABLE,
        WIDGET
    }

    /**
     * Test the URL against predefined destinations in app and forward the intent to the
     * destination, if found
     */
    fun handleConfigDestination(
        urlParser: URLParser,
        intent: Intent,
        context: Context,
    ): Boolean {
        if (!urlParser.isValid()) return false
        for (destination in config.deepLinkConfig.destinations) {
            val resolvedDestination = DESTINATIONS[destination.destination]
            if (urlParser.getScheme().matches(destination.scheme.toRegex()) &&
                urlParser.getDomain().matches(destination.domain.toRegex()) &&
                urlParser.getPath().matches(destination.path.toRegex()) &&
                resolvedDestination != null
            ) {
                Logger.d(TAG, "handleConfigDestination(), resolved to $resolvedDestination")

                val newIntent = Intent(context, resolvedDestination)
                newIntent.fillIn(intent, 0)
                newIntent.data = intent.data
                context.startActivity(newIntent)
                return true
            }
        }
        return false
    }

    fun isNavigation(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return isTopStoriesNav(urlParser) ||
                isListenNav(urlParser) ||
                isGamesNav(urlParser) ||
                isWatchNav(urlParser) ||
                isMyPostNav(urlParser) ||
                isAskNav(urlParser) ||
                isFindUrl(urlParser) ||
                isPrintEditionNav(urlParser) ||
                isPromoCodeLink(urlParser)
    }

    fun isTopStoriesNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(TOP_STORIES_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isListenNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(LISTEN_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isGamesNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(GAMES_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isWatchNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(WATCH_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isMyPostNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(MY_POST_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isAskNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(ASK_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isFindUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(FIND_URL_PATTERN, urlParser.getPath())
    }

    fun getMyPostFilter(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        return urlParser.getQueryParameter(FILTER_QUERY)
    }

    fun isPrintEditionNav(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(PRINT_EDITION_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isAlerts(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(ALERTS_NAV_URL_PATTERN, urlParser.getPath())
    }

    fun isAudioPlayerLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(AUDIO_PATH_PATTERN, urlParser.getPath())
    }

    private fun isWapoLink(urlParser: URLParser): Boolean =
        urlParser.getDomain().endsWith(WP_DOMAIN, true) ||
                urlParser.getDomain().endsWith(WP_SHORT_LINK, true)

    private fun isWapoStoreLink(urlParser: URLParser): Boolean =
        urlParser.getDomain().equals(WP_STORE_DOMAIN, ignoreCase = true)

    /**
     * The Legacy path is supported for backward compatibility but is deprecated.
     * The name was changed for clarity, to disambiguate paywall/blocker flow from the new direct purchase flow.
     * Two optional query params are supported for this deeplink pattern:
     * - name - blockerName, name of which paywall/blocker to display
     * - choice - 0 or 1, representing which Interval tab to display by default on paywall (Monthly or Annual)
     */
    fun isBlockerLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (Pattern.matches(BLOCKER_URL_PATTERN, urlParser.getPath()) ||
            Pattern.matches(
                LEGACY_BLOCKER_URL_PATTERN,
                urlParser.getPath(),
            )
        ) {
            return areAllQueryParamsAllowed(
                urlParser.getQueryParameterNames(),
                ALLOWED_BLOCKER_QUERY_PARAMS,
            )
        }
        return false
    }

    fun isDirectIapPurchaseLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(DIRECT_IAP_PURCHASE_PATTERN, urlParser.getPath()) ||
                Pattern.matches(
                    DIRECT_IAP_PURCHASE_OFFER_PATTERN,
                    urlParser.getPath(),
                )
    }

    /**
     * Returns true if this is an add-on purchase deep link: /subs/addon/<addOnProductName>
     */
    fun isAddonPurchaseLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(ADDON_PURCHASE_PATTERN, urlParser.getPath())
    }

    /**
     * Extracts the add-on product name from an add-on purchase deep link.
     * e.g. /subs/addon/AD_FREE -> "AD_FREE"
     */
    fun getAddonProductName(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        val segments = urlParser.uri?.pathSegments ?: return null
        return if (segments.size >= 3) segments[2] else null
    }

    /**
     * Returns true if path from the url or path from the SECTION_PATH_PATTERN (washpost:/section/<path>) matches
     * with one of the site service sections.
     */
    fun isSectionPath(urlParser: URLParser): Boolean = getSectionId(urlParser) != null

    /**
     * Returns true if url matches with SECTION_URL_PATTERN (washpost:///section?url=value)
     */
    fun isSectionUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(SECTION_URL_PATTERN, urlParser.getPathAndParameters())
    }

    /**
     * Returns true if link is WaPo homepage.
     */
    fun isWapoHomepage(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return urlParser.getDomain() == WP_DOMAIN_WWW && urlParser.getPath().trimEnd('/').isEmpty()
    }

    /**
     * Returns true if link is Social Redirect.
     */
    fun isSocialRedirect(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return isWapoLink(urlParser) && urlParser.getPath().contains(SOCIAL_REDIRECT)
    }

    /**
     * Returns true if the URL is the default redirect URL (washpost://)
     * used to get user back to the app.
     */
    private fun isDefaultRedirect(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return urlParser.rawLink.equals(WASHPOST_SCHEMA)
    }

    /**
     * Returns true if link's domain is permitted by config.
     */
    private fun isAllowedByConfig(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        val domain = urlParser.getDomain().lowercase()
        config.deepLinkConfig.allowedHosts.forEach {
            if (Pattern.matches(it, domain)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if link is WaPo Comments.
     */
    fun isCommentsUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return isWapoLink(urlParser) &&
                (
                        urlParser.getPath().contains(COMMENT) ||
                                urlParser.getQueryParameter(OUTPUT_TYPE)
                                    ?.contains(COMMENT) == true ||
                                urlParser.getQueryParameterNames().contains(COMMENT_ID_QUERY_PARAM)
                        )
    }

    /**
     * Returns true if link is WaPo Crosswords.
     */
    fun isCrosswordsUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return isWapoLink(urlParser) && urlParser.getPath().contains(CROSSWORD)
    }

    /**
     * Returns true if link is a Wapo Privacy Policy page.
     */
    fun isPrivacyPolicyUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getPath().contains(PRIVACY_POLICY)
    }

    /**
     * Returns true if link is a Wapo advertising info page
     */
    fun isAdvertisingInfoUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getPath().contains(AD_CHOICES)
    }

    /**
     * Returns true if link is a Wapo newsletter page.
     */
    fun isNewsletterUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getPath().contains(NEWSLETTER)
    }

    /**
     * Returns true if link is a Wapo terms of service page.
     */
    fun isTermsOfServiceUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getPath().contains(TERMS_OF_SERVICE)
    }

    /**
     * Return true if link is a zendesk url
     */
    fun isZendeskUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getDomain().contains(ZENDESK_URL)
    }

    fun isInlineOfferUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getPath().contains(EXTRA_ACCOUNT_URL)
    }

    fun isMyPostUrl(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        if (!isWapoLink(urlParser)) return false
        return urlParser.getPath().contains(MY_POST_URL)
    }


    /**
     * Check query params for blocker name and return value if found, or null otherwise.
     */
    fun getBlockerName(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        return urlParser.getQueryParameter("name")
    }

    /**
     * Check query params for blocker choice and return value if found, or null otherwise.
     * Supported values are 0 (monthly) or 1 (annual).
     */
    fun getBlockerChoice(urlParser: URLParser): Int? {
        if (!urlParser.isValid()) return null
        return urlParser.getQueryParameter("choice")?.toIntOrNull()
    }

    /**
     * Checks if last two path segments has a productName which maps to a productId OR if it is a productId
     * and has an optional valid offer name (in allowed offers list and eligible offer)
     *
     * - Return null if productName is not valid .
     * - Return a Pair with productId if a match is found and an offer name if it is found valid (otherwise return null for this value)
     * Note: While productId may differ between platforms, productName is platform-agnostic.
     */
    fun getDirectIapPurchaseProductIdAndOffer(urlParser: URLParser): Pair<String, String?>? {
        if (!urlParser.isValid()) return null
        if (urlParser.uri == null) return null
        val numPathsInUri = urlParser.uri?.pathSegments?.size ?: 0
        val productName =
            if (numPathsInUri >= 3) {
                urlParser.uri?.let {
                    it.pathSegments[2]
                }
            } else {
                null
            }
        val offerName =
            if (numPathsInUri == 4) {
                urlParser.uri?.let {
                    // Convert codes to lowercase as defined codes are in lowercase in play console.
                    it.pathSegments[3].lowercase()
                }
            } else {
                null
            }
        val matchingProductId = productName?.let { PaywallUtil.mapProductNameToProductId(it) }
        val isValidOfferId: Boolean =
            if (matchingProductId != null && offerName != null) {
                PaywallService
                    .getConnector()
                    .iapSubItems
                    .getItem(matchingProductId)
                    ?.offers
                    ?.firstOrNull { it.offerId == offerName } != null
            } else {
                false
            }

        return when {
            !matchingProductId.isNullOrEmpty() ->
                Pair(
                    matchingProductId,
                    if (isValidOfferId) offerName else null,
                )

            productName != null &&
                    PaywallService.getBillingHelper().isValidSubscriptionProductId(
                        productName,
                    ) -> Pair(productName, if (isValidOfferId) offerName else null)

            else -> null
        }
    }

    /**
     * Returns section's id from SECTION_PATH_PATTERN or from the url path that matches
     * with one of the site service sections. Otherwise returns null.
     */
    fun getSectionId(urlParser: URLParser): String? {
        if (!urlParser.isValid() || urlParser.getPath().isEmpty()) return null
        val configManager: DefaultConfigManager =
            FlagshipApplication.getInstance().contentManager.wapoConfigManager
        val sectionPath =
            if (Pattern.matches(SECTION_PATH_PATTERN, urlParser.getPath())) {
                Regex(SECTION_PATH_PATTERN)
                    .find(urlParser.getPath())
                    ?.groups
                    ?.lastOrNull()
                    ?.value
            } else {
                urlParser.getPath()
            }
        if (sectionPath.isNullOrEmpty()) return null
        val section =
            findSectionByPathAliases(
                sectionPath.trimEnd('/'),
                configManager.sectionsBarConfig,
                configManager.sectionsFeaturedConfig,
                configManager.sectionsAZConfig,
                configManager.sectionsUnlistedConfig,
            ) ?: findSectionByPathAliases(
                sectionPath,
                configManager.sectionsBarConfig,
                configManager.sectionsFeaturedConfig,
                configManager.sectionsAZConfig,
                configManager.sectionsUnlistedConfig,
            )
        Logger.d(
            TAG,
            "getSectionId(), path=${urlParser.getPathAndParameters()}, sectionPath=$sectionPath, sectionId=${section?.sectionId}",
        )
        return section?.sectionId
    }

    /**
     * Returns value from the SECTION_URL_PATTERN (washpost://section?url=value)
     */
    fun getSectionUrl(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        return if (isSectionUrl(urlParser)) {
            urlParser.getQueryParameter("url")
        } else {
            null
        }
    }

    fun isSignin(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(SIGNIN_URL_PATTERN, urlParser.getPath())
    }

    fun isSettings(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(SETTINGS_URL_PATTERN, urlParser.getPath())
    }

    fun isContactUs(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(CONTACT_US_PATTERN, urlParser.getPath())
    }

    fun isEnvironmentPath(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(ENVIRONMENT_PATH_PATTERN, urlParser.getPath())
    }

    fun isPromoCodeLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return Pattern.matches(PROMOCODE_URL_PATTERN, urlParser.getPath())
    }

    /**
     * Checks if URL should be delegated to [SimpleWebViewActivity]
     * Applies to:
     * - Comments
     * - Comments thread
     * - Crosswords
     * - Privacy Policy
     * - Advertising Info
     * - Newsletter
     * - Zendesk
     * - Terms of Service
     * - urls from simpleWebviewlist config
     */
    fun shouldDelegateToAppWebView(urlParser: URLParser): Boolean {
        return  (isCommentsUrl(urlParser) && !isNativeCommentDeepLink(urlParser)) ||
                isCrosswordsUrl(urlParser) ||
                isPrivacyPolicyUrl(urlParser) ||
                isAdvertisingInfoUrl(urlParser) ||
                isNewsletterUrl(urlParser) ||
                isZendeskUrl(urlParser) ||
                isTermsOfServiceUrl(urlParser) ||
                isSimpleWebViewActivityLink(urlParser) ||
                isInlineOfferUrl(urlParser) ||
                isMyPostUrl(urlParser)
    }

    /**
     * Needed to admit AppsFlyer article deeplinks that come from [OneLinkListener].
     */
    private fun isArticleLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return isWapoLink(urlParser)
    }

    /**
     * Extract pwapi_token from article url
     */
    fun giftToken(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        return urlParser.getQueryParameter(GIFT_TOKEN_PARAM)
    }

    fun tetroUtm(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        return urlParser.getQueryParameter(TETRO_UTM_PARAM)
    }

    fun referrer(urlParser: URLParser): String? {
        if (!urlParser.isValid()) return null
        return urlParser.getQueryParameter(REFERRER_PARAM)
    }

    fun isDeepLinkSupported(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return isNavigation(urlParser) ||
                isBlockerLink(urlParser) ||
                isSignin(urlParser) ||
                isSettings(urlParser) ||
                isPromoCodeLink(urlParser) ||
                isContactUs(urlParser) ||
                isDirectIapPurchaseLink(urlParser) ||
                isAddonPurchaseLink(urlParser) ||
                isArticleLink(urlParser) ||
                isAlerts(urlParser) ||
                isEnvironmentPath(urlParser) ||
                isSectionPath(urlParser) ||
                isSectionUrl(urlParser) ||
                isWapoHomepage(urlParser) ||
                isSocialRedirect(urlParser) ||
                isAllowedByConfig(urlParser) ||
                isAudioPlayerLink(urlParser) ||
                (isCommentsUrl(urlParser) && isNativeCommentDeepLink(urlParser)) ||
                !urlParser.isExternalOrigin
    }

    private fun areAllQueryParamsAllowed(
        queryParams: Set<String>,
        allowedQueryParams: List<String>,
    ): Boolean {
        queryParams.forEach { queryParam ->
            if (!allowedQueryParams.contains(queryParam)) {
                return false
            }
        }
        return true
    }

    suspend fun process(
        link: String?,
        activityContext: Context? = null,
        isExternalOrigin: Boolean? = null,
        sourceType: SourceType? = null,
        referrer: String? = null,
        bundle: Bundle? = null
    ): Boolean {
        Logger.d(TAG, "process(), link=$link")
        val urlParser = URLParser(link)
        isExternalOrigin?.let {
            urlParser.isExternalOrigin = it
        }
        referrer?.let {
            urlParser.referrer = it
        }
        return if (urlParser.isValid()) {
            process(urlParser, activityContext, sourceType, null, bundle)
        } else {
            false
        }
    }

    fun processAsync(
        link: String?,
        activityContext: Context? = null,
        isExternalOrigin: Boolean? = null,
        sourceType: SourceType? = null,
        referrer: String? = null,
        bundle: Bundle? = null,
        scope: CoroutineScope? = null,
    ) {
        (scope ?: this.scope).launch(Dispatchers.Main) {
            process(link, activityContext, isExternalOrigin, sourceType, referrer, bundle)
        }
    }

    /**
     * All deeplink entry points should flow through here.
     * Exceptions:
     * - Magic Link auth flows (login via email link) are handled in [AuthHelper]
     * - Social Provider oauth flows (login via 3rd party) are handled in [AuthHelper]
     */
    suspend fun process(
        urlParser: URLParser,
        activityContext: Context? = null,
        sourceType: SourceType? = null,
        pushMetadata: PushMetadata? = null,
        bundle: Bundle? = null
    ): Boolean {
        Logger.d(TAG, "process(), urlParser=${urlParser.rawLink}")
        val resultUrlParser = urlParser.appendQueryParameters()
        if (resultUrlParser.rawLink != urlParser.rawLink) {
            Logger.d(TAG, "resultUrlParser=${resultUrlParser.rawLink}")
        }
        if (resultUrlParser.isValid() && isDeepLinkSupported(resultUrlParser)) {
            val context = activityContext ?: getCurrentActivity()
            context?.let {
                if (isExcluded(resultUrlParser)) {
                    redirectToBrowser(it, resultUrlParser)
                } else if (isDefaultRedirect(resultUrlParser)) {
                    // No action needed, just bring user back to app
                } else {
                    IntentHelper().offer(getIntent(resultUrlParser, sourceType, pushMetadata, bundle), it)
                }
                return true
            }
        }
        return false
    }

    fun isNativeCommentDeepLink(urlParser: URLParser): Boolean {
        if (!isCommentsUrl(urlParser)) return false
        val queryParams = urlParser.getQueryParameterNames()
        return (Pattern.matches(COMMENTS_NAV_URL_PATTERN, urlParser.getPath()) &&
                queryParams.contains("storyUrl")) ||
                urlParser.getQueryParameter(OUTPUT_TYPE)?.contains(COMMENT) == true ||
                queryParams.contains(COMMENT_ID_QUERY_PARAM)
    }

    /**
     * Extracts (storyUrl, commentID) from a comment deep link URL.
     * Returns null if the data is not present.
     */
    fun getCommentDeepLinkData(urlParser: URLParser): Pair<String, String?>? {
        val storyUrl = if (urlParser.getPath().trimEnd('/').equals("/comments", ignoreCase = true)) {
            urlParser.getQueryParameter("storyUrl") ?: return null
        } else {
            urlParser.uri?.buildUpon()?.clearQuery()?.build()?.toString() ?: return null
        }
        return Pair(storyUrl, urlParser.getQueryParameter(COMMENT_ID_QUERY_PARAM))
    }

    fun processAsync(
        urlParser: URLParser,
        activityContext: Context? = null,
        sourceType: SourceType? = null,
        pushMetadata: PushMetadata? = null,
        bundle: Bundle? = null,
        scope: CoroutineScope? = null
    ) {
        (scope ?: this.scope).launch(Dispatchers.Main) {
            process(urlParser, activityContext, sourceType, pushMetadata, bundle)
        }
    }

    private suspend fun URLParser.appendQueryParameters(): URLParser {
        val resultUriBuilder = uri?.buildUpon() ?: return this
        if (shouldAddNonce(this)) {
            val authHelper = AuthHelper.getInstance(FlagshipApplication.getInstance())
            val nonce = authHelper.fetchNonce(urlString)
            nonce?.let { resultUriBuilder.appendQueryParameter("logmeinNonce", nonce) }
        }

        val resultUri = resultUriBuilder.build()
        return if (resultUri.toString() != uri.toString()) {
            copy(rawLink = resultUri.toString())
        } else {
            this
        }
    }

    private fun shouldAddNonce(urlParser: URLParser): Boolean {
        return isWapoLink(urlParser) && isExternalUrl(urlParser) && !isWapoStoreLink(urlParser)
    }

    private fun getIntent(
        urlParser: URLParser,
        sourceType: SourceType?,
        pushMetadata: PushMetadata?,
        bundle: Bundle?
    ): Intent =
        Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(urlParser.rawLink)
            putExtra(ARG_SOURCE_TYPE, sourceType?.name)
            putExtra(ARG_URL_PARSER, urlParser)
            putExtra(PUSH_ORIGINATED, urlParser.isPushOriginated)
            bundle?.let { putExtras(bundle) }
            if (sourceType == SourceType.WIDGET) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (urlParser.isPushOriginated && pushMetadata != null) {
                putExtra(PushListener.HEADLINE, pushMetadata.headline)
                putExtra(PushListener.KICKER, pushMetadata.kicker)
                putExtra(PushListener.TRACKING_NOTIFICATION_ID, pushMetadata.trackingId)
                putExtra(PushListener.ANALYTICS_ID, pushMetadata.analyticsId)
                putExtra(PushListener.NOTIFICATION_TIMESTAMP, pushMetadata.pushTimestamp)
            }
        }

    fun getCurrentActivity(): Activity? = FlagshipApplication.getInstance().currentActivity

    /**
     * Returns true if either the path or the host is excluded.
     * Ignores this check if from push.
     */
    private fun isExcluded(urlParser: URLParser): Boolean {
        if (!urlParser.isValid() || urlParser.isPushOriginated) {
            return false
        }
        return isPathExcluded(urlParser) || isHostExcluded(urlParser) || isUnknownLink(urlParser) || isExternalUrl(urlParser) || isWapoStoreLink(urlParser)
    }

    /**
     * Excludes certain paths for WaPo links.
     * External exclude list -> open in browser if coming from outside app.
     * Internal exclude list -> open in browser if coming from inside app.
     */
    private fun isPathExcluded(urlParser: URLParser): Boolean {
        if (isWapoLink(urlParser)) {
            val excludedPaths =
                when (urlParser.isExternalOrigin) {
                    true -> config.deepLinkConfig.externalExcludeList
                    false -> config.deepLinkConfig.internalExcludeList
                }
            excludedPaths.forEach {
                if (urlParser.getPath().contains(it)) {
                    return true
                }
            }
            val excludedParams = config.deepLinkConfig.paramExcludeList
            excludedParams.forEach {
                if (urlParser.getParameters().contains(other = it, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    fun getAudioData(urlParser: URLParser): Pair<String, String>? {
        val matcher = Pattern.compile(AUDIO_PATH_PATTERN).matcher(urlParser.getPath())

        return if (matcher.matches()) {
            Pair(matcher.group(1), matcher.group(2))
        } else {
            null
        }
    }

    /**
     * For washpost:// links with external origin, allows only specific hosts through. Ignore case.
     * Always allows empty host through to support navigation deeplinks, e.g. washpost:///tab/home
     */
    private fun isHostExcluded(urlParser: URLParser): Boolean {
        if (urlParser.isWashPostScheme() && urlParser.isExternalOrigin && urlParser.getDomain()
                .isNotBlank()
        ) {
            return !isAllowedByConfig(urlParser)
        }
        return false
    }

    fun isExternalUrl(urlParser: URLParser): Boolean {
        if (isWapoLink(urlParser)) {
            val externUrls = config.deepLinkConfig.externalPurchaseUrlList
            return externUrls.any {
                urlParser.rawLink?.let { link -> Pattern.matches(it, link) } == true
            }
        }
        return false
    }

    /**
     * Verify if this link should be opened on SimpleWebViewActivyty
     */
    fun isSimpleWebViewActivityLink(urlParser: URLParser): Boolean {
        if (!urlParser.isValid()) return false
        return config.deepLinkConfig.simpleWebViewList.any { regex ->
            urlParser.urlString?.let { url ->
                Pattern.matches(regex, url)
            } ?: false
        } ?: false
    }

    /**
     * With auto-verify set to true, if a magic deeplink comes into this app, it will be sent
     * to the browser for validation and redirected back to this app.
     */
    private fun redirectToBrowser(
        context: Context,
        urlParser: URLParser,
    ) {
        val intent = Intent(Intent.ACTION_VIEW, urlParser.uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        intent.setPackage(CHROME_BROWSER)

        // Use Chrome Browser to see if it works
        startInBrowser(context, intent) {
            // On Failure, check if device is Fire tablet and try loading in Amazon Browser
            if (Utils.isKindleFire()) {
                intent.setPackage(AMAZON_BROWSER)
                startInBrowser(context, intent)
            }
            // Default to whatever browser is available.
            else {
                val browser = getBrowserPackage(context, intent)
                intent.setPackage(browser)
                startInBrowser(context, intent) {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("DeepLink No Browser Found Error")
                            setModule(LogModules.DEEPLINK)
                            set("url", urlParser.urlString)
                        }.run {
                            RemoteLog.e(context, build())
                        }
                }
            }
        }
    }

    /**
     * With auto-verify set to true, if a magic deeplink comes into this app, it will be sent
     * to the browser for validation and redirected back to this app.
     */
    fun redirectToBrowser(
        context: Context,
        url: String,
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        intent.setPackage(CHROME_BROWSER)

        // Use Chrome Browser to see if it works
        startInBrowser(context, intent) {
            // On Failure, check if device is Fire tablet and try loading in Amazon Browser
            if (Utils.isKindleFire()) {
                intent.setPackage(AMAZON_BROWSER)
                startInBrowser(context, intent)
            }
            // Default to whatever browser is available.
            else {
                val browser = getBrowserPackage(context, intent)
                intent.setPackage(browser)
                startInBrowser(context, intent) {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("DeepLink No Browser Found Error")
                            setModule(LogModules.DEEPLINK)
                            set("url", url)
                        }.run {
                            RemoteLog.e(context, build())
                        }
                }
            }
        }
    }

    /**
     * Attempts to start a Chrome Custom Tab to handle an auth redirect from a social provider.
     * Falls back to external browser.
     */
    fun handleAuthRedirect(
        context: Context,
        urlParser: URLParser,
    ) {
        try {
            val supportedPackages = Utils.getChromeCustomTabsSupportedPackage(context)
            val colorSchemeParams =
                CustomTabColorSchemeParams
                    .Builder()
                    .setToolbarColor(context.resources.getColor(R.color.black))
                    .build()
            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setDefaultColorSchemeParams(colorSchemeParams)
                    .build()
            if (supportedPackages != null) {
                customTabsIntent.intent.setPackage(supportedPackages)
            }
            urlParser.uri?.let {
                customTabsIntent.launchUrl(context, it)
            }
        } catch (e: Exception) {
            redirectToBrowser(context, urlParser)
        }
    }

    fun handleDebugPanelPrefs(urlParser: URLParser): Boolean {
        if ((AppContextUtils.isDebuggableBuild()) && isEnvironmentPath(urlParser)) {
            setDebugPanelPrefs(urlParser)
            return true
        }
        return false
    }

    private fun setDebugPanelPrefs(urlParser: URLParser) {
        val queryParams = urlParser.urlString?.let { getQueryParameters(it) }
        val overrides = mutableListOf<ConfigOverride>().apply {
            addAll(ConfigManager.getInstance().state.value?.overrides.orEmpty())
        }
        queryParams?.forEach { (key, values) ->
            values.forEach { value ->
                when (key) {
                    "set", "add" -> {
                        val override = when (value) {
                            "paywall-stage" -> ConfigOverride.PAYWALL_STAGE
                            "site-stage" -> ConfigOverride.SITE_ARC_SANDBOX
                            "webview-beta" -> ConfigOverride.WEBVIEW_BETA
                            else -> ConfigOverride.fromIdOrNull(value)
                        }
                        if (override != null) overrides.add(override)
                    }

                    "remove" -> {
                        val group = when (value) {
                            "paywall-stage" -> OverrideGroup.PAYWALL_SUBS
                            "paywall-sandbox" -> OverrideGroup.PAYWALL_SUBS
                            "site-stage" -> OverrideGroup.SITE_SERVICE
                            "webview-beta" -> OverrideGroup.WEBVIEW
                            else -> OverrideGroup.fromIdOrNull(value)
                        }
                        if (group != null) overrides.removeAll { it.group == group }
                    }
                }
            }
        }
        if (overrides.isNotEmpty()) {
            ConfigManager.getInstance().setOverrides(overrides)
        }
    }

    private fun getQueryParameters(url: String): Map<String, List<String>> {
        val queryMap = mutableMapOf<String, MutableList<String>>()

        // Split the URL to isolate the query part
        val queryString = url.substringAfter("?", "")
        queryString.split("&").forEach { param ->
            val (key, value) = param.split("=").let { it[0] to it.getOrNull(1) }
            value?.split(",")?.forEach { subValue ->
                queryMap.getOrPut(key) { mutableListOf() }.add(subValue)
            }
        }
        return queryMap
    }

    /**
     * Try loading url in selected browser, else call fallback.
     */
    private fun startInBrowser(
        context: Context,
        intent: Intent,
        fallback: () -> Unit = { Logger.d(TAG, "Failed to Load Browser") },
    ) {
        try {
            context.startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            fallback()
        }
    }

    /**
     * Get package of browser.
     */
    private fun getBrowserPackage(
        context: Context,
        intent: Intent,
    ): String? {
        intent.setPackage(null)
        val list =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            } else {
                listOf<ResolveInfo>()
            }.filter { it.activityInfo.packageName != context.packageName }

        return list.firstOrNull()?.activityInfo?.packageName
    }

    fun sectionPathToDeepLink(sectionPath: String): String =
        "$WASHPOST_SCHEMA/section?url=$sectionPath"

    fun pathToFullUrl(sectionPath: String): String = "https://$WP_DOMAIN_WWW$sectionPath"

    fun canProcessWebTypeLink(link: String?): Boolean {
        link ?: return false
        val urlParser = URLParser(link)
        return isAskNav(urlParser) || isNativeCommentDeepLink(urlParser)
    }

    /*
     * Returns true if app should delegate link to an external browser (same as excluded links)
     */
    private fun isUnknownLink(urlParser: URLParser): Boolean {
        return !(isWapoLink(urlParser) || urlParser.isWashPostScheme() || urlParser.isAirshipScheme())
    }
}
