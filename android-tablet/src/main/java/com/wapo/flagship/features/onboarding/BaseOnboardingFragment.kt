package com.wapo.flagship.features.onboarding

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.*
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import java.lang.IllegalArgumentException

class BaseOnboardingFragment : DialogFragment() {
    private lateinit var onboardingService: OnboardingService
    private lateinit var onboardingConfig: OnboardingConfig
    private var eventListener: OnboardingEventListener? = null
    private lateinit var viewPager: ViewPager
    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private var backgroundImage: ImageView? = null

    private val actionListener = View.OnClickListener { v -> performAction(v) }

    private val pageListener: ViewPager.OnPageChangeListener =
        object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {}

            override fun onPageSelected(position: Int) {
                updateButtons(position)
                updateBackgroundImage(position)
                (viewPager.adapter as OnboardingViewPagerAdapter).getRegisteredFragment(position)?.let {
                    (it as OnboardingScreenFragment).startMagnifierAnimation()
                }
            }
        }

    private fun updateButtons(position: Int) {
        val screen =
            eventListener?.getScreenConfig(onboardingConfig, position)
                ?: onboardingConfig.screens?.get(position)
        updateButton(button1, screen?.command1)
        updateButton(button2, screen?.command2)
        updateButton(button3, screen?.command3)
        if (screen?.command1?.action == ActionType.SUBSCRIBE.name && PaywallService.getInstance().isSubscriptionTerminated) {
            button1.text = "Resubscribe"
        }
    }

    private fun handleSubStatus(position: Int) {
        val screen =
            eventListener?.getScreenConfig(onboardingConfig, position)
                ?: onboardingConfig.screens?.get(position)
        if (PaywallService.getInstance().isPremiumUser && screen?.onlyForNonSubscriber == true) {
            dismiss()
        }
    }

    private fun updateBackgroundImage(position: Int) {
        val screen =
            eventListener?.getScreenConfig(onboardingConfig, position)
                ?: onboardingConfig.screens?.get(position)

        screen?.backgroundImage?.let {
            var resString = it
            if (isPhoneLandscape()) {
                resString = it.replace("phone", "tablet")
            }
            val resID = resources.getIdentifier(resString, "drawable", context?.packageName)
            backgroundImage!!.setImageResource(resID)
        }
    }

    private fun updateButton(
        button: Button,
        command: Command?,
    ) {
        if (command != null && !TextUtils.isEmpty(command.text)) {
            // If use is Signed In we want to show "Sign in as someone else" otherwise default to
            // regular sign in text "Already a Subscriber? Sign In"
            val text =
                if (command.loggedInText != null && PaywallService.getInstance().isWpUserLoggedIn) {
                    command.loggedInText
                } else if (command.subscriberText != null && PaywallService.getInstance().isSubActive) {
                    command.subscriberText
                } else {
                    command.text
                }
            button.text = Html.fromHtml(text)
            button.tag = command.action
            when (command.style) {
                CommandStyle.TEXT.name -> {
                    button.background = null
                    if (CommandColor.WHITE.name == command.color) {
                        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                    }
                }
                else -> {
                    button.setBackgroundResource(R.drawable.gdpr_button)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }
            button.visibility = View.VISIBLE
        } else {
            button.visibility = View.INVISIBLE
        }
    }

    private fun performAction(v: View) {
        val consumed = eventListener?.onClick(onboardingConfig.id, v, activity) ?: false
        if (!consumed) {
            when (v?.tag) {
                ActionType.NEXT.name -> {
                    val count = viewPager.adapter?.count ?: 0
                    if (count - 1 == viewPager.currentItem) {
                        dismiss()
                    } else {
                        viewPager.currentItem += 1
                    }
                }
                ActionType.SKIP.name -> {
                    onboardingService.setShownFlag(true)
                    dismiss()
                }
                ActionType.SUBSCRIBE.name -> {
                    if (activity?.isFinishing == false) {
                        (activity as MainActivity).showPaywallDialog(
                            PaywallConstants.ONBOARDING,
                            PaywallConstants.WallType.ONBOARDING_PAYWALL,
                        )
                        onboardingService.setShownFlag(true)
                    }
                }
                ActionType.LOGIN.name -> {
                    if (activity?.isFinishing == false) {
                        PaywallService.getConnector().showSignInScreen(
                            activity?.supportFragmentManager,
                            AuthIntentBuilder().build(),
                            null,
                            PaywallConstants.WallType.ONBOARDING_PAYWALL,
                            false,
                            null
                        )
                        onboardingService.setShownFlag(true)
                    }
                }
            }
        }
    }

    private fun initializeConfigs(context: Context) {
        if (context is OnboardingProvider) {
            val initializer = context.getOnboardingInitializer()
            onboardingService = initializer.getOnboardingService()
            eventListener = initializer.getOnboardingEventListener()
            onboardingConfig = onboardingService.getOnboardingConfig()
                ?: throw IllegalArgumentException(
                    "onboardingConfig is null! Please check onboarding_config.json.",
                )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        initializeConfigs(activity)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initializeConfigs(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view =
            if (isPhoneLandscape()) {
                inflater.inflate(
                    R.layout.fragment_base_onboarding_variant_phone_land,
                    container,
                    false,
                )
            } else {
                inflater.inflate(R.layout.fragment_base_onboarding_variant, container, false)
            }

        button1 = view.findViewById(R.id.onboarding_button1)
        button2 = view.findViewById(R.id.onboarding_button2)
        button3 = view.findViewById(R.id.onboarding_button3)
        button1.setOnClickListener(actionListener)
        button2.setOnClickListener(actionListener)
        button3.setOnClickListener(actionListener)
        viewPager = view.findViewById(R.id.onboarding_screens_viewpager)
        viewPager.adapter =
            OnboardingViewPagerAdapter(
                onboardingConfig,
                eventListener,
                childFragmentManager,
            )
        viewPager.addOnPageChangeListener(pageListener)

        view.findViewById<TabLayout>(R.id.onboarding_screens_indicator).setupWithViewPager(
            viewPager,
        )

        backgroundImage = view.findViewById(R.id.onboarding_background_image)
        updateBackgroundImage(position = 0)

        updateButtons(position = 0)

        PaywallService.getConnector().iapSubStatus.observe(this, {
            updateButtons(viewPager.currentItem)
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        handleSubStatus(viewPager.currentItem)
        updateButtons(viewPager.currentItem)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            try {
                // Note: found this interesting way to handle orientation. Didn't find a clear one.
                val ft = fragmentManager?.beginTransaction()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ft?.setReorderingAllowed(false)
                }
                ft?.detach(this)?.attach(this)?.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isPhoneLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            UIUtil.isPhone(
                context,
            )

    interface OnboardingEventListener {
        fun onClick(
            featureId: String?,
            v: View,
            activity: FragmentActivity?,
        ): Boolean

        fun getScreenConfig(
            onboardingConfig: OnboardingConfig,
            position: Int,
        ): Screen?
    }
}
