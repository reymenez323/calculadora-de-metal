package com.reymildo.calculadoradelmetal.domain.model

import com.reymildo.calculadoradelmetal.domain.calculation.UnitConverter
import kotlinx.serialization.Serializable

@Serializable
data class DimensionValue(val value: Double, val unit: LengthUnit) {
    fun toCm(): Double = UnitConverter.toCm(value, unit)
}
