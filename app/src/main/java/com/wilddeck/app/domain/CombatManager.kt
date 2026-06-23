package com.wilddeck.app.domain

import com.wilddeck.app.model.AbilityType
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.CombatActionResult
import com.wilddeck.app.model.CombatEffect
import com.wilddeck.app.model.CombatEffectType
import com.wilddeck.app.model.CombatRole
import com.wilddeck.app.model.CombatSession
import com.wilddeck.app.model.CombatUnit
import kotlin.math.roundToInt
import kotlin.random.Random

class CombatManager(
    private val catalog: List<AnimalCard>,
    private val random: Random = Random.Default,
    frames: List<CardFrame> = emptyList()
) {
    private val framesById = frames.associateBy { it.id }

    fun startRun(playerCards: List<AnimalCard>, playerMultiplier: Double): CombatSession? {
        if (playerCards.isEmpty()) return null
        val players = playerCards.take(5).mapIndexed { index, card ->
            createUnit(card, playerMultiplier, "player_${card.id}_$index", applyFrameBonuses = true)
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

        var updated = action.session.copy(
            battleLog = (session.battleLog + action.message).takeLast(8)
        )
        if (updated.enemyUnits.none { it.isAlive }) {
            updated = updated.copy(pointsEarnedThisRun = updated.pointsEarnedThisRun + 1)
            return CombatActionResult(
                updated,
                "Round cleared! You earned 1 point.",
                true,
                action.effects + CombatEffect(CombatEffectType.ROUND_CLEAR, label = "Round cleared") +
                    CombatEffect(CombatEffectType.POINT, amount = 1, label = "+1 point")
            )
        }

        if (updated.playerUnits.filter { it.isAlive }.all { it.hasActed }) {
            updated = runEnemyTurn(updated)
        }
        val enemyEffects = buildEnemyEffects(action.session, updated)
        return CombatActionResult(updated, action.message, effects = action.effects + enemyEffects)
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
    ): ActionOutcome? {
        val target = session.enemyUnits.firstOrNull { it.instanceId == targetId && it.isAlive } ?: return null
        var damage = actor.damage + actor.attackPowerBonus
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
        val updated = session.copy(
            playerUnits = session.playerUnits.replace(actor.instanceId, acted),
            enemyUnits = session.enemyUnits.replace(target.instanceId, damagedTarget)
        )
        val effects = buildList {
            add(CombatEffect(CombatEffectType.ATTACK, actor.instanceId, target.instanceId, abilityType = actor.card.ability.type))
            add(CombatEffect(CombatEffectType.DAMAGE, actor.instanceId, target.instanceId, damage, actor.card.ability.type, "-$damage"))
            if (shieldGain > 0) add(CombatEffect(CombatEffectType.SHIELD, actor.instanceId, actor.instanceId, shieldGain, label = "+$shieldGain shield"))
            if (taunts) add(CombatEffect(CombatEffectType.TAUNT, actor.instanceId, actor.instanceId, label = "Taunt"))
            if (stuns && damagedTarget.isAlive) add(CombatEffect(CombatEffectType.STUN, actor.instanceId, target.instanceId, label = "Stunned"))
            if (!damagedTarget.isAlive) add(CombatEffect(CombatEffectType.DEFEAT, actor.instanceId, target.instanceId, label = "Defeated"))
        }
        return ActionOutcome(
            updated,
            "${actor.card.name} used ${actor.card.ability.name} on ${target.card.name} for $damage damage.",
            effects
        )
    }

    private fun supportAction(
        session: CombatSession,
        actor: CombatUnit,
        targetId: String
    ): ActionOutcome? {
        val target = session.playerUnits.firstOrNull { it.instanceId == targetId && it.isAlive } ?: return null
        val power = actor.card.ability.power + actor.supportPowerBonus
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
        val updated = session.copy(
            playerUnits = session.playerUnits
                .replace(target.instanceId, supported)
                .replace(actor.instanceId, if (actor.instanceId == target.instanceId) supported.copy(hasActed = true) else acted)
        )
        val effectType = when (actor.card.ability.type) {
            AbilityType.HEAL -> CombatEffectType.HEAL
            AbilityType.SHIELD, AbilityType.DODGE -> CombatEffectType.SHIELD
            AbilityType.EMPOWER -> CombatEffectType.EMPOWER
            else -> CombatEffectType.HEAL
        }
        return ActionOutcome(
            updated,
            "${actor.card.name} used ${actor.card.ability.name} on ${target.card.name}.",
            listOf(
                CombatEffect(effectType, actor.instanceId, target.instanceId, power, actor.card.ability.type, "+$power")
            )
        )
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

    private fun createUnit(
        card: AnimalCard,
        multiplier: Double,
        instanceId: String,
        applyFrameBonuses: Boolean = false
    ): CombatUnit {
        val frame = if (applyFrameBonuses) framesById[card.currentFrameId] else null
        val bonus = frame?.combatBonus
        val effectiveMultiplier = multiplier * (frame?.type?.statMultiplier ?: 1.0)
        val health = ((card.health * effectiveMultiplier).roundToInt() + (bonus?.healthBonus ?: 0)).coerceAtLeast(1)
        val damage = if (card.combatRole == CombatRole.SUPPORT) {
            0
        } else {
            ((card.danger * effectiveMultiplier).roundToInt() + (bonus?.damageBonus ?: 0)).coerceAtLeast(1)
        }
        val openingShield = bonus?.openingShield ?: 0
        return CombatUnit(
            instanceId = instanceId,
            card = card,
            maxHealth = health,
            currentHealth = health,
            damage = damage,
            multiplier = effectiveMultiplier,
            frame = frame,
            damageMitigation = bonus?.damageMitigation ?: 0,
            attackPowerBonus = bonus?.attackPowerBonus ?: 0,
            supportPowerBonus = bonus?.supportPowerBonus ?: 0,
            shield = openingShield
        )
    }

    private fun enemyMultiplier(round: Int): Double = 1.0 + ((round - 1) * 0.1)

    private fun takeDamage(unit: CombatUnit, amount: Int): CombatUnit {
        val absorbed = minOf(unit.shield, amount)
        val remainingDamage = (amount - absorbed - unit.damageMitigation).coerceAtLeast(0)
        return unit.copy(
            currentHealth = (unit.currentHealth - remainingDamage).coerceAtLeast(0),
            shield = unit.shield - absorbed
        )
    }

    private fun List<CombatUnit>.replace(id: String, replacement: CombatUnit): List<CombatUnit> =
        map { if (it.instanceId == id) replacement else it }

    private fun buildEnemyEffects(before: CombatSession, after: CombatSession): List<CombatEffect> =
        after.playerUnits.mapNotNull { updated ->
            val original = before.playerUnits.firstOrNull { it.instanceId == updated.instanceId } ?: return@mapNotNull null
            val loss = original.currentHealth - updated.currentHealth
            if (loss <= 0) null else CombatEffect(
                CombatEffectType.DAMAGE,
                targetId = updated.instanceId,
                amount = loss,
                label = "-$loss"
            )
        } + after.playerUnits.filterNot { it.isAlive }.mapNotNull { defeated ->
            before.playerUnits.firstOrNull { it.instanceId == defeated.instanceId && it.isAlive }?.let {
                CombatEffect(CombatEffectType.DEFEAT, targetId = defeated.instanceId, label = "Defeated")
            }
        }

    private data class ActionOutcome(
        val session: CombatSession,
        val message: String,
        val effects: List<CombatEffect>
    )
}
