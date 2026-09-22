package com.wapo.flagship.features.articles2.viewholders

import android.content.Intent
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wapo.android.commons.config.ConfigManager
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.config.Section
import com.wapo.flagship.config.SiteServiceConfig
import com.wapo.Utils
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.SubNav
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.search2.ui.Search2Activity
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.databinding.ItemSubNavBinding
import com.wpds.utils.IconUtils

/**
 * Dead since article body rendering moved to articles3 -- the adapter that creates this is never
 * constructed (ArticleContentNativeViewHolder, its only construction site, is entirely commented
 * out). Kept only because Articles2ItemsRecyclerViewAdapter and MarginItemDecoration still
 * reference the type. The live implementation is articles3/views/SubNavView.
 */
class SubNavViewHolder(
    private val binding: ItemSubNavBinding,
    private val onNavigationBehaviorChanged: ((String?) -> Unit)? = null,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<SubNav>(binding.root) {
    private val iconUtils = IconUtils(FlagshipApplication.getInstance().applicationContext)

    override fun bind(
        item: SubNav,
        position: Int,
    ) {
        // The element carries only the sitemap endpoint, so there is no embed to load here:
        // fetching the endpoint is what fills the strip.
        item.url?.let { siteMapUrl ->
            binding.root.findActivityOfType<Articles2Activity>()?.fetchElectionChildren(siteMapUrl)
        }

        updateSubNavElectionConfig()
    }

    fun getItemView(): View = binding.root

    private fun updateSubNavElectionConfig() {
        ConfigManager
            .instance()
            .getConfigSubjectOfType(
                Constants.ConfigType.ELECTION_SUB_NAV_CONFIG,
            ).subscribe {
                // The strip is fed by SubNavConfig now; this holder still speaks the old
                // site-service shape, so it simply renders nothing rather than crashing.
                val children = (it as? SiteServiceConfig)?.sections.orEmpty()
                children.firstOrNull()?.let { it1 -> configureSectionText(it1) }
                children.firstOrNull()?.let { it1 -> configureSearchText(it1) }
                children.firstOrNull()?.let { it1 -> configureLiveUpdatesText(it1) }
            }
    }

    private fun configureSectionText(config: Section) {
        config.icon?.let {
            val drawableId = iconUtils.getDrawableId(it)
            val drawable =
                drawableId?.let { it1 ->
                    ContextCompat.getDrawable(
                        binding.root.context,
                        it1,
                    )
                }
            drawable?.setBounds(
                0,
                0,
                binding.root.context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.sections.R.dimen.compound_label_icon_large,
                ),
                binding.root.context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.sections.R.dimen.compound_label_icon_large,
                ),
            )
            binding.electionText.setCompoundDrawables(drawable, null, null, null)
        }
        binding.electionText.text = config.sectionName
        //TODO why was this using sectionPath?
        config.fusionPath?.let { path ->
            binding.electionText.setOnClickListener {
                DeepLinksProcessor.processAsync(
                    DeepLinksProcessor.sectionPathToDeepLink(path),
                    scope = binding.electionText.findViewTreeLifecycleOwner()?.lifecycleScope
                )
                onNavigationBehaviorChanged?.invoke(config.behavior)
            }
        }
    }

    private fun configureSearchText(config: Section) {
        val searchConfig = config.sections?.firstOrNull { it.sectionSubType == SectionSubType.SEARCH.typeName }
        searchConfig?.let {
            it.icon?.let { icon ->
                val drawableId = iconUtils.getDrawableId(icon)
                binding.searchText.setCompoundDrawablesWithIntrinsicBounds(drawableId ?: 0, 0, 0, 0)
            }
            binding.searchText.text = it.sectionName
            binding.searchText.setOnClickListener {
                openSearch()
                Measurement.trackSubNavItemClick(searchConfig.behavior)
            }
        }
    }

    private fun configureLiveUpdatesText(config: Section) {
        val liveUpdatesConfig = config.sections?.firstOrNull { it.sectionSubType == SectionSubType.LUF.typeName }
        liveUpdatesConfig?.let {
            if (liveUpdatesConfig.fusionPath.isNullOrEmpty()) {
                binding.liveUpdates.visibility = View.GONE
            } else {
                addLiveUpdate(it)
                binding.liveUpdates.setOnClickListener {
                    openLiveUpdates(liveUpdatesConfig.fusionPath, liveUpdatesConfig.behavior)
                }
            }
        }
    }

    private fun openLiveUpdates(
        sectionPath: String?,
        behavior: String?,
    ) {
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(sectionPath)
                .setNavigationBehavior(behavior)
                .setSubNavOriginated(true)
                .buildIntent(binding.root.context)
        binding.root.context.startActivity(intent)
    }

    private fun addLiveUpdate(section: Section) {
        val dateLineText = SpannableStringBuilder()
        val sectionName = section.sectionName
        val displayDate = section.displayDate
        val date = Utils.toDateLong(displayDate, Utils.getDefaultDateFormat())
        val isRecent = Utils.isRecentlyUpdated(displayDate)
        val formattedPublishedDate =
            if (isRecent) {
                Utils.getAbbreviatedRelativeTime(date) ?: "" // Handle null return case
            } else {
                ""
            }
        val sectionNameSpan = SpannableString(sectionName)
        val dateSpan = SpannableString(formattedPublishedDate)
        dateSpan.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    binding.root.context,
                    com.washingtonpost.android.sections.R.color.live_update_recency_threshold,
                ),
            ),
            0,
            dateSpan.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        dateLineText.append(sectionNameSpan)
        dateLineText.append("  ")
        dateLineText.append(dateSpan)
        binding.liveUpdates.text = dateLineText
    }

    private fun openSearch() {
        val intent = Intent(binding.root.context, Search2Activity::class.java)
        intent.putExtra("type", "election")
        binding.root.context.startActivity(intent)
    }

    private enum class SectionSubType(
        val typeName: String,
    ) {
        SEARCH("search"),
        LUF("luf"),
    }
}
