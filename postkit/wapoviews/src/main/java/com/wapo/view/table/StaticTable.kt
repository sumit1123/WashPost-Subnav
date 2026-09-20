package com.wapo.view.table

class StaticTable {

    enum class CellType { NORMAL, ROW_HEADER, COLUMN_HEADER }


    private val data: MutableList<List<Any>> = mutableListOf()

    var hasRowHeader = false

    var hasColumnHeader = false

    val rowsCount: Int
        get() = data.size

    val columnsCount: Int
        get() = if (data.isEmpty()) 0 else {
            data[0].size
        }

    val itemsCount: Int
        get() = columnsCount * rowsCount

    fun addRow(cells: List<Any>): StaticTable {
        data.add(cells)
        return this
    }

    operator fun get(i: Int): Any {
        val row = getRowForIndex(i)
        val column = getColumnForIndex(i)
        return data[row][column]
    }

    fun getCellType(position: Int): CellType {
        if (hasRowHeader && getRowForIndex(position) == 0) return CellType.ROW_HEADER
        if (hasColumnHeader && getColumnForIndex(position) == 0) return CellType.COLUMN_HEADER
        return CellType.NORMAL
    }

    fun getColumnForIndex(position: Int): Int = position / rowsCount

    fun getRowForIndex(position: Int): Int = position % rowsCount


}