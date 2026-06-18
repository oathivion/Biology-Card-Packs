package com.wilddeck.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.Deck
import com.wilddeck.app.ui.components.AnimalCardView

@Composable
fun HomeScreen(
    ownedCount: Int,
    deckCount: Int,
    onPlay: () -> Unit,
    onCollection: () -> Unit,
    onDecks: () -> Unit,
    onFrames: () -> Unit,
    onDetails: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("WILDDECK", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text(
            "Collect real animals. Learn how they live. Build a tiny, biologically brilliant deck.",
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSummary("$ownedCount", "cards")
            StatSummary("$deckCount/5", "decks")
        }
        Spacer(Modifier.height(6.dp))
        HomeButton("Play Mini Game", "Match an animal with its food", onPlay)
        HomeButton("View Collection", "Browse every card you have earned", onCollection)
        HomeButton("Build Decks", "Create and score five-card teams", onDecks)
        HomeButton("Customize Frames", "Change a card's cosmetic border", onFrames)
        OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth()) {
            Text("View Card Details")
        }
    }
}

@Composable
private fun StatSummary(value: String, label: String) {
    Card {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HomeButton(title: String, subtitle: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CollectionScreen(
    cards: List<AnimalCard>,
    framesById: Map<String, CardFrame>,
    decks: List<Deck>,
    onOpenCard: (String) -> Unit,
    onAddToDeck: (String, String) -> Unit,
    onPlay: () -> Unit
) {
    if (cards.isEmpty()) {
        EmptyCollection(onPlay)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(220.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(cards, key = { it.id }) { card ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimalCardView(
                    card = card,
                    frame = framesById.getValue(card.currentFrameId),
                    modifier = Modifier.fillMaxWidth().height(350.dp),
                    compact = true,
                    onClick = { onOpenCard(card.id) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenCard(card.id) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Details") }
                    AddToDeckButton(card.id, decks, onAddToDeck, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AddToDeckButton(
    cardId: String,
    decks: List<Deck>,
    onAddToDeck: (String, String) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (decks.isEmpty()) {
                DropdownMenuItem(text = { Text("Create a deck first") }, onClick = { expanded = false })
            } else {
                decks.forEach { deck ->
                    DropdownMenuItem(
                        text = { Text("${deck.name} (${deck.cardIds.size}/5)") },
                        onClick = {
                            expanded = false
                            onAddToDeck(deck.id, cardId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCollection(onPlay: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your field journal is empty", style = MaterialTheme.typography.headlineSmall)
        Text("Get three food matches right to earn your first animal.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onPlay) { Text("Play Mini Game") }
    }
}
