package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.text.GlobalFont
import com.wapo.text.GlobalFont.MAX_FONT_SIZE
import com.wapo.text.GlobalFont.MIN_FONT_SIZE
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wpds.theme.ArticleFontScale
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.R

class FontSizePreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    lateinit var seekBar: SeekBar
    lateinit var magnifierView: TextView
    lateinit var defaultFontSizeCheckBoxPreference: CheckBoxPreference
    var textItemStyle = -1
    var textSpacingExtra = -1f
    var textSpacingMulti = -1f

    inner class TextSizeSeekBarChangeListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean,
        ) {
            saveProgressAndUpdateView(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            defaultFontSizeCheckBoxPreference.isChecked = false
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.run {
            magnifierView = findViewById(R.id.text_size_magnifier) as TextView
            val checkboxKey = context.getString(R.string.pref_settings_match_default_font_size_key)
            defaultFontSizeCheckBoxPreference =
                preferenceManager.findPreference<CheckBoxPreference>(
                    checkboxKey,
                ) as CheckBoxPreference
            seekBar =
                (findViewById(R.id.text_size_seek_bar) as SeekBar).apply {
                    isEnabled = true
                    progress = sizeToProgress(AppPreferences.getTextSizeAsPerDefaultFontCheck())
                    setOnSeekBarChangeListener(TextSizeSeekBarChangeListener())
                }
            updateMagnifierView()
        }
    }

    private fun updateMagnifierView() {
        val builder = SpannableStringBuilder()
        val text = context.getString(R.string.pref_settings_text_size_maginfier_text)
        builder.append("$text\n$text\n$text")

        var a: TypedArray? = null
        var spacingAttributes: TypedArray? = null
        try {
            if (textItemStyle == -1) {
                a =
                    context.theme.obtainStyledAttributes(
                        com.washingtonpost.android.articles.R.styleable.ArticleItems_article_text_style,
                        com.washingtonpost.android.articles.R.styleable.ArticleItems,
                    )
                textItemStyle =
                    a.getResourceId(
                        com.washingtonpost.android.articles.R.styleable.ArticleItems_article_text_style,
                        com.washingtonpost.android.articles.R.style.ArticleText,
                    )
            }
            builder.setSpan(
                WpTextAppearanceSpan(context, textItemStyle),
                0,
                builder.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
            )

            if (textSpacingExtra == -1f || textSpacingMulti == -1f) {
                spacingAttributes =
                    context
                        .obtainStyledAttributes(
                            textItemStyle,
                            com.washingtonpost.android.articles.R.styleable.ArticleItems,
                        ).apply {
                            textSpacingExtra =
                                getDimensionPixelSize(
                                    com.washingtonpost.android.articles.R.styleable.ArticleItems_article_line_spacing_extra,
                                    0,
                                ).toFloat()
                            textSpacingMulti =
                                getFloat(
                                    com.washingtonpost.android.articles.R.styleable.ArticleItems_article_line_spacing_mult,
                                    1f,
                                )
                        }
            }
            magnifierView.setLineSpacing(textSpacingExtra, textSpacingMulti)
        } finally {
            a?.recycle()
            spacingAttributes?.recycle()
        }

        builder.setSpan(
            ForegroundColorSpan(Color.LTGRAY),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.settings_text)),
            text.length + 1,
            (text.length * 2) + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            ForegroundColorSpan(Color.LTGRAY),
            (text.length * 2) + 2,
            (text.length * 3) + 2,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        builder.setSpan(
            GlobalFontAdjustmentSpan(),
            0,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        magnifierView.text = builder
    }

    override fun onDependencyChanged(
        dependency: Preference,
        disableDependent: Boolean,
    ) {
        if (::seekBar.isInitialized && dependency is CheckBoxPreference) {
            seekBar.progress = sizeToProgress(AppPreferences.getTextSizeAsPerDefaultFontCheck())
        }
        super.onDependencyChanged(dependency, disableDependent)
    }

    private fun saveProgressAndUpdateView(progress: Int) {
        val size = progressToSize(progress)
        AppPreferences.setTextSizeAsPerDefaultFontCheck(size)
        GlobalFont.fontSizeAdjustment = size
        ArticleFontScale.setAdjustment(size)
        updateMagnifierView()
    }

    private fun progressToSize(progress: Int): Float = MIN_FONT_SIZE + (progress * 0.01f * (MAX_FONT_SIZE - MIN_FONT_SIZE))

    private fun sizeToProgress(range: Float): Int = ((range - MIN_FONT_SIZE) * 100f / ((MAX_FONT_SIZE - MIN_FONT_SIZE))).toInt()
}
