package com.reymildo.calculadoradelmetal.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

/** Mosaico seleccionable con icono y texto: la base de los selectores visuales (operación, máquina, tipo). */
@Composable
fun ChoiceTile(
    glyph: Glyph,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) primary else MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlyphIcon(glyph, color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 34.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Reparte los elementos en filas de [columns] mosaicos del mismo ancho. */
@Composable
fun <T> TileGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    tile: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { tile(it, Modifier.weight(1f)) }
                repeat(columns - rowItems.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Etiqueta pequeña con color de énfasis (Convencional, CNC, De fábrica…). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
            color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Campo numérico con etiqueta arriba y sufijo de unidad; permite dejarlo vacío cuando el dato es opcional. */
@Composable
fun LabeledNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    integer: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            val ok = if (integer) candidate.all(Char::isDigit) else candidate.isValidDecimalInput()
            if (ok) onValueChange(candidate)
        },
        modifier = modifier,
        singleLine = true,
        label = { Text(label, maxLines = 1) },
        suffix = suffix?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = NumberFamily, fontWeight = FontWeight.Medium),
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
    )
}

/** Selector desplegable con etiqueta en mayúsculas. */
@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(selected, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false })
                }
            }
        }
    }
}

/** Botón con la unidad actual; al tocarlo lista las demás unidades de la misma magnitud. */
@Composable
fun UnitPicker(
    unit: MachUnit,
    suffix: String = "",
    modifier: Modifier = Modifier,
    onSelect: (MachUnit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(unit.symbol + suffix + " ⌄", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MachUnit.of(unit.quantity).forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate.symbol + suffix) },
                    onClick = { onSelect(candidate); expanded = false },
                )
            }
        }
    }
}
