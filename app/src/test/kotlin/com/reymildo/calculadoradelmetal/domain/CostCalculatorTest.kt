package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.calculation.CostCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CostCalculatorTest {

    @Test
    fun `normal case computes per piece and total cost`() {
        val result = CostCalculator.calculate(
            pieceVolumeCm3 = 10.0,
            quantity = 5,
            costPerVolumeCm3 = 2.0,
        ).getOrThrow()
        assertEquals(20.0, result.costPerPiece, 1e-9)
        assertEquals(100.0, result.totalCost, 1e-9)
    }

    @Test
    fun `manual override takes precedence over rate`() {
        val result = CostCalculator.calculate(
            pieceVolumeCm3 = 10.0,
            quantity = 3,
            costPerVolumeCm3 = 2.0,
            manualPieceCostOverride = 50.0,
        ).getOrThrow()
        assertEquals(50.0, result.costPerPiece, 1e-9)
        assertEquals(150.0, result.totalCost, 1e-9)
    }

    @Test
    fun `zero or negative quantity fails`() {
        assertTrue(CostCalculator.calculate(pieceVolumeCm3 = 10.0, quantity = 0, costPerVolumeCm3 = 2.0).isFailure)
        assertTrue(CostCalculator.calculate(pieceVolumeCm3 = 10.0, quantity = -1, costPerVolumeCm3 = 2.0).isFailure)
    }

    @Test
    fun `zero or negative volume fails`() {
        assertTrue(CostCalculator.calculate(pieceVolumeCm3 = 0.0, quantity = 5, costPerVolumeCm3 = 2.0).isFailure)
        assertTrue(CostCalculator.calculate(pieceVolumeCm3 = -1.0, quantity = 5, costPerVolumeCm3 = 2.0).isFailure)
    }

    @Test
    fun `missing cost source fails`() {
        val result = CostCalculator.calculate(pieceVolumeCm3 = 10.0, quantity = 5)
        assertTrue(result.isFailure)
    }

    @Test
    fun `negative rate fails`() {
        val result = CostCalculator.calculate(pieceVolumeCm3 = 10.0, quantity = 5, costPerVolumeCm3 = -2.0)
        assertTrue(result.isFailure)
    }
}
