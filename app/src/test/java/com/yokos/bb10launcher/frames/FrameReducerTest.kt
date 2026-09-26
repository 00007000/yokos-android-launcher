package com.yokos.bb10launcher.frames

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameReducerTest {
    private val launchable = setOf("mail", "maps", "chat", "camera", "launcher")

    private fun reduce(events: List<UsageEvent>, closed: Map<String, Long> = emptyMap(), max: Int = 8) =
        FrameReducer.reduce(events, launchable, closed, exclude = setOf("launcher"), max = max)

    @Test
    fun `latest use per app newest first`() {
        val events = listOf(
            UsageEvent("mail", 10),
            UsageEvent("maps", 20),
            UsageEvent("mail", 30),
        )
        assertEquals(listOf(Frame("mail", 30), Frame("maps", 20)), reduce(events))
    }

    @Test
    fun `non launchable and excluded packages are skipped`() {
        val events = listOf(UsageEvent("systemui", 50), UsageEvent("launcher", 40), UsageEvent("chat", 5))
        assertEquals(listOf(Frame("chat", 5)), reduce(events))
    }

    @Test
    fun `closed frames stay hidden until the app is used again`() {
        val events = listOf(UsageEvent("mail", 10), UsageEvent("maps", 30))
        assertEquals(listOf(Frame("maps", 30)), reduce(events, closed = mapOf("mail" to 15, "maps" to 25)))
    }

    @Test
    fun `at most max frames`() {
        val events = listOf("mail", "maps", "chat", "camera").mapIndexed { i, pkg -> UsageEvent(pkg, i.toLong()) }
        assertEquals(listOf("camera", "chat"), reduce(events, max = 2).map { it.packageName })
    }

    @Test
    fun `old close markers are pruned`() {
        val pruned = FrameReducer.pruneClosed(mapOf("a" to 100, "b" to 900), now = 1000, windowMillis = 500)
        assertEquals(mapOf("b" to 900L), pruned)
    }
}
