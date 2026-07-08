package com.wilddeck.app.domain

import com.wilddeck.app.data.SampleData
import com.wilddeck.app.model.AbilityType
import com.wilddeck.app.model.AnimalAbility
import com.wilddeck.app.model.CombatRole
import com.wilddeck.app.model.CombatEffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CombatManagerTest {
    @Test
    fun cardsBelowFiveCombinedStats_areSupports() {
        assertEquals(CombatRole.SUPPORT, SampleData.animalCards.first { it.id == "rabbit" }.combatRole)
        assertEquals(CombatRole.SUPPORT, SampleData.animalCards.first { it.id == "clownfish" }.combatRole)
        assertEquals(CombatRole.ATTACKER, SampleData.animalCards.first { it.id == "anemone" }.combatRole)
    }

    @Test
    fun multiplierChangesBothHealthAndDamage() {
        val lion = SampleData.animalCards.first { it.id == "lion" }
        val manager = CombatManager(SampleData.animalCards, Random(1))

        val session = requireNotNull(manager.startRun(listOf(lion), 1.5))
        val unit = session.playerUnits.single()

        assertEquals(12, unit.maxHealth)
        assertEquals(14, unit.damage)
    }

    @Test
    fun equippedFramesModifyPlayerCombatStats() {
        val lion = SampleData.animalCards.first { it.id == "lion" }.copy(currentFrameId = "ember")
        val manager = CombatManager(SampleData.animalCards, random = Random(9), frames = SampleData.frames)

        val session = requireNotNull(manager.startRun(listOf(lion), 1.0))
        val unit = session.playerUnits.single()

        assertEquals("ember", unit.frame?.id)
        assertEquals(14, unit.damage)
        assertEquals(5, unit.attackPowerBonus)
        assertEquals(1.0, unit.multiplier, 0.0)
    }

    @Test
    fun oceanFrameGrantsShieldEachRound() {
        val lion = SampleData.animalCards.first { it.id == "lion" }.copy(currentFrameId = "ocean")
        val manager = CombatManager(SampleData.animalCards, random = Random(11), frames = SampleData.frames)
        val session = requireNotNull(manager.startRun(listOf(lion), 1.0))
        val unit = session.playerUnits.single()

        val next = manager.nextRound(session.copy(enemyUnits = emptyList()))
        val scaled = next.playerUnits.single()

        assertEquals(unit.maxHealth, scaled.maxHealth)
        assertEquals(unit.currentHealth, scaled.currentHealth)
        assertEquals(unit.damage, scaled.damage)
        assertEquals(unit.shield + 3, scaled.shield)
    }

    @Test
    fun enemiesGainRoundScalingFrameAfterRoundTenWithFloorRounding() {
        val lion = SampleData.animalCards.first { it.id == "lion" }
        val manager = CombatManager(SampleData.combatCards, Random(10))
        var session = requireNotNull(manager.startRun(listOf(lion), 1.0))

        repeat(10) {
            session = manager.nextRound(session.copy(enemyUnits = emptyList()))
        }

        val enemy = session.enemyUnits.first()
        val expectedMultiplier = 2.0 * 1.1

        assertEquals(11, session.round)
        assertEquals((enemy.card.health * expectedMultiplier).toInt(), enemy.maxHealth)
        assertEquals((enemy.card.danger * expectedMultiplier).toInt(), enemy.damage)
        assertEquals("Enemy Scaling Frame", enemy.frame?.name)
    }

    @Test
    fun monitorBossAppearsOnBossRoundsOnly() {
        val lion = SampleData.animalCards.first { it.id == "lion" }
        val manager = CombatManager(SampleData.combatCards, Random(12))
        var session = requireNotNull(manager.startRun(listOf(lion), 1.0))

        repeat(9) {
            session = manager.nextRound(session.copy(enemyUnits = emptyList()))
        }

        assertEquals(10, session.round)
        assertEquals(listOf(CombatManager.BOSS_CARD_ID), session.enemyUnits.map { it.card.id })
        assertTrue(session.enemyUnits.single().maxHealth >= 40)

        val next = manager.nextRound(session.copy(enemyUnits = emptyList()))
        assertTrue(next.enemyUnits.none { it.card.id == CombatManager.BOSS_CARD_ID })
    }

    @Test
    fun splashAbilityDamagesMultipleEnemies() {
        val monitor = SampleData.secretCards.first().copy(
            danger = 30,
            ability = AnimalAbility("test_splash", "Test Splash", AbilityType.SPLASH, "Splashes enemies.", 3)
        )
        val manager = CombatManager(SampleData.combatCards, Random(13))
        var session = requireNotNull(manager.startRun(listOf(monitor), 1.0))
        repeat(4) {
            session = manager.nextRound(session.copy(enemyUnits = emptyList()))
        }
        val actor = session.playerUnits.single()
        val target = session.enemyUnits.first()

        val result = manager.act(session, actor.instanceId, target.instanceId)

        assertTrue(result.session.enemyUnits.count { it.currentHealth < it.maxHealth } > 1)
        assertTrue(result.effects.count { it.type == CombatEffectType.DAMAGE } > 1)
    }

    @Test
    fun attackerCanOnlyTargetEnemy() {
        val lion = SampleData.animalCards.first { it.id == "lion" }
        val manager = CombatManager(SampleData.animalCards, Random(2))
        val session = requireNotNull(manager.startRun(listOf(lion), 1.0))
        val actor = session.playerUnits.single()

        val invalid = manager.act(session, actor.instanceId, actor.instanceId)

        assertEquals(session, invalid.session)
        assertTrue(invalid.message.contains("Attackers"))
    }

    @Test
    fun supportTargetsAllyAndUsesAbility() {
        val rabbit = SampleData.animalCards.first { it.id == "rabbit" }
        val lion = SampleData.animalCards.first { it.id == "lion" }
        val manager = CombatManager(SampleData.animalCards, Random(3))
        val session = requireNotNull(manager.startRun(listOf(rabbit, lion), 1.0))
        val support = session.playerUnits.first { it.card.id == "rabbit" }
        val ally = session.playerUnits.first { it.card.id == "lion" }

        val result = manager.act(session, support.instanceId, ally.instanceId)
        val protectedAlly = result.session.playerUnits.first { it.instanceId == ally.instanceId }

        assertEquals(3, protectedAlly.shield)
        assertTrue(result.session.playerUnits.first { it.instanceId == support.instanceId }.hasActed)
    }

    @Test
    fun clearedRoundAwardsExactlyOnePoint() {
        val crocodile = SampleData.animalCards.first { it.id == "crocodile" }
        val manager = CombatManager(SampleData.animalCards, Random(5))
        val session = requireNotNull(manager.startRun(listOf(crocodile), 10.0))
        val actor = session.playerUnits.single()
        val enemy = session.enemyUnits.single()

        val result = manager.act(session, actor.instanceId, enemy.instanceId)
        val repeated = manager.act(result.session, actor.instanceId, enemy.instanceId)

        assertTrue(result.session.isRoundCleared)
        assertTrue(result.roundPointAwarded)
        assertEquals(1, result.session.pointsEarnedThisRun)
        assertFalse(repeated.roundPointAwarded)
        assertEquals(1, repeated.session.pointsEarnedThisRun)
    }

    @Test
    fun combatActionsEmitTypedAnimationEvents() {
        val lion = SampleData.animalCards.first { it.id == "lion" }
        val manager = CombatManager(SampleData.animalCards, Random(8))
        val session = requireNotNull(manager.startRun(listOf(lion), 1.0))

        val result = manager.act(
            session,
            session.playerUnits.single().instanceId,
            session.enemyUnits.single().instanceId
        )

        assertTrue(result.effects.any { it.type == CombatEffectType.ATTACK })
        assertTrue(result.effects.any { it.type == CombatEffectType.DAMAGE })
    }
}
