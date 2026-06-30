package com.wilddeck.app.data

import com.wilddeck.app.model.FrameEffect
import com.wilddeck.app.model.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameEffectsTest {
    @Test
    fun animatedFrames_haveDifferentAnimatedEffects() {
        val animatedFrames = SampleData.frames.filter { it.effect != FrameEffect.NONE }

        assertEquals(24, animatedFrames.size)
        assertEquals(24, animatedFrames.map { it.effect }.toSet().size)
        assertTrue(animatedFrames.none { it.isUnlockedByDefault })
        assertTrue(animatedFrames.all { it.type != FrameType.BALANCED })
        assertTrue(animatedFrames.all { it.combatBonus.description != "No combat bonus." })
    }

    @Test
    fun evolutionFrame_providesDoubleXp() {
        val xpFrames = SampleData.frames.filter { it.xpMultiplier > 1.0 }

        assertEquals(listOf("evolution"), xpFrames.map { it.id })
        assertEquals(2.0, xpFrames.single().xpMultiplier, 0.0)
    }
}
