package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.extensions.toEllipsisActionItem
import com.wapo.flagship.features.grid.model.CarouselRecipe
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.flagship.features.grid.views.carousel.tracking.CarouselTrackingHelper
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.wapo.flagship.features.sections.tracking.SectionsTracker
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselRecipeViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.sections.R

class CarouselRecipeHolder(
    itemView: View, val requestListener: CarouselProvider, val parent: ViewGroup
) : GridViewHolder(itemView) {
    private lateinit var carouselRecipe: CarouselRecipe
    private lateinit var carouselView: CarouselView
    private var compoundLabel: CompoundLabel? = null
    private lateinit var compoundLabelView: CompoundLabelView
    private lateinit var ctaView: CompoundLabelView
    private var carouselTrackingHelper: CarouselTrackingHelper? = null
    private var gridAdapter: GridAdapter? = null

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        carouselRecipe = gridAdapter.items[position] as CarouselRecipe
        compoundLabel = carouselRecipe.label
        carouselView = itemView.findViewById(R.id.carousel_view)
        compoundLabelView = itemView.findViewById(R.id.compoundLabel)
        ctaView = itemView.findViewById(R.id.cta_view)
        this.gridAdapter = gridAdapter
        val isFullWidth = carouselRecipe.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val sideMargin = if (isSmallerBreakPoint) 0 else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding
        if (carouselRecipe.label == null) {
           compoundLabelView.setVisible(false)
        } else {
            if (isSmallerBreakPoint) {
                carouselRecipe.label?.secondaryText = "1 of ${carouselRecipe.items.size}"
            } else {
                carouselRecipe.label?.secondaryText = null
            }
            compoundLabelView.setLabel(carouselRecipe.label)
            compoundLabelView.setVisible(true)
        }

        if (carouselRecipe.cta == null) {
            ctaView.setVisible(false)
        } else {
            ctaView.setLabel(carouselRecipe.cta)
            ctaView.setVisible(true)
            ctaView.setOnClickListener {
                carouselRecipe.cta?.let {
                    gridAdapter.onLabelClicked?.invoke(it, null)
                }
            }
        }

        val cardWidth = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_immersion_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_immersion_card_padding)
        val cardAspectRatio = (carouselRecipe.items.minOfOrNull { it.media?.aspectRatio ?: 1f }) ?: 1f
        val cardHeight = cardWidth / cardAspectRatio
        carouselView.cardHeight = cardHeight.toInt()
        carouselView.recyclerView.minimumHeight = cardHeight.toInt()

        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                gridAdapter.onCarouselRecipeCardClicked
                    ?.invoke(carouselRecipe.items.map { it.link }, positionInCarousel)
                gridAdapter.backToFront = true
            }
        }

        carouselView.initLayout(requestListener, clickListener,
            object : CarouselView.CarouselConsumeTouchEventRule {
                override fun canCarouselConsumeTouchEvent(): Boolean {
                    return true
                }
            },
            shouldDisplaySectionName = false,
            shouldDisplayArrow = !isSmallerBreakPoint,
            shouldDisplayDateTime = false,
            marginStart = sideMargin,
            marginEnd = sideMargin,
            fixedCardWidth = cardWidth,
            fixedCardPadding = cardPadding,
            onBookmarkClick = { actionItem, view, isStatusChecked ->
                gridAdapter.onBookmarkClick?.invoke(actionItem.toEllipsisActionItem(menuType = EllipsisMenu.Carousel), view, isStatusChecked)
            })


        val immersionCarouselItems: MutableList<CarouselViewItem>? =
            MutableList(carouselRecipe.items.size) { index ->
                val art = carouselRecipe.items[index].media
                val link = carouselRecipe.items[index].link.url
                val headline = carouselRecipe.items[index].headline.text
                val duration = carouselRecipe.items[index].recipeInfo?.totalTime?.toLong()
                val rating = carouselRecipe.items[index].rating?.value ?: 0.0
                val reviews = carouselRecipe.items[index].rating?.count ?: 0
                var course = ""
                if ((carouselRecipe.items[index].recipeInfo?.courses?.size ?: 0) > 0) {
                    course = carouselRecipe.items[index].recipeInfo?.courses?.get(0)?.description ?: ""
                }
                CarouselRecipeViewItem(
                    headline,
                    art?.url,
                    cardAspectRatio,
                    link,
                    "",
                    false,
                    duration,
                    course,
                    rating,
                    reviews,
                    carouselRecipe.cardify
                )
            }
        carouselView.carouselItemsFetchListener.onCarouselItemsFetched(immersionCarouselItems)

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
                            val currentPosition = if (newScrollPosition > carouselRecipe.items.size) carouselRecipe.items.size else newScrollPosition
                            if (currentPosition > 0) {
                                compoundLabel?.secondaryText = "$currentPosition of ${carouselRecipe.items.size}"
                                compoundLabelView.setLabel(compoundLabel)
                            }
                        }

                        if (newScrollPosition > previousScrollPosition) {
                            sectionTracker?.trackImmersionCarouselNavigation(CarouselRecipeHolder.NAVIGATION_IMMERSION_CAROUSEL_FORWARD)
                        } else if (newScrollPosition < previousScrollPosition) {
                            sectionTracker?.trackImmersionCarouselNavigation(CarouselRecipeHolder.NAVIGATION_IMMERSION_CAROUSEL_BACK)
                        }

                        previousScrollPosition = newScrollPosition

                        Logger.d("", "CarouselImmersion, onScrollStateChanged(), this=${this.hashCode()}")
                    }
                }
            })
        }

        if (isSmallerBreakPoint) {
            val horizontalPadding =
                parent.context.resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding)
            if (horizontalPadding > 0) {
                carouselView.recyclerView.setPadding(
                    horizontalPadding.toInt(),
                    0,
                    horizontalPadding.toInt(),
                    0
                )
                compoundLabelView.setPadding(horizontalPadding.toInt(), compoundLabelView.paddingTop, horizontalPadding.toInt(), 0)
                ctaView.setPadding(horizontalPadding.toInt(), itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), horizontalPadding.toInt(), 0)
            }
        } else {
            compoundLabelView.setPadding(sideMargin, 0, sideMargin, 0)
            ctaView.setPadding(sideMargin, itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), sideMargin, 0)
        }

        initTracking()
    }

    private fun initTracking() {
        carouselTrackingHelper = object : CarouselTrackingHelper() {
            override fun trackCarouselSeen() {
                SectionTrackerFactory.get(itemView.context).trackImmersionCarouselSeen(gridAdapter?.backToFront ?: false)
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

    companion object {
        private val TAG = CarouselRecipeHolder::class.java.toString()
        const val NAVIGATION_IMMERSION_CAROUSEL_FORWARD = "immersion_carousel_forward"
        const val NAVIGATION_IMMERSION_CAROUSEL_BACK = "immersion_carousel_back"
    }
}