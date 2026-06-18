package com.wilddeck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wilddeck.app.model.AnimalCard
import com.wilddeck.app.model.CardFrame

@Composable
fun AnimalCardView(
    card: AnimalCard,
    frame: CardFrame,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(if (compact) 10.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge("DANGER", card.danger, Color(0xFF9B342E), "danger_${card.id}", compact)
                Text(
                    text = card.name,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                StatBadge("HEALTH", card.health, Color(0xFF286B45), "health_${card.id}", compact)
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
                Text(
                    text = card.imageEmoji,
                    fontSize = if (compact) 58.sp else 92.sp,
                    textAlign = TextAlign.Center
                )
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
