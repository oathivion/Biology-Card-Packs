package com.wilddeck.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.wilddeck.app.data.PlayerDataStore
import com.wilddeck.app.data.SampleData
import com.wilddeck.app.domain.DeckManager
import com.wilddeck.app.domain.FrameManager
import com.wilddeck.app.domain.MiniGameManager
import com.wilddeck.app.domain.PlayerInventory
import com.wilddeck.app.domain.SymbiosisManager
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
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
    private var previousMiniGameCardId: String? = null

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
        val excludedIds = inventory.allIds() + listOfNotNull(previousMiniGameCardId)
        val session = miniGameManager.startSession(excludedIds)
        session?.let { previousMiniGameCardId = it.targetCard.id }
        uiState = uiState.copy(
            miniGameSession = session,
            miniGameFeedback = null,
            message = if (session == null) {
                "No new animal is available. You may already own every card."
            } else {
                null
            }
        )
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
        message: String? = uiState.message
    ) {
        val persisted = PersistedPlayerData(
            ownedCardIds = inventory.allIds(),
            decks = deckManager.allDecks(),
            selectedFrames = frameManager.selectedFrames(),
            unlockedFrameIds = frameManager.unlockedIds()
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
            message = message
        )
    }

    private fun AnimalCard.withSelectedFrame(): AnimalCard =
        copy(currentFrameId = frameManager.selectedFrameId(id))
}
