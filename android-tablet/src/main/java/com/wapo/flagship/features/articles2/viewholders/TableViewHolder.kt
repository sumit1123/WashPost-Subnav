package com.wapo.flagship.features.articles2.viewholders

import android.text.Html
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.TableItem
import com.wapo.view.table.StaticTable
import com.washingtonpost.android.databinding.FragmentArticleTableBinding

class TableViewHolder(
    private val binding: FragmentArticleTableBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<TableItem>(
        binding.root,
    ) {
    override fun bind(
        item: TableItem,
        position: Int,
    ) {
        super.bind(item, position)
        val tableView = binding.table

        tableView.setHasFixedSize(true)
        val staticTable = getStaticTable(item)
        tableView.setTable(staticTable)
    }

    private fun getStaticTable(item: TableItem): StaticTable {
        val table = StaticTable()
        table.hasColumnHeader = true
        item.header
            .map { Html.fromHtml(it.content) }
            .let {
                if (it.isNotEmpty()) {
                    table.addRow(it)
                    table.hasRowHeader = true
                }
            }

        item.row.forEach { entityRow ->
            entityRow
                .map { Html.fromHtml(it.content) }
                .let { table.addRow(it) }
        }
        return table
    }
}
