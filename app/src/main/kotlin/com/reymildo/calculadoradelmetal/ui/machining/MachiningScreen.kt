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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reymildo.calculadoradelmetal.R
import com.reymildo.calculadoradelmetal.data.local.entity.MachineProfileEntity
import com.reymildo.calculadoradelmetal.data.local.relation.ToolWithRecommendations
import com.reymildo.calculadoradelmetal.domain.machining.CuttingRecommendation
import com.reymildo.calculadoradelmetal.domain.machining.FieldIssue
import com.reymildo.calculadoradelmetal.domain.machining.IssueCode
import com.reymildo.calculadoradelmetal.domain.machining.IssueSeverity
import com.reymildo.calculadoradelmetal.domain.machining.IsoGroup
import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCalculator
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCatalog
import com.reymildo.calculadoradelmetal.domain.machining.MachiningDraft
import com.reymildo.calculadoradelmetal.domain.machining.MachiningLinks
import com.reymildo.calculadoradelmetal.domain.machining.MachiningResult
import com.reymildo.calculadoradelmetal.domain.machining.MachiningValidator
import com.reymildo.calculadoradelmetal.domain.machining.MachiningUnitSystem
import com.reymildo.calculadoradelmetal.domain.machining.MillingInput
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.Quantity
import com.reymildo.calculadoradelmetal.domain.machining.RecommendationSource
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import com.reymildo.calculadoradelmetal.domain.machining.TurningInput
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation
import com.reymildo.calculadoradelmetal.domain.machining.unitOf
import com.reymildo.calculadoradelmetal.ui.calc.CalcMode
import com.reymildo.calculadoradelmetal.ui.common.ChoiceTile
import com.reymildo.calculadoradelmetal.ui.common.DropdownField
import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.IsoGroupPicker
import com.reymildo.calculadoradelmetal.ui.common.GlyphIcon
import com.reymildo.calculadoradelmetal.ui.common.SectionCard
import com.reymildo.calculadoradelmetal.ui.common.TileGrid
import com.reymildo.calculadoradelmetal.ui.common.UnitPicker
import com.reymildo.calculadoradelmetal.ui.common.isValidDecimalInput
import com.reymildo.calculadoradelmetal.ui.common.machineGlyph
import com.reymildo.calculadoradelmetal.ui.common.millingGlyph
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import com.reymildo.calculadoradelmetal.ui.common.turningGlyph
import com.reymildo.calculadoradelmetal.ui.theme.NumberFamily

/** Recomendación aplicable: valores del fabricante de la herramienta o, si no hay, los genéricos conservadores. */
private fun resolveRecommendation(
    tool: ToolWithRecommendations?,
    group: IsoGroup?,
    turning: Boolean,
): CuttingRecommendation? {
    if (tool == null || group == null) return null
    tool.recommendations.firstOrNull { it.isoGroup == group.name }?.let {
        return CuttingRecommendation(
            it.vcMin, it.vcStart, it.vcMax, it.feedMin, it.feedStart, it.feedMax, it.depthMaxMm,
            RecommendationSource.MANUFACTURER, it.isoGroup,
        )
    }
    return MachiningCatalog.generic(group, tool.tool.toolKind, turning)
}

@Composable
fun MachiningScreen(
    mode: CalcMode,
    machines: List<MachineProfileEntity>,
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
    val material = IsoGroup.entries.firstOrNull { it.name == draft.materialId } ?: IsoGroup.P

    // Coherencia de los valores: cada problema se marca en su campo y se resume abajo.
    val issues = MachiningValidator.validate(
        turning = turning,
        operation = draft.operation,
        value = { key ->
            draft.fields[key]?.toDecimalOrNull()?.let { number ->
                MachiningLinks.quantityOf(key)?.let { MachUnit.toBase(number, draft.unitOf(key, it)) } ?: number
            }
        },
        intValue = { key -> draft.fields[key]?.toIntOrNull() },
        machine = machine?.toLimits(),
        recommendation = resolveRecommendation(tool, material, turning),
    )
    val issueMap = issues.groupBy { it.key }.mapValues { (_, list) -> list.minBy { it.severity.ordinal } }
    val errors = issues.filter { it.severity == IssueSeverity.ERROR }

    var machinePickerOpen by rememberSaveable { mutableStateOf(false) }

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
                if (machine != null) {
                    MachineSelectorCard(
                        machine = machine,
                        kindLabel = kindLabels.getValue(machine.controlKind),
                        onClick = { machinePickerOpen = true },
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
                    ChoiceTile(turningGlyph(op), labels.getValue(op), op == current, { update { MachiningLinks.recompute(it.copy(operation = op.name)) } }, tileModifier)
                }
            } else {
                val current = runCatching { MillingOperation.valueOf(draft.operation) }.getOrDefault(MillingOperation.FACE)
                val labels = MillingOperation.entries.associateWith { millingOperationLabel(it) }
                TileGrid(items = MillingOperation.entries, columns = 3) { op, tileModifier ->
                    ChoiceTile(millingGlyph(op), labels.getValue(op), op == current, { update { MachiningLinks.recompute(it.copy(operation = op.name)) } }, tileModifier)
                }
            }
        }

        SectionCard(title = stringResource(R.string.mach_step_material_tool)) {
            if (tool == null) {
                Text(stringResource(R.string.mach_no_profiles), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    stringResource(R.string.mach_technical_material).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IsoGroupPicker(
                    selected = material,
                    taken = emptySet(),
                    onSelect = { picked -> update { it.copy(materialId = picked.name, recommendationApplied = false) } },
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
                                val feedKey = if (turning) "feed" else "fz"
                                val vc = MachUnit.convert(recommendation.cuttingSpeedStartMMin, MachUnit.M_MIN, current.unitOf("vc", Quantity.CUTTING_SPEED))
                                val feed = MachUnit.convert(recommendation.feedStartMm, MachUnit.MM, current.unitOf(feedKey, Quantity.LENGTH))
                                MachiningLinks.recompute(
                                    current.copy(
                                        fields = current.fields + ("vc" to Fmt.editable(vc)) + (feedKey to Fmt.editable(feed)),
                                        recommendationApplied = true,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.mach_apply_recommendation)) }
                    Text(
                        text = if (recommendation.source == RecommendationSource.MANUFACTURER) {
                            stringResource(R.string.mach_recommendation_manufacturer, tool.tool.name, recommendation.isoCode ?: material.name)
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
            if (turning) TurningFields(draft, machine?.controlKind == MachineKind.CNC, issueMap, update) else MillingFields(draft, issueMap, update)
        }

        if (errors.isNotEmpty()) {
            IssuesCard(errors, draft)
        } else {
            MachiningResultCard(machine?.let { calculate(draft, turning, it) })
        }
        Spacer(Modifier.height(16.dp))
    }

    if (machinePickerOpen) {
        val kindLabels = MachineKind.entries.associateWith { machineKindLabel(it) }
        MachinePickerDialog(
            machines = compatibleMachines,
            selectedId = machine?.id,
            kindLabels = kindLabels,
            onPick = { picked ->
                update { it.copy(machineProfileId = picked.id) }
                machinePickerOpen = false
            },
            onDismiss = { machinePickerOpen = false },
        )
    }
}

@Composable
private fun TurningFields(draft: MachiningDraft, cnc: Boolean, issues: Map<String, FieldIssue>, update: ((MachiningDraft) -> MachiningDraft) -> Unit) {
    val op = TurningOperation.valueOf(draft.operation)
    val finishHint = stringResource(R.string.mach_finish_hint)
    NumberRow(stringResource(R.string.mach_initial_diameter), draft, "initialDiameter", Quantity.LENGTH, update, issue = issues["initialDiameter"])
    NumberRow(stringResource(R.string.mach_final_diameter), draft, "finalDiameter", Quantity.LENGTH, update, issue = issues["finalDiameter"])
    when (op) {
        TurningOperation.FACING -> NumberRow(stringResource(R.string.mach_stock_to_remove), draft, "cutLength", Quantity.LENGTH, update, issue = issues["cutLength"])
        TurningOperation.GROOVING, TurningOperation.PARTING -> Unit
        else -> NumberRow(stringResource(R.string.dim_length), draft, "cutLength", Quantity.LENGTH, update, issue = issues["cutLength"])
    }
    NumberRow(stringResource(R.string.mach_cutting_speed), draft, "vc", Quantity.CUTTING_SPEED, update, issue = issues["vc"])
    NumberRow(stringResource(R.string.mach_spindle_rpm), draft, "rpm", null, update, "RPM", issue = issues["rpm"])
    if (op != TurningOperation.THREADING) {
        NumberRow(stringResource(R.string.mach_feed_rev), draft, "feed", Quantity.LENGTH, update, "/rev", issue = issues["feed"])
        NumberRow(stringResource(R.string.mach_feed_rate), draft, "vf", Quantity.FEED_RATE, update, issue = issues["vf"])
    }
    if (op !in setOf(TurningOperation.PARTING, TurningOperation.THREADING)) {
        NumberRow(stringResource(R.string.mach_depth_pass), draft, "depth", Quantity.LENGTH, update, issue = issues["depth"])
    }
    if (op in setOf(TurningOperation.TURNING, TurningOperation.BORING, TurningOperation.FACING)) {
        NumberRow(stringResource(R.string.mach_finish_allowance), draft, "finish", Quantity.LENGTH, update, hint = finishHint, issue = issues["finish"])
    }
    if (op == TurningOperation.GROOVING) {
        NumberRow(stringResource(R.string.mach_groove_width), draft, "grooveWidth", Quantity.LENGTH, update, issue = issues["grooveWidth"])
        NumberRow(stringResource(R.string.mach_tool_width), draft, "toolWidth", Quantity.LENGTH, update, issue = issues["toolWidth"])
    }
    if (op == TurningOperation.THREADING) {
        NumberRow(stringResource(R.string.mach_thread_pitch), draft, "pitch", Quantity.LENGTH, update, issue = issues["pitch"])
        IntegerRow(stringResource(R.string.mach_thread_starts), draft, "starts", update, issues["starts"])
        IntegerRow(stringResource(R.string.mach_thread_passes), draft, "threadPasses", update, issues["threadPasses"])
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
private fun MillingFields(draft: MachiningDraft, issues: Map<String, FieldIssue>, update: ((MachiningDraft) -> MachiningDraft) -> Unit) {
    val perTooth = stringResource(R.string.unit_per_tooth)
    val finishHint = stringResource(R.string.mach_finish_hint)
    NumberRow(stringResource(R.string.mach_cutter_diameter), draft, "diameter", Quantity.LENGTH, update, issue = issues["diameter"])
    NumberRow(stringResource(R.string.mach_cutting_speed), draft, "vc", Quantity.CUTTING_SPEED, update, issue = issues["vc"])
    NumberRow(stringResource(R.string.mach_spindle_rpm), draft, "rpm", null, update, "RPM", issue = issues["rpm"])
    IntegerRow(stringResource(R.string.mach_effective_teeth), draft, "teeth", update, issues["teeth"])
    NumberRow(stringResource(R.string.mach_feed_tooth), draft, "fz", Quantity.LENGTH, update, perTooth, issue = issues["fz"])
    NumberRow(stringResource(R.string.mach_feed_rate), draft, "vf", Quantity.FEED_RATE, update, issue = issues["vf"])
    NumberRow(stringResource(R.string.mach_axial_depth), draft, "ap", Quantity.LENGTH, update, issue = issues["ap"])
    NumberRow(stringResource(R.string.mach_radial_width), draft, "ae", Quantity.LENGTH, update, issue = issues["ae"])
    NumberRow(stringResource(R.string.mach_radial_pct), draft, "aePct", null, update, "%", issue = issues["aePct"])
    NumberRow(stringResource(R.string.mach_total_depth), draft, "totalDepth", Quantity.LENGTH, update, issue = issues["totalDepth"])
    NumberRow(stringResource(R.string.mach_path_level), draft, "path", Quantity.LENGTH, update, issue = issues["path"])
    NumberRow(stringResource(R.string.mach_finish_allowance), draft, "finish", Quantity.LENGTH, update, hint = finishHint, issue = issues["finish"])
}

private fun calculate(draft: MachiningDraft, turning: Boolean, profile: MachineProfileEntity): Result<MachiningResult> {
    fun number(key: String): Double = draft.fields[key]?.toDecimalOrNull() ?: Double.NaN
    fun mm(key: String): Double = MachUnit.toBase(number(key), draft.unitOf(key, Quantity.LENGTH))
    fun mmOrZero(key: String): Double = if (draft.fields[key].isNullOrBlank()) 0.0 else mm(key)
    fun vc(): Double = MachUnit.toBase(number("vc"), draft.unitOf("vc", Quantity.CUTTING_SPEED))
    return if (turning) {
        val op = TurningOperation.valueOf(draft.operation)
        MachiningCalculator.turning(
            TurningInput(
                operation = op,
                initialDiameterMm = mm("initialDiameter"), finalDiameterMm = mm("finalDiameter"),
                cuttingLengthMm = if (op in setOf(TurningOperation.GROOVING, TurningOperation.PARTING)) 1.0 else mm("cutLength"),
                cuttingSpeedMMin = vc(), feedPerRevolutionMm = if (op == TurningOperation.THREADING) 1.0 else mm("feed"),
                depthPerPassMm = if (op in setOf(TurningOperation.PARTING, TurningOperation.THREADING)) 1.0 else mm("depth"),
                finishAllowanceMm = if (op in setOf(TurningOperation.TURNING, TurningOperation.BORING, TurningOperation.FACING)) mmOrZero("finish") else 0.0,
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
                totalDepthMm = mm("totalDepth"), pathLengthPerLevelMm = mm("path"), finishAllowanceMm = mmOrZero("finish"),
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
private fun NumberRow(
    label: String,
    draft: MachiningDraft,
    key: String,
    quantity: Quantity?,
    update: ((MachiningDraft) -> MachiningDraft) -> Unit,
    suffix: String = "",
    hint: String? = null,
    issue: FieldIssue? = null,
) {
    val unit = quantity?.let { draft.unitOf(key, it) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = draft.fields[key].orEmpty(),
            onValueChange = { candidate -> if (candidate.isValidDecimalInput()) update { MachiningLinks.apply(it, key, candidate) } },
            trailingIcon = {
                if (unit != null && quantity != null) {
                    UnitPicker(unit, suffix) { picked ->
                        update { current ->
                            val old = current.unitOf(key, quantity)
                            val converted = current.fields[key]?.toDecimalOrNull()?.let { Fmt.editable(MachUnit.convert(it, old, picked)) }
                            current.copy(
                                fields = if (converted != null) current.fields + (key to converted) else current.fields,
                                fieldUnits = current.fieldUnits + (key to picked.name),
                                recommendationApplied = false,
                            )
                        }
                    }
                } else if (suffix.isNotEmpty()) {
                    Text(suffix, Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            isError = issue?.severity == IssueSeverity.ERROR,
            supportingText = issue?.let { { IssueText(it, unit) } },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1.5f),
        )
    }
}

@Composable
private fun IntegerRow(
    label: String,
    draft: MachiningDraft,
    key: String,
    update: ((MachiningDraft) -> MachiningDraft) -> Unit,
    issue: FieldIssue? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = draft.fields[key].orEmpty(),
            onValueChange = { candidate -> if (candidate.all(Char::isDigit)) update { MachiningLinks.apply(it, key, candidate) } },
            isError = issue?.severity == IssueSeverity.ERROR,
            supportingText = issue?.let { { IssueText(it, null) } },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.5f),
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

/** Muestra la máquina elegida; al tocarla se abre la ventana para cambiarla. */
@Composable
private fun MachineSelectorCard(machine: MachineProfileEntity, kindLabel: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlyphIcon(machineGlyph(machine.type), MaterialTheme.colorScheme.primary, size = 36.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(machine.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    kindLabel + " · " + Fmt.number(machine.maxRpm, 0) + " RPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(stringResource(R.string.mach_change), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("⌄", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MachinePickerDialog(
    machines: List<MachineProfileEntity>,
    selectedId: Long?,
    kindLabels: Map<MachineKind, String>,
    onPick: (MachineProfileEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.mach_pick_machine), style = MaterialTheme.typography.titleLarge)
            machines.forEach { item ->
                val selected = item.id == selectedId
                Card(
                    onClick = { onPick(item) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GlyphIcon(machineGlyph(item.type), MaterialTheme.colorScheme.primary, size = 34.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                kindLabels.getValue(item.controlKind) + " · " + Fmt.number(item.maxRpm, 0) + " RPM · " +
                                    Fmt.number(item.maxFeedMmMin, 0) + " mm/min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val maker = listOf(item.brand, item.model).filter { it.isNotBlank() }.joinToString(" ")
                            if (maker.isNotBlank()) {
                                Text(maker, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun issueMessage(issue: FieldIssue, unit: MachUnit?): String = when (issue.code) {
    IssueCode.REQUIRED -> stringResource(R.string.issue_required)
    IssueCode.REQUIRED_ZERO_OK -> stringResource(R.string.issue_required_zero_ok)
    IssueCode.MIN_ONE -> stringResource(R.string.issue_min_one)
    IssueCode.FINAL_GT_INITIAL -> stringResource(R.string.issue_final_gt_initial)
    IssueCode.FINAL_LT_INITIAL -> stringResource(R.string.issue_final_lt_initial)
    IssueCode.FINISH_TOO_BIG -> stringResource(R.string.issue_finish_too_big)
    IssueCode.RADIAL_EXCEEDS_DIAMETER -> stringResource(R.string.issue_radial_exceeds)
    IssueCode.NO_STEP_LOW -> stringResource(R.string.issue_no_step, Fmt.number(issue.a ?: 0.0, 0))
    IssueCode.SINGLE_PASS -> stringResource(R.string.issue_single_pass)
    IssueCode.RPM_OVER_MAX -> stringResource(R.string.issue_rpm_over_max, Fmt.number(issue.a ?: 0.0, 0))
    IssueCode.FEED_OVER_MAX -> {
        val shown = if (unit != null) MachUnit.convert(issue.a ?: 0.0, MachUnit.base(unit.quantity), unit) else issue.a ?: 0.0
        stringResource(R.string.issue_feed_over_max, Fmt.number(shown, 0) + (unit?.let { " " + it.symbol } ?: ""))
    }
    IssueCode.OUT_OF_RANGE -> {
        val lo = issue.a ?: 0.0
        val hi = issue.b ?: 0.0
        val decimals = if (unit?.quantity == Quantity.CUTTING_SPEED) 0 else 3
        val (from, to) = if (unit != null) {
            MachUnit.convert(lo, MachUnit.base(unit.quantity), unit) to MachUnit.convert(hi, MachUnit.base(unit.quantity), unit)
        } else lo to hi
        stringResource(R.string.issue_out_of_range, Fmt.number(from, decimals), Fmt.number(to, decimals)) + (unit?.let { " " + it.symbol } ?: "")
    }
}

@Composable
private fun IssueText(issue: FieldIssue, unit: MachUnit?) {
    Text(
        issueMessage(issue, unit),
        color = if (issue.severity == IssueSeverity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun fieldLabel(key: String, operation: String): String = stringResource(
    when (key) {
        "initialDiameter" -> R.string.mach_initial_diameter
        "finalDiameter" -> R.string.mach_final_diameter
        "cutLength" -> if (operation == TurningOperation.FACING.name) R.string.mach_stock_to_remove else R.string.dim_length
        "vc" -> R.string.mach_cutting_speed
        "feed" -> R.string.mach_feed_rev
        "depth" -> R.string.mach_depth_pass
        "finish" -> R.string.mach_finish_allowance
        "grooveWidth" -> R.string.mach_groove_width
        "toolWidth" -> R.string.mach_tool_width
        "pitch" -> R.string.mach_thread_pitch
        "starts" -> R.string.mach_thread_starts
        "threadPasses" -> R.string.mach_thread_passes
        "diameter" -> R.string.mach_cutter_diameter
        "teeth" -> R.string.mach_effective_teeth
        "fz" -> R.string.mach_feed_tooth
        "ap" -> R.string.mach_axial_depth
        "ae" -> R.string.mach_radial_width
        "totalDepth" -> R.string.mach_total_depth
        "rpm" -> R.string.mach_spindle_rpm
        "vf" -> R.string.mach_feed_rate
        "aePct" -> R.string.mach_radial_pct
        else -> R.string.mach_path_level
    },
)

/** Sustituye al resultado mientras haya valores incoherentes: dice qué campo corregir y por qué. */
@Composable
private fun IssuesCard(errors: List<FieldIssue>, draft: MachiningDraft) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.issue_summary_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            errors.forEach { issue ->
                val unit = MachiningLinks.quantityOf(issue.key)?.let { draft.unitOf(issue.key, it) }
                Text(
                    "• " + fieldLabel(issue.key, draft.operation) + ": " + issueMessage(issue, unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
