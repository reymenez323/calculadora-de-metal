package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineLimits
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCalculator
import com.reymildo.calculadoradelmetal.domain.machining.MillingInput
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.TurningInput
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MachiningCalculatorTest {
    private val cnc = MachineLimits(MachineKind.CNC, maxRpm = 3000.0, maxFeedMmMin = 5000.0)

    @Test
    fun `reference milling case is limited to 3000 rpm and 600 millimeters per minute`() {
        val result = MachiningCalculator.milling(
            MillingInput(MillingOperation.CONTOUR, 10.0, 100.0, 4, 0.05, 2.0, 5.0, 6.0, 100.0),
            cnc,
        ).getOrThrow()
        assertEquals(3183.0988, result.theoreticalRpm, 0.001)
        assertEquals(3000.0, result.adjustedRpm, 0.001)
        assertEquals(600.0, result.feedMmMin, 0.001)
        assertEquals(3, result.passes.totalPasses)
    }

    @Test
    fun `conventional machine chooses nearest lower available speed`() {
        val machine = MachineLimits(MachineKind.CONVENTIONAL, 2500.0, 3000.0, listOf(500.0, 1000.0, 1600.0, 2000.0))
        val result = MachiningCalculator.milling(
            MillingInput(MillingOperation.FACE, 50.0, 300.0, 4, 0.05, 2.0, 20.0, 2.0, 100.0),
            machine,
        ).getOrThrow()
        assertEquals(1600.0, result.adjustedRpm, 0.001)
        assertTrue("stepped_rpm" in result.warnings)
    }

    @Test
    fun `finish allowance adds one finishing pass`() {
        val result = MachiningCalculator.turning(
            TurningInput(TurningOperation.TURNING, 50.0, 40.0, 100.0, 180.0, 0.2, 2.0, finishAllowanceMm = 0.5),
            cnc,
        ).getOrThrow()
        assertEquals(4, result.passes.totalPasses)
        assertEquals(3, result.passes.roughPasses)
        assertEquals(1, result.passes.finishPasses)
    }

    @Test
    fun `multi start thread uses lead as feed per revolution`() {
        val result = MachiningCalculator.turning(
            TurningInput(
                TurningOperation.THREADING, 30.0, 29.0, 25.0, 40.0, 1.0, 1.0,
                threadPitchMm = 1.5, threadStarts = 2, threadPasses = 8,
            ), cnc,
        ).getOrThrow()
        assertEquals(3.0, result.feedPerRevolutionMm, 1e-9)
        assertEquals(8, result.passes.totalPasses)
    }

    @Test
    fun `constant surface speed remains finite at center`() {
        val result = MachiningCalculator.turning(
            TurningInput(
                TurningOperation.FACING, 100.0, 0.0, 2.0, 180.0, 0.2, 1.0,
                constantSurfaceSpeed = true,
            ), cnc,
        ).getOrThrow()
        assertTrue(result.cuttingTimeMin.isFinite())
        assertTrue(result.cuttingTimeMin > 0.0)
    }

    @Test
    fun `boring requires a larger final diameter`() {
        assertTrue(
            MachiningCalculator.turning(
                TurningInput(TurningOperation.BORING, 20.0, 15.0, 50.0, 100.0, 0.1, 1.0), cnc,
            ).isFailure,
        )
    }
}
