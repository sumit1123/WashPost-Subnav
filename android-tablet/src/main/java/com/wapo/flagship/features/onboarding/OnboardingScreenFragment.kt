package com.wapo.flagship.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.washingtonpost.android.R

class OnboardingScreenFragment : Fragment() {
    private val screenConfigBundleName = "screenConfig"
    private lateinit var screenConfig: Screen
    private var magnifierImage: ImageView? = null

    companion object {
        fun newInstance(screenConfig: Screen) =
            OnboardingScreenFragment().apply {
                this.screenConfig = screenConfig
                return this
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            it.getSerializable(screenConfigBundleName)?.apply {
                screenConfig = this as Screen
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.onboarding_screen_variant, container, false)

        val heading = view.findViewById<TextView>(R.id.onboarding_heading)
        val description = view.findViewById<TextView>(R.id.onboarding_description)
        heading.text = screenConfig.title
        description.text = screenConfig.description

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(screenConfigBundleName, screenConfig)
        super.onSaveInstanceState(outState)
    }

    fun startMagnifierAnimation() {
        magnifierImage?.let {
            if (isAdded && context != null) {
                if (it.visibility == View.VISIBLE && it.drawable != null) {
                    val fadeIn = AnimationUtils.loadAnimation(context, R.anim.onboarding_fade_in)
                    it.startAnimation(fadeIn)
                }
            }
        }
    }
}
