package com.wapo.flagship.util

import com.wapo.flagship.features.articles2.models.deserialized.Audio

class AudioUtil {
    companion object {
        // Calculate total duration of a luf article including its children and transitions
        fun calculateTotalDuration(item: Audio): Long? {
            var totalDuration = item.duration
            val currentChildren = item.children
            val currentTransitions = item.transitions

            if (!currentChildren.isNullOrEmpty()) {
                val childrenDuration = currentChildren.sumOf { it.duration ?: 0L }
                totalDuration = totalDuration?.plus(childrenDuration)
            }
            if (!currentTransitions.isNullOrEmpty()) {
                val transitionsDuration = currentTransitions.sumOf { it.duration ?: 0L }
                totalDuration = totalDuration?.plus(transitionsDuration)
            }

            totalDuration = totalDuration?.plus(currentChildren?.size?.toLong()?.times(1000) ?: 0L)
            totalDuration =
                totalDuration?.plus(currentTransitions?.size?.toLong()?.times(1000) ?: 0L)

            return totalDuration
        }
    }
}