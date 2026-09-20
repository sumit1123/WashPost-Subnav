package com.wapo.android.commons.util

/**
 * Produces a flattened [List] that is an interleaved merge of the input iterables.
 *
 * For example:
 *
 * ```
 * listOf(
 *   listOf(1, 2, 3, 4),
 *   listOf('a', 'b')
 * ).interleave()
 * ```
 *
 * will produce this list:
 *
 * ```
 * [1, 'a', 2, 'b', 3, 4]
 * ```
 */
fun <T> Iterable<Iterable<T>>.interleave(): List<T> {
    val result = ArrayList<T>()
    val iterators = this.map { it.iterator() }

    do {
        var hasMore = false
        for (i in iterators) {
            if (i.hasNext()) {
                result.add(i.next())
                hasMore = true
            }
        }
    } while (hasMore)

    return result
}