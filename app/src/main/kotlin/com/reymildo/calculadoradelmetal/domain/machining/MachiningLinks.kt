package com.reymildo.calculadoradelmetal.domain.machining

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI

/**
 * Relaciones entre los valores de un mismo cálculo. Cuando se edita uno, los que dependen de él se
 * actualizan al momento, y al revés:
 *
 *  - velocidad de corte Vc  ⇄  RPM del husillo (con el diámetro de referencia): n = Vc·1000 / (π·D)
 *  - avance por revolución/diente  ⇄  avance en mm/min (con RPM y dientes): vf = f·z·n
 *  - ancho radial ae  ⇄  ae como % del diámetro de la fresa
 *
 * Al cambiar el diámetro se conserva la Vc (y se recalculan las RPM y el avance) y se conserva ae
 * en mm (y se recalcula su %). Las unidades de cada campo se respetan.
 */
object MachiningLinks {
    fun isTurning(operation: String): Boolean = TurningOperation.entries.any { it.name == operation }

    /** Magnitud de una clave del borrador; null = valor sin unidad (RPM, %, contadores). */
    fun quantityOf(key: String): Quantity? = when (key) {
        "vc" -> Quantity.CUTTING_SPEED
        "vf" -> Quantity.FEED_RATE
        "rpm", "aePct", "teeth", "starts", "threadPasses", "css" -> null
        else -> Quantity.LENGTH
    }

    /** Escribe [text] en [key] y actualiza lo que depende de ese campo. */
    fun apply(draft: MachiningDraft, key: String, text: String): MachiningDraft =
        relink(draft.copy(fields = draft.fields + (key to text), recommendationApplied = false), key)

    /** Rellena los campos derivados a partir de los principales (al abrir, o tras cambiar de operación). */
    fun recompute(draft: MachiningDraft): MachiningDraft {
        val feedKey = if (isTurning(draft.operation)) "feed" else "fz"
        return listOf("vc", feedKey, "ae").fold(draft) { current, key -> relink(current, key) }
    }

    private fun relink(d: MachiningDraft, key: String): MachiningDraft {
        val turning = isTurning(d.operation)
        val boring = d.operation == TurningOperation.BORING.name
        val threading = d.operation == TurningOperation.THREADING.name
        val diameterKey = when {
            !turning -> "diameter"
            boring -> "finalDiameter"
            else -> "initialDiameter"
        }
        val feedKey = if (turning) "feed" else "fz"
        val fields = d.fields.toMutableMap()

        fun number(k: String): Double? = fields[k]?.replace(',', '.')?.toDoubleOrNull()

        fun base(k: String): Double? {
            val n = number(k) ?: return null
            val q = quantityOf(k) ?: return n
            return MachUnit.toBase(n, d.unitOf(k, q))
        }

        fun put(k: String, baseValue: Double, decimals: Int) {
            val q = quantityOf(k)
            val shown = if (q == null) baseValue else MachUnit.convert(baseValue, MachUnit.base(q), d.unitOf(k, q))
            fields[k] = format(shown, decimals)
        }

        val diameter = base(diameterKey)?.takeIf { it > 0 }

        // 1) Vc ⇄ RPM
        if (key == "rpm") {
            val n = number("rpm")?.takeIf { it > 0 }
            if (n != null && diameter != null) put("vc", PI * diameter * n / 1000.0, 4)
        } else if (key == "vc" || key == diameterKey) {
            val vc = base("vc")?.takeIf { it > 0 }
            if (vc != null && diameter != null) put("rpm", vc * 1000.0 / (PI * diameter), 1)
        }

        // 2) avance por revolución/diente ⇄ avance en mm/min (en roscado el avance lo da el paso)
        val teeth = if (turning) 1 else fields["teeth"]?.toIntOrNull()?.takeIf { it > 0 }
        val rpm = number("rpm")?.takeIf { it > 0 }
        if (!threading && teeth != null && rpm != null) {
            if (key == "vf") {
                val vf = base("vf")?.takeIf { it > 0 }
                if (vf != null) put(feedKey, vf / (rpm * teeth), 6)
            } else if (key in setOf(feedKey, "teeth", "vc", "rpm", diameterKey)) {
                val feed = base(feedKey)?.takeIf { it > 0 }
                if (feed != null) put("vf", feed * teeth * rpm, 3)
            }
        }

        // 3) ancho radial ⇄ % del diámetro (solo fresado)
        if (!turning && diameter != null) {
            if (key == "aePct") {
                val pct = number("aePct")?.takeIf { it > 0 }
                if (pct != null) put("ae", pct / 100.0 * diameter, 6)
            } else if (key == "ae" || key == diameterKey) {
                val ae = base("ae")?.takeIf { it > 0 }
                if (ae != null) fields["aePct"] = format(ae / diameter * 100.0, 1)
            }
        }
        return d.copy(fields = fields)
    }

    private fun format(value: Double, decimals: Int): String =
        BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
