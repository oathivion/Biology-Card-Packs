package com.wilddeck.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.wilddeck.app.data.SampleData
import com.wilddeck.app.ui.components.AnimalCardView
import com.wilddeck.app.ui.theme.WildDeckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnimalCardLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statsImageAndDescription_followRequiredCardLayout() {
        val card = SampleData.animalCards.first()
        val frame = SampleData.frames.first()
        composeRule.setContent {
            WildDeckTheme {
                AnimalCardView(card = card, frame = frame)
            }
        }

        val danger = composeRule.onNodeWithTag("danger_${card.id}").assertExists()
            .fetchSemanticsNode().boundsInRoot
        val health = composeRule.onNodeWithTag("health_${card.id}").assertExists()
            .fetchSemanticsNode().boundsInRoot
        val image = composeRule.onNodeWithTag("animal_image_${card.id}").assertExists()
            .fetchSemanticsNode().boundsInRoot
        val description = composeRule.onNodeWithTag("description_${card.id}").assertExists()
            .fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag("animal_card_${card.id}").assertIsDisplayed()
        assertTrue(danger.left < health.left)
        assertTrue(image.top >= danger.bottom)
        assertTrue(description.top >= image.bottom)
        assertEquals("black", card.defaultFrameId)
    }
}
