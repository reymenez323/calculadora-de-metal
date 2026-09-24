@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui.suppliers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.local.DimensionsCodec
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials
import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.domain.calculation.StockPricing
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.domain.model.Shape
import com.reymildo.calculadoradelmetal.ui.common.DimensionRow
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.MoneyField
import com.reymildo.calculadoradelmetal.ui.common.RoundTubeInnerDiameterRow
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.ShapeGlyph
import com.reymildo.calculadoradelmetal.ui.common.ShapeIsoDiagram
import com.reymildo.calculadoradelmetal.ui.common.shapeNameRes
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

private data class FormTarget(val supplierId: Long, val existing: MaterialEntity?)

@Composable
fun SuppliersScreen(
    suppliers: List<SupplierWithMaterials>,
    settings: AppSettings,
    onSaveMaterial: (
        supplierId: Long,
        existing: MaterialEntity?,
        name: String,
        shape: Shape,
        dimensions: Map<DimensionType, DimensionValue>,
        stockPrice: Double,
    ) -> Unit,
    onDeleteMaterial: (MaterialEntity) -> Unit,
    onAddSupplier: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var formTarget by remember { mutableStateOf<FormTarget?>(null) }
    var supplierDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        items(count = suppliers.size, key = { suppliers[it].supplier.id }) { index ->
            val group = suppliers[index]
            val expanded = expandedId == group.supplier.id
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val countLabel = if (group.materials.size == 1) {
                        stringResource(R.string.sup_material_count_one)
                    } else {
                        stringResource(R.string.sup_material_count_other, group.materials.size)
                    }
                    val builtInLabel = stringResource(R.string.sup_builtin)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(group.supplier.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (group.supplier.isBuiltIn) "$countLabel · $builtInLabel" else countLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        expandedId = if (expanded) null else group.supplier.id
                    }) {
                        Text(if (expanded) "⌃" else "⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        group.materials.forEach { entity ->
                            MaterialRowCard(
                                entity = entity,
                                settings = settings,
                                onEdit = { formTarget = FormTarget(group.supplier.id, entity) },
                                onDelete = { onDeleteMaterial(entity) },
                            )
                        }
                        OutlinedButton(
                            onClick = { formTarget = FormTarget(group.supplier.id, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = stringResource(R.string.sup_add_material),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
        item(key = "add-supplier") {
            OutlinedButton(
                onClick = { supplierDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = stringResource(R.string.sup_add_supplier),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    formTarget?.let { target ->
        MaterialFormSheet(
            target = target,
            settings = settings,
            onDismiss = { formTarget = null },
            onSave = { name, shape, dims, price ->
                onSaveMaterial(target.supplierId, target.existing, name, shape, dims, price)
                expandedId = target.supplierId
                formTarget = null
            },
        )
    }

    if (supplierDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { supplierDialog = false },
            title = { Text(stringResource(R.string.sup_new_supplier)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.sup_supplier_name)) },
                    shape = RoundedCornerShape(13.dp),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) onAddSupplier(name.trim())
                        supplierDialog = false
                    },
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { supplierDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun MaterialRowCard(
    entity: MaterialEntity,
    settings: AppSettings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = runCatching { Shape.fromId(entity.stockShapeId) }.getOrNull()
    val dims = runCatching { DimensionsCodec.decode(entity.stockDimensionsJson) }.getOrDefault(emptyMap())
    val configured = entity.costPerVolumeCm3 > 0.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            shape?.let {
                ShapeGlyph(
                    shape = it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entity.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = listOfNotNull(
                        shape?.let { stringResource(shapeNameRes(it)) },
                        Fmt.dimensions(shape?.requiredDimensions.orEmpty(), dims),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = NumberFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (configured) {
                        Fmt.money(entity.stockPrice, settings, 0)
                    } else {
                        stringResource(R.string.material_no_price)
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = NumberFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (configured) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (configured) {
                        Fmt.money(entity.costPerVolumeCm3, settings, 2) + "/cm³"
                    } else {
                        stringResource(R.string.material_tap_to_configure)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Text("✎", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!entity.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MaterialFormSheet(
    target: FormTarget,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (String, Shape, Map<DimensionType, DimensionValue>, Double) -> Unit,
) {
    val existing = target.existing
    val existingDims = remember(existing) {
        existing?.let { runCatching { DimensionsCodec.decode(it.stockDimensionsJson) }.getOrNull() }.orEmpty()
    }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var shape by remember {
        mutableStateOf(existing?.let { runCatching { Shape.fromId(it.stockShapeId) }.getOrNull() } ?: Shape.Plate)
    }
    var priceText by remember {
        mutableStateOf(existing?.stockPrice?.takeIf { it > 0.0 }?.let { Fmt.trimNumber(it) } ?: "")
    }
    val values = remember { mutableStateMapOf<DimensionType, String>() }
    val units = remember { mutableStateMapOf<DimensionType, LengthUnit>() }
    var shapeMenu by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    remember(shape) {
        shape.requiredDimensions.forEach { type ->
            if (!values.containsKey(type)) {
                values[type] = existingDims[type]?.value?.let { Fmt.trimNumber(it) } ?: ""
                units[type] = existingDims[type]?.unit ?: settings.defaultLengthUnit
            }
        }
        true
    }

    val dimensions: Map<DimensionType, DimensionValue> = shape.requiredDimensions.mapNotNull { type ->
        values[type]?.toDecimalOrNull()?.let { number ->
            type to DimensionValue(number, units[type] ?: settings.defaultLengthUnit)
        }
    }.toMap()

    val price = priceText.toDecimalOrNull() ?: 0.0
    val rate = if (dimensions.size == shape.requiredDimensions.size) {
        StockPricing.costPerVolumeCm3(shape, dimensions, price).getOrNull()
    } else {
        null
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text(
                text = stringResource(if (existing == null) R.string.form_new else R.string.form_edit),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError,
                label = { Text(stringResource(R.string.form_name)) },
                placeholder = { Text(stringResource(R.string.form_name_hint)) },
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.form_shape).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { shapeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    ShapeGlyph(
                        shape = shape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(shapeNameRes(shape)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = shapeMenu, onDismissRequest = { shapeMenu = false }) {
                    Shape.ALL.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(stringResource(shapeNameRes(candidate))) },
                            onClick = {
                                shape = candidate
                                candidate.requiredDimensions.forEach { type ->
                                    if (!values.containsKey(type)) {
                                        values[type] = ""
                                        units[type] = settings.defaultLengthUnit
                                    }
                                }
                                shapeMenu = false
                            },
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.dim_reference_title)) {
                ShapeIsoDiagram(shape = shape, modifier = Modifier.fillMaxWidth())
            }

            SectionCard(title = stringResource(R.string.form_stock_dims)) {
                shape.requiredDimensions.forEach { type ->
                    DimensionRow(
                        label = stringResource(com.reymildo.calculadoradelmetal.ui.common.dimensionNameRes(type)),
                        value = values[type] ?: "",
                        onValueChange = { values[type] = it },
                        unit = units[type] ?: settings.defaultLengthUnit,
                        onUnitChange = { units[type] = it },
                    )
                }
                if (shape == Shape.RoundTube) {
                    RoundTubeInnerDiameterRow(values = values, units = units)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.form_price).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    currencySymbol = settings.currencySymbol,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.form_rate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = rate?.takeIf { it > 0.0 }?.let { Fmt.money(it, settings, 3) } ?: "—",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = NumberFamily),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onSave(name.trim(), shape, dimensions, price)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(
                    text = stringResource(if (existing == null) R.string.form_save else R.string.form_save_changes),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
