package com.yokos.bb10launcher.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AgeTest {
    @Test
    fun `ages are bucketed into minutes hours and days`() {
        assertEquals(Age.Now, Age.of(-5_000))
        assertEquals(Age.Now, Age.of(59_999))
        assertEquals(Age.Minutes(1), Age.of(60_000))
        assertEquals(Age.Minutes(59), Age.of(59 * 60_000L + 59_999))
        assertEquals(Age.Hours(1), Age.of(60 * 60_000L))
        assertEquals(Age.Hours(23), Age.of(24 * 60 * 60_000L - 1))
        assertEquals(Age.Days(2), Age.of(2 * 24 * 60 * 60_000L))
    }
}
