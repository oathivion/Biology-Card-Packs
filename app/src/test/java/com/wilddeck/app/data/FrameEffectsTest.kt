package com.wilddeck.app.data

import com.wilddeck.app.model.FrameEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameEffectsTest {
    @Test
    fun animatedFrames_haveDifferentAnimatedEffects() {
        val animatedFrames = SampleData.frames.filter { it.effect != FrameEffect.NONE }

        assertEquals(25, animatedFrames.size)
        assertEquals(25, animatedFrames.map { it.effect }.toSet().size)
        assertTrue(animatedFrames.none { it.isUnlockedByDefault })
        assertTrue(animatedFrames.all { it.combatBonus.description != "No combat bonus." })
    }

    @Test
    fun overhauledFrames_useRunEffectsInsteadOfXpMultipliers() {
        val ocean = SampleData.frames.single { it.id == "ocean" }
        val evolution = SampleData.frames.single { it.id == "evolution" }
        val gravebound = SampleData.frames.single { it.id == "gravebound" }

        assertEquals(3, ocean.combatBonus.perRoundShield)
        assertEquals(35, evolution.combatBonus.randomGrowthChancePercent)
        assertEquals(1, evolution.combatBonus.randomGrowthAmount)
        assertEquals(0.5, gravebound.combatBonus.deathScalingPerFallen, 0.0)
    }
}
