package com.reymildo.calculadoradelmetal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineLimits
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import kotlinx.serialization.Serializable

/**
 * Una máquina del taller. [kind] (convencional/CNC) y [machineType] (torno/fresadora) se guardan
 * por separado: forman cuatro categorías con características muy distintas. Todas las medidas
 * están en milímetros.
 */
@Serializable
@Entity(tableName = "machine_profiles")
data class MachineProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val maxRpm: Double,
    val maxFeedMmMin: Double,
    val steppedRpmCsv: String = "",
    val maxThreadingRpm: Double? = null,
    val maxPartingRpm: Double? = null,
    val isBuiltIn: Boolean = false,
    val machineType: String = MachineType.LATHE.name,
    val brand: String = "",
    val model: String = "",
    val minRpm: Double? = null,
    val powerKw: Double? = null,
    val swingMm: Double? = null,
    val centersDistanceMm: Double? = null,
    val chuckMm: Double? = null,
    val spindleBoreMm: Double? = null,
    val travelXMm: Double? = null,
    val travelYMm: Double? = null,
    val travelZMm: Double? = null,
    val spindleTaper: String = "",
    val axes: Int? = null,
    val toolCapacity: Int? = null,
    val notes: String = "",
) {
    val controlKind: MachineKind
        get() = runCatching { MachineKind.valueOf(kind) }.getOrDefault(MachineKind.CNC)

    val type: MachineType
        get() = runCatching { MachineType.valueOf(machineType) }.getOrDefault(MachineType.LATHE)

    fun toLimits(): MachineLimits = MachineLimits(
        kind = controlKind,
        maxRpm = maxRpm,
        maxFeedMmMin = maxFeedMmMin,
        steppedRpm = steppedRpmCsv.split(',').mapNotNull { it.trim().toDoubleOrNull() },
        maxThreadingRpm = maxThreadingRpm,
        maxPartingRpm = maxPartingRpm,
    )
}
