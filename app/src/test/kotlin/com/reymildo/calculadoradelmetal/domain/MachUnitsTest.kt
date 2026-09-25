package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.domain.machining.MachiningUnitSystem
import com.reymildo.calculadoradelmetal.domain.machining.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MachUnitsTest {
    @Test
    fun `power converts to kilowatts`() {
        assertEquals(0.745699872, MachUnit.toBase(1.0, MachUnit.HP), 1e-9)
        assertEquals(14.9, MachUnit.toBase(14900.0, MachUnit.W), 1e-9)
        assertEquals(14.914, MachUnit.toBase(20.0, MachUnit.HP), 1e-3)
    }

    @Test
    fun `cutting speed and feed rate convert to base units`() {
        assertEquals(100.0, MachUnit.toBase(328.083989501, MachUnit.SFM), 1e-6)
        assertEquals(508.0, MachUnit.toBase(20.0, MachUnit.IN_MIN), 1e-9)
        assertEquals(25.4, MachUnit.toBase(1.0, MachUnit.IN), 1e-9)
    }

    @Test
    fun `round trip keeps the value`() {
        for (unit in MachUnit.entries) {
            val base = MachUnit.toBase(12.5, unit)
            assertEquals(12.5, MachUnit.convert(base, MachUnit.base(unit.quantity), unit), 1e-9)
        }
    }

    @Test
    fun `units of different magnitudes cannot be mixed and defaults follow the system`() {
        assertThrows(IllegalArgumentException::class.java) { MachUnit.convert(1.0, MachUnit.MM, MachUnit.HP) }
        assertEquals(MachUnit.SFM, MachUnit.default(MachiningUnitSystem.IMPERIAL, Quantity.CUTTING_SPEED))
        assertEquals(MachUnit.KW, MachUnit.default(MachiningUnitSystem.IMPERIAL, Quantity.POWER))
    }
}
