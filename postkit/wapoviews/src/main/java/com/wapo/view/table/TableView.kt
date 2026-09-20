package com.wapo.view.table

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TableView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var textStyleNormal: Int = 0
    var textStyleRowHeader: Int = 0
    var textStyleColumnHeader: Int = 0

    init {
        adapter = TableViewAdapter(this)
    }

    fun setTable(table: StaticTable) {
        (adapter as TableViewAdapter).setTable(table)
        layoutManager = GridLayoutManager(context, table.rowsCount, GridLayoutManager.HORIZONTAL, false)
    }
}