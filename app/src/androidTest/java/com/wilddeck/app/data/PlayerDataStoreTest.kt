package com.wilddeck.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.PersistedPlayerData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerDataStoreTest {
    @Test
    fun inventoryDecksAndFrames_roundTripThroughLocalStorage() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = PlayerDataStore(context)
        val expected = PersistedPlayerData(
            ownedCardIds = setOf("lion", "rabbit"),
            decks = listOf(Deck("test-deck", "Test Deck", listOf("lion"), 17, 1.0)),
            selectedFrames = mapOf("lion" to "forest"),
            unlockedFrameIds = setOf("black", "forest"),
            progressionPoints = 7
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }
}
