package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallIconBinding
import com.washingtonpost.android.paywall.util.PaywallConstants

/**
 * Image component for regwalls and paywalls.
 */
class IconView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallIconBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Treats URL as highest priority source.
     * Local drawable is second priority, used when URL fails or is absent.
     * Fallback case hides the ImageView entirely instead of displaying a generic image.
     */
    fun setImage(imageName: String?, darkMode: Int?, urlLight: String?, urlDark: String?, imageUrl: String?, width: Int?, height: Int?) {
        val remoteUrl = getRemoteUrl(darkMode, urlLight, urlDark, imageUrl)
        val localImg: Int = if (imageName != null) {
            this.resources.getIdentifier(imageName, "drawable", context.packageName)
        } else {
            0
        }
        when {
            remoteUrl != null -> {
                val requestOption = if (localImg != 0) {
                    RequestOptions().centerCrop().error(localImg)
                } else {
                    RequestOptions().centerCrop()
                }
                Glide.with(binding.root).load(remoteUrl)
                    .apply(requestOption).into(binding.wallImage)
                setImageBounds(width, height)
            }
            localImg != 0 -> {
                binding.wallImage.setImageResource(localImg)
                setImageBounds(width, height)
            }
            else -> {
                binding.wallImage.visibility = GONE
            }
        }
    }

    /**
     *  Returns appropriate light or dark URL if possible.
     *  Uses agnostic URL as fallback, or light URL if no agnostic URL.
     */
    private fun getRemoteUrl(darkMode: Int?, urlLight: String?, urlDark: String?, imageUrl: String?): String? {
        return when (darkMode) {
            Configuration.UI_MODE_NIGHT_NO -> urlLight ?: imageUrl
            Configuration.UI_MODE_NIGHT_YES -> urlDark ?: imageUrl
            else -> imageUrl ?: urlLight
        }
    }

    /**
     * Overrides default size with custom width and height from config.
     * Values are in dp.
     */
    private fun setImageBounds(width: Int?, height: Int?) {
        if (width != null && width > 0 && height != null && height > 0) {
            val params = binding.wallImage.layoutParams
            params.width = (width * context.resources.displayMetrics.density).toInt()
            params.height = (height * context.resources.displayMetrics.density).toInt()
            binding.wallImage.layoutParams = params
        }
    }
}