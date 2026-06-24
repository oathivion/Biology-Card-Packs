package com.wilddeck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wilddeck.app.R
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame
import com.wilddeck.app.model.FrameEffect
import kotlin.math.sin
import kotlin.math.absoluteValue

@Composable
fun AnimalCardView(
    card: AnimalCard,
    frame: CardFrame,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showStats: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val frameColor = Color(frame.colorArgb)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .then(clickModifier)
            .border(if (compact) 5.dp else 8.dp, frameColor, RoundedCornerShape(18.dp))
            .semantics { stateDescription = "Frame: ${frame.id}" }
            .testTag("animal_card_${card.id}"),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 10.dp else 16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (showStats) Arrangement.SpaceBetween else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showStats) {
                    StatBadge("DANGER", card.danger, Color(0xFF9B342E), "danger_${card.id}", compact)
                }
                Text(
                    text = card.name,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = if (showStats) Modifier.weight(1f).padding(horizontal = 8.dp) else Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (showStats) {
                    StatBadge("HEALTH", card.health, Color(0xFF286B45), "health_${card.id}", compact)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(frameColor.copy(alpha = 0.28f), MaterialTheme.colorScheme.surfaceVariant)
                        )
                    )
                    .testTag("animal_image_${card.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (compact) {
                    Text(
                        text = card.imageEmoji,
                        fontSize = 58.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    AnimalPhoto(
                        card = card,
                        modifier = Modifier.fillMaxSize(),
                        fallbackFontSize = 92.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(frameColor.copy(alpha = 0.1f))
                    .padding(if (compact) 8.dp else 12.dp)
                    .testTag("description_${card.id}")
            ) {
                Text(
                    card.species.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = frameColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    card.description,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    maxLines = if (compact) 4 else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            }
            AnimatedFrameEffect(frame.effect, frameColor, Modifier.fillMaxSize())
        }
    }
}

@Composable
fun AnimalPhoto(
    card: AnimalCard,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackFontSize: androidx.compose.ui.unit.TextUnit = 58.sp
) {
    val imageResource = animalImageResourceId(card.id)
    if (imageResource != null) {
        Image(
            painter = painterResource(imageResource),
            contentDescription = "${card.name} photo",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = card.imageEmoji,
                fontSize = fallbackFontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun animalImageResourceId(cardId: String): Int? = when (cardId) {
    "lion" -> R.drawable.animal_lion
    "elephant" -> R.drawable.animal_elephant
    "crocodile" -> R.drawable.animal_crocodile
    "wolf" -> R.drawable.animal_wolf
    "rabbit" -> R.drawable.animal_rabbit
    "eagle" -> R.drawable.animal_eagle
    "shark" -> R.drawable.animal_shark
    "clownfish" -> R.drawable.animal_clownfish
    "anemone" -> R.drawable.animal_anemone
    "rhino" -> R.drawable.animal_rhino
    "oxpecker" -> R.drawable.animal_oxpecker
    "pistol_shrimp" -> R.drawable.animal_pistol_shrimp
    "goby" -> R.drawable.animal_goby
    "remora" -> R.drawable.animal_remora
    "blue_whale" -> R.drawable.animal_blue_whale
    "octopus" -> R.drawable.animal_octopus
    "green_sea_turtle" -> R.drawable.animal_green_sea_turtle
    "manta_ray" -> R.drawable.animal_manta_ray
    "bottlenose_dolphin" -> R.drawable.animal_bottlenose_dolphin
    "seahorse" -> R.drawable.animal_seahorse
    "squid" -> R.drawable.animal_squid
    "moray_eel" -> R.drawable.animal_moray_eel
    "humpback_whale" -> R.drawable.animal_humpback_whale
    "jaguar" -> R.drawable.animal_jaguar
    "gorilla" -> R.drawable.animal_gorilla
    "orangutan" -> R.drawable.animal_orangutan
    "toucan" -> R.drawable.animal_toucan
    "poison_dart_frog" -> R.drawable.animal_poison_dart_frog
    "sloth" -> R.drawable.animal_sloth
    "tapir" -> R.drawable.animal_tapir
    "anaconda" -> R.drawable.animal_anaconda
    "leafcutter_ant" -> R.drawable.animal_leafcutter_ant
    "polar_bear" -> R.drawable.animal_polar_bear
    "emperor_penguin" -> R.drawable.animal_emperor_penguin
    "walrus" -> R.drawable.animal_walrus
    "arctic_fox" -> R.drawable.animal_arctic_fox
    "musk_ox" -> R.drawable.animal_musk_ox
    "snow_leopard" -> R.drawable.animal_snow_leopard
    "beluga_whale" -> R.drawable.animal_beluga_whale
    "reindeer" -> R.drawable.animal_reindeer
    "harp_seal" -> R.drawable.animal_harp_seal
    "dromedary_camel" -> R.drawable.animal_dromedary_camel
    "fennec_fox" -> R.drawable.animal_fennec_fox
    "emperor_scorpion" -> R.drawable.animal_emperor_scorpion
    "gila_monster" -> R.drawable.animal_gila_monster
    "meerkat" -> R.drawable.animal_meerkat
    "roadrunner" -> R.drawable.animal_roadrunner
    "sidewinder" -> R.drawable.animal_sidewinder
    "addax" -> R.drawable.animal_addax
    "frilled_lizard" -> R.drawable.animal_frilled_lizard
    else -> null
}

@Composable
private fun AnimatedFrameEffect(effect: FrameEffect, frameColor: Color, modifier: Modifier = Modifier) {
    if (effect == FrameEffect.NONE) return
    val transition = rememberInfiniteTransition(label = "frame-${effect.name}")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (effect) {
                    FrameEffect.LIGHTNING -> 900
                    FrameEffect.SPARKLE, FrameEffect.FROST -> 1800
                    else -> 3200
                }
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame-progress"
    )
    Canvas(modifier) {
        when (effect) {
            FrameEffect.LIGHT_SWEEP -> {
                val y = size.height * progress
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.58f), Color.Transparent),
                        startY = y - 55f,
                        endY = y + 55f
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = size
                )
                drawLine(Color.White.copy(alpha = 0.78f), Offset(0f, y), Offset(size.width, y), 2.5f)
            }
            FrameEffect.SPARKLE -> repeat(12) { index ->
                val phase = (progress + index / 12f) % 1f
                val x = size.width * ((index * 37 % 97) / 97f)
                val y = size.height * ((index * 61 % 101) / 101f)
                val radius = 1.5f + 5f * sin(phase * Math.PI).toFloat().coerceAtLeast(0f)
                drawCircle(Color.White.copy(alpha = 0.25f + phase * 0.55f), radius, Offset(x, y))
            }
            FrameEffect.AURORA -> {
                val x = size.width * progress
                drawRect(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF72F1B8).copy(alpha = 0.22f),
                            Color(0xFFB98CFF).copy(alpha = 0.27f),
                            Color.Transparent
                        ),
                        startX = x - size.width,
                        endX = x + size.width
                    )
                )
            }
            FrameEffect.EMBERS -> repeat(14) { index ->
                val phase = (progress + index / 14f) % 1f
                val x = size.width * ((index * 29 % 89) / 89f)
                val y = size.height * (1f - phase)
                drawCircle(Color(0xFFFF7A32).copy(alpha = 1f - phase), 2f + index % 3, Offset(x, y))
            }
            FrameEffect.BUBBLES -> repeat(10) { index ->
                val phase = (progress + index / 10f) % 1f
                val x = size.width * ((index * 43 % 91) / 91f)
                val y = size.height * (1f - phase)
                drawCircle(
                    Color.White.copy(alpha = 0.35f),
                    4f + index % 4 * 2f,
                    Offset(x, y),
                    style = Stroke(1.7f)
                )
            }
            FrameEffect.LEAVES -> repeat(8) { index ->
                val phase = (progress + index / 8f) % 1f
                val x = size.width * ((index * 31 % 83) / 83f) + sin(phase * 6.28f) * 15f
                val y = size.height * phase
                drawOval(
                    Color(0xFF85C66A).copy(alpha = 0.55f),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(8f, 14f)
                )
            }
            FrameEffect.FROST -> {
                repeat(18) { index ->
                    val x = if (index % 2 == 0) 4f else size.width - 4f
                    val y = size.height * ((index * 17 % 97) / 97f)
                    val pulse = 0.25f + 0.55f * sin((progress + index / 18f) * 6.28f).absoluteValue
                    drawCircle(Color.White.copy(alpha = pulse), 2f + index % 3, Offset(x, y))
                }
                drawRoundRect(Color.White.copy(alpha = 0.2f), style = Stroke(4f), cornerRadius = CornerRadius(18f))
            }
            FrameEffect.LIGHTNING -> {
                val alpha = if (progress < 0.15f || progress in 0.48f..0.58f) 0.85f else 0.12f
                val path = Path().apply {
                    moveTo(size.width * 0.12f, 0f)
                    lineTo(size.width * 0.28f, size.height * 0.24f)
                    lineTo(size.width * 0.18f, size.height * 0.43f)
                    lineTo(size.width * 0.38f, size.height * 0.7f)
                    lineTo(size.width * 0.3f, size.height)
                }
                drawPath(path, Color(0xFFAEC8FF).copy(alpha = alpha), style = Stroke(3f, cap = StrokeCap.Round))
            }
            FrameEffect.RAIN -> repeat(18) { index ->
                val phase = (progress + index / 18f) % 1f
                val x = size.width * ((index * 23 % 97) / 97f)
                val y = size.height * phase
                drawLine(
                    Color(0xFFBDE8FF).copy(alpha = 0.35f),
                    Offset(x, y),
                    Offset(x - 10f, y + 24f),
                    1.5f
                )
            }
            FrameEffect.STARFIELD -> {
                drawRect(Color(0xFF130D2E).copy(alpha = 0.16f))
                repeat(20) { index ->
                    val x = size.width * ((index * 47 % 101) / 101f)
                    val y = size.height * ((index * 67 % 103) / 103f)
                    val twinkle = 0.2f + 0.8f * sin((progress + index / 20f) * 6.28f).absoluteValue
                    drawCircle(Color.White.copy(alpha = twinkle), 1.2f + index % 3, Offset(x, y))
                }
            }
            FrameEffect.ROUND_GROWTH -> {
                val pulse = 0.25f + 0.45f * sin(progress * 6.28f).absoluteValue
                drawRect(Color(0xFFFFFF7A).copy(alpha = pulse * 0.45f))
                drawRoundRect(Color(0xFFFFFFA8).copy(alpha = pulse), style = Stroke(8f), cornerRadius = CornerRadius(22f))
            }
            FrameEffect.WHALE_SONG, FrameEffect.DOLPHIN_ECHO -> repeat(5) { index ->
                val phase = (progress + index / 5f) % 1f
                drawCircle(
                    Color(0xFFBFEFFF).copy(alpha = 1f - phase),
                    size.minDimension * phase * 0.65f,
                    Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(2f)
                )
            }
            FrameEffect.TENTACLE -> repeat(5) { index ->
                val y = size.height * (index + 1) / 6f
                val path = Path().apply {
                    moveTo(0f, y)
                    cubicTo(size.width * 0.25f, y - 35f, size.width * 0.35f, y + 35f, size.width * (0.55f + progress * 0.25f), y)
                }
                drawPath(path, Color(0xFFC19AF0).copy(alpha = 0.34f), style = Stroke(5f, cap = StrokeCap.Round))
            }
            FrameEffect.TURTLE_SHELL -> {
                repeat(4) { index ->
                    val x = size.width * (index + 1) / 5f
                    drawLine(Color(0xFF9ACB82).copy(alpha = 0.42f), Offset(x, 0f), Offset(x, size.height), 2f)
                }
                repeat(5) { index ->
                    val y = size.height * (index + 1) / 6f
                    drawLine(Color(0xFF9ACB82).copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 2f)
                }
            }
            FrameEffect.MANTA_CURRENT -> repeat(8) { index ->
                val y = size.height * ((progress + index / 8f) % 1f)
                drawLine(Color(0xFFA9F3FF).copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y + sin(progress * 6.28f) * 18f), 3f)
            }
            FrameEffect.JUNGLE_VINES -> repeat(6) { index ->
                val x = size.width * (index + 1) / 7f
                val path = Path().apply {
                    moveTo(x, 0f)
                    cubicTo(x - 24f, size.height * 0.25f, x + 24f, size.height * 0.55f, x, size.height)
                }
                drawPath(path, Color(0xFF64B56A).copy(alpha = 0.36f), style = Stroke(4f, cap = StrokeCap.Round))
            }
            FrameEffect.POISON_GLOW -> {
                val alpha = 0.18f + 0.22f * sin(progress * 6.28f).absoluteValue
                drawRect(Color(0xFF2DFF74).copy(alpha = alpha))
                repeat(10) { index ->
                    drawCircle(Color(0xFFFFE85A).copy(alpha = 0.38f), 4f + index % 4, Offset(size.width * ((index * 37 % 91) / 91f), size.height * ((index * 53 % 89) / 89f)))
                }
            }
            FrameEffect.APE_STRENGTH -> repeat(4) { index ->
                val inset = 10f + index * 20f + progress * 15f
                drawRoundRect(
                    Color(0xFFE2E2E2).copy(alpha = 0.18f),
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(5f),
                    cornerRadius = CornerRadius(18f)
                )
            }
            FrameEffect.ARCTIC_AURA -> {
                drawRect(Color(0xFFDDFBFF).copy(alpha = 0.16f))
                repeat(12) { index ->
                    val x = size.width * ((index * 41 % 97) / 97f)
                    val y = size.height * ((progress + index / 12f) % 1f)
                    drawCircle(Color.White.copy(alpha = 0.5f), 2f + index % 3, Offset(x, y))
                }
            }
            FrameEffect.TUSK_GUARD -> {
                drawArc(Color(0xFFFFF2D0).copy(alpha = 0.46f), 210f, 110f, false, style = Stroke(7f, cap = StrokeCap.Round))
                drawArc(Color(0xFFFFF2D0).copy(alpha = 0.46f), -30f, 110f, false, style = Stroke(7f, cap = StrokeCap.Round))
            }
            FrameEffect.SNOW_TRACKS -> repeat(8) { index ->
                val phase = (progress + index / 8f) % 1f
                val x = size.width * ((index * 29 % 83) / 83f)
                val y = size.height * phase
                drawOval(Color.White.copy(alpha = 0.55f), topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(7f, 13f))
            }
            FrameEffect.DESERT_HEAT -> repeat(7) { index ->
                val x = size.width * (index + 1) / 8f
                val path = Path().apply {
                    moveTo(x, size.height)
                    cubicTo(x - 16f, size.height * 0.7f, x + 16f, size.height * 0.35f, x + sin(progress * 6.28f) * 12f, 0f)
                }
                drawPath(path, Color(0xFFFFD17A).copy(alpha = 0.3f), style = Stroke(3f, cap = StrokeCap.Round))
            }
            FrameEffect.SCORPION_STING -> {
                val path = Path().apply {
                    moveTo(size.width * 0.85f, size.height)
                    cubicTo(size.width * 0.55f, size.height * 0.7f, size.width * 0.88f, size.height * 0.45f, size.width * (0.72f + progress * 0.18f), size.height * 0.2f)
                    lineTo(size.width * 0.79f, size.height * 0.25f)
                }
                drawPath(path, Color(0xFFFF9A4A).copy(alpha = 0.58f), style = Stroke(5f, cap = StrokeCap.Round))
            }
            FrameEffect.NONE -> Unit
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: Int,
    color: Color,
    tag: String,
    compact: Boolean
) {
    Column(
        modifier = Modifier
            .size(if (compact) 52.dp else 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = if (compact) 8.sp else 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value.toString(),
            color = Color.White,
            fontSize = if (compact) 20.sp else 27.sp,
            fontWeight = FontWeight.Black
        )
    }
}
