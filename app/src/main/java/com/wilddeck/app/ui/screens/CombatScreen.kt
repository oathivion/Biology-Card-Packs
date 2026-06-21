package com.wilddeck.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
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
    lockedFrames: List<CardFrame>,
    frameCost: (String) -> Int,
    onStart: (String?) -> Unit,
    onAction: (String, String) -> Unit,
    onNextRound: () -> Unit,
    onEndRun: () -> Unit,
    onUnlockFrame: (String) -> Unit
) {
    if (session == null) {
        CombatLobby(points, decks, ownedCards, lockedFrames, frameCost, onStart, onUnlockFrame)
    } else {
        CombatBoard(session, points, onAction, onNextRound, onEndRun)
    }
}

@Composable
private fun CombatLobby(
    points: Int,
    decks: List<Deck>,
    ownedCards: List<AnimalCard>,
    lockedFrames: List<CardFrame>,
    frameCost: (String) -> Int,
    onStart: (String?) -> Unit,
    onUnlockFrame: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Wild Run", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Drag attackers onto enemies and supports onto allies. Clear a round to earn 1 point.")
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
            Text(
                "$points points",
                Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text("Choose a team", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        decks.filter { it.cardIds.isNotEmpty() }.forEach { deck ->
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
            Text("Earn your first creature through Animal Trivia to begin.")
        }

        Text("Buy frames", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        lockedFrames.forEach { frame ->
            UnlockRow(
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
            Text("◇", fontSize = 30.sp)
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
    var inspectedUnit by remember { mutableStateOf<CombatUnit?>(null) }

    Column(
        Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Round ${session.round}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "You ×${formatCombatMultiplier(session.playerMultiplier)} · " +
                        "Enemy ×${formatCombatMultiplier(session.enemyMultiplier)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("$points pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Enemies · long-press for details", fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                session.enemyUnits.filter { it.isAlive }.forEach { unit ->
                    CombatTile(
                        unit = unit,
                        enemy = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .onGloballyPositioned { targetBounds[unit.instanceId] = it.boundsInRoot() }
                            .pointerInput(unit.instanceId) {
                                detectTapGestures(onLongPress = { inspectedUnit = unit })
                            }
                    )
                }
            }
            session.battleLog.lastOrNull()?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
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
                    else -> "Drag an available card to its target."
                },
                Modifier.padding(7.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Your team · long-press for details", fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        onInspect = { inspectedUnit = unit },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .onGloballyPositioned { targetBounds[unit.instanceId] = it.boundsInRoot() }
                    )
                }
            }
        }

        when {
            session.isRoundCleared -> Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                Text("Enter Round ${session.round + 1}")
            }
            session.isDefeated -> Button(onClick = onEndRun, modifier = Modifier.fillMaxWidth()) {
                Text("Return to Camp")
            }
        }
        TextButton(onClick = onEndRun, modifier = Modifier.fillMaxWidth()) { Text("End Run") }
    }

    inspectedUnit?.let { unit ->
        CombatCardDialog(unit = unit, onDismiss = { inspectedUnit = null })
    }
}

@Composable
private fun DraggableCombatTile(
    unit: CombatUnit,
    allTargetBounds: Map<String, Rect>,
    validTargetIds: Set<String>,
    enabled: Boolean,
    onDrop: (String) -> Unit,
    onInspect: () -> Unit,
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
            .pointerInput(unit.instanceId) {
                detectTapGestures(onLongPress = { onInspect() })
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
        modifier = modifier.border(
            2.dp,
            if (enemy) MaterialTheme.colorScheme.error else roleColor,
            RoundedCornerShape(10.dp)
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(unit.card.imageEmoji, fontSize = 27.sp)
            Text(
                unit.card.name,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "♥${unit.currentHealth}/${unit.maxHealth} ⚔${unit.damage}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            if (unit.shield > 0) {
                Text("Shield ${unit.shield}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                unit.card.combatRole.name,
                color = roleColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(unit.card.ability.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun CombatCardDialog(unit: CombatUnit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(Modifier.fillMaxSize().padding(14.dp), shape = RoundedCornerShape(22.dp)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DANGER ${unit.card.danger}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Text("HEALTH ${unit.card.health}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Text(unit.card.imageEmoji, fontSize = 108.sp)
                Text(unit.card.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "${unit.card.combatRole.name.lowercase().replaceFirstChar(Char::uppercase)} · " +
                        "Combat HP ${unit.currentHealth}/${unit.maxHealth} · Damage ${unit.damage}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                InspectionBlock("Animal", unit.card.description)
                InspectionBlock("Ability: ${unit.card.ability.name}", unit.card.ability.description)
                InspectionBlock("Habitat", unit.card.habitat)
                InspectionBlock("Diet", unit.card.food)
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Return to battle") }
            }
        }
    }
}

@Composable
private fun InspectionBlock(title: String, description: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description)
        }
    }
}

private fun formatCombatMultiplier(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
