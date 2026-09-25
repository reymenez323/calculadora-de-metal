package com.reymildo.calculadoradelmetal.domain.machining

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

enum class IssueSeverity { ERROR, WARNING }

enum class IssueCode {
    /** Falta el valor o no es mayor que cero. */
    REQUIRED,

    /** Falta el valor o es negativo (el cero sí vale). */
    REQUIRED_ZERO_OK,
    MIN_ONE,
    FINAL_GT_INITIAL,
    FINAL_LT_INITIAL,
    FINISH_TOO_BIG,
    RADIAL_EXCEEDS_DIAMETER,

    /** La máquina convencional no tiene una velocidad tan baja; [FieldIssue.a] es su mínimo en RPM. */
    NO_STEP_LOW,

    /** Aviso: la pasada pedida es mayor que lo que hay que retirar, así que será una sola. */
    SINGLE_PASS,

    /** Aviso: las RPM escritas superan el máximo de la máquina ([FieldIssue.a]). */
    RPM_OVER_MAX,

    /** Aviso: el avance escrito supera el máximo de la máquina ([FieldIssue.a], en unidades base). */
    FEED_OVER_MAX,

    /** Aviso: fuera del rango recomendado; [FieldIssue.a] y [FieldIssue.b] en unidades base. */
    OUT_OF_RANGE,
}

/** Un problema (o aviso) de un campo concreto, identificado por su clave en el borrador. */
data class FieldIssue(
    val key: String,
    val code: IssueCode,
    val severity: IssueSeverity = IssueSeverity.ERROR,
    val a: Double? = null,
    val b: Double? = null,
)

/**
 * Comprueba que los valores del formulario sean coherentes entre sí ANTES de calcular, para poder
 * decirle al usuario exactamente qué campo corregir. Los valores llegan ya en unidades base
 * (mm, m/min); `null` significa vacío o ilegible.
 */
object MachiningValidator {
    fun validate(
        turning: Boolean,
        operation: String,
        value: (String) -> Double?,
        intValue: (String) -> Int?,
        machine: MachineLimits?,
        recommendation: CuttingRecommendation?,
    ): List<FieldIssue> = buildList {
        fun positive(key: String) {
            val v = value(key)
            if (v == null || v <= 0.0) add(FieldIssue(key, IssueCode.REQUIRED))
        }

        fun atLeastOne(key: String) {
            if ((intValue(key) ?: 0) < 1) add(FieldIssue(key, IssueCode.MIN_ONE))
        }

        val rpmDiameter: Double?
        if (turning) {
            val op = runCatching { TurningOperation.valueOf(operation) }.getOrDefault(TurningOperation.TURNING)
            positive("initialDiameter")
            val d0 = value("initialDiameter")?.takeIf { it > 0 }
            val d1 = value("finalDiameter")
            if (d1 == null || d1 < 0.0) {
                add(FieldIssue("finalDiameter", IssueCode.REQUIRED_ZERO_OK))
            } else if (d0 != null) {
                if (op == TurningOperation.BORING) {
                    if (d1 <= d0) add(FieldIssue("finalDiameter", IssueCode.FINAL_GT_INITIAL))
                } else if (d1 >= d0) {
                    add(FieldIssue("finalDiameter", IssueCode.FINAL_LT_INITIAL))
                }
            }
            if (op in setOf(TurningOperation.TURNING, TurningOperation.BORING, TurningOperation.FACING, TurningOperation.THREADING)) {
                positive("cutLength")
            }
            positive("vc")
            if (op != TurningOperation.THREADING) positive("feed")
            if (op !in setOf(TurningOperation.PARTING, TurningOperation.THREADING)) positive("depth")
            if (op == TurningOperation.GROOVING) {
                positive("grooveWidth")
                positive("toolWidth")
            }
            if (op == TurningOperation.THREADING) {
                positive("pitch")
                atLeastOne("starts")
                atLeastOne("threadPasses")
            }

            val relationOk = d0 != null && d1 != null && d1 >= 0.0 &&
                (if (op == TurningOperation.BORING) d1 > d0 else d1 < d0)
            val radial = if (relationOk) abs(d0!! - d1!!) / 2.0 else null
            val total = if (op == TurningOperation.FACING) value("cutLength")?.takeIf { it > 0 } else radial

            if (op in setOf(TurningOperation.TURNING, TurningOperation.BORING, TurningOperation.FACING)) {
                val finish = value("finish") ?: 0.0
                if (finish < 0.0) {
                    add(FieldIssue("finish", IssueCode.REQUIRED_ZERO_OK))
                } else if (total != null && finish >= total) {
                    add(FieldIssue("finish", IssueCode.FINISH_TOO_BIG))
                } else if (total != null) {
                    val depth = value("depth")
                    if (depth != null && depth > total - finish) {
                        add(FieldIssue("depth", IssueCode.SINGLE_PASS, IssueSeverity.WARNING))
                    }
                }
            } else if (op == TurningOperation.GROOVING && radial != null) {
                val depth = value("depth")
                if (depth != null && depth > radial) add(FieldIssue("depth", IssueCode.SINGLE_PASS, IssueSeverity.WARNING))
            }
            rpmDiameter = if (op == TurningOperation.BORING) d1?.takeIf { it > 0 } else d0
        } else {
            positive("diameter")
            positive("vc")
            atLeastOne("teeth")
            positive("fz")
            positive("ap")
            positive("ae")
            positive("totalDepth")
            positive("path")
            val diameter = value("diameter")?.takeIf { it > 0 }
            val ae = value("ae")?.takeIf { it > 0 }
            if (diameter != null && ae != null && ae > diameter) {
                add(FieldIssue("ae", IssueCode.RADIAL_EXCEEDS_DIAMETER))
            }
            val total = value("totalDepth")?.takeIf { it > 0 }
            val finish = value("finish") ?: 0.0
            if (finish < 0.0) {
                add(FieldIssue("finish", IssueCode.REQUIRED_ZERO_OK))
            } else if (total != null && finish >= total) {
                add(FieldIssue("finish", IssueCode.FINISH_TOO_BIG))
            } else if (total != null) {
                val ap = value("ap")
                if (ap != null && ap > total - finish) add(FieldIssue("ap", IssueCode.SINGLE_PASS, IssueSeverity.WARNING))
            }
            rpmDiameter = diameter
        }

        // Máquina convencional: hay que poder encontrar una velocidad escalonada a la altura del corte.
        val vc = value("vc")?.takeIf { it > 0 }
        if (machine != null && machine.kind == MachineKind.CONVENTIONAL && machine.steppedRpm.isNotEmpty() && vc != null && rpmDiameter != null) {
            val theoretical = vc * 1000.0 / (PI * rpmDiameter)
            val cap = when (operation) {
                TurningOperation.THREADING.name -> machine.maxThreadingRpm
                TurningOperation.PARTING.name -> machine.maxPartingRpm
                else -> null
            }
            val limited = min(theoretical, min(machine.maxRpm, cap ?: Double.MAX_VALUE))
            val lowest = machine.steppedRpm.filter { it.isFinite() && it > 0 }.minOrNull()
            if (lowest != null && lowest > limited) add(FieldIssue("vc", IssueCode.NO_STEP_LOW, a = lowest))
        }

        // Avisos: lo que se escribe directamente como RPM o avance en mm/min pasa del límite de la máquina.
        if (machine != null) {
            val rpmTyped = value("rpm")
            if (rpmTyped != null && rpmTyped > machine.maxRpm * 1.0001) {
                add(FieldIssue("rpm", IssueCode.RPM_OVER_MAX, IssueSeverity.WARNING, machine.maxRpm))
            }
            val feedRateTyped = value("vf")
            if (feedRateTyped != null && feedRateTyped > machine.maxFeedMmMin * 1.0001) {
                add(FieldIssue("vf", IssueCode.FEED_OVER_MAX, IssueSeverity.WARNING, machine.maxFeedMmMin))
            }
        }

        // Avisos (no bloquean): fuera del rango recomendado.
        if (recommendation != null) {
            val feedKey = if (turning) "feed" else "fz"
            val skipFeed = turning && operation == TurningOperation.THREADING.name
            fun range(key: String, v: Double?, lo: Double, hi: Double) {
                if (v == null || v <= 0 || any { it.key == key }) return
                if (v < lo * 0.999 || v > hi * 1.001) add(FieldIssue(key, IssueCode.OUT_OF_RANGE, IssueSeverity.WARNING, lo, hi))
            }
            range("vc", vc, recommendation.cuttingSpeedMinMMin, recommendation.cuttingSpeedMaxMMin)
            if (!skipFeed) range(feedKey, value(feedKey), recommendation.feedMinMm, recommendation.feedMaxMm)
        }
    }
}
