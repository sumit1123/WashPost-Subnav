package com.wapo.flagship.features.grid

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.events.ActionButtonEvent
import com.wapo.flagship.features.grid.model.ArtPosition
import com.wapo.flagship.features.grid.model.ArtWidth
import com.wapo.flagship.features.grid.model.Audio
import com.wapo.flagship.features.grid.model.AudioArticle
import com.wapo.flagship.features.grid.model.Bleed
import com.wapo.flagship.features.grid.model.BleedItemType
import com.wapo.flagship.features.grid.model.BlurbList
import com.wapo.flagship.features.grid.model.CardSegmentType
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.Count
import com.wapo.flagship.features.grid.model.FootNote
import com.wapo.flagship.features.grid.model.Headline
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.LiveBlog
import com.wapo.flagship.features.grid.model.Media
import com.wapo.flagship.features.grid.model.MediaType
import com.wapo.flagship.features.grid.model.RelatedLinks
import com.wapo.flagship.features.grid.model.Signature
import com.wapo.flagship.features.grid.model.SlideShow
import com.wapo.flagship.features.grid.model.SubItemType
import com.wapo.flagship.features.grid.model.VerticalAlignment
import com.wapo.flagship.features.grid.model.WebComponent
import com.wapo.flagship.features.grid.model.Zone
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.flagship.features.grid.views.CountView
import com.wapo.flagship.features.grid.views.FootNoteView
import com.wapo.flagship.features.grid.views.LiveImageContainerView
import com.wapo.flagship.features.grid.views.SignatureActionsView
import com.wapo.flagship.features.grid.views.SlideShowContainerView
import com.wapo.flagship.features.grid.views.WebComponentView
import com.wapo.flagship.features.pagebuilder.BlurbView
import com.wapo.flagship.features.pagebuilder.CellLiveBlogView
import com.wapo.flagship.features.pagebuilder.CellMediaView
import com.wapo.flagship.features.pagebuilder.InlineAudioView
import com.wapo.flagship.features.pagebuilder.RelatedLinksView
import com.wapo.flagship.features.pagebuilder.getColumnsWidth
import com.wapo.flagship.features.pagebuilder.getMediaWidth
import com.wapo.flagship.features.pagebuilder.getSize
import com.wapo.flagship.features.pagebuilder.gravity
import com.wapo.flagship.features.pagebuilder.isMediaThumbnail
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.json.OlympicsMedals
import com.wapo.flagship.json.OlympicsSchedule
import com.wapo.olympics.OlympicsMedalsView
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.AsyncCell
import com.wapo.view.FlowableTextView
import com.wapo.view.FlowableView
import com.washingtonpost.android.androidlive.cache.AndroidLiveCache
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.sections.databinding.ViewHomepageStory2Binding
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.reflect.KMutableProperty1

class HomepageStoryView2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private var storyItem: HomepageStory? = null
    private var inlineAudioView: InlineAudioView? = null
    private val binding = ViewHomepageStory2Binding.inflate(LayoutInflater.from(getContext()), this)

    var liveImageView: LiveImageContainerView? = null
        private set
    var slideShowContainerView: SlideShowContainerView? = null
        private set
    var olympicsMedalsView: OlympicsMedalsView? = null
        private set
    var liveBlogView: CellLiveBlogView? = null
        private set
    var relatedLinksView: RelatedLinksView? = null
        private set
    var blurbView: BlurbView? = null
        private set
    var ctaView: CompoundLabelView? = null
        private set
    var topperLabelView: CompoundLabelView? = null
        private set
    var footNoteView: FootNoteView? = null
        private set
    var compoundLabelView: CompoundLabelView? = null
        private set
    var actionButtonsView: SignatureActionsView? = null
        private set
    val mediaView: CellMediaView
        get() = binding.media
    var webComponentView: WebComponentView? = null
        private set
    var countView: CountView? = null
        private set

    private var vertSpacing = 16
    private var horSpacing = 16
    var deckStyle = 0
    private var isNightMode = false
    private var liveBlogProxyUrl: String? = null
    private var isCardified = false
    private var top: Zone? = null
    private var left: Zone? = null
    private var right: Zone? = null
    private var bottom: Zone? = null
    private var sidebar: Zone? = null

    private val unknownView by lazy { Space(context) }

    private var leftZoneHeight = 0
    private var rightZoneHeight = 0

    //reusing int array instead of allocating it every time
    private val constraintDimensions = intArrayOf(0, 0)

    init {
        readAttrs(context, attrs, defStyleAttr)
    }

    private fun readAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) {
            return
        }

        context.withStyledAttributes(attrs, R.styleable.HomepageStoryView, defStyleAttr, 0) {
            vertSpacing =
                getDimensionPixelSize(R.styleable.HomepageStoryView_vertical_spacing, vertSpacing)
            horSpacing = getDimensionPixelSize(
                R.styleable.HomepageStoryView_horizontal_spacing,
                horSpacing
            )
            val minHorSpacing = Math.max(
                0,
                getDimensionPixelSize(R.styleable.HomepageStoryView_min_horizontal_spacing, 0)
            )
            horSpacing = max(minHorSpacing, horSpacing)
            deckStyle = getResourceId(
                R.styleable.HomepageStoryView_deck_font_style,
                R.style.homepagestory_deck_style
            )
        }
    }

    fun initActionClick(onClick:(ActionButtonEvent) -> Unit) {
        binding.signatureAction.onClick = WeakReference(onClick)
    }

    fun initBlurbsLinkClick(onClick: (String?) -> Unit) {
        blurbView?.onBlurbsLinkClick = onClick
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        leftZoneHeight = 0
        rightZoneHeight = 0

        if (top == null && left == null && right == null && bottom == null) {
            setMeasuredDimension(
                resolveSize(0, widthMeasureSpec),
                resolveSize(0, heightMeasureSpec)
            )
        } else {
            var totalHeight = paddingTop

            //measure sidebar views
            var sidebarHeight = 0
            var sidebarWidth = 0
            sidebar?.items?.forEach { subItemType ->
                val subItemTypeView = mapTypeToView(subItemType)
                if (isSubItemTypeCount(subItemType)) {
                    measureCountConstrained()
                    sidebarHeight += getChildHeightWithSpacing(subItemTypeView)
                    sidebarWidth += getChildWidth(subItemTypeView)
                    totalHeight += (sidebarHeight  - resources.getDimensionPixelSize(com.wapo.view.R.dimen.headline_text_size)) / 2
                }
            }

            //measure top views
            top?.items?.forEach { subItemType ->
                val subItemTypeView = mapTypeToView(subItemType)
                if (isSubItemTypeMedia(subItemType) || isSubItemTypeWebview(subItemType)) {
                    measureMedia(widthMeasureSpec, ArtWidth.FULL_WIDTH, sidebarWidth)
                } else {
                    measureView(subItemTypeView, widthMeasureSpec, heightMeasureSpec, sidebarWidth)
                }
                totalHeight += getChildHeightWithSpacing(subItemTypeView)
            }

            //measure constrained views, i.e. media or audio views that are in a left or right column, so that the flowable text knows it's width/height
            left?.items?.let {
                val mediaIndex = left?.items?.indexOf(SubItemType.MEDIA)
                //only measure media if it's not being dynamically replaced by another sub item
                if (mediaIndex != -1) {
                    when (storyItem?.media?.dynamicReplacement) {
                        SubItemType.OLYMPICS_MEDALS -> {
                            measureOlympicsConstrained(widthMeasureSpec, sidebarWidth)
                        }
                        SubItemType.SLIDESHOW -> {
                            measureSlideshowConstrained(widthMeasureSpec, sidebarWidth)
                        }
                        else -> {
                            measureMedia(widthMeasureSpec, left?.width, sidebarWidth)
                        }
                    }
                }
                if (left?.items?.contains(SubItemType.AUDIO) == true || left?.items?.contains(SubItemType.AUDIO_ARTICLE) == true) {
                    measureAudioConstrained(widthMeasureSpec, left?.width, false, sidebarWidth)
                }
                if (left?.items?.contains(SubItemType.OLYMPICS_MEDALS) == true) {
                    measureOlympicsConstrained(widthMeasureSpec, sidebarWidth)
                }
                if (left?.items?.contains(SubItemType.SLIDESHOW) == true) {
                    measureSlideshowConstrained(widthMeasureSpec, sidebarWidth)
                }
                if (left?.items?.contains(SubItemType.CTA) == true) {
                    measureCTAConstrained(widthMeasureSpec, left?.width, sidebarWidth)
                }
            }
            right?.items?.let {
                val mediaIndex = right?.items?.indexOf(SubItemType.MEDIA)
                if (mediaIndex != -1) {
                    when (storyItem?.media?.dynamicReplacement) {
                        SubItemType.OLYMPICS_MEDALS -> {
                            measureOlympicsConstrained(widthMeasureSpec, sidebarWidth)
                        }
                        SubItemType.SLIDESHOW -> {
                            measureSlideshowConstrained(widthMeasureSpec, sidebarWidth)
                        }
                        else -> {
                            measureMedia(widthMeasureSpec, right?.width, sidebarWidth)
                        }
                    }
                }
                if (right?.items?.contains(SubItemType.AUDIO) == true || right?.items?.contains(SubItemType.AUDIO_ARTICLE) == true) {
                    measureAudioConstrained(widthMeasureSpec, right?.width, true, sidebarWidth)
                }
                if (right?.items?.contains(SubItemType.OLYMPICS_MEDALS) == true) {
                    measureOlympicsConstrained(widthMeasureSpec, sidebarWidth)
                }
                if (right?.items?.contains(SubItemType.SLIDESHOW) == true) {
                    measureSlideshowConstrained(widthMeasureSpec, sidebarWidth)
                }
                if (right?.items?.contains(SubItemType.CTA) == true) {
                    measureCTAConstrained(widthMeasureSpec, right?.width, sidebarWidth)
                }
            }

            var leftHeight = 0 //measure left views
            left?.items?.forEach { subItemType ->
                val subItemTypeView = mapTypeToView(subItemType)
                if (!isConstrainedView(subItemTypeView)) {
                    measureSideView(
                        subItemTypeView,
                        widthMeasureSpec,
                        leftHeight,
                        totalHeight,
                        FlowableTextView.FLOAT_RIGHT,
                        right,
                        sidebarWidth
                    )
                }
                val viewHeight = getChildHeightWithSpacing(subItemTypeView)
                leftHeight += viewHeight
            }
            leftZoneHeight = leftHeight

            //measure right views
            var rightHeight = 0
            right?.items?.forEach { subItemType ->
                val subItemTypeView = mapTypeToView(subItemType)
                if (!isConstrainedView(subItemTypeView)) {
                    measureSideView(
                        subItemTypeView,
                        widthMeasureSpec,
                        rightHeight,
                        totalHeight,
                        FlowableTextView.FLOAT_LEFT,
                        left,
                        sidebarWidth
                    )
                }
                rightHeight += getChildHeightWithSpacing(subItemTypeView)
            }
            rightZoneHeight = rightHeight

            totalHeight += max(leftHeight, rightHeight)

            //measure bottom views
            bottom?.items?.forEach { subItemType ->
                val subItemTypeView = mapTypeToView(subItemType)
                if (isSubItemTypeMedia(subItemType)) {
                    measureMedia(widthMeasureSpec, ArtWidth.FULL_WIDTH, sidebarWidth)
                } else {
                    measureView(subItemTypeView, widthMeasureSpec, heightMeasureSpec, sidebarWidth)
                }
                totalHeight += getChildHeightWithSpacing(subItemTypeView)
            }

            totalHeight = max(totalHeight, sidebarHeight)
            totalHeight -= vertSpacing
            totalHeight += paddingBottom
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(totalHeight, heightMeasureSpec)
            )
        }
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var viewLeft = paddingLeft
        var viewTop = paddingTop
        val viewRight = r - l - paddingRight

        //layout sidebar
        sidebar?.items?.forEach { subItemType ->
            val subItemTypeView = mapTypeToView(subItemType)
            if (isSubItemTypeCount(subItemType)) {
                subItemTypeView.layout(
                    viewLeft,
                    viewTop,
                    viewLeft + subItemTypeView.measuredWidth,
                    viewTop + subItemTypeView.measuredHeight
                )
                viewLeft += resources.getDimensionPixelSize(R.dimen.count_view_width)
                viewTop += (subItemTypeView.measuredHeight - resources.getDimensionPixelSize(com.wapo.view.R.dimen.headline_text_size)) / 2
            }
        }

        //layout top views
        top?.items?.forEach { subItemType ->
            val subItemTypeView = mapTypeToView(subItemType)
            subItemTypeView.layout(
                viewLeft,
                viewTop,
                viewLeft + subItemTypeView.measuredWidth,
                viewTop + subItemTypeView.measuredHeight
            )
            val bottomSpacing: Float = vertSpacing.toFloat()
            viewTop += (subItemTypeView.measuredHeight + bottomSpacing).toInt()
        }

        //layout right views
        var rightViewTop = viewTop
        when (right?.valign) {
            VerticalAlignment.CENTER -> {
                // right zone items need to be vertical aligned with left zone
                // it only can be done if left zone is taller than right zone
                if (rightZoneHeight < leftZoneHeight) {
                    val shiftToCenterDelta = (leftZoneHeight - rightZoneHeight) / 2
                    rightViewTop += shiftToCenterDelta
                }
            }

            VerticalAlignment.BOTTOM -> {
                if (rightZoneHeight < leftZoneHeight) {
                    val shiftToBottomDelta = (leftZoneHeight - rightZoneHeight)
                    rightViewTop += shiftToBottomDelta
                }
            }

            null -> { /* no-op */
            }
        }
        right?.items?.forEach { subItemType ->
            val subItemTypeView = mapTypeToView(subItemType)
            if (isConstrainedView(subItemTypeView)) {
                subItemTypeView.layout(
                    viewRight - getChildWidth(subItemTypeView),
                    rightViewTop,
                    viewRight,
                    rightViewTop + getChildHeight(subItemTypeView)
                )
            } else {
                val dimenArray = getDimensionsOfConstrainedItems(left)
                val constrainedWidthWithSpacing = dimenArray[0] + horSpacing
                subItemTypeView.layout(
                    viewLeft + constrainedWidthWithSpacing,
                    rightViewTop,
                    viewLeft + constrainedWidthWithSpacing + subItemTypeView.measuredWidth,
                    rightViewTop + subItemTypeView.measuredHeight
                )
            }
            val bottomSpacing: Float = vertSpacing.toFloat()
            rightViewTop += (subItemTypeView.measuredHeight + bottomSpacing).toInt()
        }

        //layout left views
        var leftViewTop = viewTop
        when (left?.valign) {
            VerticalAlignment.CENTER -> {
                // left zone items need to be vertical aligned with right zone
                // it only can be done if right zone is taller than left zone
                if (leftZoneHeight < rightZoneHeight) {
                    val shiftToCenterDelta = (rightZoneHeight - leftZoneHeight) / 2
                    leftViewTop += shiftToCenterDelta
                }
            }
            VerticalAlignment.BOTTOM -> {
                if (leftZoneHeight < rightZoneHeight) {
                    val shiftToBottomDelta = (rightZoneHeight - leftZoneHeight)
                    leftViewTop += shiftToBottomDelta
                }
            }
            null -> {/* no-op */}
        }

        left?.items?.forEach { subItemType ->
            val subItemTypeView = mapTypeToView(subItemType)
            subItemTypeView.layout(
                viewLeft,
                leftViewTop,
                viewLeft + subItemTypeView.measuredWidth,
                leftViewTop + subItemTypeView.measuredHeight
            )
            val bottomSpacing: Float = vertSpacing.toFloat()
            leftViewTop += (subItemTypeView.measuredHeight + bottomSpacing).toInt()
        }

        viewTop = max(leftViewTop, rightViewTop)

        //layout bottom views
        bottom?.items?.forEach { subItemType ->
            val subItemTypeView = mapTypeToView(subItemType)
            subItemTypeView.layout(
                viewLeft,
                viewTop,
                viewLeft + subItemTypeView.measuredWidth,
                viewTop + subItemTypeView.measuredHeight
            )
            val bottomSpacing: Float = vertSpacing.toFloat()
            viewTop += (subItemTypeView.measuredHeight + bottomSpacing).toInt()
        }
    }

    fun setFeatureItem(
        story: HomepageStory,
        imageLoader: AnimatedImageLoader,
        itemId: Long
    ) {
        storyItem = story
        isCardified = story.cardSegmentType !== CardSegmentType.NO_CARD && story.cardSegmentType !== CardSegmentType.UNASSIGNED

        val gravity = story.textAlignment?.gravity ?: Gravity.START
        top = story.arrangements?.default?.top ?: story.arrangements?.top
        left = story.arrangements?.default?.left ?: story.arrangements?.left
        right = story.arrangements?.default?.right ?: story.arrangements?.right
        bottom = story.arrangements?.default?.bottom ?: story.arrangements?.bottom
        sidebar = story.arrangements?.default?.sidebar ?: story.arrangements?.sidebar
        //show action buttons if has byline and page is cardified
        val hasSignatureActions = (top?.items?.contains(SubItemType.BYLINE) ?: false ||
                left?.items?.contains(SubItemType.BYLINE) ?: false ||
                right?.items?.contains(SubItemType.BYLINE) ?: false ||
                bottom?.items?.contains(SubItemType.BYLINE) ?: false) && isCardified

        if (hasSignatureActions) {
            left = reArrangeSignature(left)
            right = reArrangeSignature(right)
        }

        //web embeds are not compatible on Android 5, fallback to live image
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (story.webComponent != null && story.media?.liveImage != null) {
                storyItem?.media?.mediaType = MediaType.LIVE_IMAGE
                storyItem?.media?.dynamicReplacement = null
                storyItem?.webComponent = null
            }
        }

        val mediaPosition = getMediaPositionFromArrangements(top, left, right, bottom)
        val mediaWidth = getMediaWidthFromArrangements(top, left, right, bottom)

        val availableWidth = getColumnsWidth(story.resolvedColumnSpan, getHorizontalSpacing(), context)

        //init views and bind data
        resetVisibility()
        story.apply {
            setHeadline(headline, deck, gravity)
            setBlurbs(blurbs, gravity)
            setMedia(media, availableWidth, mediaPosition, imageLoader, itemId)
            setByline(signature, gravity)
            setCta(cta)
            setFootnote(footNote)
            setRelatedLinks(relatedLinks, gravity)
            setLabel(label, isCardified)
            setSlideshow(slideShow, getAvailableViewWidth(mediaPosition, availableWidth), mediaPosition, mediaWidth)
            setOlympicsMedals(olympicsMedals, olympicsSchedule)
            setLiveTicker(liveBlog)
            setAudio(audio, audioArticle)
            setTopperLabel(topperLabel)
            setWebComponent(webComponent)
            setCount(count)
        }

        if (isCardified) {
            makeCardBleedAdjustments(binding.root as HomepageStoryView2)
        }
    }

    /**
     * By design, when signature action buttons are visible they need to be placed below left/right zones
     * This method will remove the signature and all items below the signature from left/right zone
     * and will put it into the bottom zone
     * @param zone - left or right zone to check for presence of signature
     * @return if the passed zone had signature, it will return a new modified side zone
     * without signature and all items below it
     */
    private fun reArrangeSignature(zone: Zone?): Zone? {
        zone ?: return null
        val zoneItems = zone.items
        if (zoneItems.isNullOrEmpty()) {
            return zone
        }
        val bylineStartsAt = zoneItems.indexOf(SubItemType.BYLINE)
        if (bylineStartsAt >= 0) {
            val subList = zoneItems.subList(bylineStartsAt, zoneItems.size)
            val newSideItems = zoneItems.take(bylineStartsAt).toMutableList()
            val newSideZone = Zone(items = newSideItems, width = zone.width, valign = zone.valign)
            val bottomItems = bottom?.items.orEmpty().toMutableList()
            bottomItems.addAll(0, subList)
            bottom = Zone(items = bottomItems, bottom?.width, bottom?.valign)
            return newSideZone
        }
        return zone
    }

    private fun resetVisibility() {
        binding.headlineGroup.visibility = GONE

        binding.ctaStub.visibility = GONE
        ctaView?.visibility = GONE

        binding.topperLabelStub.visibility = GONE
        topperLabelView?.visibility = GONE

        binding.footnoteStub.visibility = GONE
        footNoteView?.visibility = GONE

        binding.olympicsView.visibility = GONE
        olympicsMedalsView?.visibility = GONE

        binding.relatedLinksStub.visibility = GONE
        relatedLinksView?.visibility = GONE

        binding.liveBlog.visibility = GONE
        liveBlogView?.visibility = GONE

        binding.slideShowImageStub.visibility = GONE
        slideShowContainerView?.visibility = GONE

        binding.signatureAction.visibility = GONE

        binding.compoundLabelStub.visibility = GONE
        compoundLabelView?.visibility = GONE

        binding.media.visibility = GONE

        binding.liveImageStub.visibility = GONE
        liveImageView?.visibility = GONE

        binding.blurbStub.visibility = GONE
        blurbView?.visibility = GONE

        actionButtonsView?.visibility = GONE

        inlineAudioView?.visibility = GONE

        binding.webComponentStub.visibility = GONE
        webComponentView?.visibility = GONE

        binding.countStub.visibility = GONE
    }

    private fun setHeadline(headline: Headline?, deck: String?, gravity: Int = Gravity.START) {
        headline?.let {
            binding.headlineGroup.visibility =
                if (headline.text.isEmpty() && headline.prefixIcon == null) GONE else VISIBLE
            binding.headlineGroup.setHeadline(headline, headline.prefixIcon, null,true)
            if (deck != null) {
                val deckSpan = SpannableStringBuilder(deck)
                deckSpan.setSpan(
                    WpTextAppearanceSpan(
                        context,
                        deckStyle
                    ),
                    0, deckSpan.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.headlineGroup.setDeck(deckSpan)
            } else {
                binding.headlineGroup.setDeck(null)
            }

            binding.headlineGroup.setTextGravity(gravity)
        }
    }

    private fun setBlurbs(blurbs: BlurbList?, gravity: Int = Gravity.START) {
        binding.blurbStub.inflateIf(
            condition = { !blurbs?.items.isNullOrEmpty() },
            assignTo = HomepageStoryView2::blurbView,
            setup = {
                setTextGravity(gravity)
                setBlurbs(blurbs, true)
            }
        )
    }

    private fun setMedia(
        media: Media?,
        availableWidth: Int,
        mediaPosition: ArtPosition?,
        imageLoader: AnimatedImageLoader,
        itemId: Long
    ) {
        if (media != null && mediaPosition != null) {
            if (media.mediaType == MediaType.LIVE_IMAGE && media.liveImage != null) {
                binding.liveImageStub.inflateIf(
                    condition = { true },
                    assignTo = HomepageStoryView2::liveImageView,
                    setup = {
                        setLiveImage(
                            media.liveImage,
                            availableWidth,
                            mediaPosition,
                            binding.headlineGroup.getText().toString(),
                            isNightMode
                        )
                    }
                )
            } else if (media.dynamicReplacement == null && !TextUtils.isEmpty(media.url)) {
                binding.media.visibility = VISIBLE
                binding.media.setIsClickable(media.video != null)
                binding.media.update(media, imageLoader, itemId)
                binding.media.setCaption(media.caption)
            }
        }
    }

    private fun setLabel(label: CompoundLabel?, isCardified: Boolean = false) {
        compoundLabelView?.resetView()
        binding.compoundLabelStub.inflateIf(
            condition = { label != null },
            assignTo = HomepageStoryView2::compoundLabelView,
            setup = {
                setLabel(label, false)
                setCardify(isCardified)
            }
        )
    }

    private fun setByline(signature: Signature?, gravity: Int = Gravity.START) {
            signature?.let {
                if (storyItem?.actions != null) {
                    binding.signatureAction.visibility = VISIBLE
                    binding.signatureAction.setTextGravity(gravity)
                    binding.signatureAction.setSignature(signature, true)
                    if (isCardified && storyItem?.actions != null) {
                        binding.signatureAction.showActionButtons(true)
                        storyItem?.let {
                            binding.signatureAction.initActionButtons(it)
                        }
                    } else {
                        binding.signatureAction.showActionButtons(false)
                    }
                }
            }
    }

    private fun setSlideshow(
        slideShow: SlideShow?,
        availableWidth: Int,
        mediaPosition: ArtPosition?,
        mediaWidth: ArtWidth?
    ) {
        binding.slideShowImageStub.inflateIf(
            condition = { slideShow != null && mediaPosition != null && mediaWidth != null },
            assignTo = HomepageStoryView2::slideShowContainerView,
            setup = {
                setSlideShowImage(
                    slideShow!!,
                    availableWidth,
                    mediaPosition!!,
                    mediaWidth!!
                )
                initializeCaptions(slideShow)
            }
        )
    }

    private fun setLiveTicker(liveBlog: LiveBlog?) {
        binding.liveBlog.inflateIf(
            condition = { liveBlog != null },
            assignTo = HomepageStoryView2::liveBlogView,
            setup = {
                AndroidLiveCache.IS_NIGHT_MODE = isNightMode
                setItem(liveBlog, liveBlogProxyUrl)
            }
        )
    }

    private fun setRelatedLinks(relatedLinks: RelatedLinks?, gravity: Int = Gravity.START) {
        binding.relatedLinksStub.inflateIf(
            condition = { relatedLinks != null },
            assignTo = HomepageStoryView2::relatedLinksView,
            setup = {
                textGravity = gravity
                isNightMode = this@HomepageStoryView2.isNightMode
                setRelatedLinks(relatedLinks, true)
            }
        )
    }

    private fun setOlympicsMedals(
        olympicsMedals: OlympicsMedals?,
        olympicsSchedule: OlympicsSchedule?
    ) {
        binding.olympicsView.inflateIf(
            condition = { olympicsMedals != null || olympicsSchedule != null },
            assignTo = HomepageStoryView2::olympicsMedalsView,
            setup = {
                if (olympicsMedals != null) {
                    setMedals(olympicsMedals)
                }
                if (olympicsSchedule != null) {
                    setSchedule(olympicsSchedule)
                }
            }
        )
    }

    private fun setWebComponent(webComponent: WebComponent?) {
        binding.webComponentStub.inflateIf(
            condition = { webComponent?.url?.isNotEmpty() == true },
            assignTo = HomepageStoryView2::webComponentView,
            setup = {
                visibility = VISIBLE
                init()
                webComponent?.url?.let { loadComponent(it) }
            }
        )
    }

    private fun setCta(cta: CompoundLabel?) {
        binding.ctaStub.inflateIf(
            condition = { cta != null },
            assignTo = HomepageStoryView2::ctaView,
            setup = {
                setLabel(cta, false)
            }
        )
    }

    private fun setTopperLabel(topperLabel: CompoundLabel?) {
        binding.topperLabelStub.inflateIf(
            condition = { topperLabel != null },
            assignTo = HomepageStoryView2::topperLabelView,
            setup = {
                setLabel(topperLabel, false)
            }
        )
    }

    private fun setFootnote(footNote: FootNote?) {
        binding.footnoteStub.inflateIf(
            condition = { footNote != null },
            assignTo = HomepageStoryView2::footNoteView,
            setup = {
                setFootNote(footNote)
            }
        )
    }

    private fun setAudio(audio: Audio?, audioArticle: AudioArticle?) {
        if ((audio?.mediaId != null) || audioArticle != null) {
            val sectionActivity = context.findActivityOfType<SectionActivity>()
            if (inlineAudioView == null) {
                inlineAudioView =
                    if (sectionActivity?.inlineAudioView != null) {
                        sectionActivity.inlineAudioView
                    } else {
                        InlineAudioView(context)
                    }
                addView(inlineAudioView)
            }

            inlineAudioView?.visibility = VISIBLE
            inlineAudioView?.setAudio(
                podcast = audio, audioArticle = audioArticle,
                sectionActivity?.appSection
            )
        }
    }

    private fun setCount(count: Count?) {
        binding.countStub.inflateIf(
            condition = { count != null },
            assignTo = HomepageStoryView2::countView,
            setup = {
                this.apply {
                    init()
                    setCount(count)
                }
            }
        )
    }

    private fun getMediaWidthFromArrangements(
        top: Zone?,
        left: Zone?,
        right: Zone?,
        bottom: Zone?
    ): ArtWidth? {
        top?.width?.let {
            return it
        }
        left?.width?.let {
            return it
        }
        right?.width?.let {
            return it
        }
        bottom?.width?.let {
            return it
        }
        return null
    }

    private fun getMediaPositionFromArrangements(
        top: Zone?,
        left: Zone?,
        right: Zone?,
        bottom: Zone?
    ): ArtPosition? {
        if (top?.items?.contains(SubItemType.MEDIA) == true || top?.items?.contains(SubItemType.SLIDESHOW) == true) {
            return ArtPosition.HIGH
        } else if (left?.items?.contains(SubItemType.MEDIA) == true || left?.items?.contains(
                SubItemType.SLIDESHOW
            ) == true
        ) {
            return ArtPosition.LEFT
        } else if (right?.items?.contains(SubItemType.MEDIA) == true || right?.items?.contains(
                SubItemType.SLIDESHOW
            ) == true
        ) {
            return ArtPosition.RIGHT
        } else if (bottom?.items?.contains(SubItemType.MEDIA) == true || bottom?.items?.contains(
                SubItemType.SLIDESHOW
            ) == true
        ) {
            return ArtPosition.LOW
        }
        return null
    }

    fun setNightMode(isNightMode: Boolean) {
        this.isNightMode = isNightMode
    }

    fun setLiveBlogProxyUrl(liveBlogProxyUrl: String?) {
        this.liveBlogProxyUrl = liveBlogProxyUrl
    }

    fun setIsCardified(isCardified: Boolean) {
        this.isCardified = isCardified
    }

    private fun mapTypeToView(subItemType: SubItemType?): View {
        when (subItemType) {
            SubItemType.HEADLINE -> return binding.headlineGroup
            SubItemType.MEDIA -> {
                storyItem?.media?.dynamicReplacement?.let {
                    return mapTypeToView(it)
                }
                return if (storyItem?.media?.mediaType == MediaType.LIVE_IMAGE) liveImageView
                    ?: unknownView else binding.media
            }
            SubItemType.SLIDESHOW -> return slideShowContainerView ?: unknownView
            SubItemType.BYLINE -> return binding.signatureAction
            SubItemType.BLURB -> return blurbView ?: unknownView
            SubItemType.LABEL -> return compoundLabelView ?: unknownView
            SubItemType.LIVE_TICKER -> return liveBlogView ?: unknownView
            SubItemType.RELATED_LINKS -> return relatedLinksView ?: unknownView
            SubItemType.AUDIO, SubItemType.AUDIO_ARTICLE -> return inlineAudioView ?: unknownView
            SubItemType.OLYMPICS_MEDALS -> return olympicsMedalsView ?: unknownView
            SubItemType.CTA -> return ctaView ?: unknownView
            SubItemType.FOOT_NOTE -> return footNoteView ?: unknownView
            SubItemType.TOPPER_LABEL -> return topperLabelView ?: unknownView
            SubItemType.WEB_EMBED -> return webComponentView ?: unknownView
            SubItemType.COUNT -> return countView ?: unknownView
            else -> return unknownView // return empty view for the types we don't understand
        }
    }

    private fun getChildWidth(view: View?): Int {
        if (view == null) return 0
        return if (view.visibility == GONE) 0 else view.measuredWidth
    }

    private fun getChildHeight(view: View?): Int {
        if (view == null) return 0
        return if (view.visibility == GONE) 0 else view.measuredHeight
    }

    private fun getChildHeightWithSpacing(view: View?): Int {
        if (view == null) return 0
        return if (view.visibility == GONE) 0 else view.measuredHeight + vertSpacing
    }

    private fun measureView(view: View, widthMeasureSpec: Int, heightMeasureSpec: Int, sidebarWidth: Int) {
        val width = max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        if (view is FlowableView) {
            (view as FlowableView).setFlowObstruction(0, 0, FlowableTextView.FLOAT_NONE)
        }
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    private fun measureSideView(
        view: View,
        widthMeasureSpec: Int,
        columnHeight: Int,
        topHeight: Int,
        floatingType: Int,
        constrainedZone: Zone?,
        sidebarWidth: Int
    ) {
        if (!isFlowSupported(view)) {
            measureNonFlowableSideView(widthMeasureSpec, view, constrainedZone)
        } else {
            measureFlowableView(
                floatingType,
                columnHeight,
                topHeight,
                view,
                widthMeasureSpec,
                constrainedZone,
                sidebarWidth
            )
        }
    }

    //measure view that has flowable text in a left or right column
    private fun measureFlowableView(
        floatingType: Int,
        columnHeight: Int,
        topHeight: Int,
        view: View,
        widthMeasureSpec: Int,
        constrainedZone: Zone?,
        sidebarWidth: Int
    ) {
        var width = max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        val dimenArray = getDimensionsOfConstrainedItems(constrainedZone)
        val constrainedWidth = dimenArray[0]
        val constrainedHeight = dimenArray[1]
        if (floatingType == FlowableTextView.FLOAT_RIGHT && (storyItem?.wrapText == false || columnHeight + topHeight <= constrainedHeight + topHeight)) {
            when (view) {
                is FlowableView -> {
                    //otherwise FlowableTextView
                    (view as FlowableView).setFlowObstruction(
                        constrainedWidth + horSpacing,
                        if (storyItem?.wrapText == true) constrainedHeight - columnHeight + topHeight + vertSpacing else Int.MAX_VALUE,
                        floatingType
                    )
                }

                is CellLiveBlogView -> {
                    view.setFlowObstruction(
                        constrainedWidth + horSpacing,
                        if (storyItem?.wrapText == true) constrainedHeight - columnHeight + topHeight + vertSpacing else Int.MAX_VALUE,
                        floatingType,
                        constrainedHeight + topHeight,
                        columnHeight + topHeight,
                        storyItem?.wrapText == true
                    )
                }
            }
        } else {
            if (floatingType == FlowableTextView.FLOAT_LEFT) {
                width = max(
                    0,
                    MeasureSpec.getSize(widthMeasureSpec) - paddingRight - paddingLeft - constrainedWidth - horSpacing
                )
            }
            when (view) {
                is FlowableView -> {
                    //otherwise FlowableTextView
                    (view as FlowableView).setFlowObstruction(0, 0, FlowableTextView.FLOAT_NONE)
                }

                is CellLiveBlogView -> {
                    view.setFlowObstruction(
                        0,
                        0,
                        FlowableTextView.FLOAT_NONE,
                        0,
                        0,
                        storyItem?.wrapText == true
                    )
                }
            }
        }
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    // if a view needs to be placed left/right but does not support Flowable
    private fun measureNonFlowableSideView(widthMeasureSpec: Int, view: View, constrainedZone: Zone?) {
        var width = max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight)
        val dimenArray = getDimensionsOfConstrainedItems(constrainedZone)
        val constrainedWidth = dimenArray[0]
        width -= constrainedWidth
        width -= horSpacing
        width = width.coerceAtLeast(0)
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    private fun isFlowSupported(view: View): Boolean {
        if (view is FlowableView) return true

        // CellLiveBlogView does support flowable text
        // but needs more data than FlowableView interface provides
        if (view is CellLiveBlogView) return true

        return false
    }

    //measure media in any zone
    private fun measureMedia(widthMeasureSpec: Int, artWidth: ArtWidth?, sidebarWidth: Int) {
        val media = storyItem?.media
        val cellWidth = if (storyItem?.bleed == Bleed.FULL) {
            max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        } else {
            max(0, MeasureSpec.getSize(widthMeasureSpec) - resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding).toInt() - resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding).toInt() - sidebarWidth)
        }
        var maxImgSize: Int = if (storyItem?.isMediaThumbnail(artWidth) == true) {
            //set image to have a fixed width thumbnail
            val fixedWidth = if(artWidth == ArtWidth.MINI) context.resources.getDimensionPixelSize(R.dimen.homepage_story_mini_size) else context.resources.getDimensionPixelSize(R.dimen.homepage_story_thumbnail_size)
            val imageWSpec = MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY)
            val imageHSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            binding.media.measure(imageWSpec, imageHSpec)
            return
        } else if (artWidth == ArtWidth.FULL_WIDTH){
            cellWidth
        } else {
            artWidth?.let { storyItem?.getMediaWidth(context, artWidth, getHorizontalSpacing()) } ?: cellWidth
        }
        val imageWSpec = MeasureSpec.makeMeasureSpec(maxImgSize, MeasureSpec.EXACTLY)
        val imageHSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        if(storyItem?.media?.dynamicReplacement == SubItemType.WEB_EMBED) {
            val webHeightSpec = if(webComponentView?.isLoaded != true) {
                storyItem?.webComponent?.sizes?.getSize(context)?.let {
                    val height = maxImgSize * (it.height ?:0)  / (it.width ?: 0)
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                } ?: MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            } else {
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            }
            webComponentView?.measure(imageWSpec, webHeightSpec)
        }
        else if (media?.mediaType == MediaType.LIVE_IMAGE) {
            liveImageView?.measure(imageWSpec, imageHSpec)
        }
        else {
            binding.media.measure(imageWSpec, imageHSpec)
        }
    }

    //measure audio view in a left or right zone
    private fun measureAudioConstrained(
        widthMeasureSpec: Int,
        artWidth: ArtWidth?,
        isRightZone: Boolean,
        sidebarWidth: Int
    ) {
        var width = max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        if (artWidth == null) {
            //set audio to be half width if on right zone
            if (isRightZone) {
                width /= 2
            }
        } else if (artWidth != ArtWidth.FULL_WIDTH) {
            width = if (storyItem?.isMediaThumbnail(artWidth) == true) {
                //set image to have a fixed width thumbnail
                val fixedWidth =
                    context.resources.getDimensionPixelSize(R.dimen.homepage_story_thumbnail_size)
                val imageWSpec = MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY)
                val imageHSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                inlineAudioView?.measure(imageWSpec, imageHSpec)
                return
            } else {
                storyItem?.getMediaWidth(context, artWidth, getHorizontalSpacing()) ?: width
            }
        }
        inlineAudioView?.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    private fun measureCountConstrained() {
        val width = resources.getDimensionPixelSize(R.dimen.count_view_width)
        countView?.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    private fun measureOlympicsConstrained(widthMeasureSpec: Int, sidebarWidth: Int) {
        val width =
            max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        olympicsMedalsView?.measure(
            MeasureSpec.makeMeasureSpec(width / 2, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    private fun measureSlideshowConstrained(widthMeasureSpec: Int, sidebarWidth: Int) {
        val width =
            max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        slideShowContainerView?.measure(
            MeasureSpec.makeMeasureSpec(width / 2, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    private fun measureCTAConstrained(widthMeasureSpec: Int, artWidth: ArtWidth?, sidebarWidth: Int) {
        var width = max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - sidebarWidth)
        if (artWidth != null && artWidth != ArtWidth.FULL_WIDTH) {
            width = if (storyItem?.isMediaThumbnail(artWidth) == true) {
                //set image to have a fixed width thumbnail
                val fixedWidth =
                    context.resources.getDimensionPixelSize(R.dimen.homepage_story_thumbnail_size)
                val imageWSpec = MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY)
                val imageHSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                ctaView?.measure(imageWSpec, imageHSpec)
                return
            } else {
                storyItem?.getMediaWidth(context, artWidth, getHorizontalSpacing()) ?: width
            }
        }
        ctaView?.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
    }

    //a view that can be wrapped by a flowable text view
    private fun isConstrainedView(view: View): Boolean {
        return view == binding.media || view == liveImageView || view == inlineAudioView || view == olympicsMedalsView || view == slideShowContainerView || view == webComponentView || view == ctaView
    }

    //returns max width and combined height of the constrained views in a left or right zone
    private fun getDimensionsOfConstrainedItems(zone: Zone?): IntArray {
        var width = 0
        var height = 0
        zone?.items?.forEach { subItemType ->
            val subItemTypeView = mapTypeToView(subItemType)
            if (isConstrainedView(subItemTypeView)) {
                width = max(width, getChildWidth(subItemTypeView))
                height += getChildHeightWithSpacing(subItemTypeView)
            }
        }
        constraintDimensions[0] = width
        constraintDimensions[1] = height
        return constraintDimensions
    }

    private fun isSubItemTypeMedia(subItemType: SubItemType): Boolean {
        return subItemType == SubItemType.MEDIA && storyItem?.media?.dynamicReplacement == null
    }

    private fun isSubItemTypeWebview(subItemType: SubItemType): Boolean {
        return subItemType == SubItemType.MEDIA && storyItem?.media?.dynamicReplacement == SubItemType.WEB_EMBED
    }

    private fun isSubItemTypeCount(subItemType: SubItemType): Boolean {
        return subItemType == SubItemType.COUNT && !storyItem?.count?.count.isNullOrEmpty()
    }

    private fun shouldBeCenterAligned(subItemType: SubItemType, artWidth: ArtWidth?): Boolean {
        return subItemType == SubItemType.MEDIA && artWidth == ArtWidth.FULL_WIDTH
    }

    private fun makeCardBleedAdjustments(storyView: HomepageStoryView2) {
        if (storyItem == null || storyItem?.bleed == Bleed.NONE || storyItem?.bleed == Bleed.CONTAINER) {
            return
        }
        adjustBleedPadding(storyView)
        if (storyItem?.bleed == Bleed.FULL) {
            val fullBleedOffset =
                resources.getDimensionPixelSize(R.dimen.card_horizontal_margin) +
                        resources.getDimensionPixelSize(R.dimen.card_horizontal_margin)
            val cardView = storyView.parent as MaterialCardView
            val cardLayoutParams = cardView.layoutParams as FrameLayout.LayoutParams
            cardLayoutParams.marginStart = fullBleedOffset
            cardLayoutParams.marginEnd = fullBleedOffset
            cardView.layoutParams = cardLayoutParams
            val marginAdjustment = fullBleedOffset * -1
            if (storyItem?.bleedItemType == BleedItemType.MEDIA) {
                val mediaFrame = storyView.mediaView.mediaFrame
                disableClipRecursively(mediaFrame)
                (mediaFrame?.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
                    marginAdjustment
                (mediaFrame?.layoutParams as? LinearLayout.LayoutParams)?.marginEnd =
                    marginAdjustment
            } else if (storyItem?.bleedItemType == BleedItemType.SLIDESHOW) {
                val pager = storyView.slideShowContainerView?.getImagePager()
                disableClipRecursively(pager)
                (pager?.layoutParams as? ConstraintLayout.LayoutParams)?.marginStart =
                    marginAdjustment
                (pager?.layoutParams as? ConstraintLayout.LayoutParams)?.marginEnd =
                    marginAdjustment
            } else if (storyItem?.bleedItemType == BleedItemType.OLYMPICS) {
                val dataList = storyView.olympicsMedalsView?.getDataList()
                disableClipRecursively(dataList)
                (dataList?.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
                    marginAdjustment
                (dataList?.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = marginAdjustment
            }
        }
    }

    // the HomepageStoryView for container bleed already has 0 padding,
    // so we need to add the appropriate horizontal padding to all views that arent the view to which the bleed applies (media, slideshow)
    private fun adjustBleedPadding(storyView: HomepageStoryView2) {
        val horizontalPadding =
            resources.getDimensionPixelSize(R.dimen.grid_cell_card_homepagestory_horizontal_padding)
        val horizontalCaptionPadding =
            resources.getDimensionPixelSize(R.dimen.grid_cell_card_homepagestory_horizontal_caption_padding)
        for (i in 0 until storyView.childCount) {
            val itemView = storyView.getChildAt(i)
            if (itemView is CellMediaView) {
                val captionView = itemView.captionView
                captionView.setPadding(
                    horizontalPadding,
                    captionView.paddingTop,
                    horizontalPadding,
                    captionView.paddingBottom
                )
                val overlayView = itemView.overylayTextView
                (overlayView.layoutParams as FrameLayout.LayoutParams).marginStart =
                    horizontalPadding
                (overlayView.layoutParams as FrameLayout.LayoutParams).marginEnd = horizontalPadding
            } else if (itemView is SlideShowContainerView) {
                val captionView = itemView.getCaptionView()
                captionView?.setPadding(
                    horizontalPadding,
                    captionView.paddingTop,
                    horizontalPadding,
                    captionView.paddingBottom
                )
                val overlayView = itemView.getOverlayTextView()
                if (overlayView != null) {
                    (overlayView.layoutParams as ConstraintLayout.LayoutParams).marginStart =
                        horizontalPadding
                    (overlayView.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        horizontalPadding
                }
            } else if (itemView is OlympicsMedalsView) {
                val medalsView = itemView
                val title = medalsView.getTitle()
                title.setPadding(
                    horizontalPadding,
                    title.paddingTop,
                    horizontalPadding,
                    title.paddingBottom
                )
                val link = medalsView.getLink()
                link.setPadding(
                    horizontalPadding,
                    link.paddingTop,
                    horizontalPadding,
                    link.paddingBottom
                )
            } else {
                itemView.setPadding(
                    horizontalPadding,
                    itemView.paddingTop,
                    horizontalPadding,
                    itemView.paddingBottom
                )
            }
        }
    }

    /**
     * Recursively disable clipping from view and its parents up to the AsyncCell
     * @param view the view where the clipping disabling should start
     */
    private fun disableClipRecursively(view: View?) {
        if (view == null) {
            return
        }
        if (view is ViewGroup) {
            view.clipChildren = false
            view.clipToPadding = false
            view.setClipToOutline(false)
        }

        // AsyncCell is the highest parent we want to disable clipping for
        if (view is AsyncCell) {
            return
        }
        if (view.parent is View) {
            disableClipRecursively(view.parent as View)
        }
    }

    fun applyStoryTextColor() {
        val color: Int = if (isNightMode) ContextCompat.getColor(
            context,
            R.color.cell_homepagestory_headline_night
        ) else ContextCompat.getColor(
            context,
            R.color.cell_homepagestory_headline
        )
        updateColor(color)
    }

    fun applyNewsprintTextColor() {
        updateColor(Color.WHITE)
    }

    private fun updateColor(color: Int) {
        compoundLabelView?.updateKickerLabelColors(color)
        binding.headlineGroup.headlineView.updateHeadlineColor(color)
        blurbView?.updateBlurbColor(color)
        binding.signatureAction.updateBylineColor(color)
        binding.media.updateCaptionColor(color)
    }

    /**
     * Returns view spacing by adding paddingLeft and paddingRight values.
     * Ref: Cell.HomepageStory.Grid.NoBleed and Cell.HomepageStory.Grid.Bleed styles
     * and makeCardBleedAdjustments() method.
     */
    private fun getHorizontalSpacing(): Int {
        return paddingLeft + paddingRight
    }

    /**
     * Returns available view width based on an ArtPosition for media related views.
     */
    private fun getAvailableViewWidth(artPosition: ArtPosition?, availableWidth: Int): Int {
        return if (artPosition == ArtPosition.LEFT || artPosition == ArtPosition.RIGHT) (availableWidth - horSpacing) / 2 else availableWidth
    }

    /**
     * Runs only if the condition is met.
     * Inflate ViewStub and assign the inflated view back to the given field.
     * Run the action function to setup the view.
     */
    private inline fun <reified T : View> ViewStub.inflateIf(
        condition: () -> Boolean,
        assignTo: KMutableProperty1<HomepageStoryView2, T?>,
        setup: T.() -> Unit
    ) {
        if (condition()) {
            var view = assignTo.get(this@HomepageStoryView2)
            if (view == null) {
                // view needs to be inflated first
                view = inflate() as T
                assignTo.set(this@HomepageStoryView2, view)
            }

            //view is inflated and ready to be updated
            view.visibility = View.VISIBLE
            view.setup()
        }
    }
}