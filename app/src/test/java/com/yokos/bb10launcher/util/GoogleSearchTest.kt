package com.yokos.bb10launcher.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSearchTest {
    @Test
    fun `query is trimmed and encoded`() {
        assertEquals("https://www.google.com/search?q=weather+in+K%C3%B6ln", GoogleSearch.url("  weather in Köln "))
        assertEquals("https://www.google.com/search?q=c%2B%2B+%26+kotlin%3F", GoogleSearch.url("c++ & kotlin?"))
    }
}
