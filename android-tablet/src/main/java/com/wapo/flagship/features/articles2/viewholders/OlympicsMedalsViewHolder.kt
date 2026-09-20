package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.CountryMedalItem
import com.wapo.flagship.features.articles2.models.deserialized.OlympicsMedals
import com.washingtonpost.android.databinding.ItemOlympicsMedalsBinding

class OlympicsMedalsViewHolder(
    private val binding: ItemOlympicsMedalsBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<OlympicsMedals>(binding.root) {
    override fun bind(
        item: OlympicsMedals,
        position: Int,
    ) {
        super.bind(item, position)
        binding.olympicsMedalsView.initMedalTable(
            item.title,
            item.linkText,
            mapMedals(item.data),
        ) {
            if (item.linkURL != null) {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.LinkClickEvent(item.linkURL),
                )
            }
        }
    }

    private fun mapMedals(data: MutableList<CountryMedalItem?>?): Array<com.wapo.flagship.json.CountryMedalItem>? =
        data
            ?.filterNotNull()
            ?.map {
                com.wapo.flagship.json.CountryMedalItem(
                    rank = it.rank,
                    icon = it.icon,
                    label = it.label,
                    bronze = it.bronze,
                    silver = it.silver,
                    gold = it.gold,
                )
            }?.toTypedArray()
}
