package com.wapo.flagship.features.grid.views.electionsdelay

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.model.ElectionsDelayMessage
import com.washingtonpost.android.sections.R

class ElectionsDelayHolder(itemView: View) : GridViewHolder(itemView) {

    private val textView = itemView.findViewById(R.id.text) as TextView

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val item = gridAdapter.items[position] as ElectionsDelayMessage
        textView.text =
                SpannableStringBuilder(item.text)
                        .append(" ")
                        .append(item.linkText)
                        .also { it.setSpan(UnderlineSpan(), it.length - item.linkText.length, it.length, SpannableString.SPAN_INCLUSIVE_INCLUSIVE) }
        textView.setOnClickListener { gridAdapter.environment.openLink(item.url) }
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}