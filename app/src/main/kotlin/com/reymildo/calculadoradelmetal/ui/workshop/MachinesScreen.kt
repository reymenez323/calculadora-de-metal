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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.ui.common.FormScreenDialog
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.ui.common.ChoiceTile
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon
import com.reymildo.calculadoradelmetal.ui.common.LabeledNumberField
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.Tag
import com.reymildo.calculadoradelmetal.ui.common.machineGlyph
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.ui.common.UnitPicker
import com.reymildo.calculadoradelmetal.ui.common.isValidDecimalInput
import kotlinx.coroutines.launch

private val categories = listOf(
    MachineType.LATHE to MachineKind.CONVENTIONAL,
    MachineType.LATHE to MachineKind.CNC,
    MachineType.MILL to MachineKind.CONVENTIONAL,
    MachineType.MILL to MachineKind.CNC,
)

@Composable
private fun categoryLabel(type: MachineType, kind: MachineKind): String = stringResource(
    when (type to kind) {
        MachineType.LATHE to MachineKind.CONVENTIONAL -> R.string.machine_cat_lathe_conventional
        MachineType.LATHE to MachineKind.CNC -> R.string.machine_cat_lathe_cnc
        MachineType.MILL to MachineKind.CONVENTIONAL -> R.string.machine_cat_mill_conventional
        else -> R.string.machine_cat_mill_cnc
    },
)

@Composable
fun MachinesScreen(
    machines: List<MachineProfileEntity>,
    onSave: suspend (MachineProfileEntity) -> Result<Long>,
    onDelete: (MachineProfileEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<MachineProfileEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MachineProfileEntity?>(null) }
    val categoryNames = categories.associateWith { (type, kind) -> categoryLabel(type, kind) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hint") {
            Text(
                stringResource(R.string.machines_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        categories.forEach { category ->
            val inCategory = machines.filter { it.type == category.first && it.controlKind == category.second }
            item(key = "header-${category.first}-${category.second}") {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlyphIcon(machineGlyph(category.first), MaterialTheme.colorScheme.primary, size = 22.dp)
                    Text(
                        categoryNames.getValue(category).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (inCategory.isEmpty()) {
                item(key = "empty-${category.first}-${category.second}") {
                    Text(
                        stringResource(R.string.machines_category_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(count = inCategory.size, key = { inCategory[it].id }) { index ->
                MachineCard(
                    machine = inCategory[index],
                    onEdit = { editing = inCategory[index] },
                    onDelete = { deleteTarget = inCategory[index] },
                )
            }
        }
        item(key = "add") {
            OutlinedButton(
                onClick = { creating = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(stringResource(R.string.machines_add), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (creating || editing != null) {
        MachineFormSheet(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSave = onSave,
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.machines_delete_title)) },
            text = { Text(stringResource(R.string.machines_delete_body, target.name)) },
            confirmButton = {
                Button(onClick = { onDelete(target); deleteTarget = null }) { Text(stringResource(R.string.material_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun MachineCard(machine: MachineProfileEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val conventionalLabel = stringResource(R.string.machine_kind_conventional)
    val cncLabel = stringResource(R.string.machine_kind_cnc)
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
            GlyphIcon(machineGlyph(machine.type), MaterialTheme.colorScheme.primary, size = 34.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(machine.name, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(if (machine.controlKind == MachineKind.CNC) cncLabel else conventionalLabel, emphasized = machine.controlKind == MachineKind.CNC)
                    val maker = listOf(machine.brand, machine.model).filter { it.isNotBlank() }.joinToString(" ")
                    if (maker.isNotBlank()) {
                        Text(maker, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = specSummary(machine),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = NumberFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Text("✎", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (!machine.isBuiltIn) {
                IconButton(onClick = onDelete) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

private fun specSummary(m: MachineProfileEntity): String = buildList {
    add((m.minRpm?.let { Fmt.number(it, 0) + "–" } ?: "") + Fmt.number(m.maxRpm, 0) + " RPM")
    add(Fmt.number(m.maxFeedMmMin, 0) + " mm/min")
    m.powerKw?.let { add(Fmt.number(it, 1) + " kW") }
    if (m.type == MachineType.LATHE) {
        m.swingMm?.let { add("Ø" + Fmt.number(it, 0)) }
        m.centersDistanceMm?.let { add("L " + Fmt.number(it, 0)) }
    } else {
        if (m.travelXMm != null && m.travelYMm != null && m.travelZMm != null) {
            add("${Fmt.number(m.travelXMm, 0)}×${Fmt.number(m.travelYMm, 0)}×${Fmt.number(m.travelZMm, 0)}")
        }
        if (m.spindleTaper.isNotBlank()) add(m.spindleTaper)
    }
}.joinToString(" · ")

@Composable
private fun MachineFormSheet(
    existing: MachineProfileEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (MachineProfileEntity) -> Result<Long>,
) {
    fun text(v: Double?) = v?.let { Fmt.editable(it) } ?: ""

    var type by remember { mutableStateOf(existing?.type ?: MachineType.LATHE) }
    var kind by remember { mutableStateOf(existing?.controlKind ?: MachineKind.CONVENTIONAL) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var brand by remember { mutableStateOf(existing?.brand ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var maxRpm by remember { mutableStateOf(text(existing?.maxRpm)) }
    var minRpm by remember { mutableStateOf(text(existing?.minRpm)) }
    var steps by remember { mutableStateOf(existing?.steppedRpmCsv ?: "") }
    val maxFeed = remember { Amount(text(existing?.maxFeedMmMin), MachUnit.MM_MIN) }
    var threadRpm by remember { mutableStateOf(text(existing?.maxThreadingRpm)) }
    var partRpm by remember { mutableStateOf(text(existing?.maxPartingRpm)) }
    val power = remember { Amount(text(existing?.powerKw), MachUnit.KW) }
    val swing = remember { Amount(text(existing?.swingMm), MachUnit.MM) }
    val centers = remember { Amount(text(existing?.centersDistanceMm), MachUnit.MM) }
    val chuck = remember { Amount(text(existing?.chuckMm), MachUnit.MM) }
    val bore = remember { Amount(text(existing?.spindleBoreMm), MachUnit.MM) }
    val tx = remember { Amount(text(existing?.travelXMm), MachUnit.MM) }
    val ty = remember { Amount(text(existing?.travelYMm), MachUnit.MM) }
    val tz = remember { Amount(text(existing?.travelZMm), MachUnit.MM) }
    var taper by remember { mutableStateOf(existing?.spindleTaper ?: "") }
    var axes by remember { mutableStateOf(existing?.axes?.toString() ?: "") }
    var toolCapacity by remember { mutableStateOf(existing?.toolCapacity?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val rpmValue = maxRpm.toDecimalOrNull()?.takeIf { it > 0 }
    val feedValue = maxFeed.base()
    val lathe = type == MachineType.LATHE
    val typeLabels = MachineType.entries.associateWith { stringResource(if (it == MachineType.LATHE) R.string.machine_type_lathe else R.string.machine_type_mill) }
    val saveFailed = stringResource(R.string.machines_save_failed)


    fun signature() = (
        listOf(type.name, kind.name, name, brand, model, maxRpm, minRpm, steps, threadRpm, partRpm, taper, axes, toolCapacity, notes) +
            listOf(maxFeed, power, swing, centers, chuck, bore, tx, ty, tz).map { it.text + it.unit.name }
        ).joinToString("|")
    val initialSignature = remember { signature() }

    FormScreenDialog(title = stringResource(if (existing == null) R.string.machines_new else R.string.machines_edit), dirty = signature() != initialSignature, onClose = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {

            SectionCard(title = stringResource(R.string.machines_form_type)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MachineType.entries.forEach { candidate ->
                        ChoiceTile(
                            glyph = machineGlyph(candidate),
                            label = typeLabels.getValue(candidate),
                            selected = type == candidate,
                            onClick = { type = candidate },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == MachineKind.CONVENTIONAL,
                        onClick = { kind = MachineKind.CONVENTIONAL },
                        label = { Text(stringResource(R.string.machine_kind_conventional)) },
                    )
                    FilterChip(
                        selected = kind == MachineKind.CNC,
                        onClick = { kind = MachineKind.CNC },
                        label = { Text(stringResource(R.string.machine_kind_cnc)) },
                    )
                }
                Text(
                    categoryLabel(type, kind),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = showErrors && name.isBlank(),
                label = { Text(stringResource(R.string.machines_form_name)) },
                shape = RoundedCornerShape(13.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(brand, { brand = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.machines_form_brand)) }, shape = RoundedCornerShape(13.dp))
                OutlinedTextField(model, { model = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.machines_form_model)) }, shape = RoundedCornerShape(13.dp))
            }

            SectionCard(title = stringResource(R.string.machines_form_spindle)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledNumberField(stringResource(R.string.mach_max_rpm), maxRpm, { maxRpm = it }, Modifier.weight(1f), suffix = "RPM")
                    LabeledNumberField(stringResource(R.string.machines_form_min_rpm), minRpm, { minRpm = it }, Modifier.weight(1f), suffix = "RPM")
                }
                if (showErrors && rpmValue == null) {
                    Text(stringResource(R.string.machines_form_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (kind == MachineKind.CONVENTIONAL) {
                    OutlinedTextField(
                        value = steps,
                        onValueChange = { steps = it.filter { c -> c.isDigit() || c in ",. " } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.mach_rpm_steps)) },
                        supportingText = { Text(stringResource(R.string.machines_form_steps_hint)) },
                        shape = RoundedCornerShape(13.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountField(stringResource(R.string.mach_max_feed_short), maxFeed, Modifier.fillMaxWidth())
                    AmountField(stringResource(R.string.machines_form_power), power, Modifier.fillMaxWidth())
                }
                if (showErrors && feedValue == null) {
                    Text(stringResource(R.string.machines_form_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (lathe) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledNumberField(stringResource(R.string.machines_form_thread_rpm), threadRpm, { threadRpm = it }, Modifier.weight(1f), suffix = "RPM")
                        LabeledNumberField(stringResource(R.string.machines_form_part_rpm), partRpm, { partRpm = it }, Modifier.weight(1f), suffix = "RPM")
                    }
                }
            }

            SectionCard(title = stringResource(R.string.machines_form_capacity)) {
                if (lathe) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountField(stringResource(R.string.machines_form_swing), swing, Modifier.fillMaxWidth())
                        AmountField(stringResource(R.string.machines_form_centers), centers, Modifier.fillMaxWidth())
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountField(stringResource(R.string.machines_form_chuck), chuck, Modifier.fillMaxWidth())
                        AmountField(stringResource(R.string.machines_form_bore), bore, Modifier.fillMaxWidth())
                    }
                    LabeledNumberField(
                        stringResource(R.string.machines_form_turret), toolCapacity, { toolCapacity = it }, Modifier.fillMaxWidth(), integer = true,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountField("X", tx, Modifier.fillMaxWidth())
                        AmountField("Y", ty, Modifier.fillMaxWidth())
                        AmountField("Z", tz, Modifier.fillMaxWidth())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            taper, { taper = it }, Modifier.weight(1f), singleLine = true,
                            label = { Text(stringResource(R.string.machines_form_taper)) }, placeholder = { Text("BT40, ISO40, R8…") },
                            shape = RoundedCornerShape(12.dp),
                        )
                        LabeledNumberField(stringResource(R.string.machines_form_axes), axes, { axes = it }, Modifier.weight(1f), integer = true)
                    }
                    LabeledNumberField(
                        stringResource(R.string.machines_form_magazine), toolCapacity, { toolCapacity = it }, Modifier.fillMaxWidth(), integer = true,
                    )
                }
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                label = { Text(stringResource(R.string.machines_form_notes)) },
                placeholder = { Text(stringResource(R.string.machines_form_notes_hint)) },
                shape = RoundedCornerShape(13.dp),
            )

            Button(
                onClick = {
                    if (name.isBlank() || rpmValue == null || feedValue == null) {
                        showErrors = true
                        return@Button
                    }
                    val entity = (existing ?: MachineProfileEntity(name = "", kind = kind.name, maxRpm = rpmValue, maxFeedMmMin = feedValue)).copy(
                        name = name.trim(),
                        kind = kind.name,
                        machineType = type.name,
                        brand = brand.trim(),
                        model = model.trim(),
                        maxRpm = rpmValue,
                        minRpm = minRpm.toDecimalOrNull()?.takeIf { it > 0 },
                        steppedRpmCsv = if (kind == MachineKind.CONVENTIONAL) steps.trim() else "",
                        maxFeedMmMin = feedValue,
                        powerKw = power.base(),
                        maxThreadingRpm = if (lathe) threadRpm.toDecimalOrNull()?.takeIf { it > 0 } else null,
                        maxPartingRpm = if (lathe) partRpm.toDecimalOrNull()?.takeIf { it > 0 } else null,
                        swingMm = if (lathe) swing.base() else null,
                        centersDistanceMm = if (lathe) centers.base() else null,
                        chuckMm = if (lathe) chuck.base() else null,
                        spindleBoreMm = if (lathe) bore.base() else null,
                        travelXMm = if (!lathe) tx.base() else null,
                        travelYMm = if (!lathe) ty.base() else null,
                        travelZMm = if (!lathe) tz.base() else null,
                        spindleTaper = if (!lathe) taper.trim() else "",
                        axes = if (!lathe) axes.toIntOrNull()?.takeIf { it > 0 } else null,
                        toolCapacity = toolCapacity.toIntOrNull()?.takeIf { it > 0 },
                        notes = notes.trim(),
                    )
                    scope.launch {
                        onSave(entity)
                            .onSuccess { onDismiss() }
                            .onFailure { error = saveFailed }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
            ) { Text(stringResource(R.string.form_save_changes), style = MaterialTheme.typography.labelLarge) }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Cantidad escrita en la unidad que el usuario prefiera; se guarda siempre en la unidad base. */
private class Amount(text: String, unit: MachUnit) {
    var text by mutableStateOf(text)
    var unit by mutableStateOf(unit)

    fun base(): Double? = text.toDecimalOrNull()?.takeIf { it > 0 }?.let { MachUnit.toBase(it, unit) }
}

@Composable
private fun AmountField(label: String, amount: Amount, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = amount.text,
        onValueChange = { if (it.isValidDecimalInput()) amount.text = it },
        modifier = modifier,
        singleLine = true,
        label = { Text(label, maxLines = 1) },
        trailingIcon = {
            UnitPicker(amount.unit) { picked ->
                amount.text.toDecimalOrNull()?.let { amount.text = Fmt.editable(MachUnit.convert(it, amount.unit, picked)) }
                amount.unit = picked
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = NumberFamily, fontWeight = FontWeight.Medium),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
    )
}
