package com.yokos.bb10launcher.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeekConfigTest {
    @Test
    fun `strip span follows length and position`() {
        assertEquals(100 to 450, PeekConfig(length = PeekLength.Half, position = PeekPosition.Top).span(100, 1000))
        assertEquals(325 to 450, PeekConfig(length = PeekLength.Half, position = PeekPosition.Center).span(100, 1000))
        assertEquals(550 to 450, PeekConfig(length = PeekLength.Half, position = PeekPosition.Bottom).span(100, 1000))
        assertEquals(100 to 900, PeekConfig(length = PeekLength.Full).span(100, 1000))
        assertEquals(100 to 300, PeekConfig(length = PeekLength.Third).span(100, 1000))
    }

    @Test
    fun `span never goes negative on a tiny screen`() {
        assertEquals(50 to 0, PeekConfig().span(50, 10))
    }

    @Test
    fun `progress is measured inward from the chosen edge`() {
        assertEquals(0.5f, PeekGesture.progress(200f, PeekEdge.Left, 400f), 0.0001f)
        assertEquals(0.5f, PeekGesture.progress(-200f, PeekEdge.Right, 400f), 0.0001f)
        assertEquals(0f, PeekGesture.progress(-200f, PeekEdge.Left, 400f), 0.0001f)
        assertEquals(1f, PeekGesture.progress(900f, PeekEdge.Left, 400f), 0.0001f)
        assertEquals(0f, PeekGesture.progress(50f, PeekEdge.Left, 0f), 0.0001f)
    }

    @Test
    fun `release commits past the threshold or on an inward fling`() {
        assertTrue(PeekGesture.shouldCommit(0.5f, 0f, PeekEdge.Left, flingVelocity = 1000f))
        assertFalse(PeekGesture.shouldCommit(0.2f, 0f, PeekEdge.Left, flingVelocity = 1000f))
        assertTrue(PeekGesture.shouldCommit(0.1f, 2000f, PeekEdge.Left, flingVelocity = 1000f))
        assertFalse(PeekGesture.shouldCommit(0.9f, -2000f, PeekEdge.Left, flingVelocity = 1000f))
        assertTrue(PeekGesture.shouldCommit(0.1f, -2000f, PeekEdge.Right, flingVelocity = 1000f))
    }
}
