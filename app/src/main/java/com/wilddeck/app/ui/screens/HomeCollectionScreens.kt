package com.wilddeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.wilddeck.app.model.TriviaQuestion
import com.wilddeck.app.ui.components.AnimalPhoto
import com.wilddeck.app.ui.components.AnimalCardView

@Composable
fun HomeScreen(
    ownedCount: Int,
    lockedCount: Int,
    deckCount: Int,
    progressionPoints: Int,
    catalog: List<AnimalCard>,
    ownedCards: List<AnimalCard>,
    decks: List<Deck>,
    frames: List<CardFrame>,
    framesById: Map<String, CardFrame>,
    unlockedFrameIds: Set<String>,
    frameCost: (String) -> Int,
    learningTriviaByCardId: Map<String, List<TriviaQuestion>>,
    humanRelationshipNotes: Map<String, String>,
    onPlay: () -> Unit,
    onCombat: () -> Unit,
    onOpenCard: (String) -> Unit,
    onAddToDeck: (String, String) -> Unit,
    onBuyFrame: (String) -> Unit,
    onCustomizeFrames: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> FrameStoreScreen(
                    frames = frames,
                    unlockedFrameIds = unlockedFrameIds,
                    points = progressionPoints,
                    frameCost = frameCost,
                    onBuy = onBuyFrame,
                    onCustomize = onCustomizeFrames
                )
                1 -> DeckSlotsPage(decks, ownedCards)
                2 -> PlayLanding(ownedCount, deckCount, progressionPoints, onPlay, onCombat)
                3 -> CollectionPagerPage(catalog, ownedCards, framesById, decks, onPlay, onOpenCard, onAddToDeck)
                else -> LearnMorePage(catalog, ownedCards, learningTriviaByCardId, humanRelationshipNotes)
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
private fun DeckSlotsPage(decks: List<Deck>, ownedCards: List<AnimalCard>) {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(Deck.MAX_CARDS) { index ->
            val deck = decks.getOrNull(index)
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Row(
                    Modifier.fillMaxSize().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeckIconStack(deck, ownedCards)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(deck?.name ?: "Open Deck Slot ${index + 1}", fontWeight = FontWeight.Black)
                            Text("${deck?.cardIds?.size ?: 0}/5")
                        }
                        if (deck == null || deck.cardIds.isEmpty()) {
                            Text("Tap deck builder to add cards.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                deck.cardIds.take(5)
                                    .mapNotNull { id -> ownedCards.firstOrNull { it.id == id }?.name }
                                    .joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                            Text("Score ${deck.score} · ×${formatHomeMultiplier(deck.symbiosisMultiplier)}",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckIconStack(deck: Deck?, ownedCards: List<AnimalCard>) {
    val cards = deck?.cardIds.orEmpty().take(5).mapNotNull { id -> ownedCards.firstOrNull { it.id == id } }
    Box(
        Modifier
            .size(width = 86.dp, height = 96.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (cards.isEmpty()) {
            Text("+", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary)
        } else {
            cards.forEachIndexed { cardIndex, card ->
                Box(
                    Modifier
                        .padding(start = (cardIndex * 7).dp, top = (cardIndex * 5).dp)
                        .size(width = 42.dp, height = 58.dp)
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(card.imageEmoji, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

private fun formatHomeMultiplier(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

@Composable
private fun CollectionPagerPage(
    catalog: List<AnimalCard>,
    ownedCards: List<AnimalCard>,
    framesById: Map<String, CardFrame>,
    decks: List<Deck>,
    onUnlock: () -> Unit,
    onOpenCard: (String) -> Unit,
    onAddToDeck: (String, String) -> Unit
) {
    val ownedIds = ownedCards.map { it.id }.toSet()
    val unlocked = ownedCards.sortedBy { it.name }
    val locked = catalog.filterNot { it.id in ownedIds }.sortedBy { it.name }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cards & Collection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Unlocked cards first, then locked cards. Both are alphabetical.")
            Spacer(Modifier.height(10.dp))
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                Text("Unlock New Card")
            }
        }
        item { CollectionSectionTitle("Unlocked", unlocked.size) }
        items(unlocked, key = { "unlocked_${it.id}" }) { card ->
            CollectionListCard(card, framesById, decks, locked = false, onOpenCard, onAddToDeck)
        }
        item { CollectionSectionTitle("Locked", locked.size) }
        items(locked, key = { "locked_${it.id}" }) { card ->
            CollectionListCard(card, framesById, decks, locked = true, onOpenCard, onAddToDeck)
        }
    }
}

@Composable
private fun CollectionSectionTitle(title: String, count: Int) {
    Text("$title · $count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
}

@Composable
private fun CollectionListCard(
    card: AnimalCard,
    framesById: Map<String, CardFrame>,
    decks: List<Deck>,
    locked: Boolean,
    onOpenCard: (String) -> Unit,
    onAddToDeck: (String, String) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimalCardView(
                card = card,
                frame = framesById[card.currentFrameId] ?: framesById.values.first(),
                modifier = Modifier.size(width = 112.dp, height = 160.dp),
                compact = locked,
                onClick = { onOpenCard(card.id) }
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (locked) "Locked: ${card.name}" else card.name, fontWeight = FontWeight.Black)
                Text("${card.species} · ${card.rarity}", style = MaterialTheme.typography.bodySmall)
                Text("H${card.health} D${card.danger} · ${card.habitat}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOpenCard(card.id) }, modifier = Modifier.weight(1f)) {
                        Text("Details")
                    }
                    if (!locked) {
                        AddToDeckButton(card.id, decks, onAddToDeck, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnMorePage(
    catalog: List<AnimalCard>,
    ownedCards: List<AnimalCard>,
    triviaByCardId: Map<String, List<TriviaQuestion>>,
    humanRelationshipNotes: Map<String, String>
) {
    val ownedIds = ownedCards.map { it.id }.toSet()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Learn more about...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Every animal includes its trivia answer key and how it relates to people.")
        }
        items(catalog.sortedBy { it.name.lowercase() }, key = { "learn_${it.id}" }) { card ->
            LearnMoreAnimalCard(
                card = card,
                unlocked = card.id in ownedIds,
                questions = triviaByCardId[card.id].orEmpty(),
                humanNote = humanRelationshipNotes[card.id].orEmpty()
            )
        }
    }
}

@Composable
private fun LearnMoreAnimalCard(
    card: AnimalCard,
    unlocked: Boolean,
    questions: List<TriviaQuestion>,
    humanNote: String
) {
    var expanded by remember(card.id) { mutableStateOf(false) }
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(74.dp)
                ) {
                    AnimalPhoto(card = card, modifier = Modifier.fillMaxSize(), fallbackFontSize = MaterialTheme.typography.displaySmall.fontSize)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (unlocked) card.name else "Locked: ${card.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black)
                    Text(card.species, style = MaterialTheme.typography.bodySmall)
                    val prompt = when {
                        !unlocked -> "Unlock this animal to reveal its facts and trivia."
                        expanded -> "Tap to collapse"
                        else -> "Tap to learn more"
                    }
                    Text(prompt, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (expanded) {
                if (!unlocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "This animal's learning notes and trivia are locked until you earn the card.",
                            Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(humanNote.ifBlank { "Human relationship notes are being researched for this animal." })
                    Text("Trivia", fontWeight = FontWeight.Black)
                    questions.forEach { question ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    question.difficulty.name.lowercase().replaceFirstChar(Char::uppercase),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(question.prompt, fontWeight = FontWeight.Bold)
                                Text(question.correctAnswer)
                            }
                        }
                    }
                }
            }
        }
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
        gridItems(cards, key = { it.id }) { card ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimalCardView(
                    card = card,
                    frame = framesById.getValue(card.currentFrameId),
                    modifier = Modifier.fillMaxWidth().height(350.dp),
                    compact = false,
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
