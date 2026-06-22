package com.wilddeck.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.wilddeck.app.data.PlayerDataStore
import com.wilddeck.app.data.SampleData
import com.wilddeck.app.domain.DeckManager
import com.wilddeck.app.domain.CombatManager
import com.wilddeck.app.domain.FrameManager
import com.wilddeck.app.domain.MiniGameManager
import com.wilddeck.app.domain.PlayerInventory
import com.wilddeck.app.domain.SymbiosisManager
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.CombatSession
import com.wilddeck.app.model.CombatEffect
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.model.PersistedPlayerData
import com.wilddeck.app.model.RuleResult
import com.wilddeck.app.model.SymbiosisRelationship
import java.util.UUID

data class WildDeckUiState(
    val catalog: List<AnimalCard> = emptyList(),
    val ownedCards: List<AnimalCard> = emptyList(),
    val decks: List<Deck> = emptyList(),
    val frames: List<CardFrame> = emptyList(),
    val unlockedFrameIds: Set<String> = emptySet(),
    val selectedFrames: Map<String, String> = emptyMap(),
    val miniGameSession: MiniGameSession? = null,
    val miniGameFeedback: String? = null,
    val combatSession: CombatSession? = null,
    val combatEffects: List<CombatEffect> = emptyList(),
    val combatEffectSequence: Long = 0,
    val progressionPoints: Int = 0,
    val reducedMotion: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val message: String? = null
)

class WildDeckViewModel(application: Application) : AndroidViewModel(application) {
    private val catalog = SampleData.animalCards.associateBy { it.id }
    private val dataStore = PlayerDataStore(application)
    private val loaded = dataStore.load()
    private val inventory = PlayerInventory(loaded.ownedCardIds)
    private val symbiosisManager = SymbiosisManager(SampleData.relationships)
    private val deckManager = DeckManager(loaded.decks, inventory, catalog, symbiosisManager)
    private val frameManager = FrameManager(
        SampleData.frames.associateBy { it.id },
        loaded.unlockedFrameIds,
        loaded.selectedFrames
    )
    private val miniGameManager = MiniGameManager(SampleData.animalCards)
    private val combatManager = CombatManager(SampleData.animalCards)
    private var previousMiniGameCardId: String? = null
    private var progressionPoints = loaded.progressionPoints
    private var reducedMotion = loaded.reducedMotion
    private var soundEnabled = loaded.soundEnabled
    private var hapticsEnabled = loaded.hapticsEnabled
    private var combatEffectSequence = 0L

    var uiState by mutableStateOf(WildDeckUiState())
        private set

    init {
        deckManager.refreshScores()
        publish()
    }

    fun card(cardId: String?): AnimalCard? = cardId?.let(catalog::get)?.withSelectedFrame()

    fun relationshipsFor(cardIds: List<String>): List<SymbiosisRelationship> =
        symbiosisManager.score(cardIds, catalog).activeRelationships

    fun startMiniGame() {
        if (progressionPoints < MINI_GAME_COST) {
            showMessage("You need $MINI_GAME_COST point to enter Animal Trivia. Earn points in Wild Run.")
            return
        }
        val excludedIds = inventory.allIds() + listOfNotNull(previousMiniGameCardId)
        val session = miniGameManager.startSession(excludedIds) ?: run {
            showMessage("No new animal is available. You may already own every card.")
            return
        }
        previousMiniGameCardId = session.targetCard.id
        progressionPoints -= MINI_GAME_COST
        publish(session = session, gameFeedback = null, message = null)
    }

    fun answerTrivia(answerText: String) {
        val session = uiState.miniGameSession ?: run {
            showMessage("No trivia question is available.")
            return
        }
        val (updated, answer) = miniGameManager.answer(session, answerText)
        answer.cardAwarded?.let { inventory.addCard(it.id) }
        publish(session = updated, gameFeedback = answer.message)
    }

    fun startCombat(deckId: String?) {
        val selectedDeckCards = deckManager.allDecks()
            .firstOrNull { it.id == deckId && it.cardIds.isNotEmpty() }
            ?.cardIds
            ?.mapNotNull(catalog::get)
            .orEmpty()
        val cards = selectedDeckCards.ifEmpty { inventory.getAll(catalog).take(5) }
        if (cards.isEmpty()) {
            showMessage("Earn or unlock a creature before starting combat.")
            return
        }
        val multiplier = symbiosisManager.score(cards.map { it.id }, catalog).multiplier
        val combat = combatManager.startRun(cards, multiplier)
        publish(combat = combat, message = "Combat started with a x${formatMultiplier(multiplier)} stat multiplier.")
    }

    fun performCombatAction(actorId: String, targetId: String) {
        val current = uiState.combatSession ?: return
        val result = combatManager.act(current, actorId, targetId)
        if (result.roundPointAwarded) progressionPoints += 1
        combatEffectSequence += 1
        publish(combat = result.session, effects = result.effects, message = result.message)
    }

    fun nextCombatRound() {
        val current = uiState.combatSession ?: return
        if (!current.isRoundCleared) return
        combatEffectSequence += 1
        publish(
            combat = combatManager.nextRound(current),
            effects = listOf(com.wilddeck.app.model.CombatEffect(
                com.wilddeck.app.model.CombatEffectType.ROUND_START,
                label = "Round ${current.round + 1}"
            )),
            message = "Round ${current.round + 1} begins."
        )
    }

    fun endCombatRun() {
        publish(combat = null, effects = emptyList(), message = "Run ended. Progression points are saved.")
    }

    fun setReducedMotion(enabled: Boolean) {
        reducedMotion = enabled
        publish(message = if (enabled) "Reduced motion enabled." else "Full motion enabled.")
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        publish()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
        publish()
    }

    fun unlockFrame(frameId: String) {
        val frame = SampleData.frames.firstOrNull { it.id == frameId }
            ?: return showMessage("Frame asset is missing.")
        if (frameManager.isUnlocked(frameId)) return showMessage("This frame is already unlocked.")
        val cost = frameUnlockCost(frameId)
        if (progressionPoints < cost) return showMessage("You need $cost points to unlock ${frame.name}.")
        when (val result = frameManager.unlock(frameId)) {
            RuleResult.Success -> {
                progressionPoints -= cost
                publish(message = "${frame.name} unlocked for $cost points.")
            }
            is RuleResult.Error -> showMessage(result.message)
        }
    }

    fun frameUnlockCost(frameId: String): Int = when (frameId) {
        "desert" -> 2
        "arctic" -> 3
        "gold" -> 5
        "radiant", "bubble", "canopy" -> 2
        "starlight", "ember", "glacier", "monsoon" -> 3
        "aurora", "storm" -> 4
        "cosmic" -> 5
        else -> 1
    }

    fun createDeck(name: String) {
        handle(deckManager.createDeck(UUID.randomUUID().toString(), name))
    }

    fun renameDeck(deckId: String, name: String) {
        handle(deckManager.renameDeck(deckId, name))
    }

    fun deleteDeck(deckId: String) {
        handle(deckManager.deleteDeck(deckId))
    }

    fun addCardToDeck(deckId: String, cardId: String) {
        handle(deckManager.addCard(deckId, cardId))
    }

    fun removeCardFromDeck(deckId: String, cardId: String) {
        handle(deckManager.removeCard(deckId, cardId))
    }

    fun applyFrame(cardId: String, frameId: String) {
        val result = frameManager.applyFrame(cardId, frameId, inventory)
        handle(result, successMessage = "Frame applied.")
    }

    fun resetFrame(cardId: String) {
        val result = frameManager.resetFrame(cardId, inventory)
        handle(result, successMessage = "Frame applied.")
    }

    fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun handle(result: RuleResult, successMessage: String? = null) {
        when (result) {
            RuleResult.Success -> publish(message = successMessage)
            is RuleResult.Error -> publish(message = result.message)
        }
    }

    private fun publish(
        session: MiniGameSession? = uiState.miniGameSession,
        gameFeedback: String? = uiState.miniGameFeedback,
        combat: CombatSession? = uiState.combatSession,
        effects: List<CombatEffect> = uiState.combatEffects,
        message: String? = uiState.message
    ) {
        val persisted = PersistedPlayerData(
            ownedCardIds = inventory.allIds(),
            decks = deckManager.allDecks(),
            selectedFrames = frameManager.selectedFrames(),
            unlockedFrameIds = frameManager.unlockedIds(),
            progressionPoints = progressionPoints,
            reducedMotion = reducedMotion,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled
        )
        dataStore.save(persisted)
        uiState = WildDeckUiState(
            catalog = SampleData.animalCards.map { it.withSelectedFrame() },
            ownedCards = inventory.getAll(catalog).map { it.withSelectedFrame() },
            decks = deckManager.allDecks(),
            frames = frameManager.allFrames(),
            unlockedFrameIds = frameManager.unlockedIds(),
            selectedFrames = frameManager.selectedFrames(),
            miniGameSession = session?.copy(targetCard = session.targetCard.withSelectedFrame()),
            miniGameFeedback = gameFeedback,
            combatSession = combat,
            combatEffects = effects,
            combatEffectSequence = combatEffectSequence,
            progressionPoints = progressionPoints,
            reducedMotion = reducedMotion,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled,
            message = message
        )
    }

    private fun AnimalCard.withSelectedFrame(): AnimalCard =
        copy(currentFrameId = frameManager.selectedFrameId(id))

    private fun formatMultiplier(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    companion object {
        const val MINI_GAME_COST = 1
    }
}
