package com.wapo.flagship.features.search2.ui.decor

object Rules {
    const val MIDDLE = 1
    const val END = 2

    fun checkMiddleRule(rule: Int): Boolean = rule and MIDDLE != 0

    fun checkEndRule(rule: Int): Boolean = rule and END != 0

    fun checkAllRule(rule: Int): Boolean = rule and (MIDDLE or END) != 0
}
