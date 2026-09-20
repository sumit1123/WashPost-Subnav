package com.wapo.flagship.features.pagebuilder

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.wapo.flagship.features.grid.model.Arrangement
import com.wapo.flagship.features.grid.model.RelatedLinkItem
import com.wapo.flagship.features.grid.model.RelatedLinks
import com.wapo.flagship.features.grid.model.RelatedLinksInfo
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.FirstTextLineDrawable
import com.wapo.view.FlowableTextView
import com.wapo.view.FlowableView
import com.wapo.view.LinkTouchMovementMethod
import com.wapo.view.TouchableSpan
import com.washingtonpost.android.sections.R

class RelatedLinksView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr), FlowableView {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private var fontStyleResId: Int
    private var gridFontStyleResId: Int
    private var bulletIcon: Drawable? = null
    private val compoundLabelView: CompoundLabelView
    private val relatedViews = mutableListOf<View>()
    var verticalSpacing = 25
    var callBack: RelatedLinksCallback? = null
    var isNightMode = false
    var isGrid = false
    var widthAdjustment = 0
    var heightAdjustment = 0
    private val relatedLinkContentDescription: String

    var textGravity: Int = Gravity.NO_GRAVITY
        set(value) {
            gravity = value
            field = value
        }

    init {
        orientation = VERTICAL
        val a = context.obtainStyledAttributes(attrs, R.styleable.RelatedLinksView, defStyleAttr, 0)
        try {
            gridFontStyleResId = a.getResourceId(R.styleable.RelatedLinksView_grid_font_style, R.style.grid_homepagestory_related_links_style)
            fontStyleResId = a.getResourceId(R.styleable.RelatedLinksView_font_style, R.style.homepagestory_related_links_style)
            verticalSpacing = a.getDimensionPixelSize(R.styleable.RelatedLinksView_vertical_space, context.resources.getDimensionPixelSize(R.dimen.cell_homepagesotry_vert_spacing))
        } finally {
            a.recycle()
        }
        val view = LayoutInflater.from(context).inflate(R.layout.fusion_cell_label, this, false) as CompoundLabelView
        compoundLabelView = view.apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, verticalSpacing)
            }
        }
        addView(compoundLabelView)
        relatedLinkContentDescription = context.getString(R.string.related_links_content_description)
    }

    fun setRelatedLinks(relatedLinks: RelatedLinks?, isGrid: Boolean) {
        this.isGrid = isGrid
        bulletIcon = if (isGrid) ContextCompat.getDrawable(context, R.drawable.diamond_bullet) else ContextCompat.getDrawable(context, R.drawable.circle_solid)
        removeAllButLabel()
        relatedViews.clear()
        if (relatedLinks?.compoundLabel == null) {
            compoundLabelView.visibility = GONE
        } else {
            compoundLabelView.visibility = View.VISIBLE
            compoundLabelView.setLabel(relatedLinks.compoundLabel, true)
        }
        when (relatedLinks?.info?.arrangement ?: Arrangement.NORMAL) {
            Arrangement.NORMAL -> fillVertical(relatedLinks)
            Arrangement.SIDE_BY_SIDE -> fillSideBySide(relatedLinks)
            Arrangement.SIDE_BY_SIDE_PIPES -> fillWithPipes(relatedLinks)
        }
    }

    override fun setFlowObstruction(widthAdjustment: Int, heightAdjustment: Int, floatingType: Int) {
        relatedViews.forEach {
            if (it is FlowableTextView) {
                it.setFlowObstruction(widthAdjustment, heightAdjustment, floatingType)
            }
        }
    }

    private fun removeAllButLabel() {
        relatedViews.forEach { removeView(it) }
    }

    private fun fillVertical(relatedLinks: RelatedLinks?) {
        val padding = verticalSpacing / 2
        relatedLinks?.items?.forEachIndexed { index, item ->
            if (!item.text.isNullOrBlank()) {
                val textSpan = SpannableString(item.text)
                        .makeClickable(item)
                        .applyWpStyle(relatedLinks.info)

                addView(FlowableTextView(context).apply {
                    val icon = bulletIcon?.run { FirstTextLineDrawable(bulletIcon!!, this@apply) }
                    setText(textSpan, icon, null)
                    setFlowObstruction(0, 0, FlowableTextView.FLOAT_NONE)
                    setTextGravity(textGravity)
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    if (index == 0) setPadding(0, 0, 0, padding) else setPadding(0, padding, 0, padding)
                    setMovementMethod(LinkTouchMovementMethod.getInstance())
                    relatedViews.add(this)
                    contentDescription = "$relatedLinkContentDescription $textSpan"
                })
            }
        }
    }

    private fun fillSideBySide(relatedLinks: RelatedLinks?) {
        val horizontalLayout = FlexboxLayout(context)
        horizontalLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        horizontalLayout.flexDirection = FlexDirection.ROW
        horizontalLayout.flexWrap = FlexWrap.WRAP
        horizontalLayout.alignItems = AlignItems.BASELINE
        val sb = SpannableStringBuilder()
        relatedLinks?.items?.forEachIndexed { index, relatedLinkItem ->
            sb.append(
                    SpannableString(relatedLinkItem.text)
                            .makeClickable(relatedLinkItem)
                            .applyWpStyle(relatedLinks.info)
            )
            sb.append("  ")
            horizontalLayout.addView(FlowableTextView(context).apply {
                val icon = if (index == 0) null else bulletIcon?.run {
                    FirstTextLineDrawable(bulletIcon!!, this@apply)
                }
                setText(sb, icon, null)
                setFlowObstruction(0, 0, FlowableTextView.FLOAT_NONE)
                setTextGravity(textGravity)
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                setMovementMethod(LinkTouchMovementMethod.getInstance())
                contentDescription = "$relatedLinkContentDescription $sb"
            })
            sb.clear()
        }
        addView(horizontalLayout)
        relatedViews.add(horizontalLayout)
    }

    private fun fillWithPipes(relatedLinks: RelatedLinks?) {
        val sb = SpannableStringBuilder()
        relatedLinks?.items?.forEachIndexed { index, item ->
            sb.append(
                    SpannableString(item.text)
                            .makeClickable(item)
                            .applyWpStyle(relatedLinks.info)
            )
            if (index != relatedLinks.items.lastIndex) {
                sb.append(
                        SpannableString(" | ")
                                .applyWpStyle(relatedLinks.info)
                )
            }
        }
        addView(FlowableTextView(context).apply {
            setText(sb, null, null)
            setFlowObstruction(0, 0, FlowableTextView.FLOAT_NONE)
            setTextGravity(textGravity)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setMovementMethod(LinkTouchMovementMethod.getInstance())
            relatedViews.add(this)
            contentDescription = "$relatedLinkContentDescription $sb"
        })
    }

    private fun SpannableString.makeClickable(relatedLinkItem: RelatedLinkItem): SpannableString {
        this.setSpan(
                object : TouchableSpan(Color.TRANSPARENT, Color.TRANSPARENT,
                        ContextCompat.getColor(context, if (!isNightMode) com.wapo.view.R.color.related_links_clickable_color_light else com.wapo.view.R.color.related_links_clickable_color_dark)) {
                    override fun onClick(widget: View) {
                        if (!relatedLinkItem.link.isNullOrBlank()) {
                            callBack?.onRelatedLinkClicked(relatedLinkItem)
                        }
                    }
                },
                0, length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }

    private fun SpannableString.applyWpStyle(info: RelatedLinksInfo?): SpannableString {
        setSpan(
                WpTextAppearanceSpan(
                        context,
                        if (isGrid) gridFontStyleResId else fontStyleResId,
                        getRelatedLinkSize(context, info, isGrid)
                ),
                0, length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }

    interface RelatedLinksCallback {
        fun onRelatedLinkClicked(relatedLinkItem: RelatedLinkItem)
    }
}
