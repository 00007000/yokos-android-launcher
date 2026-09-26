package com.yokos.bb10launcher.apps

/** Pure ordering, paging and search rules for the app grid. */
object AppOrdering {
    const val COLUMNS = 4
    const val ROWS = 6
    const val PAGE_SIZE = COLUMNS * ROWS

    /**
     * Puts items in the user's saved order. Items the saved order doesn't mention (new installs, or
     * everything before the first rearrange) follow, sorted by label.
     */
    fun <T> order(
        items: List<T>,
        savedOrder: List<String>,
        key: (T) -> String,
        label: (T) -> String,
    ): List<T> {
        val byKey = items.associateBy(key)
        val placed = LinkedHashSet(savedOrder).mapNotNull { byKey[it] }
        val placedKeys = placed.mapTo(HashSet(), key)
        val rest = items
            .filter { key(it) !in placedKeys }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, label))
        return placed + rest
    }

    /** Moves the element at [from] to [to], shifting the ones in between. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || from == to) return list
        val result = list.toMutableList()
        val item = result.removeAt(from)
        result.add(to.coerceIn(0, result.size), item)
        return result
    }

    /** Splits the grid into pages; there is always at least one page. */
    fun <T> pages(list: List<T>, pageSize: Int = PAGE_SIZE): List<List<T>> =
        if (list.isEmpty()) listOf(emptyList()) else list.chunked(pageSize)

    /** Case-insensitive search: label prefixes first, then word starts, then anything containing it. */
    fun <T> search(items: List<T>, query: String, label: (T) -> String): List<T> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        fun rank(item: T): Int {
            val text = label(item)
            return when {
                text.startsWith(q, ignoreCase = true) -> 0
                text.split(' ', '-', '_').any { it.startsWith(q, ignoreCase = true) } -> 1
                text.contains(q, ignoreCase = true) -> 2
                else -> -1
            }
        }
        return items
            .map { it to rank(it) }
            .filter { it.second >= 0 }
            .sortedWith(compareBy<Pair<T, Int>> { it.second }.thenBy(String.CASE_INSENSITIVE_ORDER) { label(it.first) })
            .map { it.first }
    }
}
