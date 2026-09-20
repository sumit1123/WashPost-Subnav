package com.washingtonpost.android.wapocontent

class Priority(val group : Group, val level : Long) {

    enum class Group {
        BACKGROUND, FOREGROUND
    }

    fun toInt() : Int {
        //
        // let's convert (class, priority) to int based on class
        // background class < foreground class
        // foreground class:
        //     level2 - level1 > 0 => level2 has higher priority
        // background class:
        //     level2 - level1 < 0 => level1 has higher priority
        //
        return (level % Integer.MAX_VALUE * if (group === Group.BACKGROUND) -1 else 1).toInt()
    }
}
