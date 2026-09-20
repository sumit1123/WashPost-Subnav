package com.wapo.flagship.features.mypost

import android.content.Intent
import android.os.Bundle
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.util.Logger
import com.amazon.aps.ads.util.d
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver
import com.wapo.flagship.FusionActivity
import com.wapo.flagship.features.audio.AudioActivity
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.mypost.fragments.MyPostRegwall
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.print.ArchivesFragment
import com.wapo.flagship.features.sections.SectionFrontsFragment
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel
import com.wapo.flagship.features.settings.SettingsActivity
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavEvent
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.sdk.iterable.IterablePlugin
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityMyPostBinding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class MyPostActivity : FusionActivity(), AudioActivity, IterablePlugin.IterableActivity {

    @Inject
    lateinit var adService: AdService

    override fun hasAudioPlayerSupportInThisScreen(): Boolean = true

    private lateinit var binding: ActivityMyPostBinding

    private val sectionNavViewModel: SectionNavViewModel by viewModels()
    private val sectionTrackingViewModel: SectionTrackingViewModel by viewModels()
    private var count = 0
    private var key: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        onBackPressedDispatcher.addCallback(this) {
            onSupportNavigateUp()
        }
        binding.settings.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }
        binding.regwallContainer.setContent {
            AndroidClassicTheme {
                MyPostRegwall {
                    Measurement.setNavigationBehavior(NavigationBehavior.MY_POST)
                    PaywallService.getConnector().showSignInScreen(
                        supportFragmentManager,
                        AuthIntentBuilder()
                            .addEntryPoint(AuthEntryPoint.MY_POST)
                            .build(),
                        null,
                        PaywallConstants.WallType.SETTINGS_PAYWALL,
                        false,
                        null
                    )
                }
            }
        }
        observePageEngagement()
        observeSaveOrRemoveRecipe()
        observeSectionNavViewModelEvents()
        observeBannerMessage()
        observeBannerEvents()
        updateRegWallVisibility(shouldTrackEvent = false)

        iterablePlugin = IterablePlugin(this, iterableActivityViewModel).also {
            lifecycle.addObserver(it)
        }
    }

    override fun onResume() {
        super.onResume()
        updateRegWallVisibility(shouldTrackEvent = true)
    }

    private fun updateRegWallVisibility(shouldTrackEvent: Boolean = true) {
        PaywallService.getInstance()?.let {
            val isVisible = !it.isWpUserLoggedIn
            if (shouldTrackEvent && isVisible) {
                Measurement.trackMyPostProfileInteraction()
            }

            binding.apply {
                regwallContainer.isVisible = isVisible
                toolbarTitle.text =
                    if (isVisible) "" else getString(com.washingtonpost.android.save.R.string.my_post)
                toolbar.setNavigationContentDescription(R.string.back_to_main)

                navHostFragment.importantForAccessibility = if (isVisible) {
                    IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                } else {
                    IMPORTANT_FOR_ACCESSIBILITY_YES
                }
            }
        }
    }

    private fun observeBannerMessage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                iterableActivityViewModel.getBannerFlowForPlacement(IamMessageType.MY_POST_BANNER)
                    .collect { banner ->
                        myPost2ViewModel.updateBannerMessage(banner)
                        myPost2ViewModel.onPreviewSectionUpdate()
                    }
            }
        }
    }

    /**
     * This activity will listen to events fired from the Global CTA Banner in order to launch the correct Intent.
     */
    private fun observeBannerEvents() {
        globalBannerViewModel.bannerEvent.observe(this) { bannerEvent ->
            when (bannerEvent) {
                is BannerEvent.BannerClicked -> {
                    val banner = bannerEvent.iamMessageType?.let { type ->
                        val type = IamMessageType.entries.find { it.type == type }
                        type?.let {
                            iterableActivityViewModel.getBannerForPlacement(type)
                        }
                    } ?: bannerEvent.message
                    banner?.let {
                        iterablePlugin.executeBannerAction(
                            it,
                            IamMessageType.BANNER.type
                        )
                    }
                }

                is BannerEvent.BannerDismissed -> {
                    bannerEvent.message.let {
                        iterablePlugin.dismissBanner(it, null)
                    }
                }

                is BannerEvent.ImpressionEvent -> {
                    iterablePlugin.handleBannerLifecycleEvent(event = bannerEvent.event)
                }
                else -> {

                }
            }
        }
    }

    private fun observePageEngagement() {
        lifecycle.addObserver(
            PageEngagementLifecycleObserver(
                pageName = Measurement.PAGE_FRONT_MY_POST,
                tabName = Measurement.APP_SECTION_MY_POST,
                contentType = Measurement.CONTENT_TYPE_FRONT
            )
        )
    }

    private fun observeSectionNavViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionNavViewModel.sectionNavEvent.collect { event ->
                    when (event) {
                        is SectionNavEvent.OpenSection -> {
                            event.navType?.let { nav ->
                                sectionTrackingViewModel.setNavigationBehavior(nav)
                            }

                            when (event.menuSection.type) {
                                MenuSection.WEB_TYPE -> DeepLinksProcessor.process(event.menuSection.bundleName)
                                else -> openSection(event.menuSection)
                            }
                        }

                        SectionNavEvent.OpenPrint -> {
                            openPrint()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun openSection(menuSection: MenuSection) {
        // If section can be opened in ribbon, open it there
        if (!openSectionInRibbon(menuSection.databaseId)) {
            // Otherwise open section in a new fragment stack
            openSectionAsFragment(menuSection)
        }
    }

    /**
     * Opens Print on the stack, not as a Tab.
     */
    private fun openPrint() {
        val fragment = ArchivesFragment()
        val childKey = getNextKey()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_host_fragment, fragment, childKey)
            .addToBackStack(childKey)
            .commit()
    }

    private fun openSectionInRibbon(sectionId: String): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(getCurrentKey())
        if (fragment is SectionFrontsFragment) {
            return fragment.loadSection(sectionId)
        }
        return false
    }

    private fun openSectionAsFragment(menuSection: MenuSection) {

        DeepLinksProcessor.processAsync(
            "washpost:///section?url=https://www.washingtonpost.com${menuSection.bundleName}",
            this,
            scope = lifecycleScope,
        )

        sectionNavViewModel.setSectionTitle()
        sectionNavViewModel.updateRecentSections(menuSection)
    }

    private fun getNextKey(): String = key + (count + 1).toString()

    private fun getCurrentKey(): String =
        if (count > 1) {
            key + count.toString()
        } else {
            key
        }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun checkConnectivity() {
        if (!ReachabilityUtil.isConnected(this)) {
            notifyNetworkProblem(binding.coordinator, false)
        }
    }

    override fun getPersoPodcastViewModel(): PersonalizedPodcastViewModel? {
        return null
    }

    override fun getAppSection(): String? {
        TODO("Not yet implemented")
    }

    override fun getCustomizedSections(): List<Section?>? {
        TODO("Not yet implemented")
    }

    override fun getPersistentPlayerFrame(): FrameLayout? {
        return binding.persistentPlayerFrame
    }
}