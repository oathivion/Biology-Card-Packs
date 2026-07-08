package com.wilddeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wilddeck.app.data.CrashReporter
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.MiniGameSession
import com.wilddeck.app.model.SymbiosisRelationship
import com.wilddeck.app.domain.CardLevelingManager
import com.wilddeck.app.ui.components.AbilityInfoButton
import com.wilddeck.app.ui.components.AnimalCardView

@Composable
fun CardDetailScreen(
    card: AnimalCard?,
    frame: CardFrame?,
    relationships: List<SymbiosisRelationship>,
    isOwned: Boolean,
    onCustomize: () -> Unit,
    onCredits: () -> Unit
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
        LevelProgressBlock(card)
        DetailBlock(
            "Equipped frame",
            "${frame.name}: ${frame.combatBonus.description}"
        )
        DetailBlock("Habitat", card.habitat)
        DetailBlock("Food", card.food)
        DetailBlock("Why health is ${card.health}", card.healthExplanation)
        DetailBlock("Why danger is ${card.danger}", card.dangerExplanation)
        AbilityInfoButton(card.ability, card.combatRole)
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
        OutlinedButton(onClick = onCredits, modifier = Modifier.fillMaxWidth()) {
            Text("Credits")
        }
    }
}

@Composable
private fun LevelProgressBlock(card: AnimalCard) {
    val xpToNext = CardLevelingManager.experienceToNextLevel(card.level)
    val progress = if (card.level >= CardLevelingManager.MAX_LEVEL || xpToNext <= 0) {
        1f
    } else {
        (card.experience.toFloat() / xpToNext).coerceIn(0f, 1f)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Level (${card.level})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text("Level up stat bonuses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBonusBox(
                    label = "Danger Bonus",
                    value = card.dangerBonus,
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.weight(1f)
                )
                StatBonusBox(
                    label = "Health Bonus",
                    value = card.healthBonus,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(18.dp)
                            .background(Color(0xFF22A447))
                    )
                }
                Text(
                    if (card.level >= CardLevelingManager.MAX_LEVEL) {
                        "Level cap reached"
                    } else {
                        "${card.experience}/$xpToNext XP"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "On each level up, health and danger each roll once for a chance at +1.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StatBonusBox(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("+$value", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
            Text(
                "Animal Trivia",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                "Answer animal questions. Reach three progress points to earn a new card.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "Entry: $entryCost point · You have $points",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onStart,
                enabled = points >= entryCost,
                modifier = Modifier.testTag("start_game")
            ) { Text("Spend $entryCost point and start") }
            if (points < entryCost) {
                Text(
                    "Clear a Wild Run round to earn an entry point.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
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
            compact = false
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

@Composable
fun CreditsScreen() {
    val context = LocalContext.current
    var latestCrash by remember { mutableStateOf(CrashReporter.latestReport(context)) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Credits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(
            "Animal Trivia and WildDecks audio credits.",
            style = MaterialTheme.typography.bodyLarge
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Alwin Brauns - Bumbumchack")
                Text("Glitch - Panacea")
                Text("Glitch - Medusa")
                Text("Glitch - Mare Tranquillitatis")
                Text("Brian Ritchie - WildDecks Devlopment")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Latest crash report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                if (latestCrash.isNullOrBlank()) {
                    Text("No crash report has been saved on this device.")
                } else {
                    Text(
                        latestCrash!!.take(2400),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (latestCrash!!.length > 2400) {
                        Text("Report shortened for display. Full report is saved in app files.")
                    }
                    OutlinedButton(
                        onClick = {
                            CrashReporter.clearReports(context)
                            latestCrash = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear crash report") }
                }
            }
        }
    }
}


