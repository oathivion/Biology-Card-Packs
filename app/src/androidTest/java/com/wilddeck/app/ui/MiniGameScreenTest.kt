package com.wilddeck.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.wilddeck.app.data.SampleData
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.model.TriviaQuestion
import com.wilddeck.app.ui.screens.MiniGameScreen
import com.wilddeck.app.ui.theme.WildDeckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MiniGameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun answerButton_respondsToTap() {
        val card = SampleData.animalCards.first()
        val frame = SampleData.frames.first()
        val question = TriviaQuestion(
            id = "lion_food",
            prompt = "What does a lion eat?",
            options = listOf("Meat", "Grass", "Algae", "Plankton"),
            correctAnswer = "Meat"
        )
        var selected: String? = null
        composeRule.setContent {
            WildDeckTheme {
                MiniGameScreen(
                    session = MiniGameSession(card, listOf(question)),
                    frame = frame,
                    feedback = null,
                    onStart = {},
                    onAnswer = { selected = it },
                    onCollection = {}
                )
            }
        }

        composeRule.onNodeWithTag("answer_0").performClick()

        composeRule.runOnIdle { assertEquals("Meat", selected) }
    }

    @Test
    fun rewardedSession_showsRewardMessage() {
        val card = SampleData.animalCards.first()
        val question = TriviaQuestion(
            id = "lion_food",
            prompt = "What does a lion eat?",
            options = listOf("Meat", "Grass", "Algae", "Plankton"),
            correctAnswer = "Meat"
        )
        composeRule.setContent {
            WildDeckTheme {
                MiniGameScreen(
                    session = MiniGameSession(
                        targetCard = card,
                        questions = listOf(question),
                        matchCount = 3,
                        isRewarded = true
                    ),
                    frame = SampleData.frames.first(),
                    feedback = "Card added to inventory.",
                    onStart = {},
                    onAnswer = {},
                    onCollection = {}
                )
            }
        }

        composeRule.onNodeWithTag("reward_message").assertIsDisplayed()
    }

    @Test
    fun inlineFeedback_doesNotReplaceAnyAnswer() {
        val card = SampleData.animalCards.first()
        val question = TriviaQuestion(
            id = "lion_food",
            prompt = "What does a lion eat?",
            options = listOf("Grass", "Algae", "Plankton", "Meat"),
            correctAnswer = "Meat"
        )
        composeRule.setContent {
            WildDeckTheme {
                MiniGameScreen(
                    session = MiniGameSession(card, listOf(question)),
                    frame = SampleData.frames.first(),
                    feedback = "Correct! Next question.",
                    onStart = {},
                    onAnswer = {},
                    onCollection = {}
                )
            }
        }

        composeRule.onNodeWithTag("answer_feedback").assertIsDisplayed()
        repeat(4) { composeRule.onNodeWithTag("answer_$it").assertExists() }
    }
}
