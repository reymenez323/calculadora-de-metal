@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.reymildo.calculadoradelmetal.ui.machining

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.data.local.entity.MachiningMaterialEntity
import com.reymildo.calculadoradelmetal.data.local.relation.ToolWithRecommendations
import com.reymildo.calculadoradelmetal.domain.machining.CuttingRecommendation
import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCalculator
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCatalog
import com.reymildo.calculadoradelmetal.domain.machining.MachiningDraft
import com.reymildo.calculadoradelmetal.domain.machining.MachiningResult
import com.reymildo.calculadoradelmetal.domain.machining.MachiningUnitSystem
import com.reymildo.calculadoradelmetal.domain.machining.MillingInput
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.RecommendationSource
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import com.reymildo.calculadoradelmetal.domain.machining.TurningInput
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation
import com.reymildo.calculadoradelmetal.ui.calc.CalcMode
import com.reymildo.calculadoradelmetal.ui.common.ChoiceTile
import com.reymildo.calculadoradelmetal.ui.common.DropdownField
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.TileGrid
import com.reymildo.calculadoradelmetal.ui.common.isValidDecimalInput
import com.reymildo.calculadoradelmetal.ui.common.machineGlyph
import com.reymildo.calculadoradelmetal.ui.common.millingGlyph
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.common.turningGlyph
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

/** Recomendación aplicable: valores del fabricante de la herramienta o, si no hay, los genéricos conservadores. */
private fun resolveRecommendation(
    tool: ToolWithRecommendations?,
    material: MachiningMaterialEntity?,
    turning: Boolean,
): CuttingRecommendation? {
    if (tool == null || material == null) return null
    tool.recommendations.firstOrNull { it.materialId == material.id }?.let {
        return CuttingRecommendation(
            it.vcMin, it.vcStart, it.vcMax, it.feedMin, it.feedStart, it.feedMax, it.depthMaxMm,
            RecommendationSource.MANUFACTURER,
        )
    }
    return MachiningCatalog.generic(material.materialCategory, tool.tool.toolKind, turning)
}

@Composable
fun MachiningScreen(
    mode: CalcMode,
    machines: List<MachineProfileEntity>,
    materials: List<MachiningMaterialEntity>,
    tools: List<ToolWithRecommendations>,
    onOpenMachines: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MachiningViewModel = viewModel(),
) {
    val turning = mode == CalcMode.TORNEADO
    val type = if (turning) MachineType.LATHE else MachineType.MILL
    val draft by if (turning) viewModel.turning else viewModel.milling
    val update: ((MachiningDraft) -> MachiningDraft) -> Unit = if (turning) viewModel::updateTurning else viewModel::updateMilling

    val compatibleMachines = machines.filter { it.type == type }
    val machine = compatibleMachines.firstOrNull { it.id == draft.machineProfileId } ?: compatibleMachines.firstOrNull()
    val compatibleTools = tools.filter { it.tool.fits(type) }
    // Sin elección previa: el perfil genérico de carburo (inserto en torno, fresa en fresadora).
    val defaultKind = if (turning) ToolKind.CARBIDE_INSERT else ToolKind.CARBIDE_ENDMILL
    val tool = compatibleTools.firstOrNull { it.tool.id == draft.toolId }
        ?: compatibleTools.firstOrNull { it.tool.isBuiltIn && it.tool.toolKind == defaultKind }
        ?: compatibleTools.firstOrNull()
    val material = materials.firstOrNull { it.id == draft.materialId } ?: materials.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(title = stringResource(R.string.mach_step_machine)) {
            if (compatibleMachines.isEmpty()) {
                Text(
                    stringResource(if (turning) R.string.mach_no_lathe else R.string.mach_no_mill),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenMachines, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mach_go_add_machine))
                }
            } else {
                val kindLabels = MachineKind.entries.associateWith { machineKindLabel(it) }
                TileGrid(items = compatibleMachines, columns = 2) { item, tileModifier ->
                    ChoiceTile(
                        glyph = machineGlyph(item.type),
                        label = item.name,
                        caption = kindLabels.getValue(item.controlKind) + " · " + Fmt.number(item.maxRpm, 0) + " RPM",
                        selected = item.id == machine?.id,
                        onClick = { update { it.copy(machineProfileId = item.id) } },
                        modifier = tileModifier,
                    )
                }
                if (machine != null) {
                    Text(
                        stringResource(R.string.mach_machine_limits, Fmt.number(machine.maxRpm, 0), Fmt.number(machine.maxFeedMmMin, 0)) +
                            (machine.powerKw?.let { " · " + Fmt.number(it, 1) + " kW" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.mach_step_operation)) {
            if (turning) {
                val current = runCatching { TurningOperation.valueOf(draft.operation) }.getOrDefault(TurningOperation.TURNING)
                val labels = TurningOperation.entries.associateWith { turningOperationLabel(it) }
                TileGrid(items = TurningOperation.entries, columns = 3) { op, tileModifier ->
                    ChoiceTile(turningGlyph(op), labels.getValue(op), op == current, { update { it.copy(operation = op.name) } }, tileModifier)
                }
            } else {
                val current = runCatching { MillingOperation.valueOf(draft.operation) }.getOrDefault(MillingOperation.FACE)
                val labels = MillingOperation.entries.associateWith { millingOperationLabel(it) }
                TileGrid(items = MillingOperation.entries, columns = 3) { op, tileModifier ->
                    ChoiceTile(millingGlyph(op), labels.getValue(op), op == current, { update { it.copy(operation = op.name) } }, tileModifier)
                }
            }
        }

        SectionCard(title = stringResource(R.string.mach_step_material_tool)) {
            if (material == null || tool == null) {
                Text(stringResource(R.string.mach_no_profiles), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                DropdownField(
                    label = stringResource(R.string.mach_technical_material),
                    selected = listOf(material.name, material.condition).filter { it.isNotBlank() }.joinToString(" · "),
                    options = materials.map { it.id to listOf(it.name, it.condition).filter { part -> part.isNotBlank() }.joinToString(" · ") },
                    onSelect = { id -> update { it.copy(materialId = id, recommendationApplied = false) } },
                )
                DropdownField(
                    label = stringResource(R.string.mach_tool),
                    selected = tool.tool.name,
                    options = compatibleTools.map { it.tool.id.toString() to it.tool.name },
                    onSelect = { id -> update { it.copy(toolId = id.toLongOrNull(), recommendationApplied = false) } },
                )
                val recommendation = resolveRecommendation(tool, material, turning)
                if (recommendation != null) {
                    Text(
                        text = stringResource(
                            R.string.mach_recommendation_range,
                            Fmt.number(recommendation.cuttingSpeedMinMMin, 0),
                            Fmt.number(recommendation.cuttingSpeedMaxMMin, 0),
                            Fmt.number(recommendation.feedMinMm, 3),
                            Fmt.number(recommendation.feedMaxMm, 3),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    recommendation.depthMaxMm?.let {
                        Text(
                            stringResource(R.string.mach_recommendation_depth, Fmt.number(it, 2)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            update { current ->
                                val imperial = current.unitSystem == MachiningUnitSystem.IMPERIAL
                                val vc = recommendation.cuttingSpeedStartMMin * if (imperial) 3.280839895 else 1.0
                                val feed = recommendation.feedStartMm / if (imperial) 25.4 else 1.0
                                current.copy(
                                    fields = current.fields + ("vc" to Fmt.editable(vc)) +
                                        ((if (turning) "feed" else "fz") to Fmt.editable(feed)),
                                    recommendationApplied = true,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.mach_apply_recommendation)) }
                    Text(
                        text = if (recommendation.source == RecommendationSource.MANUFACTURER) {
                            stringResource(R.string.mach_recommendation_manufacturer, tool.tool.name)
                        } else {
                            stringResource(R.string.mach_recommendation_generic)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.mach_step_parameters)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnitButton(
                    text = stringResource(R.string.mach_metric),
                    selected = draft.unitSystem == MachiningUnitSystem.METRIC,
                    onClick = { viewModel.switchUnits(turning, MachiningUnitSystem.METRIC) },
                    modifier = Modifier.weight(1f),
                )
                UnitButton(
                    text = stringResource(R.string.mach_imperial),
                    selected = draft.unitSystem == MachiningUnitSystem.IMPERIAL,
                    onClick = { viewModel.switchUnits(turning, MachiningUnitSystem.IMPERIAL) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (turning) TurningFields(draft, machine?.controlKind == MachineKind.CNC, update) else MillingFields(draft, update)
        }

        val result = machine?.let { calculate(draft, turning, it) }
        MachiningResultCard(result)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TurningFields(draft: MachiningDraft, cnc: Boolean, update: ((MachiningDraft) -> MachiningDraft) -> Unit) {
    val op = TurningOperation.valueOf(draft.operation)
    val unit = if (draft.unitSystem == MachiningUnitSystem.METRIC) "mm" else "in"
    val speed = if (draft.unitSystem == MachiningUnitSystem.METRIC) "m/min" else "SFM"
    NumberRow(stringResource(R.string.mach_initial_diameter), draft, "initialDiameter", unit, update)
    NumberRow(stringResource(R.string.mach_final_diameter), draft, "finalDiameter", unit, update)
    when (op) {
        TurningOperation.FACING -> NumberRow(stringResource(R.string.mach_stock_to_remove), draft, "cutLength", unit, update)
        TurningOperation.GROOVING, TurningOperation.PARTING -> Unit
        else -> NumberRow(stringResource(R.string.dim_length), draft, "cutLength", unit, update)
    }
    NumberRow(stringResource(R.string.mach_cutting_speed), draft, "vc", speed, update)
    if (op != TurningOperation.THREADING) {
        NumberRow(stringResource(R.string.mach_feed_rev), draft, "feed", "$unit/rev", update)
    }
    if (op !in setOf(TurningOperation.PARTING, TurningOperation.THREADING)) {
        NumberRow(stringResource(R.string.mach_depth_pass), draft, "depth", unit, update)
    }
    if (op in setOf(TurningOperation.TURNING, TurningOperation.BORING, TurningOperation.FACING)) {
        NumberRow(stringResource(R.string.mach_finish_allowance), draft, "finish", unit, update)
    }
    if (op == TurningOperation.GROOVING) {
        NumberRow(stringResource(R.string.mach_groove_width), draft, "grooveWidth", unit, update)
        NumberRow(stringResource(R.string.mach_tool_width), draft, "toolWidth", unit, update)
    }
    if (op == TurningOperation.THREADING) {
        NumberRow(stringResource(R.string.mach_thread_pitch), draft, "pitch", unit, update)
        IntegerRow(stringResource(R.string.mach_thread_starts), draft, "starts", update)
        IntegerRow(stringResource(R.string.mach_thread_passes), draft, "threadPasses", update)
    }
    if (cnc && op in setOf(TurningOperation.FACING, TurningOperation.GROOVING, TurningOperation.PARTING)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.mach_css), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.mach_css_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = draft.fields["css"] == "1",
                onCheckedChange = { checked -> update { it.copy(fields = it.fields + ("css" to if (checked) "1" else "0")) } },
            )
        }
    }
}

@Composable
private fun MillingFields(draft: MachiningDraft, update: ((MachiningDraft) -> MachiningDraft) -> Unit) {
    val unit = if (draft.unitSystem == MachiningUnitSystem.METRIC) "mm" else "in"
    val speed = if (draft.unitSystem == MachiningUnitSystem.METRIC) "m/min" else "SFM"
    NumberRow(stringResource(R.string.mach_cutter_diameter), draft, "diameter", unit, update)
    NumberRow(stringResource(R.string.mach_cutting_speed), draft, "vc", speed, update)
    IntegerRow(stringResource(R.string.mach_effective_teeth), draft, "teeth", update)
    NumberRow(stringResource(R.string.mach_feed_tooth), draft, "fz", "$unit/tooth", update)
    NumberRow(stringResource(R.string.mach_axial_depth), draft, "ap", unit, update)
    NumberRow(stringResource(R.string.mach_radial_width), draft, "ae", unit, update)
    NumberRow(stringResource(R.string.mach_total_depth), draft, "totalDepth", unit, update)
    NumberRow(stringResource(R.string.mach_path_level), draft, "path", unit, update)
    NumberRow(stringResource(R.string.mach_finish_allowance), draft, "finish", unit, update)
}

private fun calculate(draft: MachiningDraft, turning: Boolean, profile: MachineProfileEntity): Result<MachiningResult> {
    fun number(key: String): Double = draft.fields[key]?.toDecimalOrNull() ?: Double.NaN
    fun mm(key: String): Double = number(key) * if (draft.unitSystem == MachiningUnitSystem.IMPERIAL) 25.4 else 1.0
    fun vc(): Double = number("vc") / if (draft.unitSystem == MachiningUnitSystem.IMPERIAL) 3.280839895 else 1.0
    return if (turning) {
        val op = TurningOperation.valueOf(draft.operation)
        MachiningCalculator.turning(
            TurningInput(
                operation = op,
                initialDiameterMm = mm("initialDiameter"), finalDiameterMm = mm("finalDiameter"),
                cuttingLengthMm = if (op in setOf(TurningOperation.GROOVING, TurningOperation.PARTING)) 1.0 else mm("cutLength"),
                cuttingSpeedMMin = vc(), feedPerRevolutionMm = if (op == TurningOperation.THREADING) 1.0 else mm("feed"),
                depthPerPassMm = if (op in setOf(TurningOperation.PARTING, TurningOperation.THREADING)) 1.0 else mm("depth"),
                finishAllowanceMm = if (op in setOf(TurningOperation.TURNING, TurningOperation.BORING, TurningOperation.FACING)) mm("finish") else 0.0,
                grooveWidthMm = if (op == TurningOperation.GROOVING) mm("grooveWidth") else 0.0,
                toolWidthMm = if (op == TurningOperation.GROOVING) mm("toolWidth") else 0.0,
                threadPitchMm = if (op == TurningOperation.THREADING) mm("pitch") else 0.0,
                threadStarts = draft.fields["starts"]?.toIntOrNull() ?: 1,
                threadPasses = draft.fields["threadPasses"]?.toIntOrNull() ?: 0,
                constantSurfaceSpeed = draft.fields["css"] == "1" && profile.controlKind == MachineKind.CNC,
            ), profile.toLimits(),
        )
    } else {
        MachiningCalculator.milling(
            MillingInput(
                operation = MillingOperation.valueOf(draft.operation), cutterDiameterMm = mm("diameter"),
                cuttingSpeedMMin = vc(), effectiveTeeth = draft.fields["teeth"]?.toIntOrNull() ?: 0,
                feedPerToothMm = mm("fz"), axialDepthPerPassMm = mm("ap"), radialWidthMm = mm("ae"),
                totalDepthMm = mm("totalDepth"), pathLengthPerLevelMm = mm("path"), finishAllowanceMm = mm("finish"),
            ), profile.toLimits(),
        )
    }
}

@Composable
private fun MachiningResultCard(result: Result<MachiningResult>?) {
    val value = result?.getOrNull()
    if (result != null && value == null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text(stringResource(R.string.mach_check_inputs), Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.mach_results).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .6f))
            Text(value?.let { "${Fmt.number(it.adjustedRpm, 0)} RPM" } ?: "—", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
            ResultLine(stringResource(R.string.mach_theoretical_rpm), value?.let { Fmt.number(it.theoreticalRpm, 1) })
            ResultLine(stringResource(R.string.mach_actual_speed), value?.let { Fmt.number(it.actualCuttingSpeedMMin, 2) + " m/min" })
            ResultLine(stringResource(R.string.mach_feed_min), value?.let { Fmt.number(it.feedMmMin, 2) + " mm/min" })
            ResultLine(stringResource(R.string.mach_passes), value?.passes?.totalPasses?.toString())
            ResultLine(stringResource(R.string.mach_cutting_time), value?.let { Fmt.number(it.cuttingTimeMin, 2) + " min" })
            value?.removalRateCm3Min?.let { ResultLine(stringResource(R.string.mach_removal_rate), Fmt.number(it, 2) + " cm³/min") }
            value?.warnings?.forEach { warning ->
                Text(warningLabel(warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(stringResource(R.string.mach_time_scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .6f))
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String?) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .65f))
        Text(value ?: "—", fontFamily = NumberFamily, color = MaterialTheme.colorScheme.inverseOnSurface)
    }
}

@Composable
private fun NumberRow(label: String, draft: MachiningDraft, key: String, suffix: String, update: ((MachiningDraft) -> MachiningDraft) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = draft.fields[key].orEmpty(),
            onValueChange = { candidate -> if (candidate.isValidDecimalInput()) update { it.copy(fields = it.fields + (key to candidate), recommendationApplied = false) } },
            suffix = { Text(suffix) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun IntegerRow(label: String, draft: MachiningDraft, key: String, update: ((MachiningDraft) -> MachiningDraft) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = draft.fields[key].orEmpty(),
            onValueChange = { candidate -> if (candidate.all(Char::isDigit)) update { it.copy(fields = it.fields + (key to candidate)) } },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun UnitButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun turningOperationLabel(operation: TurningOperation): String = stringResource(when (operation) {
    TurningOperation.TURNING -> R.string.op_turning
    TurningOperation.BORING -> R.string.op_boring
    TurningOperation.FACING -> R.string.op_facing
    TurningOperation.GROOVING -> R.string.op_grooving
    TurningOperation.PARTING -> R.string.op_parting
    TurningOperation.THREADING -> R.string.op_threading
})

@Composable
private fun millingOperationLabel(operation: MillingOperation): String = stringResource(when (operation) {
    MillingOperation.FACE -> R.string.op_face_milling
    MillingOperation.CONTOUR -> R.string.op_contour
    MillingOperation.SLOT -> R.string.op_slot
})

@Composable
private fun warningLabel(key: String): String = stringResource(when (key) {
    "rpm_limited" -> R.string.mach_warning_rpm_limit
    "feed_limited" -> R.string.mach_warning_feed_limit
    "stepped_rpm" -> R.string.mach_warning_step
    "css_rpm_limit_applied" -> R.string.mach_warning_css
    else -> R.string.mach_warning_generic
})
@Composable
private fun machineKindLabel(kind: MachineKind): String = stringResource(
    if (kind == MachineKind.CNC) R.string.machine_kind_cnc else R.string.machine_kind_conventional,
)
