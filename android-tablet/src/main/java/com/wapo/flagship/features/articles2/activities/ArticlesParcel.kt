package com.wapo.flagship.features.articles2.activities

import android.content.Context
import android.content.Intent
import com.wapo.flagship.common.getUrlAndAnchorRefPair
import com.wapo.flagship.external.WidgetData
import com.wapo.flagship.features.articles.ArticleLinkType
import com.wapo.Utils
import com.wapo.flagship.features.grid.model.LinkType
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.flagship.model.ArticleMeta
import java.util.concurrent.TimeUnit


const val INTENT_ID = "intent_id"
const val ARTICLES_LIST_INDEX_OF_CLICKED = "article_link_clicked"
const val ARTICLES_META_LIST = "ARTICLES_META_LIST"
const val SECTION_DISPLAY_NAME = "SECTION_DISPLAY_NAME"
const val CAROUSEL_TITLE = "CAROUSEL_TITLE"
const val CAROUSEL_CATEGORY_ID = "CAROUSEL_CATEGORY_ID"
const val PUSH_ORIGINATED = "PUSH_ORIGINATED"
const val ALERT_PAGE_ORIGINATED = "ALERT_PAGE_ORIGINATED"
const val LIVE_BLOG_ORIGINATEED = "LIVE_BLOG_ORIGINATEED"
const val LINK_TYPE = "LINK_TYPE"
const val OPINION_PUSH_ORIGINATED = "OPINION_PUSH_ORIGINATED"
const val WPMM_PAYWALL = "WPMM_PAYWALL"
const val NEWSPRINT_ORIGINATED = "NEWSPRINT_ORIGINATED"
const val WIDGET_ORIGINATED = "WIDGET_ORIGINATED"
const val WIDGET_ID = "WIDGET_ID"
const val DEEPLINK_ORIGINATED = "DEEPLINK_ORIGINATED"
const val FOR_YOU_SECTION_ORIGINATED = "FOR_YOU_SECTION_ORIGINATED"
const val AIRSHIP_ORIGINATED = "AIRSHIP_ORIGINATED"
const val ONELINK_ORIGINATED = "ONELINK_ORIGINATED"
const val HABIT_TILES_ORIGINATED = "HABIT_TILES_ORIGINATED"
const val ASK_THE_POST_ORIGINATED = "ASK_THE_POST_ORIGINATED"
const val SUBNAV_LUF_ORIGINATED = "SUBNAV_LUF_ORIGINATED"
const val FAILOVER_ORIGINATED = "FAILOVER_ORIGINATED"
const val GIFT_TOKEN = "GIFT_TOKEN"
const val REFERRER = "REFERRER"
const val TETRO_UTM = "TETRO_UTM"
const val PUSH_TOPIC = "PUSH_TOPIC"
const val OMNITURE_PATH_VIEW = "OMNITURE_PATH_VIEW"
const val PRINT_ORIGINATED = "PRINT_ORIGINATED"
const val DEFAULT_FOR_YOU = "DEFAULT_FOR_YOU"
const val IS_CAROUSEL_ORIGINATED = "IS_CAROUSEL_ORIGINATED"
const val IS_RECIRC_MODULE_ORIGINATED = "IS_RECIRC_MODULE_ORIGINATED"
const val TAB_NAME = "TAB_NAME"
const val NAVIGATION_BEHAVIOR = "NAVIGATION_BEHAVIOR"
const val PUSH_SENT_TIMESTAMP = "PUSH_SENT_TIMESTAMP"
const val PUSH_TOPIC_PLATFORM = "PUSH_TOPIC_PLATFORM"
const val PUSH_ID = "PUSH_ID"
const val PUSH_HEADLINE = "PUSH_HEADLINE"
const val PUSH_TITLE = "PUSH_TITLE"
const val POSITION_IN_BRIGHTS = "POSITION_IN_BRIGHTS"
const val POSITION_IN_MY_POST_CAROUSEL = "POSITION_IN_MY_POST_CAROUSEL"
const val POSITION_IN_FOR_YOU_SECTION = "POSITION_IN_FOR_YOU_SECTION"
const val INLINE_LINK_ORIGINATED = "INLINE_LINK_ORIGINATED"
const val LUF_OUTCOME_POST_ORIGINATED = "LUF_OUTCOME_POST_ORIGINATED"
const val OPENED_FROM_SEARCH = "OPENED_FROM_SEARCH"
const val OPENED_FROM_SECTION_FRONT = "OPENED_FROM_SECTION_FRONT"
const val SHOULD_NOT_SUPPRESS_PAGE_VIEW = "SHOULD_NOT_SUPPRESS_PAGE_VIEW"
const val ITID = "ITID"
const val SOURCE_APP = "SOURCE_APP"
const val SHOULD_PLAY_AUDIO_ARTICLE = "SHOULD_PLAY_AUDIO_ARTICLE"
const val AUDIO_ARTICLE_POSITION = "AUDIO_ARTICLE_POSITION"
const val IS_FROM_RELATED_ARTICLE = "IS_FROM_RELATED_ARTICLE"

// Brought from legacy articles activity
const val EXTRA_REVEAL_ANIMATION = "EXTRA_REVEAL_ANIMATION"
const val ARTICLES_URL_PARAM = "ARTICLES_URL_PARAM"
const val CURRENT_ARTICLE_ID_PARAM = "CURRENT_ARTICLE_ID_PARAM"
const val LIVE_VIDEO_ORIGINATED = "LIVE_VIDEO_ORIGINATED"
const val CURRENT_TAB_NAME = "CurrentTabName"
const val CURRENT_APP_SECTION = "CurrentTabName"
const val SECTION_START_POS = "SECTION_START_POS"
const val EXTRA_PAGE_NUMBER = "EXTRA_PAGE_NUMBER"
const val BREAKING_NEWS_ORIGINATED = "BreakingNewsOriginated"

/**
 * A convenient class to create an articles activity intent,
 * pack data into it, and retrieve data from the intent
 */
class ArticlesParcel(
    private val intent: Intent,
) {
    private var articleMetaList: List<ArticleMeta>? = null
    private var articleIndex: Int = 0
    private var articleSectionDisplayName = ""
    private var carouselTitle = ""
    private var carouselCategoryId = ""

    init {
        val intentId = intent.getStringExtra(INTENT_ID)
        val originArticleList = articlesMetaMap[intentId].orEmpty()
        articleMetaList = originArticleList.distinct()
        val index = intent.getIntExtra(ARTICLES_LIST_INDEX_OF_CLICKED, 0)
        articleSectionDisplayName = intent.getStringExtra(SECTION_DISPLAY_NAME) ?: ""
        carouselTitle = intent.getStringExtra(CAROUSEL_TITLE) ?: ""
        carouselCategoryId = intent.getStringExtra(CAROUSEL_CATEGORY_ID) ?: ""
        if (index > 0 && index < originArticleList.size) {
            val clickedArticle = originArticleList[index]
            articleIndex = articleMetaList?.indexOfFirst { it.id == clickedArticle.id } ?: 0
        }
    }

    fun getIntent() = intent

    fun getArticleMetas(): List<ArticleMeta> = articleMetaList.orEmpty()
    fun getArticleUrls(): List<String>? = articleMetaList?.map { it.id }

    fun getFrontArticleIndex(): Int = articleIndex.coerceAtLeast(0)

    fun getSectionDisplayName() = articleSectionDisplayName

    fun getCarouselTitle() = carouselTitle

    fun getCarouselCategoryId() = carouselCategoryId

    fun isPushOriginated() = intent.getBooleanExtra(PUSH_ORIGINATED, false)

    fun linkType(): String? = intent.getStringExtra(LINK_TYPE)

    fun isAlertPageOriginated() = intent.getBooleanExtra(ALERT_PAGE_ORIGINATED, false)

    fun isLiveBlogOriginated() = intent.getBooleanExtra(LIVE_BLOG_ORIGINATEED, false)

    fun getPushTopic() = intent.getStringExtra(PUSH_TOPIC)

    fun isPrintOriginated() = intent.getBooleanExtra(PRINT_ORIGINATED, false)

    fun isDefaultForYou() = intent.getBooleanExtra(DEFAULT_FOR_YOU, false)

    fun isCarouselOriginated() = intent.getBooleanExtra(IS_CAROUSEL_ORIGINATED, false)

    fun isRecircModuleOriginated() = intent.getBooleanExtra(IS_RECIRC_MODULE_ORIGINATED, false)

    fun isDeepLinkOriginated() = intent.getBooleanExtra(DEEPLINK_ORIGINATED, false)

    fun isForYouSectionOriginated() = intent.getBooleanExtra(FOR_YOU_SECTION_ORIGINATED, false)

    fun isAirshipOriginated() = intent.getBooleanExtra(AIRSHIP_ORIGINATED, false)

    fun isOneLinkOriginated() = intent.getBooleanExtra(ONELINK_ORIGINATED, false)

    fun isHabitTilesOriginated() = intent.getBooleanExtra(HABIT_TILES_ORIGINATED, false)
    fun isAskThePostOriginated() = intent.getBooleanExtra(ASK_THE_POST_ORIGINATED, false)

    fun isSubNavLUFOriginated() = intent.getBooleanExtra(SUBNAV_LUF_ORIGINATED, false)

    fun isOpinionPushOriginated() = intent.getBooleanExtra(OPINION_PUSH_ORIGINATED, false)

    fun isFailoverOriginated() = intent.getBooleanExtra(FAILOVER_ORIGINATED, false)

    fun isOpenedFromSearch() = intent.getBooleanExtra(OPENED_FROM_SEARCH, false)

    fun isOpenedFromSectionFront() = intent.getBooleanExtra(OPENED_FROM_SECTION_FRONT, false)

    fun shouldNotSuppressPageView() = intent.getBooleanExtra(SHOULD_NOT_SUPPRESS_PAGE_VIEW, false)

    fun getGiftToken() = intent.getStringExtra(GIFT_TOKEN)

    fun getReferrer() = intent.getStringExtra(REFERRER)

    fun getTetroUtm() = intent.getStringExtra(TETRO_UTM)

    fun isGiftArticle() = !getGiftToken().isNullOrEmpty()

    fun isNewsprint() = intent.getBooleanExtra(NEWSPRINT_ORIGINATED, false)

    fun isWidgetOriginated() = intent.getBooleanExtra(WIDGET_ORIGINATED, false)

    fun isInlineLinkOriginated() = intent.getBooleanExtra(INLINE_LINK_ORIGINATED, false)

    fun isLufOutcomePostOriginated() = intent.getBooleanExtra(LUF_OUTCOME_POST_ORIGINATED, false)

    fun getOmnitureToPathView() = intent.getStringExtra(OMNITURE_PATH_VIEW)

    fun isWpmmArticle() = intent.getBooleanExtra(WPMM_PAYWALL, false)

    fun getTabName() = intent.getStringExtra(TAB_NAME)

    fun getNavigationBehavior() = intent.getStringExtra(NAVIGATION_BEHAVIOR)

    fun getPushId() = intent.getStringExtra(PUSH_ID)

    fun getPushSentTimestamp() = intent.getStringExtra(PUSH_SENT_TIMESTAMP)

    fun getPushTopicPlatform() = intent.getStringExtra(PUSH_TOPIC_PLATFORM)

    fun getPushHeadline() = intent.getStringExtra(PUSH_HEADLINE)

    fun getPushTitle() = intent.getStringExtra(PUSH_TITLE)

    fun getPositionInBrights() = intent.getIntExtra(POSITION_IN_BRIGHTS, 0)

    fun getPositionInMyPostCarousel() = intent.getIntExtra(POSITION_IN_MY_POST_CAROUSEL, 0)

    fun getPositionInForYouSection() = intent.getIntExtra(POSITION_IN_FOR_YOU_SECTION, 0)

    fun getItId() = intent.getStringExtra(ITID)

    fun getSourceApp() = intent.getStringExtra(SOURCE_APP)

    fun getIsFromRelatedArticle() = intent.getBooleanExtra(IS_FROM_RELATED_ARTICLE, false)

    fun getWidgetType() = intent.getStringExtra(WidgetData.EXTRAS_WIDGET_TYPE)
    fun getWidgetId() = intent.getStringExtra(WIDGET_ID)

    fun getArticleContentUrl() = intent.getStringExtra(CURRENT_ARTICLE_ID_PARAM)



    companion object {
        @JvmStatic
        fun builder(): Builder = ModernBuilder()

        //  In-memory cache used to temporarily store large ArticleMeta lists during navigation between screen
        //  This avoids passing large data through intent extras or bundles which can cause
        //  TransactionTooLargeException due to Binder limits
        private val articlesMetaMap: MutableMap<String, List<ArticleMeta>> = mutableMapOf()

        // Define how old a file can be before it's considered orphaned. 24 hours is a safe value.
        private val MAX_FILE_AGE_MS = TimeUnit.HOURS.toMillis(24)

        fun removeArticlesMeta(intent: Intent) {
            val intentId = intent.getStringExtra(INTENT_ID)
            articlesMetaMap.remove(intentId)
        }

        fun putArticlesMeta(intentId: String, data: List<ArticleMeta>) {
            val safeData = data.map {
                //  Set defaults - just like ArticleMeta.writeToParcel does
                if (it.articleLinkType == null) it.articleLinkType = ArticleLinkType.NONE
                it
            }
            articlesMetaMap.put(intentId, safeData)
        }
    }

    abstract class Builder {
        protected var articleMetas: List<ArticleMeta>? = null
        protected var articleUrls: List<String>? = null
        protected var articleUrl: String? = null
        protected var lastModified: String? = null
        protected var position: Int? = null
        protected var linkType: LinkType? = null
        protected var tabName: String? = null
        protected var sectionDisplayName: String? = null
        protected var carouselTitle: String? = null
        protected var carouselCategoryId: String? = null
        protected var appSection: String? = null
        protected var pushOriginated = false
        protected var alertOriginated = false
        protected var liveBlogOriginated = false
        protected var opinionPushOriginated = false
        protected var breakingNewsOriginated = false
        protected var carouselOriginated = false
        protected var recircModuleOriginated = false
        protected var followOriginated = false
        protected var printOriginated = false
        protected var pushTopic: String? = null
        protected var isLiveVideoOriginated = false
        protected var isSubNavLufOriginated = false
        protected var deepLinkOriginated = false
        protected var forYouSectionOriginated = false
        protected var airshipOriginated = false
        protected var oneLinkOriginated = false
        protected var habitTilesOriginated = false
        protected var askThePostOriginated = false
        protected var giftToken: String? = null
        protected var newsprintOriginated = false
        protected var widgetOriginated = false
        protected var inlineLinkOriginated = false
        protected var lufOutcomePostOriginated = false
        protected var failoverOriginated = false
        protected var widgetType: String? = null
        protected var wpmmPaywall = false
        protected var omniturePathToView: String? = null
        protected var navigationBehavior: String? = null
        protected var pushId: String? = null
        protected var itId: String? = null
        protected var pushHeadline: String? = null
        protected var pushTopicPlatform: String? = null
        protected var pushSentTimestamp: String? = null
        protected var pushTitle: String? = null
        protected var positionInCarousel: Int? = null
        protected var positionInMyPostCarousel: Int? = null
        protected var positionInForYouSection: Int? = null
        protected var tetroUtm: String? = null
        protected var referrer: String? = null
        protected var openedFromSectionFront: Boolean? = false
        protected var shouldNotSuppressPageView: Boolean? = false
        protected var openedFromSearch: Boolean? = false
        protected var sourceApp: String? = null
        protected var defaultForYou: Boolean? = false
        protected var shouldPlayAudioArticle = false
        protected var isFromRelatedArticle = false
        protected var categoryName: String? = null
        protected var categoryPath: String? = null
        protected var backActivityParam: String? = null
        protected var widgetId: String? = null


        fun setArticleMetas(
            list: List<ArticleMeta>?,
            position: Int?,
        ): Builder {
            this.articleMetas = list
            this.position = position
            return this
        }

        fun setArticleUrls(
            list: List<String>?,
            position: Int?,
        ): Builder {
            this.articleUrls = list
            this.position = position
            return this
        }

        fun setWidgetId(
            widgetId: String,
        ): Builder {
            this.widgetId = widgetId
            return this
        }

        fun setArticleSingleUrl(articleUrl: String?): Builder {
            this.articleUrl = articleUrl
            return this
        }

        fun setArticleSingleUrl(articleUrl: String?, lastModified: String? = null): Builder {
            this.articleUrl = articleUrl
            this.lastModified = lastModified
            return this
        }

        fun setLinkType(linkType: LinkType?): Builder {
            this.linkType = linkType
            return this
        }

        /**
         * News, Alerts, MyPost, Print, or For You
         */
        fun setTabName(tabName: String?): Builder {
            this.tabName = tabName
            return this
        }

        /**
         * used for tracking when user was first brought to For You section on launch
         */
        fun setDefaultForYou(defaultForYou: Boolean): Builder {
            this.defaultForYou = defaultForYou
            return this
        }

        /**
         * Used downstream for tracking, e.g. previous_page
         */
        fun setSectionDisplayName(sectionDisplayName: String?): Builder {
            this.sectionDisplayName = sectionDisplayName
            return this
        }

        fun setCarouselTitle(carouselTitle: String?): Builder {
            this.carouselTitle = carouselTitle
            return this
        }

        fun setCarouselCategoryId(carouselCategoryId: String?): Builder {
            this.carouselCategoryId = carouselCategoryId
            return this
        }

        fun setAppSection(appSection: String?): Builder {
            this.appSection = appSection
            return this
        }

        fun pushOriginated(isPushOriginated: Boolean): Builder {
            pushOriginated = isPushOriginated
            return this
        }

        fun alertOriginated(isAlertOriginated: Boolean): Builder {
            alertOriginated = isAlertOriginated
            return this
        }

        fun liveBlogOriginated(isLiveBlogOriginated: Boolean): Builder {
            liveBlogOriginated = isLiveBlogOriginated
            return this
        }

        fun opinionPushOriginated(opinionPushOriginated: Boolean): Builder {
            this.opinionPushOriginated = opinionPushOriginated
            if (opinionPushOriginated) {
                pushOriginated = opinionPushOriginated
            }
            return this
        }

        fun breakingNewsOriginated(isBreakingNewsOriginated: Boolean): Builder {
            breakingNewsOriginated = isBreakingNewsOriginated
            return this
        }

        fun setSubNavOriginated(subNavLufOriginated: Boolean): Builder {
            this.isSubNavLufOriginated = subNavLufOriginated
            return this
        }

        fun setCarouselOriginated(carouselOriginated: Boolean): Builder {
            this.carouselOriginated = carouselOriginated
            return this
        }

        fun setRecircModuleOriginated(recircModuleOriginated: Boolean): Builder {
            this.recircModuleOriginated = recircModuleOriginated
            return this
        }

        fun setPlayAudioArticle(shouldPlayAudioArticle: Boolean): Builder {
            this.shouldPlayAudioArticle = shouldPlayAudioArticle
            return this
        }

        fun followOriginated(followOriginated: Boolean): Builder {
            this.followOriginated = followOriginated
            return this
        }

        fun printOriginated(printOriginated: Boolean): Builder {
            this.printOriginated = printOriginated
            return this
        }

        fun setPushTopic(pushTopic: String?): Builder {
            this.pushTopic = pushTopic
            return this
        }

        fun liveVideoOriginated(isLiveVideoOriginated: Boolean): Builder {
            this.isLiveVideoOriginated = isLiveVideoOriginated
            return this
        }

        fun deepLinkOriginated(deepLinkOriginated: Boolean): Builder {
            this.deepLinkOriginated = deepLinkOriginated
            return this
        }

        fun forYouSectionOriginated(forYouSectionOriginated: Boolean): Builder {
            this.forYouSectionOriginated = forYouSectionOriginated
            return this
        }

        fun airshipOriginated(airshipOriginated: Boolean): Builder {
            this.airshipOriginated = airshipOriginated
            return this
        }

        fun oneLinkOriginated(oneLinkOriginated: Boolean): Builder {
            this.oneLinkOriginated = oneLinkOriginated
            return this
        }

        fun habitTilesOriginated(habitTilesOriginated: Boolean): Builder {
            this.habitTilesOriginated = habitTilesOriginated
            return this
        }

        fun askThePostOriginated(askThePostOriginated: Boolean): Builder {
            this.askThePostOriginated = askThePostOriginated
            return this
        }

        fun setGiftToken(token: String?): Builder {
            token?.let {
                this.giftToken = it
            }
            return this
        }

        fun setTetroUtm(tetroUtm: String?): Builder {
            tetroUtm?.let {
                this.tetroUtm = it
            }
            return this
        }

        fun setReferrer(referrer: String?): Builder {
            referrer?.let {
                this.referrer = it
            }
            return this
        }

        fun setNewsprintOriginated(newsprintOriginated: Boolean): Builder {
            this.newsprintOriginated = newsprintOriginated
            return this
        }

        fun widgetOriginated(widgetOriginated: Boolean): Builder {
            this.widgetOriginated = widgetOriginated
            return this
        }

        fun inlineLinkOriginated(inlineLinkOriginated: Boolean): Builder {
            this.inlineLinkOriginated = inlineLinkOriginated
            return this
        }

        fun lufOutcomePostOriginated(lufOutcomePostOriginated: Boolean): Builder {
            this.lufOutcomePostOriginated = lufOutcomePostOriginated
            return this
        }

        fun failoverOriginated(failoverOriginated: Boolean): Builder {
            this.failoverOriginated = failoverOriginated
            return this
        }

        fun widgetType(widgetType: String?): Builder {
            this.widgetType = widgetType
            return this
        }

        fun wpmmPaywall(wpmmPaywall: Boolean): Builder {
            this.wpmmPaywall = wpmmPaywall
            return this
        }

        fun setOmniturePathToView(omniturePathToView: String?): Builder {
            this.omniturePathToView = omniturePathToView
            return this
        }

        fun setNavigationBehavior(navigationBehavior: String?): Builder {
            this.navigationBehavior = navigationBehavior
            return this
        }

        fun setPushId(pushId: String?): Builder {
            this.pushId = pushId
            return this
        }

        fun setPushHeadline(pushHeadline: String?): Builder {
            this.pushHeadline = pushHeadline
            return this
        }

        fun setPushTopicPlatform(pushTopicPlatform: String?): Builder {
            this.pushTopicPlatform = pushTopicPlatform
            return this
        }

        fun setPushSentTimestamp(pushSentTimestamp: String?): Builder {
            this.pushSentTimestamp = pushSentTimestamp
            return this
        }

        fun setPushTitle(pushTitle: String?): Builder {
            this.pushTitle = pushTitle
            return this
        }

        fun setPositionInCarousel(position: Int): Builder {
            this.positionInCarousel = position
            return this
        }

        fun setArticleOpenedFromSectionFront(openedFromSectionFront: Boolean): Builder {
            this.openedFromSectionFront = openedFromSectionFront
            return this
        }

        fun setShouldNotSuppressPageView(shouldNotSuppressPageView: Boolean): Builder {
            this.shouldNotSuppressPageView = shouldNotSuppressPageView
            return this
        }

        fun setArticleOpenedFromSearch(openedFromSearch: Boolean): Builder {
            this.openedFromSearch = openedFromSearch
            return this
        }

        fun setPositionInMyPostCarousel(position: Int?): Builder {
            this.positionInMyPostCarousel = position
            return this
        }

        fun setPositionInForYouSection(position: Int?): Builder {
            this.positionInForYouSection = position
            return this
        }

        fun setIsFromRelatedArticle(isFromRelatedArticle: Boolean): Builder {
            this.isFromRelatedArticle = isFromRelatedArticle
            return this
        }

        /**
         * For analytics purposes.
         * This itId is sent in every page view event.
         * [itId] Generated for fusion section front stories.
         */
        fun setItId(itId: String?): Builder {
            this.itId = itId
            return this
        }

        fun setSourceApp(sourceApp: String?): Builder {
            this.sourceApp = sourceApp
            return this
        }

        fun setCategoryName(categoryName: String): Builder {
            this.categoryName = categoryName
            return this
        }

        fun setCategoryPath(categoryPath: String): Builder {
            this.categoryPath = categoryPath
            return this
        }

        fun setBackActivityParam(backActivityParam: String): Builder {
            this.backActivityParam = backActivityParam
            return this
        }

        abstract fun buildIntent(source: Context? = null): Intent
    }

    class ModernBuilder : Builder() {
        override fun buildIntent(source: Context?): Intent {
            val intent = Intent(source, Articles2Activity::class.java)
            val intentId = java.util.UUID.randomUUID().toString()
            intent.putExtra(INTENT_ID, intentId)

            articleUrl
                ?.let { getUrlAndAnchorRefPair(it) }
                ?.let {
                    ArticleMeta(it.first, false).apply {
                        anchorId = it.second

                        this@ModernBuilder.lastModified?.let { lastModifiedValue ->
                            try {
                                Utils.getDefaultDateFormat().parse(lastModifiedValue)?.time
                            } catch (_: Exception) {
                                null
                            }
                        }?.let { lastModifiedTime ->
                            lastModified = lastModifiedTime
                        }
                    }
                }
                ?.let { arrayListOf(it) }
                ?.also { putArticlesMeta(intentId, it) }

            articleUrls
                ?.map {
                    val pair = getUrlAndAnchorRefPair(it)
                    ArticleMeta(pair.first, false).also { meta -> meta.anchorId = pair.second }
                }?.also { putArticlesMeta(intentId, it) }

            articleMetas
                ?.let { putArticlesMeta(intentId, it) }

            position?.let {
                if (it >= 0) {
                    intent.putExtra(ARTICLES_LIST_INDEX_OF_CLICKED, it)
                }
            }

            giftToken?.let {
                intent.putExtra(GIFT_TOKEN, it)
            }

            referrer?.let {
                intent.putExtra(REFERRER, it)
            }

            tetroUtm?.let {
                intent.putExtra(TETRO_UTM, it)
            }

            positionInCarousel?.let {
                if (it >= 0) {
                    intent.putExtra(POSITION_IN_BRIGHTS, it)
                }
            }

            positionInMyPostCarousel?.let {
                if (it >= 0) {
                    intent.putExtra(POSITION_IN_MY_POST_CAROUSEL, it)
                }
            }
            positionInForYouSection?.let {
                if (it >= 0) {
                    intent.putExtra(POSITION_IN_FOR_YOU_SECTION, it)
                }
            }

            intent.apply {
                putExtra(SECTION_DISPLAY_NAME, sectionDisplayName)
                putExtra(CAROUSEL_TITLE, carouselTitle)
                putExtra(CAROUSEL_CATEGORY_ID, carouselCategoryId)
                putExtra(PUSH_ORIGINATED, pushOriginated)
                putExtra(ALERT_PAGE_ORIGINATED, alertOriginated)
                putExtra(LIVE_BLOG_ORIGINATEED, liveBlogOriginated)
                putExtra(LINK_TYPE, linkType?.name)
                putExtra(OPINION_PUSH_ORIGINATED, opinionPushOriginated)
                putExtra(NEWSPRINT_ORIGINATED, newsprintOriginated)
                putExtra(WIDGET_ORIGINATED, widgetOriginated)
                putExtra(DEEPLINK_ORIGINATED, deepLinkOriginated)
                putExtra(FOR_YOU_SECTION_ORIGINATED, forYouSectionOriginated)
                putExtra(AIRSHIP_ORIGINATED, airshipOriginated)
                putExtra(ONELINK_ORIGINATED, oneLinkOriginated)
                putExtra(HABIT_TILES_ORIGINATED, habitTilesOriginated)
                putExtra(ASK_THE_POST_ORIGINATED, askThePostOriginated)
                putExtra(WPMM_PAYWALL, wpmmPaywall)
                putExtra(PUSH_TOPIC, pushTopic)
                putExtra(OMNITURE_PATH_VIEW, omniturePathToView)
                putExtra(PRINT_ORIGINATED, printOriginated)
                putExtra(IS_CAROUSEL_ORIGINATED, carouselOriginated)
                putExtra(IS_RECIRC_MODULE_ORIGINATED, recircModuleOriginated)
                putExtra(INLINE_LINK_ORIGINATED, inlineLinkOriginated)
                putExtra(LUF_OUTCOME_POST_ORIGINATED, lufOutcomePostOriginated)
                putExtra(SUBNAV_LUF_ORIGINATED, isSubNavLufOriginated)
                putExtra(FAILOVER_ORIGINATED, failoverOriginated)
                putExtra(WidgetData.EXTRAS_WIDGET_TYPE, widgetType)
                putExtra(TAB_NAME, tabName)
                putExtra(NAVIGATION_BEHAVIOR, navigationBehavior)
                putExtra(SHOULD_PLAY_AUDIO_ARTICLE, shouldPlayAudioArticle)
                putExtra(PUSH_SENT_TIMESTAMP, pushSentTimestamp)
                putExtra(PUSH_HEADLINE, pushHeadline)
                putExtra(PUSH_ID, pushId)
                putExtra(PUSH_TOPIC_PLATFORM, pushTopicPlatform)
                putExtra(PUSH_TITLE, pushTitle)
                putExtra(ITID, itId)
                putExtra(OPENED_FROM_SECTION_FRONT, openedFromSectionFront)
                putExtra(SHOULD_NOT_SUPPRESS_PAGE_VIEW, shouldNotSuppressPageView)
                putExtra(OPENED_FROM_SEARCH, openedFromSearch)
                putExtra(SOURCE_APP, sourceApp)
                putExtra(DEFAULT_FOR_YOU, defaultForYou)
                putExtra(IS_FROM_RELATED_ARTICLE, isFromRelatedArticle)
                putExtra(TopBarFragment.SectionDisplayName, categoryName)
                putExtra(TopBarFragment.EXTRAS_BUNDLE_PATH, categoryPath)
                putExtra(TopBarFragment.BackActivityClassParam, backActivityParam)
                putExtra(CURRENT_ARTICLE_ID_PARAM, articleUrl)
                putExtra(WIDGET_ID, widgetId)
            }

            return intent
        }
    }
}