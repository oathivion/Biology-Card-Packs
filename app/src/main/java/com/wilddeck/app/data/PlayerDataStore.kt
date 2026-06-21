package com.wilddeck.app.data

import android.content.Context
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.PersistedPlayerData
import org.json.JSONArray
import org.json.JSONObject

class PlayerDataStore(context: Context) {
    private val preferences = context.getSharedPreferences("wilddeck_player", Context.MODE_PRIVATE)

    fun load(): PersistedPlayerData {
        val raw = preferences.getString(KEY_DATA, null) ?: return PersistedPlayerData()
        return runCatching {
            val root = JSONObject(raw)
            val ownedIds = root.optJSONArray("ownedCardIds").toStringSet()
            val unlockedIds = root.optJSONArray("unlockedFrameIds").toStringSet()
                .ifEmpty { setOf("black", "forest", "ocean") }
            val selectedFrames = buildMap {
                val objectValue = root.optJSONObject("selectedFrames") ?: JSONObject()
                objectValue.keys().forEach { cardId ->
                    put(cardId, objectValue.getString(cardId))
                }
            }
            val decks = buildList {
                val deckArray = root.optJSONArray("decks") ?: JSONArray()
                for (index in 0 until deckArray.length()) {
                    val item = deckArray.getJSONObject(index)
                    add(
                        Deck(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            cardIds = item.optJSONArray("cardIds").toStringList(),
                            score = item.optInt("score"),
                            symbiosisMultiplier = item.optDouble("multiplier", 1.0)
                        )
                    )
                }
            }
            PersistedPlayerData(
                ownedCardIds = ownedIds,
                decks = decks,
                selectedFrames = selectedFrames,
                unlockedFrameIds = unlockedIds,
                progressionPoints = root.optInt("progressionPoints", 0)
            )
        }.getOrDefault(PersistedPlayerData())
    }

    fun save(data: PersistedPlayerData) {
        val root = JSONObject().apply {
            put("ownedCardIds", JSONArray(data.ownedCardIds.toList()))
            put("unlockedFrameIds", JSONArray(data.unlockedFrameIds.toList()))
            put("selectedFrames", JSONObject(data.selectedFrames))
            put("progressionPoints", data.progressionPoints)
            put("decks", JSONArray().apply {
                data.decks.forEach { deck ->
                    put(JSONObject().apply {
                        put("id", deck.id)
                        put("name", deck.name)
                        put("cardIds", JSONArray(deck.cardIds))
                        put("score", deck.score)
                        put("multiplier", deck.symbiosisMultiplier)
                    })
                }
            })
        }
        preferences.edit().putString(KEY_DATA, root.toString()).apply()
    }

    private fun JSONArray?.toStringSet(): Set<String> = toStringList().toSet()

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) add(getString(index))
        }
    }

    private companion object {
        const val KEY_DATA = "player_data"
    }
}
