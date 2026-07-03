package com.wilddeck.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wilddeck.app.model.AbilityType
import com.wilddeck.app.model.AnimalAbility
import com.wilddeck.app.model.CombatRole

@Composable
fun AbilityInfoButton(
    ability: AnimalAbility,
    role: CombatRole,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var open by remember(ability.id, role) { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true },
        shape = RoundedCornerShape(if (compact) 8.dp else 14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (compact) 0.58f else 0.86f)
    ) {
        Text(
            text = if (compact) ability.name else "Ability: ${ability.name}",
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 14.dp, vertical = if (compact) 4.dp else 10.dp),
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = if (compact) 2 else Int.MAX_VALUE
        )
    }
    if (open) {
        AbilityInfoDialog(
            ability = ability,
            role = role,
            onDismiss = { open = false }
        )
    }
}

@Composable
fun AbilityInfoDialog(
    ability: AnimalAbility,
    role: CombatRole,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ability.name, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AbilityInfoLine("Role", role.name.lowercase().replaceFirstChar(Char::uppercase))
                AbilityInfoLine("Type", ability.type.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase))
                AbilityInfoLine("Power", ability.power.toString())
                AbilityInfoLine("What it means", ability.description)
                AbilityInfoLine("Exact Wild Run effect", abilityMechanicText(ability, role))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
private fun AbilityInfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun abilityMechanicText(ability: AnimalAbility, role: CombatRole): String {
    val power = ability.power
    return when (ability.type) {
        AbilityType.STRIKE ->
            "When this attacker hits an enemy, it adds +$power extra damage if that enemy is already injured."
        AbilityType.TAUNT ->
            "When this attacker hits, it adds +$power damage, gains $power shield, and forces enemies to target it while taunt is active."
        AbilityType.HEAL ->
            "When used on a living ally, it restores $power health plus any support-power frame bonus, up to that ally's maximum health."
        AbilityType.SHIELD ->
            "When used on a living ally, it gives $power shield plus any support-power frame bonus. Shield blocks incoming damage before health is lost."
        AbilityType.PACK ->
            "When this attacker hits, it gains +$power damage for each other living ally on your team."
        AbilityType.AMBUSH ->
            "When this attacker hits an enemy at full health, it adds +$power bonus damage. If the target is already hurt, the bonus does not apply."
        AbilityType.STUN ->
            if (role == CombatRole.SUPPORT) {
                "When used on a living ally, it applies a stun-style support effect with power $power plus any support-power frame bonus."
            } else {
                "When this attacker hits, it stuns the target. A stunned unit loses its next action."
            }
        AbilityType.DODGE ->
            "When used on a living ally, it gives $power shield plus any support-power frame bonus, representing the ally dodging or avoiding the next hit."
        AbilityType.POISON ->
            "When this attacker hits, it deals its normal damage. The poison trait marks the hit as a harmful special strike for effects and visuals."
        AbilityType.EMPOWER ->
            "When used on a living ally, it increases that ally's damage by $power plus any support-power frame bonus for the rest of the round."
        AbilityType.SPLASH ->
            "When this attacker hits, the main target takes full damage. Every other living enemy also takes $power splash damage plus any attack-power frame bonus."
    }
}
