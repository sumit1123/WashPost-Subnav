package com.wapo.flagship.features.pagebuilder

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.wapo.flagship.features.grid.model.BlurbList
import com.wapo.flagship.features.grid.model.BlurbFontStyle
import com.wapo.flagship.features.grid.model.BlurbStyle
import com.wapo.flagship.features.grid.model.BulletType
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.FirstTextLineDrawable
import com.wapo.view.FlowableTextView
import com.wapo.view.FlowableView
import com.washingtonpost.android.sections.R

class BlurbView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr),
    FlowableView {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private var textGravity: Int = Gravity.NO_GRAVITY
    private var blurbs: BlurbList? = null
    private var blurbStyleNormal: Int
    private var blurbLikeArticleBodyStyle: Int
    private var conversationsBlurbStyleNormal: Int
    private val listDrawable: Drawable?
    private val blurbViews = mutableListOf<View>()
    private var textColor: Int? = null
    var onBlurbsLinkClick: ((String?) -> Unit)? = null


    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BlurbView, defStyleAttr, 0)
        try {
            blurbStyleNormal = a.getResourceId(R.styleable.BlurbView_font_size_normal, R.style.homepagestory_blurb_style)
            blurbLikeArticleBodyStyle = a.getResourceId(R.styleable.BlurbView_like_article_body, R.style.homepagestory_blurb_style_like_article_body)
            conversationsBlurbStyleNormal = a.getResourceId(R.styleable.BlurbView_conversations_normal, R.style.homepagestory_conversations_blurb_style)
            val listDrawable = a.getDrawable(R.styleable.BlurbView_blurb_list_drawable)
            if (listDrawable != null) {
                this.listDrawable = listDrawable
            } else {
                this.listDrawable = ContextCompat.getDrawable(context, R.drawable.circle_solid)
            }
        } finally {
            a.recycle()
        }
        isFocusable = true
    }

    fun setBlurbs(blurbs: BlurbList?, isGrid : Boolean) {
        if (this.blurbs == blurbs) {
            return
        }
        this.blurbs = blurbs
        removeAllViews()
        blurbViews.clear()
        blurbs?.items?.forEachIndexed { index, blurb ->
            if (!blurb.text.isNullOrBlank()) {
                val blurbSpan = if (blurb.mime == "text/html") {
                    SpannableStringBuilder(HtmlCompat.fromHtml(blurb.text, FROM_HTML_MODE_LEGACY))
                } else {
                    SpannableStringBuilder(blurb.text.replace("\\n".toRegex(), "\n\n"))
                }
                val blurbStyle = when(blurbs.style) {
                    BlurbStyle.CONVERSATIONS -> conversationsBlurbStyleNormal
                    else -> getBlurbStyle(blurbs)
                }
                val textAppearanceSpan = WpTextAppearanceSpan(
                    context,
                    blurbStyle,
                    getBlurbSize(context, blurbs.info, isGrid)
                ).apply {
                    if (blurbs.style != BlurbStyle.CONVERSATIONS) {
                        textColor?.let { setTextColor(ColorStateList.valueOf(it)) }
                    }
                }
                blurbSpan.setSpan(
                        textAppearanceSpan,
                        0, blurbSpan.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setClickableSpans(blurbSpan)
                addView(FlowableTextView(context).apply {
                    if (hasClickableSpans(blurbSpan)) {
                        setMovementMethod(LinkMovementMethod.getInstance())
                    }
                    setTextGravity(textGravity)
                    val wrappedBullet = if (listDrawable != null) FirstTextLineDrawable(listDrawable, this) else null
                    setText(blurbSpan, if (blurb.type == BulletType.BULLET) wrappedBullet else null, null)
                    val lineSpacing = TypedValue()
                    resources.getValue(R.dimen.blurb_line_spacing, lineSpacing, true)
                    setLineSpacing(0f, lineSpacing.float)
                    blurbViews.add(this)
                })
            }
        }
    }

    private fun getBlurbStyle(blurbs: BlurbList): Int {
        return when (blurbs.info?.fontStyle) {
            BlurbFontStyle.NORMAL_STYLE -> blurbStyleNormal
            BlurbFontStyle.LIKE_ARTICLE_BODY -> blurbLikeArticleBodyStyle
            else -> blurbStyleNormal
        }
    }

    override fun setFlowObstruction(widthAdjustment: Int, heightAdjustment: Int, floatType: Int) {
        blurbViews.forEach {
            if (it is FlowableTextView) {
                it.setFlowObstruction(widthAdjustment, heightAdjustment, floatType)
            }
        }
    }

    fun setTextGravity(gravity: Int) {
        textGravity = gravity
        for (i in 0 until childCount) {
            (getChildAt(i) as FlowableTextView).setTextGravity(gravity)
        }
    }

    fun hasTextBelowBox(): Boolean {
        return if (childCount > 0) {
            (getChildAt(childCount - 1) as FlowableTextView).hasTextBelowBox()
        } else {
            false
        }
    }

    fun updateBlurbColor(color: Int) {
        this.textColor = color
        val blurbs = this.blurbs
        this.blurbs = null
        setBlurbs(blurbs, true)
    }

    private fun hasClickableSpans(spannable: Spannable): Boolean {
        return spannable.getSpans(0, spannable.length, ClickableSpan::class.java).isNotEmpty()
    }

    private fun setClickableSpans(spannable: Spannable) {
        val urlSpans: Array<URLSpan> = spannable.getSpans(
            0, spannable.length,
            URLSpan::class.java
        )
        urlSpans.forEach { urlSpan ->
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onBlurbsLinkClick?.invoke(urlSpan.url)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = true
                    }
                },
                spannable.getSpanStart(urlSpan),
                spannable.getSpanEnd(urlSpan),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.removeSpan(urlSpan)
        }
    }
}