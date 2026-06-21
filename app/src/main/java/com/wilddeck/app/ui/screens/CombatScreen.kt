package com.wilddeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.CardRarity
import com.wilddeck.app.model.CombatRole
import com.wilddeck.app.model.CombatSession
import com.wilddeck.app.model.CombatUnit
import com.wilddeck.app.model.Deck

@Composable
fun CombatScreen(
    session: CombatSession?,
    points: Int,
    decks: List<Deck>,
    ownedCards: List<AnimalCard>,
    lockedCards: List<AnimalCard>,
    lockedFrames: List<CardFrame>,
    creatureCost: (CardRarity) -> Int,
    frameCost: (String) -> Int,
    onStart: (String?) -> Unit,
    onAction: (String, String) -> Unit,
    onNextRound: () -> Unit,
    onEndRun: () -> Unit,
    onUnlockCreature: (String) -> Unit,
    onUnlockFrame: (String) -> Unit
) {
    if (session == null) {
        CombatLobby(
            points, decks, ownedCards, lockedCards, lockedFrames,
            creatureCost, frameCost, onStart, onUnlockCreature, onUnlockFrame
        )
    } else {
        CombatBoard(session, points, onAction, onNextRound, onEndRun)
    }
}

@Composable
private fun CombatLobby(
    points: Int,
    decks: List<Deck>,
    ownedCards: List<AnimalCard>,
    lockedCards: List<AnimalCard>,
    lockedFrames: List<CardFrame>,
    creatureCost: (CardRarity) -> Int,
    frameCost: (String) -> Int,
    onStart: (String?) -> Unit,
    onUnlockCreature: (String) -> Unit,
    onUnlockFrame: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Wild Run", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Drag attackers onto enemies and supports onto allies. Clear a round to earn 1 point.")
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "$points unlock points",
                Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text("Choose a team", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val usableDecks = decks.filter { it.cardIds.isNotEmpty() }
        usableDecks.forEach { deck ->
            Button(onClick = { onStart(deck.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("${deck.name} · ${deck.cardIds.size} cards · ×${formatCombatMultiplier(deck.symbiosisMultiplier)}")
            }
        }
        OutlinedButton(
            onClick = { onStart(null) },
            enabled = ownedCards.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use first ${minOf(5, ownedCards.size)} collected creatures")
        }
        if (ownedCards.isEmpty()) {
            Text("Earn a creature through trivia or unlock one below to begin.")
        }

        Text("Unlock creatures", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        lockedCards.forEach { card ->
            UnlockRow(
                icon = card.imageEmoji,
                title = card.name,
                detail = "${card.combatRole.name.lowercase().replaceFirstChar(Char::uppercase)} · ${card.ability.name}",
                cost = creatureCost(card.rarity),
                points = points,
                onUnlock = { onUnlockCreature(card.id) }
            )
        }
        if (lockedCards.isEmpty()) Text("Every creature has been unlocked.")

        Text("Unlock frames", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        lockedFrames.forEach { frame ->
            UnlockRow(
                icon = "◇",
                title = frame.name,
                detail = frame.borderStyle,
                cost = frameCost(frame.id),
                points = points,
                onUnlock = { onUnlockFrame(frame.id) }
            )
        }
        if (lockedFrames.isEmpty()) Text("Every frame has been unlocked.")
    }
}

@Composable
private fun UnlockRow(
    icon: String,
    title: String,
    detail: String,
    cost: Int,
    points: Int,
    onUnlock: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, fontSize = 30.sp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onUnlock, enabled = points >= cost) { Text("$cost pts") }
        }
    }
}

@Composable
private fun CombatBoard(
    session: CombatSession,
    points: Int,
    onAction: (String, String) -> Unit,
    onNextRound: () -> Unit,
    onEndRun: () -> Unit
) {
    val targetBounds = remember { mutableStateMapOf<String, Rect>() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Round ${session.round}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Player ×${formatCombatMultiplier(session.playerMultiplier)} · Enemy ×${formatCombatMultiplier(session.enemyMultiplier)}")
            }
            Text("$points pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Text("Enemies", fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            session.enemyUnits.filter { it.isAlive }.forEach { unit ->
                CombatTile(
                    unit = unit,
                    enemy = true,
                    modifier = Modifier.onGloballyPositioned { targetBounds[unit.instanceId] = it.boundsInRoot() }
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    session.isDefeated -> "Your team was defeated."
                    session.isRoundCleared -> "Round cleared. One point earned."
                    else -> "Drag a glowing player card to a valid target."
                },
                Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        Text("Your team", fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            session.playerUnits.filter { it.isAlive }.forEach { unit ->
                DraggableCombatTile(
                    unit = unit,
                    allTargetBounds = targetBounds,
                    validTargetIds = if (unit.card.combatRole == CombatRole.SUPPORT) {
                        session.playerUnits.filter { it.isAlive }.map { it.instanceId }.toSet()
                    } else {
                        session.enemyUnits.filter { it.isAlive }.map { it.instanceId }.toSet()
                    },
                    enabled = !unit.hasActed && !session.isDefeated && !session.isRoundCleared,
                    onDrop = { targetId -> onAction(unit.instanceId, targetId) },
                    modifier = Modifier.onGloballyPositioned { targetBounds[unit.instanceId] = it.boundsInRoot() }
                )
            }
        }

        Text("Battle log", fontWeight = FontWeight.Bold)
        session.battleLog.takeLast(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }

        when {
            session.isRoundCleared -> Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                Text("Enter Round ${session.round + 1}")
            }
            session.isDefeated -> Button(onClick = onEndRun, modifier = Modifier.fillMaxWidth()) {
                Text("Return to Camp")
            }
        }
        TextButton(onClick = onEndRun, modifier = Modifier.fillMaxWidth()) { Text("End Run") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DraggableCombatTile(
    unit: CombatUnit,
    allTargetBounds: Map<String, Rect>,
    validTargetIds: Set<String>,
    enabled: Boolean,
    onDrop: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember(unit.instanceId) { mutableStateOf(Offset.Zero) }
    var originalBounds by remember(unit.instanceId) { mutableStateOf(Rect.Zero) }
    CombatTile(
        unit = unit,
        enemy = false,
        modifier = modifier
            .onGloballyPositioned {
                if (dragOffset == Offset.Zero) originalBounds = it.boundsInRoot()
            }
            .zIndex(if (dragOffset != Offset.Zero) 10f else 0f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                alpha = if (unit.hasActed) 0.55f else 1f
            }
            .pointerInput(unit.instanceId, enabled, validTargetIds) {
                if (enabled) {
                    detectDragGestures(
                        onDragEnd = {
                            val dropPoint = originalBounds.center + dragOffset
                            allTargetBounds.entries
                                .firstOrNull { it.key in validTargetIds && it.value.contains(dropPoint) }
                                ?.key
                                ?.let(onDrop)
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = { dragOffset = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount
                        }
                    )
                }
            }
            .testTag("combat_actor_${unit.instanceId}")
    )
}

@Composable
private fun CombatTile(unit: CombatUnit, enemy: Boolean, modifier: Modifier = Modifier) {
    val roleColor = if (unit.card.combatRole == CombatRole.SUPPORT) Color(0xFF287B72) else Color(0xFF9B3D32)
    Card(
        modifier = modifier
            .size(width = 150.dp, height = 190.dp)
            .border(3.dp, if (enemy) MaterialTheme.colorScheme.error else roleColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(unit.card.imageEmoji, fontSize = 42.sp)
            Text(unit.card.name, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
            Text("HP ${unit.currentHealth}/${unit.maxHealth}  DMG ${unit.damage}", fontWeight = FontWeight.Bold)
            if (unit.shield > 0) Text("Shield ${unit.shield}", color = MaterialTheme.colorScheme.primary)
            Text(
                unit.card.combatRole.name,
                color = roleColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(unit.card.ability.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            Text(unit.card.ability.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}

private fun formatCombatMultiplier(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
