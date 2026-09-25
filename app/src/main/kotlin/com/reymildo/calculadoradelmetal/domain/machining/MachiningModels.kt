package com.reymildo.calculadoradelmetal.domain.machining

import kotlinx.serialization.Serializable

/** Tipo de control: una máquina convencional tiene velocidades escalonadas y sin G96/CSS. */
enum class MachineKind { CONVENTIONAL, CNC }

/** Torno y fresadora son máquinas distintas aunque compartan el tipo de control. */
enum class MachineType { LATHE, MILL }
enum class MachiningUnitSystem { METRIC, IMPERIAL }
enum class TurningOperation { TURNING, BORING, FACING, GROOVING, PARTING, THREADING }
enum class MillingOperation { FACE, CONTOUR, SLOT }

enum class ToolKind { HSS, CARBIDE_INSERT, CARBIDE_ENDMILL }

/** Familia del material a mecanizar; sirve para elegir el valor genérico conservador. */
enum class MaterialCategory { CARBON_STEEL, ALLOY_STEEL, STAINLESS, ALUMINUM, CAST_IRON, COPPER_ALLOY, TITANIUM, PLASTIC }

data class MachineLimits(
    val kind: MachineKind,
    val maxRpm: Double,
    val maxFeedMmMin: Double,
    val steppedRpm: List<Double> = emptyList(),
    val maxThreadingRpm: Double? = null,
    val maxPartingRpm: Double? = null,
)

enum class RecommendationSource { MANUFACTURER, GENERIC }

/** Rango de trabajo (métrico): Vc en m/min y avance en mm/rev (torno) o mm/diente (fresa). */
data class CuttingRecommendation(
    val cuttingSpeedMinMMin: Double,
    val cuttingSpeedStartMMin: Double,
    val cuttingSpeedMaxMMin: Double,
    val feedMinMm: Double,
    val feedStartMm: Double,
    val feedMaxMm: Double,
    val depthMaxMm: Double? = null,
    val source: RecommendationSource = RecommendationSource.GENERIC,
)

data class PassPlan(
    val roughPasses: Int,
    val finishPasses: Int,
    val totalPasses: Int,
    val roughDepthMm: Double,
    val lastRoughDepthMm: Double,
)

data class MachiningResult(
    val theoreticalRpm: Double,
    val adjustedRpm: Double,
    val actualCuttingSpeedMMin: Double,
    val feedMmMin: Double,
    val feedPerRevolutionMm: Double,
    val passes: PassPlan,
    val cuttingTimeMin: Double,
    val removalRateCm3Min: Double?,
    val removedVolumeCm3: Double?,
    val warnings: List<String>,
)

data class MillingInput(
    val operation: MillingOperation,
    val cutterDiameterMm: Double,
    val cuttingSpeedMMin: Double,
    val effectiveTeeth: Int,
    val feedPerToothMm: Double,
    val axialDepthPerPassMm: Double,
    val radialWidthMm: Double,
    val totalDepthMm: Double,
    val pathLengthPerLevelMm: Double,
    val finishAllowanceMm: Double = 0.0,
)

data class TurningInput(
    val operation: TurningOperation,
    val initialDiameterMm: Double,
    val finalDiameterMm: Double,
    val cuttingLengthMm: Double,
    val cuttingSpeedMMin: Double,
    val feedPerRevolutionMm: Double,
    val depthPerPassMm: Double,
    val finishAllowanceMm: Double = 0.0,
    val grooveWidthMm: Double = 0.0,
    val toolWidthMm: Double = 0.0,
    val threadPitchMm: Double = 0.0,
    val threadStarts: Int = 1,
    val threadPasses: Int = 0,
    val constantSurfaceSpeed: Boolean = false,
)

@Serializable
data class MachiningDraft(
    val operation: String,
    val fields: Map<String, String> = emptyMap(),
    val unitSystem: MachiningUnitSystem = MachiningUnitSystem.METRIC,
    val materialId: String = "a36",
    val toolId: Long? = null,
    val machineProfileId: Long? = null,
    val recommendationApplied: Boolean = false,
)
