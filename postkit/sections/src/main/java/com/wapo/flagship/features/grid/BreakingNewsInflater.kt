package com.wapo.flagship.features.grid

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.wapo.text.TypefaceCache
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView

/**
 * A helper class that inflates and sets up breaking news views
 */
class BreakingNewsInflater {

    private lateinit var typeFace: Typeface

    fun createBreakingNewsBar(item: BreakingNewsBarEntity, context: Context, container: ViewGroup, onClickListener: View.OnClickListener, onCloseListener: (View) -> Unit): View {
        val view = LayoutInflater.from(context).inflate(R.layout.breaking_news_overlay, container, false)
        val textView = view.findViewById<TextView>(R.id.breaking_news_text)
        textView.text = item.getText()
        view.setOnClickListener(onClickListener)
        view.findViewById<View>(R.id.close).setOnClickListener {
            onCloseListener(view)
        }
        return view
    }

    fun createLiveVideoBar(item: LiveVideoBarEntity, context: Context, container: ViewGroup, onClickListener: View.OnClickListener, onCloseListener: (View) -> Unit, imageLoader: AnimatedImageLoader): View {
        val view = LayoutInflater.from(context).inflate(R.layout.live_video_overlay, container, false)
        val textView = view.findViewById<TextView>(R.id.live_video_text)
        textView.text = item.getText()
        if (!this::typeFace.isInitialized) {
            typeFace = TypefaceCache.getTypeface(context, "Franklin-ITC-Pro-Bold.otf")
        }
        textView.typeface = typeFace
        view.setOnClickListener(onClickListener)
        view.findViewById<View>(R.id.close).setOnClickListener {
            onCloseListener(view)
        }
        val url = item.media?.url ?: item.media?.promoImageURL
        view.findViewById<NetworkAnimatedImageView>(R.id.image_live_video_thumbnail)?.setImageUrl(url, imageLoader)
        return view
    }
}