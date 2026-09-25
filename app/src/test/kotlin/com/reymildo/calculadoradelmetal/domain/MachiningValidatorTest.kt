package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.domain.machining.CuttingRecommendation
import com.reymildo.calculadoradelmetal.domain.machining.FieldIssue
import com.reymildo.calculadoradelmetal.domain.machining.IssueCode
import com.reymildo.calculadoradelmetal.domain.machining.IssueSeverity
import com.reymildo.calculadoradelmetal.domain.machining.MachineKind
import com.reymildo.calculadoradelmetal.domain.machining.MachineLimits
import com.reymildo.calculadoradelmetal.domain.machining.MachiningValidator
import com.reymildo.calculadoradelmetal.domain.machining.MillingOperation
import com.reymildo.calculadoradelmetal.domain.machining.TurningOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MachiningValidatorTest {
    private val turningDefaults = mapOf(
        "initialDiameter" to 50.0, "finalDiameter" to 40.0, "cutLength" to 100.0,
        "vc" to 180.0, "feed" to 0.2, "depth" to 2.0, "finish" to 0.5,
        "grooveWidth" to 5.0, "toolWidth" to 3.0, "pitch" to 1.5,
    )
    private val ints = mapOf("starts" to 1, "threadPasses" to 8, "teeth" to 4)
    private val millingDefaults = mapOf(
        "diameter" to 10.0, "vc" to 100.0, "fz" to 0.05, "ap" to 2.0, "ae" to 5.0,
        "totalDepth" to 6.0, "path" to 100.0, "finish" to 0.0,
    )

    private fun turning(op: TurningOperation, machine: MachineLimits? = null, rec: CuttingRecommendation? = null, vararg override: Pair<String, Double?>) =
        MachiningValidator.validate(true, op.name, (turningDefaults + override.toMap()).let { m -> { k: String -> m[k] } }, { ints[it] }, machine, rec)

    private fun milling(vararg override: Pair<String, Double?>) =
        MachiningValidator.validate(false, MillingOperation.FACE.name, (millingDefaults + override.toMap()).let { m -> { k: String -> m[k] } }, { ints[it] }, null, null)

    private fun List<FieldIssue>.codeOf(key: String) = firstOrNull { it.key == key }?.code

    @Test
    fun `boring with final diameter not larger than initial is flagged on the final diameter`() {
        val issues = turning(TurningOperation.BORING)
        assertEquals(IssueCode.FINAL_GT_INITIAL, issues.codeOf("finalDiameter"))
        assertTrue(turning(TurningOperation.BORING, override = arrayOf("initialDiameter" to 20.0, "finalDiameter" to 30.0)).none { it.severity == IssueSeverity.ERROR })
    }

    @Test
    fun `turning with final diameter not smaller than initial is flagged`() {
        assertEquals(IssueCode.FINAL_LT_INITIAL, turning(TurningOperation.TURNING, override = arrayOf("finalDiameter" to 60.0)).codeOf("finalDiameter"))
        assertTrue(turning(TurningOperation.TURNING).none { it.severity == IssueSeverity.ERROR })
    }

    @Test
    fun `missing or non positive values are reported on their own field`() {
        val issues = turning(TurningOperation.TURNING, override = arrayOf("feed" to null, "vc" to 0.0))
        assertEquals(IssueCode.REQUIRED, issues.codeOf("feed"))
        assertEquals(IssueCode.REQUIRED, issues.codeOf("vc"))
        assertNull(issues.codeOf("depth"))
    }

    @Test
    fun `finishing stock must be smaller than the stock to remove`() {
        assertEquals(IssueCode.FINISH_TOO_BIG, turning(TurningOperation.TURNING, override = arrayOf("finish" to 5.0)).codeOf("finish"))
        assertEquals(IssueCode.FINISH_TOO_BIG, milling("finish" to 6.0).codeOf("finish"))
        assertNull(turning(TurningOperation.TURNING, override = arrayOf("finish" to null)).codeOf("finish"))
    }

    @Test
    fun `radial width larger than the cutter is an error and a deep pass is only a warning`() {
        assertEquals(IssueCode.RADIAL_EXCEEDS_DIAMETER, milling("ae" to 12.0).codeOf("ae"))
        val deep = milling("ap" to 10.0).first { it.key == "ap" }
        assertEquals(IssueCode.SINGLE_PASS, deep.code)
        assertEquals(IssueSeverity.WARNING, deep.severity)
    }

    @Test
    fun `conventional machine without a slow enough step flags the cutting speed`() {
        val lathe = MachineLimits(MachineKind.CONVENTIONAL, 2000.0, 1000.0, listOf(500.0, 1000.0, 2000.0))
        val issue = turning(TurningOperation.TURNING, lathe, null, "initialDiameter" to 200.0, "finalDiameter" to 190.0, "vc" to 20.0).first { it.key == "vc" }
        assertEquals(IssueCode.NO_STEP_LOW, issue.code)
        assertEquals(500.0, issue.a!!, 1e-9)
    }

    @Test
    fun `values outside the recommended range only warn`() {
        val rec = CuttingRecommendation(140.0, 180.0, 220.0, 0.10, 0.20, 0.35)
        val issues = turning(TurningOperation.TURNING, null, rec, "vc" to 400.0)
        val warning = issues.first { it.key == "vc" }
        assertEquals(IssueCode.OUT_OF_RANGE, warning.code)
        assertEquals(IssueSeverity.WARNING, warning.severity)
        assertTrue(issues.none { it.severity == IssueSeverity.ERROR })
    }
}
