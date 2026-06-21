package com.wilddeck.app.domain

import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.DeckScore
import com.wilddeck.app.model.MiniGameAnswer
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.model.RuleResult
import com.wilddeck.app.model.SymbiosisRelationship
import com.wilddeck.app.model.TriviaQuestion
import kotlin.math.roundToInt
import kotlin.random.Random

class PlayerInventory(initialCardIds: Collection<String> = emptyList()) {
    private val ownedCardIds = initialCardIds.toMutableSet()

    fun addCard(cardId: String): Boolean = ownedCardIds.add(cardId)
    fun owns(cardId: String): Boolean = cardId in ownedCardIds
    fun allIds(): Set<String> = ownedCardIds.toSet()
    fun getAll(catalog: Map<String, AnimalCard>): List<AnimalCard> =
        ownedCardIds.mapNotNull(catalog::get)
}

class SymbiosisManager(relationships: List<SymbiosisRelationship>) {
    private val relationshipsByPair = relationships.associateBy {
        normalizedPair(it.animalAId, it.animalBId)
    }

    fun relationshipBetween(firstId: String, secondId: String): SymbiosisRelationship? =
        relationshipsByPair[normalizedPair(firstId, secondId)]

    fun score(cardIds: List<String>, catalog: Map<String, AnimalCard>): DeckScore {
        val base = cardIds.mapNotNull(catalog::get).sumOf { it.health + it.danger }
        val active = buildList {
            for (first in cardIds.indices) {
                for (second in first + 1 until cardIds.size) {
                    relationshipBetween(cardIds[first], cardIds[second])?.let(::add)
                }
            }
        }
        val multiplier = active.fold(1.0) { total, relationship ->
            total * relationship.type.multiplier
        }
        return DeckScore(base, multiplier, (base * multiplier).roundToInt(), active)
    }

    private fun normalizedPair(firstId: String, secondId: String): String =
        if (firstId <= secondId) "$firstId|$secondId" else "$secondId|$firstId"
}

class DeckManager(
    initialDecks: List<Deck>,
    private val inventory: PlayerInventory,
    private val catalog: Map<String, AnimalCard>,
    private val symbiosisManager: SymbiosisManager
) {
    private val decksById = initialDecks.associateByTo(linkedMapOf()) { it.id }

    fun allDecks(): List<Deck> = decksById.values.toList()

    fun createDeck(id: String, name: String): RuleResult {
        if (decksById.size >= MAX_DECKS) return RuleResult.Error("You can only have 5 decks.")
        decksById[id] = Deck(id, name.trim().ifBlank { "Untitled Deck" })
        return RuleResult.Success
    }

    fun deleteDeck(deckId: String): RuleResult =
        if (decksById.remove(deckId) != null) RuleResult.Success
        else RuleResult.Error("Deck not found.")

    fun renameDeck(deckId: String, name: String): RuleResult {
        val deck = decksById[deckId] ?: return RuleResult.Error("Deck not found.")
        decksById[deckId] = deck.copy(name = name.trim().ifBlank { deck.name })
        return RuleResult.Success
    }

    fun addCard(deckId: String, cardId: String): RuleResult {
        val deck = decksById[deckId] ?: return RuleResult.Error("Deck not found.")
        if (!inventory.owns(cardId)) return RuleResult.Error("You do not own this card yet.")
        if (cardId in deck.cardIds) return RuleResult.Error("This card is already in the deck.")
        if (deck.cardIds.size >= Deck.MAX_CARDS) return RuleResult.Error("Deck is full.")
        decksById[deckId] = withUpdatedScore(deck.copy(cardIds = deck.cardIds + cardId))
        return RuleResult.Success
    }

    fun removeCard(deckId: String, cardId: String): RuleResult {
        val deck = decksById[deckId] ?: return RuleResult.Error("Deck not found.")
        decksById[deckId] = withUpdatedScore(deck.copy(cardIds = deck.cardIds - cardId))
        return RuleResult.Success
    }

    fun refreshScores() {
        decksById.replaceAll { _, deck -> withUpdatedScore(deck) }
    }

    private fun withUpdatedScore(deck: Deck): Deck {
        val score = symbiosisManager.score(deck.cardIds, catalog)
        return deck.copy(score = score.finalScore, symbiosisMultiplier = score.multiplier)
    }

    companion object {
        const val MAX_DECKS = 5
    }
}

class FrameManager(
    private val frames: Map<String, CardFrame>,
    unlockedFrameIds: Collection<String>,
    selectedFrames: Map<String, String>
) {
    private val unlockedIds = unlockedFrameIds.toMutableSet().apply { add("black") }
    private val selectedByCardId = selectedFrames.toMutableMap()

    fun allFrames(): List<CardFrame> = frames.values.toList()
    fun unlockedIds(): Set<String> = unlockedIds.toSet()
    fun selectedFrames(): Map<String, String> = selectedByCardId.toMap()
    fun selectedFrameId(cardId: String): String = selectedByCardId[cardId] ?: "black"
    fun isUnlocked(frameId: String): Boolean = frameId in unlockedIds

    fun applyFrame(cardId: String, frameId: String, inventory: PlayerInventory): RuleResult {
        if (!inventory.owns(cardId)) return RuleResult.Error("You do not own this card yet.")
        if (frameId !in frames) return RuleResult.Error("Frame asset is missing.")
        if (!isUnlocked(frameId)) return RuleResult.Error("This frame is locked.")
        selectedByCardId[cardId] = frameId
        return RuleResult.Success
    }

    fun resetFrame(cardId: String, inventory: PlayerInventory): RuleResult =
        applyFrame(cardId, "black", inventory)
}

class MiniGameManager(
    private val cards: List<AnimalCard>,
    private val random: Random = Random.Default
) {
    fun startSession(excludedCardIds: Set<String> = emptySet()): MiniGameSession? {
        val availableCards = cards.filterNot { it.id in excludedCardIds }
        if (availableCards.isEmpty()) return null
        val target = availableCards[random.nextInt(availableCards.size)]
        return MiniGameSession(
            targetCard = target,
            questions = createQuestions(target).shuffled(random)
        )
    }

    fun answer(session: MiniGameSession, selectedAnswer: String): Pair<MiniGameSession, MiniGameAnswer> {
        if (session.isRewarded) {
            return session to MiniGameAnswer(false, "Card already awarded.")
        }
        val isCorrect = selectedAnswer == session.currentQuestion.correctAnswer
        val newCount = if (isCorrect) {
            session.matchCount + 1
        } else {
            (session.matchCount - 1).coerceAtLeast(0)
        }
        val won = newCount >= session.requiredMatchCount
        val nextQuestionIndex = (session.questionIndex + 1) % session.questions.size
        val updated = session.copy(
            questionIndex = nextQuestionIndex,
            matchCount = newCount,
            isRewarded = won
        )
        val answer = if (won && isCorrect) {
            MiniGameAnswer(true, "Card added to inventory.", session.targetCard)
        } else if (isCorrect) {
            MiniGameAnswer(true, "Correct! Next question.")
        } else {
            MiniGameAnswer(false, "Incorrect. Progress decreased by 1.")
        }
        return updated to answer
    }

    fun createQuestions(target: AnimalCard): List<TriviaQuestion> = listOf(
        question(target, "food", "What is a typical food for ${target.name}?", target.food) { it.food },
        question(target, "habitat", "Where does ${target.name} naturally live?", target.habitat) { it.habitat },
        question(target, "species", "Which animal group best describes ${target.name}?", target.species) { it.species },
        question(target, "health", "What health value does ${target.name} have?", target.health.toString()) { it.health.toString() },
        question(target, "danger", "What danger value does ${target.name} have?", target.danger.toString()) { it.danger.toString() },
        question(
            target,
            "rarity",
            "What is ${target.name}'s card rarity?",
            target.rarity.name.lowercase().replaceFirstChar(Char::uppercase)
        ) { it.rarity.name.lowercase().replaceFirstChar(Char::uppercase) },
        question(target, "description", "Which fact describes ${target.name}?", target.description) { it.description },
        question(
            target,
            "health_reason",
            "Which fact explains ${target.name}'s health value?",
            target.healthExplanation
        ) { it.healthExplanation },
        question(
            target,
            "danger_reason",
            "Which fact explains ${target.name}'s danger value?",
            target.dangerExplanation
        ) { it.dangerExplanation },
        question(
            target,
            "identity",
            "Which animal matches the card and facts shown above?",
            target.name
        ) { it.name }
    )

    private fun question(
        target: AnimalCard,
        idSuffix: String,
        prompt: String,
        correctAnswer: String,
        answerFor: (AnimalCard) -> String
    ): TriviaQuestion {
        val distractors = cards.asSequence()
            .filter { it.id != target.id }
            .map(answerFor)
            .filter { it != correctAnswer }
            .distinct()
            .shuffled(random)
            .take(3)
            .toList()
        return TriviaQuestion(
            id = "${target.id}_$idSuffix",
            prompt = prompt,
            options = (distractors + correctAnswer).shuffled(random),
            correctAnswer = correctAnswer
        )
    }
}
