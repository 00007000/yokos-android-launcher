package com.yokos.bb10launcher.frames

/** An app coming to the foreground (or leaving it) at [time]. */
data class UsageEvent(val packageName: String, val time: Long)

/** A recent-app card on the Active Frames page. */
data class Frame(val packageName: String, val lastUsed: Long)

/** Turns raw usage events into BB10 Active Frames. Pure, so it is unit-tested on the JVM. */
object FrameReducer {
    /** BB10 showed at most eight Active Frames. */
    const val MAX_FRAMES = 8

    /**
     * The most recently used launchable apps, newest first. An app the user closed stays hidden
     * until it is used again after being closed.
     */
    fun reduce(
        events: List<UsageEvent>,
        launchable: Set<String>,
        closed: Map<String, Long>,
        exclude: Set<String>,
        max: Int = MAX_FRAMES,
    ): List<Frame> {
        val latest = HashMap<String, Long>()
        for (event in events) {
            if (event.packageName !in launchable || event.packageName in exclude) continue
            latest.merge(event.packageName, event.time, ::maxOf)
        }
        return latest
            .filter { (pkg, time) -> closed[pkg]?.let { time > it } ?: true }
            .map { (pkg, time) -> Frame(pkg, time) }
            .sortedByDescending { it.lastUsed }
            .take(max)
    }

    /** Drops close markers older than the usage window; they can no longer hide anything. */
    fun pruneClosed(closed: Map<String, Long>, now: Long, windowMillis: Long): Map<String, Long> =
        closed.filterValues { it >= now - windowMillis }
}
