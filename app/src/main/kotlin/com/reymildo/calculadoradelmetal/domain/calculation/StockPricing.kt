package com.reymildo.calculadoradelmetal.domain.calculation

import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.Shape

object StockPricing {

    /**
     * stockPrice == 0.0 means the material's price hasn't been configured yet;
     * this legitimately yields a rate of 0.0 rather than a validation failure.
     */
    fun costPerVolumeCm3(
        shape: Shape,
        stockDims: Map<DimensionType, DimensionValue>,
        stockPrice: Double,
    ): Result<Double> {
        if (!stockPrice.isFinite() || stockPrice < 0) {
            return Result.failure(IllegalArgumentException("El costo del material en bruto debe ser un número positivo."))
        }

        return VolumeCalculator.calculate(shape, stockDims).map { stockVolumeCm3 ->
            if (stockPrice == 0.0) 0.0 else stockPrice / stockVolumeCm3
        }
    }
}
