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
import com.reymildo.calculadoradelmetal.domain.machining.IsoGroup
import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import com.reymildo.calculadoradelmetal.ui.common.ChoiceTile
import com.reymildo.calculadoradelmetal.ui.common.DropdownField
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.Glyph
import com.reymildo.calculadoradelmetal.ui.common.IsoBadge
import com.reymildo.calculadoradelmetal.ui.common.IsoGroupPicker
import com.reymildo.calculadoradelmetal.ui.common.isoDescription
import com.reymildo.calculadoradelmetal.ui.common.isoName
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon
import com.reymildo.calculadoradelmetal.ui.common.LabeledNumberField
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.Tag
import com.reymildo.calculadoradelmetal.ui.common.UnitPicker
import com.reymildo.calculadoradelmetal.ui.common.machineGlyph
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.common.toolGlyph
import kotlinx.coroutines.launch

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
            ToolsList(tools, onSaveTool, onDeleteTool, Modifier.weight(1f))
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
    val groupNames = IsoGroup.entries.associateWith { isoName(it) }

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
                iso = material.group,
                title = material.name,
                tags = buildList {
                    add(material.group.name + " · " + groupNames.getValue(material.group) to true)
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
private fun MaterialFormSheet(
    existing: MachiningMaterialEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (MachiningMaterialEntity) -> Result<Unit>,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var group by remember { mutableStateOf(existing?.group ?: IsoGroup.P) }
    var condition by remember { mutableStateOf(existing?.condition ?: "") }
    var hardness by remember { mutableStateOf(existing?.hardness ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            SectionCard(title = stringResource(R.string.lib_material_group)) {
                IsoGroupPicker(selected = group, taken = emptySet(), onSelect = { group = it })
                Text(
                    stringResource(R.string.lib_material_group_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    condition, { condition = it }, Modifier.weight(1f), singleLine = true,
                    label = { Text(stringResource(R.string.lib_material_condition)) }, placeholder = { Text("T6, recocido…") },
                    shape = RoundedCornerShape(13.dp),
                )
                OutlinedTextField(
                    hardness, { hardness = it }, Modifier.weight(1f), singleLine = true,
                    label = { Text(stringResource(R.string.lib_material_hardness)) }, placeholder = { Text("≈ 95 HB · 55 HRC") },
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
                                isoGroup = group.name,
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

@Composable
private fun ToolsList(
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
                isoCodes = item.recommendations.map { it.isoGroup }.sorted(),
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
        ToolFormSheet(editing, onDismiss = { creating = false; editing = null }, onSave = onSave)
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
    isoCodes: List<String> = emptyList(),
    iso: IsoGroup? = null,
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
            if (iso != null) IsoBadge(iso, size = 36.dp) else GlyphIcon(glyph, MaterialTheme.colorScheme.primary, size = 32.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { (label, emphasized) -> Tag(label, emphasized = emphasized) }
                }
                if (isoCodes.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { isoCodes.forEach { code -> IsoGroup.entries.firstOrNull { it.name == code }?.let { IsoBadge(it, size = 22.dp) } } }
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

/** Borrador editable de los valores del fabricante para un grupo ISO (todo en texto hasta guardar). */
private data class RecDraft(
    val group: IsoGroup,
    val vcMin: String = "", val vcStart: String = "", val vcMax: String = "",
    val feedMin: String = "", val feedStart: String = "", val feedMax: String = "",
    val depth: String = "",
    val vcUnit: MachUnit = MachUnit.M_MIN,
    val feedUnit: MachUnit = MachUnit.MM,
    val depthUnit: MachUnit = MachUnit.MM,
)

/** Cambia la unidad de un grupo de campos convirtiendo lo ya escrito. */
private fun convertText(text: String, from: MachUnit, to: MachUnit): String =
    text.toDecimalOrNull()?.let { Fmt.editable(MachUnit.convert(it, from, to)) } ?: text

private fun RecDraft.toEntity(toolId: Long): ToolRecommendationEntity? {
    fun vc(text: String) = text.toDecimalOrNull()?.let { MachUnit.toBase(it, vcUnit) }
    fun feed(text: String) = text.toDecimalOrNull()?.let { MachUnit.toBase(it, feedUnit) }
    val vMin = vc(vcMin)?.takeIf { it > 0 } ?: return null
    val vMax = vc(vcMax)?.takeIf { it >= vMin } ?: return null
    val fMin = feed(feedMin)?.takeIf { it > 0 } ?: return null
    val fMax = feed(feedMax)?.takeIf { it >= fMin } ?: return null
    val vStart = vc(vcStart)?.takeIf { it in vMin..vMax } ?: if (vcStart.isBlank()) (vMin + vMax) / 2 else return null
    val fStart = feed(feedStart)?.takeIf { it in fMin..fMax } ?: if (feedStart.isBlank()) (fMin + fMax) / 2 else return null
    return ToolRecommendationEntity(
        toolId = toolId, isoGroup = group.name,
        vcMin = vMin, vcStart = vStart, vcMax = vMax,
        feedMin = fMin, feedStart = fStart, feedMax = fMax,
        depthMaxMm = depth.toDecimalOrNull()?.takeIf { it > 0 }?.let { MachUnit.toBase(it, depthUnit) },
    )
}

@Composable
private fun ToolFormSheet(
    existing: ToolWithRecommendations?,
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
                val group = IsoGroup.entries.firstOrNull { g -> g.name == it.isoGroup } ?: return@forEach
                add(
                    RecDraft(
                        group, text(it.vcMin), text(it.vcStart), text(it.vcMax),
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
    val feedSuffix = if (effectiveType == MachineType.LATHE.name) "/rev" else stringResource(R.string.unit_per_tooth)

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
                    val taken = recs.map { it.group }.toSet() - rec.group
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                IsoGroupPicker(
                                    selected = rec.group,
                                    taken = taken,
                                    onSelect = { recs[index] = rec.copy(group = it) },
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { recs.removeAt(index) }) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.lib_rec_speed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                UnitPicker(rec.vcUnit) { picked ->
                                    recs[index] = rec.copy(
                                        vcMin = convertText(rec.vcMin, rec.vcUnit, picked), vcStart = convertText(rec.vcStart, rec.vcUnit, picked),
                                        vcMax = convertText(rec.vcMax, rec.vcUnit, picked), vcUnit = picked,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledNumberField(stringResource(R.string.lib_rec_min), rec.vcMin, { recs[index] = rec.copy(vcMin = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_start), rec.vcStart, { recs[index] = rec.copy(vcStart = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_max), rec.vcMax, { recs[index] = rec.copy(vcMax = it) }, Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.lib_rec_feed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                UnitPicker(rec.feedUnit, feedSuffix) { picked ->
                                    recs[index] = rec.copy(
                                        feedMin = convertText(rec.feedMin, rec.feedUnit, picked), feedStart = convertText(rec.feedStart, rec.feedUnit, picked),
                                        feedMax = convertText(rec.feedMax, rec.feedUnit, picked), feedUnit = picked,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LabeledNumberField(stringResource(R.string.lib_rec_min), rec.feedMin, { recs[index] = rec.copy(feedMin = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_start), rec.feedStart, { recs[index] = rec.copy(feedStart = it) }, Modifier.weight(1f))
                                LabeledNumberField(stringResource(R.string.lib_rec_max), rec.feedMax, { recs[index] = rec.copy(feedMax = it) }, Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LabeledNumberField(
                                    stringResource(R.string.lib_rec_depth), rec.depth, { recs[index] = rec.copy(depth = it) },
                                    Modifier.weight(1f),
                                )
                                UnitPicker(rec.depthUnit) { picked ->
                                    recs[index] = rec.copy(depth = convertText(rec.depth, rec.depthUnit, picked), depthUnit = picked)
                                }
                            }
                        }
                    }
                }
                val unused = IsoGroup.entries.filter { g -> recs.none { it.group == g } }
                OutlinedButton(
                    onClick = { unused.firstOrNull()?.let { recs.add(RecDraft(it)) } },
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
