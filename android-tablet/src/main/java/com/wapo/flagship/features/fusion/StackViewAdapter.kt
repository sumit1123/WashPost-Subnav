package com.wapo.flagship.features.fusion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wapo.flagship.features.grid.model.Carousel
import com.wapo.view.ProportionalLayout
import com.washingtonpost.android.sections.R

class StackViewAdapter(
    private val carousel: Carousel,
) : BaseAdapter() {
    override fun getCount(): Int = carousel.items.size

    override fun getItem(position: Int): Any = carousel.items[position]

    override fun getItemId(position: Int): Long =
        carousel.items[position]
            .link.url
            .hashCode()
            .toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val item = carousel.items[position]
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.section_stack_item, parent, false)
            holder = ViewHolder()
            holder.imageContainer = (convertView as ViewGroup).findViewById(R.id.image_container)
            holder.imageView = (convertView as ViewGroup).findViewById(R.id.image)
            holder.excerpt = (convertView as ViewGroup).findViewById(R.id.excerpt_text)

            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.imageContainer?.aspectRatio = 0.8f

        val requestOption = RequestOptions().centerCrop()
        Glide.with(parent.context).load(item.media.url).apply(requestOption).into(
            holder.imageView!!,
        )
        if (item.excerpt != null) {
            holder.excerpt?.visibility = View.VISIBLE
            holder.excerpt?.text = item.excerpt?.text
        } else {
            holder.excerpt?.visibility = View.GONE
        }
        return convertView
    }

    inner class ViewHolder {
        internal var imageContainer: ProportionalLayout? = null
        internal var imageView: ImageView? = null
        internal var excerpt: TextView? = null
    }
}
