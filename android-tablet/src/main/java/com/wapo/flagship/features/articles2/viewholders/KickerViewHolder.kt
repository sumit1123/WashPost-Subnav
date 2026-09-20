package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Kicker
import com.wapo.flagship.features.articles2.models.deserialized.Style
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.applyUnderline
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemKickerBinding

class KickerViewHolder(
    private val binding: ItemKickerBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<Kicker>(binding.root, null) {
    private val context = itemView.context

    override fun onBindItem(
        item: Kicker,
        position: Int,
    ) {
        val displayLabel =
            if (item.liveText != null && item.coverageActive == true) {
                item.liveText
            } else {
                item.displayLabel
            }

        val displayTransparency: String? = item.displayTransparency
        val subType: Kicker.SubType = Kicker.SubType.getValue(displayLabel)
        val style = Style.getValue(item.style)

        when (subType) {
            Kicker.SubType.LIVE -> {
                setPillTextAndStyles(
                    binding.articleHeadingKicker.context,
                    com.washingtonpost.android.articles.R.string.kicker_live_updates,
                    com.washingtonpost.android.articles.R.color.kicker_pill_live_bg_color,
                    KickerStyleHelper.getKickerPillLiveTextStyle(binding.root.context),
                    null,
                    null,
                )
                binding.articleHeadingKicker.visibility = View.VISIBLE
                return
            }
            Kicker.SubType.EXCLUSIVE -> {
                setPillTextAndStyles(
                    binding.articleHeadingKicker.context,
                    com.washingtonpost.android.articles.R.string.kicker_exclusive,
                    com.washingtonpost.android.articles.R.color.kicker_pill_exclusive_bg_color,
                    KickerStyleHelper.getKickerPillExclusiveTextStyle(binding.root.context),
                    com.washingtonpost.android.articles.R.drawable.ic_wp,
                    com.washingtonpost.android.articles.R.color.kicker_pill_exclusive,
                )
                binding.articleHeadingKicker.visibility = View.VISIBLE
                return
            }
            else -> {
                // no op
            }
        }
        val spannableStringBuilder = SpannableStringBuilder()
        if (!displayLabel.isNullOrEmpty()) {
            val styleRes =
                if (style == Style.BRIEFS) {
                    spannableStringBuilder.append(displayLabel.uppercase())
                    KickerStyleHelper.getTextKickerBriefsStyle(binding.root.context)
                } else {
                    spannableStringBuilder.append(displayLabel)
                    KickerStyleHelper.getTextKickerDefaultStyle(binding.root.context)
                }
            spannableStringBuilder.setSpan(
                WpTextAppearanceSpan(
                    itemView.context,
                    styleRes,
                ),
                0,
                displayLabel.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        if (Style.getValue(item.style) == Style.OPINIONS) {
            spannableStringBuilder.applyUnderline(
                context,
                0,
                1,
                com.wpds.wpds.R.color.opinion_spark,
                context.resources.getInteger(com.wapo.view.R.integer.first_part_opinion_left_padding_underline).toFloat(),
                context.resources.getInteger(com.wapo.view.R.integer.first_part_opinion_right_padding_underline).toFloat(),
                -4f,
            )
            displayLabel?.length?.let {
                spannableStringBuilder.applyUnderline(
                    context,
                    2,
                    displayLabel.length,
                    com.wpds.wpds.R.color.opinion_spark,
                    context.resources
                        .getInteger(
                            com.wapo.view.R.integer.second_part_opinion_left_padding_underline,
                        ).toFloat(),
                    context.resources
                        .getInteger(
                            com.wapo.view.R.integer.second_part_opinion_right_padding_underline,
                        ).toFloat(),
                    -4f,
                )
            }
        }
        if (!displayTransparency.isNullOrEmpty()) {
            val startIndex = spannableStringBuilder.length
            if (startIndex > 0) {
                spannableStringBuilder.append("  \u2022  ")
            }
            spannableStringBuilder.append(displayTransparency)
            spannableStringBuilder.setSpan(
                WpTextAppearanceSpan(
                    itemView.context,
                    KickerStyleHelper.getDisplayTransparencyStyle(binding.root.context),
                ),
                startIndex,
                spannableStringBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (spannableStringBuilder.isNotEmpty()) {
            setBottomMargin(0f)
            if (style == Style.BRIEFS) {
                val context = binding.root.context
                val icon = getBriefsIcon(context)
                binding.articleHeadingKicker.gravity = Gravity.CENTER_VERTICAL
                binding.articleHeadingKicker.setCompoundDrawablesWithIntrinsicBounds(
                    icon,
                    null,
                    null,
                    null,
                )
            } else {
                binding.articleHeadingKicker.gravity = Gravity.NO_GRAVITY
                binding.articleHeadingKicker.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null,
                )
            }
            binding.articleHeadingKicker.text = spannableStringBuilder
            binding.articleHeadingKicker.visibility = View.VISIBLE
        } else {
            binding.articleHeadingKicker.visibility = View.GONE
        }
    }

    private fun setUpRedDotAnimation() =
        AlphaAnimation(1f, 0f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }

    override fun setPlaceHolderData(item: Kicker): PlaceHolderData? = null

    override fun onLowDataModeEnable(item: Kicker) {
        binding.root.setOnClickListener(null)
        with(binding.articleHeadingKickerRedPill) {
            clearAnimation()
            visibility = View.GONE
        }
    }

    override fun onLowDataModeDisable(item: Kicker) {
        if (item.liveText != null && item.coverageActive == true) {
            binding.articleHeadingKickerRedPill.visibility = View.VISIBLE
            binding.articleHeadingKickerRedPill.startAnimation(setUpRedDotAnimation())
        } else {
            binding.articleHeadingKickerRedPill.visibility = View.GONE
        }

        val kickerDeepLink = item.path.takeIf { !it.isNullOrEmpty() }
        if (kickerDeepLink != null) {
            val fullUrl = DeepLinksProcessor.pathToFullUrl(kickerDeepLink)
            binding.root.setOnClickListener {
                binding.root.context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(DeepLinksProcessor.sectionPathToDeepLink(fullUrl))),
                )
            }
        } else {
            binding.root.setOnClickListener(null)
        }
    }

    private fun getBriefsIcon(context: Context): Drawable? = AppCompatResources.getDrawable(context, R.drawable.ic_label_briefs)

    private fun setPillTextAndStyles(
        context: Context,
        @StringRes string: Int,
        @ColorRes background: Int,
        @StyleRes style: Int,
        @DrawableRes drawable: Int?,
        @ColorRes drawableTint: Int?,
    ) {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        val cornerRadius =
            context.resources.getDimensionPixelSize(com.washingtonpost.android.articles.R.dimen.kicker_pill_corner_radius)
        shape.cornerRadii =
            floatArrayOf(
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
            )
        shape.setColor(context.resources.getColor(background))
        binding.articleHeadingKicker.background = shape
        val hPadding =
            context.resources.getDimensionPixelSize(com.washingtonpost.android.articles.R.dimen.kicker_pill_hor_padding)
        val vPadding =
            context.resources.getDimensionPixelSize(com.washingtonpost.android.articles.R.dimen.kicker_pill_vert_padding)
        binding.articleHeadingKicker.gravity = Gravity.CENTER_VERTICAL
        binding.articleHeadingKicker.setPadding(hPadding, vPadding, hPadding, vPadding)
        setBottomMargin(
            context.resources.getDimension(com.washingtonpost.android.articles.R.dimen.article_kicker_bottom_padding),
        )
        val ss = SpannableStringBuilder()
        if (drawable != null) {
            val icon =
                AppCompatResources.getDrawable(context, drawable)
            if (icon != null) {
                if (drawableTint != null) {
                    icon.mutate().setColorFilter(
                        ContextCompat.getColor(
                            context,
                            drawableTint,
                        ),
                        PorterDuff.Mode.SRC_IN,
                    )
                }
                binding.articleHeadingKicker.setCompoundDrawablesWithIntrinsicBounds(
                    icon,
                    null,
                    null,
                    null,
                )
            }
        }
        ss.append(context.getString(string))
        ss.setSpan(
            WpTextAppearanceSpan(itemView.context, style),
            0,
            ss.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        binding.articleHeadingKicker.text = ss
    }

    private fun setBottomMargin(margin: Float) {
        val layoutParams =
            binding.articleHeadingKicker.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.bottomMargin = margin.toInt()
    }
}
