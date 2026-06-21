package com.wilddeck.app.domain

import com.wilddeck.app.model.AbilityType
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CombatActionResult
import com.wilddeck.app.model.CombatRole
import com.wilddeck.app.model.CombatSession
import com.wilddeck.app.model.CombatUnit
import kotlin.math.roundToInt
import kotlin.random.Random

class CombatManager(
    private val catalog: List<AnimalCard>,
    private val random: Random = Random.Default
) {
    fun startRun(playerCards: List<AnimalCard>, playerMultiplier: Double): CombatSession? {
        if (playerCards.isEmpty()) return null
        val players = playerCards.take(5).mapIndexed { index, card ->
            createUnit(card, playerMultiplier, "player_${card.id}_$index")
        }
        return CombatSession(
            round = 1,
            playerUnits = players,
            enemyUnits = createEnemies(1),
            playerMultiplier = playerMultiplier,
            enemyMultiplier = enemyMultiplier(1),
            battleLog = listOf("Round 1 begins. Drag a creature to its target.")
        )
    }

    fun act(session: CombatSession, actorId: String, targetId: String): CombatActionResult {
        if (session.isDefeated || session.isRoundCleared) {
            return CombatActionResult(session, "This round is already over.")
        }
        val actor = session.playerUnits.firstOrNull { it.instanceId == actorId && it.isAlive }
            ?: return CombatActionResult(session, "That creature cannot act.")
        if (actor.hasActed) return CombatActionResult(session, "${actor.card.name} already acted.")

        val action = if (actor.card.combatRole == CombatRole.SUPPORT) {
            supportAction(session, actor, targetId)
        } else {
            attackAction(session, actor, targetId)
        } ?: return CombatActionResult(
            session,
            if (actor.card.combatRole == CombatRole.SUPPORT) {
                "Support creatures must be dragged onto a living ally."
            } else {
                "Attackers must be dragged onto a living enemy."
            }
        )

        var updated = action.first.copy(
            battleLog = (session.battleLog + action.second).takeLast(8)
        )
        if (updated.enemyUnits.none { it.isAlive }) {
            updated = updated.copy(pointsEarnedThisRun = updated.pointsEarnedThisRun + 1)
            return CombatActionResult(updated, "Round cleared! You earned 1 point.", true)
        }

        if (updated.playerUnits.filter { it.isAlive }.all { it.hasActed }) {
            updated = runEnemyTurn(updated)
        }
        return CombatActionResult(updated, action.second)
    }

    fun nextRound(session: CombatSession): CombatSession {
        require(session.isRoundCleared) { "The current round is not cleared." }
        val nextRound = session.round + 1
        val refreshedPlayers = session.playerUnits.filter { it.isAlive }.map {
            it.copy(
                currentHealth = (it.currentHealth + 1).coerceAtMost(it.maxHealth),
                hasActed = false,
                isTaunting = false,
                isStunned = false
            )
        }
        return session.copy(
            round = nextRound,
            playerUnits = refreshedPlayers,
            enemyUnits = createEnemies(nextRound),
            enemyMultiplier = enemyMultiplier(nextRound),
            battleLog = (session.battleLog + "Round $nextRound begins. Survivors recover 1 health.").takeLast(8),
            isDefeated = false
        )
    }

    private fun attackAction(
        session: CombatSession,
        actor: CombatUnit,
        targetId: String
    ): Pair<CombatSession, String>? {
        val target = session.enemyUnits.firstOrNull { it.instanceId == targetId && it.isAlive } ?: return null
        var damage = actor.damage
        var shieldGain = 0
        var taunts = false
        var stuns = false
        when (actor.card.ability.type) {
            AbilityType.PACK ->
                damage += session.playerUnits.count { it.isAlive && it.instanceId != actor.instanceId } *
                    actor.card.ability.power
            AbilityType.AMBUSH ->
                if (target.currentHealth == target.maxHealth) damage += actor.card.ability.power
            AbilityType.STRIKE ->
                if (actor.card.id != "shark" || target.currentHealth < target.maxHealth) {
                    damage += actor.card.ability.power
                }
            AbilityType.TAUNT -> {
                damage += actor.card.ability.power
                shieldGain = actor.card.ability.power
                taunts = true
            }
            AbilityType.SHIELD -> shieldGain = actor.card.ability.power
            AbilityType.STUN -> stuns = true
            else -> Unit
        }
        val damagedTarget = takeDamage(target, damage).copy(isStunned = stuns && target.currentHealth > damage)
        val acted = actor.copy(
            hasActed = true,
            shield = actor.shield + shieldGain,
            isTaunting = taunts
        )
        return session.copy(
            playerUnits = session.playerUnits.replace(actor.instanceId, acted),
            enemyUnits = session.enemyUnits.replace(target.instanceId, damagedTarget)
        ) to "${actor.card.name} used ${actor.card.ability.name} on ${target.card.name} for $damage damage."
    }

    private fun supportAction(
        session: CombatSession,
        actor: CombatUnit,
        targetId: String
    ): Pair<CombatSession, String>? {
        val target = session.playerUnits.firstOrNull { it.instanceId == targetId && it.isAlive } ?: return null
        val power = actor.card.ability.power
        val supported = when (actor.card.ability.type) {
            AbilityType.HEAL -> target.copy(
                currentHealth = (target.currentHealth + power).coerceAtMost(target.maxHealth)
            )
            AbilityType.SHIELD, AbilityType.DODGE -> target.copy(shield = target.shield + power)
            AbilityType.EMPOWER -> target.copy(
                maxHealth = target.maxHealth + power,
                currentHealth = target.currentHealth + power,
                damage = target.damage + power
            )
            else -> target.copy(currentHealth = (target.currentHealth + power).coerceAtMost(target.maxHealth))
        }
        val acted = actor.copy(hasActed = true)
        return session.copy(
            playerUnits = session.playerUnits
                .replace(target.instanceId, supported)
                .replace(actor.instanceId, if (actor.instanceId == target.instanceId) supported.copy(hasActed = true) else acted)
        ) to "${actor.card.name} used ${actor.card.ability.name} on ${target.card.name}."
    }

    private fun runEnemyTurn(session: CombatSession): CombatSession {
        var players = session.playerUnits
        var enemies = session.enemyUnits
        val log = session.battleLog.toMutableList()
        enemies.filter { it.isAlive }.forEach { enemy ->
            val currentEnemy = enemies.first { it.instanceId == enemy.instanceId }
            if (currentEnemy.isStunned) {
                enemies = enemies.replace(currentEnemy.instanceId, currentEnemy.copy(isStunned = false))
                log += "${currentEnemy.card.name} was stunned and missed its turn."
            } else {
                val livingPlayers = players.filter { it.isAlive }
                if (livingPlayers.isNotEmpty()) {
                    val taunts = livingPlayers.filter { it.isTaunting }
                    val target = (taunts.ifEmpty { livingPlayers })[random.nextInt(taunts.ifEmpty { livingPlayers }.size)]
                    val damaged = takeDamage(target, currentEnemy.damage)
                    players = players.replace(target.instanceId, damaged)
                    log += "${currentEnemy.card.name} hit ${target.card.name} for ${currentEnemy.damage}."
                }
            }
        }
        players = players.map { it.copy(hasActed = false, isTaunting = false) }
        return session.copy(
            playerUnits = players,
            enemyUnits = enemies,
            battleLog = log.takeLast(8),
            isDefeated = players.none { it.isAlive }
        )
    }

    private fun createEnemies(round: Int): List<CombatUnit> {
        val attackers = catalog.filter { it.combatRole == CombatRole.ATTACKER }
        val count = (1 + (round - 1) / 2).coerceAtMost(3)
        val multiplier = enemyMultiplier(round)
        return attackers.shuffled(random).take(count).mapIndexed { index, card ->
            createUnit(card, multiplier, "enemy_${round}_${card.id}_$index")
        }
    }

    private fun createUnit(card: AnimalCard, multiplier: Double, instanceId: String): CombatUnit {
        val health = (card.health * multiplier).roundToInt().coerceAtLeast(1)
        val damage = if (card.combatRole == CombatRole.SUPPORT) {
            0
        } else {
            (card.danger * multiplier).roundToInt().coerceAtLeast(1)
        }
        return CombatUnit(instanceId, card, health, health, damage, multiplier)
    }

    private fun enemyMultiplier(round: Int): Double = 1.0 + ((round - 1) * 0.1)

    private fun takeDamage(unit: CombatUnit, amount: Int): CombatUnit {
        val absorbed = minOf(unit.shield, amount)
        val remainingDamage = amount - absorbed
        return unit.copy(
            currentHealth = (unit.currentHealth - remainingDamage).coerceAtLeast(0),
            shield = unit.shield - absorbed
        )
    }

    private fun List<CombatUnit>.replace(id: String, replacement: CombatUnit): List<CombatUnit> =
        map { if (it.instanceId == id) replacement else it }
}
