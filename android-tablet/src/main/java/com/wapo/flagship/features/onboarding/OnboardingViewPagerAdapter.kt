package com.wapo.flagship.features.onboarding

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.lang.IllegalArgumentException

class OnboardingViewPagerAdapter(
    val onboardingConfig: OnboardingConfig,
    private val eventListener: BaseOnboardingFragment.OnboardingEventListener?,
    fm: FragmentManager,
) : FragmentStatePagerAdapter(
        fm,
    ) {
    private val filteredScreens: List<Screen>? = onboardingConfig.screens
    private val NUM_OF_SCREENS: Int = filteredScreens?.size ?: 0
    private var registeredFragments = SparseArray<Fragment>()

    override fun instantiateItem(
        container: ViewGroup,
        position: Int,
    ): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any,
    ) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    override fun getItem(position: Int): Fragment {
        val screenConfig =
            eventListener?.getScreenConfig(onboardingConfig, position)
                ?: filteredScreens?.get(position)
                ?: throw IllegalArgumentException(
                    "screenConfig is null! Please check onboarding_config.json.",
                )
        return OnboardingScreenFragment.newInstance(screenConfig)
    }

    override fun getCount(): Int = NUM_OF_SCREENS

    fun getRegisteredFragment(position: Int): Fragment? = registeredFragments.get(position)
}
