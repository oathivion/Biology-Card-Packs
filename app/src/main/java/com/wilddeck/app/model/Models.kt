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
    val defaultFrameId: String = "black",
    val currentFrameId: String = defaultFrameId
)

enum class CardRarity { COMMON, UNCOMMON, RARE, LEGENDARY }

data class CardFrame(
    val id: String,
    val name: String,
    val borderStyle: String,
    val colorArgb: Long,
    val isUnlockedByDefault: Boolean,
    val rarityRequirement: CardRarity? = null
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
    val foodOptions: List<String>,
    val matchCount: Int = 0,
    val requiredMatchCount: Int = 3,
    val isRewarded: Boolean = false
)

data class PersistedPlayerData(
    val ownedCardIds: Set<String> = emptySet(),
    val decks: List<Deck> = emptyList(),
    val selectedFrames: Map<String, String> = emptyMap(),
    val unlockedFrameIds: Set<String> = setOf("black", "forest", "ocean")
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
