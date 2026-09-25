package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.machining.MachUnit
import com.reymildo.calculadoradelmetal.domain.machining.MachiningDraft
import com.reymildo.calculadoradelmetal.domain.machining.MachiningLinks
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI

class MachiningLinksTest {
    private fun milling() = MachiningLinks.recompute(
        MachiningDraft(
            operation = MillingOperation.FACE.name,
            fields = mapOf("diameter" to "10", "vc" to "100", "teeth" to "4", "fz" to "0.05", "ap" to "2", "ae" to "5", "totalDepth" to "6", "path" to "100", "finish" to "0"),
        ),
    )

    private fun MachiningDraft.num(key: String) = fields.getValue(key).toDouble()

    @Test
    fun `recompute fills rpm, feed rate and radial percentage from the primary values`() {
        val d = milling()
        assertEquals(100.0 * 1000 / (PI * 10), d.num("rpm"), 0.1)
        assertEquals(0.05 * 4 * d.num("rpm"), d.num("vf"), 0.01)
        assertEquals(50.0, d.num("aePct"), 1e-9)
    }

    @Test
    fun `editing rpm updates cutting speed and feed rate, and editing them updates rpm`() {
        var d = MachiningLinks.apply(milling(), "rpm", "2000")
        assertEquals(PI * 10 * 2000 / 1000.0, d.num("vc"), 1e-3)
        assertEquals(0.05 * 4 * 2000, d.num("vf"), 1e-2)
        d = MachiningLinks.apply(d, "vc", "50")
        assertEquals(50.0 * 1000 / (PI * 10), d.num("rpm"), 0.1)
    }

    @Test
    fun `editing the feed rate solves the feed per tooth, and teeth change the feed rate`() {
        var d = MachiningLinks.apply(milling(), "vf", "500")
        assertEquals(500.0 / (4 * d.num("rpm")), d.num("fz"), 1e-5)
        d = MachiningLinks.apply(d, "teeth", "2")
        assertEquals(d.num("fz") * 2 * d.num("rpm"), d.num("vf"), 0.01)
    }

    @Test
    fun `changing the diameter keeps cutting speed and radial width but updates rpm, feed and percentage`() {
        val before = milling()
        val d = MachiningLinks.apply(before, "diameter", "20")
        assertEquals(100.0, d.num("vc"), 1e-9)
        assertEquals(before.num("rpm") / 2, d.num("rpm"), 0.1)
        assertEquals(before.num("vf") / 2, d.num("vf"), 0.1)
        assertEquals(5.0, d.num("ae"), 1e-9)
        assertEquals(25.0, d.num("aePct"), 1e-9)
    }

    @Test
    fun `radial percentage drives the radial width`() {
        val d = MachiningLinks.apply(milling(), "aePct", "40")
        assertEquals(4.0, d.num("ae"), 1e-9)
    }

    @Test
    fun `linked values respect the unit chosen for each field`() {
        val base = milling().copy(fieldUnits = mapOf("vc" to MachUnit.SFM.name))
        val d = MachiningLinks.apply(base, "vc", "328.084")
        assertEquals(100.0 * 1000 / (PI * 10), d.num("rpm"), 0.5)
        val fromRpm = MachiningLinks.apply(d, "rpm", "1000")
        assertEquals(PI * 10 * 1000 / 1000.0 / 0.3048, fromRpm.num("vc"), 1e-2)
    }

    @Test
    fun `boring uses the final diameter as reference and threading has no feed rate link`() {
        val boring = MachiningLinks.recompute(
            MachiningDraft(TurningOperation.BORING.name, mapOf("initialDiameter" to "20", "finalDiameter" to "30", "vc" to "100", "feed" to "0.1")),
        )
        assertEquals(100.0 * 1000 / (PI * 30), boring.num("rpm"), 0.1)
        val untouched = MachiningLinks.apply(boring, "initialDiameter", "22")
        assertEquals(boring.num("rpm"), untouched.num("rpm"), 1e-9)

        val threading = MachiningLinks.recompute(
            MachiningDraft(TurningOperation.THREADING.name, mapOf("initialDiameter" to "20", "finalDiameter" to "18", "vc" to "60", "feed" to "0.1")),
        )
        assertNull(threading.fields["vf"])
    }
}
