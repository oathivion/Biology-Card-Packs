package com.wilddeck.app.domain

import com.wilddeck.app.data.SampleData
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.RuleResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class GameManagersTest {
    private val catalog = SampleData.animalCards.associateBy { it.id }
    private lateinit var inventory: PlayerInventory
    private lateinit var symbiosis: SymbiosisManager

    @Before
    fun setUp() {
        inventory = PlayerInventory()
        symbiosis = SymbiosisManager(SampleData.relationships)
    }

    @Test
    fun inventory_addDetectAndReturnCards() {
        assertTrue(inventory.addCard("lion"))
        assertFalse(inventory.addCard("lion"))
        assertTrue(inventory.owns("lion"))
        assertEquals(listOf("Lion"), inventory.getAll(catalog).map { it.name })
    }

    @Test
    fun deck_rejectsUnownedCards() {
        val manager = DeckManager(listOf(Deck("deck", "Test")), inventory, catalog, symbiosis)

        assertEquals(
            RuleResult.Error("You do not own this card yet."),
            manager.addCard("deck", "lion")
        )
    }

    @Test
    fun deck_rejectsSixthCard() {
        val ids = catalog.keys.take(6)
        ids.forEach(inventory::addCard)
        val manager = DeckManager(listOf(Deck("deck", "Test")), inventory, catalog, symbiosis)
        ids.take(5).forEach { assertEquals(RuleResult.Success, manager.addCard("deck", it)) }

        assertEquals(RuleResult.Error("Deck is full."), manager.addCard("deck", ids.last()))
        assertEquals(5, manager.allDecks().single().cardIds.size)
    }

    @Test
    fun deckManager_rejectsSixthDeck() {
        val manager = DeckManager(emptyList(), inventory, catalog, symbiosis)
        repeat(5) { assertEquals(RuleResult.Success, manager.createDeck("$it", "Deck $it")) }

        assertEquals(
            RuleResult.Error("You can only have 5 decks."),
            manager.createDeck("six", "Deck 6")
        )
    }

    @Test
    fun symbiosis_detectsRealPairInEitherOrder() {
        assertNotNull(symbiosis.relationshipBetween("clownfish", "anemone"))
        assertNotNull(symbiosis.relationshipBetween("anemone", "clownfish"))
        assertNull(symbiosis.relationshipBetween("lion", "rabbit"))
    }

    @Test
    fun score_combinesStatsAndSymbiosisMultiplier() {
        val score = symbiosis.score(listOf("clownfish", "anemone"), catalog)

        assertEquals(8, score.baseScore)
        assertEquals(1.5, score.multiplier, 0.0)
        assertEquals(12, score.finalScore)
        assertEquals(1, score.activeRelationships.size)
    }

    @Test
    fun miniGame_incorrectAnswerDoesNotAdvance() {
        val manager = MiniGameManager(SampleData.animalCards, Random(7))
        val session = requireNotNull(manager.startSession())
        val wrong = session.foodOptions.first { it != session.targetCard.food }

        val (updated, result) = manager.answer(session, wrong)

        assertFalse(result.isCorrect)
        assertEquals("Incorrect food.", result.message)
        assertEquals(0, updated.matchCount)
    }

    @Test
    fun miniGame_awardsExactlyAfterThreeCorrectAnswers() {
        val manager = MiniGameManager(SampleData.animalCards, Random(4))
        var session = requireNotNull(manager.startSession())
        repeat(2) {
            val (updated, result) = manager.answer(session, session.targetCard.food)
            session = updated
            assertNull(result.cardAwarded)
        }

        val (won, reward) = manager.answer(session, session.targetCard.food)
        val (unchanged, repeated) = manager.answer(won, won.targetCard.food)

        assertTrue(won.isRewarded)
        assertEquals(3, won.matchCount)
        assertNotNull(reward.cardAwarded)
        assertEquals(won, unchanged)
        assertNull(repeated.cardAwarded)
    }

    @Test
    fun frameManager_appliesUnlockedAndRejectsLockedFrames() {
        inventory.addCard("lion")
        val manager = FrameManager(
            SampleData.frames.associateBy { it.id },
            setOf("black", "forest"),
            emptyMap()
        )

        assertEquals(RuleResult.Success, manager.applyFrame("lion", "forest", inventory))
        assertEquals("forest", manager.selectedFrameId("lion"))
        assertEquals(
            RuleResult.Error("This frame is locked."),
            manager.applyFrame("lion", "gold", inventory)
        )
        assertEquals("forest", manager.selectedFrameId("lion"))
    }
}
