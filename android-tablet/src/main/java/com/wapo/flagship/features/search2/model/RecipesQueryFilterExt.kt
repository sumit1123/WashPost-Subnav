package com.wapo.flagship.features.search2.model

/**
 * These are extension properties added to Query Filter for Recipes.
 * Keeping these in a separate class to keep QueryFilter class clean
 */

val QueryFilter.READYIN_KEY: String
    get() = "time"

val QueryFilter.COURSE_KEY: String
    get() = "course"

val QueryFilter.DIET_KEY: String
    get() = "diet"

var QueryFilter.readyIn: String?
    get() = filters[READYIN_KEY]
    set(value) {
        filters[READYIN_KEY] = value
    }

var QueryFilter.courseType: String?
    get() = filters[COURSE_KEY]
    set(value) {
        filters[COURSE_KEY] = value
    }

var QueryFilter.diet: String?
    get() = filters[DIET_KEY]
    set(value) {
        filters[DIET_KEY] = value
    }

fun QueryFilter.getRecipeQueryParams(): Map<String, String> =
    mutableMapOf<String, String>().apply {
        filters.forEach {
            it.value?.let { value ->
                put(it.key, value)
            }
        }
        put(QueryFilter.QUERY_KEY, query)
        put(QueryFilter.COUNT_KEY, count.toString())
        put(QueryFilter.OFFSET_KEY, offset.toString())
    }
