package com.wapo.flagship.features.grid.views

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import com.wapo.android.commons.util.Logger
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.text.HtmlCompat
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.wapo.android.commons.util.UiUtils.dp
import com.wapo.flagship.features.grid.LabelStyleEntity
import com.wapo.flagship.features.grid.model.Alignment
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.Form
import com.wapo.flagship.features.pagebuilder.gravity
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.applyUnderline
import com.wapo.view.FormView
import com.washingtonpost.android.sections.R
import com.wpds.theme.AndroidClassicTheme


class CompoundLabelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val styleFullSpan: Int
    private val stylePackageStandard: Int
    private val stylePackageNestedStandard: Int
    private val stylePackageCardified: Int
    private val stylePackageNestedCardified: Int
    private var stylePackage: Int
    private var stylePackageNested: Int
    private val stylePill: Int
    private val styleMini: Int
    private val styleKicker: Int
    private val styleLiveUpdatePrimary: Int
    private val styleExclusivePrimary: Int
    private val styleCta: Int
    private val styleNewsletter: Int
    private val styleButton: Int
    private val styleComment: Int
    private val styleFeaturing: Int

    private val styleSecondaryExplainer: Int
    private val styleSecondaryContentType: Int
    private val styleSecondaryContentTypeNewsletter: Int

    private var textColor: Int
    private val packageLabelRuleColor: Int
    private val packageLabelRuleWidth: Int

    private val labelContainer: LinearLayout
    private val textViewPrimary: TextView
    private val textViewSecondary: TextView
    private val composeFormWrapperStub: ViewStub
    private var composeFormWrapper: ComposeView? = null

    private var rightChevronIcon: Drawable?
    private var rightChevronLufIcon: Drawable?
    private var rightArrowIcon: Drawable?

    private var label: CompoundLabel? = null

    private var packageLabelPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val packageLabelRulePadding: Int
    private val showPackagerNestedRuler : Boolean
    private val iconSizeSmall: Int
    private val iconSizeLarge: Int

    private val horizontalMarginBetweenLabels: Int

    private var isCardified: Boolean = false

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_compound_label, this, true)
        labelContainer = findViewById(R.id.label_container)
        textViewPrimary = findViewById(R.id.labelPrimary)
        textViewSecondary = findViewById(R.id.labelSecondary)
        composeFormWrapperStub = findViewById(R.id.compose_form_wrapper_stub)

        this.orientation = VERTICAL

        horizontalMarginBetweenLabels =
            resources.getDimensionPixelSize(R.dimen.compound_label_horizontal_margin)

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CompoundLabelView,
            0, 0)

        styleFullSpan = a.getResourceId(R.styleable.CompoundLabelView_font_style_full_span, R.style.grid_label_full_span)

        stylePackageStandard = a.getResourceId(R.styleable.CompoundLabelView_font_style_package, R.style.grid_label_primary_package)
        stylePackageNestedStandard = a.getResourceId(R.styleable.CompoundLabelView_font_style_package, R.style.grid_label_primary_package)
        stylePackageCardified = a.getResourceId(R.styleable.CompoundLabelView_font_style_package_cardified, R.style.grid_label_primary_package_cardified)
        stylePackageNestedCardified = a.getResourceId(R.styleable.CompoundLabelView_font_style_package_cardified, R.style.grid_label_primary_package_cardified)
        stylePackage = if (isCardified) {
            stylePackageCardified
        } else {
            stylePackageStandard
        }
        stylePackageNested = if (isCardified) {
            stylePackageNestedCardified
        } else {
            stylePackageNestedStandard
        }

        stylePill = a.getResourceId(R.styleable.CompoundLabelView_font_style_pill, R.style.grid_label_primary_pill)
        styleMini = a.getResourceId(R.styleable.CompoundLabelView_font_style_mini_all_caps, R.style.grid_label_primary_mini)
        styleKicker = a.getResourceId(R.styleable.CompoundLabelView_font_style_kicker, R.style.grid_label_primary_kicker)
        styleLiveUpdatePrimary = a.getResourceId(R.styleable.CompoundLabelView_font_style_live_update, R.style.grid_label_live_updates_primary)
        styleExclusivePrimary = a.getResourceId(R.styleable.CompoundLabelView_pill_exclusive_style, R.style.grid_label_exclusive_primary)
        styleCta = a.getResourceId(R.styleable.CompoundLabelView_font_style_cta, R.style.grid_label_primary_cta)
        styleNewsletter = a.getResourceId(R.styleable.CompoundLabelView_font_style_newsletter, R.style.grid_label_primary_newsletter)
        styleButton = a.getResourceId(R.styleable.CompoundLabelView_font_style_button, R.style.grid_label_primary_button)
        styleComment = a.getResourceId(R.styleable.CompoundLabelView_font_style_comment, R.style.grid_label_primary_comment)
        styleFeaturing = a.getResourceId(R.styleable.CompoundLabelView_font_style_featuring, R.style.grid_label_primary_featuring)


        styleSecondaryExplainer = a.getResourceId(R.styleable.CompoundLabelView_font_secondary_explainer, R.style.grid_label_secondary_explainer)
        styleSecondaryContentType = a.getResourceId(R.styleable.CompoundLabelView_font_secondary_content_type, R.style.grid_label_secondary_content_type)
        styleSecondaryContentTypeNewsletter = a.getResourceId(R.styleable.CompoundLabelView_font_secondary_content_type, R.style.grid_label_secondary_content_type_newsletter)

        textColor = a.getColor(R.styleable.CompoundLabelView_label_text_color, context.resources.getColor(R.color.cell_homepagestory_label))
        packageLabelRuleColor = a.getColor(R.styleable.CompoundLabelView_package_label_rule_color, context.resources.getColor(R.color.cell_package_label_rule))
        packageLabelRuleWidth = a.getDimensionPixelSize(R.styleable.CompoundLabelView_package_label_rule_width, resources.getDimensionPixelSize(R.dimen.package_label_rule_width))
        packageLabelPaint.apply {
            color = packageLabelRuleColor
            strokeWidth = packageLabelRuleWidth.toFloat()
            style = Paint.Style.STROKE
        }
        packageLabelRulePadding = context.resources.getDimensionPixelSize(R.dimen.package_label_rule_padding)
        showPackagerNestedRuler = context.resources.getBoolean(R.bool.compound_label_show_package_nested_ruler)
        iconSizeSmall = a.getDimensionPixelSize(R.styleable.CompoundLabelView_label_icon_size_small, resources.getDimensionPixelSize(R.dimen.compound_label_icon_small))
        iconSizeLarge = a.getDimensionPixelSize(R.styleable.CompoundLabelView_label_icon_size_large, resources.getDimensionPixelSize(R.dimen.compound_label_icon_large))
        a.recycle()

        labelContainer.orientation = HORIZONTAL
        labelContainer.isBaselineAligned = true

        rightChevronIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_chevron_right, context.theme)
        rightArrowIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_arrow_right, context.theme)
        rightChevronLufIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_chevron_luf_right, context.theme)
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        //if label is more than line and horizontal, change to vertical and remeasure
        if ((textViewPrimary.lineCount > 1 || textViewSecondary.lineCount > 1) && labelContainer.orientation == HORIZONTAL) {
            labelContainer.orientation = VERTICAL
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun getIconColor(): Int {
        return when(label?.icon){
            CompoundLabel.Icon.COMMENTS -> ContextCompat.getColor(context, R.color.compound_label_comment_icon_tint)
            else -> ContextCompat.getColor(context, R.color.compound_label_icon_tint)
        }
    }

    fun setLabel(label: CompoundLabel?, isRelatedLinksLabel: Boolean = false) {
        if (label != null) {
            this.label = label
            when (label.type) {

                CompoundLabel.Type.Pill -> {
                    applyText(textViewPrimary, getPrimaryStyle(label.type), label.text)
                    textViewPrimary.height = context.resources.getDimensionPixelSize(R.dimen.pill_height)
                    textViewPrimary.gravity = Gravity.CENTER_VERTICAL
                    textViewPrimary.setBackgroundResource(R.drawable.button_rounded_red)
                }
                CompoundLabel.Type.Exclusive -> {
                    setExclusivePillTextAndStyles(textViewPrimary, label.text, getPrimaryStyle(label.type), R.drawable.ic_wp, R.color.exclusive_text_color)
                }
                CompoundLabel.Type.BrandPromo -> {
                    @ColorRes val textColorRes = when (label.style) {
                        CompoundLabel.LabelStyle.WP_INTELLIGENCE -> R.color.featuring_wpi_text_color
                        else -> R.color.featuring_default_text_color
                    }
                    applyText(
                        textViewPrimary,
                        getPrimaryStyle(label.type),
                        label.text,
                        textColor = ContextCompat.getColor(context, textColorRes)
                    )

                    textViewPrimary.height = context.resources.getDimensionPixelSize(R.dimen.pill_height)
                    textViewPrimary.gravity = Gravity.CENTER_VERTICAL

                    @DrawableRes val bgRes = when (label.style) {
                        CompoundLabel.LabelStyle.WP_INTELLIGENCE -> R.drawable.featuring_wp_intelligence_shape
                        else -> R.drawable.featuring_default_shape
                    }
                    textViewPrimary.setBackgroundResource(bgRes)
                }
                CompoundLabel.Type.Button -> when {
                    isIconButton(label) -> {
                        val fgColor = ContextCompat.getColor(context, R.color.newsletter_button_text)
                        val icon = getDrawable(
                            label.icon ?: CompoundLabel.Icon.EXTERNAL_LINK,
                            fgColor
                        )?.mutate()
                        val background = ContextCompat.getDrawable(context, R.drawable.black_circular_shape)?.mutate()
                        if (background != null && icon != null) {
                            textViewPrimary.apply {
                                isVisible = true
                                text = null
                                width = resources.getDimensionPixelSize(R.dimen.circular_button_height)
                                height = resources.getDimensionPixelSize(R.dimen.circular_button_height)

                                val layers = arrayOf(
                                    background,
                                    InsetDrawable(icon, context.dp(12))
                                )
                                val layerDrawable = LayerDrawable(layers)
                                this.background = layerDrawable
                            }
                        } else {
                            textViewPrimary.isVisible = false
                        }
                    }

                    else -> {
                        applyText(textViewPrimary, getPrimaryStyle(label.type), label.text)
                        textViewPrimary.height = context.resources.getDimensionPixelSize(R.dimen.pill_height)
                        textViewPrimary.setBackgroundResource(R.drawable.black_pill_shape)
                    }
                }
                CompoundLabel.Type.Newsletter -> {
                    applyText(textViewPrimary, getPrimaryStyle(label.type), label.text)
                    applyText(textViewSecondary, getSecondaryStyle(label.type), label.secondaryText)
                    textViewPrimary.setBackgroundResource(R.drawable.rectangle_outline_shape)
                    textViewSecondary.setBackgroundResource(R.drawable.rectangle_outline_shape_secondary)
                }
                CompoundLabel.Type.Comment -> {
                    applyText(textViewPrimary, getPrimaryStyle(label.type), label.text, label.style)
                    textViewPrimary.background = null
                }
                CompoundLabel.Type.Kicker -> {
                    applyText(
                        textViewPrimary,
                        textStyle = getPrimaryStyle(label.type),
                        text = label.text,
                        style = label.style,
                        textSizeSp = if (label.style == CompoundLabel.LabelStyle.THE_SEVEN_LIVE) 26 else null,
                    )
                    textViewPrimary.background = null
                    textViewPrimary.setPadding(0, 0, 0, 0)
                }
                else -> {
                    applyText(textViewPrimary, getPrimaryStyle(label.type), label.text, label.style)
                    textViewPrimary.background = null
                    textViewPrimary.setPadding(0, 0, 0, 0)
                }
            }
            applyText(textViewSecondary, getSecondaryStyle(label.type), label.secondaryText)
        } else {
            textViewPrimary.background = null
            textViewPrimary.text = null
            textViewSecondary.text = null
        }
        (getGravity(label)).let {
            this.gravity = it
            labelContainer.gravity = it
            textViewPrimary.gravity = it or Gravity.CENTER_VERTICAL
            textViewSecondary.gravity = it or Gravity.CENTER_VERTICAL
        }

        applyOrientation(label)
        applyMargin(label)
        applyIcons(label)
        applyForm(label?.form)
    }

    private fun applyForm(form: Form?) {
        if (form == null) {
            composeFormWrapperStub.visibility = View.GONE
        } else {
            composeFormWrapperStub.visibility = View.VISIBLE
            if (composeFormWrapper == null) {
                composeFormWrapper = findViewById(R.id.compose_form_wrapper)
            }
            composeFormWrapper?.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )

            val searchField = form.fields.firstOrNull { it.type == "search" }
            composeFormWrapper?.setContent {
                AndroidClassicTheme {
                    Surface (
                        color = Color.Unspecified
                    ) {
                        if (searchField != null) {
                            FormView(
                                showSearch = true,
                                searchPlaceHolder = searchField.placeHolder,
                                onSearchTapped = {
                                    launchAsDeepLink(
                                        form.action
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyMargin(label: CompoundLabel?) {
        when (label?.type) {
            CompoundLabel.Type.Kicker -> {
                val layoutParams = textViewPrimary.layoutParams as LinearLayout.LayoutParams
                layoutParams.marginEnd = horizontalMarginBetweenLabels
                textViewPrimary.layoutParams = layoutParams
            }
            else -> {
                val layoutParams = textViewPrimary.layoutParams as LinearLayout.LayoutParams
                layoutParams.marginEnd = 0
                textViewPrimary.layoutParams = layoutParams
            }
        }
    }

    fun setCardify(isCardified: Boolean) {
        this.isCardified = isCardified
        if (isCardified) {
            stylePackage = stylePackageCardified
            stylePackageNested = stylePackageNestedCardified
        } else {
            stylePackage = stylePackageStandard
            stylePackageNested = stylePackageNestedStandard
        }
    }

    private fun applyIcons(label: CompoundLabel?) {
        when {
            label == null -> {
                textViewPrimary.setCompoundDrawablesRelative(null, null, null, null)
                textViewSecondary.setCompoundDrawablesRelative(null, null, null, null)
            }
            label.type == CompoundLabel.Type.Exclusive || isIconButton(label) -> {
                // skip
            }
            else -> {
                //primary
                val rightDrawable = getRightDrawable(label)
                val leftDrawable = getLeftDrawable(label)
                textViewPrimary.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.compound_drawable_padding)
                textViewPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    leftDrawable,
                    null,
                    rightDrawable,
                    null
                )

                //secondary
                val rightDrawableSecondary = getRightDrawableSecondary(label)
                textViewSecondary.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.compound_drawable_padding)
                textViewSecondary.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    rightDrawableSecondary,
                    null
                )
            }
        }
    }

    /**
     * Calculates the desired orientation and
     * puts some space between primary and secondary labels when in VERTICAL mode
     */
    private fun applyOrientation(label: CompoundLabel?) {
        labelContainer.orientation = getOrientation(label)
        if (labelContainer.orientation == VERTICAL) {
            val spacing = resources.getDimensionPixelSize(R.dimen.compound_label_spacing)
            with (textViewSecondary) {
                setPadding(paddingLeft, spacing, paddingRight, paddingBottom)
            }
        } else {
            with (textViewSecondary) {
                setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        }
    }

    private fun getLeftDrawable(label: CompoundLabel?): Drawable? = getDrawable(label?.icon)

    private fun getDrawable(icon: CompoundLabel.Icon?, iconColor: Int = getIconColor()): Drawable? {
        val size = getLabelIconSize(label)
        if (size > 0) {
            var drawable = when (icon) {
                CompoundLabel.Icon.CAMERA -> VectorDrawableCompat.create(context.resources, R.drawable.ic_label_camera, context.theme)
                CompoundLabel.Icon.CHART -> VectorDrawableCompat.create(context.resources, R.drawable.ic_label_chart, context.theme)
                CompoundLabel.Icon.HEADPHONES -> VectorDrawableCompat.create(context.resources, R.drawable.ic_label_headphones, context.theme)
                CompoundLabel.Icon.ELECTION_STAR -> VectorDrawableCompat.create(context.resources, R.drawable.ic_label_elections, context.theme)
                CompoundLabel.Icon.PLAY -> ContextCompat.getDrawable(context, com.wpds.wpds.R.drawable.play)
                CompoundLabel.Icon.OLYMPICS -> VectorDrawableCompat.create(context.resources, R.drawable.ic_olympics, context.theme)
                CompoundLabel.Icon.THE_7 -> VectorDrawableCompat.create(context.resources, R.drawable.ic_label_briefs, context.theme)
                CompoundLabel.Icon.THE_7_LIVE_CHIP -> VectorDrawableCompat.create(context.resources, R.drawable.the_seven_live_chip, context.theme)
                CompoundLabel.Icon.WORLD_CUP -> VectorDrawableCompat.create(context.resources, R.drawable.ic_world_cup, context.theme)
                CompoundLabel.Icon.POST_PULSE -> VectorDrawableCompat.create(context.resources, R.drawable.ic_post_pulse, context.theme)
                CompoundLabel.Icon.COMMENTS -> VectorDrawableCompat.create(context.resources, com.wpds.wpds.R.drawable.comment, context.theme)
                CompoundLabel.Icon.EXTERNAL_LINK -> VectorDrawableCompat.create(context.resources, com.wapo.flagship.features.audio.R.drawable.ic_external_link, context.theme)
                else -> null
            }
            drawable?.setBounds(0, 0, size, size)
            if (label?.icon?.isColored != true) {
                drawable = drawable?.mutate()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    drawable?.colorFilter = BlendModeColorFilter(iconColor, BlendMode.SRC_ATOP)
                } else {
                    drawable?.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
                }
            }
            return drawable
        }
        return null
    }

    private fun getLabelIconSize(label: CompoundLabel?): Int {
        return when (label?.type) {
            CompoundLabel.Type.Pill -> 0
            CompoundLabel.Type.MiniAllCaps -> iconSizeSmall
            CompoundLabel.Type.Kicker -> iconSizeSmall
            CompoundLabel.Type.Promo -> iconSizeSmall
            null -> 0
            else -> iconSizeLarge
        }
    }

    private fun getGravity(label: CompoundLabel?): Int {
        return label?.alignment.gravity.takeIf { it != Gravity.NO_GRAVITY } ?: Gravity.START
    }

    private fun getPrimaryStyle(type: CompoundLabel.Type): Int {
        return when (type) {
            CompoundLabel.Type.FullSpan -> styleFullSpan
            CompoundLabel.Type.Package -> stylePackage
            CompoundLabel.Type.Pill -> stylePill
            CompoundLabel.Type.MiniAllCaps -> styleMini
            CompoundLabel.Type.Kicker -> styleKicker
            CompoundLabel.Type.LiveUpdates -> styleLiveUpdatePrimary
            CompoundLabel.Type.Exclusive -> styleExclusivePrimary
            CompoundLabel.Type.PackageNested -> stylePackageNested
            CompoundLabel.Type.Promo -> styleFullSpan
            CompoundLabel.Type.Cta -> styleCta
            CompoundLabel.Type.Newsletter -> styleNewsletter
            CompoundLabel.Type.Button -> styleButton
            CompoundLabel.Type.Comment -> styleComment
            CompoundLabel.Type.BrandPromo -> styleFeaturing
        }
    }

    private fun getSecondaryStyle(type: CompoundLabel.Type): Int {
        // package, package-nested, live-updates, pill, exclusive-pill, and promo use Georgia-Italic
        // kicker and cta use FranklinITCStd-Light
        return when (type) {
            CompoundLabel.Type.FullSpan -> styleSecondaryExplainer
            CompoundLabel.Type.Package -> styleSecondaryExplainer
            CompoundLabel.Type.Pill -> styleSecondaryExplainer
            CompoundLabel.Type.MiniAllCaps -> styleSecondaryContentType
            CompoundLabel.Type.Kicker -> styleSecondaryContentType
            CompoundLabel.Type.LiveUpdates -> styleSecondaryExplainer
            CompoundLabel.Type.Exclusive -> styleSecondaryExplainer
            CompoundLabel.Type.PackageNested -> styleSecondaryExplainer
            CompoundLabel.Type.Promo -> styleSecondaryExplainer
            CompoundLabel.Type.Cta -> styleSecondaryContentType
            CompoundLabel.Type.Newsletter -> styleSecondaryContentTypeNewsletter
            CompoundLabel.Type.Button -> styleSecondaryExplainer
            CompoundLabel.Type.Comment -> styleSecondaryExplainer
            CompoundLabel.Type.BrandPromo -> styleSecondaryExplainer
        }
    }

    private fun applyText(
        textView: TextView,
        textStyle: Int,
        text: String?,
        style: CompoundLabel.LabelStyle? = null,
        textColor: Int? = null,
        textSizeSp: Int? = null,
    ) {
        if (text.isNullOrEmpty()) {
            textView.text = null
            textView.visibility = View.GONE
            return
        }
        textView.visibility = View.VISIBLE

        val htmlSpanned = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val labelSpan = SpannableStringBuilder(htmlSpanned)

        val fontSpan = if (textSizeSp != null) {
            val textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp.toFloat(),
                context.resources.displayMetrics
            )
            WpTextAppearanceSpan(context, textStyle, textSizePx.toInt())
        } else {
            WpTextAppearanceSpan(context, textStyle)
        }
        if ((textStyle != stylePill && textStyle != styleLiveUpdatePrimary && textStyle != stylePackage && textStyle != stylePackageNested && textStyle != styleButton && textStyle != styleComment && textStyle != styleFeaturing) || textColor != null) {
            fontSpan.setTextColor(ColorStateList.valueOf(textColor ?: this.textColor))
        }
        labelSpan.setSpan(
            fontSpan,
            0, labelSpan.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (style?.name == LabelStyleEntity.OPINIONS.name) {
            labelSpan.applyUnderline(context, 0, 1, com.wpds.wpds.R.color.opinion_spark, resources.getInteger(com.wapo.view.R.integer.first_part_opinion_left_padding_underline).toFloat(),  resources.getInteger(
                com.wapo.view.R.integer.first_part_opinion_right_padding_underline).toFloat(), -4f)
            labelSpan.applyUnderline(context,2, labelSpan.length, com.wpds.wpds.R.color.opinion_spark,   resources.getInteger(
                com.wapo.view.R.integer.second_part_opinion_left_padding_underline).toFloat(),  resources.getInteger(
                com.wapo.view.R.integer.second_part_opinion_right_padding_underline).toFloat(), -4f)
        }
        textView.setTextAppearance(context, textStyle)
        textView.text = labelSpan

    }

    private fun getRightDrawable(label: CompoundLabel?): Drawable? {
        return when {
            label?.style == CompoundLabel.LabelStyle.THE_SEVEN_LIVE -> getDrawable(CompoundLabel.Icon.THE_7_LIVE_CHIP)
            shouldDisplayRightDrawableOnPrimary() -> getRightChevron(label)
            else -> null
        }
    }

    private fun getRightDrawableSecondary(label: CompoundLabel?): Drawable? {
        return if (shouldDisplayRightDrawableOnSecondary()) {
            getRightChevron(label)
        } else {
            null
        }
    }

    private fun getRightChevron(label: CompoundLabel?): Drawable? {
        label ?: return rightChevronIcon
        if (label.type == CompoundLabel.Type.LiveUpdates) {
            return rightChevronLufIcon
        } else if (label.type == CompoundLabel.Type.Cta) {
            return rightArrowIcon
        }
        return rightChevronIcon
    }

    /**
     * Chevron Rules for Primary and Secondary labels:
     * 1. If the primary and secondary labels are on separate lines, the chevron goes after the primary label
     * 2. If the primary and secondary labels are on the same line, the chevron goes at the end of the secondary label
     * Single line primary+secondary labels are only supported for kicker style labels.
     * But, if the combined primary and secondary kicker text wraps, it falls back to stacking the
     * two labels and putting the chevron after the primary label.
     */
    private fun shouldDisplayRightDrawableOnPrimary(): Boolean {
        return label?.hasArrow == true && (label?.secondaryText == null || labelContainer.orientation == VERTICAL)
    }

    private fun shouldDisplayRightDrawableOnSecondary(): Boolean {
        return label?.hasArrow == true && label?.secondaryText != null && labelContainer.orientation == HORIZONTAL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isCardified && hasRuler(label)) {
            labelContainer.setPadding(0, packageLabelRulePadding, 0 ,0)
            val top = textViewPrimary.top.toFloat() - packageLabelRulePadding
            canvas.drawLine(0f, top, width.toFloat(), top, packageLabelPaint)
        }
    }

    /**
     * Sets the pill style for exclusive type.
     * [textView] textview on which the styling is applied
     * [text] label
     * [style] text view style
     * [drawable] left/start drawable
     * [drawableTint] drawable color
     */
    private fun setExclusivePillTextAndStyles(
        textView: TextView,
        text: String?,
        @StyleRes style: Int,
        @DrawableRes drawable: Int?,
        @ColorRes drawableTint: Int?
    ) {
        if (text.isNullOrEmpty()) {
            textView.text = null
            textView.visibility = View.GONE
            return
        }
        textView.visibility = VISIBLE

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        val cornerRadius = context.resources.getDimensionPixelSize(R.dimen.exclusive_pill_corner_radius).toFloat()
        shape.cornerRadii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
        shape.setColor(context.resources.getColor(R.color.exclusive_bg_color))
        textView.background = shape
        textView.height = context.resources.getDimensionPixelSize(R.dimen.pill_height)
        val hPadding = context.resources.getDimensionPixelSize(R.dimen.exclusive_pill_horizontal_padding)
        val vPadding = context.resources.getDimensionPixelSize(R.dimen.exclusive_pill_verticle_padding)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.setPadding(hPadding, vPadding, hPadding, vPadding)
        val ss = SpannableStringBuilder()
        if (drawable != null) {
            val icon = AppCompatResources.getDrawable(context, drawable)
            if (icon != null) {
                if (drawableTint != null) {
                    icon.mutate().setColorFilter(ContextCompat.getColor(context, drawableTint), PorterDuff.Mode.SRC_IN)
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                textView.compoundDrawablePadding = context.resources.getDimension(R.dimen.exclusive_pill_drawable_padding).toInt()
            }
        }
        ss.append(text)
        ss.setSpan(
            WpTextAppearanceSpan(textView.context, style),
            0,
            ss.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = ss
    }

    private fun hasRuler(label: CompoundLabel?) : Boolean {
        return when (label?.type) {
            CompoundLabel.Type.Package -> true // always
            CompoundLabel.Type.PackageNested -> false
            CompoundLabel.Type.FullSpan -> label.alignment != Alignment.CENTER // only if not center aligned
            else -> false
        }
    }

    private fun getOrientation(label: CompoundLabel?): Int {
        return when (label?.type) {
            CompoundLabel.Type.Kicker -> HORIZONTAL
            CompoundLabel.Type.Cta -> HORIZONTAL
            CompoundLabel.Type.Newsletter -> HORIZONTAL
            else -> VERTICAL
        }
    }

    fun updateKickerLabelColors(color: Int) {
        if (this.label?.type == CompoundLabel.Type.Kicker) {
            val primarySpan = SpannableString(textViewPrimary.text)
            primarySpan.setSpan(
                ForegroundColorSpan(color),
                0,
                primarySpan.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textViewPrimary.text = primarySpan

            val secondarySpan = SpannableString(textViewSecondary.text)
            secondarySpan.setSpan(
                ForegroundColorSpan(context.resources.getColor(com.wpds.wpds.R.color.gray80)),
                0,
                secondarySpan.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textViewSecondary.text = secondarySpan
        }
    }

    fun setTextColor(color: Int) {
        textColor = color
    }

    private fun isIconButton(label: CompoundLabel?): Boolean {
        return label?.type == CompoundLabel.Type.Button && label.text.isNullOrEmpty() && label.secondaryText.isNullOrEmpty()
    }

    private fun launchAsDeepLink(action: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse(action)
            intent.data = uri
            Logger.d("CompoundLabelView", "launching deep link: $uri")
            context.startActivity(intent)
        } catch (t: Throwable) {
            Logger.e("CompoundLabelView", "failed to launch deeplink")
        }
    }

    /**
     * Reset label data and internal views (if needed) to make it ready for
     * other items
     */
    fun resetView() {
        this.label = null
    }
}
