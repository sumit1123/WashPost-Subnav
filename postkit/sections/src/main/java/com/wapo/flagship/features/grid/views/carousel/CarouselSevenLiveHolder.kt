package com.wapo.flagship.features.grid.views.carousel

import android.graphics.Color
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.Logger
import com.wapo.Utils
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.extensions.toEllipsisActionItem
import com.wapo.flagship.features.grid.model.CarouselSevenLive
import com.wapo.flagship.features.grid.model.CarouselSevenLiveItem
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.model.Link
import com.wapo.flagship.features.grid.model.LinkType
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.flagship.features.grid.views.carousel.tracking.CarouselTrackingHelper
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.wapo.flagship.features.sections.tracking.SectionsTracker
import com.wapo.text.TextMeasure
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselEndCardViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselExternalViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselSevenLiveViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.recirculation.databinding.CarouselSevenLiveItemBinding
import com.washingtonpost.android.sections.R
import java.util.concurrent.TimeUnit

class CarouselSevenLiveHolder(
    itemView: View,
    val requestListener: CarouselProvider,
    val nightModeEnabled: Boolean,
    val parent: ViewGroup
) : GridViewHolder(itemView) {
    private lateinit var carouselSevenLive: CarouselSevenLive
    private var compoundLabel: CompoundLabel? = null
    private lateinit var compoundLabelView: CompoundLabelView
    private lateinit var carouselView: CarouselView
    private lateinit var ctaView: CompoundLabelView
    private var carouselTrackingHelper: CarouselTrackingHelper? = null
    private var gridAdapter: GridAdapter? = null
    private val itemsRefreshInterval: Long = TimeUnit.MINUTES.toMillis(1)

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        carouselSevenLive = gridAdapter.items[position] as CarouselSevenLive
        compoundLabel = carouselSevenLive.label
        carouselView = itemView.findViewById(R.id.carousel_view)
        ctaView = itemView.findViewById(R.id.cta_view)
        compoundLabelView = itemView.findViewById(R.id.compoundLabel)
        this.gridAdapter = gridAdapter
        val isFullWidth = carouselSevenLive.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val sideMargin = if (isSmallerBreakPoint) 0 else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding

        if (carouselSevenLive.label == null) {
            compoundLabelView.setVisible(false)
        } else {
            compoundLabelView.setLabel(carouselSevenLive.label)
            compoundLabelView.setVisible(true)
        }

        if (carouselSevenLive.cta == null) {
            ctaView.setVisible(false)
        } else {
            ctaView.setLabel(carouselSevenLive.cta)
            ctaView.setVisible(true)
            ctaView.setOnClickListener {
                carouselSevenLive.cta?.let {
                    val itId = "${it.link?.itId}_recirc"
                    gridAdapter.onLabelClicked?.invoke(it, itId)
                }
            }
        }

        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                if (url.isNullOrEmpty()) return
                if (isSmallerBreakPoint && positionInCarousel == carouselSevenLive.items.size) {
                    // end card
                    url.let {
                        carouselSevenLive.cta?.let {
                            val itId = "${it.link?.itId}_end_card"
                            gridAdapter.onLabelClicked?.invoke(it, itId)
                        }
                    }
                } else {
                    gridAdapter.onCarouselSevenLiveCardClicked
                        ?.invoke(carouselSevenLive.items.map { getLink(it) ?: it.link }, positionInCarousel)
                    gridAdapter.backToFront = true
                }
            }
        }

        val cardWidth = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_external_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_external_card_padding)
        val mediaAspectRatio = (carouselSevenLive.items.minOfOrNull { it.media?.aspectRatio ?: 1f }) ?: 1f
        carouselView.initLayout(
            requestListener, clickListener,
            object : CarouselView.CarouselConsumeTouchEventRule {
                override fun canCarouselConsumeTouchEvent(): Boolean {
                    return true
                }
            },
            shouldDisplaySectionName = false,
            shouldDisplayArrow = !isSmallerBreakPoint,
            shouldDisplayDateTime = false,
            fixedCardWidth = cardWidth,
            fixedCardPadding = cardPadding,
            marginStart = sideMargin,
            marginEnd = sideMargin,
            onEllipsisClick = { actionItem ->
                gridAdapter.onEllipsisClick?.invoke(
                    actionItem.toEllipsisActionItem(menuType = EllipsisMenu.Carousel)
                )
            },
            isNewsprint = carouselSevenLive.isNewsprint
        )

        val sevenLiveCarouselItems: MutableList<CarouselViewItem>? =
            MutableList(carouselSevenLive.items.size) { index ->
                val item = carouselSevenLive.items[index]
                val art = item.media
                val link = item.link
                val headline = item.headline
                val byLine = item.signature ?: ""
                val secondaryLabel = item.secondaryText
                val kicker = item.kicker
                val liveImage = item.liveImage ?: false
                val style = item.style
                val artUrl: String? = if (liveImage) {
                    val tabs = art?.liveImage?.tabs
                    if (!tabs.isNullOrEmpty() && tabs[0].images.isNotEmpty()) {
                        if (nightModeEnabled)
                            tabs[0].images[0].darkModeUrl
                        else
                            tabs[0].images[0].url
                    } else null
                } else {
                    art?.url
                }
                val artAspectRatio = if (liveImage) {
                    val tabs = art?.liveImage?.tabs
                    if (!tabs.isNullOrEmpty() && tabs[0].images.isNotEmpty()) {
                        tabs[0].images[0].aspectRatio
                    } else mediaAspectRatio
                } else mediaAspectRatio
                val refreshInterval = if (liveImage) itemsRefreshInterval else -1
                val linkUrl = (getLink(item) ?: item.link).url
                val dateLong = Utils.toDateLong(item.timestamp, Utils.getDefaultDateFormat())
                val diff = System.currentTimeMillis() - dateLong
                val date = if (dateLong > 0L && diff >= 0L) {
                    Utils.getAbbreviatedRelativeTime(dateLong)
                } else {
                    null
                }
                val oneHourInMillis = TimeUnit.HOURS.toMillis(1)
                val isRecent = dateLong > 0L && diff in 0L..oneHourInMillis

                CarouselSevenLiveViewItem(
                    headline.text,
                    artUrl,
                    artAspectRatio,
                    null,
                    linkUrl,
                    null,
                    carouselSevenLive.label?.text,
                    kicker,
                    byLine,
                    link.type == LinkType.WEB,
                    headline.prefix,
                    liveImage,
                    carouselSevenLive.cardify,
                    refreshInterval,
                    style,
                    secondaryLabel,
                    date,
                    isRecent
                )
            }

        calculateMinCardHeight(sevenLiveCarouselItems)

        carouselView.setInStoryDividers()
        carouselView.setOnCarouselItemTouchListener(object : OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                itemView.findActivityOfType<GridActivity>()?.let { activity ->
                    val pagerView = activity.getGridEnvironment().getPager()
                    pagerView?.setShouldAllowScroll(!disallowIntercept)
                }
            }
        })

        // Add end card for mobile only
        if (isSmallerBreakPoint) {
            carouselSevenLive.cta?.let {
                if (it.link?.url != null && it.text != null) {
                    it.link?.let { link ->
                        val endCard = CarouselEndCardViewItem(link.url, it.text, carouselSevenLive.cardify)
                        sevenLiveCarouselItems?.add(endCard)
                    }
                }
            }
        }
        carouselView.carouselItemsFetchListener.onCarouselItemsFetched(sevenLiveCarouselItems)

        carouselView.recyclerView?.apply {
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                private val sectionTracker: SectionsTracker? = SectionTrackerFactory.get(itemView.context)
                private val llm = carouselView.recyclerView?.layoutManager as? LinearLayoutManager
                private var previousScrollPosition: Int = 1
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val newScrollPosition = llm?.findFirstCompletelyVisibleItemPosition()?.plus(1) ?: return

                        if (newScrollPosition > previousScrollPosition) {
                            sectionTracker?.trackImmersionCarouselNavigation(NAVIGATION_SEVEN_LIVE_FORWARD)
                        } else if (newScrollPosition < previousScrollPosition) {
                            sectionTracker?.trackImmersionCarouselNavigation(NAVIGATION_SEVEN_LIVE_BACK)
                        }
                        previousScrollPosition = newScrollPosition
                        Logger.d(TAG, "CarouselSevenLive, onScrollStateChanged(), this=${this.hashCode()}")
                    }
                }
            })
        }

        if (isSmallerBreakPoint) {
            val horizontalPadding = parent.context.resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding)
            if (horizontalPadding > 0) {
                carouselView.recyclerView!!.setPadding(horizontalPadding.toInt(), 0, horizontalPadding.toInt(), 0)
                compoundLabelView.setPadding(horizontalPadding.toInt(), compoundLabelView.paddingTop, horizontalPadding.toInt(), compoundLabelView.paddingBottom)
                ctaView.setPadding(horizontalPadding.toInt(), itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), horizontalPadding.toInt(), compoundLabelView.paddingBottom)
            }
        } else {
            compoundLabelView.setPadding(sideMargin, 0, sideMargin, 0)
            ctaView.setPadding(sideMargin, itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), sideMargin, 0)
        }

        initTracking()

        if (carouselSevenLive.isNewsprint) {
            NewsprintHelper.applyNewsprintGradient(itemView, carouselSevenLive)
            compoundLabelView.setTextColor(Color.WHITE)
            compoundLabelView.updateKickerLabelColors(Color.WHITE)
        }
    }

    private fun getLink(item: CarouselSevenLiveItem?): Link? {
        item ?: return null
        return if (item.liveImage == true) item.media?.link else item.link
    }

    private fun initTracking() {
        carouselTrackingHelper = object : CarouselTrackingHelper() {
            override fun trackCarouselSeen() {
                SectionTrackerFactory.get(itemView.context).trackSevenLiveCarouselSeen(gridAdapter?.backToFront ?: false)
                gridAdapter?.backToFront = false
            }
        }
        carouselTrackingHelper?.bind(carouselView.recyclerView)
    }

    override fun unbind() {
        super.unbind()
        carouselTrackingHelper?.unbind(carouselView.recyclerView)
        carouselTrackingHelper = null
        this.gridAdapter = null
    }

    private fun calculateMinCardHeight(carouselViewItems: List<CarouselViewItem>?) {
        var tallestHeadlineHeight = 0
        var tallestItem: CarouselViewItem? = null
        val cardVerticalMargins = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_card_text_margin_large) * 2
        val cardWidth = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_external_card_width) - cardVerticalMargins

        carouselViewItems?.forEach { item ->
            val timestampTextHeight = if (item is CarouselSevenLiveViewItem && !item.relativeTime.isNullOrEmpty()) {
                TextMeasure().measureTextHeight(
                    SpannableString(item.relativeTime),
                    cardWidth,
                    itemView.context,
                    com.washingtonpost.android.recirculation.R.style.external_carousel_item_kicker
                )
            } else 0

            val secondaryTextHeight = if (!item.secondaryLabel.isNullOrEmpty())
                TextMeasure().measureTextHeight(SpannableString(item.secondaryLabel), cardWidth, itemView.context, com.washingtonpost.android.recirculation.R.style.external_carousel_item_byline) else 0
            val parsedTitle = HtmlCompat.fromHtml(item.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val headlineTextHeight = if (item.title.isNotEmpty())
                TextMeasure().measureTextHeight(SpannableString(parsedTitle), cardWidth, itemView.context, com.washingtonpost.android.recirculation.R.style.audio_carousel_item_headline) else 0
            val bylineTextHeight = if (item.byline.isNotEmpty())
                TextMeasure().measureTextHeight(item.byline, cardWidth, itemView.context, com.washingtonpost.android.recirculation.R.style.external_carousel_item_byline) else 0
            val imageReqH = if (item is CarouselSevenLiveViewItem && item.liveImage == true && item.artAspectRatio != null)
                (cardWidth / (item.artAspectRatio ?: 1f)).toInt() else 0
            val totalReqH = timestampTextHeight + headlineTextHeight + bylineTextHeight + imageReqH + secondaryTextHeight
            if (totalReqH > tallestHeadlineHeight || (totalReqH == tallestHeadlineHeight && (tallestItem?.title?.length ?: 0) < item.title.length)) {
                tallestHeadlineHeight = totalReqH
                tallestItem = item
            }
        }

        tallestItem?.let {
            val dummyView: CarouselSevenLiveItemBinding = CarouselSevenLiveItemBinding.inflate(LayoutInflater.from(itemView.context))
            val imageReqH = if (it is CarouselSevenLiveViewItem && it.liveImage == true && it.artAspectRatio != null) {
                dummyView.itemSevenLiveArt.visibility = View.GONE
                (cardWidth / (it.artAspectRatio ?: 1f)).toInt()
            } else {
                dummyView.itemSevenLiveArt.visibility = View.VISIBLE
                0
            }

            if (it is CarouselSevenLiveViewItem && !it.relativeTime.isNullOrEmpty()) {
                dummyView.sevenLiveTimestamp.text = it.relativeTime
                dummyView.sevenLiveTimestamp.visibility = View.VISIBLE
            } else {
                dummyView.sevenLiveTimestamp.visibility = View.GONE
            }

            dummyView.secondaryLabel.visibility = if (!it.secondaryLabel.isNullOrEmpty()) { dummyView.secondaryLabel.text = it.secondaryLabel; View.VISIBLE } else View.GONE
            dummyView.sevenLiveHeadlineTitle.visibility = if (it.title.isNotEmpty()) { dummyView.sevenLiveHeadlineTitle.text = it.title; View.VISIBLE } else View.GONE

            dummyView.root.measure(
                View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            val cardVerticalMarginsInner = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_card_text_margin_large)

            val measuredHeight = if (dummyView.itemSevenLiveArt.isGone &&
                dummyView.sevenLiveTimestamp.isGone &&
                dummyView.secondaryLabel.isGone &&
                dummyView.sevenLiveHeadlineTitle.isGone) {
                0
            } else {
                dummyView.root.measuredHeight + cardVerticalMarginsInner
            }

            carouselView.recyclerView.minimumHeight = measuredHeight + imageReqH
            carouselView.recyclerView.layoutParams.height = measuredHeight + imageReqH
        }
    }

    companion object {
        private val TAG = CarouselSevenLiveHolder::class.java.toString()
        const val NAVIGATION_SEVEN_LIVE_FORWARD = "seven_live_carousel_forward"
        const val NAVIGATION_SEVEN_LIVE_BACK = "seven_live_carousel_back"
    }
}