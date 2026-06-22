package com.wilddeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.Deck
import com.wilddeck.app.ui.components.AnimalCardView

@Composable
fun HomeScreen(
    ownedCount: Int,
    lockedCount: Int,
    deckCount: Int,
    progressionPoints: Int,
    onPlay: () -> Unit,
    onCombat: () -> Unit,
    onCollection: () -> Unit,
    onDecks: () -> Unit,
    onFrames: () -> Unit,
    onDetails: () -> Unit,
    onLockedDetails: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> HubPage("Frame Store", "Buy and equip cosmetic animated frames.", "Open Frame Store", onFrames, "SECOND LEFT")
                1 -> HubPage("Decks", "$deckCount of 5 decks created.", "Build Decks", onDecks, "FIRST LEFT")
                2 -> PlayLanding(ownedCount, deckCount, progressionPoints, onPlay, onCombat)
                3 -> CollectionHub(ownedCount, lockedCount, onCollection, onDetails, onLockedDetails)
                else -> PlaceholderHub()
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                Box(
                    Modifier.padding(4.dp).size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
private fun PlayLanding(
    ownedCount: Int,
    deckCount: Int,
    progressionPoints: Int,
    onPlay: () -> Unit,
    onCombat: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("WILDDECK", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("Choose your next expedition.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatSummary("$ownedCount", "cards", Modifier.weight(1f))
            StatSummary("$deckCount/5", "decks", Modifier.weight(1f))
            StatSummary("$progressionPoints", "points", Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        HomeButton("ANIMAL TRIVIA", "Spend a point, answer questions, earn a new creature", onPlay)
        Spacer(Modifier.height(12.dp))
        HomeButton("WILD RUN", "Battle with your deck and earn progression points", onCombat)
        Spacer(Modifier.height(20.dp))
        Text("Swipe left or right for decks, cards, frames, and more.", textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HubPage(title: String, description: String, action: String, onClick: () -> Unit, eyebrow: String) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text(description, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(action) }
    }
}

@Composable
private fun CollectionHub(
    ownedCount: Int,
    lockedCount: Int,
    onCollection: () -> Unit,
    onDetails: () -> Unit,
    onLockedDetails: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("FIELD GUIDE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Cards & Collection", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Unlocked · $ownedCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Browse your collection, inspect cards, and add them to decks.")
                Spacer(Modifier.height(10.dp))
                Button(onClick = onCollection, modifier = Modifier.fillMaxWidth()) { Text("Open Collection") }
                OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth()) { Text("Card Information") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Locked · $lockedCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Preview undiscovered creatures and learn what remains.")
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onLockedDetails, enabled = lockedCount > 0, modifier = Modifier.fillMaxWidth()) {
                    Text("View Locked Card")
                }
            }
        }
    }
}

@Composable
private fun PlaceholderHub() {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SECOND RIGHT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Coming Soon", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("This space is reserved for the next major feature.", textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatSummary(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
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
