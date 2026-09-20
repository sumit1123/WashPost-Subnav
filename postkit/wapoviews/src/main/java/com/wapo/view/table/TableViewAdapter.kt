package com.wapo.view.table

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wapo.view.R

class TableViewAdapter(val tableView: TableView) : RecyclerView.Adapter<CellViewHolder>() {

    private lateinit var table: StaticTable

    fun setTable(table: StaticTable) {
        this.table = table
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val layoutId = when (viewType) {
            StaticTable.CellType.NORMAL.ordinal -> R.layout.tableview_cell_normal
            StaticTable.CellType.ROW_HEADER.ordinal -> R.layout.tableview_cell_row_header
            StaticTable.CellType.COLUMN_HEADER.ordinal -> R.layout.tableview_cell_column_header
            else -> throw IllegalArgumentException("wrong view type $viewType")
        }
        return LayoutInflater
            .from(parent.context)
            .inflate(layoutId, parent, false)
            .run { CellViewHolder(this) }
    }

    override fun getItemViewType(position: Int): Int {
        return table.getCellType(position).ordinal
    }

    override fun getItemCount(): Int {
        return table.itemsCount
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        holder.bind(position, table, tableView)
    }
}