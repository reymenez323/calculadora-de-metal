@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui.calc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.local.DimensionsCodec
import com.reymildo.calculadoradelmetal.data.local.entity.MaterialEntity
import com.reymildo.calculadoradelmetal.data.local.relation.SupplierWithMaterials
import com.reymildo.calculadoradelmetal.data.settings.AppSettings
import com.reymildo.calculadoradelmetal.domain.calculation.CostCalculator
import com.reymildo.calculadoradelmetal.domain.calculation.VolumeCalculator
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.domain.model.Shape
import com.reymildo.calculadoradelmetal.ui.common.DimensionRow
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.MoneyField
import com.reymildo.calculadoradelmetal.ui.common.ResultStat
import com.reymildo.calculadoradelmetal.ui.common.RoundTubeInnerDiameterRow
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.ShapeGlyph
import com.reymildo.calculadoradelmetal.ui.common.ShapeIsoDiagram
import com.reymildo.calculadoradelmetal.ui.common.Stepper
import com.reymildo.calculadoradelmetal.ui.common.dimensionNameRes
import com.reymildo.calculadoradelmetal.ui.common.sanitizeInt
import com.reymildo.calculadoradelmetal.ui.common.shapeNameRes
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

/**
 * Modo de trabajo de la pestaña Calcular. Por ahora solo materia prima está implementado;
 * torneado y fresado aparecen en el selector para cuando se construyan.
 */
enum class CalcMode { MATERIA_PRIMA, TORNEADO, FRESADO }

/**
 * Lo que la barra superior debe mostrar en vez del texto/dropdown, cuando ya se eligió una forma:
 * su nombre, las dimensiones que pide, y el botón para volver al selector de forma. Sustituye el
 * espacio en blanco que quedaba arriba al ocultar el subtítulo.
 */
data class CalcHeader(val title: String, val subtitle: String, val onBack: () -> Unit)

private data class MaterialRow(val entity: MaterialEntity, val supplierName: String) {
    val shape: Shape? = runCatching { Shape.fromId(entity.stockShapeId) }.getOrNull()
    val dimensions: Map<DimensionType, DimensionValue> =
        runCatching { DimensionsCodec.decode(entity.stockDimensionsJson) }.getOrDefault(emptyMap())
}

/**
 * Dimensiones que de verdad se "cortan a medida" por pieza. Todo lo demás (diámetro, grosor,
 * lado, entre caras, grosor de pared/alma/ala) no lo elige quien corta: viene fijo por el bruto
 * que compró, así que cuando el material coincide en forma se rellena y se bloquea en vez de
 * pedírselo dos veces.
 */
private fun Shape.cuttableDimensions(): Set<DimensionType> = when (this) {
    Shape.Plate -> setOf(DimensionType.WIDTH, DimensionType.LENGTH)
    else -> setOf(DimensionType.LENGTH)
}

/** Copia las dimensiones "de perfil" del bruto elegido hacia el formulario, cuando la forma coincide. */
private fun applyLockedDimensions(
    shape: Shape,
    material: MaterialRow?,
    values: MutableMap<DimensionType, String>,
    units: MutableMap<DimensionType, LengthUnit>,
) {
    if (material == null || material.entity.stockShapeId != shape.id) return
    val cuttable = shape.cuttableDimensions()
    shape.requiredDimensions.filterNot { it in cuttable }.forEach { type ->
        material.dimensions[type]?.let { dim ->
            values[type] = Fmt.trimNumber(dim.value)
            units[type] = dim.unit
        }
    }
}

@Composable
fun CalculatorScreen(
    suppliers: List<SupplierWithMaterials>,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    mode: CalcMode = CalcMode.MATERIA_PRIMA,
    onHeaderChange: (CalcHeader?) -> Unit = {},
) {
    if (mode != CalcMode.MATERIA_PRIMA) {
        LaunchedEffect(Unit) { onHeaderChange(null) }
        ComingSoonScreen(modifier = modifier)
        return
    }

    var selectedShapeId by remember { mutableStateOf<String?>(null) }
    val values = remember { mutableStateMapOf<DimensionType, String>() }
    val units = remember { mutableStateMapOf<DimensionType, LengthUnit>() }
    var quantity by remember { mutableStateOf("1") }
    var manualCostText by remember { mutableStateOf("") }
    var selectedMaterialId by remember { mutableStateOf<Long?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    val recentIds = remember { mutableStateListOf<Long>() }

    val rows = remember(suppliers) {
        suppliers.flatMap { group -> group.materials.map { MaterialRow(it, group.supplier.name) } }
    }
    val selectedShape = selectedShapeId?.let { id -> runCatching { Shape.fromId(id) }.getOrNull() }
    val header = selectedShape?.let { shape ->
        val dimensionLabels = shape.requiredDimensions.map { stringResource(dimensionNameRes(it)) }
        CalcHeader(
            title = stringResource(shapeNameRes(shape)),
            subtitle = dimensionLabels.joinToString(" · "),
            onBack = { selectedShapeId = null },
        )
    }
    LaunchedEffect(selectedShapeId) { onHeaderChange(header) }

    if (selectedShape == null) {
        ShapePickerScreen(
            modifier = modifier,
            onPick = { shape ->
                selectedShapeId = shape.id
                values.clear()
                units.clear()
                shape.requiredDimensions.forEach { type ->
                    values[type] = ""
                    units[type] = settings.defaultLengthUnit
                }
                manualCostText = ""
                // el costo/cm³ de un bruto solo aplica a su misma forma
                val compatible = rows.filter { it.entity.stockShapeId == shape.id }
                val picked = compatible.firstOrNull { it.entity.costPerVolumeCm3 > 0.0 } ?: compatible.firstOrNull()
                selectedMaterialId = picked?.entity?.id
                applyLockedDimensions(shape, picked, values, units)
            },
        )
        return
    }

    BackHandler { selectedShapeId = null }

    val dimensions: Map<DimensionType, DimensionValue> = selectedShape.requiredDimensions
        .mapNotNull { type ->
            values[type]?.toDecimalOrNull()?.let { number ->
                type to DimensionValue(number, units[type] ?: settings.defaultLengthUnit)
            }
        }
        .toMap()

    val incomplete = selectedShape.requiredDimensions.any { type ->
        (values[type]?.toDecimalOrNull() ?: 0.0) <= 0.0
    }
    val geometryErrors = if (incomplete) emptyList() else selectedShape.additionalValidation(dimensions)
    val volume = if (incomplete || geometryErrors.isNotEmpty()) {
        null
    } else {
        VolumeCalculator.calculate(selectedShape, dimensions).getOrNull()
    }

    val pieces = quantity.toIntOrNull() ?: 0
    val manualCost = manualCostText.toDecimalOrNull()?.takeIf { it > 0.0 }
    val material = rows.firstOrNull { it.entity.id == selectedMaterialId }
    val rate = material?.entity?.costPerVolumeCm3 ?: 0.0
    val priceMissing = manualCost == null && rate <= 0.0

    val cost = if (volume != null && pieces > 0 && !priceMissing) {
        CostCalculator.calculate(
            pieceVolumeCm3 = volume,
            quantity = pieces,
            costPerVolumeCm3 = rate,
            manualPieceCostOverride = manualCost,
        ).getOrNull()
    } else {
        null
    }

    val warning = when {
        incomplete -> stringResource(R.string.calc_error_incomplete)
        geometryErrors.isNotEmpty() -> geometryErrors.first()
        pieces <= 0 -> stringResource(R.string.calc_error_quantity)
        priceMissing -> stringResource(R.string.calc_error_no_price)
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(title = stringResource(R.string.dim_reference_title)) {
            ShapeIsoDiagram(shape = selectedShape, modifier = Modifier.fillMaxWidth())
        }

        val lockedByMaterial = material != null && material.entity.stockShapeId == selectedShape.id
        val cuttable = selectedShape.cuttableDimensions()

        SectionCard(title = stringResource(R.string.calc_dimensions)) {
            selectedShape.requiredDimensions.forEach { type ->
                val locked = lockedByMaterial && type !in cuttable
                DimensionRow(
                    label = stringResource(dimensionNameRes(type)),
                    value = values[type] ?: "",
                    onValueChange = { values[type] = it },
                    unit = units[type] ?: settings.defaultLengthUnit,
                    onUnitChange = { units[type] = it },
                    enabled = !locked,
                    helper = if (locked) stringResource(R.string.dim_locked_by_material) else null,
                )
            }
            if (selectedShape == Shape.RoundTube) {
                RoundTubeInnerDiameterRow(
                    values = values,
                    units = units,
                    enabled = !lockedByMaterial,
                    helper = if (lockedByMaterial) stringResource(R.string.dim_locked_by_material) else null,
                )
            }
        }

        SectionCard(title = stringResource(R.string.calc_material)) {
            Card(
                onClick = { pickerOpen = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (material?.shape != null) {
                        ShapeGlyph(
                            shape = material.shape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = material?.entity?.name ?: stringResource(R.string.calc_choose_material),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = material?.let {
                                it.supplierName + " · " +
                                    Fmt.dimensions(it.shape?.requiredDimensions.orEmpty(), it.dimensions)
                            } ?: stringResource(R.string.calc_material_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (material != null && manualCost == null && material.entity.stockShapeId != selectedShape.id) {
                Text(
                    text = stringResource(R.string.calc_shape_mismatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.calc_manual_cost),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.calc_manual_cost_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MoneyField(
                    value = manualCostText,
                    onValueChange = { manualCostText = it },
                    currencySymbol = settings.currencySymbol,
                    placeholder = "—",
                    modifier = Modifier.width(176.dp),
                )
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.calc_quantity),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Stepper(
                    value = quantity,
                    onValueChange = { quantity = it.sanitizeInt() },
                    onDecrement = { quantity = (pieces - 1).coerceAtLeast(1).toString() },
                    onIncrement = { quantity = (pieces + 1).toString() },
                )
            }
        }

        if (warning != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        ResultCard(
            settings = settings,
            pieces = pieces,
            volumeCm3 = volume,
            costPerPiece = cost?.costPerPiece,
            totalCost = cost?.totalCost,
            rate = if (manualCost != null) null else rate.takeIf { it > 0.0 },
        )

        Spacer(Modifier.height(16.dp))
    }

    if (pickerOpen) {
        MaterialPickerSheet(
            shape = selectedShape,
            rows = rows,
            recentIds = recentIds.toList(),
            selectedId = selectedMaterialId,
            settings = settings,
            onDismiss = { pickerOpen = false },
            onPick = { id ->
                selectedMaterialId = id
                manualCostText = ""
                applyLockedDimensions(selectedShape, rows.firstOrNull { it.entity.id == id }, values, units)
                recentIds.remove(id)
                recentIds.add(0, id)
                while (recentIds.size > 3) recentIds.removeAt(recentIds.lastIndex)
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun ComingSoonScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.calc_mode_coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShapePickerScreen(
    onPick: (Shape) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(Shape.ALL) { shape ->
            Card(
                onClick = { onPick(shape) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 104.dp)
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
                ) {
                    ShapeGlyph(
                        shape = shape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = stringResource(shapeNameRes(shape)),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    settings: AppSettings,
    pieces: Int,
    volumeCm3: Double?,
    costPerPiece: Double?,
    totalCost: Double?,
    rate: Double?,
) {
    val ink = MaterialTheme.colorScheme.inverseOnSurface
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.calc_total_cost).uppercase() + " · " +
                            pieces.toString() + " " + stringResource(R.string.calc_pieces_suffix),
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.55f),
                    )
                    Text(
                        text = totalCost?.let { Fmt.money(it, settings) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        color = ink,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.calc_per_piece).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.55f),
                    )
                    Text(
                        text = costPerPiece?.let { Fmt.money(it, settings) } ?: "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = NumberFamily),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider(color = ink.copy(alpha = 0.14f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultStat(
                    label = stringResource(R.string.calc_volume),
                    value = volumeCm3?.let { Fmt.number(it, 2) + " cm³" } ?: "—",
                    valueColor = ink,
                    modifier = Modifier.weight(1f),
                )
                ResultStat(
                    label = stringResource(R.string.calc_rate),
                    value = rate?.let { Fmt.money(it, settings, 2) + "/cm³" }
                        ?: stringResource(R.string.calc_rate_manual),
                    valueColor = ink,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MaterialPickerSheet(
    shape: Shape,
    rows: List<MaterialRow>,
    recentIds: List<Long>,
    selectedId: Long?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shapeName = stringResource(shapeNameRes(shape)).lowercase()
    val othersCount = rows.count { it.entity.stockShapeId != shape.id }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.calc_material_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.calc_material_search)) },
                shape = RoundedCornerShape(13.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Text(
                text = if (showAll) {
                    stringResource(R.string.material_filter_all_note)
                } else {
                    stringResource(R.string.material_filter_note, shapeName)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (query.isBlank() && recentIds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    recentIds.mapNotNull { id -> rows.firstOrNull { it.entity.id == id } }
                        .filter { showAll || it.entity.stockShapeId == shape.id }
                        .forEach { row ->
                            AssistChip(
                                onClick = { onPick(row.entity.id) },
                                label = { Text(row.entity.name, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                }
            }
        }

        val filter = query.trim().lowercase()
        val grouped = rows
            .filter { showAll || it.entity.stockShapeId == shape.id }
            .filter {
                filter.isEmpty() ||
                    it.entity.name.lowercase().contains(filter) ||
                    it.supplierName.lowercase().contains(filter)
            }
            .groupBy { it.supplierName }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            grouped.forEach { (supplierName, items) ->
                item(key = "header-$supplierName") {
                    Text(
                        text = supplierName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                items(items, key = { it.entity.id }) { row ->
                    val selected = row.entity.id == selectedId
                    val configured = row.entity.costPerVolumeCm3 > 0.0
                    Card(
                        onClick = { onPick(row.entity.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            row.shape?.let {
                                ShapeGlyph(
                                    shape = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(row.entity.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = listOfNotNull(
                                        row.shape
                                            ?.takeIf { it.id != shape.id }
                                            ?.let { stringResource(shapeNameRes(it)) },
                                        Fmt.dimensions(row.shape?.requiredDimensions.orEmpty(), row.dimensions),
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = NumberFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = if (configured) {
                                    Fmt.money(row.entity.stockPrice, settings, 0)
                                } else {
                                    stringResource(R.string.material_no_price)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = NumberFamily,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = if (configured) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
            if (grouped.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = if (showAll) {
                            stringResource(R.string.material_empty_search)
                        } else {
                            stringResource(R.string.material_empty_for_shape, shapeName)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp),
                    )
                }
            }
            if (othersCount > 0) {
                item(key = "toggle") {
                    OutlinedButton(
                        onClick = { showAll = !showAll },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = if (showAll) {
                                stringResource(R.string.material_show_compatible, shapeName)
                            } else {
                                stringResource(R.string.material_show_all, othersCount)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}
