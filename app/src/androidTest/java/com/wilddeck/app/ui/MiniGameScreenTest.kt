package com.wilddeck.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.wilddeck.app.data.SampleData
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.ui.screens.MiniGameScreen
import com.wilddeck.app.ui.theme.WildDeckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MiniGameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun foodButton_respondsToTap() {
        val card = SampleData.animalCards.first()
        val frame = SampleData.frames.first()
        var selected: String? = null
        composeRule.setContent {
            WildDeckTheme {
                MiniGameScreen(
                    session = MiniGameSession(card, listOf(card.food, "Grass")),
                    frame = frame,
                    onStart = {},
                    onAnswer = { selected = it },
                    onCollection = {}
                )
            }
        }

        composeRule.onNodeWithTag("food_${card.food}").performClick()

        composeRule.runOnIdle { assertEquals(card.food, selected) }
    }

    @Test
    fun rewardedSession_showsRewardMessage() {
        val card = SampleData.animalCards.first()
        composeRule.setContent {
            WildDeckTheme {
                MiniGameScreen(
                    session = MiniGameSession(
                        targetCard = card,
                        foodOptions = listOf(card.food),
                        matchCount = 3,
                        isRewarded = true
                    ),
                    frame = SampleData.frames.first(),
                    onStart = {},
                    onAnswer = {},
                    onCollection = {}
                )
            }
        }

        composeRule.onNodeWithTag("reward_message").assertIsDisplayed()
    }
}
