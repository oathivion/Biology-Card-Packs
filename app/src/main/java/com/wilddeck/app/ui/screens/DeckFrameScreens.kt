package com.wilddeck.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wilddecks.app.R
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.Deck
import com.wilddeck.app.model.SymbiosisRelationship
import com.wilddeck.app.ui.components.AnimalCardView

@Composable
fun DeckBuilderScreen(
    decks: List<Deck>,
    ownedCards: List<AnimalCard>,
    relationshipsFor: (List<String>) -> List<SymbiosisRelationship>,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String, String) -> Unit
) {
    var createDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Your Decks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("${decks.size}/5 decks · up to 5 cards each")
            }
            Button(onClick = { createDialog = true }) { Text("New Deck") }
        }
        if (decks.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No decks yet. Make one and start pairing animals.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(decks, key = { it.id }) { deck ->
                    DeckPanel(deck, ownedCards, relationshipsFor(deck.cardIds), onRename, onDelete, onAdd, onRemove)
                }
            }
        }
    }
    if (createDialog) {
        NameDialog(
            title = "Create deck",
            initialName = "Field Deck ${decks.size + 1}",
            onDismiss = { createDialog = false },
            onSave = {
                createDialog = false
                onCreate(it)
            }
        )
    }
}

@Composable
private fun DeckPanel(
    deck: Deck,
    ownedCards: List<AnimalCard>,
    relationships: List<SymbiosisRelationship>,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String, String) -> Unit
) {
    var renameDialog by remember { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(deck.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Score ${deck.score} · Symbiosis ×${formatMultiplier(deck.symbiosisMultiplier)}")
                }
                Text("${deck.cardIds.size}/5", style = MaterialTheme.typography.titleMedium)
            }
            if (deck.cardIds.isEmpty()) {
                Text("This deck is waiting for its first animal.")
            } else {
                deck.cardIds.forEach { id ->
                    val card = ownedCards.firstOrNull { it.id == id }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(card?.let { "${it.imageEmoji} ${it.name}  H${it.health} D${it.danger}" } ?: "Missing card data")
                        TextButton(onClick = { onRemove(deck.id, id) }) { Text("Remove") }
                    }
                }
            }
            if (relationships.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text("Active symbiosis", fontWeight = FontWeight.Bold)
                    relationships.forEach { relationship ->
                        Text("${relationship.type.name.lowercase().replaceFirstChar(Char::uppercase)} ×${relationship.type.multiplier}")
                        Text(relationship.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { addMenu = true }) { Text("Add Card") }
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    if (ownedCards.isEmpty()) {
                        DropdownMenuItem(text = { Text("Earn a card first") }, onClick = { addMenu = false })
                    } else {
                        ownedCards.forEach { card ->
                            DropdownMenuItem(
                                text = { Text("${card.imageEmoji} ${card.name}") },
                                onClick = {
                                    addMenu = false
                                    onAdd(deck.id, card.id)
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { renameDialog = true }) { Text("Rename") }
                TextButton(onClick = { onDelete(deck.id) }) { Text("Delete") }
            }
        }
    }
    if (renameDialog) {
        NameDialog(
            title = "Rename deck",
            initialName = deck.name,
            onDismiss = { renameDialog = false },
            onSave = {
                renameDialog = false
                onRename(deck.id, it)
            }
        )
    }
}

@Composable
private fun NameDialog(title: String, initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Deck name") }) },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FrameCustomizationScreen(
    ownedCards: List<AnimalCard>,
    frames: List<CardFrame>,
    unlockedFrameIds: Set<String>,
    initialCardId: String?,
    onApply: (String, String) -> Unit,
    onReset: (String) -> Unit
) {
    var selectedCardId by remember(initialCardId) {
        mutableStateOf(initialCardId?.takeIf { id -> ownedCards.any { it.id == id } } ?: ownedCards.firstOrNull()?.id)
    }
    var selectedFrameId by remember(selectedCardId) {
        mutableStateOf(ownedCards.firstOrNull { it.id == selectedCardId }?.currentFrameId ?: "black")
    }
    var inspectedFrame by remember { mutableStateOf<CardFrame?>(null) }
    val card = ownedCards.firstOrNull { it.id == selectedCardId }
    val previewFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.first()

    if (card == null) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Earn a card before customizing frames.")
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        BubbleShopBackground()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Frame Workshop", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Frames now add clear Wild Run effects: shields, healing, growth, damage, and survival perks.")
        }
        item {
            var cardMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { cardMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${card.imageEmoji} ${card.name}")
                }
                DropdownMenu(expanded = cardMenu, onDismissRequest = { cardMenu = false }) {
                    ownedCards.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.imageEmoji} ${option.name}") },
                            onClick = {
                                selectedCardId = option.id
                                selectedFrameId = option.currentFrameId
                                cardMenu = false
                            }
                        )
                    }
                }
            }
        }
        item {
            AnimalCardView(
                card = card.copy(currentFrameId = selectedFrameId),
                frame = previewFrame,
                modifier = Modifier.fillMaxWidth().height(500.dp)
            )
        }
        items(frames, key = { it.id }) { frame ->
            val unlocked = frame.id in unlockedFrameIds
            Row(
                modifier = Modifier.fillMaxWidth()
                    .border(
                        if (selectedFrameId == frame.id) 3.dp else 1.dp,
                        Color(frame.colorArgb),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { inspectedFrame = frame }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(54.dp)
                        .background(Color(frame.colorArgb), RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                Column(Modifier.weight(1f)) {
                    Text(frame.name, fontWeight = FontWeight.Bold)
                    Text(
                        if (selectedFrameId == frame.id) "Selected • tap for details" else "Tap for details",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (false) {
                    Text(frame.borderStyle, style = MaterialTheme.typography.bodySmall)
                    Text(frame.combatBonus.description, style = MaterialTheme.typography.labelSmall)
                    if (frame.effect != com.wilddeck.app.model.FrameEffect.NONE) {
                        Text(
                            "Animated: ${frame.effect.name.lowercase().replace('_', ' ')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    }
                }
                if (false) Text(if (unlocked) "Available" else "Locked")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    selectedFrameId = "black"
                    onReset(card.id)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset to Black Frame") }
        }
    }
    }
    inspectedFrame?.let { frame ->
        FrameWorkshopDetailDialog(
            frame = frame,
            unlocked = frame.id in unlockedFrameIds,
            selected = frame.id == selectedFrameId,
            onSelect = {
                selectedFrameId = frame.id
                onApply(card.id, frame.id)
            },
            onDismiss = { inspectedFrame = null }
        )
    }
}

@Composable
private fun FrameWorkshopDetailDialog(
    frame: CardFrame,
    unlocked: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(Color(frame.colorArgb), RoundedCornerShape(10.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                )
                Text(frame.name, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                DetailBullet("Style", frame.borderStyle)
                DetailBullet("Run effect", frame.combatBonus.description)
                DetailBullet(
                    "Effect",
                    if (frame.effect == com.wilddeck.app.model.FrameEffect.NONE) "Static frame"
                    else frame.effect.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
                )
                DetailBullet("Status", if (unlocked) "Owned" else "Locked")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelect()
                    onDismiss()
                },
                enabled = unlocked && !selected
            ) { Text(if (selected) "Selected" else "Select Frame") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatMultiplier(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

@Composable
fun FrameStoreScreen(
    frames: List<CardFrame>,
    unlockedFrameIds: Set<String>,
    points: Int,
    frameCost: (String) -> Int,
    onBuy: (String) -> Unit,
    onCustomize: () -> Unit
) {
    var selectedFrame by remember { mutableStateOf<CardFrame?>(null) }
    Box(Modifier.fillMaxSize()) {
        BubbleShopBackground()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FinFramesHeader(points)
            if (false) Text("Frame Store", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            if (false) {
            Text("$points points available · Frames add Wild Run bonuses.")
        }
        }
        items(frames, key = { it.id }) { frame ->
            val unlocked = frame.id in unlockedFrameIds
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable { selectedFrame = frame }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(54.dp)
                            .background(Color(frame.colorArgb), RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(frame.name, fontWeight = FontWeight.Bold)
                        Text("Tap for details", style = MaterialTheme.typography.bodySmall)
                        if (false) {
                        Text(frame.borderStyle, style = MaterialTheme.typography.bodySmall)
                        Text(frame.combatBonus.description, style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (frame.effect == com.wilddeck.app.model.FrameEffect.NONE) "Static frame"
                            else "Effect: ${frame.effect.name.lowercase().replace('_', ' ')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        }
                    }
                    if (false) {
                    if (unlocked) {
                        Text("Owned", fontWeight = FontWeight.Bold)
                    } else {
                        Button(onClick = { onBuy(frame.id) }, enabled = points >= frameCost(frame.id)) {
                            Text("${frameCost(frame.id)} pts")
                        }
                    }
                    }
                }
            }
        }
        item {
            Button(onClick = onCustomize, modifier = Modifier.fillMaxWidth()) {
                Text("Equip Owned Frames")
            }
        }
    }
    }
    selectedFrame?.let { frame ->
        FrameStoreDetailDialog(
            frame = frame,
            unlocked = frame.id in unlockedFrameIds,
            points = points,
            cost = frameCost(frame.id),
            onBuy = { onBuy(frame.id) },
            onDismiss = { selectedFrame = null }
        )
    }
}

@Composable
private fun FrameStoreDetailDialog(
    frame: CardFrame,
    unlocked: Boolean,
    points: Int,
    cost: Int,
    onBuy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(Color(frame.colorArgb), RoundedCornerShape(10.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                )
                Text(frame.name, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                DetailBullet("Style", frame.borderStyle)
                DetailBullet("Run effect", frame.combatBonus.description)
                DetailBullet(
                    "Effect",
                    if (frame.effect == com.wilddeck.app.model.FrameEffect.NONE) "Static frame"
                    else frame.effect.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
                )
                DetailBullet("Status", if (unlocked) "Owned" else "Locked")
                if (!unlocked) {
                    DetailBullet("Cost", "$cost points")
                }
            }
        },
        confirmButton = {
            if (unlocked) {
                TextButton(onClick = onDismiss) { Text("Close") }
            } else {
                Button(
                    onClick = {
                        onBuy()
                        onDismiss()
                    },
                    enabled = points >= cost
                ) { Text("Buy for $cost pts") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (unlocked) "Done" else "Cancel") }
        }
    )
}

@Composable
private fun DetailBullet(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("•", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, fontWeight = FontWeight.Bold)
            Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FinFramesHeader(points: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(285.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.wilddecks_fin_final),
            contentDescription = "Fin presenting the frame shop",
            modifier = Modifier.fillMaxWidth().height(285.dp),
            contentScale = ContentScale.FillWidth
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 104.dp)
        ) {
                Text(
                    "Fins Frames",
                    fontFamily = FontFamily.Cursive,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Text(
                    "$points points available",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
        }
    }
}

@Composable
private fun BubbleShopBackground() {
    val transition = rememberInfiniteTransition(label = "bubble-shop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble-drift"
    )
    Canvas(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E4F73), Color(0xFF61B4D8), Color(0xFFC8F2FF))))
    ) {
        repeat(26) { index ->
            val baseX = ((index * 47) % 100) / 100f * size.width
            val wobble = kotlin.math.sin((drift * 6.28f + index) * 1.7f) * 22f
            val y = size.height - ((drift + index / 26f) % 1f) * (size.height + 90f)
            val radius = 5f + (index % 6) * 3.5f
            drawCircle(
                Color.White.copy(alpha = 0.23f),
                radius = radius,
                center = Offset(baseX + wobble, y)
            )
            drawCircle(
                Color.White.copy(alpha = 0.42f),
                radius = radius * 0.28f,
                center = Offset(baseX + wobble - radius * 0.32f, y - radius * 0.32f)
            )
        }
    }
}
