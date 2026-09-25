package com.reymildo.calculadoradelmetal.domain

import androidx.lifecycle.SavedStateHandle
import com.reymildo.calculadoradelmetal.domain.machining.MachiningUnitSystem
import com.reymildo.calculadoradelmetal.ui.machining.MachiningViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class MachiningDraftStateTest {
    @Test
    fun `unit change preserves physical magnitude and survives recreation`() {
        val handle = SavedStateHandle()
        val first = MachiningViewModel(handle)
        first.updateMilling { it.copy(fields = it.fields + ("diameter" to "10")) }
        first.switchUnits(turning = false, target = MachiningUnitSystem.IMPERIAL)
        assertEquals("0.393701", first.milling.value.fields.getValue("diameter"))

        val recreated = MachiningViewModel(handle)
        assertEquals(MachiningUnitSystem.IMPERIAL, recreated.milling.value.unitSystem)
        recreated.switchUnits(turning = false, target = MachiningUnitSystem.METRIC)
        assertEquals(10.0, recreated.milling.value.fields.getValue("diameter").toDouble(), 0.0001)
    }

    @Test
    fun `turning and milling keep independent drafts`() {
        val viewModel = MachiningViewModel(SavedStateHandle())
        viewModel.updateTurning { it.copy(materialId = "ss304") }
        viewModel.updateMilling { it.copy(materialId = "al6061") }
        assertEquals("ss304", viewModel.turning.value.materialId)
        assertEquals("al6061", viewModel.milling.value.materialId)
    }
}
