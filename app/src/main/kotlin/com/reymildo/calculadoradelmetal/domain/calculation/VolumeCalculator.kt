package com.reymildo.calculadoradelmetal.domain.calculation

import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.Shape

object VolumeCalculator {

    fun validate(shape: Shape, dims: Map<DimensionType, DimensionValue>): List<String> {
        val errors = mutableListOf<String>()

        for (required in shape.requiredDimensions) {
            val dimension = dims[required]
            if (dimension == null) {
                errors += "Falta la dimensión $required."
            } else if (!dimension.value.isFinite() || dimension.value <= 0) {
                errors += "La dimensión $required debe ser un número positivo."
            }
        }

        if (errors.isEmpty()) {
            errors += shape.additionalValidation(dims)
        }

        return errors
    }

    fun calculate(shape: Shape, dims: Map<DimensionType, DimensionValue>): Result<Double> {
        val errors = validate(shape, dims)
        if (errors.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(errors.joinToString(" ")))
        }
        return Result.success(shape.volumeCm3(dims))
    }
}
