package com.reymildo.calculadoradelmetal.domain.machining

enum class Quantity { LENGTH, POWER, FEED_RATE, CUTTING_SPEED }

/**
 * Unidades que se pueden elegir valor por valor. Todo se guarda y se calcula en la unidad base de
 * su magnitud (mm, kW, mm/min, m/min); [toBase] es cuántas unidades base vale una de esta.
 */
enum class MachUnit(val symbol: String, val quantity: Quantity, val toBase: Double) {
    MM("mm", Quantity.LENGTH, 1.0),
    CM("cm", Quantity.LENGTH, 10.0),
    IN("in", Quantity.LENGTH, 25.4),
    FT("ft", Quantity.LENGTH, 304.8),
    M("m", Quantity.LENGTH, 1000.0),

    KW("kW", Quantity.POWER, 1.0),
    W("W", Quantity.POWER, 0.001),
    HP("HP", Quantity.POWER, 0.745699872),
    CV("CV", Quantity.POWER, 0.73549875),

    MM_MIN("mm/min", Quantity.FEED_RATE, 1.0),
    IN_MIN("in/min", Quantity.FEED_RATE, 25.4),
    M_MIN_FEED("m/min", Quantity.FEED_RATE, 1000.0),

    M_MIN("m/min", Quantity.CUTTING_SPEED, 1.0),
    SFM("SFM", Quantity.CUTTING_SPEED, 0.3048),
    ;

    companion object {
        fun of(quantity: Quantity): List<MachUnit> = entries.filter { it.quantity == quantity }

        fun base(quantity: Quantity): MachUnit = of(quantity).first()

        /** Unidad por defecto de cada magnitud en el sistema métrico o imperial. */
        fun default(system: MachiningUnitSystem, quantity: Quantity): MachUnit = when (quantity) {
            Quantity.LENGTH -> if (system == MachiningUnitSystem.METRIC) MM else IN
            Quantity.POWER -> KW
            Quantity.FEED_RATE -> if (system == MachiningUnitSystem.METRIC) MM_MIN else IN_MIN
            Quantity.CUTTING_SPEED -> if (system == MachiningUnitSystem.METRIC) M_MIN else SFM
        }

        fun parse(name: String?, fallback: MachUnit): MachUnit =
            name?.let { runCatching { valueOf(it) }.getOrNull() }?.takeIf { it.quantity == fallback.quantity } ?: fallback

        fun convert(value: Double, from: MachUnit, to: MachUnit): Double {
            require(from.quantity == to.quantity) { "magnitudes distintas" }
            return value * from.toBase / to.toBase
        }

        fun toBase(value: Double, from: MachUnit): Double = value * from.toBase
    }
}

fun MachiningDraft.unitOf(key: String, quantity: Quantity): MachUnit =
    MachUnit.parse(fieldUnits[key], MachUnit.default(unitSystem, quantity))
