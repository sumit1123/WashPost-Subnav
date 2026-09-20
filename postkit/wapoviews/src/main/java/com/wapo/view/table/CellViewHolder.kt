package com.wapo.view.table

import android.text.SpannableString
import android.text.Spanned
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.ViewUtil.findActivity
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.R

class CellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val textView: TextView = itemView.findViewById(R.id.table_cell_text)

    fun bind(position: Int, table: StaticTable, tableView: TableView) {
        when (table.getCellType(position)) {
            StaticTable.CellType.NORMAL -> renderCell(tableView.textStyleNormal, table, position)
            StaticTable.CellType.ROW_HEADER -> renderCell(tableView.textStyleRowHeader, table, position)
            StaticTable.CellType.COLUMN_HEADER -> renderCell(tableView.textStyleColumnHeader, table, position)
        }

        val metrics = DisplayMetrics()
        val activity = itemView.findActivity()
        if (activity != null) {
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            textView.maxWidth = (metrics.widthPixels * 0.8).toInt()
        }
    }

    private fun renderCell(textStyle: Int, table: StaticTable, position: Int) {
        val text = SpannableString(table[position].toString())
        text.setSpan(
            WpTextAppearanceSpan(itemView.context, textStyle),
            0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = text
    }
}
