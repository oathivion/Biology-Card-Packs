package com.wilddeck.app.model

data class AnimalCard(
    val id: String,
    val name: String,
    val species: String,
    val health: Int,
    val danger: Int,
    val imageEmoji: String,
    val description: String,
    val food: String,
    val habitat: String,
    val rarity: CardRarity,
    val healthExplanation: String,
    val dangerExplanation: String,
    val ability: AnimalAbility = AnimalAbility("field_notes", "Field Notes", AbilityType.STRIKE,
        "Uses its natural adaptations in combat."),
    val defaultFrameId: String = "black",
    val currentFrameId: String = defaultFrameId
) {
    val combatRole: CombatRole
        get() = if (health + danger < 5) CombatRole.SUPPORT else CombatRole.ATTACKER
}

enum class CombatRole { ATTACKER, SUPPORT }

enum class AbilityType {
    STRIKE, TAUNT, HEAL, SHIELD, PACK, AMBUSH, STUN, DODGE, POISON, EMPOWER
}

data class AnimalAbility(
    val id: String,
    val name: String,
    val type: AbilityType,
    val description: String,
    val power: Int = 1
)

enum class CardRarity { COMMON, UNCOMMON, RARE, LEGENDARY }

data class CardFrame(
    val id: String,
    val name: String,
    val borderStyle: String,
    val colorArgb: Long,
    val isUnlockedByDefault: Boolean,
    val rarityRequirement: CardRarity? = null,
    val effect: FrameEffect = FrameEffect.NONE,
    val type: FrameType = FrameType.BALANCED,
    val combatBonus: FrameCombatBonus = FrameCombatBonus()
)

enum class FrameEffect {
    NONE,
    LIGHT_SWEEP,
    SPARKLE,
    AURORA,
    EMBERS,
    BUBBLES,
    LEAVES,
    FROST,
    LIGHTNING,
    RAIN,
    STARFIELD
}

enum class FrameType(val displayName: String, val statMultiplier: Double) {
    BALANCED("Balanced", 1.00),
    ASSAULT("Assault", 1.08),
    GUARDIAN("Guardian", 1.06),
    SUPPORT("Support", 1.05),
    CONTROL("Control", 1.04),
    APEX("Apex", 1.10)
}

data class FrameCombatBonus(
    val healthBonus: Int = 0,
    val damageBonus: Int = 0,
    val openingShield: Int = 0,
    val damageMitigation: Int = 0,
    val attackPowerBonus: Int = 0,
    val supportPowerBonus: Int = 0,
    val description: String = "No combat bonus."
)

enum class RelationshipType(val multiplier: Double) {
    MUTUALISM(1.5),
    COMMENSALISM(1.25),
    PARASITISM(1.1)
}

data class SymbiosisRelationship(
    val animalAId: String,
    val animalBId: String,
    val type: RelationshipType,
    val description: String
)

data class Deck(
    val id: String,
    val name: String,
    val cardIds: List<String> = emptyList(),
    val score: Int = 0,
    val symbiosisMultiplier: Double = 1.0
) {
    companion object {
        const val MAX_CARDS = 5
    }
}

data class DeckScore(
    val baseScore: Int,
    val multiplier: Double,
    val finalScore: Int,
    val activeRelationships: List<SymbiosisRelationship>
)

data class MiniGameSession(
    val targetCard: AnimalCard,
    val questions: List<TriviaQuestion>,
    val questionIndex: Int = 0,
    val matchCount: Int = 0,
    val requiredMatchCount: Int = 3,
    val isRewarded: Boolean = false
) {
    val currentQuestion: TriviaQuestion
        get() = questions[questionIndex]
}

data class TriviaQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctAnswer: String,
    val difficulty: TriviaDifficulty = TriviaDifficulty.EASY
)

enum class TriviaDifficulty { EASY, MEDIUM, HARD }

data class PersistedPlayerData(
    val ownedCardIds: Set<String> = emptySet(),
    val decks: List<Deck> = emptyList(),
    val selectedFrames: Map<String, String> = emptyMap(),
    val unlockedFrameIds: Set<String> = setOf("black", "forest", "ocean"),
    val progressionPoints: Int = 1,
    val reducedMotion: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

sealed interface RuleResult {
    data object Success : RuleResult
    data class Error(val message: String) : RuleResult
}

data class MiniGameAnswer(
    val isCorrect: Boolean,
    val message: String,
    val cardAwarded: AnimalCard? = null
)

data class CombatUnit(
    val instanceId: String,
    val card: AnimalCard,
    val maxHealth: Int,
    val currentHealth: Int,
    val damage: Int,
    val multiplier: Double,
    val frame: CardFrame? = null,
    val damageMitigation: Int = 0,
    val attackPowerBonus: Int = 0,
    val supportPowerBonus: Int = 0,
    val hasActed: Boolean = false,
    val shield: Int = 0,
    val isTaunting: Boolean = false,
    val isStunned: Boolean = false
) {
    val isAlive: Boolean get() = currentHealth > 0
}

data class CombatSession(
    val round: Int,
    val playerUnits: List<CombatUnit>,
    val enemyUnits: List<CombatUnit>,
    val playerMultiplier: Double,
    val enemyMultiplier: Double,
    val pointsEarnedThisRun: Int = 0,
    val battleLog: List<String> = emptyList(),
    val isDefeated: Boolean = false
) {
    val isRoundCleared: Boolean get() = enemyUnits.none { it.isAlive }
}

data class CombatActionResult(
    val session: CombatSession,
    val message: String,
    val roundPointAwarded: Boolean = false,
    val effects: List<CombatEffect> = emptyList()
)

enum class CombatEffectType {
    ATTACK, DAMAGE, HEAL, SHIELD, EMPOWER, TAUNT, STUN, DEFEAT, ROUND_START, ROUND_CLEAR, POINT
}

data class CombatEffect(
    val type: CombatEffectType,
    val sourceId: String? = null,
    val targetId: String? = null,
    val amount: Int = 0,
    val abilityType: AbilityType? = null,
    val label: String = ""
)
