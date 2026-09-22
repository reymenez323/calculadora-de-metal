package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.calculation.VolumeCalculator
import com.reymildo.calculadoradelmetal.domain.model.DimensionType
import com.reymildo.calculadoradelmetal.domain.model.DimensionValue
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import com.reymildo.calculadoradelmetal.domain.model.Shape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class VolumeCalculatorTest {

    private fun cm(value: Double) = DimensionValue(value, LengthUnit.CM)

    @Test
    fun `round bar volume`() {
        val dims = mapOf(DimensionType.DIAMETER to cm(2.0), DimensionType.LENGTH to cm(10.0))
        val result = VolumeCalculator.calculate(Shape.RoundBar, dims).getOrThrow()
        assertEquals(PI * 1.0 * 1.0 * 10.0, result, 1e-9)
    }

    @Test
    fun `square bar volume`() {
        val dims = mapOf(DimensionType.SIDE to cm(2.0), DimensionType.LENGTH to cm(5.0))
        val result = VolumeCalculator.calculate(Shape.SquareBar, dims).getOrThrow()
        assertEquals(20.0, result, 1e-9)
    }

    @Test
    fun `rectangular bar volume`() {
        val dims = mapOf(
            DimensionType.WIDTH to cm(2.0),
            DimensionType.HEIGHT to cm(3.0),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.RectangularBar, dims).getOrThrow()
        assertEquals(60.0, result, 1e-9)
    }

    @Test
    fun `plate volume`() {
        val dims = mapOf(
            DimensionType.WIDTH to cm(10.0),
            DimensionType.LENGTH to cm(20.0),
            DimensionType.THICKNESS to cm(0.5),
        )
        val result = VolumeCalculator.calculate(Shape.Plate, dims).getOrThrow()
        assertEquals(100.0, result, 1e-9)
    }

    @Test
    fun `round tube volume`() {
        val dims = mapOf(
            DimensionType.DIAMETER to cm(4.0),
            DimensionType.WALL_THICKNESS to cm(0.5),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.RoundTube, dims).getOrThrow()
        assertEquals(17.5 * PI, result, 1e-9)
    }

    @Test
    fun `round tube rejects wall thicker than radius`() {
        val dims = mapOf(
            DimensionType.DIAMETER to cm(2.0),
            DimensionType.WALL_THICKNESS to cm(2.0),
            DimensionType.LENGTH to cm(10.0),
        )
        assertTrue(VolumeCalculator.calculate(Shape.RoundTube, dims).isFailure)
    }

    @Test
    fun `rectangular tube volume`() {
        val dims = mapOf(
            DimensionType.WIDTH to cm(4.0),
            DimensionType.HEIGHT to cm(4.0),
            DimensionType.WALL_THICKNESS to cm(0.5),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.RectangularTube, dims).getOrThrow()
        assertEquals(70.0, result, 1e-9)
    }

    @Test
    fun `hex bar volume`() {
        val dims = mapOf(DimensionType.ACROSS_FLATS to cm(2.0), DimensionType.LENGTH to cm(10.0))
        val result = VolumeCalculator.calculate(Shape.HexBar, dims).getOrThrow()
        assertEquals((sqrt(3.0) / 2) * 4.0 * 10.0, result, 1e-9)
    }

    @Test
    fun `angle volume`() {
        val dims = mapOf(
            DimensionType.WIDTH to cm(5.0),
            DimensionType.HEIGHT to cm(5.0),
            DimensionType.THICKNESS to cm(1.0),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.Angle, dims).getOrThrow()
        assertEquals(90.0, result, 1e-9)
    }

    @Test
    fun `angle rejects thickness larger than a leg`() {
        val dims = mapOf(
            DimensionType.WIDTH to cm(1.0),
            DimensionType.HEIGHT to cm(5.0),
            DimensionType.THICKNESS to cm(2.0),
            DimensionType.LENGTH to cm(10.0),
        )
        assertTrue(VolumeCalculator.calculate(Shape.Angle, dims).isFailure)
    }

    @Test
    fun `channel volume`() {
        val dims = mapOf(
            DimensionType.WIDTH to cm(5.0),
            DimensionType.HEIGHT to cm(10.0),
            DimensionType.THICKNESS to cm(1.0),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.Channel, dims).getOrThrow()
        assertEquals(180.0, result, 1e-9)
    }

    @Test
    fun `i beam volume`() {
        val dims = mapOf(
            DimensionType.HEIGHT to cm(10.0),
            DimensionType.WIDTH to cm(5.0),
            DimensionType.WEB_THICKNESS to cm(1.0),
            DimensionType.FLANGE_THICKNESS to cm(1.0),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.IBeam, dims).getOrThrow()
        assertEquals(180.0, result, 1e-9)
    }

    @Test
    fun `t bar volume`() {
        val dims = mapOf(
            DimensionType.HEIGHT to cm(10.0),
            DimensionType.WIDTH to cm(5.0),
            DimensionType.WEB_THICKNESS to cm(1.0),
            DimensionType.FLANGE_THICKNESS to cm(1.0),
            DimensionType.LENGTH to cm(10.0),
        )
        val result = VolumeCalculator.calculate(Shape.TBar, dims).getOrThrow()
        assertEquals(140.0, result, 1e-9)
    }

    @Test
    fun `cross unit round bar volume`() {
        val dims = mapOf(
            DimensionType.DIAMETER to DimensionValue(1.0, LengthUnit.IN),
            DimensionType.LENGTH to DimensionValue(1.0, LengthUnit.FT),
        )
        val result = VolumeCalculator.calculate(Shape.RoundBar, dims).getOrThrow()
        assertEquals(154.44, result, 0.01)
    }

    @Test
    fun `missing dimension fails validation`() {
        val dims = mapOf(DimensionType.DIAMETER to cm(2.0))
        assertTrue(VolumeCalculator.calculate(Shape.RoundBar, dims).isFailure)
    }

    @Test
    fun `zero dimension fails validation`() {
        val dims = mapOf(DimensionType.DIAMETER to cm(0.0), DimensionType.LENGTH to cm(10.0))
        assertTrue(VolumeCalculator.calculate(Shape.RoundBar, dims).isFailure)
    }

    @Test
    fun `negative dimension fails validation`() {
        val dims = mapOf(DimensionType.DIAMETER to cm(-2.0), DimensionType.LENGTH to cm(10.0))
        assertTrue(VolumeCalculator.calculate(Shape.RoundBar, dims).isFailure)
    }

    @Test
    fun `nan dimension fails validation`() {
        val dims = mapOf(DimensionType.DIAMETER to cm(Double.NaN), DimensionType.LENGTH to cm(10.0))
        assertTrue(VolumeCalculator.calculate(Shape.RoundBar, dims).isFailure)
    }
}
