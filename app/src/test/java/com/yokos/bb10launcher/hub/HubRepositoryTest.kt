package com.yokos.bb10launcher.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HubRepositoryTest {
    private class FakeStore(initial: List<HubItem> = emptyList()) : HubStore {
        val rows = initial.associateBy { it.id }.toMutableMap()
        override fun loadAll() = rows.values.toList()
        override fun upsert(items: Collection<HubItem>) = items.forEach { rows[it.id] = it }
        override fun delete(ids: Collection<Long>) = ids.forEach { rows.remove(it) }
    }

    private class FakeController : HubController {
        val calls = mutableListOf<String>()
        override fun open(key: String): Boolean { calls += "open:$key"; return true }
        override fun dismiss(key: String) { calls += "dismiss:$key" }
        override fun snooze(key: String, durationMillis: Long) { calls += "snooze:$key:$durationMillis" }
        override fun reply(key: String, text: String): Boolean { calls += "reply:$key:$text"; return true }
    }

    private val openedApps = mutableListOf<String>()
    private var now = 1_000_000_000L

    private fun repo(store: FakeStore = FakeStore(), maxItems: Int = 100, retentionMillis: Long = Long.MAX_VALUE / 2) =
        HubRepository(store, openApp = { openedApps += it; true }, clock = { now }, maxItems = maxItems, retentionMillis = retentionMillis)

    private fun posted(key: String, time: Long, text: String = "text $key", pkg: String = "com.chat", title: String = "t$key") =
        PostedNotification(key, pkg, pkg.substringAfterLast('.'), title, text, time, HubCategory.Message, hasReply = true)

    private fun HubRepository.item(key: String) = state.value.items.first { it.key == key }

    @Test
    fun `connecting records live notifications newest first`() {
        val hub = repo()
        hub.onConnected(listOf(posted("a", 1), posted("b", 3), posted("c", 2)))
        val state = hub.state.value
        assertTrue(state.connected)
        assertEquals(listOf("b", "c", "a"), state.items.map { it.key })
        assertTrue(state.items.all { it.active && !it.read })
    }

    @Test
    fun `removed notifications stay in the history`() {
        val hub = repo()
        hub.onPosted(posted("a", 1))
        hub.onRemoved("a")
        val item = hub.item("a")
        assertFalse(item.active)
        assertFalse(item.canReply)
        assertEquals(1, hub.state.value.items.size)
    }

    @Test
    fun `each new message in a conversation is its own entry`() {
        val hub = repo()
        hub.onPosted(posted("chat", 1, text = "hi"))
        hub.onPosted(posted("chat", 2, text = "are you there?"))
        val items = hub.state.value.items
        assertEquals(listOf("are you there?", "hi"), items.map { it.text })
        assertEquals(listOf(true, false), items.map { it.active })
    }

    @Test
    fun `a repost with the same text does not duplicate or mark unread`() {
        val hub = repo()
        hub.onPosted(posted("a", 1))
        hub.markRead(listOf(hub.item("a").id))
        hub.onRemoved("a")
        hub.onPosted(posted("a", 5))
        val items = hub.state.value.items
        assertEquals(1, items.size)
        assertTrue(items.single().active)
        assertTrue(items.single().read)
    }

    @Test
    fun `history survives a restart and is inactive until the listener reconnects`() {
        val store = FakeStore()
        val first = repo(store)
        first.onPosted(posted("a", 1))
        first.onPosted(posted("b", 2))
        first.markRead(listOf(first.item("a").id))

        val second = repo(store)
        assertEquals(listOf("b", "a"), second.state.value.items.map { it.key })
        assertTrue(second.state.value.items.none { it.active })
        assertTrue(second.item("a").read)

        second.onConnected(listOf(posted("b", 2)))
        assertTrue(second.item("b").active)
        assertFalse(second.item("a").active)
        assertEquals(2, second.state.value.items.size)

        second.onPosted(posted("c", 3))
        assertTrue(second.item("c").id > second.item("b").id)
    }

    @Test
    fun `open uses the live notification, or the app once it is gone`() {
        val hub = repo()
        val controller = FakeController()
        hub.controller = controller
        hub.onPosted(posted("a", 1, pkg = "com.mail"))
        hub.onPosted(posted("b", 2, pkg = "com.chat"))
        hub.onRemoved("b")

        assertTrue(hub.open(hub.item("a").id))
        assertTrue(hub.open(hub.item("b").id))

        assertEquals(listOf("open:a"), controller.calls)
        assertEquals(listOf("com.chat"), openedApps)
        assertTrue(hub.state.value.items.all { it.read })
    }

    @Test
    fun `delete removes the entry and dismisses it only if still live`() {
        val store = FakeStore()
        val hub = repo(store)
        val controller = FakeController()
        hub.controller = controller
        hub.onPosted(posted("a", 1))
        hub.onPosted(posted("b", 2))
        hub.onRemoved("b")

        hub.delete(hub.item("a").id)
        hub.delete(hub.item("b").id)

        assertEquals(listOf("dismiss:a"), controller.calls)
        assertTrue(hub.state.value.items.isEmpty())
        assertTrue(store.rows.isEmpty())
    }

    @Test
    fun `snooze and reply need a live notification`() {
        val hub = repo()
        val controller = FakeController()
        hub.controller = controller
        hub.onPosted(posted("a", 1))
        hub.onPosted(posted("b", 2))
        hub.onRemoved("b")

        assertTrue(hub.reply(hub.item("a").id, "hi"))
        assertFalse(hub.reply(hub.item("b").id, "hi"))
        hub.snooze(hub.item("b").id, 60_000)
        hub.snooze(hub.item("a").id, 60_000)

        assertEquals(listOf("reply:a:hi", "snooze:a:60000"), controller.calls)
        assertEquals(listOf("b"), hub.state.value.items.map { it.key })
    }

    @Test
    fun `clear history keeps live notifications`() {
        val hub = repo()
        hub.onPosted(posted("a", 1, pkg = "com.mail"))
        hub.onPosted(posted("b", 2, pkg = "com.chat"))
        hub.onPosted(posted("c", 3, pkg = "com.chat"))
        hub.onRemoved("a")
        hub.onRemoved("b")

        hub.deleteAll("com.chat")
        assertEquals(listOf("c", "a"), hub.state.value.items.map { it.key })
        hub.deleteAll(null)
        assertEquals(listOf("c"), hub.state.value.items.map { it.key })
    }

    @Test
    fun `old and excess cleared entries are pruned, live ones never`() {
        val store = FakeStore()
        val hub = repo(store, maxItems = 2, retentionMillis = 10_000)
        hub.onPosted(posted("old", now - 20_000))
        hub.onRemoved("old")
        hub.onPosted(posted("a", now - 3))
        hub.onPosted(posted("b", now - 2))
        hub.onRemoved("a")
        hub.onRemoved("b")
        hub.onPosted(posted("c", now - 1))
        hub.onPosted(posted("live-old", now - 50_000))

        // "old" expired. With a cap of 2 the oldest cleared entries go ("a", then "b"),
        // but a live notification is never pruned, however old.
        assertEquals(listOf("c", "live-old"), hub.state.value.items.map { it.key })
        assertEquals(2, store.rows.size)
    }

    @Test
    fun `mark all read only touches the filtered app`() {
        val hub = repo()
        hub.onPosted(posted("a", 1, pkg = "com.mail"))
        hub.onPosted(posted("b", 2, pkg = "com.chat"))
        hub.markAllRead("com.mail")
        assertEquals(1, hub.state.value.unreadCount)
        hub.markUnread(hub.item("a").id)
        hub.markAllRead(null)
        assertEquals(0, hub.state.value.unreadCount)
    }

    private fun entry(key: String, time: Long, pkg: String, title: String = "t$key", read: Boolean = false) = HubItem(
        id = time, key = key, packageName = pkg, appLabel = pkg.substringAfterLast('.'), title = title,
        text = "text $key", postTime = time, category = HubCategory.Message, hasReply = false, active = false, read = read,
    )

    @Test
    fun `accounts are ordered by latest activity with unread counts`() {
        val state = HubState(
            connected = true,
            items = listOf(entry("a", 9, "com.chat"), entry("b", 5, "com.mail"), entry("c", 1, "com.chat", read = true)),
        )
        val accounts = state.accounts()
        assertEquals(listOf("com.chat", "com.mail"), accounts.map { it.packageName })
        assertEquals(listOf(1, 1), accounts.map { it.unread })
        assertEquals(listOf(2, 1), accounts.map { it.total })
    }

    @Test
    fun `search matches title text and app name`() {
        val state = HubState(items = listOf(entry("a", 1, "com.mail", "Invoice"), entry("b", 2, "com.chat", "Lunch")))
        assertEquals(listOf("a"), state.search("invo").map { it.key })
        assertEquals(listOf("b"), state.search("chat").map { it.key })
        assertEquals(listOf("a", "b"), state.search("text").map { it.key })
    }

    @Test
    fun `items are grouped into today yesterday and older days`() {
        val day = 86_400_000L
        val noon = LocalDate.of(2026, 9, 26).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + 10 * 3_600_000L
        val items = listOf(entry("a", noon - 60_000, "p"), entry("b", noon - day, "p"), entry("c", noon - 3 * day, "p"))
        val groups = groupByDay(items, noon, ZoneOffset.UTC)
        assertEquals(
            listOf(DayBucket.Today, DayBucket.Yesterday, DayBucket.On(LocalDate.of(2026, 9, 23))),
            groups.map { it.first },
        )
        assertEquals(listOf("a", "b", "c"), groups.flatMap { g -> g.second.map { it.key } })
    }

    @Test
    fun `categories come from the notification, then from well-known apps`() {
        assertEquals(HubCategory.Message, HubCategory.fromNotificationCategory("email"))
        assertEquals(HubCategory.Call, HubCategory.fromNotificationCategory("missed_call"))
        assertEquals(HubCategory.Alarm, HubCategory.classify("reminder", "com.whatsapp"))
        assertEquals(HubCategory.Message, HubCategory.classify(null, "com.whatsapp"))
        assertEquals(HubCategory.Social, HubCategory.classify(null, "com.instagram.android"))
        assertEquals(HubCategory.Default, HubCategory.classify(null, "com.example"))
    }
}
