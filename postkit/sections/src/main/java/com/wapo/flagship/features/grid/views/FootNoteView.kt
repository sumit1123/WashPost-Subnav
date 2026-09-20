package com.wapo.flagship.features.grid.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.wapo.flagship.features.grid.model.Alignment
import com.wapo.flagship.features.grid.model.FootNote
import com.wapo.flagship.features.pagebuilder.gravity

class FootNoteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {


    fun setFootNote(footNote: FootNote?) {
        if (footNote == null) {
            text = null
            return
        }

        text = if (footNote.mime == "text/html") {
            HtmlCompat.fromHtml(footNote.text, HtmlCompat.FROM_HTML_MODE_COMPACT)
        } else {
            footNote.text
        }

        gravity = footNote.alignment.gravity
    }
}