package com.wapo.flagship.features.search2.model

data class QueryFilter(
    var query: String,
    var queryId: String? = null,
    var filters: MutableMap<String, String?> = mutableMapOf(),
    val count: Int = LOAD_COUNT,
    var offset: Int = 0,
) {
    var sortBy: String?
        get() = filters[SORTBY_KEY] ?: SORT_DEFAULT
        set(value) {
            filters[SORTBY_KEY] = value
        }

    var date: String?
        get() = filters[DATE_KEY] ?: DATE_DEFUALT
        set(value) {
            filters[DATE_KEY] = value
        }

    var section: String?
        get() = filters[SECTION_KEY]
        set(value) {
            filters[SECTION_KEY] = value
        }

    var author: String?
        get() = filters[AUTHOR_KEY]
        set(value) {
            filters[AUTHOR_KEY] = value
        }

    fun setFilter(
        key: String,
        value: String?,
    ) {
        filters[key] = value
    }

    fun resetFilters() {
        filters.clear()
        offset = 0
    }

    fun copyFilters(other: QueryFilter) {
        this.filters.clear()
        other.filters.forEach {
            this.filters[it.key] = it.value
        }
    }

    fun isFilterSame(other: QueryFilter): Boolean {
        if (this.query != other.query) {
            return false
        }
        if (other.filters.count() != this.filters.count()) {
            return false
        }
        other.filters.forEach {
            if (!this.filters.contains(it.key)) {
                return false
            }
            if (this.filters[it.key] != it.value) {
                return false
            }
        }
        return true
    }

    fun hasRecipeQueryOrFilters(): Boolean {
        val nonNullFilters = filters.filter { it.value?.isNotEmpty() == true }
        return query.isNotEmpty() || nonNullFilters.isNotEmpty()
    }

    fun toQueryParamsMap(): Map<String, String> =
        mutableMapOf<String, String>().apply {
            put(QUERY_KEY, query)
            put(COUNT_KEY, count.toString())
            put(OFFSET_KEY, offset.toString())
            // Temporarily set sortby to relevancy for Search AB Test
            sortBy?.let { put(SORTBY_KEY, "relevancy") }
            date?.let { put(DATE_KEY, it) }
            section?.let { put(SECTION_KEY, it) }
            author?.let { put(AUTHOR_KEY, it) }
        }

    fun toRecipeQueryParamsMap(): Map<String, String> =
        mutableMapOf<String, String>().apply {
            if (query.isNotEmpty()) {
                put(QUERY_KEY, query)
            } else {
                put(QUERY_KEY, "")
            }
            put(COUNT_KEY, count.toString())
            put(OFFSET_KEY, offset.toString())
            filters
                .filter {
                    it.key != DATE_KEY &&
                        it.key != SORTBY_KEY &&
                        it.key != AUTHOR_KEY &&
                        it.key != SECTION_KEY &&
                        !it.value.isNullOrEmpty()
                }.forEach { entry ->
                    entry.value?.let {
                        put(entry.key, it)
                    }
                }
        }

    fun toElectionQueryParamsMap(): Map<String, String> =
        mutableMapOf<String, String>().apply {
            put(QUERY_KEY, query)
            put(COUNT_KEY, count.toString())
            put(OFFSET_KEY, offset.toString())
            put(SECTION_KEY, "elections/results/2024")
            sortBy?.let { put(SORTBY_KEY, "relevancy") }
        }

    companion object {
        private const val LOAD_COUNT = 100
        private const val SORTBY_KEY = "sort"
        private const val DATE_KEY = "date"
        private const val SECTION_KEY = "section"
        private const val AUTHOR_KEY = "author"

        const val QUERY_KEY = "query"
        const val COUNT_KEY = "count"
        const val OFFSET_KEY = "offset"

        private const val SORT_DEFAULT = "date"
        private const val DATE_DEFUALT = "all"
    }
}
