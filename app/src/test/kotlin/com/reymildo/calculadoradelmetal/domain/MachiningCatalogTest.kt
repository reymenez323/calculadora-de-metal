package com.reymildo.calculadoradelmetal.domain

import com.reymildo.calculadoradelmetal.data.local.entity.CuttingToolEntity
import com.reymildo.calculadoradelmetal.data.repository.MachiningRepository
import com.reymildo.calculadoradelmetal.domain.machining.MachineType
import com.reymildo.calculadoradelmetal.domain.machining.MachiningCatalog
import com.reymildo.calculadoradelmetal.domain.machining.MaterialCategory
import com.reymildo.calculadoradelmetal.domain.machining.ToolKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MachiningCatalogTest {
    @Test
    fun `every category has ordered generic ranges for every tool kind and operation family`() {
        for (category in MaterialCategory.entries) for (kind in ToolKind.entries) for (turning in listOf(true, false)) {
            val r = MachiningCatalog.generic(category, kind, turning)
            assertTrue("$category $kind", r.cuttingSpeedMinMMin <= r.cuttingSpeedStartMMin && r.cuttingSpeedStartMMin <= r.cuttingSpeedMaxMMin)
            assertTrue("$category $kind", r.feedMinMm <= r.feedStartMm && r.feedStartMm <= r.feedMaxMm)
            assertTrue(r.cuttingSpeedMinMMin > 0 && r.feedMinMm > 0)
        }
    }

    @Test
    fun `hss generic values are more conservative than carbide`() {
        for (category in MaterialCategory.entries) for (turning in listOf(true, false)) {
            val hss = MachiningCatalog.generic(category, ToolKind.HSS, turning)
            val carbide = MachiningCatalog.generic(category, ToolKind.CARBIDE_INSERT, turning)
            assertTrue("$category", hss.cuttingSpeedStartMMin < carbide.cuttingSpeedStartMMin)
        }
    }

    @Test
    fun `built-in tool defaults keep both generic profiles and separate lathe from mill`() {
        val names = MachiningRepository.builtInTools.map { it.name }
        assertTrue(names.contains("Inserto de carburo genérico"))
        assertFalse(names.any { it.contains("plaquita", ignoreCase = true) })
        val endMill = MachiningRepository.builtInTools.first { it.toolKind == ToolKind.CARBIDE_ENDMILL }
        assertFalse(endMill.fits(MachineType.LATHE))
        assertTrue(endMill.fits(MachineType.MILL))
        val hss = MachiningRepository.builtInTools.first { it.toolKind == ToolKind.HSS }
        assertTrue(hss.fits(MachineType.LATHE) && hss.fits(MachineType.MILL))
        assertEquals(setOf("a36", "ss304", "ss316", "al6061", "al6063"), MachiningRepository.builtInMaterials.map { it.id }.toSet())
    }

    @Test
    fun `user tool is bound to a single machine type`() {
        val insert = CuttingToolEntity(name = "CNMG", kind = ToolKind.CARBIDE_INSERT.name, machineType = MachineType.LATHE.name)
        assertTrue(insert.fits(MachineType.LATHE))
        assertFalse(insert.fits(MachineType.MILL))
    }
}
