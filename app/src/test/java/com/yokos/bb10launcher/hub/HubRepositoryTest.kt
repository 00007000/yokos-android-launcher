package com.yokos.bb10launcher.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HubRepositoryTest {
    private fun item(key: String, time: Long, pkg: String = "com.chat", title: String = "t$key") = HubItem(
        key = key,
        packageName = pkg,
        appLabel = pkg.substringAfterLast('.'),
        title = title,
        text = "text $key",
        postTime = time,
        category = HubCategory.Message,
        canReply = false,
        canOpen = true,
    )

    private class FakeController : HubController {
        val calls = mutableListOf<String>()
        override fun open(key: String): Boolean { calls += "open:$key"; return true }
        override fun dismiss(key: String) { calls += "dismiss:$key" }
        override fun snooze(key: String, durationMillis: Long) { calls += "snooze:$key:$durationMillis" }
        override fun reply(key: String, text: String): Boolean { calls += "reply:$key:$text"; return true }
    }

    @Test
    fun `connecting sorts newest first and drops stale read markers`() {
        val repo = HubRepository()
        repo.restoreRead(setOf("a", "gone"))
        repo.onConnected(listOf(item("a", 1), item("b", 3), item("c", 2)))
        val state = repo.state.value
        assertTrue(state.connected)
        assertEquals(listOf("b", "c", "a"), state.items.map { it.key })
        assertEquals(setOf("a"), state.readKeys)
        assertEquals(2, state.unreadCount)
    }

    @Test
    fun `an update with a new post time is unread again`() {
        val repo = HubRepository()
        repo.onConnected(listOf(item("a", 1)))
        repo.markRead(listOf("a"))
        repo.upsert(item("a", 1, title = "same time"))
        assertFalse(repo.state.value.isUnread(repo.state.value.items.single()))
        repo.upsert(item("a", 5))
        assertTrue(repo.state.value.isUnread(repo.state.value.items.single()))
    }

    @Test
    fun `remove forgets the item and its read marker`() {
        val repo = HubRepository()
        repo.onConnected(listOf(item("a", 1), item("b", 2)))
        repo.markRead(listOf("a"))
        repo.remove("a")
        assertEquals(listOf("b"), repo.state.value.items.map { it.key })
        assertEquals(emptySet<String>(), repo.state.value.readKeys)
    }

    @Test
    fun `mark all read only touches the filtered app`() {
        val repo = HubRepository()
        repo.onConnected(listOf(item("a", 1, "com.mail"), item("b", 2, "com.chat")))
        repo.markAllRead("com.mail")
        assertEquals(setOf("a"), repo.state.value.readKeys)
        repo.markAllRead(null)
        assertEquals(setOf("a", "b"), repo.state.value.readKeys)
    }

    @Test
    fun `actions go to the controller and update the stream`() {
        val repo = HubRepository()
        val controller = FakeController()
        repo.controller = controller
        repo.onConnected(listOf(item("a", 1), item("b", 2), item("c", 3)))

        assertTrue(repo.open("a"))
        repo.snooze("b", 60_000)
        repo.dismiss("c")
        assertTrue(repo.reply("a", "hi"))

        assertEquals(listOf("open:a", "snooze:b:60000", "dismiss:c", "reply:a:hi"), controller.calls)
        assertEquals(listOf("a"), repo.state.value.items.map { it.key })
        assertEquals(setOf("a"), repo.state.value.readKeys)
    }

    @Test
    fun `actions without a connected listener fail quietly`() {
        val repo = HubRepository()
        repo.onConnected(listOf(item("a", 1)))
        assertFalse(repo.open("a"))
        assertFalse(repo.reply("a", "hi"))
    }

    @Test
    fun `persistable read keys ignore markers for missing items once connected`() {
        val repo = HubRepository()
        repo.restoreRead(setOf("x"))
        assertEquals(setOf("x"), repo.persistableReadKeys())
        repo.onConnected(listOf(item("a", 1)))
        repo.markRead(listOf("a", "y"))
        assertEquals(setOf("a"), repo.persistableReadKeys())
    }

    @Test
    fun `accounts are ordered by latest activity with unread counts`() {
        val state = HubState(
            connected = true,
            items = listOf(item("a", 9, "com.chat"), item("b", 5, "com.mail"), item("c", 1, "com.chat")),
            readKeys = setOf("c"),
        )
        val accounts = state.accounts()
        assertEquals(listOf("com.chat", "com.mail"), accounts.map { it.packageName })
        assertEquals(listOf(1, 1), accounts.map { it.unread })
        assertEquals(listOf(2, 1), accounts.map { it.total })
    }

    @Test
    fun `search matches title text and app name`() {
        val state = HubState(items = listOf(item("a", 1, "com.mail", "Invoice"), item("b", 2, "com.chat", "Lunch")))
        assertEquals(listOf("a"), state.search("invo").map { it.key })
        assertEquals(listOf("b"), state.search("chat").map { it.key })
        assertEquals(listOf("a", "b"), state.search("text").map { it.key })
    }

    @Test
    fun `items are grouped into today yesterday and older days`() {
        val day = 86_400_000L
        val now = LocalDate.of(2026, 9, 26).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + 10 * 3_600_000L
        val items = listOf(item("a", now - 60_000), item("b", now - day), item("c", now - 3 * day))
        val groups = groupByDay(items, now, ZoneOffset.UTC)
        assertEquals(
            listOf(DayBucket.Today, DayBucket.Yesterday, DayBucket.On(LocalDate.of(2026, 9, 23))),
            groups.map { it.first },
        )
        assertEquals(listOf("a", "b", "c"), groups.flatMap { g -> g.second.map { it.key } })
    }

    @Test
    fun `notification categories map to stripe colours`() {
        assertEquals(HubCategory.Message, HubCategory.fromNotificationCategory("email"))
        assertEquals(HubCategory.Call, HubCategory.fromNotificationCategory("missed_call"))
        assertEquals(HubCategory.Alarm, HubCategory.fromNotificationCategory("reminder"))
        assertEquals(HubCategory.Default, HubCategory.fromNotificationCategory(null))
    }
}
