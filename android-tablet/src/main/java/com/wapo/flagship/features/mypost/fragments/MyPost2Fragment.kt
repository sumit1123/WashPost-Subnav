package com.wapo.flagship.features.mypost.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.TEXT_ALIGNMENT_CENTER
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.onetrust.otpublishers.headless.Public.Keys.OTBroadcastServiceKeys
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.mypost.MyPostAuthorPageActivity
import com.wapo.flagship.features.mypost.openArticles
import com.wapo.flagship.features.mypost.viewmodels.MyPost2ViewModel
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wapomain.MainConstants.ACTION_GAMES
import com.washingtonpost.android.follow.activity.AuthorPageActivity
import com.washingtonpost.android.follow.fragment.MyPostFollowFragment
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.FragmentMyPost2Binding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.types.MyPostSection
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class MyPost2Fragment : Fragment() {
    private lateinit var binding: FragmentMyPost2Binding

    private val myPost2ViewModel: MyPost2ViewModel by activityViewModels()
    private val sectionsRibbonViewModel: SectionsRibbonViewModel by activityViewModels()

    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)

    private var sectionList: List<MyPostSection> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyPost2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            cgSections.children.forEach {
                it.setOnClickListener {
                    Measurement.trackMyPostToolBarNavigation(getSection(it.id), false)
                }
            }
        }

        binding.apply {
            myPost2ViewModel.allSections.observe(viewLifecycleOwner) { list ->
                if (sectionList != list) {
                    sectionList = list
                    if (list.size >= 4) {
                        val id1 = R.id.chip_my_post2_all
                        val id2 = getSectionId(list[0])
                        val id3 = getSectionId(list[1])
                        val id4 = getSectionId(list[2])
                        val id5 = getSectionId(list[3])
                        val chip1 = createChip(id1)
                        val chip2 = createChip(id2)
                        val chip3 = createChip(id3)
                        val chip4 = createChip(id4)
                        val chip5 = createChip(id5)
                        cgSections.removeAllViews()
                        cgSections.addView(chip1)
                        cgSections.addView(chip2)
                        cgSections.addView(chip3)
                        cgSections.addView(chip4)
                        cgSections.addView(chip5)
                        if (list.size > 4 && !myPost2ViewModel.purchasedArticles.value.isNullOrEmpty()) {
                            val id6 = getSectionId(list[4])
                            val chip6 = createChip(id6)
                            cgSections.addView(chip6)
                        }
                        cgSections.children.forEach {
                            setChipClickListener(it)
                        }
                        myPost2ViewModel.section.value?.let {
                            selectSection(it)
                        }
                    }
                }
            }
            cgSections.children.forEach {
                setChipClickListener(it)
            }
            cgSections.setOnCheckedChangeListener { group, checkedId ->
                group.children.forEach { child ->
                    child.setPadding(1)
                    child.textAlignment = TEXT_ALIGNMENT_CENTER
                    (child as? TextView)?.let {
                        if (it.id == checkedId) {
                            it.typeface = Typeface.DEFAULT_BOLD
                            val left = if (it.id == R.id.chip_my_post2_all) 0 else it.left
                            hsvMyPost2ScrollContainer.smoothScrollTo(left, it.top)
                        } else {
                            it.typeface = Typeface.DEFAULT
                        }
                    }
                }
                onClickSection(checkedId)
            }
        }


        if (!isDeeplinkOriginated()) {
            myPost2ViewModel.section.value?.let {
                selectSection(it)
                Measurement.trackMyPostToolBarNavigation(it, true)
            }
        }

        myPost2ViewModel.setTargetingEnabled(OneTrustHelper.isTargetingEnabled())

        observeSectionSelection()
        observeArticleItemClickEvent()
        observeViewMoreClickEvent()
        observeMoreFromAuthorClickEvent()
        observeOptionsClickEvent()
        observeViewArchiveClickEvent()
        observeTopStoriesClickEvent()
        observeSignInClickEvent()
        observeSettingsClickEvent()
        observeUpdateConsentSettingsClickEvent()
        observeBirthdayFrontPageClickEvent()
        observeOpenSectionClickEvent()
        observeFollowOptionsClickEvent()
    }

    override fun onResume() {
        super.onResume()
        handleFollowDeepLink()
        handleSectionDeeplink()
        myPost2ViewModel.handleAfterSignInAttempt()
        myPost2ViewModel.refresh()
    }

    override fun onStart() {
        super.onStart()
        // listens for consent updates to refresh reading history
        ContextCompat.registerReceiver(
            requireContext(),
            consentUpdated,
            IntentFilter(OTBroadcastServiceKeys.OT_CONSENT_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        requireContext().unregisterReceiver(consentUpdated)
        super.onStop()
    }

    private val consentUpdated = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (myPost2ViewModel.isConsentGiven()) {
                myPost2ViewModel.refreshReadingHistory()
            }
        }
    }

    private fun createChip(id: Int): Chip {
        val chip = Chip(context)
        chip.apply {
            this.id = id
            val chipDrawable =
                ChipDrawable.createFromAttributes(
                    context as Context,
                    null,
                    0,
                    R.style.MyPost_Choice_Chip,
                )
            setChipDrawable(chipDrawable)
            height = LayoutParams.WRAP_CONTENT
            width = LayoutParams.WRAP_CONTENT
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                setTextAppearance(context, R.style.MyPost_Choice_Chip_Text)
            } else {
                setTextAppearance(R.style.MyPost_Choice_Chip_Text)
            }
            text =
                when (id) {
                    R.id.chip_my_post2_all -> "  All  "
                    R.id.chip_my_post2_saved_stories -> getString(R.string.my_post_saved_stories_label)
                    R.id.chip_my_post2_interests -> getString(R.string.my_post_interests_label)
                    R.id.chip_my_post2_following -> getString(R.string.my_post_following_label)
                    R.id.chip_my_post2_reading_history -> getString(
                        R.string.my_post_reading_history_label,
                    )

                    R.id.chip_my_post2_purchased_articles -> getString(
                        R.string.my_post_purchased_articles_label,
                    )

                    else -> ""
                }
        }
        return chip
    }

    private fun setChipClickListener(view: View) {
        view.setOnClickListener {
            Measurement.trackMyPostToolBarNavigation(getSection(it.id), false)
        }
    }

    private fun selectSection(section: MyPostSection) {
        binding.cgSections.check(getSectionId(section))
    }

    private fun onClickSection(checkedId: Int) {
        getSection(checkedId)?.let {
            if (myPost2ViewModel.section.value != it) {
                myPost2ViewModel.setSection(it)
            }
        }
    }

    private fun observeSectionSelection() {
        myPost2ViewModel.section.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {
                    MyPostSection.ALL -> {
                        openPreviewFragment()
                    }

                    else -> {
                        openDetailFragment(it)
                    }
                }
            },
        )
    }

    private fun observeArticleItemClickEvent() {
        myPost2ViewModel.articleItemClickEvent.observe(viewLifecycleOwner) {
            if (it.isPreviewHeroArticle == true) {
                Measurement.trackNavigateToArticleFromReadingHistory(Measurement.MY_POST_READING_HISTORY_ARTICLE_TOP)
            } else if (isCarouselOriginated(it)) {
                Measurement.trackNavigateToArticleFromReadingHistory(Measurement.MY_POST_READING_HISTORY_CAROUSEL)
            } else {
                Measurement.trackNavigateToArticleFromReadingHistory(Measurement.MY_POST_READING_HISTORY)
            }

            val currentSection = myPost2ViewModel.section.value
                ?: MyPostSection.ALL

            openArticles(
                context,
                "My Post",
                myPostArticleItems = it.articleList,
                it.url,
                currentSection.name,
                it.section.name,
                isCarouselOriginated(it),
                it.isPreviewHeroArticle,
                getCarouselIndex(it),
                true,
                it.shouldPlayAudioArticle
            )
        }
        myPost2ViewModel.refreshReadingHistory()
    }

    /**
     * Determines if the clicked article item is in a carousel
     *
     * @return
     * true if the the current section is the "All" section AND
     * the article is in its corresponding My Post Section's carousel items list.
     * false otherwise.
     */
    private fun isCarouselOriginated(articleActionItem: ArticleActionItem): Boolean {
        val carouselIndex = getCarouselIndex(articleActionItem)
        return myPost2ViewModel.section.value == MyPostSection.ALL && carouselIndex != null && carouselIndex >= 0
    }

    /**
     * Gets the article's index in the section's carousel
     *
     * @return
     * The article's carousel index if the article is in its section's carousel items list.
     * -1 if the article is not in its section's carousel items list.
     * null if the article's section's carousel items list is null.
     */
    private fun getCarouselIndex(articleActionItem: ArticleActionItem): Int? {
        val carouselItems = myPost2ViewModel.getCarouselListBySection(articleActionItem.section)
        val carouselPosition =
            carouselItems?.indexOfFirst { carouselArticle ->
                carouselArticle.contentUrl == articleActionItem.url
            }

        return carouselPosition
    }

    private fun observeViewMoreClickEvent() {
        myPost2ViewModel.viewMoreClickEvent.observe(viewLifecycleOwner) {
            binding.cgSections.let { cg ->
                cg.check(getSectionId(it))
                myPost2ViewModel.setSection(it)
                Measurement.trackMyPostToolBarNavigation(getSection(getSectionId(it)), false)
            }
        }
    }

    private fun observeMoreFromAuthorClickEvent() {
        myPost2ViewModel.moreFromAuthorClickEvent.observe(viewLifecycleOwner) {
            val intent =
                Intent(context, MyPostAuthorPageActivity::class.java).apply {
                    putExtra(AuthorPageActivity.PARAM_AUTHOR, it)
                }
            val bundle =
                ActivityOptionsCompat
                    .makeCustomAnimation(
                        requireContext(),
                        R.anim.slide_up_400,
                        R.anim.fade_out_250,
                    ).toBundle()
            startActivity(intent, bundle)
        }
    }

    private fun observeOptionsClickEvent() {
        myPost2ViewModel.optionsClickEvent.observe(viewLifecycleOwner) {
            UtilityMenuFragment().show(childFragmentManager, UtilityMenuFragment.tag)
        }
    }

    private fun observeViewArchiveClickEvent() {
        myPost2ViewModel.viewArchiveClickEvent.observe(viewLifecycleOwner) {
            val archiveLink = getString(R.string.archive_link)
            Utils.startWebActivity(archiveLink, requireContext())
        }
    }

    private fun observeTopStoriesClickEvent() {
        myPost2ViewModel.viewTopStoriesClickEvent.observe(viewLifecycleOwner) {
            val topStoriesLink = getString(com.washingtonpost.android.R.string.uri_tab_home)
            DeepLinksProcessor.processAsync(topStoriesLink, context, scope = lifecycleScope)
        }
    }

    private fun observeSignInClickEvent() {
        myPost2ViewModel.signInClickEvent.observe(viewLifecycleOwner) {
            Measurement.trackSignInAttempt()
            PaywallService.getConnector().showSignInScreen(
                activity?.supportFragmentManager,
                AuthIntentBuilder().build(),
                null,
                PaywallConstants.WallType.SETTINGS_PAYWALL,
                false,
                null
            )
        }
    }

    private fun observeSettingsClickEvent() {
        myPost2ViewModel.settingsClickEvent.observe(viewLifecycleOwner) {
            val link = getString(com.washingtonpost.android.R.string.uri_settings_main)
            DeepLinksProcessor.processAsync(link, context, scope = lifecycleScope)
        }
    }

    private fun observeUpdateConsentSettingsClickEvent() {
        myPost2ViewModel.updateConsentSettingsClickEvent.observe(viewLifecycleOwner) {
            val context = FlagshipApplication.getInstance()
            if (OneTrustHelper.ot.isBannerShown(context) == -1) {
                val activity = activity
                if (activity is AppCompatActivity) {
                    OneTrustHelper.initSdk(activity)
                }
            } else {
                activity?.let { OneTrustHelper.ot.showPreferenceCenterUI(it) }
            }
        }
    }

    private fun observeBirthdayFrontPageClickEvent() {
        myPost2ViewModel.birthdayFrontPageClickEvent.observe(viewLifecycleOwner) {
            val link = myPost2ViewModel.config.birthdayFrontPageConfig.birthdayFrontPageUrl
            val linkWithItId = "$link/?itid=my_post_banner"
            Utils.startWebActivity(linkWithItId, context, false, false, false)
            Measurement.trackMyPostBannerClick(link, myPost2ViewModel.section.value)
        }
    }

    private fun observeOpenSectionClickEvent() {
        myPost2ViewModel.openSectionClickEvent.observe(viewLifecycleOwner) {
            context?.let {
                IntentHelper.getMainActivityIntent(it, false).apply {
                    fillIn(this, 0)
                    action = ACTION_GAMES
                    data = null
                    it.startActivity(this)
                }
            }
        }
    }

    private fun observeFollowOptionsClickEvent() {
        followViewModel.utilityMenuClickEvent.observe(viewLifecycleOwner) {
            myPost2ViewModel.handleFollowOptionsClickEvent(it)
        }
    }

    private fun handleFollowDeepLink() {
        activity?.intent?.apply {
            val followId = getStringExtra(FOLLOW_ID)
            if (!followId.isNullOrEmpty()) {
                putExtra(FOLLOW_ID, "")
                followViewModel.setDeepLinkedAuthorId(followId)
                selectSection(MyPostSection.FOLLOWING)
            }
        }
    }

    private fun handleSectionDeeplink() {
        activity?.intent?.apply {
            val section = getStringExtra(MY_POST_SECTION)
            if (!section.isNullOrEmpty()) {
                putExtra(MY_POST_SECTION, "")
                selectSection(enumValueOf(section))
                Measurement.trackMyPostToolBarNavigation(enumValueOf(section), false)
            }
        }
    }

    private fun isDeeplinkOriginated(): Boolean =
        activity?.intent?.getStringExtra(MY_POST_SECTION) != null ||
                activity?.intent?.getStringExtra(
                    FOLLOW_ID,
                ) != null

    private fun openPreviewFragment() {
        myPost2ViewModel.previousTopics = null
        val fragment = SectionsPreviewFragment()
        sectionsRibbonViewModel.removeLastViewedSection()
        childFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.slide_up_400, R.anim.fade_out_250)
            .replace(R.id.layout_my_post2_content_container, fragment)
            .commit()
    }

    private fun openDetailFragment(section: MyPostSection) {
        myPost2ViewModel.previousTopics = null
        val fragment =
            when (section) {
                MyPostSection.FOLLOWING -> MyPostFollowFragment()
                MyPostSection.TOPICS -> MyPostTopicsDetailFragment()
                else -> SectionDetailFragment()
            }
        childFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.slide_up_400, R.anim.fade_out_250)
            .replace(R.id.layout_my_post2_content_container, fragment)
            .commit()
    }

    private fun getSectionId(section: MyPostSection): Int =
        when (section) {
            MyPostSection.ALL -> R.id.chip_my_post2_all
            MyPostSection.SAVED_STORIES -> R.id.chip_my_post2_saved_stories
            MyPostSection.TOPICS -> R.id.chip_my_post2_interests
            MyPostSection.FOLLOWING -> R.id.chip_my_post2_following
            MyPostSection.READING_HISTORY -> R.id.chip_my_post2_reading_history
            MyPostSection.PURCHASE -> R.id.chip_my_post2_purchased_articles
        }

    @Nullable
    private fun getSection(
        @IdRes resId: Int,
    ): MyPostSection? =
        when (resId) {
            R.id.chip_my_post2_all -> MyPostSection.ALL
            R.id.chip_my_post2_saved_stories -> MyPostSection.SAVED_STORIES
            R.id.chip_my_post2_interests -> MyPostSection.TOPICS
            R.id.chip_my_post2_following -> MyPostSection.FOLLOWING
            R.id.chip_my_post2_reading_history -> MyPostSection.READING_HISTORY
            R.id.chip_my_post2_purchased_articles -> MyPostSection.PURCHASE
            else -> null
        }

    companion object {
        @JvmField
        val FOLLOW_ID: String = MyPostFollowFragment::class.java.simpleName + ".followId"
        val MY_POST_SECTION = "MY_POST_SECTION"
    }
}
