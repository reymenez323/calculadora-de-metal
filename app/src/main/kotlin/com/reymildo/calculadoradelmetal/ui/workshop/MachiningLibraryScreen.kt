@file:OptIn(ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui.workshop

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.entity.ToolRecommendationEntity
import com.reymildo.calculadoradelmetal.data.local.relation.ToolWithRecommendations
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.MaterialCategory
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import com.reymildo.calculadoradelmetal.ui.common.ChoiceTile
import com.reymildo.calculadoradelmetal.ui.common.DropdownField
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.Glyph
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon
import com.reymildo.calculadoradelmetal.ui.common.LabeledNumberField
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.Tag
import com.reymildo.calculadoradelmetal.ui.common.machineGlyph
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.common.toolGlyph
import kotlinx.coroutines.launch

@Composable
private fun categoryLabel(category: MaterialCategory): String = stringResource(
    when (category) {
        MaterialCategory.CARBON_STEEL -> R.string.cat_carbon_steel
        MaterialCategory.ALLOY_STEEL -> R.string.cat_alloy_steel
        MaterialCategory.STAINLESS -> R.string.cat_stainless
        MaterialCategory.ALUMINUM -> R.string.cat_aluminum
        MaterialCategory.CAST_IRON -> R.string.cat_cast_iron
        MaterialCategory.COPPER_ALLOY -> R.string.cat_copper_alloy
        MaterialCategory.TITANIUM -> R.string.cat_titanium
        MaterialCategory.PLASTIC -> R.string.cat_plastic
    },
)

@Composable
private fun toolKindLabel(kind: ToolKind): String = stringResource(
    when (kind) {
        ToolKind.HSS -> R.string.tool_kind_hss
        ToolKind.CARBIDE_INSERT -> R.string.tool_kind_insert
        ToolKind.CARBIDE_ENDMILL -> R.string.tool_kind_endmill
    },
)

@Composable
private fun machineTypeLabel(type: String): String = stringResource(
    when (type) {
        MachineType.LATHE.name -> R.string.machine_type_lathe
        MachineType.MILL.name -> R.string.machine_type_mill
        else -> R.string.machine_type_both
    },
)

@Composable
fun MachiningLibraryScreen(
    materials: List<MachiningMaterialEntity>,
    tools: List<ToolWithRecommendations>,
    onSaveMaterial: suspend (MachiningMaterialEntity) -> Result<Unit>,
    onDeleteMaterial: (MachiningMaterialEntity) -> Unit,
    onSaveTool: suspend (CuttingToolEntity, List<ToolRecommendationEntity>) -> Result<Long>,
    onDeleteTool: (CuttingToolEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTools by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !showTools,
                onClick = { showTools = false },
                leadingIcon = { GlyphIcon(Glyph.MATERIAL_CUBE, MaterialTheme.colorScheme.primary, size = 18.dp) },
                label = { Text(stringResource(R.string.lib_materials)) },
            )
            FilterChip(
                selected = showTools,
                onClick = { showTools = true },
                leadingIcon = { GlyphIcon(Glyph.TOOL_INSERT, MaterialTheme.colorScheme.primary, size = 18.dp) },
                label = { Text(stringResource(R.string.lib_tools)) },
            )
        }
        if (showTools) {
            ToolsList(materials, tools, onSaveTool, onDeleteTool, Modifier.weight(1f))
        } else {
            MaterialsList(materials, onSaveMaterial, onDeleteMaterial, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MaterialsList(
    materials: List<MachiningMaterialEntity>,
    onSave: suspend (MachiningMaterialEntity) -> Result<Unit>,
    onDelete: (MachiningMaterialEntity) -> Unit,
    modifier: Modifier,
) {
    var editing by remember { mutableStateOf<MachiningMaterialEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MachiningMaterialEntity?>(null) }
    val builtInLabel = stringResource(R.string.sup_builtin)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hint") {
            Text(
                stringResource(R.string.lib_materials_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(count = materials.size, key = { materials[it].id }) { index ->
            val material = materials[index]
            LibraryCard(
                glyph = Glyph.MATERIAL_CUBE,
                title = material.name,
                tags = buildList {
                    add(categoryLabel(material.materialCategory) to true)
                    if (material.isBuiltIn) add(builtInLabel to false)
                },
                detail = listOf(material.condition, material.hardness).filter { it.isNotBlank() }.joinToString(" · "),
                onEdit = { editing = material },
                onDelete = if (material.isBuiltIn) null else ({ deleteTarget = material }),
            )
        }
        item(key = "add") {
            AddButton(stringResource(R.string.lib_add_material)) { creating = true }
        }
    }

    if (creating || editing != null) {
        MaterialFormSheet(editing, onDismiss = { creating = false; editing = null }, onSave = onSave)
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.lib_delete_material_title)) },
            text = { Text(stringResource(R.string.lib_delete_material_body, target.name)) },
            confirmButton = { Button(onClick = { onDelete(target); deleteTarget = null }) { Text(stringResource(R.string.material_delete_confirm)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun ToolsList(
    materials: List<MachiningMaterialEntity>,
    tools: List<ToolWithRecommendations>,
    onSave: suspend (CuttingToolEntity, List<ToolRecommendationEntity>) -> Result<Long>,
    onDelete: (CuttingToolEntity) -> Unit,
    modifier: Modifier,
) {
    var editing by remember { mutableStateOf<ToolWithRecommendations?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CuttingToolEntity?>(null) }
    val builtInLabel = stringResource(R.string.sup_builtin)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hint") {
            Text(
                stringResource(R.string.lib_tools_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(count = tools.size, key = { tools[it].tool.id }) { index ->
            val item = tools[index]
            val tool = item.tool
            val count = item.recommendations.size
            LibraryCard(
                glyph = toolGlyph(tool.toolKind),
                title = tool.name,
                tags = buildList {
                    add(toolKindLabel(tool.toolKind) to true)
                    add(machineTypeLabel(tool.machineType) to false)
                    if (tool.isBuiltIn) add(builtInLabel to false)
                },
                detail = listOf(
                    listOf(tool.brand, tool.code, tool.coating).filter { it.isNotBlank() }.joinToString(" · "),
                    if (tool.isBuiltIn || count == 0) stringResource(R.string.lib_tool_generic) else stringResource(R.string.lib_tool_manufacturer_count, count),
                ).filter { it.isNotBlank() }.joinToString("\n"),
                onEdit = if (tool.isBuiltIn) null else ({ editing = item }),
                onDelete = if (tool.isBuiltIn) null else ({ deleteTarget = tool }),
            )
        }
        item(key = "add") {
            AddButton(stringResource(R.string.lib_add_tool)) { creating = true }
        }
    }

    if (creating || editing != null) {
        ToolFormSheet(editing, materials, onDismiss = { creating = false; editing = null }, onSave = onSave)
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.lib_delete_tool_title)) },
            text = { Text(stringResource(R.string.lib_delete_tool_body, target.name)) },
            confirmButton = { Button(onClick = { onDelete(target); deleteTarget = null }) { Text(stringResource(R.string.material_delete_confirm)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun LibraryCard(
    glyph: Glyph,
    title: String,
    tags: List<Pair<String, Boolean>>,
    detail: String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlyphIcon(glyph, MaterialTheme.colorScheme.primary, size = 32.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { (label, emphasized) -> Tag(label, emphasized = emphasized) }
                }
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onEdit != null) IconButton(onClick = onEdit) { Text("✎", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (onDelete != null) IconButton(onClick = onDelete) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun MaterialFormSheet(
    existing: MachiningMaterialEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (MachiningMaterialEntity) -> Result<Unit>,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.materialCategory ?: MaterialCategory.CARBON_STEEL) }
    var condition by remember { mutableStateOf(existing?.condition ?: "") }
    var hardness by remember { mutableStateOf(existing?.hardness ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categoryLabels = MaterialCategory.entries.associateWith { categoryLabel(it) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text(
                stringResource(if (existing == null) R.string.lib_new_material else R.string.lib_edit_material),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                name, { name = it; nameError = false }, Modifier.fillMaxWidth(), singleLine = true, isError = nameError,
                label = { Text(stringResource(R.string.form_name)) },
                placeholder = { Text(stringResource(R.string.lib_material_name_hint)) },
                shape = RoundedCornerShape(13.dp),
            )
            DropdownField(
                label = stringResource(R.string.lib_material_category),
                selected = categoryLabels.getValue(category),
                options = MaterialCategory.entries.map { it.name to categoryLabels.getValue(it) },
                onSelect = { category = MaterialCategory.valueOf(it) },
            )
            Text(
                stringResource(R.string.lib_material_category_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    condition, { condition = it }, Modifier.weight(1f), singleLine = true,
                    label = { Text(stringResource(R.string.lib_material_condition)) }, placeholder = { Text("T6, recocido…") },
                    shape = RoundedCornerShape(13.dp),
                )
                OutlinedTextField(
                    hardness, { hardness = it }, Modifier.weight(1f), singleLine = true,
                    label = { Text(stringResource(R.string.lib_material_hardness)) }, placeholder = { Text("≈ 95 HB") },
                    shape = RoundedCornerShape(13.dp),
                )
            }
            OutlinedTextField(
                notes, { notes = it }, Modifier.fillMaxWidth(), minLines = 2,
                label = { Text(stringResource(R.string.machines_form_notes)) },
                shape = RoundedCornerShape(13.dp),
            )
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    scope.launch {
                        onSave(
                            MachiningMaterialEntity(
                                id = existing?.id ?: "",
                                name = name.trim(),
                                category = category.name,
                                condition = condition.trim(),
                                hardness = hardness.trim(),
                                notes = notes.trim(),
                                isBuiltIn = existing?.isBuiltIn ?: false,
                            ),
                        ).onSuccess { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
            ) { Text(stringResource(R.string.form_save_changes), style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Borrador editable de los valores del fabricante para un material (todo en texto hasta guardar). */
private data class RecDraft(
    val materialId: String,
    val vcMin: String = "", val vcStart: String = "", val vcMax: String = "",
    val feedMin: String = "", val feedStart: String = "", val feedMax: String = "",
    val depth: String = "",
)

private fun RecDraft.toEntity(toolId: Long): ToolRecommendationEntity? {
    val vMin = vcMin.toDecimalOrNull()?.takeIf { it > 0 } ?: return null
    val vMax = vcMax.toDecimalOrNull()?.takeIf { it >= vMin } ?: return null
    val fMin = feedMin.toDecimalOrNull()?.takeIf { it > 0 } ?: return null
    val fMax = feedMax.toDecimalOrNull()?.takeIf { it >= fMin } ?: return null
    val vStart = vcStart.toDecimalOrNull()?.takeIf { it in vMin..vMax } ?: if (vcStart.isBlank()) (vMin + vMax) / 2 else return null
    val fStart = feedStart.toDecimalOrNull()?.takeIf { it in fMin..fMax } ?: if (feedStart.isBlank()) (fMin + fMax) / 2 else return null
    return ToolRecommendationEntity(
        toolId = toolId, materialId = materialId,
        vcMin = vMin, vcStart = vStart, vcMax = vMax,
        feedMin = fMin, feedStart = fStart, feedMax = fMax,
        depthMaxMm = depth.toDecimalOrNull()?.takeIf { it > 0 },
    )
}

@Composable
private fun ToolFormSheet(
    existing: ToolWithRecommendations?,
    materials: List<MachiningMaterialEntity>,
    onDismiss: () -> Unit,
    onSave: suspend (CuttingToolEntity, List<ToolRecommendationEntity>) -> Result<Long>,
) {
    val tool = existing?.tool
    fun text(v: Double) = Fmt.editable(v)

    var name by remember { mutableStateOf(tool?.name ?: "") }
    var kind by remember { mutableStateOf(tool?.toolKind ?: ToolKind.CARBIDE_INSERT) }
    var machineType by remember { mutableStateOf(tool?.machineType?.takeIf { it != "BOTH" } ?: MachineType.LATHE.name) }
    var brand by remember { mutableStateOf(tool?.brand ?: "") }
    var code by remember { mutableStateOf(tool?.code ?: "") }
    var coating by remember { mutableStateOf(tool?.coating ?: "") }
    var notes by remember { mutableStateOf(tool?.notes ?: "") }
    val recs = remember {
        mutableStateListOf<RecDraft>().apply {
            existing?.recommendations?.forEach {
                add(
                    RecDraft(
                        it.materialId, text(it.vcMin), text(it.vcStart), text(it.vcMax),
                        text(it.feedMin), text(it.feedStart), text(it.feedMax), it.depthMaxMm?.let(::text) ?: "",
                    ),
                )
            }
        }
    }
    var nameError by remember { mutableStateOf(false) }
    var recsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kindLabels = ToolKind.entries.associateWith { toolKindLabel(it) }
    val typeLathe = stringResource(R.string.machine_type_lathe)
    val typeMill = stringResource(R.string.machine_type_mill)
    val effectiveType = if (kind == ToolKind.CARBIDE_ENDMILL) MachineType.MILL.name else machineType
    val feedUnit = if (effectiveType == MachineType.LATHE.name) "mm/rev" else stringResource(R.string.unit_mm_per_tooth)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text(stringResource(if (existing == null) R.string.lib_new_tool else R.string.lib_edit_tool), style = MaterialTheme.typography.titleLarge)

            SectionCard(title = stringResource(R.string.lib_tool_kind)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolKind.entries.forEach { candidate ->
                        ChoiceTile(toolGlyph(candidate), kindLabels.getValue(candidate), kind == candidate, { kind = candidate }, Modifier.weight(1f))
                    }
                }
                if (kind != ToolKind.CARBIDE_ENDMILL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MachineType.entries.forEach { candidate ->
                            ChoiceTile(
                                glyph = machineGlyph(candidate),
                                label = if (candidate == MachineType.LATHE) typeLathe else typeMill,
                                selected = machineType == candidate.name,
                                onClick = { machineType = candidate.name },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                name, { name = it; nameError = false }, Modifier.fillMaxWidth(), singleLine = true, isError = nameError,
                label = { Text(stringResource(R.string.lib_tool_name)) },
                placeholder = { Text("CNMG 120408 · GC4325") },
                shape = RoundedCornerShape(13.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(brand, { brand = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.machines_form_brand)) }, shape = RoundedCornerShape(13.dp))
                OutlinedTextField(code, { code = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.lib_tool_code)) }, shape = RoundedCornerShape(13.dp))
            }
            OutlinedTextField(
                coating, { coating = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.lib_tool_coating)) }, shape = RoundedCornerShape(13.dp),
            )

            SectionCard(title = stringResource(R.string.lib_tool_recommendations)) {
                Text(
                    stringResource(R.string.lib_tool_recommendations_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                recs.forEachIndexed { index, rec ->
                    val taken = recs.map { it.materialId }.toSet() - rec.materialId
                    val available = materials.filter { it.id !in taken }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                DropdownField(
                                    label = stringResource(R.string.lib_rec_material),
                                    selected = materials.firstOrNull { it.id == rec.materialId }?.name ?: "—",
                                    options = available.map { it.id to it.name },
                                    onSelect = { recs[index] = rec.copy(materialId = it) },
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { recs.removeAt(index) }) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Text("Vc (m/min)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledNumberField(stringResource(R.string.lib_rec_min), rec.vcMin, { recs[index] = rec.copy(vcMin = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_start), rec.vcStart, { recs[index] = rec.copy(vcStart = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_max), rec.vcMax, { recs[index] = rec.copy(vcMax = it) }, Modifier.weight(1f))
                            }
                            Text(stringResource(R.string.lib_rec_feed, feedUnit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledNumberField(stringResource(R.string.lib_rec_min), rec.feedMin, { recs[index] = rec.copy(feedMin = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_start), rec.feedStart, { recs[index] = rec.copy(feedStart = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_max), rec.feedMax, { recs[index] = rec.copy(feedMax = it) }, Modifier.weight(1f))
                            }
                            LabeledNumberField(
                                stringResource(R.string.lib_rec_depth), rec.depth, { recs[index] = rec.copy(depth = it) },
                                Modifier.fillMaxWidth(), suffix = "mm",
                            )
                        }
                    }
                }
                val unused = materials.filter { m -> recs.none { it.materialId == m.id } }
                OutlinedButton(
                    onClick = { unused.firstOrNull()?.let { recs.add(RecDraft(it.id)) } },
                    enabled = unused.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) { Text(stringResource(R.string.lib_rec_add), color = MaterialTheme.colorScheme.primary) }
                if (recsError) {
                    Text(stringResource(R.string.lib_rec_invalid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                notes, { notes = it }, Modifier.fillMaxWidth(), minLines = 2,
                label = { Text(stringResource(R.string.machines_form_notes)) }, shape = RoundedCornerShape(13.dp),
            )

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val entities = recs.map { it.toEntity(tool?.id ?: 0L) }
                    if (entities.any { it == null }) { recsError = true; return@Button }
                    scope.launch {
                        onSave(
                            CuttingToolEntity(
                                id = tool?.id ?: 0L, name = name.trim(), kind = kind.name, machineType = effectiveType,
                                brand = brand.trim(), code = code.trim(), coating = coating.trim(), notes = notes.trim(),
                            ),
                            entities.filterNotNull(),
                        ).onSuccess { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
            ) { Text(stringResource(R.string.form_save_changes), style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(20.dp))
        }
    }
}
