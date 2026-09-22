package com.reymildo.calculadoradelmetal.domain.calculation

import com.reymildo.calculadoradelmetal.domain.model.LengthUnit

object UnitConverter {
    fun toCm(value: Double, unit: LengthUnit): Double = value * unit.toCmFactor

    fun fromCm(valueCm: Double, target: LengthUnit): Double = valueCm / target.toCmFactor

    fun convert(value: Double, from: LengthUnit, to: LengthUnit): Double =
        fromCm(toCm(value, from), to)
}
