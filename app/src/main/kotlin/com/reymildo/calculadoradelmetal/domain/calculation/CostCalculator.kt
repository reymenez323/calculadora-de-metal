package com.reymildo.calculadoradelmetal.domain.calculation

import com.reymildo.calculadoradelmetal.domain.model.PieceCostResult

object CostCalculator {

    fun calculate(
        pieceVolumeCm3: Double,
        quantity: Int,
        costPerVolumeCm3: Double? = null,
        manualPieceCostOverride: Double? = null,
    ): Result<PieceCostResult> {
        if (!pieceVolumeCm3.isFinite() || pieceVolumeCm3 <= 0) {
            return Result.failure(IllegalArgumentException("El volumen de la pieza debe ser un número positivo."))
        }
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("La cantidad de piezas debe ser mayor que cero."))
        }
        if (manualPieceCostOverride == null && costPerVolumeCm3 == null) {
            return Result.failure(IllegalArgumentException("Se necesita un costo por volumen o un costo manual."))
        }

        val costPerPiece = when {
            manualPieceCostOverride != null -> {
                if (!manualPieceCostOverride.isFinite() || manualPieceCostOverride < 0) {
                    return Result.failure(IllegalArgumentException("El costo manual debe ser un número positivo."))
                }
                manualPieceCostOverride
            }
            else -> {
                if (!costPerVolumeCm3!!.isFinite() || costPerVolumeCm3 < 0) {
                    return Result.failure(IllegalArgumentException("El costo por volumen debe ser un número positivo."))
                }
                pieceVolumeCm3 * costPerVolumeCm3
            }
        }

        return Result.success(PieceCostResult(costPerPiece = costPerPiece, totalCost = costPerPiece * quantity))
    }
}
