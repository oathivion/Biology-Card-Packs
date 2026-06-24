package com.wilddeck.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.model.SymbiosisRelationship
import com.wilddeck.app.ui.components.AnimalCardView

@Composable
fun CardDetailScreen(
    card: AnimalCard?,
    frame: CardFrame?,
    relationships: List<SymbiosisRelationship>,
    isOwned: Boolean,
    onCustomize: () -> Unit
) {
    if (card == null || frame == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Card data is missing.")
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimalCardView(card, frame, Modifier.fillMaxWidth().height(560.dp))
        DetailBlock("Habitat", card.habitat)
        DetailBlock("Food", card.food)
        DetailBlock("Why health is ${card.health}", card.healthExplanation)
        DetailBlock("Why danger is ${card.danger}", card.dangerExplanation)
        DetailBlock(
            "Wild Run role",
            "${card.combatRole.name.lowercase().replaceFirstChar(Char::uppercase)} — ${card.ability.name}\n${card.ability.description}"
        )
        DetailBlock("Rarity", card.rarity.name.lowercase().replaceFirstChar(Char::uppercase))
        DetailBlock(
            "Known symbiotic partners",
            if (relationships.isEmpty()) "No relationship is included in the current field guide."
            else relationships.joinToString("\n\n") {
                val partnerId = if (it.animalAId == card.id) it.animalBId else it.animalAId
                val partnerName = partnerId.replace('_', ' ').replaceFirstChar(Char::uppercase)
                "$partnerName — ${it.type.name.lowercase().replaceFirstChar(Char::uppercase)}\n${it.description}"
            }
        )
        if (isOwned) {
            Button(onClick = onCustomize, modifier = Modifier.fillMaxWidth()) {
                Text("Customize this card's frame")
            }
        } else {
            Text("Earn this card in the mini game to customize it.", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun DetailBlock(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MiniGameScreen(
    session: MiniGameSession?,
    frame: CardFrame?,
    feedback: String?,
    points: Int,
    entryCost: Int,
    onStart: () -> Unit,
    onAnswer: (String) -> Unit,
    onCollection: () -> Unit
) {
    if (session == null || frame == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Animal Trivia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Answer animal questions. Reach three progress points to earn a new card.")
            Text(
                "Entry: $entryCost point · You have $points",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onStart,
                enabled = points >= entryCost,
                modifier = Modifier.testTag("start_game")
            ) { Text("Spend $entryCost point and start") }
            if (points < entryCost) {
                Text("Clear a Wild Run round to earn an entry point.")
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Animal Trivia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("${session.matchCount}/${session.requiredMatchCount}", style = MaterialTheme.typography.headlineSmall)
        }
        AnimalCardView(
            card = session.targetCard,
            frame = frame,
            modifier = Modifier.fillMaxWidth().height(430.dp),
            compact = !session.isRewarded
        )
        if (session.isRewarded) {
            Text(
                "You earned ${session.targetCard.name}!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("reward_message")
            )
            Button(onClick = onCollection, modifier = Modifier.fillMaxWidth()) { Text("View Collection") }
            OutlinedButton(
                onClick = onStart,
                enabled = points >= entryCost,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Play Again ($entryCost point)") }
        } else {
            feedback?.let {
                Surface(
                    color = if (it.startsWith("Correct")) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().testTag("answer_feedback")
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                session.currentQuestion.prompt,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().testTag("trivia_prompt")
            )
            session.currentQuestion.options.forEachIndexed { index, option ->
                OutlinedButton(
                    onClick = { onAnswer(option) },
                    modifier = Modifier.fillMaxWidth().testTag("answer_$index")
                ) {
                    Text(option, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
