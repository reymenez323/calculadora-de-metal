package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.ui.common.Fmt
import com.reymildo.calculadoradelmetal.ui.common.sanitizeDecimal
import com.reymildo.calculadoradelmetal.ui.common.toDecimalOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumericInputTest {
    @Test fun `comma and point decimals parse`() {
        assertEquals(12.5, "12,5".toDecimalOrNull()!!, 0.0)
        assertEquals(12.5, "12.5".toDecimalOrNull()!!, 0.0)
    }

    @Test fun `ambiguous and invalid input is rejected without changing meaning`() {
        assertNull("1,234.50".toDecimalOrNull())
        assertNull("1/2".toDecimalOrNull())
        assertNull("-5".toDecimalOrNull())
        assertEquals("7", "1/2".sanitizeDecimal(previous = "7"))
    }

    @Test fun `editable format has no grouping and preserves small values`() {
        assertEquals("1234.5", Fmt.editable(1234.5))
        assertEquals("0.000012345", Fmt.editable(0.000012345))
    }
}
