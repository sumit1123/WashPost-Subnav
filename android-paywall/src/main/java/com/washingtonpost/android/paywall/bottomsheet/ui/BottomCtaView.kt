package com.washingtonpost.android.paywall.bottomsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.text.HtmlCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wapo.android.commons.util.setVisible
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.bottomsheet.model.BottomCtaType
import com.washingtonpost.android.paywall.databinding.ArticleBottomsheetCtaBinding
import com.washingtonpost.android.paywall.util.*

class BottomCtaView : FrameLayout {

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var sheetBehavior: BottomSheetBehavior<BottomCtaView>? = null

    private val binding =
        ArticleBottomsheetCtaBinding.inflate(LayoutInflater.from(context), this, true)

    fun initBehavior() {
        sheetBehavior = BottomSheetBehavior.from(this)
        sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        sheetBehavior?.peekHeight = binding.root.height
    }

    fun setCtaType(ctaType: BottomCtaType) {
        binding.giftCta.setVisible(ctaType == BottomCtaType.GIFT)
    }

    fun setGiftText(subText: String) {
        val fullText = "${context.resources.getString(R.string.valid_gift_cta)} <b>$subText</b>"
        binding.giftCta.text = getFormattedStringForText(
            context, fullText, mutableListOf(
                RichTextEncodingStyle(
                    style = R.style.gift_cta_text_style_bold,
                    BOLD_START,
                    BOLD_ENDED
                ),
            )
        )
    }
}