package com.wapo.flagship.features.articles2.viewholders

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.wapo.Utils.convertDpToPixel
import com.wapo.Utils.getDate
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.ElementGroup
import com.wapo.flagship.features.articles2.models.deserialized.ElementGroupLinkBox
import com.wapo.flagship.features.articles2.models.deserialized.ListItem
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.FlowableLayout
import com.wapo.view.selection.SelectableTextView
import com.washingtonpost.android.articles.R

/**
 * A helper class that encapsulates [ElementGroupViewHolder]'s view setup
 */
class ElementLinkBoxBinder(
    @NonNull val container: ViewGroup,
    val helper: ElementGroupStyleHelper,
    private val onGroupToggleClicked: (Int, Item) -> Unit,
    private val expandedItemLookUp: (Item) -> Boolean,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) {
    companion object {
        const val DATE_FORMAT_STRING = "MMMM dd, yyyy"
        const val MIN_ITEMS_COUNT = 3
        const val ITEMS_CONTAINER_HEIGHT_FACTOR = 0.25f
        const val GRADIENT_HEIGHT_FACTOR = 0.4f
        const val USE_ITEMS_COUNT_APPROACH = true
    }

    private lateinit var sanitizedHtmlTextFormatter: SanitizedHtmlTextFormatter
    private val context = container.context
    private val kicker = container.findViewById(R.id.element_group_kicker) as SelectableTextView
    private val title = container.findViewById(R.id.element_group_title) as SelectableTextView
    private val dateTime = container.findViewById(R.id.element_group_dateline) as SelectableTextView
    private val subHeadline = container.findViewById(R.id.element_group_subheadline) as SelectableTextView
    private val topRule = container.findViewById(R.id.top_rule) as View
    private val itemsContainer = container.findViewById(R.id.element_group_items_container) as LinearLayout
    private val show = container.findViewById(R.id.element_group_show) as AppCompatButton
    private val gradientContainer =
        container.findViewById(
            R.id.element_group_link_box_gradient_container,
        ) as ViewGroup
    private var expanded = false

    fun bind(
        elementGroup: ElementGroupLinkBox,
        position: Int,
    ) {
        sanitizedHtmlTextFormatter = SanitizedHtmlTextFormatter(context, articlesInteractionHelper)
        sanitizedHtmlTextFormatter.textStyleProducer = { helper.textLinkBoxItemStyle }
        sanitizedHtmlTextFormatter.subheadTextStyleProducer = { helper.textLinkBoxItemSubheadStyle }
        expanded = expandedItemLookUp(elementGroup)
        val items: List<Item> = (elementGroup.contentElements ?: emptyList()) as List<Item>
        prepareGradientContainer()
        prepareHeaderItems(position, elementGroup)
        prepareBodyItems(position, items)
        prepareFooterItems(position, items, elementGroup)
        prepareTopRule()
        if (USE_ITEMS_COUNT_APPROACH) {
            showFirstKItemsInTheBody(if (expanded) items.size else MIN_ITEMS_COUNT)
            itemsContainer.viewTreeObserver.apply {
                if (isAlive) {
                    addOnGlobalLayoutListener(
                        object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                if (itemsContainer.viewTreeObserver.isAlive) {
                                    itemsContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                }
                                updateGradient(items.size)
                            }
                        },
                    )
                }
            }
        } else {
            adjustItemsContainerHeightBasedOnShowStatus()
        }
    }

    private fun prepareGradientContainer() {
        gradientContainer.apply {
            background =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ContextCompat.getDrawable(context, R.drawable.element_group_link_box_gradient)
                } else {
                    // Note: ?attr in drawable xmls will work from +21 versions. clean this else block when app's minSdkVersion is 21 or greater.
                    val colors =
                        intArrayOf(
                            ContextCompat.getColor(
                                context,
                                R.color.articles_link_box_gradient_start_color,
                            ),
                            ContextCompat.getColor(
                                context,
                                R.color.articles_link_box_gradient_end_color,
                            ),
                        )
                    GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply { shape = GradientDrawable.RECTANGLE }
                }
        }
    }

    private fun prepareHeaderItems(
        itemPosition: Int,
        model: ElementGroupLinkBox,
    ) {
        kicker.visibility = View.GONE
        title.visibility = View.GONE
        dateTime.visibility = View.GONE

        // apply styles
        applyStyleToTextView(
            itemPosition,
            kicker,
            model.additionalProperties?.kicker,
            helper.textLinkBoxKickerStyle,
        )
        applyStyleToTextView(
            itemPosition,
            title,
            model.additionalProperties?.linkBoxTitle,
            helper.textLinkBoxHeadlineStyle,
        )
        applyStyleToTextView(
            itemPosition,
            subHeadline,
            model.additionalProperties?.linkBoxSubheadline,
            helper.textLinkBoxSubHeadlineStyle,
        )
        var dateTimeStr =
            getDate(
                DATE_FORMAT_STRING,
                model.additionalProperties?.linkBoxDisplayDate,
            )
        if (!dateTimeStr.isNullOrEmpty()) {
            dateTimeStr = context.getString(R.string.element_group_date_prefix) + dateTimeStr
            applyStyleToTextView(
                itemPosition,
                dateTime,
                dateTimeStr,
                helper.textLinkBoxDatelineStyle,
            )
        }
        createKey(itemPosition, kicker, kicker.text)
        createKey(itemPosition, title, title.text)
        createKey(itemPosition, dateTime, dateTime.text)
        createKey(itemPosition, subHeadline, subHeadline.text)
    }

    private fun prepareBodyItems(
        itemPosition: Int,
        items: List<Item>,
    ) {
        itemsContainer.removeAllViews()
        var i = 0
        var offset = 0
        while (i < items.size + offset && i < items.size) {
            if (items[i] is SanitizedHtml) {
                val formattedText = sanitizedHtmlTextFormatter.format(items[i] as SanitizedHtml)
                val builder = SpannableStringBuilder(formattedText as SpannableString)
                if (builder.isNotEmpty()) {
                    val articleTextItem =
                        LayoutInflater.from(container.context).inflate(
                            R.layout.fragment_article_text,
                            itemsContainer,
                            false,
                        )
                    val textView = articleTextItem.findViewById(R.id.article_text) as SelectableTextView
                    // Note: There is a spacing issue between elements and the show button due to last item is not having a new line.
                    if (builder[builder.length - 1] != '\n') {
                        builder.append('\n')
                    }
                    textView.setLineSpacing(
                        StylesHelper.getTextSpacingExtra(context),
                        StylesHelper.getTextSpacingMult(context),
                    )
                    textView.text = builder

                    textView.movementMethod = LinkMovementMethod.getInstance()

                    itemsContainer.addView(articleTextItem)
                    createKey(itemPosition, textView, textView.text)
                }
            } else if (items[i] is ListItem) {
                val listItem: ListItem = items[i] as ListItem
                val articleText: SelectableTextView =
                    LayoutInflater.from(container.context).inflate(
                        R.layout.fragment_article_text,
                        itemsContainer,
                        false,
                    ) as SelectableTextView

                articleText.movementMethod = LinkMovementMethod.getInstance()

                val builder = ListViewHolder.processListItems(listItem)

                val style: Int = helper.textLinkBoxItemStyle

                builder.setSpan(
                    WpTextAppearanceSpan(context, style),
                    0,
                    builder.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
                )

                val stringBuilderWithLinks =
                    StylesHelper.makeLinkClickable(
                        builder,
                        context,
                        articlesInteractionHelper,
                    )

                articleText.setLineSpacing(
                    StylesHelper.getTextSpacingExtra(context),
                    StylesHelper.getTextSpacingMult(context),
                )

                articleText.text = stringBuilderWithLinks
                articleText.key =
                    KeyHelper.createKey(
                        itemPosition,
                        stringBuilderWithLinks.toString(),
                    )
                articleText.append("\n")
                itemsContainer.addView(articleText)
            } else {
                offset++
            }
            i++
        }
        itemsContainer.visibility = if (itemsContainer.childCount > 0) View.VISIBLE else View.GONE
    }

    private fun prepareFooterItems(
        itemPosition: Int,
        items: List<Item>,
        parentItem: ElementGroup,
    ) {
        show.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && this is MaterialButton) {
                setRippleColorResource(R.color.post_dsm_blue_bright)
            }
            setOnClickListener {
                visibility = View.INVISIBLE
                onGroupToggleClicked(itemPosition, parentItem)
            }
            text =
                context.getString(
                    if (expanded) R.string.element_group_show_less else R.string.element_group_show_more,
                )
            updateShowButtonDrawable()
            if (!USE_ITEMS_COUNT_APPROACH || (USE_ITEMS_COUNT_APPROACH && items.size > MIN_ITEMS_COUNT)) {
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun showFirstKItemsInTheBody(k: Int) {
        if (itemsContainer.childCount > 0) {
            // already container prepared. just show/hide items.
            // show first k items
            for (i in 0 until k) {
                if (i < itemsContainer.childCount) {
                    itemsContainer.getChildAt(i).visibility = View.VISIBLE
                }
            }
            // and hide remaining items from k+1
            if (k < itemsContainer.childCount) {
                for (i in k until itemsContainer.childCount) {
                    itemsContainer.getChildAt(i).visibility = View.GONE
                }
            }
        }
    }

    private fun adjustItemsContainerHeightBasedOnShowStatus() {
        context.findComponentActivity()?.let {
            itemsContainer.layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FlowableLayout.LayoutParams.WRAP_CONTENT,
                )
            itemsContainer.post {
                itemsContainer.apply {
                    val dimension = DisplayMetrics()
                    it.windowManager.defaultDisplay.getMetrics(dimension)
                    val minHeight = (ITEMS_CONTAINER_HEIGHT_FACTOR * dimension.heightPixels).toInt()
                    if (!expanded) {
                        layoutParams.height = minHeight
                        showGradient()
                    } else {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FlowableLayout.LayoutParams.WRAP_CONTENT,
                            )
                        hideGradient()
                        if (minHeight >= height) {
                            show.visibility = View.GONE
                        }
                    }
                    requestLayout()
                }
            }
        }
    }

    private fun prepareTopRule() {
        val showTopRule =
            (kicker.visibility == View.VISIBLE || title.visibility == View.VISIBLE || dateTime.visibility == View.VISIBLE) &&
                itemsContainer.visibility == View.VISIBLE
        topRule.visibility = if (showTopRule) View.VISIBLE else View.GONE
    }

    private fun applyStyleToTextView(
        itemPosition: Int,
        view: SelectableTextView,
        content: String?,
        style: Int,
    ) {
        if (!content.isNullOrEmpty()) {
            val spannable = SpannableStringBuilder(content)
            spannable.setSpan(
                WpTextAppearanceSpan(view.context, style),
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            view.text = spannable
            view.visibility = View.VISIBLE
            return
        }
        view.visibility = View.GONE
    }

    private fun createKey(
        itemPosition: Int,
        selectableTextView: SelectableTextView,
        content: CharSequence?,
    ) {
        if (!content.isNullOrEmpty()) {
            selectableTextView.key = KeyHelper.createKey(itemPosition, content.toString())
        }
    }

    private fun updateShowButtonDrawable() {
        show.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (expanded) {
                R.drawable.ic_chevronup16
            } else {
                R.drawable.ic_chevrondown16
            },
            0,
        )
        show.compoundDrawablePadding = convertDpToPixel(8f, context).toInt()
    }

    private fun updateGradient(itemSize: Int) {
        gradientContainer.apply {
            if (!expanded && itemSize > MIN_ITEMS_COUNT) {
                visibility = View.VISIBLE
                layoutParams.height = (itemsContainer.measuredHeight * GRADIENT_HEIGHT_FACTOR).toInt()
            } else {
                hideGradient()
            }
            invalidate()
            requestLayout()
        }
    }

    private fun showGradient() {
        gradientContainer.apply {
            visibility = View.VISIBLE
            layoutParams.height = (itemsContainer.layoutParams.height * GRADIENT_HEIGHT_FACTOR).toInt()
            invalidate()
            requestLayout()
        }
    }

    private fun hideGradient() {
        gradientContainer.visibility = View.GONE
    }
}
