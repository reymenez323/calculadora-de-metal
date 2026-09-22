package com.reymildo.calculadoradelmetal.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LengthUnit(val toCmFactor: Double, val symbol: String) {
    MM(0.1, "mm"),
    CM(1.0, "cm"),
    M(100.0, "m"),
    IN(2.54, "in"),
    FT(30.48, "ft"),
}
