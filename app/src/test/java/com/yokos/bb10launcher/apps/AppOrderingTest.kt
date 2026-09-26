package com.yokos.bb10launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderingTest {
    private data class App(val key: String, val label: String)

    private val maps = App("maps", "Maps")
    private val camera = App("camera", "camera")
    private val browser = App("browser", "Browser")
    private val all = listOf(maps, camera, browser)

    private fun order(saved: List<String>) = AppOrdering.order(all, saved, App::key, App::label)

    @Test
    fun `without a saved order apps are alphabetical ignoring case`() {
        assertEquals(listOf(browser, camera, maps), order(emptyList()))
    }

    @Test
    fun `saved order comes first and new apps are appended alphabetically`() {
        assertEquals(listOf(maps, browser, camera), order(listOf("maps")))
    }

    @Test
    fun `uninstalled and duplicate keys in the saved order are ignored`() {
        assertEquals(listOf(camera, maps, browser), order(listOf("gone", "camera", "maps", "camera")))
    }

    @Test
    fun `move shifts the items in between`() {
        assertEquals(listOf("b", "c", "a", "d"), AppOrdering.move(listOf("a", "b", "c", "d"), 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), AppOrdering.move(listOf("a", "b", "c", "d"), 3, 0))
    }

    @Test
    fun `move clamps the target and ignores an unknown source`() {
        assertEquals(listOf("b", "c", "a"), AppOrdering.move(listOf("a", "b", "c"), 0, 99))
        assertEquals(listOf("a", "b"), AppOrdering.move(listOf("a", "b"), -1, 0))
    }

    @Test
    fun `pages always has at least one page`() {
        assertEquals(listOf(emptyList<Int>()), AppOrdering.pages(emptyList<Int>(), 4))
        assertEquals(listOf(listOf(1, 2), listOf(3)), AppOrdering.pages(listOf(1, 2, 3), 2))
    }

    @Test
    fun `search ranks prefixes before word starts before substrings`() {
        val apps = listOf(App("1", "Google Maps"), App("2", "Mail"), App("3", "Gmail"), App("4", "Clock"))
        val result = AppOrdering.search(apps, "ma", App::label).map { it.label }
        assertEquals(listOf("Mail", "Google Maps", "Gmail"), result)
    }

    @Test
    fun `blank search returns nothing`() {
        assertEquals(emptyList<App>(), AppOrdering.search(all, "  ", App::label))
    }
}
