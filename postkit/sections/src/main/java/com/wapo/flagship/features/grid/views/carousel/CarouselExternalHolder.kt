package com.wapo.flagship.features.grid.views.carousel

import android.graphics.Color
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.extensions.toEllipsisActionItem
import com.wapo.flagship.features.grid.model.CarouselExternal
import com.wapo.flagship.features.grid.model.CarouselExternalItem
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
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.recirculation.databinding.CarouselExternalItemBinding
import com.washingtonpost.android.sections.R
import java.util.concurrent.TimeUnit
import kotlin.collections.forEach
import androidx.core.view.isGone

class CarouselExternalHolder(
    itemView: View,
    val requestListener: CarouselProvider,
    val nightModeEnabled: Boolean,
    val parent: ViewGroup
) : GridViewHolder(itemView) {
    private lateinit var carouselExternal: CarouselExternal
    private var compoundLabel: CompoundLabel? = null
    private lateinit var compoundLabelView: CompoundLabelView
    private lateinit var carouselView: CarouselView
    private lateinit var ctaView: CompoundLabelView
    private var carouselTrackingHelper: CarouselTrackingHelper? = null
    private var gridAdapter: GridAdapter? = null
    private val itemsRefreshInterval: Long = TimeUnit.MINUTES.toMillis(1)

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        carouselExternal = gridAdapter.items[position] as CarouselExternal
        compoundLabel = carouselExternal.label
        carouselView = itemView.findViewById(R.id.carousel_view)
        ctaView = itemView.findViewById(R.id.cta_view)
        compoundLabelView = itemView.findViewById(R.id.compoundLabel)
        this.gridAdapter = gridAdapter
        val isFullWidth = carouselExternal?.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val sideMargin = if (isSmallerBreakPoint) 0 else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding
        if (carouselExternal.label == null) {
            compoundLabelView.setVisible(false)
        } else {
            if (isSmallerBreakPoint) {
                carouselExternal.label?.secondaryText = "1 of ${carouselExternal.items.size}"
            } else {
                carouselExternal.label?.secondaryText = null
            }
            compoundLabelView.setLabel(carouselExternal.label)
            compoundLabelView.setVisible(true)
        }

        if (carouselExternal.cta == null) {
            ctaView.setVisible(false)
        } else {
            ctaView.setLabel(carouselExternal.cta)
            ctaView.setVisible(true)
            ctaView.setOnClickListener {
                carouselExternal.cta?.let {
                    val itId = "${it.link?.itId}_recirc"
                    gridAdapter.onLabelClicked?.invoke(it, itId)
                }
            }
        }

        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                if (url.isNullOrEmpty()) return
                if (isSmallerBreakPoint && positionInCarousel == carouselExternal.items.size) {
                    // end card
                    url.let {
                        carouselExternal.cta?.let {
                            val itId = "${it.link?.itId}_end_card"
                            gridAdapter.onLabelClicked?.invoke(it, itId)
                        }
                    }
                } else {
                    gridAdapter.onCarouselExternalCardClicked
                        ?.invoke(carouselExternal.items.map { getLink(it) ?: it.link }, positionInCarousel)
                    gridAdapter.backToFront = true
                }
            }
        }

        val cardWidth = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_external_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_external_card_padding)
        val mediaAspectRatio = (carouselExternal.items.minOfOrNull { it.media?.aspectRatio ?: 1f }) ?: 1f
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
            isNewsprint = carouselExternal.isNewsprint
        )

        // Set Data to CarouselView
        val externalCarouselItems: MutableList<CarouselViewItem>? =
            MutableList(carouselExternal.items.size) { index ->
                val item = carouselExternal.items[index]
                val art = carouselExternal.items[index].media
                val link = carouselExternal.items[index].link
                val headline = carouselExternal.items[index].headline
                val byLine = carouselExternal.items[index].signature ?: ""
                val secondaryLabel = carouselExternal.items[index].secondaryText
                val kicker = carouselExternal.items[index].kicker
                val liveImage = carouselExternal.items[index].liveImage ?: false
                val style = carouselExternal.items[index].style
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
                CarouselExternalViewItem(
                    headline.text,
                    artUrl,
                    artAspectRatio,
                    null,
                    linkUrl,
                    null,
                    carouselExternal.label?.text,
                    kicker,
                    byLine,
                    link.type == LinkType.WEB,
                    headline.prefix,
                    liveImage,
                    carouselExternal.cardify,
                    refreshInterval,
                    style,
                    secondaryLabel
                )
            }

        calculateMinCardHeight(externalCarouselItems)

        carouselView.setInStoryDividers()
        carouselView.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                itemView.findActivityOfType<GridActivity>()?.let { activity ->
                    val pagerView = activity.getGridEnvironment().getPager()
                    pagerView?.setShouldAllowScroll(!disallowIntercept)
                }
            }
        })

        //add end card for only mobile
        if (isSmallerBreakPoint) {
            carouselExternal.cta?.let {
                if (it.link?.url != null && it.text != null) {
                    // Only add the end card if both the url and the text are not null
                    it.link?.let { link ->
                        val endCard = CarouselEndCardViewItem(link.url, it.text, carouselExternal.cardify)
                        externalCarouselItems?.add(endCard)
                    }
                }
            }

        }
        carouselView.carouselItemsFetchListener.onCarouselItemsFetched(externalCarouselItems)

        carouselView.recyclerView?.apply {
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                private val sectionTracker: SectionsTracker? =
                    SectionTrackerFactory.get(itemView.context)
                private val llm = carouselView.recyclerView?.layoutManager as? LinearLayoutManager
                private var previousScrollPosition: Int = 1
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val newScrollPosition =
                            llm?.findFirstCompletelyVisibleItemPosition()?.plus(1) ?: return
                        if (compoundLabel != null && isSmallerBreakPoint) {
                            val currentPosition = if (newScrollPosition > carouselExternal.items.size) carouselExternal.items.size else newScrollPosition
                            if (currentPosition > 0) {
                                compoundLabel?.secondaryText = "$currentPosition of ${carouselExternal.items.size}"
                                compoundLabelView.setLabel(compoundLabel)
                            }
                        }

                        if (newScrollPosition > previousScrollPosition) {
                            sectionTracker?.trackImmersionCarouselNavigation(NAVIGATION_IMMERSION_CAROUSEL_FORWARD)
                        } else if (newScrollPosition < previousScrollPosition) {
                            sectionTracker?.trackImmersionCarouselNavigation(NAVIGATION_IMMERSION_CAROUSEL_BACK)
                        }

                        previousScrollPosition = newScrollPosition

                        Logger.d(TAG, "CarouselImmersion, onScrollStateChanged(), this=${this.hashCode()}")
                    }
                }
            })
        }

        if (isSmallerBreakPoint) {
            val horizontalPadding =
                parent.context.resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding)
            if (horizontalPadding > 0) {
                carouselView.recyclerView!!.setPadding(
                    horizontalPadding.toInt(),
                    0,
                    horizontalPadding.toInt(),
                    0
                )
                compoundLabelView.setPadding(horizontalPadding.toInt(), compoundLabelView.paddingTop, horizontalPadding.toInt(), compoundLabelView.paddingBottom)
                ctaView.setPadding(horizontalPadding.toInt(), itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), horizontalPadding.toInt(), compoundLabelView.paddingBottom)
            }
        } else {
            compoundLabelView.setPadding(sideMargin, 0, sideMargin, 0)
            ctaView.setPadding(sideMargin, itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), sideMargin, 0)
        }

        initTracking()

        if (carouselExternal.isNewsprint) {
            NewsprintHelper.applyNewsprintGradient(itemView, carouselExternal)
            compoundLabelView.setTextColor(Color.WHITE)
            compoundLabelView.updateKickerLabelColors(Color.WHITE)
        }
    }

    private fun getLink(item: CarouselExternalItem?): Link? {
        item ?: return null
        return if (item.liveImage == true) item.media?.link else item.link
    }

    private fun initTracking() {
        carouselTrackingHelper = object : CarouselTrackingHelper() {
            override fun trackCarouselSeen() {
                SectionTrackerFactory.get(itemView.context).trackExternalCarouselSeen(gridAdapter?.backToFront ?: false)
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
            val kickerTextHeight = if (!item.kicker.isNullOrEmpty())
                TextMeasure().measureTextHeight(
                    SpannableString(item.kicker),
                    cardWidth,
                    itemView.context,
                    com.washingtonpost.android.recirculation.R.style.external_carousel_item_kicker
                ) else 0
            val secondaryTextHeight = if (!item.secondaryLabel.isNullOrEmpty())
                TextMeasure().measureTextHeight(
                    SpannableString(item.secondaryLabel),
                    cardWidth,
                    itemView.context,
                    com.washingtonpost.android.recirculation.R.style.external_carousel_item_byline
                ) else 0
            val headlineTextHeight = if (item.title.isNotEmpty())
                TextMeasure().measureTextHeight(
                    item.title,
                    cardWidth,
                    itemView.context,
                    com.washingtonpost.android.recirculation.R.style.audio_carousel_item_headline
                ) else 0
            val bylineTextHeight = if (item.byline.isNotEmpty())
                TextMeasure().measureTextHeight(
                    item.byline,
                    cardWidth,
                    itemView.context,
                    com.washingtonpost.android.recirculation.R.style.external_carousel_item_byline
                ) else 0
            val imageReqH = if (item is CarouselExternalViewItem && item.liveImage == true && item.artAspectRatio != null)
                (cardWidth / (item.artAspectRatio ?: 1f)).toInt() else 0
            val totalReqH = kickerTextHeight + headlineTextHeight + bylineTextHeight + imageReqH + secondaryTextHeight
            if (totalReqH > tallestHeadlineHeight
                || (totalReqH == tallestHeadlineHeight && (tallestItem?.title?.length ?: 0) < item.title.length)) {
                tallestHeadlineHeight = totalReqH
                tallestItem = item
            }
        }

        tallestItem?.let {
            val dummyView: CarouselExternalItemBinding =
                CarouselExternalItemBinding.inflate(LayoutInflater.from(itemView.context))
            val imageReqH = if (it is CarouselExternalViewItem && it.liveImage == true && it.artAspectRatio != null) {
                dummyView.itemExternalArt.visibility = View.GONE
                (cardWidth / (it.artAspectRatio ?: 1f)).toInt()
            } else {
                dummyView.itemExternalArt.visibility = View.VISIBLE
                0
            }
            if (!it.kicker.isNullOrEmpty()) {
                dummyView.externalKicker.visibility = View.VISIBLE
                dummyView.externalKicker.text = it.kicker
            } else dummyView.externalKicker.visibility = View.GONE
            if (!it.secondaryLabel.isNullOrEmpty()) {
                dummyView.secondaryLabel.visibility = View.VISIBLE
                dummyView.secondaryLabel.text = it.secondaryLabel
            } else dummyView.secondaryLabel.visibility = View.GONE
            if (it.title.isNotEmpty()) {
                dummyView.externalHeadlineTitle.visibility = View.VISIBLE
                dummyView.externalHeadlineTitle.text = it.title
            } else dummyView.externalHeadlineTitle.visibility = View.GONE

            dummyView.root.measure(
                View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            val cardVerticalMargins = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_card_text_margin_large)
            val measuredHeight = if (dummyView.itemExternalArt.isGone
                && dummyView.externalKicker.isGone
                && dummyView.secondaryLabel.isGone
                && dummyView.externalHeadlineTitle.isGone
            ) 0 else dummyView.root.measuredHeight + cardVerticalMargins
            val tallestCardHeight = measuredHeight + imageReqH

            carouselView.recyclerView.minimumHeight = tallestCardHeight
            carouselView.recyclerView.layoutParams.height = tallestCardHeight
        }
    }

    companion object {
        private val TAG = CarouselExternalHolder::class.java.toString()
        const val NAVIGATION_IMMERSION_CAROUSEL_FORWARD = "immersion_carousel_forward"
        const val NAVIGATION_IMMERSION_CAROUSEL_BACK = "immersion_carousel_back"
    }

}