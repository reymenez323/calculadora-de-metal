package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.calculation.UnitConverter
import com.reymildo.calculadoradelmetal.domain.model.LengthUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    private val epsilon = 1e-9

    @Test
    fun `inches convert to cm`() {
        assertEquals(2.54, UnitConverter.toCm(1.0, LengthUnit.IN), epsilon)
    }

    @Test
    fun `feet convert to cm`() {
        assertEquals(30.48, UnitConverter.toCm(1.0, LengthUnit.FT), epsilon)
    }

    @Test
    fun `millimeters convert to cm`() {
        assertEquals(1.0, UnitConverter.toCm(10.0, LengthUnit.MM), epsilon)
    }

    @Test
    fun `meters convert to cm`() {
        assertEquals(100.0, UnitConverter.toCm(1.0, LengthUnit.M), epsilon)
    }

    @Test
    fun `round trip conversion preserves value`() {
        val result = UnitConverter.convert(UnitConverter.convert(5.0, LengthUnit.FT, LengthUnit.MM), LengthUnit.MM, LengthUnit.FT)
        assertEquals(5.0, result, 1e-6)
    }
}
