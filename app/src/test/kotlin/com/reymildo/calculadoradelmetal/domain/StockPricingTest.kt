package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.calculation.StockPricing
import com.reymildo.calculadoradelmetal.domain.calculation.VolumeCalculator
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.domain.model.Shape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockPricingTest {

    private val plateDims = mapOf(
        DimensionType.WIDTH to DimensionValue(4.0, LengthUnit.FT),
        DimensionType.LENGTH to DimensionValue(8.0, LengthUnit.FT),
        DimensionType.THICKNESS to DimensionValue(0.25, LengthUnit.IN),
    )

    @Test
    fun `cost per volume matches manual division`() {
        val stockPrice = 1000.0
        val volume = VolumeCalculator.calculate(Shape.Plate, plateDims).getOrThrow()
        val result = StockPricing.costPerVolumeCm3(Shape.Plate, plateDims, stockPrice).getOrThrow()
        assertEquals(stockPrice / volume, result, 1e-9)
    }

    @Test
    fun `zero price is treated as not configured, not an error`() {
        val result = StockPricing.costPerVolumeCm3(Shape.Plate, plateDims, 0.0).getOrThrow()
        assertEquals(0.0, result, 1e-9)
    }

    @Test
    fun `invalid stock dimensions propagate failure`() {
        val invalidDims = mapOf(DimensionType.WIDTH to DimensionValue(4.0, LengthUnit.FT))
        val result = StockPricing.costPerVolumeCm3(Shape.Plate, invalidDims, 1000.0)
        assertTrue(result.isFailure)
    }

    @Test
    fun `negative price fails`() {
        val result = StockPricing.costPerVolumeCm3(Shape.Plate, plateDims, -1.0)
        assertTrue(result.isFailure)
    }
}
