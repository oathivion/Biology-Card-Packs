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
import com.wilddeck.app.domain.CardLevelingManager
import com.wilddeck.app.domain.FrameManager
import com.wilddeck.app.domain.MiniGameManager
import com.wilddeck.app.domain.PlayerInventory
import com.wilddeck.app.domain.SymbiosisManager
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.CombatSession
import com.wilddeck.app.model.CombatEffect
import com.wilddeck.app.model.CombatEffectType
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
    private val catalog = SampleData.combatCards.associateBy { it.id }
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
    private val levelingManager = CardLevelingManager(loaded.cardProgress)
    private val miniGameManager = MiniGameManager(SampleData.animalCards)
    private val combatManager = CombatManager(SampleData.combatCards, frames = SampleData.frames)
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

    fun card(cardId: String?): AnimalCard? = cardId?.let(catalog::get)?.withProgress()?.withSelectedFrame()

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
        val selectedFrameIds = frameManager.selectedFrames()
        val cards = selectedDeckCards
            .ifEmpty { inventory.getAll(catalog).take(5) }
            .map { it.withProgress() }
            .map { it.withBattleFrame(selectedFrameIds[it.id]) }
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
        var message = result.message
        var effects = result.effects
        if (result.roundPointAwarded) {
            progressionPoints += 1
            val baseXp = CardLevelingManager.roundExperience(result.session.round)
            val multipliers = current.playerUnits.associate { unit ->
                unit.card.id to (unit.frame?.xpMultiplier ?: 1.0)
            }
            val leveling = levelingManager.addExperience(
                current.playerUnits.map { it.card.id },
                baseXp,
                multipliers
            )
            val xpAwards = multipliers.values.map { (baseXp * it).toInt() }.ifEmpty { listOf(baseXp) }
            val xpLabel = if (xpAwards.minOrNull() == xpAwards.maxOrNull()) {
                "+${xpAwards.first()} XP"
            } else {
                "+${xpAwards.minOrNull()}-${xpAwards.maxOrNull()} XP"
            }
            effects = result.effects + CombatEffect(
                CombatEffectType.XP_GAIN,
                amount = xpAwards.maxOrNull() ?: baseXp,
                label = xpLabel
            )
            if (
                current.round == 30 &&
                current.enemyUnits.any { it.card.id == CombatManager.BOSS_CARD_ID } &&
                inventory.addCard(CombatManager.BOSS_CARD_ID)
            ) {
                effects = effects + CombatEffect(
                    CombatEffectType.CARD_UNLOCK,
                    sourceId = CombatManager.BOSS_CARD_ID,
                    label = "Card unlocked"
                )
            }
            val xpText = if (multipliers.values.any { it > 1.0 }) {
                "$baseXp base XP, doubled by equipped Evolution Frames"
            } else {
                "$baseXp XP"
            }
            message = if (leveling.totalLevelsGained > 0) {
                "${result.message} Cards gained $xpText. ${leveling.totalLevelsGained} level up!"
            } else {
                "${result.message} Cards gained $xpText."
            }
        }
        combatEffectSequence += 1
        publish(combat = result.session, effects = effects, message = message)
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
        "temp_dev" -> 1
        "desert" -> 20
        "arctic" -> 30
        "gold" -> 50
        "radiant", "bubble", "canopy" -> 20
        "starlight", "ember", "glacier", "monsoon" -> 30
        "aurora", "storm" -> 40
        "cosmic" -> 50
        "evolution" -> 60
        "leviathan", "kraken", "shellback", "ray_current", "echowave" -> 40
        "rainforest", "dart_frog", "silverback", "polar_night" -> 40
        "tuskguard", "snowprint", "mirage", "venomtail" -> 40
        else -> 10
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
            cardProgress = levelingManager.allProgress(),
            progressionPoints = progressionPoints,
            reducedMotion = reducedMotion,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled
        )
        dataStore.save(persisted)
        uiState = WildDeckUiState(
            catalog = visibleCatalog().map { it.withProgress().withSelectedFrame() },
            ownedCards = inventory.getAll(catalog).map { it.withProgress().withSelectedFrame() },
            decks = deckManager.allDecks(),
            frames = frameManager.allFrames(),
            unlockedFrameIds = frameManager.unlockedIds(),
            selectedFrames = frameManager.selectedFrames(),
            miniGameSession = session?.copy(targetCard = session.targetCard.withProgress().withSelectedFrame()),
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

    private fun AnimalCard.withProgress(): AnimalCard =
        levelingManager.applyProgress(this)

    private fun AnimalCard.withBattleFrame(knownSelectedFrameId: String? = null): AnimalCard {
        val selectedFrameId = knownSelectedFrameId ?: frameManager.selectedFrameId(id)
        val battleFrameId = if (frameManager.isUnlocked(selectedFrameId)) selectedFrameId else defaultFrameId
        return copy(currentFrameId = battleFrameId)
    }

    private fun visibleCatalog(): List<AnimalCard> =
        SampleData.animalCards + SampleData.secretCards.filter { inventory.owns(it.id) }

    private fun formatMultiplier(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    companion object {
        const val MINI_GAME_COST = 1
    }
}
