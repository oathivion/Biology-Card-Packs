package com.wilddeck.app.data

import com.wilddeck.app.model.FrameEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameEffectsTest {
    @Test
    fun tenNewFrames_haveTenDifferentAnimatedEffects() {
        val animatedFrames = SampleData.frames.filter { it.effect != FrameEffect.NONE }

        assertEquals(10, animatedFrames.size)
        assertEquals(10, animatedFrames.map { it.effect }.toSet().size)
        assertTrue(animatedFrames.none { it.isUnlockedByDefault })
    }
}
