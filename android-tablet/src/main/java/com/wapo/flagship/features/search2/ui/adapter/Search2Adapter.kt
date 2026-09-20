package com.wapo.flagship.features.search2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wapo.adsinf.BannerAdView
import com.wapo.adsinf.databinding.AdLayoutBinding
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.AdItem
import com.wapo.flagship.features.search2.model.ArticleItem
import com.wapo.flagship.features.search2.model.AskQuestionsItem
import com.wapo.flagship.features.search2.model.ElectionItem
import com.wapo.flagship.features.search2.model.ExpandableItem
import com.wapo.flagship.features.search2.model.HeaderItem
import com.wapo.flagship.features.search2.model.LoaderItem
import com.wapo.flagship.features.search2.model.NoResult
import com.wapo.flagship.features.search2.model.PostAnswerContainerItem
import com.wapo.flagship.features.search2.model.RecipeItem
import com.wapo.flagship.features.search2.model.SearchItem
import com.wapo.flagship.features.search2.model.SearchQueryItem
import com.wapo.flagship.features.search2.model.SectionItem
import com.wapo.flagship.features.search2.model.SpacerItem
import com.wapo.flagship.features.search2.ui.viewholder.AdViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.ArticleItemViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.AskQuestionsViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.ElectionStateItemViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.ExpandableItemViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.HeaderViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.LoadingViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.NoResultViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.PostAnswersViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.QueryItemViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.RecipeItemViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.SectionItemViewHolder
import com.wapo.flagship.features.search2.ui.viewholder.SpacerViewHolder
import com.washingtonpost.android.databinding.PostAnswersContainerBinding
import com.washingtonpost.android.databinding.SearchArticleItemBinding
import com.washingtonpost.android.databinding.SearchAskQuestionsItemBinding
import com.washingtonpost.android.databinding.SearchElectionItemBinding
import com.washingtonpost.android.databinding.SearchExpandableItemBinding
import com.washingtonpost.android.databinding.SearchHeaderBinding
import com.washingtonpost.android.databinding.SearchLoaderBinding
import com.washingtonpost.android.databinding.SearchNoResultBinding
import com.washingtonpost.android.databinding.SearchQueryItemBinding
import com.washingtonpost.android.databinding.SearchRecipeItemBinding
import com.washingtonpost.android.databinding.SearchSectionItemBinding
import com.washingtonpost.android.databinding.SearchSpacerBinding

/**
 * Adapter for handling the various items in Search Recycler View
 */
class Search2Adapter(
    private val askQuestionsViewModel: AskQuestionsViewModel? = null,
    private val askThePostUIEvent: (AskThePostEvent) -> Unit,
    private val shouldSuppressAds: Boolean,
    private val askThePostViewModel: AskThePostViewModel? = null,
    private val fragmentManager: FragmentManager? = null,
    val onItemClick: (UserEvent) -> Unit,
) : ListAdapter<SearchItem, RecyclerView.ViewHolder>(DiffUtils()) {

    val persistentAdViews = mutableMapOf<String, BannerAdView>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SearchItemType.HEADER.id -> {
                val viewBinding = SearchHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.SECTION.id -> {
                val viewBinding = SearchSectionItemBinding.inflate(inflater, parent, false)
                SectionItemViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.SPACER.id -> {
                val viewBinding = SearchSpacerBinding.inflate(inflater, parent, false)
                SpacerViewHolder(viewBinding)
            }

            SearchItemType.ARTICLE.id -> {
                val viewBinding = SearchArticleItemBinding.inflate(inflater, parent, false)
                ArticleItemViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.AD.id -> {
                val viewBinding = AdLayoutBinding.inflate(inflater, parent, false)
                AdViewHolder(viewBinding, shouldSuppressAds)
            }

            SearchItemType.QUERY.id -> {
                val viewBinding = SearchQueryItemBinding.inflate(inflater, parent, false)
                QueryItemViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.EXPANDABLE.id -> {
                val viewBinding = SearchExpandableItemBinding.inflate(inflater, parent, false)
                ExpandableItemViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.NO_RESULT.id -> {
                val viewBinding = SearchNoResultBinding.inflate(inflater, parent, false)
                NoResultViewHolder(viewBinding)
            }

            SearchItemType.LOADER.id -> {
                val viewBinding = SearchLoaderBinding.inflate(inflater, parent, false)
                LoadingViewHolder(viewBinding)
            }

            SearchItemType.RECIPE.id -> {
                val viewBinding = SearchRecipeItemBinding.inflate(inflater, parent, false)
                RecipeItemViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.POST_ANSWERS.id -> {
                val viewBinding = PostAnswersContainerBinding.inflate(inflater, parent, false)
                PostAnswersViewHolder(
                    viewBinding,
                    askThePostUIEvent,
                    onItemClick,
                    askThePostViewModel,
                    fragmentManager
                )
            }

            SearchItemType.ELECTION.id -> {
                val viewBinding = SearchElectionItemBinding.inflate(inflater, parent, false)
                ElectionStateItemViewHolder(viewBinding, onItemClick)
            }

            SearchItemType.ASK_QUESTIONS.id -> {
                val viewBinding = SearchAskQuestionsItemBinding.inflate(inflater, parent, false)
                AskQuestionsViewHolder(viewBinding, askQuestionsViewModel, onItemClick)
            }

            else -> {
                val viewBinding = SearchSpacerBinding.inflate(inflater, parent, false)
                SpacerViewHolder(viewBinding)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is AdViewHolder && getItem(position) is AdItem) {
            if (shouldSuppressAds) {
                AdsUtil.setAdLayoutVisibility(adLayout = holder.binding.root, isVisible = false)
                return
            }
            val adItem = getItem(position) as AdItem
            val adKey = "${adItem.id}_$position"
            val parent = holder.binding.adView

            val adView = persistentAdViews.getOrPut(adKey) {
                BannerAdView(parent.context).apply {
                    val adConfig = AdConfig(
                        adUnitId = AdsUtil.getAdUnitId(
                            context = parent.context,
                            contentType = adItem.commercialNode,
                            adType = adItem.adPosition,
                            adKey = adItem.commercialNode,
                        ),
                        adRequestTargets = AdRequestTargets.getDefault().apply {
                            addSlotSizeParameters(adItem.adSlotType)
                            addSection("search")
                            addAdPosition(adItem.adPosition)
                        },
                        adDimensions = listOf(adItem.adDimension),
                        adSlotType = adItem.adSlotType,
                        section = "search"
                    )
                    // Set up and load ad ONCE for this slot
                    loadAd(adConfig)
                }
            }

            // Remove from old parent, if any
            (adView.parent as? ViewGroup)?.removeView(adView)
            // Add to current holder
            if (parent.indexOfChild(adView) == -1) {
                parent.removeAllViews()
                parent.addView(adView)
            }
        } else {
            (holder as? SearchViewHolder<SearchItem>)?.bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is HeaderItem -> SearchItemType.HEADER.id
            is SectionItem -> SearchItemType.SECTION.id
            is SpacerItem -> SearchItemType.SPACER.id
            is ArticleItem -> SearchItemType.ARTICLE.id
            is AdItem -> SearchItemType.AD.id
            is SearchQueryItem -> SearchItemType.QUERY.id
            is ExpandableItem -> SearchItemType.EXPANDABLE.id
            is NoResult -> SearchItemType.NO_RESULT.id
            is LoaderItem -> SearchItemType.LOADER.id
            is RecipeItem -> SearchItemType.RECIPE.id
            is PostAnswerContainerItem -> SearchItemType.POST_ANSWERS.id
            is ElectionItem -> SearchItemType.ELECTION.id
            is AskQuestionsItem -> SearchItemType.ASK_QUESTIONS.id
            else -> SearchItemType.SEARCH_ITEM.id
        }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AdViewHolder) {
            holder.unbind()
        }
    }

    fun cleanup() {
        persistentAdViews.forEach { (_, adView) ->
            adView.release()
        }
        persistentAdViews.clear()
    }

    class DiffUtils : DiffUtil.ItemCallback<SearchItem>() {
        override fun areItemsTheSame(
            oldItem: SearchItem,
            newItem: SearchItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SearchItem,
            newItem: SearchItem,
        ): Boolean = newItem.id == oldItem.id
    }

    enum class SearchItemType(
        val id: Int,
    ) {
        HEADER(0),
        SECTION(1),
        ARTICLE(2),
        SEARCH_ITEM(3),
        SPACER(4),
        QUERY(5),
        EXPANDABLE(6),
        NO_RESULT(7),
        LOADER(8),
        RECIPE(9),
        POST_ANSWERS(10),
        ELECTION(11),
        ASK_QUESTIONS(12),
        AD(13),
    }

    open class SearchViewHolder<T : SearchItem>(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        open fun bind(item: T) {
        }

        open fun unbind() {
        }
    }
}
