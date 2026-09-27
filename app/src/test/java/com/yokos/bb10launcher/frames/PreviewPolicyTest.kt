package com.yokos.bb10launcher.frames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPolicyTest {
    @Test
    fun `screenshots are scaled down to the preview width keeping aspect ratio`() {
        assertEquals(360 to 800, PreviewPolicy.targetSize(1080, 2400))
        assertEquals(360 to 180, PreviewPolicy.targetSize(2400, 1200))
        assertEquals(300 to 500, PreviewPolicy.targetSize(300, 500))
        assertEquals(0 to 0, PreviewPolicy.targetSize(0, 500))
    }

    @Test
    fun `an all black picture is blank and is not saved`() {
        val black = 0xFF000000.toInt()
        val nearBlack = 0xFF050505.toInt()
        val white = 0xFFFFFFFF.toInt()
        assertTrue(PreviewPolicy.isBlank(IntArray(100) { black }))
        assertTrue(PreviewPolicy.isBlank(IntArray(100) { nearBlack }))
        assertTrue(PreviewPolicy.isBlank(IntArray(0)))
        assertFalse(PreviewPolicy.isBlank(IntArray(100) { if (it == 50) white else black }))
    }
}
