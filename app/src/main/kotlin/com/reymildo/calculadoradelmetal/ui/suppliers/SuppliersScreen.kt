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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.ui.common.FormScreenDialog
import com.reymildo.calculadoradelmetal.data.local.DimensionsCodec
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.SupplierEntity
import com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials
import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.domain.calculation.StockPricing
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.domain.model.Shape
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.ui.common.DimensionRow
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.MoneyField
import com.reymildo.calculadoradelmetal.ui.common.RoundTubeInnerDiameterRow
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.ShapeGlyph
import com.reymildo.calculadoradelmetal.ui.common.ShapeIsoDiagram
import com.reymildo.calculadoradelmetal.ui.common.dimensionNameRes
import com.reymildo.calculadoradelmetal.ui.common.shapeNameRes
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

private data class FormTarget(val supplierId: Long, val existing: MaterialEntity?)

@Composable
fun SuppliersScreen(
    suppliers: List<SupplierWithMaterials>,
    machiningMaterials: List<MachiningMaterialEntity>,
    settings: AppSettings,
    onSaveMaterial: suspend (
        supplierId: Long,
        existing: MaterialEntity?,
        name: String,
        shape: Shape,
        dimensions: Map<DimensionType, DimensionValue>,
        stockPrice: Double?,
        technicalMaterialId: String?,
    ) -> Result<Unit>,
    onDeleteMaterial: (MaterialEntity) -> Unit,
    onAddSupplier: (String) -> Unit,
    onRenameSupplier: (Long, String) -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var formTarget by remember { mutableStateOf<FormTarget?>(null) }
    var supplierDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MaterialEntity?>(null) }
    var renameSupplierTarget by remember { mutableStateOf<SupplierEntity?>(null) }
    var deleteSupplierTarget by remember { mutableStateOf<SupplierEntity?>(null) }

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
                    val countLabel = pluralStringResource(
                        R.plurals.sup_material_count,
                        group.materials.size,
                        group.materials.size,
                    )
                    val builtInLabel = stringResource(R.string.sup_builtin)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(group.supplier.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (group.supplier.isBuiltIn) "$countLabel · $builtInLabel" else countLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!group.supplier.isBuiltIn) {
                        IconButton(onClick = { renameSupplierTarget = group.supplier }) {
                            Text("✎", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { deleteSupplierTarget = group.supplier }) {
                            Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                                onDelete = { deleteTarget = entity },
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
            machiningMaterials = machiningMaterials,
            onDismiss = { formTarget = null },
            onSave = { name, shape, dims, price, technicalMaterialId ->
                onSaveMaterial(target.supplierId, target.existing, name, shape, dims, price, technicalMaterialId)
            },
            onSaved = {
                expandedId = target.supplierId
                formTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.material_delete_title)) },
            text = { Text(stringResource(R.string.material_delete_body, target.name)) },
            confirmButton = {
                Button(onClick = { onDeleteMaterial(target); deleteTarget = null }) {
                    Text(stringResource(R.string.material_delete_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (supplierDialog) {
        var name by remember { mutableStateOf("") }
        val duplicate = suppliers.any { it.supplier.name.equals(name.trim(), ignoreCase = true) }
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
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text(stringResource(R.string.sup_duplicate)) }) else null,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && !duplicate) onAddSupplier(name.trim())
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

    renameSupplierTarget?.let { target ->
        var name by remember(target.id) { mutableStateOf(target.name) }
        val duplicate = suppliers.any { it.supplier.id != target.id && it.supplier.name.equals(name.trim(), ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { renameSupplierTarget = null },
            title = { Text(stringResource(R.string.sup_rename)) },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text(stringResource(R.string.sup_duplicate)) }) else null,
                )
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank() && !duplicate,
                    onClick = { onRenameSupplier(target.id, name.trim()); renameSupplierTarget = null },
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { renameSupplierTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    deleteSupplierTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteSupplierTarget = null },
            title = { Text(stringResource(R.string.sup_delete_title)) },
            text = { Text(stringResource(R.string.sup_delete_body, target.name)) },
            confirmButton = {
                Button(onClick = { onDeleteSupplier(target); deleteSupplierTarget = null }) {
                    Text(stringResource(R.string.material_delete_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { deleteSupplierTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
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
    val configured = entity.priceConfigured

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
                        Fmt.money(entity.stockPrice, settings.copy(currencySymbol = entity.currencyCode ?: settings.currencySymbol), 0)
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
                        Fmt.money(entity.costPerVolumeCm3, settings.copy(currencySymbol = entity.currencyCode ?: settings.currencySymbol), 2) + "/cm³"
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
    machiningMaterials: List<MachiningMaterialEntity>,
    onDismiss: () -> Unit,
    onSave: suspend (String, Shape, Map<DimensionType, DimensionValue>, Double?, String?) -> Result<Unit>,
    onSaved: () -> Unit,
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
        mutableStateOf(existing?.stockPrice?.takeIf { existing.priceConfigured }?.let { Fmt.editable(it) } ?: "")
    }
    val values = remember { mutableStateMapOf<DimensionType, String>() }
    val units = remember { mutableStateMapOf<DimensionType, LengthUnit>() }
    var shapeMenu by remember { mutableStateOf(false) }
    var technicalMaterialMenu by remember { mutableStateOf(false) }
    var technicalMaterialId by remember { mutableStateOf(existing?.technicalMaterialId) }
    var nameError by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    // Tras un intento fallido de guardar, los errores se marcan en su campo y se van quitando al corregirlos.
    var showErrors by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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

    val price = priceText.toDecimalOrNull()
    val rate = if (dimensions.size == shape.requiredDimensions.size) {
        StockPricing.costPerVolumeCm3(shape, dimensions, price ?: 0.0).getOrNull()
    } else {
        null
    }

    val noTechnicalMaterialLabel = stringResource(R.string.form_technical_material_none)
    val shapeLabels = Shape.ALL.associateWith { candidate ->
        stringResource(shapeNameRes(candidate))
    }

    // Qué impide guardar, campo por campo, con el nombre visible de cada medida.
    val missingMessage = stringResource(R.string.dim_error_missing)
    val positiveMessage = stringResource(R.string.dim_error_positive)
    val dimensionLabels = shape.requiredDimensions.associateWith { stringResource(dimensionNameRes(it)) }
    val dimensionErrors = shape.requiredDimensions.mapNotNull { type ->
        val text = values[type]?.trim().orEmpty()
        val number = text.toDecimalOrNull()
        when {
            text.isEmpty() -> type to missingMessage
            number == null || !number.isFinite() || number <= 0.0 -> type to positiveMessage
            else -> null
        }
    }.toMap()
    // Relaciones entre medidas (p. ej. grosor de pared mayor que el radio): solo si cada medida es válida por sí sola.
    val relationErrors = if (dimensionErrors.isEmpty()) shape.additionalValidation(dimensions) else emptyList()
    // Un nombre que describe la geometría escrita: el usuario puede aceptarlo en vez de inventar uno.
    val suggestedName = shapeLabels.getValue(shape) + Fmt.dimensions(shape.requiredDimensions, dimensions).let { if (it.isBlank()) "" else " · $it" }
    val nameBlank = name.isBlank()


    fun signature() = (
        listOf(name, shape.id, priceText, technicalMaterialId ?: "") +
            shape.requiredDimensions.map { values[it] ?: "" } +
            shape.requiredDimensions.map { (units[it] ?: settings.defaultLengthUnit).name }
        ).joinToString("|")
    val initialSignature = remember { signature() }

    FormScreenDialog(title = stringResource(if (existing == null) R.string.form_new else R.string.form_edit), dirty = signature() != initialSignature, onClose = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError && nameBlank,
                supportingText = if (nameError && nameBlank) ({ Text(stringResource(R.string.form_error_name_hint)) }) else null,
                label = { Text(stringResource(R.string.form_name)) },
                placeholder = { Text(stringResource(R.string.form_name_hint)) },
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            if (nameBlank) {
                AssistChip(
                    onClick = { name = suggestedName; nameError = false },
                    label = { Text(stringResource(R.string.form_use_suggested, suggestedName), maxLines = 1) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.form_technical_material).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { technicalMaterialMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        machiningMaterials.firstOrNull { it.id == technicalMaterialId }?.name
                            ?: noTechnicalMaterialLabel,
                        modifier = Modifier.weight(1f),
                    )
                    Text("⌄")
                }
                DropdownMenu(expanded = technicalMaterialMenu, onDismissRequest = { technicalMaterialMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(noTechnicalMaterialLabel) },
                        onClick = { technicalMaterialId = null; technicalMaterialMenu = false },
                    )
                    machiningMaterials.forEach { material ->
                        DropdownMenuItem(
                            text = { Text(material.group.name + " · " + material.name) },
                            onClick = { technicalMaterialId = material.id; technicalMaterialMenu = false },
                        )
                    }
                }
            }

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
                        text = shapeLabels.getValue(shape),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = shapeMenu, onDismissRequest = { shapeMenu = false }) {
                    Shape.ALL.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(shapeLabels.getValue(candidate)) },
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
                        error = if (showErrors) dimensionErrors[type] else null,
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
                        text = rate?.takeIf { it > 0.0 }?.let { Fmt.money(it, settings, 2) } ?: "—",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = NumberFamily),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val problems = buildList {
                if (nameBlank) add(stringResource(R.string.form_error_name))
                dimensionErrors.forEach { (type, message) -> add(dimensionLabels.getValue(type) + ": " + message) }
                addAll(relationErrors)
                saveError?.let { add(it) }
            }
            if ((showErrors || saveError != null) && problems.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.form_error_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        problems.forEach { problem ->
                            Text("• $problem", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        if (nameBlank) {
                            TextButton(onClick = { name = suggestedName; nameError = false }) {
                                Text(stringResource(R.string.form_use_suggested, suggestedName))
                            }
                        }
                    }
                }
            }

            val saveFailedMessage = stringResource(R.string.form_error_save_failed)
            Button(
                onClick = {
                    showErrors = true
                    saveError = null
                    if (nameBlank) nameError = true
                    if (nameBlank || dimensionErrors.isNotEmpty() || relationErrors.isNotEmpty()) {
                        // El aviso está encima del botón: se baja hasta él para que se vea.
                        scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        onSave(name.trim(), shape, dimensions, price, technicalMaterialId)
                            .onSuccess { onSaved() }
                            .onFailure {
                                saveError = it.message ?: saveFailedMessage
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        saving = false
                    }
                },
                enabled = !saving,
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
