package com.reymildo.calculadoradelmetal.ui.machining

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.domain.machining.MachiningDraft
import com.reymildo.calculadoradelmetal.domain.machining.MachiningLinks
import com.reymildo.calculadoradelmetal.domain.machining.Quantity
import com.reymildo.calculadoradelmetal.domain.machining.unitOf
import com.reymildo.calculadoradelmetal.domain.machining.MachiningUnitSystem
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MachiningViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val _turning = mutableStateOf(MachiningLinks.recompute(load(TURNING_KEY) ?: defaultTurning()))
    val turning: State<MachiningDraft> = _turning

    private val _milling = mutableStateOf(MachiningLinks.recompute(load(MILLING_KEY) ?: defaultMilling()))
    val milling: State<MachiningDraft> = _milling

    fun updateTurning(transform: (MachiningDraft) -> MachiningDraft) {
        _turning.value = transform(_turning.value)
        save(TURNING_KEY, _turning.value)
    }

    fun updateMilling(transform: (MachiningDraft) -> MachiningDraft) {
        _milling.value = transform(_milling.value)
        save(MILLING_KEY, _milling.value)
    }

    /** Pone todos los valores en el sistema elegido (atajo); cada campo puede cambiarse luego por separado. */
    fun switchUnits(turning: Boolean, target: MachiningUnitSystem) {
        val source = if (turning) _turning.value else _milling.value
        val lengthKeys = if (turning) TURNING_LENGTH_KEYS else MILLING_LENGTH_KEYS
        val quantities = lengthKeys.associateWith { Quantity.LENGTH } + ("vc" to Quantity.CUTTING_SPEED) + ("vf" to Quantity.FEED_RATE)
        val units = source.fieldUnits.toMutableMap()
        val converted = source.fields.toMutableMap()
        quantities.forEach { (key, quantity) ->
            val from = source.unitOf(key, quantity)
            val to = MachUnit.default(target, quantity)
            val value = source.fields[key]?.replace(',', '.')?.toDoubleOrNull()
            if (value != null && from != to) converted[key] = format(MachUnit.convert(value, from, to))
            units[key] = to.name
        }
        val next = source.copy(fields = converted, unitSystem = target, fieldUnits = units)
        if (turning) {
            _turning.value = next
            save(TURNING_KEY, next)
        } else {
            _milling.value = next
            save(MILLING_KEY, next)
        }
    }

    private fun load(key: String): MachiningDraft? = savedStateHandle.get<String>(key)?.let {
        runCatching { json.decodeFromString<MachiningDraft>(it) }.getOrNull()
    }

    private fun save(key: String, draft: MachiningDraft) {
        savedStateHandle[key] = json.encodeToString(draft)
    }

    companion object {
        private const val TURNING_KEY = "turning_draft"
        private const val MILLING_KEY = "milling_draft"
        private val TURNING_LENGTH_KEYS = setOf(
            "initialDiameter", "finalDiameter", "cutLength", "feed", "depth", "finish",
            "grooveWidth", "toolWidth", "pitch",
        )
        private val MILLING_LENGTH_KEYS = setOf("diameter", "fz", "ap", "ae", "totalDepth", "path", "finish")

        private fun defaultTurning() = MachiningDraft(
            operation = TurningOperation.TURNING.name,
            fields = mapOf(
                "initialDiameter" to "50", "finalDiameter" to "40", "cutLength" to "100",
                "vc" to "180", "feed" to "0.2", "depth" to "2", "finish" to "0.5",
                "grooveWidth" to "5", "toolWidth" to "3", "pitch" to "1.5",
                "starts" to "1", "threadPasses" to "8", "css" to "0",
            ),
        )

        private fun defaultMilling() = MachiningDraft(
            operation = MillingOperation.FACE.name,
            fields = mapOf(
                "diameter" to "10", "vc" to "100", "teeth" to "4", "fz" to "0.05",
                "ap" to "2", "ae" to "5", "totalDepth" to "6", "path" to "100", "finish" to "0",
            ),
        )

        private fun format(value: Double) = java.math.BigDecimal.valueOf(value)
            .setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
