@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

/** Tarjeta blanca con encabezado en mayúsculas, el contenedor base de todas las pantallas. */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

/** Fila "Etiqueta …… [ 12,5 | in ▾ ]" — el patrón central de la app. */
@Composable
fun DimensionRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: LengthUnit,
    onUnitChange: (LengthUnit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    helper: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (helper != null) {
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        NumberFieldWithUnit(
            value = value,
            onValueChange = onValueChange,
            unit = unit,
            onUnitChange = onUnitChange,
            enabled = enabled,
            modifier = Modifier.width(176.dp),
        )
    }
}

@Composable
fun NumberFieldWithUnit(
    value: String,
    onValueChange: (String) -> Unit,
    unit: LengthUnit,
    onUnitChange: (LengthUnit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.sanitizeDecimal()) },
        modifier = modifier,
        enabled = enabled,
        placeholder = { Text("0", style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = NumberFamily,
            fontWeight = FontWeight.Medium,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        trailingIcon = {
            Box {
                TextButton(onClick = { if (enabled) expanded = true }) {
                    Text(
                        text = unit.symbol,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    LengthUnit.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.symbol) },
                            onClick = {
                                onUnitChange(candidate)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

/** Campo de dinero: prefijo con el símbolo de la moneda y cifra alineada a la derecha. */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.sanitizeDecimal()) },
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(placeholder, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
        prefix = {
            Text(
                text = currencySymbol,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = NumberFamily,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

/** − [ 4 ] + para la cantidad de piezas y los decimales. */
@Composable
fun Stepper(
    value: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilledTonalIconButton(onClick = onDecrement, modifier = Modifier.size(38.dp)) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        if (editable) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.sanitizeInt()) },
                modifier = Modifier.width(66.dp).height(50.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = NumberFamily,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(11.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = NumberFamily),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp),
            )
        }
        FilledTonalIconButton(onClick = onIncrement, modifier = Modifier.size(38.dp)) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Etiqueta de dato dentro de la tarjeta oscura de resultado. */
@Composable
fun ResultStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = NumberFamily),
            color = valueColor.copy(alpha = 0.92f),
        )
    }
}
