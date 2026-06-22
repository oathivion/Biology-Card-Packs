package com.wilddeck.app.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.wilddeck.app.model.AbilityType
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.CombatEffect
import com.wilddeck.app.model.CombatEffectType
import com.wilddeck.app.model.CombatRole
import com.wilddeck.app.model.CombatSession
import com.wilddeck.app.model.CombatUnit
import com.wilddeck.app.model.Deck
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@Composable
fun CombatScreen(
    session: CombatSession?,
    effects: List<CombatEffect>,
    effectSequence: Long,
    points: Int,
    decks: List<Deck>,
    ownedCards: List<AnimalCard>,
    lockedFrames: List<CardFrame>,
    reducedMotion: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    frameCost: (String) -> Int,
    onStart: (String?) -> Unit,
    onAction: (String, String) -> Unit,
    onNextRound: () -> Unit,
    onEndRun: () -> Unit,
    onUnlockFrame: (String) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit
) {
    AnimatedContent(targetState = session, label = "combat-mode") { combat ->
        if (combat == null) {
            CombatLobby(
                points, decks, ownedCards, lockedFrames, reducedMotion, soundEnabled, hapticsEnabled,
                frameCost, onStart, onUnlockFrame, onReducedMotion, onSound, onHaptics
            )
        } else {
            CombatBoard(
                combat, effects, effectSequence, points, reducedMotion, soundEnabled, hapticsEnabled,
                onAction, onNextRound, onEndRun
            )
        }
    }
}

@Composable
private fun CombatLobby(
    points: Int,
    decks: List<Deck>,
    ownedCards: List<AnimalCard>,
    lockedFrames: List<CardFrame>,
    reducedMotion: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    frameCost: (String) -> Int,
    onStart: (String?) -> Unit,
    onUnlockFrame: (String) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit
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
            PressScaleButton(onClick = { onStart(deck.id) }) {
                Text("${deck.name} · ${deck.cardIds.size} cards · ×${formatCombatMultiplier(deck.symbiosisMultiplier)}")
            }
        }
        OutlinedButton(
            onClick = { onStart(null) },
            enabled = ownedCards.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Use first ${minOf(5, ownedCards.size)} collected creatures") }
        if (ownedCards.isEmpty()) Text("Earn your first creature through Animal Trivia to begin.")

        Text("Buy frames", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        lockedFrames.forEach { frame ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.clip(CircleShape).background(Color(frame.colorArgb)).padding(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(frame.name, fontWeight = FontWeight.Bold)
                        Text(frame.borderStyle, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onUnlockFrame(frame.id) }, enabled = points >= frameCost(frame.id)) {
                        Text("${frameCost(frame.id)} pts")
                    }
                }
            }
        }
        if (lockedFrames.isEmpty()) Text("Every frame has been unlocked.")

        Text("Effects & accessibility", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingToggle("Sound effects", soundEnabled, onSound)
        SettingToggle("Haptic feedback", hapticsEnabled, onHaptics)
        SettingToggle("Reduced motion", reducedMotion, onReducedMotion)
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun CombatBoard(
    session: CombatSession,
    effects: List<CombatEffect>,
    effectSequence: Long,
    points: Int,
    reducedMotion: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onAction: (String, String) -> Unit,
    onNextRound: () -> Unit,
    onEndRun: () -> Unit
) {
    val targetBounds = remember { mutableStateMapOf<String, Rect>() }
    var inspectedUnit by remember { mutableStateOf<CombatUnit?>(null) }
    var activeEffects by remember { mutableStateOf(emptyList<CombatEffect>()) }
    val haptics = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    LaunchedEffect(effectSequence) {
        if (effects.isEmpty()) return@LaunchedEffect
        activeEffects = effects
        if (hapticsEnabled) {
            haptics.performHapticFeedback(
                if (effects.any { it.type == CombatEffectType.DAMAGE || it.type == CombatEffectType.DEFEAT })
                    HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
        if (soundEnabled) {
            tone.startTone(toneFor(effects), if (reducedMotion) 80 else 180)
        }
        delay(if (reducedMotion) 250 else 900)
        activeEffects = emptyList()
    }

    Box(Modifier.fillMaxSize()) {
        BiomeBackground(session, reducedMotion)
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
                AnimatedContent(points, label = "points") { value ->
                    Text("$value pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            CombatTeamRow(
                title = "Enemies · long-press for details",
                units = session.enemyUnits,
                enemy = true,
                activeEffects = activeEffects,
                targetBounds = targetBounds,
                reducedMotion = reducedMotion,
                onInspect = { inspectedUnit = it },
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
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
                    session.playerUnits.forEach { unit ->
                        val validIds = if (unit.card.combatRole == CombatRole.SUPPORT) {
                            session.playerUnits.filter { it.isAlive }.map { it.instanceId }.toSet()
                        } else {
                            session.enemyUnits.filter { it.isAlive }.map { it.instanceId }.toSet()
                        }
                        DraggableCombatTile(
                            unit = unit,
                            effect = activeEffects.lastOrNull { it.targetId == unit.instanceId || it.sourceId == unit.instanceId },
                            allTargetBounds = targetBounds,
                            validTargetIds = validIds,
                            enabled = unit.isAlive && !unit.hasActed && !session.isDefeated && !session.isRoundCleared &&
                                activeEffects.isEmpty(),
                            reducedMotion = reducedMotion,
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

            AnimatedVisibility(
                visible = session.isRoundCleared || session.isDefeated,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                if (session.isRoundCleared) {
                    Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                        Text("Enter Round ${session.round + 1}")
                    }
                } else {
                    Button(onClick = onEndRun, modifier = Modifier.fillMaxWidth()) { Text("Return to Camp") }
                }
            }
            TextButton(onClick = onEndRun, modifier = Modifier.fillMaxWidth()) { Text("End Run") }
        }

        activeEffects.lastOrNull { it.type == CombatEffectType.ROUND_CLEAR || it.type == CombatEffectType.ROUND_START }
            ?.let { RoundBanner(it.label, reducedMotion) }
        activeEffects.lastOrNull { it.type == CombatEffectType.POINT }?.let { PointReward(reducedMotion) }
    }

    inspectedUnit?.let { unit ->
        CombatCardDialog(unit, reducedMotion) { inspectedUnit = null }
    }
}

@Composable
private fun CombatTeamRow(
    title: String,
    units: List<CombatUnit>,
    enemy: Boolean,
    activeEffects: List<CombatEffect>,
    targetBounds: MutableMap<String, Rect>,
    reducedMotion: Boolean,
    onInspect: (CombatUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            units.forEach { unit ->
                AnimatedVisibility(
                    visible = unit.isAlive || activeEffects.any {
                        it.type == CombatEffectType.DEFEAT && it.targetId == unit.instanceId
                    },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    enter = fadeIn() + scaleIn(initialScale = 0.7f),
                    exit = fadeOut(tween(if (reducedMotion) 80 else 350)) + scaleOut(targetScale = 0.65f)
                ) {
                    CombatTile(
                        unit = unit,
                        enemy = enemy,
                        effect = activeEffects.lastOrNull { it.targetId == unit.instanceId || it.sourceId == unit.instanceId },
                        reducedMotion = reducedMotion,
                        highlighted = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { targetBounds[unit.instanceId] = it.boundsInRoot() }
                            .pointerInput(unit.instanceId) {
                                detectTapGestures(onLongPress = { onInspect(unit) })
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableCombatTile(
    unit: CombatUnit,
    effect: CombatEffect?,
    allTargetBounds: Map<String, Rect>,
    validTargetIds: Set<String>,
    enabled: Boolean,
    reducedMotion: Boolean,
    onDrop: (String) -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember(unit.instanceId) { mutableStateOf(Offset.Zero) }
    var origin by remember(unit.instanceId) { mutableStateOf(Rect.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val animatedOffset by animateOffsetAsState(
        targetValue = dragOffset,
        animationSpec = if (dragging) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "drag-snap"
    )
    val rotation = if (reducedMotion) 0f else (animatedOffset.x / 24f).coerceIn(-8f, 8f)
    val hoveredTarget = if (dragging) {
        val point = origin.center + animatedOffset
        allTargetBounds.entries.firstOrNull { it.value.contains(point) }?.key
    } else null

    CombatTile(
        unit = unit,
        enemy = false,
        effect = effect,
        reducedMotion = reducedMotion,
        highlighted = hoveredTarget in validTargetIds,
        modifier = modifier
            .onGloballyPositioned { if (!dragging) origin = it.boundsInRoot() }
            .zIndex(if (dragging) 20f else 0f)
            .graphicsLayer {
                translationX = animatedOffset.x
                translationY = animatedOffset.y
                scaleX = if (dragging) 1.08f else 1f
                scaleY = if (dragging) 1.08f else 1f
                rotationZ = rotation
                shadowElevation = if (dragging) 28f else 5f
                alpha = if (unit.hasActed) 0.55f else 1f
            }
            .pointerInput(unit.instanceId) {
                detectTapGestures(onLongPress = { onInspect() })
            }
            .pointerInput(unit.instanceId, enabled, validTargetIds) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            val dropPoint = origin.center + animatedOffset
                            allTargetBounds.entries
                                .firstOrNull { it.key in validTargetIds && it.value.contains(dropPoint) }
                                ?.key?.let(onDrop)
                            dragging = false
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            dragging = false
                            dragOffset = Offset.Zero
                        },
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
private fun CombatTile(
    unit: CombatUnit,
    enemy: Boolean,
    effect: CombatEffect?,
    reducedMotion: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val roleColor = if (unit.card.combatRole == CombatRole.SUPPORT) Color(0xFF287B72) else Color(0xFF9B3D32)
    val healthProgress by animateFloatAsState(
        targetValue = unit.currentHealth.toFloat() / unit.maxHealth.coerceAtLeast(1),
        animationSpec = tween(if (reducedMotion) 80 else 550, easing = FastOutSlowInEasing),
        label = "health"
    )
    val impact = effect?.type == CombatEffectType.DAMAGE
    val glow = effect?.type in setOf(CombatEffectType.HEAL, CombatEffectType.SHIELD, CombatEffectType.EMPOWER)
    val shake by animateFloatAsState(
        targetValue = if (impact && !reducedMotion) 7f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "impact"
    )
    val borderColor = when {
        highlighted -> Color(0xFF51D88A)
        glow -> effectColor(effect)
        enemy -> MaterialTheme.colorScheme.error
        else -> roleColor
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                translationX = if (impact) shake else 0f
                scaleX = if (glow && !reducedMotion) 1.04f else 1f
                scaleY = if (glow && !reducedMotion) 1.04f else 1f
            }
            .border(if (highlighted || glow) 4.dp else 2.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(unit.card.imageEmoji, fontSize = 27.sp)
                Text(unit.card.name, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2,
                    style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { healthProgress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
                    color = healthColor(healthProgress)
                )
                Text("HP ${unit.currentHealth}/${unit.maxHealth} · DMG ${unit.damage}",
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                if (unit.shield > 0) Text("◈ ${unit.shield}", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall)
                Text(unit.card.combatRole.name, color = roleColor, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold)
                Text(unit.card.ability.name, style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center, maxLines = 2)
            }
            EffectVisual(effect, unit.card.id, reducedMotion)
        }
    }
}

@Composable
private fun EffectVisual(effect: CombatEffect?, animalId: String, reducedMotion: Boolean) {
    if (effect == null) return
    var visible by remember(effect) { mutableStateOf(true) }
    LaunchedEffect(effect) {
        delay(if (reducedMotion) 180 else 750)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (effect.type) {
                CombatEffectType.DAMAGE -> Text(effect.label, color = Color(0xFFFF5A55), fontSize = 25.sp,
                    fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopCenter))
                CombatEffectType.HEAL -> Text("✚ ${effect.label}", color = Color(0xFF31C86A), fontSize = 22.sp,
                    fontWeight = FontWeight.Black)
                CombatEffectType.SHIELD -> Text("◈", color = Color(0xFF55C7FF), fontSize = 54.sp)
                CombatEffectType.EMPOWER -> Text("✦ ${effect.label}", color = Color(0xFFFFC83D), fontSize = 22.sp,
                    fontWeight = FontWeight.Black)
                CombatEffectType.STUN -> Text("✦ ✦", color = Color(0xFFFFD34E), fontSize = 24.sp)
                CombatEffectType.TAUNT -> Text("!", color = Color(0xFFFF8A45), fontSize = 42.sp, fontWeight = FontWeight.Black)
                CombatEffectType.ATTACK -> Text(animalEffectGlyph(animalId, effect.abilityType), fontSize = 40.sp)
                CombatEffectType.DEFEAT -> Text("✕", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Black)
                else -> Unit
            }
        }
    }
}

@Composable
private fun BiomeBackground(session: CombatSession, reducedMotion: Boolean) {
    val habitat = session.enemyUnits.firstOrNull()?.card?.habitat.orEmpty().lowercase()
    val colors = when {
        "ocean" in habitat || "reef" in habitat || "river" in habitat -> listOf(Color(0xFF0B5E78), Color(0xFF6EC6C5))
        "arctic" in habitat || "tundra" in habitat -> listOf(Color(0xFF8DBBC8), Color(0xFFE8F6F8))
        "forest" in habitat -> listOf(Color(0xFF1D5638), Color(0xFF8CB56D))
        "desert" in habitat || "savanna" in habitat -> listOf(Color(0xFFB77532), Color(0xFFF2D28B))
        else -> listOf(Color(0xFF345B45), Color(0xFFC8B77B))
    }
    val transition = rememberInfiniteTransition(label = "biome")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(7000), RepeatMode.Reverse),
        label = "drift"
    )
    Canvas(Modifier.fillMaxSize().background(colors.first())) {
        drawRect(colors[1].copy(alpha = 0.45f))
        repeat(if (reducedMotion) 4 else 12) { index ->
            val x = ((index * 83f + drift * size.width) % size.width)
            val y = ((index * 137f + drift * size.height * 0.35f) % size.height)
            drawCircle(Color.White.copy(alpha = 0.12f), 6f + index % 4 * 3f, Offset(x, y))
        }
    }
}

@Composable
private fun RoundBanner(text: String, reducedMotion: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(text, Modifier.padding(horizontal = 34.dp, vertical = 18.dp),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PointReward(reducedMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "point")
    val offset by transition.animateFloat(
        0f, if (reducedMotion) 0f else -70f,
        infiniteRepeatable(tween(700), RepeatMode.Restart), label = "point-fly"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("+1 POINT", color = Color(0xFFFFD34E), fontSize = 28.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer { translationY = offset })
    }
}

@Composable
private fun CombatCardDialog(unit: CombatUnit, reducedMotion: Boolean, onDismiss: () -> Unit) {
    var tilt by remember { mutableStateOf(Offset.Zero) }
    val scale by animateFloatAsState(1f, tween(if (reducedMotion) 1 else 280), label = "inspect")
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).blur(if (reducedMotion) 0.dp else 3.dp)) {
            Card(
                Modifier.fillMaxSize().padding(14.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationY = if (reducedMotion) 0f else tilt.x.coerceIn(-8f, 8f)
                        rotationX = if (reducedMotion) 0f else -tilt.y.coerceIn(-8f, 8f)
                        cameraDistance = 12f * density
                    }
                    .pointerInput(unit.instanceId) {
                        detectDragGestures(
                            onDragEnd = { tilt = Offset.Zero },
                            onDragCancel = { tilt = Offset.Zero }
                        ) { change, amount ->
                            change.consume()
                            tilt += Offset(amount.x / 10f, amount.y / 10f)
                        }
                    },
                shape = RoundedCornerShape(22.dp)
            ) {
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
                    Text("${unit.card.combatRole.name.lowercase().replaceFirstChar(Char::uppercase)} · " +
                        "Combat HP ${unit.currentHealth}/${unit.maxHealth} · Damage ${unit.damage}",
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    InspectionBlock("Animal", unit.card.description)
                    InspectionBlock("Ability: ${unit.card.ability.name}", unit.card.ability.description)
                    InspectionBlock("Habitat", unit.card.habitat)
                    InspectionBlock("Diet", unit.card.food)
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Return to battle") }
                }
            }
        }
    }
}

@Composable
private fun InspectionBlock(title: String, description: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description)
        }
    }
}

@Composable
private fun PressScaleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), content = { content() })
}

private fun healthColor(progress: Float): Color = when {
    progress > 0.6f -> Color(0xFF35A866)
    progress > 0.3f -> Color(0xFFE0A52E)
    else -> Color(0xFFD4473D)
}

private fun effectColor(effect: CombatEffect?): Color = when (effect?.type) {
    CombatEffectType.HEAL -> Color(0xFF39C86A)
    CombatEffectType.SHIELD -> Color(0xFF55C7FF)
    CombatEffectType.EMPOWER -> Color(0xFFFFC83D)
    else -> Color.White
}

private fun animalEffectGlyph(animalId: String, type: AbilityType?): String = when (animalId) {
    "pistol_shrimp" -> "◎"
    "eagle" -> "〽"
    "crocodile" -> "≋"
    "lion" -> "◉"
    "elephant" -> "⬇"
    "clownfish" -> "〰"
    "rabbit" -> "⌁"
    "shark" -> "➤"
    else -> when (type) {
        AbilityType.STUN -> "✦"
        AbilityType.HEAL -> "✚"
        AbilityType.SHIELD -> "◈"
        else -> "✹"
    }
}

private fun toneFor(effects: List<CombatEffect>): Int = when {
    effects.any { it.type == CombatEffectType.ROUND_CLEAR || it.type == CombatEffectType.POINT } ->
        ToneGenerator.TONE_PROP_ACK
    effects.any { it.type == CombatEffectType.HEAL || it.type == CombatEffectType.SHIELD } ->
        ToneGenerator.TONE_PROP_BEEP2
    effects.any { it.type == CombatEffectType.DEFEAT } -> ToneGenerator.TONE_PROP_NACK
    else -> ToneGenerator.TONE_PROP_BEEP
}

private fun formatCombatMultiplier(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
